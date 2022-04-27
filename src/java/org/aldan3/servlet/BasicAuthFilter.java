/* aldan3 - Attachment.java
 * Copyright (C) 1999-2010 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: BasicAuthFilter.java,v 1.5 2011/09/16 03:41:37 dmitriy Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aldan3.app.Env;
import org.aldan3.util.inet.Base64Codecs;

public class BasicAuthFilter implements Filter {
	public static final String REQUPDATE_ATTR_NAME = "BasicAuthFilter.update.external.config";

	private String user, password, realm;

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hreq = (HttpServletRequest) req;
		String credentials = hreq.getHeader("Authorization");
		if (credentials != null) {
			if (req.getServletContext().getAttribute(REQUPDATE_ATTR_NAME) != null)
				readExtConfig(req.getServletContext());
			credentials = Base64Codecs.base64Decode(
					credentials.substring(credentials.indexOf(' ') + 1),
					Base64Codecs.UTF_8);
			int i = credentials.indexOf(':');
			String u = credentials.substring(0, i);
			String p = credentials.substring(i + 1);
			//System.err.println("us "+user+",p "+password+" uc "+u+", pc "+p );
			if (u.endsWith(user) && password.equals(p)) {
				chain.doFilter(req, resp);
				return;
			}
		}
		HttpServletResponse hresp = (HttpServletResponse) resp;
		hresp.setHeader("WWW-Authenticate", "basic realm=\"" + realm + '"');
		hresp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		user = config.getInitParameter("USER");
		if (user == null) { // use property file
			readExtConfig(config.getServletContext());
		} else {
			password = config.getInitParameter("PASSWORD");
			realm = config.getInitParameter("REALM");
		}
		if (realm == null)
			realm = "Aldan3";
	}

	private void readExtConfig(ServletContext sctx) {
		Properties authInfo = new Properties();
		FileInputStream propertyStream = null;
		try {
			authInfo.load(propertyStream = new FileInputStream(
					getAuthPropertiesFile(sctx.getContextPath())));
			user = authInfo.getProperty("USER");
			password = authInfo.getProperty("PASSWORD");
			realm = authInfo.getProperty("REALM");
		} catch (IOException e) {
			sctx.log("No user specified as parameter and no credentials file",
					e);
		} finally {
			try {
				propertyStream.close();
			} catch(Exception e) {
				
			}
		}
		if (user == null)
			user = "admin";
		if (password == null)
			password = "admin";
	}

	public static final File getAuthPropertiesFile(String contextPath) {
		if (contextPath.startsWith("/"))
			contextPath = contextPath.substring(1);
		String homeDir = null;
		if (Env.isAndroid()) {
			homeDir = System.getProperty("tjws.webappdir");
			if (homeDir != null) {
				homeDir = new File(homeDir).getParent();
			}
		}
		if (homeDir == null)
			homeDir = System.getProperty("user.home");
		return new File(homeDir, ".auth_" + contextPath
				+ ".properties");
	}
}
