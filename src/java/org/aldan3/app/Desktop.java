/* aldan3 - Desktop.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Desktop.java,v 1.2 2008/01/05 06:18:55 dmitriy Exp $                
 *  Created on Feb 8, 2007
 *  @author dmitriy
 */
package org.aldan3.app;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Desktop {
	public Frame getMainFrame() {
		return null;
	}

	public static void showUrl(String url_help) {
		try {
			java.awt.Desktop.getDesktop().browse(new URI(url_help)) ;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean openFile(File file) {
		if (java.awt.Desktop.isDesktopSupported()) {
			try {
				java.awt.Desktop.getDesktop().open(file);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
