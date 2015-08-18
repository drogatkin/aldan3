/* aldan3 - HttpUtils.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: HttpUtils.java,v 1.16 2012/08/11 06:15:44 dmitriy Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.util.inet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.aldan3.util.DataConv;

/** Base HTTP/HTML manipulation methods.
 * 
 * @author dmitriy
 *
 */
public class HttpUtils {
	final static String[] hex = { "%00", "%01", "%02", "%03", "%04", "%05", "%06", "%07", "%08", "%09", "%0a", "%0b",
			"%0c", "%0d", "%0e", "%0f", "%10", "%11", "%12", "%13", "%14", "%15", "%16", "%17", "%18", "%19", "%1a",
			"%1b", "%1c", "%1d", "%1e", "%1f", "+", "!", "%22", "%23", "$", "%25", "%26", "'", "(", ")", "*", "%2b",
			",", "-", ".", "%2f", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "%3a", "%3b", "%3c", "%3d", "%3e",
			"%3f", "%40", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
			"S", "T", "U", "V", "W", "X", "Y", "Z", "%5b", "%5c", "%5d", "%5e", "_", "%60", "a", "b", "c", "d", "e",
			"f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
			"%7b", "%7c", "%7d", "%7e", "%7f", "%80", "%81", "%82", "%83", "%84", "%85", "%86", "%87", "%88", "%89",
			"%8a", "%8b", "%8c", "%8d", "%8e", "%8f", "%90", "%91", "%92", "%93", "%94", "%95", "%96", "%97", "%98",
			"%99", "%9a", "%9b", "%9c", "%9d", "%9e", "%9f", "%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6", "%a7",
			"%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af", "%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6",
			"%b7", "%b8", "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf", "%c0", "%c1", "%c2", "%c3", "%c4", "%c5",
			"%c6", "%c7", "%c8", "%c9", "%ca", "%cb", "%cc", "%cd", "%ce", "%cf", "%d0", "%d1", "%d2", "%d3", "%d4",
			"%d5", "%d6", "%d7", "%d8", "%d9", "%da", "%db", "%dc", "%dd", "%de", "%df", "%e0", "%e1", "%e2", "%e3",
			"%e4", "%e5", "%e6", "%e7", "%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee", "%ef", "%f0", "%f1", "%f2",
			"%f3", "%f4", "%f5", "%f6", "%f7", "%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff" };
	
	public static final String BYTES_UNIT = "bytes";
	
