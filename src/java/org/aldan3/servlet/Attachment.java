/* aldan3 - Attachment.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Attachment.java,v 1.1 2007/02/05 07:47:35 rogatkin Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.servlet;

import java.io.File;

public class Attachment {
	String type;

	String fileName;

	byte[] data;

	File fileData;

	Attachment(String fileName, String type) {
		this.type = type;
		this.fileName = fileName;
	}

	Attachment(String fileName, String type, byte[] bytes) {
		this(fileName, type);
		data = bytes;
	}

	Attachment(String fileName, String type, File file) {
		this(fileName, type);
		fileData = file;
	}

	void setData(File file) {
		fileData = file;
	}

	void setData(byte[] bytes) {
		data = bytes;
	}

	public String getContentType() {
		return type;
	}

	public String getFileName() {
		return fileName;
	}

	public Object getData() {
		if (data != null)
			return data;
		else
			return fileData;
	}

}
