/* aldan3 - Constant.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Constant.java,v 1.19 2013/02/13 03:22:11 cvs Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.servlet;

public interface Constant {
	/**
	 * servlet config parameter name
	 * 
	 */
	public final static String IP_PROPERTIES = "properties";

	public final static String PRODUCT_NAME = "Aldan 3 full Java tech stack";

	public final static int VERSION_MJ = 1;

	public final static int VERSION_MN = 2;

	public final static String BUILD = "38";

	public static final String COPYRIGHT = "Dmitriy Rogatkin";

	public static final String HOME_URL = "https://github.com/drogatkin/aldan3";

	public static final String ATTR_DEF_TEMPLATE_PROC = "ATTR_DEF_TEMPLATE_PROC";

	public static interface Request {
		public static final String ATTR_USE_FORWARD = "ATTR_USE_FORWARD";

		public static final String INNER_VIEW = "INNER_VIEW";
		
		public static final String REQUESTED_VIEW = "REQUESTED_VIEW";

		public static final String LABEL = "LABEL";

		public static final String MODEL = "MODEL";
		
		public static final String AUTHETICATE_REQ = "AUTHENTICATED_REQUESTER";
	}

	public static interface Property {
		/**
		 * defines package of page service providers classes
		 * 
		 */
		public static final String PAGE_SERV_PACKG = "WorkerPrefix";

		/**
		 * define package name of presentation services utilized them by
		 * front controller for automatic service class name resolution
		 */
		public static final String PRESENT_SERV_PACKG = "PresentServicePackage";

		/**
		 * Defines default instance locale
		 * 
		 */
		public static final String LOCALE = "LOCALE";

		/**
		 * property name for default time zone
		 * 
		 */
		public final static String TIMEZONE = "TIMEZONE";

		public final static String WELCOME_SERVICE = "DefaultServant";

		public final static String MAX_UPLOAD_SIZE = "MaxUploadSize";

		public static final String TEMPLATE_ROOT = "TEMPLATEROOT";

		/**
		 * Defines resources root in any format
		 */
		public static final String RESOURCE_ROOT = "RESOURCEROOT";

		/**
		 * Defines debug flag as true or false
		 */
		public static final String DEBUG = "DEBUG";

		public static final String PERFORMANCE = "PERFORMANCE";
		
		public static final String BASE_PATH = "BASE_PATH";
	}

	public static interface Session {
		public final static String SIGNED = "logged";

		public static final String LOCALE = "LOCALE";

		public final static String TIMEZONE = "TIMEZONE";
	}

	public static interface CharSet {
		public static final String ASCII = "iso-8859-1";

		public static final String UTF8 = "utf-8";
	}

	public static interface Form {
		public static final String SUBMIT = "submit";

		public static final String SUBMIT_X = "submit.x";
	}

	public static interface Variable {
		public static final String UNIQUE_ID = "uniqueid";

		public static final String LABEL = "label";

		public static final String SERVLET = "servlet";

		public static final String SERVLET_PATH = "servletpath";

		public static final String CONTEXT_PATH = "contextpath";

		public static final String SECURE = "secure";

		public static final String SESSION = "$session";

		public static final String ERROR = "error";

		public static final String PAGE_TITLE = "title";

		public static final String SERVLET_INFO = "servletinfo";

		public static final String REFERER = "referer";

		public static final String TRACELOG = "tracelog";
		
		public static final String REQUIRED_MSG = "required_msg";
		
		public static final String DEF_REQUIRED_MSG = "* field '%s' is required"; // TODO localize
		
		public static final String LOCALE = "LOCALE";
	}

	public static interface HTTP {
		public static final String MULTIPARTDATA = "multipart/form-data";

		public static final String BOUNDARY_EQ = "boundary=";

		public static final String BOUNDARY_END_SFX = "--";

		public static final String CONTENT_DISP = "Content-Disposition";

		public static final String FORM_DATA = "form-data";

		public static final String FILENAME_EQ_QT = "filename=\"";

		public static final String NAME_EQ_QT = "name=\"";

		public static final String FILENAME = "filename";

		public static final String CONTENT_TYPE = "Content-Type";

		public static final String REFERER = "referer";

		public static final String USER_AGENT = "User-Agent";

		public static final String AUTHORIZATION = "Authorization";

		public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	}
}