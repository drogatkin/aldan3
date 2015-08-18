/* aldan3 - HexDump.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://aldan3.sourceforge.net to get the latest infromation
 *  about Rogatkin's products.                                                        
 *  $Id: HexDump.java,v 1.1 2007/04/14 07:30:21 rogatkin Exp $                
 *  Created on Apr 12, 2007
 *  @author Dmitriy
 */
package org.aldan3.util;

import java.io.PrintStream;
import java.text.DecimalFormat;

public class HexDump {
	/** Print out hex dump similar to standard
	 * @param <code>PrintStream</code> stream for dump
	 * @param <code>byte[]</code> source byte to dump
	 * @param <code>int</code> start index
	 * @param <code>int</code> length
	 * @param <code>int</code> start offset to print  
	 */
	public static void dumpBuffer(PrintStream ps, byte abyte0[], int i, int j, int k)
	{
		if (i+j >abyte0.length)
			j = abyte0.length - i;
		int l = j / 16;
		for(int i1 = 0; i1 < l; i1++)
		{
			printDecimal(ps, k);
			ps.print(": ");
			for(int j1 = 0; j1 < 8; j1++)
			{
				printHex(ps, abyte0[i + j1]);
				ps.print(" ");
			}
			ps.print("| ");
			for(int j1 = 8; j1 < 16; j1++)
			{
				printHex(ps, abyte0[i + j1]);
				ps.print(" ");
			}

			for(int k1 = 0; k1 < 16; k1++)
			{
				printChar(ps, abyte0[i + k1]);
//				ps.print("  ");
			}

			i += 16;
			k += 16;
			ps.println();
		}
		l = j%16;
		if (l > 0) {
			printDecimal(ps, k);
			ps.print(": ");
			for(int j1 = 0; j1 < 8 && j1 < l; j1++)
			{
				printHex(ps, abyte0[i + j1]);
				ps.print(" ");
			}
			ps.print("| ");
			for(int j1 = 8; j1 < l; j1++)
			{
				printHex(ps, abyte0[i + j1]);
				ps.print(" ");
			}
			// fill tail
			for(int j1 = 0; j1 < 16-l; j1++)
				ps.print("   ");

			for(int k1 = 0; k1 < l; k1++)
			{
				printChar(ps, abyte0[i + k1]);
			}
			ps.println();
		}

	}

	public static final int getIntLE(byte abyte0[], int i)
	{
		int j = abyte0[i] & 0xff;
		int k = abyte0[i + 1] & 0xff;
		int l = abyte0[i + 2] & 0xff;
		int i1 = abyte0[i + 3] & 0xff;
		return i1 << 24 | l << 16 | k << 8 | j;
	}

	public static final short getShortLE(byte abyte0[], int i)
	{
		int j = abyte0[i] & 0xff;
		int k = abyte0[i + 1] & 0xff;
		return (short)(k << 8 | j);
	}

	public static final String getString(byte abyte0[], int i, int j)
	{
		if(j == 0)
			return "<none>";
		j = j / 2 - 1;
		StringBuffer stringbuffer = new StringBuffer(j);
		for(int k = 0; k < j; k++)
		{
			int l = getUnsignedShortLE(abyte0, i);
			stringbuffer.append((char)l);
			i += 2;
		}

		return stringbuffer.toString();
	}

	public static final long getUnsignedIntLE(byte abyte0[], int i)
	{
		long l = abyte0[i] & 0xff;
		long l1 = abyte0[i + 1] & 0xff;
		long l2 = abyte0[i + 2] & 0xff;
		long l3 = abyte0[i + 3] & 0xff;
		return l3 << 24 | l2 << 16 | l1 << 8 | l;
	}

	public static final int getUnsignedShortLE(byte abyte0[], int i)
	{
		int j = abyte0[i] & 0xff;
		int k = abyte0[i + 1] & 0xff;
		return k << 8 | j;
	}

	private static void printChar(PrintStream ps, byte byte0)
	{
		char c = (char)(byte0 & 0xff);
		if(c >= '!' && c <= '~')
		{
			ps.print(' ');
			ps.print(c);
		}
		else
			if(c == 0)
				ps.print("^@");
			else
				if(c < ' ')
				{
					ps.print('^');
					ps.print((char)((65 + c) - 1));
				}
				else
					if(c == ' ')
						ps.print("__");
					else
						ps.print("??");
	}

	private static void printDecimal(PrintStream ps, int i)
	{
		DecimalFormat decimalformat = new DecimalFormat("00000");
		ps.print(decimalformat.format(i));
	}

	private static void printHex(PrintStream ps, byte byte0)
	{
		int i = byte0 & 0xff;
		int j = i / 16;
		int k = i % 16;
		if(j < 10)
			ps.print((char)(48 + j));
		else
			ps.print((char)((97 + j) - 10));
		if(k < 10)
			ps.print((char)(48 + k));
		else
			ps.print((char)((97 + k) - 10));
	}

}