	/**
	 * Encode a sequence of bytes to the "x-www-form-urlencoded" form
	 *
	 * <ul>
	 * <li><p>The ASCII characters 'a' through 'z', 'A' through 'Z',
	 *        and '0' through '9' remain the same.
	 *
	 * <li><p>The space character ' ' is converted into a plus sign '+'.
	 *
	 * <li><p>All other ASCII characters are converted into the
	 *        3-character string "%xy", where xy is
	 *        the two-digit hexadecimal representation of the character
	 *        code
	 *
	 * </ul>
	 *
	 * @param s The string to be encoded
	 * @return The encoded string
	 */
	public static String urlEncode(String _s) {
		try {
			return URLEncoder.encode(_s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return URLEncoder.encode(_s);
		}
	}

	/** Encodes URL string using %XX sequences
	 * 
	 * @param _s URl string
	 * @return encoded result
	 */
	public static String urlHexEncode(String _s) {
		char[] sa = _s.toCharArray();
		StringBuffer sbuf = new StringBuffer(sa.length);
		for (int i = 0; i < sa.length; i++)
			if (sa[i] == '.' || (sa[i] >= 'A' && sa[i] <= 'Z') || (sa[i] >= 'a' && sa[i] <= 'z')
					|| (sa[i] >= '0' && sa[i] <= '9'))
				sbuf.append(sa[i]);
			else if (sa[i] < 16)
				sbuf.append('%').append('0').append(Integer.toHexString(sa[i] & 255));
			else
				sbuf.append('%').append(Integer.toHexString(sa[i] & 255));
		return sbuf.toString();
	}

	/** Does URL decoding
	 * 
	 * @param _encoded string
	 * @return decoded result
	 */
	public static String urlDecode(String _encoded) {
		// TODO: rewrite using [i] instead of charAt(i)
		StringBuffer decoded = new StringBuffer();
		int len = _encoded.length();
		for (int i = 0; i < len; ++i) {
			if (_encoded.charAt(i) == '%' && i + 2 < len) {
				int d1 = Character.digit(_encoded.charAt(i + 1), 16);
				int d2 = Character.digit(_encoded.charAt(i + 2), 16);
				if (d1 != -1 && d2 != -1)
					decoded.append((char) ((d1 << 4) + d2));
				i += 2;
			} else if (_encoded.charAt(i) == '+')
				decoded.append(' ');
			else
				decoded.append(_encoded.charAt(i));
		}
		return decoded.toString();
	}

	/** Does HTML encode
	 * 
	 * @param _s source string
	 * @return HTMl encoded result
	 */
	public static String htmlEncode(String _s) {
		return htmlEncode(_s, false, false);
	}

	/** Does HTML encoding with special blanks processing.
	 * 
	 * @param _s source string to encode
	 * @param _encodeLeadingSpaces true means convert all leading blanks to &amp;nbsp;
	 * @param _encodeWhiteSpaces true means convert all white spaces to &amp;nbsp;
	 * @return result string encoded
	 */
	public static String htmlEncode(String _s, boolean _encodeLeadingSpaces, boolean _encodeWhiteSpaces) {
		if (_s == null)
			return null;
		char[] s = _s.toCharArray();
		StringBuffer res = new StringBuffer(s.length);
		int ls = 0;
		boolean blankMet = true;
		for (int i = 0; i < s.length; i++) {
			switch (s[i]) {
			case '<':
				res.append(s, ls, i - ls);
				res.append("&lt;");
				ls = i + 1;
				break;
			case '>':
				res.append(s, ls, i - ls);
				res.append("&gt;");
				ls = i + 1;
				break;
			case '"':
				res.append(s, ls, i - ls);
				res.append("&quot;");
				ls = i + 1;
				break;
			case '&':
				res.append(s, ls, i - ls);
				res.append("&amp;");
				ls = i + 1;
				break;
			case '\'':
				res.append(s, ls, i - ls);
				res.append("&#39;");
				ls = i + 1;
				break;
			case ' ':
				if (blankMet && (_encodeLeadingSpaces || _encodeWhiteSpaces)) {
					res.append(s, ls, i - ls);
					res.append("&nbsp;");
					ls = i + 1;
				} else
					blankMet = true;
				break;
			case '\n':
				if (_encodeWhiteSpaces) {
					res.append(s, ls, i - ls);
					ls = i + 1;
				}
				break;
			case '\r':
				if (_encodeWhiteSpaces) {
					res.append(s, ls, i - ls);
					res.append("<BR>");
					ls = i + 1;
				}
				break;
			//			case '\'':
			//				res.append(s, ls, i - ls);
			//				res.append("&#x27;");
			//				ls = i + 1;
			//				break;
			default:
				blankMet = false;
				_encodeLeadingSpaces = false;
			}
		}
		if (ls < s.length)
			res.append(s, ls, s.length - ls);
		return res.toString();
	}

	/** hashes possible &lt;script&gt; 
	 * 
	 * @param s
	 * @return with hashed tags
	 * Use for preventing cross site scripting when string can include HTML 
	 */
	public static String hashJSScriptTag(String s) {
		if (s == null)
			return s;
		return s.replaceAll("<[sS][cC][rR][iI][pP][tT]\\s*.*>", "<&#x73;&#x43;&#x52;&#x49;&#x50;&#x54;>");
	}

	public static String htmlDecode(String _s) {
		return htmlDecode(_s, null);
	}

	/** Decodes HTML encoded string
	 * 
	 * @param _s source HTML encoded string 
	 * @return result string with no encoding
	 */
	public static String htmlDecode(String _s, String encoding) {
		int len = _s.length();
		StringBuffer res = new StringBuffer(len);
		int pos = 0;
		do {
			int ai = _s.indexOf('&', pos);
			if (ai < 0) { // no more escapes in the string
				res.append(_s.substring(pos, len));
				break;
			}
			res.append(_s.substring(pos, ai));
			int si = _s.indexOf(';', ai + 1);
			if (si > ai) { // it can't be 0 b'cause ai + 1
				String c = (String) htmlEscMap.get(_s.substring(ai + 1, si));
				if (c != null)
					res.append(c);
				else if (_s.charAt(ai + 1) == '#' && ai + 2 < si)
					res.append((char) Integer.parseInt(_s.substring(ai + 2, si)));
				else
					res.append(_s.substring(ai, si)); // no entity, put as is
				pos = si + 1;
			} else { // no more ;
				res.append(_s.substring(ai));
				break;
			}
		} while (pos < len);
		return res.toString();
	}

	/** Converts a string to be suitable for Java string presentation.
	 * It converts line separator and other specific characters to Java lang string acceptable
	 * @param _s source string
	 * @return result string with possible convertion
	 */
	public static String toJavaString(String _s) {
		if (_s == null) {
			return null;
		}

		char[] s = _s.toCharArray();
		StringBuffer toReturn = new StringBuffer(s.length);

		for (int i = 0; i < s.length; i++) {
			switch (s[i]) {
			case '"':
				toReturn.append("\\" + "x22");
				break;
			case '\'':
				toReturn.append("\\" + "x27");
				break;
			case '\b':
				toReturn.append("\\" + "b");
				break;
			case '\t':
				toReturn.append("\\" + "t");
				break;
			case '\n':
				toReturn.append("\\" + "n");
				break;
			case '\r':
				toReturn.append("\\" + "r");
				break;
			case '\f':
				toReturn.append("\\" + "f");
				break;
			case '\\':
				toReturn.append("\\\\");
				break;
			//			case '\v':
			//				toReturn.append("\\" + "v");
			//				break;
			default:
				// TODO if (s[i] > 127)  toReturn.append("\\x").append(tohex
				toReturn.append(String.valueOf(s[i]));
			}
		}
		return toReturn.toString();
	}

	/** CZonvert arbitrary string to string with escaping upon JSON format 
	 * 
	 * @param _s input string
	 * @return JSON escaped string
	 */
	public static String toJSONString(String _s) {
		if (_s == null) {
			return "null";
		}

		char[] s = _s.toCharArray();
		StringBuffer toReturn = new StringBuffer(s.length);

		for (int i = 0; i < s.length; i++) {
			switch (s[i]) {
			case '"':
				toReturn.append("\\\"");
				break;
			case '/':
				toReturn.append("\\/");
				break;
			case '\\':
				toReturn.append("\\\\");
				break;
			case '\b':
				toReturn.append("\\b");
				break;
			case '\t':
				toReturn.append("\\" + "t");
				break;
			case '\n':
				toReturn.append("\\" + "n");
				break;
			case '\r':
				toReturn.append("\\" + "r");
				break;
			case '\f':
				toReturn.append("\\" + "f");
				break;
			default:
				// TODO more work on special characters
				if (s[i] > 127 || s[i] < 32)
					toReturn.append(String.format("\\u%04x", (int) s[i]));
				else
					toReturn.append(s[i]);
			}
		}
		return toReturn.toString();
	}
	
	/** converts arbitrary object to JSON string
	 * 
	 * @param _o Object
	 * @return JSON string of object
	 */
	public static String toJSONString(Object _o) {
		return toJSONString(DataConv.objectToString(_o));
	}

	/** the methods provides quoted printable encoding with line splitting.
	 * <p> 
	 *  Note: UNICoDE characters get excluded
	 * @param s
	 * @return returned as encoded, although it is a string, it supposes to be interpreted as byte[]
	 * @exception NullPounterException if source string is null
	 *  See also Request for Comments: 2045 for details on the encoding.
	 */
	public static String quoted_printableEncode(String s, String encoding) {
		try {
			byte[] sbtBytes = encoding == null ? s.getBytes() : s.getBytes(encoding);
			
			StringBuffer result = new StringBuffer(sbtBytes.length * 3 / 2);

			int ll = 0;
			for (int i = 0, n = sbtBytes.length; i < n; i++) {
				char c = (char) (sbtBytes[i] & 255);
				if (c >= 33 && c <= 60 || c >= 62 && c <= 126) {
					result.append(c);
					ll++;
				} else if (c < 256) {
					result.append('=');
					if (c < 16)
						result.append('0');
					result.append(Integer.toHexString(c).toUpperCase());
					ll += 3;
				}
				if (ll >= 72) {
					result.append("=\r\n");
					ll = 0;
				}
			}
			return result.toString();
		} catch (Exception e) {
			return null;
		}
	}
	
	/** Analyzes range header and extract range values for HTTP requests with range
	 * 
	 * @param range Range header value, can be null
	 * @param flen requested resource full length
	 * @return range boundaries as start and end values, total requested length can be calculated as 
	 * result[1] - result[0] + 1
	 * @exception Illegal argument exception gets thrown if range header can't be parsed 
	 */
	public static long[] parseRangeHeader(String range, long flen) {
		if (range == null || range.length() == 0)
			return null;
		long[] result = null;
		if (range.regionMatches(true, 0, BYTES_UNIT, 0, BYTES_UNIT.length())) {
			int i = range.indexOf('-');
			if (i > 0) {
				result = new long[2];
				result[0] = 0;
				result[1] = -1;
				try {
					result[0] = Long.parseLong(range.substring(BYTES_UNIT.length() + 1, i));
					if (result[0] < 0)
						throw new NumberFormatException("Invalid start range value:" + result[0]);
					try {
						result[1] = Long.parseLong(range.substring(i + 1));
					} catch (NumberFormatException nfe) {
						result[1] = flen - 1;
					}
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Ranges values are invalid", nfe);
				}
			} // else invalid range? ignore?
		} // else other units not supported
		// long clen = er < 0 ? flen : (er - sr + 1);
		return result;
	}

	static HashMap htmlEscMap = new HashMap(4);
	static {
		htmlEscMap.put("lt", "<");
		htmlEscMap.put("gt", ">");
		htmlEscMap.put("quot", "\"");
		htmlEscMap.put("amp", "&");
		htmlEscMap.put("reg", "\u00AE");
		htmlEscMap.put("copy", "\u00A9");
		htmlEscMap.put("nbsp", " ");
		htmlEscMap.put("sect", "\u00A7");
		htmlEscMap.put("cent", "\u00A2");
		htmlEscMap.put("pound", "\u00A3");
		htmlEscMap.put("yen", "\u00A5");
		htmlEscMap.put("curren", "\u00A4");
		htmlEscMap.put("micro", "\u00B5");
		htmlEscMap.put("euro", "\u20AC");
		htmlEscMap.put("trade", "\u2122");
		// more codes ess http://www.w3schools.com/tags/ref_entities.asp 
		// and code converter http://code.cside.com/3rdpage/us/javaUnicode/converter.html
	}
}
