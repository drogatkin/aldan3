/* aldan3 - DataConv.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: DataConv.java,v 1.14 2013/05/17 02:43:39 cvs Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.util;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Set of utility methods for data operations.
 * 
 * @author dmitriy
 * 
 */
public class DataConv {
	public final static String [] HTML5_CHARSETS = {"US-ASCII",
		"UTF-8",
		"ISO-8859-5",
		"windows-1251",
		"ISO-8859-2",
		"windows-1255",
		"windows-31J",
		"windows-949",
		"windows-1254",
		"windows-1257",
		"ISO-8859-13",
		"GB18030",
		"Big5",
		"KOI8-R",
		"KOI8-U",
		"UTF-16",
		"UTF-16BE",
		"UTF-16LE",
		"UTF-32",
		"UTF-32BE",
		"UTF-32LE"
		};
	
	private static String HASHHASH = "aldan3lib-1";

	private static final String[] HEX_DIGITS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
			"e", "f" };

	/**
	 * Bumps array size by one
	 *  Recreates a new array containing all source elements plus 1 extra.
	 * 
	 * @param array source array
	 * @return result array having plus one length
	 */
	public static Object[] extendByOne(Object[] array) {
		return extendByOne(array, true);
	}
	
	public static Object[] extendByOne(Object[] array, boolean right) {
		if (array == null)
			return new Object[1];
		Object[] result = new Object[array.length + 1];
		System.arraycopy(array, 0, result, right?0:1, array.length);
		return result;
	}

	// TODO: add listToString

	/** Converts enumeration to string with '|' element separator
	 * @param source enumeration
	 * @return a string concatenating all elements toString()
	 * 
	 */
	public static String enumerationToString(Enumeration _enumeration) {
		return enumerationToString(_enumeration, '|');
	}

	/** Converts enumeration to string with a specified element separator
	 * @param source enumeration
	 * @param element separator char
	 * @return a string concatenating all elements toString()
	 * 
	 */
	public static String enumerationToString(Enumeration _enumeration, char _div) {
		String result = "";
		if (_enumeration != null) {
			while (_enumeration.hasMoreElements()) {
				result += _enumeration.nextElement().toString();
				if (_enumeration.hasMoreElements())
					result += _div;
			}
		}
		return result;
	}

	/** Convert arbitrary object array to string presentation. Method toString used for
	 * elements of array.
	 * 
	 * @param _array arbitrary object array
	 * @param _div elements separator in result
	 * @return string presentation
	 */
	public static String arrayToString(Object _array, char _div) {
		if (_array == null)
			return "null";
		if (_array instanceof Object[] == false)
			return _array.toString();
		Object[] array = (Object[]) _array;
		StringBuffer result = new StringBuffer((array.length + 1) * 50);
		for (int i = 0; i < array.length; i++) {
			result.append(array[i] == null ? "null" : array[i].toString());
			if (i < array.length - 1)
				result.append(_div);
		}
		return result.toString();
	}
	
	/** Converts a collection to a string with separators
	 * 
	 * @param _collection
	 * @param _div separtor
	 * @param _null value used for null values, default "null"
	 * @return
	 */
	public static <C extends Collection<?>> String collectionToString(C _collection, String _div, String _null) {
		if (_collection == null)
			return "null";
        if (_null == null)
        	_null = "null";
		StringBuffer result = new StringBuffer((_collection.size() + 1) * 50);
		int c = 0;
		for (Iterator<?> i = _collection.iterator(); i.hasNext();) {
			Object obj = i.next();
			result.append(obj == null ? _null : obj.toString());
			if (c < _collection.size() - 1)
				result.append(_div);
			c++;
		}
		return result.toString();
	}

	/** Substitutes all entries in source matching to a pattern to another pattern, quoted if requested.
	 * 
	 * @param _source string where to replace
	 * @param _what a pattern to be replaced
	 * @param _by a new pattern
	 * @param _quoted needs to be quoted
	 * @return a new created string with new pattern
	 * Note this method doesn't work with reg exp.
	 */
	public static String substituteAllEntries(String _source, String _what, String _by, boolean _quoted) {
		return substituteAllEntries(_source, _what, _by, _quoted, "");
	}

	/** Substitutes all entries in source matching to a pattern to another pattern, quoted if requested.
	 * 
	 * @param _source string where to replace
	 * @param _what a pattern to be replaced
	 * @param _by a new pattern
	 * @param _quoted needs to be quoted
	 * @param default substitution for null
	 * @return a new created string with new pattern
	 * Note this method doesn't work with reg exp.
	 */
	public static String substituteAllEntries(String _source, String _what, String _by, boolean _quoted, String _default) {
		String result = _source;
		if (result == null)
			return result;
		if (_by == null) {
			if (_default != null)
				_by = _default;
			else
				_by = "";
		}
		if (_what.length() == 0)
			throw new IllegalArgumentException("Cannot substitute empty string.");
		if (_by.equals(_what))
			throw new IllegalArgumentException("Substitution for the same value " + _by + '.');
		int pp = 0;
		if (_quoted)
			while ((pp = result.indexOf(_what, pp)) >= 0) {
				result = result.substring(0, pp) + '\'' + _by + '\'' + result.substring(pp + _what.length());
				pp += _by.length() + 2;
			}
		else
			while ((pp = result.indexOf(_what, pp)) >= 0) {
				result = result.substring(0, pp) + _by + result.substring(pp + _what.length());
				pp += _by.length();
			}
		return result;
	}

	/**
	 * builds a locale from string in form lang_COUNTRY, or lang
	 * 
	 * @param locale
	 *            String locale, if null, then null returned
	 * @return Locale, no checks for correctness
	 * 
	 */
	public static Locale localeFromString(String locale) {
		if (locale == null)
			return null;
		int up = locale.indexOf('_');
		if (up > 0)
			return new Locale(locale.substring(0, up).toLowerCase(), locale.substring(up + 1).trim().toUpperCase());
		return new Locale(locale.trim().toLowerCase());
	}

	/**
	 * builds time zone from string
	 * @param string with time zone
	 * @return time zone object
	 * 
	 */
	public static TimeZone timeZoneFromString(String timeZone) {
		if (timeZone == null)
			return null;
		return TimeZone.getTimeZone(timeZone);
	}

	public static String encryptXor(String s) { // not trusted
		if (s == null)
			return s;
		StringBuffer result = new StringBuffer(s);
		for (int i = 0; i < result.length(); i++) {
			result.setCharAt(i, (char) (result.charAt(i) ^ HASHHASH.charAt(i % HASHHASH.length())));
		}
		return result.toString();
	}

	public static String bytesToHex(byte[] ba) {
		return bytesToHex(ba, 0, ba.length);
	}

	/** Converts bytes from range in hex string
	 * 
	 * @param ba source byte array
	 * @param start index
	 * @param len number bytes
	 * @return result hex string
	 */
	public static String bytesToHex(byte[] ba, int start, int len) {
		StringBuffer result = new StringBuffer(len);
		for (int i = 0; i < len; i++) {
			result.append(HEX_DIGITS[(ba[i + start] >> 4) & 15]).append(HEX_DIGITS[ba[i + start] & 15]);
		}
		return result.toString();
	}

	/** considers int as concatenation of 4 (max) 8-bit chars and returns the chars as String
	 * 
	 * @param val
	 * @return
	 */
	public static String intAsChars(int val) {
		String result = "";
		char c = (char) (val & 255);
		if (c != 0) {
			result += c;
			val >>= 8;
			c = (char) (val & 255);
			if (c != 0) {
				result += c;
				val >>= 8;
				c = (char) (val & 255);
				if (c != 0) {
					result += c;
				}
			}
		}
		return result;
	}

	/** Converts hex string to byte array
	 * 
	 * @param s hex string
	 * @return result byte array
	 * @exception OutOfBoundaries exception may happen if string has invalid hex presentation
	 */
	public static byte[] hexToBytes(String s) {
		char[] ca = s.toCharArray();
		byte[] result = new byte[(ca.length + 1) / 2];
		for (int i = 0; i < (ca.length + 1) / 2; i++) {
			int r = ca[i * 2] - '0';
			if (r > 9)
				r = ca[i * 2] - 'a' + 10;
			if (r > 15)
				r = ca[i * 2] - 'A' + 10;
			if (r > 15)
				r = 0;
			result[i] = (byte) (r << 4);
			if (i * 2 + 1 < ca.length) {
				r = ca[i * 2 + 1] - '0';
				if (r > 9)
					r = ca[i * 2 + 1] - 'a' + 10;
				if (r > 15)
					r = ca[i * 2 + 1] - 'A' + 10;
				if (r < 16)
					result[i] += (byte) r;
			}
		}
		return result;
	}

	/** A convenient method to convert string to it with default value
	 * 
	 * @param s source string
	 * @param def default value
	 * @return integer presentation of string or def if the string is null or not a valid integer
	 */
	public static int toIntWithDefault(String s, int def) {
		if (s == null)
			return def;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static long toLongWithDefault(String s, long def) {
		if (s == null)
			return def;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/** convert object to String using empty String for null object
	 * 
	 * @param obj
	 * @return obj.toString() or empty String if obj is null
	 */
	public static String objectToString(Object obj) {
		if (obj == null)
			return "";
		if (obj instanceof Date) {
			return DateTime.dateToJson((Date)obj);
		}
		return obj.toString();
	}
	
	/** checks if a string not null and not empty
	 * 
	 * @param s
	 * @return
	 */
	public static boolean hasValue(String s) {
		return s!= null && s.length() > 0;
	}
	
	/** returns first parameter which isn't null
	 * 
	 * @param vals parameters
	 * 
	 * @return
	 */
	public static <S> S ifNull(S... vals) {
		for (S v : vals)
			if (v != null)
				return v;
		return null;
	}

	/** truncates string with showing it as dots
	 * 
	 * @param s string to truncate
	 * @param max length
	 * @return truncated string, or original if not long enough
	 */
	public static String truncate(String s, int max) {
		if (s == null || s.length() < max || max < 4)
			return s;
		
		return s.substring(0, max-4) + "...";
	}

	// TODO add mode to manage of cutting begining, end of middle and perhaps for UI 
	public static String ellipsis(String s, int max, int mode) {
              return truncate(s, max);
	}
	
	/** represents long value as human readable string with units
	 * 
	 * @param bytes
	 * @return value ending by unit as KB,MB,GB, or TB
	 */
	public static String toStringInUnits(long bytes) {
		int u = 0;
		if (bytes < 1024) {
			bytes <<= 10;
		} else {
			for (u=1;bytes > 1024*1024; bytes >>= 10) u++;
		}
		return String.format("%.1f %cB", bytes/1024f, " KMGTPE".charAt(u));
	}
	
	/** removes enclosing quotes (double)
	 * 
	 * @param s
	 * @return
	 */
	public static String unquote(String s) {
		if (s.startsWith("\""))
			s = s.substring(1);
		if (s.endsWith("\""))
			s = s.substring(0, s.length() - 1);
		return s;
	}
}