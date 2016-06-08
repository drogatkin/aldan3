/* aldan3 - SendMail.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: SendMail.java,v 1.8 2012/08/06 06:24:38 dmitriy Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.util.inet;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.net.ssl.SSLSocketFactory;

/** A simple but powerful solution for sending e-mails. This class provides a basic  e-mail
 * send operation with unlimited enhancements possibilities. 
 * <p>
 * An order to use the following:
 * <ol>
 * <li>Create SendMail object specifying SMTP server/MTA parameters in Properties; the following properties
 * supported:
 *   <ul> 
 *     <li>MAILHOST - IP or symbolic address of SMTP server
 *     <li>MAILPORT - connection port, if not specified then default 25 used
 *     <li>MAILTIMEOUT - connection timeout in minutes, default - 2
 *     <li>MAILACCOUNT - login if authentication required
 *     <li>MAILPASSWORD - password if authentication required, note specifying password will trigger authentication sequence
 *     <li>MAILSECURELAYER - flag to use secure socket to SMTP server connection, 'true' means use, others - do not
 *   </ul>
 *   For example:<br>
 *   <code>
 *       Properties ps = new Properties();
 *       ps.put(SendMail.PROP_MAILHOST, "mailhost.com");
 *       SendMail sm = new SendMail(ps);
 *   </code>
 * <li>Prepare additional mail headers and store them in Properties object
 * <li>Send mail using <code>send</code> method.
 * </ol>
 * @author dmitriy
 *
 */
public class SendMail {
	protected final static int DEF_TIMEOUT = 2; // minutes

	protected final static int DEF_MAILPORT = 25;

	/** SMTP server connection timeout in minutes
	 * 
	 */
	public final static String PROP_MAILTIMEOUT = "MAILTIMEOUT";

	/**
	 * SMTP server host
	 */
	public final static String PROP_MAILHOST = "MAILHOST";

	/**
	 * SMTP server port
	 */
	public final static String PROP_MAILPORT = "MAILPORT";

	/**
	 * SMTP server account required for authentication
	 */
	public final static String PROP_POPACCNT = "MAILACCOUNT";

	/**
	 * SMTP server password
	 */
	public final static String PROP_PASSWORD = "MAILPASSWORD";
	
	/** host name or IP of e-mail sender
	 * 
	 */
	public final static String PROP_MAILSEND_HOST = "MAILSENDHOST";

	/**
	 * Additional oAuth2 token for stronger authentication
	 */
	public  static final String PROP_OAUTH2_TOKEN = "OAUTH2_TOKEN";

	/**
	 * Using secure layer flag
	 */
	public final static String PROP_SECURE = "MAILSECURELAYER";


	protected Properties properties;

	protected static final SimpleDateFormat PROTOCOL_GMTDATE = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'");

	static {
		TimeZone tz = TimeZone.getTimeZone("GMT");
		tz.setID("GMT");
		PROTOCOL_GMTDATE.setTimeZone(tz);
	}

	protected static String fixDataLines(String s) {
		StringBuffer data = new StringBuffer(s.length());
		StringBuffer line = new StringBuffer(128);

		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);

