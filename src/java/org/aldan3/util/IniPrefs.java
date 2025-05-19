/* aldan3 - IniPrefs.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: IniPrefs.java,v 1.4 2009/08/04 22:35:38 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.aldan3.model.Log;
import org.aldan3.model.ServiceProvider;

/**
 * Parser of Windows style ini file [section] parameter=value,...
 */

/**
 * Provides object state serialization using Windows .ini like flat files
 * 
 */
public class IniPrefs extends AbstractPreferences implements ServiceProvider
{
	public final static String HOMEDIRSUFX = ".home";

	final static String INIEXT = ".ini";

	final static char COMMENT = '#';

	final static char ENC_MARK = '@';

	final static char ENDLN = '\n';

	final static char STARTSEC = '[';

	final static char ENDSEC = ']';

	final static char EQ = '=';

	final static char COMMA = ',';

	Hashtable directory;

	String inifolder;

	String programname;

	String encoding;
	
	Log logger;

	/**
	 * Creates a serializer with a given name
	 * 
	 * @param name
	 */
	public IniPrefs(String name) {
		this(name, null);
	}

	/**
	 * Creates a serializer with given name and using specified encoding
	 * 
	 * @param name
	 * @param encoding
	 */
	public IniPrefs(String name, String encoding) {
		super(null, "");
		logger = new Log() {

			@Override
			public void log(String severity, String where, String message, Throwable t, Object... details) {
				System.err.printf("%s: [%s] [%s] %s\n", new Date(), severity, where, String.format(message, details));
				if (t != null)
					t.printStackTrace();
			}
		};
		this.programname = name;
		this.encoding = encoding;
		inifolder = System.getProperty(name + HOMEDIRSUFX);
		if (inifolder == null) {
			Class use = null;
			try {
				use = Class.forName("javax.jnlp.UnavailableServiceException");
			} catch (ClassNotFoundException cne) {
			} catch (Error e) {
			}
		}

		if (inifolder == null)
			inifolder = System.getProperty("user.home");
		if (inifolder == null)
			inifolder = System.getProperty("user.dir");
		directory = new Hashtable();
	}

	/**
	 * Return home directory where information will be stored.
	 * 
	 * @return directory path string
	 */
	public String getHomeDirectory() {
		return inifolder;
	}

	/**
	 * Reads requested value from topic and entry
	 * 
	 * @param topic
	 * @param entry
	 * @return value, null if not defined yet
	 */
	public Object getProperty(Object topic, Object entry) {
		Hashtable chapter = (Hashtable) directory.get(topic);
		if (chapter == null)
			return null;
		return chapter.get(entry);
	}

	/**
	 * set and remove a property as well, if property value is null
	 * 
	 * @param topic
	 * @param entry
	 * @param value,
	 *            if null then removing
	 */
	public void setProperty(Object topic, Object entry, Object value) {
		Hashtable chapter = (Hashtable) directory.get(topic);
		if (chapter == null) {
			if (value == null)
				return; // nothing to do here
			chapter = new Hashtable();
			directory.put(topic, chapter);
		}
		if (value == null)
			chapter.remove(entry);
		else
			chapter.put(entry, value);
	}

	/**
	 * Load data
	 * 
	 * 
	 */
	public void load() {
		String line, el;
		char[] sb;
		Hashtable chapter = null;
		Object[] params, tempa;
		InputStreamReader isr = null;
		BufferedReader fr = null;
		boolean eof = true;
		enc: do {
			try { // System.err.println("open");
				if (fr != null) {
					fr.close();
					isr = null;
				}
				if (isr == null)
					if (encoding == null)
						isr = new InputStreamReader(new FileInputStream(new File(inifolder, programname + INIEXT)));
					else
						isr = new InputStreamReader(new FileInputStream(new File(inifolder, programname + INIEXT)),
								encoding);

				fr = new BufferedReader(isr);

				do {
					line = fr.readLine();
					eof = line == null;
					if (eof)
						break;
					sb = line.toCharArray();
					if (sb.length > 0) {
						if (sb[0] == COMMENT) {
							if (sb.length > 5) {
								if (sb[1] == ENC_MARK) {
									String actEncoding = new String(sb, 2, sb.length - 2);
									if (!actEncoding.equalsIgnoreCase(encoding)) {
										encoding = actEncoding;
										continue enc;
									}
								}
							}
							continue; // skip comment
						}
						if (sb[0] == STARTSEC) {
							line = new String(sb, 1, sb.length - 2); // System.err.println("sec "+line);
							chapter = (Hashtable) directory.get(line);
							if (chapter == null) {
								chapter = new Hashtable();
								directory.put(line, chapter);
							}
						} else {
							if (chapter == null)
								continue;
							StringTokenizer st = new StringTokenizer(line, "" + EQ);
							if (st.hasMoreTokens()) {
								line = st.nextToken();
								params = new Object[0];
								try {
									el = st.nextToken("" + COMMA).substring(1); // eat '='
									params = new Object[1]; // System.err.println("tok "+el);
									try {
										params[0] = new Integer(el);
									} catch (NumberFormatException nfe) {
										params[0] = el;
									}
								} catch (NoSuchElementException nsee) {
									params = new Object[0];
								}
								while (st.hasMoreTokens()) {
									el = st.nextToken();
									tempa = new Object[params.length + 1];
									System.arraycopy(params, 0, tempa, 0, params.length);
									params = tempa;
									try {
										params[params.length - 1] = new Integer(el);
									} catch (NumberFormatException nfe) {
										params[params.length - 1] = el;
									}
								}
								if (params.length == 1)
									chapter.put(line, params[0]);
								else if (params.length > 1)
									chapter.put(line, params);
							}
						}
					}
				} while (!eof);
				fr.close();
			} catch (IOException ioe) {
				logger.error("IO at reading INI", ioe);
				eof = true;
			}
		} while (!eof);
	}

