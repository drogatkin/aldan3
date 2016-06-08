/* aldan3 - Main.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Main.java,v 1.15 2012/06/25 02:46:49 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.servlet;

import java.io.File;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.aldan3.model.Log;
import org.aldan3.util.ResourceManager;
import org.aldan3.util.TemplateEngine;
import org.aldan3.util.ResourceManager.ResourceType;

public class Main extends FrontController {
	/**
	 * Servlet init method
	 * <p>
	 * Override this method for particular application <code>
	 * public void init(ServletConfig _config) throws ServletException {
	 *     super.init(_config);
	 *     // read business objects from config and setup in attributes
	 *     // ...
	 * }
	 * </code>
	 * 
	 * @param servlet
	 *            config
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		boolean debug = "1".equals(getProperty(Constant.Property.DEBUG));
		// NOTE currently hierarchy of res managers is out of support
		initResourceManager(ResourceManager.CHARARRAY_RES, getProperty(Constant.Property.TEMPLATE_ROOT), config.getServletContext(), "org.aldan3.resource");
		getResourceManager(ResourceManager.CHARARRAY_RES).setCheckLastModified(debug);
		initResourceManager(ResourceManager.RESOURCE_RES, getProperty(Constant.Property.RESOURCE_ROOT), config.getServletContext(), "org/aldan3/resource");
		getResourceManager(ResourceManager.RESOURCE_RES).setCheckLastModified(debug);
		synchronized(Log.class) {
			Log.l = setDebug(debug);
		}
		ResourceManager.setLogger(Log.l);
		if (getAttribute(getTemplateEngineAttributeName()) == null) {
			TemplateEngine te = new TemplateEngine();
			te.init(getResourceManager(ResourceManager.CHARARRAY_RES), Log.l,
					getSharedDataIdent());
			getServletContext().setAttribute(getTemplateEngineAttributeName(), te);
		}
	}

	/**
	 * Changes debug mode
	 * 
	 * @param true
	 *            then debug
	 */
	protected Log setDebug(boolean _on) {
		TemplateEngine.__debug = _on;
		if (_on == false) {
			return new Log() {
				@Override
				public void log(String severity, String where, String message, Throwable t, Object... details) {
				}
			};
		}
		return new Log() {
			@Override
			public void log(String severity, String where, String message, Throwable t, Object... details) {
				if (details != null && details.length > 0)
					try {
						message = String.format(message, details);
					} catch (Exception e) {
						message = "Error in formatting " + message + ", " + e;
					}
				Main.this.log(String.format("%s %s", severity, message), t);
			}
		};
	}
	
	public String getSharedDataIdent() {
		return null;
	}
	
	public String getTemplateEngineAttributeName() {
		return getServletName()+'_'+Constant.ATTR_DEF_TEMPLATE_PROC;
	}
	
	protected ResourceManager getResourceManager(ResourceType type) {
		return (ResourceManager) getAttribute(getResourceId(type));
	}

	protected ResourceManager getResourceManager1(ResourceType type) {
		return resources.get(type);
	}

	private void initResourceManager(ResourceType type, String resourceRoot, ServletContext ctx, String libRoot) {
		ResourceManager resourceManager = ResourceManager.createResourceManager(type, null);
		resourceManager.setFileSearchPaths(resourceRoot);
		resourceManager.setServletContext(ctx);
		resourceManager.setResourcePackages(resourceRoot + File.pathSeparator + libRoot);
		resourceManager.setServletContextLocations(resourceRoot + File.pathSeparator + libRoot);
		ctx.setAttribute(getResourceId(type), resourceManager);
		resources.put(type, resourceManager);
	}
	
	private String getResourceId(ResourceType type) {
		return getResourceId(getServletName(), type);		
	}
	
	public static String getResourceId(String servletName, ResourceType type) {
		return servletName+"_resource_"+type;
	}
	
	@Override
	public void destroy() {
		super.destroy();
		resources.clear();
	}
	
	private HashMap<ResourceType,ResourceManager> resources = new HashMap<ResourceType,ResourceManager> (2); 
}