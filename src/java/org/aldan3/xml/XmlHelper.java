/* aldan3 - XmlHelper.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: XmlHelper.java,v 1.2 2009/08/04 22:35:39 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.xml;

import org.xml.sax.helpers.DefaultHandler;

public class XmlHelper extends DefaultHandler {

	/** Appends a tag in result buffer
	 * 
	 * @param xmlBuf result buffer (TODO make it Appendable)
	 * @param tag 
	 * @param attrs
	 * @param insertion tag content
	 */
	public static void appendTag(StringBuffer xmlBuf, String tag, String attrs, String insertion) {
		xmlBuf.append('<').append(tag);
		if (attrs != null)
			xmlBuf.append(' ').append(attrs);

		if (insertion != null)
			xmlBuf.append('>').append(insertion).append("</").append(tag).append('>');
		else
			xmlBuf.append("/>");
	}

	/** Appends a tag with name space prefix
	 * 
	 * @param xmlBuf
	 * @param tag
	 * @param attrs
	 * @param insertion
	 * @param prefix
	 */
	public static void appendTag(StringBuffer xmlBuf, String tag, String attrs, String insertion, String prefix) {
		xmlBuf.append('<').append(prefix).append(':').append(tag);
		if (attrs != null)
			xmlBuf.append(' ').append(attrs);

		if (insertion != null)
			xmlBuf.append('>').append(insertion).append("</").append(prefix).append(':').append(tag).append('>');
		else
			xmlBuf.append("/>");
	}

	/** Creates a tag content as a string
	 * 
	 * @param tag
	 * @param attrs
	 * @param insertion
	 * @param prefix
	 * @return tag string presentation
	 */
	public static String getTag(String tag, String attrs, String insertion, String prefix) {
		StringBuffer result = new StringBuffer(200);
		result.append('<').append(prefix).append(':').append(tag);
		if (attrs != null)
			result.append(' ').append(attrs);

		if (insertion != null)
			result.append('>').append(insertion).append("</").append(prefix).append(':').append(tag).append('>');
		else
			result.append("/>");
		return result.toString();
	}

	/** Creates a tag string content
	 * 
	 * @param tag
	 * @param attrs
	 * @param insertion
	 * @return tag string
	 */
	public static String getTag(String tag, String attrs, String insertion) {
		StringBuffer result = new StringBuffer(200);
		result.append('<').append(tag);
		if (attrs != null)
			result.append(' ').append(attrs);

		if (insertion != null)
			result.append('>').append(insertion).append("</").append(tag).append('>');
		else
			result.append("/>");
		return result.toString();
	}

	/** Appends attribute in a tag
	 * 
	 * @param attrStr current attr string
	 * @param name attr name
	 * @param value attr value
	 * @return a new attribute string
	 */
	public static String appendAttr(String attrStr, String name, String value) {
		if (attrStr == null || attrStr.trim().length() == 0)
			return name+"=\""+value+'"';
		return attrStr +" "+name+"=\""+value+'"';
	}
	
	/** Empty implementation
	 * 
	 * @param xmlObject
	 * @param url
	 */
	public static void readObject(XmlExposable xmlObject, String url) {
	}

	/** Returns atandard XML header string in specified encoding
	 * 
	 * @param encoding
	 * @return a string like &lt;?xml version="1.0" encoding="utf-8"?&gt;
	 */
	public static String getXmlHeader(String encoding) {
		return "<?xml version=\"1.0\" "+(encoding!=null?"encoding=\""+encoding+'"':"")+"?>";
	}

}
