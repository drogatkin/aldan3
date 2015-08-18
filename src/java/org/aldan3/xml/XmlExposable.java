/* aldan3 - XmlExposable.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: XmlExposable.java,v 1.2 2009/08/04 22:35:39 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public interface XmlExposable {
	/**
	 * XML presentation of the object
	 * 
	 * @return XML string object presentation
	 */
	public String toXmlString();

	/**
	 * name space prefix provided for convenience
	 * http://xml.org/sax/features/namespace-prefixes
	 * 
	 * @return name space
	 */
	public String getNameSpacePrefix();

	/**
	 * see http://xml.org/sax/features/namespaces
	 * 
	 * @return name space URL
	 */
	public String getNameSpaceUri();

	/**
	 * Returns a handler for SOX XML parsing
	 * 
	 * @param parent
	 *            object handler
	 * @param namespaceURI
	 *            name space
	 * @param localName
	 *            name of parsed tag
	 * @param qName
	 *            name of parsed tag
	 * @param atts
	 * @return a handler if this XMl can be parsed by this object
	 */
	public DefaultHandler getXmlHandler(ContentHandler parent, String namespaceURI, String localName, String qName,
			Attributes atts);

}
