/* aldan3 - FrontController.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: FrontController.java,v 1.22 2015/02/26 21:08:07 cvs Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.servlet;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Implements a front controller design pattern servlet
 * 
 * @author Dmitriy
 *
 */
public class FrontController extends HttpServlet {
	public String servicesPackage = "org.aldan3.servlet.";

	private static final String ID = "Aldan3.FrontController>";

	protected Properties properties;

	private boolean initialized;

	private PageHelpersCache helpersCache;

	private Locale defLocale;

	private TimeZone defTimeZone;

	private String basePath;
	
	private HashSet<Closeable> resources; // TODO move to context

	public void init(ServletConfig _config) throws ServletException {
		super.init(_config);
		if (properties == null) 
			properties = loadProperties(_config.getInitParameter(Constant.IP_PROPERTIES).trim(), _config.getServletContext(), null);
		helpersCache = new PageHelpersCache();
		servicesPackage = properties.getProperty(Constant.Property.PRESENT_SERV_PACKG);
		if (servicesPackage != null) {
			if (servicesPackage.endsWith(".") == false)
				servicesPackage += ".";
		} else
			servicesPackage = properties.getProperty(Constant.Property.PAGE_SERV_PACKG, servicesPackage);
		String localeStr = properties.getProperty(Constant.Property.LOCALE);
		if (localeStr != null) {
			int up = localeStr.indexOf('_');
			if (up > 0) {
				defLocale = new Locale(localeStr.substring(0, up), localeStr.substring(up + 1));
			} else
				defLocale = new Locale(localeStr);
			Locale.setDefault(defLocale);
		} else
			defLocale = Locale.getDefault();
		String timezoneId = properties.getProperty(Constant.Property.TIMEZONE);
		if (timezoneId != null) {
			defTimeZone = TimeZone.getTimeZone(timezoneId);
			if (defTimeZone != null)
				TimeZone.setDefault(defTimeZone);
			else {
				defTimeZone = TimeZone.getDefault();
			}
		}

		basePath = properties.getProperty(Constant.Property.BASE_PATH, "");
		resources = new HashSet<Closeable>();

		initialized = true;
	}
		
	public static Properties loadProperties(String location, ServletContext ctx, ClassLoader cl) {
		if (location == null || location .isEmpty())
			return null;
		Properties result = new Properties();		
		try {
			result.load(ctx.getResourceAsStream(location));
		} catch (Exception e) {
			ctx.log(ID + " " + e + " : Failed reading a config as a resource '" + (location==null?"NULL":location)
					+ "'.");
			try {
				result.load(new FileInputStream(location));
			} catch (Exception e2) {
				ctx.log(ID + " " + e2
						+ ": Failed reading a config as a file.");
				try {
					if (cl == null)
						cl = FrontController.class.getClassLoader();
					result.load(cl.getResourceAsStream(location));
				} catch (Exception e3) {
					result = null;
					ctx.log(ID
							+ " "
							+ e3
							+ ": Failed reading a config as a class path resource.  The application may not function properly.");
				}
			}
		}
		ctx.setAttribute(Constant.ALDAN3_CONFIG, result);
		return result;
	}

	/** Returns servlet
	 * 
	 * @return current servlet
	 */
	public final HttpServlet getServlet() {
		return this;
	}

	/** Returns default locale as string
	 * 
	 * @return default string locale
	 */
	public final String getDefLocaleString() {
		return defLocale.toString();
	}

	/** Returns default locale
	 * 
	 * @return default locale
	 */
	public final Locale getDefLocale() {
		return defLocale;
	}

	/** Returns default time zone
	 * 
	 * @return time zone
	 */
	public final TimeZone getDefTimeZone() {
		return defTimeZone;
	}

	/** servlet service method
	 * 
	 */
	protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException,
			java.io.IOException {
		doRequest(req, res);
	}

