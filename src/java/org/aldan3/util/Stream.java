/* aldan3 - Stream.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Stream.java,v 1.9 2013/05/01 03:23:45 cvs Exp $                
 *  Created on Feb 8, 2007
 *  @author dmitriy
 */
package org.aldan3.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;

public class Stream {
	private final static int DEFAULT_BUF_SIZE = 8192;
	
	private static int BUF_SIZE = DEFAULT_BUF_SIZE;

	public static void setCopyBufferSize(int newSize) {
		if (newSize > DEFAULT_BUF_SIZE)
			BUF_SIZE = newSize;
		else
			BUF_SIZE = DEFAULT_BUF_SIZE;
	}

	public static String streamToString(InputStream is, String encoding,
			int maxLength) throws IOException {
		if (is == null)
			throw new NullPointerException("Specified stream is null.");
		StringBuffer result = new StringBuffer(100);
		byte[] buffer = new byte[BUF_SIZE];
		int len;
		if (encoding == null)
			while ((len = is.read(buffer)) > 0) {
				result.append(new String(buffer, 0, len));
				if (maxLength > 0 && result.length() > maxLength)
					break;

			}
		else {
			byte[] bres = null;
			while ((len = is.read(buffer)) > 0) {
				if (bres == null)
					bres = new byte[len];
				else
					bres = Arrays.copyOf(bres, bres.length + len);
				System.arraycopy(buffer, 0, bres, bres.length - len, len);
				if (maxLength > 0 && bres.length > maxLength)
					break;
			}
			if (bres != null)
				try {
					return new String(bres, encoding);
				} catch (UnsupportedEncodingException uee) {
					throw new IOException("Unsupported encoding:" + encoding,
							uee);
				}
		}
		return result.toString();
	}

	public static int streamToBytes(byte[] val, InputStream is) throws IOException {
		int cp = 0;
		int l;
		while ((l = is.read(val, cp, val.length-cp)) > 0) {
			cp += l;
		}
		return cp;
	}
	
	public static byte[] streamToBytes(InputStream is, int maxLength) throws IOException {
		byte[] result = new byte[0];
		byte[] buffer = new byte[BUF_SIZE];
		int l;
		while ((l = is.read(buffer)) > 0) {
			if (maxLength > 0 && result.length + l > maxLength)
				throw new IOException("Length exceeded:" + maxLength);
			byte[] tempBuffer = new byte[result.length + l];
			if (result.length > 0)
				System.arraycopy(result, 0, tempBuffer, 0, result.length);
			System.arraycopy(buffer, 0, tempBuffer, result.length, l);
			result = tempBuffer;
		}
		return result;
	}

	public static long copyStream(InputStream is, OutputStream os, long maxLen) throws IOException {
		byte[] buffer = new byte[maxLen > 0 && maxLen < BUF_SIZE ? (int) maxLen : BUF_SIZE];
		int len = buffer.length;
		long result = 0;
		while ((len = is.read(buffer, 0, len)) > 0) {
			os.write(buffer, 0, len);
			result += len;
			if (maxLen > 0) {
				if (result >= maxLen)
					break;
				len = Math.min((int) (maxLen - result), buffer.length);
			} else
				len = buffer.length;
		}
		return result;
	}

	public static long copyStream(InputStream is, OutputStream os) throws IOException {
		return copyStream(is, os, 0);
	}

	public static long copyFile(File in, OutputStream os) throws IOException {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(in), BUF_SIZE);
		try {
			return copyStream(is, os);
		} finally {
			if (is != null)
				is.close();
		}
	}

	public static long copyFile(File in, File out) throws IOException {
		if (!in.equals(out)) {
			BufferedOutputStream bos = null;
			try {
				long result = copyFile(in, bos = new BufferedOutputStream(new FileOutputStream(out), BUF_SIZE));
				bos.flush();
				return result;
			} finally {
				if (bos != null)
					bos.close();
			}
		} else
			throw new IOException("An attempt to copy file " + in + " to itself.");
	}

	public static long copyStream(Reader r, Writer w, long maxLen) throws IOException {
		char[] buffer = new char[maxLen > 0 && maxLen < BUF_SIZE ? (int) maxLen : BUF_SIZE];
		int len = buffer.length;
		long result = 0;
		while ((len = r.read(buffer, 0, len)) > 0) {
			w.write(buffer, 0, len);
			result += len;
			if (maxLen > 0) {
				if (result >= maxLen)
					break;
				len = Math.min((int) (maxLen - result), buffer.length);
			} else
				len = buffer.length;
		}
		return result;
	}

}