	boolean readString(StringBuffer sb, BufferedReader rd) {
		sb.setLength(0);
		int c;
		for (;;) {
			try {
				c = rd.read();
				if (c == '\n')
					break;
				if (c == '\r')
					continue;
				if (c == -1) // eof
					return false;
				sb.append((char) c);
			} catch (IOException ioe) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Save data
	 * 
	 * 
	 */
	public void save() {
		try {
			OutputStreamWriter osw = null;
			if (osw == null)
				if (encoding != null)
					osw = new OutputStreamWriter(new FileOutputStream(new File(inifolder, programname + INIEXT)),
							encoding);
				else
					osw = new OutputStreamWriter(new FileOutputStream(new File(inifolder, programname + INIEXT)));
			BufferedWriter fw = new BufferedWriter(osw);
			fw.write(COMMENT + programname + ENDLN);
			if (encoding != null)
				fw.write("" + COMMENT + ENC_MARK + encoding + ENDLN);
			else
				fw.write("" + COMMENT + ENC_MARK + osw.getEncoding() + ENDLN);
			Enumeration ed = directory.keys();
			Hashtable chapter;
			Object chaptername, entryname;
			Object entry;
			while (ed.hasMoreElements()) {
				chaptername = ed.nextElement();
				fw.write(STARTSEC + chaptername.toString() + ENDSEC);
				fw.newLine();
				chapter = (Hashtable) directory.get(chaptername);
				Enumeration ec = chapter.keys();
				while (ec.hasMoreElements()) {
					entryname = ec.nextElement();
					if (entryname instanceof String)
						fw.write((String) entryname + EQ);
					else
						fw.write(entryname.toString() + EQ);
					entry = chapter.get(entryname);
					if (entry instanceof Object[]) {
						fw.write(((Object[]) entry)[0].toString());
						for (int i = 1; i < ((Object[]) entry).length; i++) {
							if (((Object[]) entry)[i] != null)
								fw.write(COMMA + ((Object[]) entry)[i].toString());
						}
						fw.newLine();
					} else if (entry instanceof String)
						fw.write((String) entry + ENDLN);
					else if (entry instanceof int[]) {
						int[] ia = (int[]) entry;
						if (ia.length > 0)
							fw.write("" + ia[0]);
						for (int i = 1; i < ia.length; i++) {
							fw.write(COMMA);
							fw.write("" + ia[i]);
						}
						fw.newLine();
					} else
						fw.write(entry.toString() + ENDLN);

				}
			}
			fw.flush();
			fw.close();
		} catch (IOException ioe) {
		}
	}

	/** Convert object to int value with default
	 * 
	 * @param o object to convert
	 * @param defval default value if null or conversion not possible
	 * @return int value
	 */
	public static int getInt(Object o, int defval) {
		if (o != null && o instanceof Integer)
			return ((Integer) o).intValue();
		return defval;
	}

	public String getPreferredServiceName() {
		return "IniHelper";
	}
	
	public Object getServiceProvider() {
		return this;
	}

	@Override
	protected AbstractPreferences childSpi(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		save();
		
	}

	@Override
	protected String getSpi(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void putSpi(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void removeSpi(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		load();
	}

}