	/** main front controller method
	 * 
	 * @param _req request
	 * @param _resp response
	 * @throws IOException 
	 * @throws ServletException
	 */
	public void doRequest(HttpServletRequest _req, HttpServletResponse _resp) throws IOException, ServletException {
		if (initialized == false)
			throw new UnavailableException("Initialization failed");
		String helperName = (String) _req.getAttribute(Constant.Request.REQUESTED_VIEW);
		if (helperName == null)
			helperName = _req.getPathInfo();
		boolean calledAsInclude = false;
		// TODO: called as forward
		if (helperName == null && (calledAsInclude = (_req.getAttribute("javax.servlet.include.request_uri") != null)))
			helperName = (String) _req.getAttribute("javax.servlet.include.path_info");

		String pathInfo = null;
		if ((helperName == null || helperName.length() == 0) && !calledAsInclude) {
			_resp.sendRedirect(_req.getContextPath() + _req.getServletPath() + '/');
			return;
		} else if (helperName.length() == 1 && properties.getProperty(Constant.Property.WELCOME_SERVICE) != null) // '/' expecting
			helperName += properties.getProperty(Constant.Property.WELCOME_SERVICE);
		else {
			//log("Helper name ==========>" + helperName, null);
			// cut possible file name info
			int sp = helperName.indexOf('/', 1);
			if (sp > 0) {
				pathInfo = helperName.substring(sp);
				helperName = helperName.substring(0, sp);
			}
			//log("Helper name <<<<<<<<<<<<<=> " + helperName, null);
		}
		final String servantPathInfo = pathInfo;
		// TODO: if (helperName == null || helperName.length() == null)
		//         helperName = getController().getProperty(Property.DEFAULTHELPER);
		// TODO: add parameter do not use cache and instantiate the class for each
		// request, since the implementation can be not reenterable
		/**
		 * +---------------+-----------------+----------------+
		 * | Thread safe   | Thread friendly | Caching result |
		 * +---------------+-----------------+----------------+
		 * |     yes       |       yes       | no cache, no   |
		 * |               |                 | synchro,       |
		 * |               |                 | one instance   |
		 * +---------------+-----------------+----------------+   
		 * |     yes       |       no        | cache,         |
		 * |               |                 | no synchro,    |
		 * |               |                 | one instance   |
		 * +---------------+-----------------+----------------+   
		 * |      no       |       yes       | cache,         |
		 * |               |                 | synchro,       |
		 * |               |                 | one instance   |
		 * +---------------+-----------------+----------------+   
		 * |      no       |       no        | no cache,      |
		 * |               |                 | synchro added, |
		 * |               |                 | new instance   |
		 * +---------------+-----------------+----------------+   
		 */
		PageService servant = (helperName != null) ? helpersCache.getServant(helperName) : null;
		if (servant != null) {
			final HttpServletRequest pReq = _req;
			_req = (HttpServletRequest) Proxy.newProxyInstance(
					javax.servlet.http.HttpServletRequest.class.getClassLoader(),
					new Class[] { javax.servlet.http.HttpServletRequest.class }, new InvocationHandler() {
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if (method.getName().equals("getPathInfo")) {
								return servantPathInfo;
							} else if (method.getName().equals("getContextPath")) {
								return basePath + pReq.getContextPath();
							}
							try {
								return method.invoke(pReq, args);
							} catch (InvocationTargetException ite) {
								throw ite.getTargetException();
							}
						}
					});

			try {
				if (servant.isThreadSafe())
					servant.serve(_req, _resp, this);
				else {
					synchronized (servant) {
						servant.serve(_req, _resp, this);
					}
				}
			} finally {
				if (!servant.isThreadFriendly() && servant.isThreadSafe())
					helpersCache.releaseServant(helperName, servant);
			}
		} else {
			if (!calledAsInclude)
				_resp.sendError(HttpServletResponse.SC_NOT_FOUND, helperName
						+ " not found or can't be instantiated at request " + _req.getRequestURI());
			else
				_resp.getWriter().write(
						helperName + " not found or can't be instantiated for request " + _req.getRequestURI());
		}
	}

	@Override
	public String getServletInfo() {
		return Constant.PRODUCT_NAME + " version " + Constant.VERSION_MJ + '.' + Constant.VERSION_MN + '/'
				+ Constant.BUILD + "  (C) " + Constant.COPYRIGHT + " " + Constant.HOME_URL;
	}

	/** Servlet destroy method
	 * 
	 */
	@Override
	public void destroy() {
		if (initialized) {
			helpersCache.destroy();
			// TODO can use a closing thread
			for (Closeable c:resources)
				try {
					c.close();
				} catch (Exception e) {
					log(ID + " An exception at closing resource "+c, e);
				}
			resources.clear();
		} else
			log(ID + " Destroy code has been skipped, since the servlet has not been initialized properly.");
	}

	/** Cache for model-controllers
	 * 
	 * @author dmitriy
	 *
	 */
	protected class PageHelpersCache extends HashMap {
		PageHelpersCache() {
		}

		public void destroy() {
			// TODO deactivate active services
			clear();
		}

		PageService getServant(String _name) {
			// normalize name (skipping / and capitalize first letter)
			int shift = _name.charAt(0) == '/' ?1:0;
			if (_name.length() == 2)
				_name = _name.substring(shift).toUpperCase();
			else if (_name.length() > 2) {
				_name = _name.substring(shift, shift+1).toUpperCase() + _name.substring(shift+1).toLowerCase();
			}
			PageService result = null;
			LinkedList ll = null;
			Object element = get(_name);

			if (element != null) {
				if (element instanceof LinkedList) {
					ll = (LinkedList) element;
					synchronized (ll) {
						if (ll.size() > 0)
							return (PageService) ll.removeFirst();
					}
				} else if (element instanceof PageService)
					result = (PageService) element;
			}
			if (result != null) {
				return result;
			} else {
				// generate class name
				try {
					synchronized (this) {
						element = get(_name); // to avoid double creation
						if (element != null) {
							if (element instanceof LinkedList) {
								ll = (LinkedList) element;
								synchronized (ll) {
									if (ll.size() > 0)
										return (PageService) ll.removeFirst();
									else
										remove(_name);
								}
							} else if (element instanceof PageService)
								return (PageService) element;
						}
						result = (PageService) Class.forName(servicesPackage + _name, true,
								Thread.currentThread().getContextClassLoader()).newInstance();
						if (result.isThreadFriendly())
							put(_name, result);
					}
					return result;
				} catch (Throwable e) {
					if (e instanceof ThreadDeath)
						throw (ThreadDeath) e;
					log(ID + "Can't instantiate class '" + servicesPackage + _name + "'. Reason: " + e);
					new Exception("TRACE").printStackTrace();
				}
				return null;
			}
		}

		/** returns a servant to servant objects pool
		 * 
		 * @param _name
		 * @param _servant
		 */
		void releaseServant(String _name, PageService _servant) {
			_servant.reset();
			if (_name.length() == 2)
				_name = _name.substring(1).toUpperCase();
			else if (_name.length() > 2) {
				_name = _name.substring(1, 2).toUpperCase() + _name.substring(2).toLowerCase();
			}
			LinkedList ll = null;
			try {
				synchronized (this) {
					ll = (LinkedList) get(_name);
					if (ll == null) {
						ll = new LinkedList();
						put(_name, ll);
					}
				}
			} catch (ClassCastException cce) {
				log(ID + " Invalid type of a cache element for released elements. " + cce);
			}
			synchronized (ll) {
				ll.addFirst(_servant);
			}
		}
	}

	/** Access to app properties file
	 * 
	 * @return properties defined in config under IP_PROPERTIES
	 */
	public final Properties getProperties() {
		return properties;
	}

	/** Convenience method to read a property
	 * 
	 * @param _propName name
	 * @return property value
	 */
	public final String getProperty(String _propName) {
		return getProperty(_propName, null);
	}

	/** Gets property value with default
	 * 
	 * @param _propName property name
	 * @param _defValue default value
	 * @return property value or default value if property name not found
	 */
	public final String getProperty(String _propName, String _defValue) {
		return properties.getProperty(_propName, _defValue);
	}

	/** Access to servlet context attributes
	 * 
	 * @param _attrName attribute name
	 * @return attribute value
	 */
	public final Object getAttribute(String _attrName) {
		return getServletConfig().getServletContext().getAttribute(_attrName);
	}
	
	/** register a resource to be closed at destroy 
	 * 
	 * @param closeable resource, can't be bull
	 */
	public final void registerResource(Closeable closeable) {
		synchronized(resources) {
		if (resources.contains(closeable) == false)
			resources.add(closeable);
		}
	}
	
	/** unregister resource for closing at destroy
	 * 
	 * @param closeable resource
	 */
	public final void unregisterResource(Closeable closeable) {
		synchronized(resources) {
			resources.remove(closeable);
		}
	}
}