			switch (ch) {
			case '\r':
				break;

			case '\n':
				data.append(line);
				data.append("\r\n");
				line.setLength(0);
				break;
			case '.':
				if (line.length() == 0)
					line.append(ch);
			default:
				line.append(ch);
				break;
			}
		}

		if (line.length() > 0) {
				data.append(line);
				data.append("\r\n");
		}
		
		return data.toString();
	}

	protected static String escapeSpecials(String s) {
		StringBuffer result = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '<' || c == '>')
				result.append('\\');
			result.append(c);
		}
		return result.toString();
	}

	public static String splitLine(String line, int size) {
		if (line.length() <= size)
			return line;
		int l = line.length();
		int chunks = l / size;
		StringBuffer result = new StringBuffer(l + chunks * 2);
		int n = 0;
		for (int i = 0; i < chunks; i++) {
			result.append(line.substring(n, n + size));
			n += size;
			result.append("\r\n");
		}
		result.append(line.substring(n));
		return result.toString();
	}

	/** Creates SendMail object and set base SMTP server protocol parameters
	 * 
	 * @param _properties SMTP server connection properties
	 */
	public SendMail(Properties _properties) {
		properties = _properties;
	}

	/** Initiates of mail send.
	 * <p> An example of use:
	 * <pre>
	 *   Properties emailHeaders = new Properties();
	 *   emailHeaders.put("To", "undisclosed-recipients:;");
	 *   emailHeaders.put("Mime-Version", "1.0");
	 *   emailHeaders.put("Content-Type", "text/plain; format=flowed; charset=UTF-8");
	 *   try {
	 *       sm.send(adminAddress, up.getField("userEmail"), "Activation notice",
	 *           Substitutor.substitute("activationemail.txt", map, properties, locale, tz), emailHeaders);
	 *   } catch(IOException ioe) {
	 *   }
	 * </pre>
	 * 
	 * @param _mailFrom address from
	 * @param _mailTo address to, can be overridden to appear in to field differently if "TO" property 
	 * specified in extra headers
	 * @param _subject subject of mail
	 * @param _body body of mail, can be html or have attachments if extra headers specified
	 * @param _extraHeaders allow to customize e-mail using different display TO values, MIME types and so on
	 * @throws IOException if there is a problem of sending e-mail including error responses from SMTP server
	 */
	public void send(String _mailFrom, String _mailTo, String _subject, String _body, Properties _extraHeaders)
			throws IOException {
		if (properties == null)
			properties = System.getProperties();

		boolean ssl = "true".equals(properties.getProperty(PROP_SECURE, "false"));
		String mailHost = properties.getProperty(PROP_MAILHOST, "Unknown");
		int mailPort = DEF_MAILPORT;
		try {
			mailPort = Integer.parseInt(properties.getProperty(PROP_MAILPORT, "" + DEF_MAILPORT));
		} catch (Exception e) {
		}
		
		String sendHost = properties.getProperty(PROP_MAILSEND_HOST);
		if (sendHost == null) {
			// TODO calculate from own InetAddress
			sendHost = "localhost";
		}
		
		String popAccount = properties.getProperty(PROP_POPACCNT, "Unknown");
		TextSocket s = null;
		try {
			s = new TextSocket(mailHost, mailPort, ssl);
			int timeout = DEF_TIMEOUT;
			try {
				timeout = Integer.parseInt(properties.getProperty(PROP_MAILTIMEOUT));
			} catch (Exception e) {
			}
			s.setSoTimeout(timeout * 60 * 1000);

			s.getResult();

			s.write("HELO ");
			//s.writeLine(popAccount);
			s.writeLine(sendHost);
			s.flush();

			if (s.getResult() != 250)
				throw new IOException("At presenting POP account " + popAccount + ", a mail server returned code "
						+ s.lastResult());
			// TODO analyze for supported auth
			boolean auth = false;
			String password = properties.getProperty(PROP_PASSWORD);
			if (password != null && password.length() > 0) {
				s.writeLine("AUTH PLAIN");
				s.flush();
				if (s.getResult() != 334)
					throw new IOException("At plain authentication of " + popAccount + ", a mail server returned code "
							+ s.lastResult());
				byte[] acntBytes = popAccount.getBytes();
				byte[] passwdBytes = password.getBytes();
				byte[] credentialBytes = new byte[acntBytes.length + passwdBytes.length + 2];
				System.arraycopy(acntBytes, 0, credentialBytes, 1, acntBytes.length);
				System.arraycopy(passwdBytes, 0, credentialBytes, acntBytes.length + 2, passwdBytes.length);
				s.writeLine(Base64Codecs.base64Encode(credentialBytes));
				s.flush();
				if (s.getResult() == 334) {
					s.writeLine("");
					auth = true;
				}
			} else {
				// https://developers.google.com/api-client-library/java/google-api-java-client/oauth2
				// https://developers.google.com/android/guides/http-auth
				// http://android-developers.blogspot.com/2012/09/google-play-services-and-oauth-identity.html	
				String oauthToken = properties.getProperty(PROP_OAUTH2_TOKEN);
				if (oauthToken != null && oauthToken.length() > 0) {
					String authToken = "user="+popAccount+'\001'+"auth=Bearer "+oauthToken+"\001\001"; 
					s.write("AUTH XOAUTH2 ");
					s.writeLine(Base64Codecs.base64Encode(authToken.getBytes()));
					s.flush();
					auth = true;
				}
			}
			if (auth && s.getResult() != 235)
				throw new IOException("At credentials check of " + popAccount + ", a mail server returned code "
						+ s.lastResult());
			// 535 5.7.1 Credentials Rejected 12sm5390241nzn
			s.write("MAIL FROM:<");
			// TODO: check that args[2] is correct domain name
			// s.write(escapeSpecials(_mailFrom));

			s.write(_mailFrom);

			if (_mailFrom.indexOf('@') < 0) {
				s.write("@");
				s.write(mailHost);
			}
			s.writeLine(">");
			s.flush();

			if (s.getResult() != 250)
				throw new IOException(String.format("From: address '%s' (%s) was rejected with code %s", _mailFrom,
						mailHost, s.lastResult()));

			StringTokenizer t = new StringTokenizer(_mailTo, ";,");
			while (t.hasMoreTokens()) {
				s.write("RCPT TO:<");
				String toAddr = t.nextToken();
				s.write(escapeSpecials(toAddr));
				if (toAddr.indexOf('@') < 0) {
					s.write("@");
					s.write(mailHost);
				}
				s.writeLine(">");
				s.flush();

				if (s.getResult() != 250)
					throw new IOException(String.format("Recipient to: address '%s' was rejected with code %s", toAddr,
							s.lastResult()));
			}

			s.writeLine("DATA");
			s.flush();

			s.getResult();

			s.write("Subject: ");
			s.writeLine(_subject);
			s.write("From: ");
			s.writeLine(_mailFrom);
			String charSet = null;
			boolean wasTo = false;
			boolean wasMailer = false;
			if (_extraHeaders != null) {
				Enumeration e = _extraHeaders.keys();
				while (e.hasMoreElements()) {
					String name = (String) e.nextElement();
					wasTo |= name.regionMatches(true, 0, "TO", 0, 2);
					wasMailer |= name.regionMatches(true, 0, "X-Mailer", 0, "X-Mailer".length());
					s.writeLine(name + ": " + _extraHeaders.getProperty(name));
					if (charSet == null && "Content-Type".equalsIgnoreCase(name)) {
						String cts = _extraHeaders.getProperty(name);
						int csp = cts.indexOf("charset=");
						if (csp > 0)
							charSet = cts.substring(csp + "charset=".length()).trim();
					}
				}
			}
			if (wasTo == false) {
				s.write("To: ");
				s.writeLine(_mailTo);
			}
			s.write("Date: ");
			s.writeLine(PROTOCOL_GMTDATE.format(new Date()));
			if (wasMailer == false)
				s.writeLine("X-Mailer: genuine Aldan3 framework $Revision: 1.8 $");

			s.writeLine("");

			if (_body != null && _body.length() > 0) {
				if (charSet == null)
					s.writeBytes(fixDataLines(_body).getBytes());
				else
					s.writeBytes(fixDataLines(_body).getBytes(charSet));
			}
			s.writeLine(".");
			s.flush();
			if (s.getResult() != 250)
				throw new IOException("At writing body the mail server returned code " + s.lastResult());

			s.writeLine("QUIT");
			s.flush();
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception ex) {
			throw (IOException) new IOException("By: " + ex).initCause(ex);
		} finally {
			if (s != null)
				s.close(); // TODO should catch IOException to do not mask an original exception
		}
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out
					.println("Use: SendMail to subject body [host] [port] [account] [pass] [ssl]"
							+ "\nFor example: SendMail jaddressbook@gmail.com Simple \"no body\" smtp.gmail.com 465 jaddressbook@gmail.com cu1cu1 true");
			System.exit(255);
			return;
		}
		Properties properties = null;
		if (args.length > 3) {
			properties = new Properties();
			properties.setProperty(PROP_MAILHOST, args[3]);
			if (args.length > 4) {
				properties.setProperty(PROP_MAILPORT, args[4]);
				if (args.length > 5) {
					properties.setProperty(PROP_POPACCNT, args[5]);
					if (args.length > 6) {
						properties.setProperty(PROP_PASSWORD, args[6]);
						if (args.length > 7) {
							properties.setProperty(PROP_SECURE, args[7]);
						}
					}
				}
			}
		}
		try {
			new SendMail(properties).send(args[0], args[0], args[1], args[2], null);
		} catch (IOException e) {
			System.out.println("Error in send:" + e);
			e.printStackTrace();
		}
	}

	public static class TextSocket {
		protected String m_lastResult;

		protected int m_code;

		protected BufferedReader m_input;

		protected OutputStream m_output;

		protected Socket socket;

		public static final byte[] LS = new byte[] { (byte) 13, (byte) 10 };

		protected void createStreams() throws IOException {
			m_input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			m_output = new BufferedOutputStream(socket.getOutputStream());
		}

		TextSocket(String host, int port, boolean ssl) throws UnknownHostException, IOException {

			if (ssl)
				socket = SSLSocketFactory.getDefault().createSocket(host, port);
			else
				socket = new Socket(host, port);

			createStreams();
		}

		void setSoTimeout(int timeout) throws SocketException {
			socket.setSoTimeout(timeout);
		}

		void close() throws IOException {
			socket.close();
		}

		public BufferedReader getBufferedInputStream() {
			return m_input;
		}

		public OutputStream getBufferedOutputStream() {
			return m_output;
		}

		public String readLine() throws IOException {
			return m_input.readLine();
		}

		public void write(String s) throws IOException {
			m_output.write(s.getBytes());
		}

		public void writeBytes(byte[] bytes) throws IOException {
			m_output.write(bytes);
		}

		public void writeLine(String s) throws IOException {
			write(s);
			m_output.write(LS);
		}

		public void flush() throws IOException {
			m_output.flush();
		}

		public String lastResult() {
			return m_lastResult;
		}

		public int lastResultCode() {
			return m_code;
		}

		public int getResult() throws IOException {
			m_lastResult = m_input.readLine();

			if (m_lastResult == null || m_lastResult.length() == 0) {
				return -1;
			}

			m_code = resultCode(m_lastResult);

			// See if it's a multiline response (nnn-...)
			if (m_code != -1 && m_lastResult.length() > 3 && m_lastResult.charAt(3) == '-') {

				do {
					String res = m_input.readLine();

					if (res.length() == 0) {
						break;
					}

					m_lastResult = m_lastResult + "\r\n" + res;

					// If it's a normal, matching reply code, we're done
					if (resultCode(res) == m_code && res.length() > 3 && res.charAt(3) == ' ') {
						break;
					}
				} while (true);
			}

			return m_code;
		}

		// Parse the first three digits of s; return -1
		// if malformed
		public int resultCode(String s) {
			int code = 0;
			int n;

			for (n = 0; n < 3 && n < s.length(); n++) {
				char ch = s.charAt(n);

				if (ch < '0' || ch > '9') {
					return -1;
				}
				code = 10 * code + ch - '0';
			}
			return code;
		}

		public boolean isMultilineResult(String s) {
			int n;

			for (n = 0; n < 3 && n < s.length(); n++) {
				if (s.charAt(n) < '0' || s.charAt(n) > '9') {
					return true;
				}
			}

			if (n < s.length() && s.charAt(n) != ' ') {
				return true;
			}
			return false;
		}
	}

}
