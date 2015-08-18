/* aldan3 - StringResourceHolder.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: StringResourceHolder.java,v 1.3 2009/09/21 05:46:35 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.util;
import java.io.InputStream;
import java.io.IOException;

/** Resource holder for strings
 * 
 * @author dmitriy
 *
 */
public class StringResourceHolder extends ResourceHolder {
	
	protected Object getResourceFromStream(InputStream is, String _encoding) throws IOException {
		byte []workBuf = new byte[2048*6];
		byte[]data = new byte[0];
		int cl;
		do {
			// TODO: consider reading directly to expanded array
			// to avoid an extra array copy
			cl = is.read(workBuf);
			if (cl < 0)
				break;
			byte []wa = new byte[data.length+cl];
			System.arraycopy(data, 0, wa, 0, data.length);
			System.arraycopy(workBuf, 0, wa, data.length, cl);
			data = wa;
		} while(true);
		if (data.length == 0)
			return "";
		if (_encoding == null)
			_encoding = "iso-8859-1";
		int cutN = 2;
		if ((data[0] & 255) == 0xEF && (data[1] & 255) == 0xBB && (data[2] & 255) == 0xBF) {
			_encoding = "UTF-8";
			cutN = 3;
		} else if ((data[0] & 255) == 0xFE && (data[1] & 255) == 0xFF)
			_encoding = "UTF-16";
		else if ((data[0] & 255) == 0xFF && (data[1] & 255) == 0xFE)
			_encoding = "UNICODE";
		else
			cutN = 0;
		
		return (Object) (cutN > 0?new String(data, cutN, data.length-cutN, _encoding):
								  new String(data, _encoding));
	}
}