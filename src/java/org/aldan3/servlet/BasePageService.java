/* aldan3 - BasePageService.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: BasePageService.java,v 1.86 2015/02/26 21:08:07 cvs Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.servlet;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;

import org.aldan3.annot.Behavior;
import org.aldan3.annot.DBField;
import org.aldan3.annot.FormField;
import org.aldan3.data.util.FieldConverter;
import org.aldan3.data.util.FieldValidator;
import org.aldan3.data.util.Filler;
import org.aldan3.model.DataObject;
import org.aldan3.model.Log;
import org.aldan3.model.ProcessException;
import org.aldan3.model.TemplateProcessor;
import org.aldan3.util.ArrayEntryMap;
import org.aldan3.util.DataConv;
import org.aldan3.util.DateTime;
import org.aldan3.util.ResourceException;
import org.aldan3.util.ResourceManager;
import org.aldan3.util.Stream;
import org.aldan3.util.TemplateEngine;
import org.aldan3.util.ResourceManager.ResourceType;

/**
 * This class is base class for all presentation layer services of the library.
 * It is recommended to create intermediate subclass of it for providing common
 * behavior of created application.
 * 
 * @author Dmitriy
 * 
 */
public abstract class BasePageService implements PageService, ResourceManager.LocalizedRequester, Constant {
	protected transient HttpServletRequest req;

	protected transient HttpServletResponse resp;

	protected transient FrontController frontController;

	protected boolean forwarded, included;

	protected PropertyResourceBundle textResource;

	protected ArrayEntryMap multiFormData;

	protected static final Random UNIQUE_GEN = new Random(System.currentTimeMillis());

	protected abstract Object getModel();

	protected abstract Object doControl();

	protected abstract String getViewName();

	protected abstract String getSubmitPage();

	protected abstract String getUnauthorizedPage();

	/**
	 * This method provides core requests processing and called from front
	 * controller.
	 * <p>
	 * A developer doesn't suppose to override the method.
	 */
	public void serve(HttpServletRequest req, HttpServletResponse resp, FrontController frotnController)
			throws IOException, ServletException {
		this.req = req;
		this.resp = resp;
		this.frontController = frotnController;
		included = this.req.getAttribute("javax.servlet.include.request_uri") != null;
		forwarded = req.getAttribute("javax.servlet.forward.request_uri") != null;
		if (useForward())
			this.req.setAttribute(Request.ATTR_USE_FORWARD, Boolean.TRUE);
		textResource = null; // avoid caching from prev use
		String charSet = getCharSet();
		if (charSet == null)
			charSet = Constant.CharSet.ASCII;
		this.req.setCharacterEncoding(charSet);
		multiFormData = fillMultipartData(req, resp); // TODO store as request attribute?
		start();
		String pi = this.req.getPathInfo();
		boolean ajax = isAjax(pi);
		if (isAllowed(isPublic()) == false) {
			if (ajax)
				this.resp.sendError(getNoAccessResponseCode(), "Not allowed or expired");
			else
				redirect(this.req, this.resp, getUnauthorizedPage());
			return;
		}

		// TODO make submit condition customizable - isSubmit()
		String method = req.getMethod();
		boolean submit = "PUT".equals(method) == false && "DELETE".equals(method) == false && DataConv.hasValue(getStringParameterValue(Constant.Form.SUBMIT,
				getStringParameterValue(Constant.Form.SUBMIT_X, null, 0), 0));
		if (forwarded) {
			String query = req.getQueryString();
			if (query == null)
				submit = false;
			else {
				int sp = query.indexOf(Constant.Form.SUBMIT + '=');
				if (sp < 0)
					submit = false;
				else {
					int psp = query.indexOf('&', sp + 1);
					if (psp < 0)
						psp = query.length();
					if (psp - sp <= Constant.Form.SUBMIT.length() + 1)
						submit = false;
				}
			}
		}
		if (submit && isAllowedGet() == false && "POST".equals(method) == false) {
			resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}
		// TemplateEngine.CurrentRequest.setRequest(req);
		PrintWriter w = null;
		try {
			String methodName = null;
			String npi = null;
			if (ajax && pi != null) {
				// find out method pref, and use
				// processXXXCall(), and getXXXViewName()
				int ns = "/ajax".length(), se, il = pi.length();
				// TODO investigate if isAjax() isn't in sync with Ajax path pattern
				methodName = getDefaultAjaxMethodName();
				if (il > ns && methodName != null) {
					if (pi.charAt(ns) == '/')
						ns++;
					if (il > ns) {
						se = pi.indexOf('/', ns/*+1*/);
						if (se > 0) {
							methodName = pi.substring(ns, se);
							if (se < il - 1)
								npi = pi.substring(se + 1);
						} else
							methodName = pi.substring(ns);
					}
				}
			}
			if (methodName != null) {
				final String fnpi = npi;
				this.req = new HttpServletRequestWrapper(req) {

					@Override
					public String getPathInfo() {
						return fnpi;
					}

				};
				try {
					Object respData = applySideEffects(
							getClass().getMethod("process" + methodName + "Call").invoke(this));
					if (respData instanceof Map) {
						w = processView(respData,
								(String) getClass().getMethod("get" + methodName + "ViewName").invoke(this), ajax);
					} else if (respData != null) {
						setContentType("", null); // TODO investigate "application/json"
						w = resp.getWriter();
						w.write(respData.toString());
					}
				} catch (Throwable e) {
					if (e instanceof ThreadDeath)
						throw (ThreadDeath) e;
					if (e instanceof InvocationTargetException)
						e = ((InvocationTargetException) e).getCause();
					log("Problem in Ajax call (%s).", e, getClass().getName());
					resp.sendError(e instanceof NoSuchMethodException ? HttpServletResponse.SC_NOT_FOUND
							: HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
				}
				return;
			} else if (submit) {
				Object controlData = applySideEffects(doControl());
				// TODO isMobileApp
				if (controlData == null && !ajax) { // the form is Ok
					redirect(req, resp, getSubmitPage());
				} else {// TODO getErrorView() can be here too
					w = processView(controlData, getViewName(), ajax);
				}
				return;
			}
			w = processView(applySideEffects(getModel()), getViewName(), ajax);
		} catch (Throwable t) {
			if (t instanceof ThreadDeath)
				throw (ThreadDeath) t;
			resp.setStatus(getErrorResponseCode());
			String errorView = getErrorView();
            log("Unexpected error: "+t+", reported to "+errorView,	t);
			if (errorView != null) {
				// an exception can be invisible in Ajax call so duplicate it in
				// log
				if (!resp.isCommitted())
					try {
						resp.reset();						
						w = processView(getErrorInfo(t), errorView, ajax);
					} catch (Exception /*Throwable*/ e) {
						log("The error above hasn't been reported becasue", e);
					}
				else
					log("The error above hasn't been reported becasue result is committed", null);
			} else {
				if (t instanceof IOException)
					throw (IOException) t;
				else
					throw (Error) t;
			}
		} finally {
			if (w != null) {
				if (w.checkError())
					log("An error was reported at response writing.", null);
			}
			finish();
		}
	}

	/** response code for unhandled errors
	 * 
	 * @return
	 */
	protected int getErrorResponseCode() {
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}

	/** response code when access is prohibited
	 * 
	 * @return
	 */
	protected int getNoAccessResponseCode() {
		return HttpServletResponse.SC_FORBIDDEN;
	}

	public void reset() {
		req = null;
		resp = null;
		frontController = null;
		textResource = null;
		multiFormData = null;
	}

	/**
	 * Override the method to have additional model massaging
	 * 
	 * @param modelData
	 *            page data model
	 * @return massaged model and some other side actions can be done
	 */
	protected Object applySideEffects(Object modelData) {
		Locale locale = getLocale();
		if (locale != null)
			resp.setLocale(locale);
		if (canCache() == false) {
			if ("HTTP/1.1".equals(req.getProtocol())) {
				resp.setHeader("Cache-Control", "no-cache");
				resp.addHeader("Cache-Control", "no-store"); // for Firefox 1.5
			} else {
				resp.setHeader("Pragma", "no-cache");
				resp.setIntHeader("Expires", -1);
			}
		} else {
			long lastModified = getLastModified();
			if (lastModified > 0) {
				resp.setDateHeader("Last-modified", lastModified);
				long ifModSince = req.getDateHeader("If-Modified-Since");
				if (ifModSince != -1 && ifModSince >= lastModified) {
					resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return null;
				}
			}
		}
		if ("HEAD".equalsIgnoreCase(req.getMethod())) {
			return null;
		}
		return modelData;
	}

	/**
	 * this method allows to bypass a mechanism of applying a template in case
	 * of returning an error
	 * 
	 * @return true, if return raw model as result
	 */
	protected boolean noTemplate() {
		return false;
	}

	/**
	 * This method generates page content.
	 * 
	 * @param modelData
	 * @param viewName
	 * @param ajaxView
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	protected PrintWriter processView(Object modelData, String viewName, boolean ajaxView)
			throws IOException, ServletException {
		if (modelData == null)
			return null;
		if (viewName == null)
			throw new NullPointerException("View name is null");
		if (noTemplate() && modelData instanceof Map == false) {
				setContentType("", null);
				PrintWriter result = resp.getWriter();
				result.write(modelData.toString());
				return result;
		}
		// TODO provide a canvas way for ajax calls
		String canvas = ajaxView ? null : getCanvasView();
		if (canvas != null && included == false) {
			modelData = addCanvasData(modelData);
			req.setAttribute(Constant.Request.INNER_VIEW, viewName);
			viewName = canvas;
		} else
			req.setAttribute(Constant.Request.REQUESTED_VIEW, viewName);
		if (useTicket())
			addTicket(modelData);
		// assume that it called only when Ajax generated using Map
		if (useLabels() && (!ajaxView || ((Map) modelData).containsKey(Constant.Variable.LABEL) == false)) {
			// no worry about multi threading, since the class isn't thread
			// friendly
			if (textResource == null)
				getResource();
			addLabels(modelData);
		}
		addEnv(modelData, ajaxView);
		TemplateProcessor tp = getTemplateProcessor(viewName);
		if (tp != null) {
			if (resp.isCommitted())  // TODO already check and possibly masks an actual problem
				throw new ServletException(
						"Can't process view, since write operation was committed. <" + req.getRequestURI()+">");
			try {
				TemplateEngine.CurrentRequest.setRequest(req);
				setContentType(viewName, null);
				PrintWriter result = resp.getWriter();
				tp.process(result, viewName, modelData, getProperties(), getLocale(), getTimeZone());
				//log("tmplate processed :"+modelData+" "+viewName, null);
				return result;
			} catch (ProcessException e) {
				throw new ServletException(e);
			} finally {
				TemplateEngine.CurrentRequest.setRequest(null);
			}
		} else {
			// no template			
			if (viewName.startsWith("/") == false)

				log("[%s] Dispatched to include view name %s doesn't lead with '/', it can issue looping in a service of the request.",
						null, Log.WARNING, viewName);
			RequestDispatcher rd = req.getRequestDispatcher(viewName);
			if (rd != null) {
				req.setAttribute(Constant.Request.MODEL, modelData);
				// TODO if req already modified then pathInfo information can be incorrect for new call
				rd.include(req, resp);
				return resp.getWriter();
			} else
				log("[%s] View %s hasn't been found.", null, Log.WARNING, viewName);
		}
		return null;
	}

	/**
	 * sets content type
	 * 
	 * @param viewName
	 *            view name for auto determination
	 * @param contentType
	 *            to override content type
	 */
	protected void setContentType(String viewName, String contentType) {
		if (contentType == null)
			contentType = getContentType(viewName);
		if (contentType != null)
			resp.setContentType(contentType);
		else {
			String charSet = getCharSet();
			if (charSet == null) {
				resp.setContentType("text/html");
			} else
				resp.setContentType("text/html; charset=" + charSet);
		}
	}

	/**
	 * Define a desired content type
	 * 
	 * @param viewName
	 *            can be used for clarifying content type
	 * @return content type, or null then default "text/html" is used
	 */
	protected String getContentType(String viewName) {
		if (noTemplate())
			return "application/json";
		return null;
	}

	/**
	 * Every presentation can be wrapped in some canvas.
	 * 
	 * @return canvas view name
	 */
	protected String getCanvasView() {
		return null;
	}

	/**
	 * Responsible for extending model by canvas specific data
	 * 
	 * @param modelData
	 * @return
	 */
	protected Object addCanvasData(Object modelData) {
		return modelData;
	}

	/**
	 * Indicates if the page has been protected from public view
	 * 
	 * @return
	 */
	public boolean isPublic() {
		return false;
	}

	/**
	 * Defines method of presentation transition, HTTP redirect or servlet
	 * forward
	 * 
	 * @return true if use servlet forward
	 */
	public boolean useForward() {
		return false;
	}

	/**
	 * Defines if the view can be cached by browsers. Useful for one time
	 * generated dynamic views
	 * 
	 * @return
	 */
	protected boolean canCache() {
		return false;
	}

	/**
	 * Requests to generate transaction ticket for a view
	 * 
	 * @return
	 */
	protected boolean useTicket() {
		return false;
	}

	/**
	 * Tells if the view needs resource bundle for labels
	 * 
	 * @return
	 */
	protected boolean useLabels() {
		return true;
	}

	/**
	 * Allows to have custom last modified for more accurate caching with
	 * possibility of flush
	 * 
	 * @return
	 */
	protected long getLastModified() {
		return -1;
	}

	/**
	 * returns if submit requests are allowed with non POST methods
	 * 
	 * @return true if allowed, false otherwise
	 */
	protected boolean isAllowedGet() {
		return true;
	}

	/**
	 * Defines view name used for error pages
	 * 
	 * @return
	 */
	protected String getErrorView() {
		if (isAjax(req.getPathInfo()) || noTemplate()) {
			setContentType(null, "application/json");
			return "unexpectederror-json.htm";
		}
		return "unexpectederror.htm";
	}

	/**
	 * Populates transaction ticket in model
	 * 
	 * @param modelData
	 */
	protected void addTicket(Object modelData) {
		if (modelData instanceof Map) {
			((Map) modelData).put(Constant.Variable.UNIQUE_ID, UNIQUE_GEN.nextInt());
		}
	}

	/**
	 * Adds labels in model
	 * 
	 * @param modelData
	 */
	protected void addLabels(Object modelData) {
		if (textResource == null)
			return;
		if (modelData instanceof Map) {
			((Map) modelData).put(Constant.Variable.LABEL, textResource);
		} else
			req.setAttribute(Constant.Request.LABEL, textResource);
	}

	/**
	 * This method provides model massaging adding common elements
	 * 
	 * @param modelData
	 *            model
	 * @param ajaxView
	 *            specifies if a call happens for Ajax (background)
	 */
	protected void addEnv(Object modelData, boolean ajaxView) {
		if (modelData instanceof Map) {
			Map mm = (Map) modelData;
			mm.put(Constant.Variable.SERVLET, getResourceName());
			mm.put(Constant.Variable.SERVLET_PATH, req.getServletPath());
			mm.put(Constant.Variable.CONTEXT_PATH, req.getContextPath());
			if (req.isSecure())
				mm.put(Constant.Variable.SECURE, Constant.Variable.SECURE);
			HttpSession session = req.getSession(false);
			if (session != null)
				mm.put(Constant.Variable.SESSION, session);
		}
	}

	/**
	 * Gets user locale based on browser settings or user profile
	 * 
	 */
	public Locale getLocale() {
		HttpSession session = req.getSession(false);
		if (session != null) {
			Locale l = (Locale) session.getAttribute(Constant.Session.LOCALE);
			if (l != null)
				return l;
		}
		return req.getLocale(); // or frontController.getDefLocale();
	}

	/**
	 * Gets time zone of view requester
	 * 
	 * @return time zone
	 */
	public TimeZone getTimeZone() {
		HttpSession session = req.getSession(false);
		if (session == null)
			return frontController.getDefTimeZone();
		TimeZone tz = (TimeZone) session.getAttribute(Constant.Session.TIMEZONE);
		if (tz == null)
			return frontController.getDefTimeZone();
		return tz;
	}

	/**
	 * Gets session attribute with default
	 * 
	 * @param attrName
	 * @param defaultValue
	 * @return
	 * @return
	 */
	public <T> T getSessionAttribute(String attrName, T defaultValue) {
		HttpSession session = req.getSession(false);
		if (session == null)
			return defaultValue;
		T result = (T) session.getAttribute(attrName);
		if (result != null)
			return result;
		return defaultValue;
	}

	/**
	 * Gets template processor for views when used
	 * 
	 * @param viewName
	 * @return
	 */
	protected TemplateProcessor getTemplateProcessor(String viewName) {
		Behavior b = getClass().getAnnotation(Behavior.class);
		String viewNamePattern = b != null && b.templatePattern().length() > 0 ? b.templatePattern() : ".*\\.ht.?.?.?";
		if (viewName.matches(viewNamePattern)) {
			if (frontController instanceof Main)
				return (TemplateProcessor) frontController
						.getAttribute(((Main) frontController).getTemplateEngineAttributeName());
			return (TemplateProcessor) frontController.getAttribute(Constant.ATTR_DEF_TEMPLATE_PROC);
		}
		return null;
	}

	/**
	 * Provides smarter access to servlet parameters including multi part, and
	 * request
	 * 
	 * @param _param
	 * @return
	 */
	public String[] getStringParameterValues(String _param) {
		String[] v = req.getParameterValues(_param);
		if (multiFormData != null) {
			Object[] vmp = (Object[]) multiFormData.get(_param);
			if (vmp == null)
				return v;
			else if (v == null) {
				String mvs[] = new String[vmp.length];
				for (int i = 0; i < vmp.length; i++)
					mvs[i] = vmp[i].toString();
				return mvs;
			} else {
				String mvs[] = new String[v.length + vmp.length];
				System.arraycopy(v, 0, mvs, 0, v.length);
				for (int i = 0; i < vmp.length; i++)
					mvs[i + v.length] = vmp[i].toString();
				return mvs;
			}
		}
		return v;
	}

	/**
	 * Convenient method for reaching parameters passed as part of GET, POST or
	 * multi-parts form data
	 * 
	 * @param _param
	 * @param _defResult
	 * @param _index
	 * @param _typeProvisioning
	 * @return
	 */
	public Object getObjectParameterValue(String param, Object _defResult, int _index, boolean _typeProvisioning) {
		// TODO method should be reconsidered as getbinvalue
		//if (_index < 0)
		//return _defResult;
		Object[] v = req.getParameterValues(param);
		if (multiFormData != null) {
			if (v != null) {
				Object[] v1 = (Object[]) multiFormData.get(param);
				if (v1 != null && v1.length > 0) {
					// System.err.println("!!!Merging for name:" + _param);

					if (_index < v.length)
						return v[_index].toString();
					_index -= v.length;
					if (_index < v1.length)
						return v1[_index].toString();
					return _defResult;
				}
			} else
				v = (Object[]) multiFormData.get(param);
		}
		if (v == null || v.length <= _index)
			return _defResult;
		if (_index < 0)
			return v;
		if (_typeProvisioning) {
			if (v[_index] instanceof String) {
				String s = (String) v[_index];
				// if (s.matches(""))
				try {
					return new Long(s);
				} catch (NumberFormatException nf) {
				}
				try {
					return new Double(s);
				} catch (NumberFormatException nf) {
				}
				try {
					return new SimpleDateFormat().parse(s);
				} catch (ParseException e) {
				}
			}
		}
		return v[_index];
	}

	/**
	 * Convenient method for getting String parameter with default
	 * 
	 * @param _param
	 * @param _defResult
	 * @param _index
	 * @return
	 */
	public String getStringParameterValue(String _param, String _defResult, int _index) {
		Object o = getObjectParameterValue(_param, _defResult, _index, false);
		return o == null ? null : o.toString();
	}

	public String getParameterValue(String param, String defResult, int index) {
		return getStringParameterValue(param, defResult, index);
	}

	public String getTrimStringParameterValue(String _param, String _defResult, int _index) {
		String result = getStringParameterValue(_param, _defResult, _index);
		return result != null ? result.trim() : null;
	}

	public int getIntParameterValue(String _param, int _defResult, int _index) {
		try {
			return Integer.parseInt(getStringParameterValue(_param, "" + _defResult, _index));
		} catch (NumberFormatException nfe) {
			return _defResult;
		}
	}

	public int getParameterValue(String param, int defResult, int index) {
		return getIntParameterValue(param, defResult, index);
	}

	public long getLongParameterValue(String _param, long _defResult, int _index) {
		try {
			return Long.parseLong(getTrimStringParameterValue(_param, "" + _defResult, _index));
		} catch (NumberFormatException nfe) {
			return _defResult;
		}
	}

	public long getParameterValue(String param, long defResult, int index) {
		return getLongParameterValue(param, defResult, index);
	}

	public double getDoubleParameterValue(String _param, double _defResult, int _index) {
		try {
			return Double.parseDouble(getTrimStringParameterValue(_param, "" + _defResult, _index));
		} catch (NumberFormatException nfe) {
			return _defResult;
		}
	}

	public double getParameterValue(String param, double defResult, int index) {
		return getDoubleParameterValue(param, defResult, index);
	}

	/**
	 * Fills data object from form data
	 * 
	 * @param model
	 * @return the object with populated values froma request
	 */
	public DataObject fillDataObject(DataObject model, Filler filler) {
		if (model != null) {
			Set<org.aldan3.model.Field> fields = model.getFields();
			for (org.aldan3.model.Field f : fields) {
				if (filler == null)
					model.modifyField(f, getParameterValue(f.getName(), null, 0));
				else
					filler.fill(f);
			}
		}
		return model;
	}

	public <T> T fillModel(T model) {
		return fillModel(model, 0);
	}

	/**
	 * Fills model from form data
	 * 
	 * 
	 * @param model
	 * @param parameters
	 *            set index, works only for models without collection members
	 * @return
	 */
	public <T> T fillModel(T model, int index) {
		if (model == null)
			return null;

		Field[] flds = model.getClass().getFields();
		IllegalArgumentException validationException = null;
		for (Field f : flds) {
			FormField ff = f.getAnnotation(FormField.class);
			if (ff != null && ff.presentType() != FormField.FieldType.Readonly) {

				String name = ff.formFieldName().length() == 0 ? f.getName() : ff.formFieldName();
				FieldValidator validator = null;
				if (ff.validator() != null && ff.validator() != FieldValidator.class)
					try {
						validator = ff.validator().newInstance();
						inject(validator);
					} catch (InstantiationException e) {
						log("Can't create validator %s for %s", null, ff.validator(), f.getName());
					} catch (IllegalAccessException e) {
						log("Can't access validator %s for %s", null, ff.validator(), f.getName());
					}
				FieldConverter convertor = null;
				if (ff.converter() != null && ff.converter() != FieldConverter.class)
					try {
						convertor = ff.converter().newInstance();
						inject(convertor);
					} catch (InstantiationException e) {
						log("Can't create convertor %s for %s", null, ff.converter(), f.getName());
					} catch (IllegalAccessException e) {
						log("Can't access convertor %s for %s", null, ff.converter(), f.getName());
					}
				String normalizeCodes = ff.normalizeCodes().toUpperCase();

				String v = null;
				Class<?> fieldClass = f.getType();
				try {
					if (fieldClass.isArray() || Collection.class.isAssignableFrom(fieldClass)) { // vector
						Object[] va = multiFormData != null ? (Object[]) multiFormData.get(name) : null;

						if (va == null)
							va = req.getParameterValues(name);
						if (va == null || va.length == 0)
							if (ff.required())
								throw new IllegalArgumentException(String.format(
										getResourceString(Variable.REQUIRED_MSG, Variable.DEF_REQUIRED_MSG),
										getResourceString(f.getName(), f.getName())));
							else {
								if (ff.normalizeCodes().indexOf('Z') < 0)
									try {
										f.set(model, null);
									} catch (Exception e) {
										log("Problem in nulling of field %s", e, f.getName());
									}
								continue;
							}
						boolean processed = false;

						Class compType = fieldClass.getComponentType();
						if (compType.isPrimitive()) {
							try {
								if (compType == int.class) {
									int[] ta = new int[va.length];
									for (int i = 0; i < va.length; i++) {
										v = va[i].toString(); // no null
										// elements
										if (validator != null)
											validator.validate(v);
										if (v.length() > 0)
											ta[i] = Integer.parseInt(v);
									}
									if (fieldClass.isArray())
										f.set(model, ta);
									else
										f.set(model, Arrays.asList(ta));
								} else if (compType == long.class) {
									long[] ta = new long[va.length];
									for (int i = 0; i < va.length; i++) {
										v = va[i].toString(); // no null
										// elements
										if (validator != null)
											validator.validate(v);
										if (v.length() > 0)
											ta[i] = Long.parseLong(v);
									}
									if (fieldClass.isArray())
										f.set(model, ta);
									else
										f.set(model, Arrays.asList(ta));
								} else if (compType == double.class) {
									double[] ta = new double[va.length];
									for (int i = 0; i < va.length; i++) {
										v = va[i].toString(); // no null
										// elements
										if (validator != null)
											validator.validate(v);
										if (v.length() > 0)
											ta[i] = Double.parseDouble(v);
									}
									if (fieldClass.isArray())
										f.set(model, ta);
									else
										f.set(model, Arrays.asList(ta));
								} else if (compType == float.class) {
									float[] ta = new float[va.length];
									for (int i = 0; i < va.length; i++) {
										v = va[i].toString(); // no null
										// elements
										if (validator != null)
											validator.validate(v);
										if (v.length() > 0)
											ta[i] = Float.parseFloat(v);
									}
									if (fieldClass.isArray())
										f.set(model, ta);
									else
										f.set(model, Arrays.asList(ta));
								} else if (compType == char.class) {
									log("Warning String to char[]", null);
									if (va.length == 0) {
										v = va[0].toString();
										if (validator != null)
											validator.validate(v);
										f.set(model, v.toCharArray());
									} else {
										char[] ca = new char[va.length];
										for (int i = 0; i < va.length; i++) {
											v = va[i].toString();
											if (validator != null)
												validator.validate(v);
											if (v.length() > 0)
												ca[i] = v.charAt(0);
										}
										f.set(model, ca);
									}
								} else if (compType == byte.class) {
									if(va.length == 1 && multiFormData != null) {
										if (va[0] instanceof byte[])
											f.set(model, va[0]);
										else if (va[0] instanceof File) {
											
										}
									}
								} else
									log("Unsupported type for auto conversion %s of %s", null, compType, f.getName());
							} catch (Exception e) {
								log("Problem in converting to %s of %s for field %s", e, compType, v, f.getName());
							}
							processed = true;
						}

						if (processed == false) {
							if (compType == String.class) {
								if (va.getClass().getComponentType() == String.class) {
									if (validator != null)
										for (int i = 0; i < va.length; i++)
											validator.validate((String) va[i]);
									try {
										if (fieldClass.isArray())
											f.set(model, va);
										else
											f.set(model, Arrays.asList(va));

									} catch (Exception e) {
										log("Problem in setting field %s", e, f.getName());
									}
								} else
									throw new UnsupportedOperationException(
											"Only String type is supported for form elements");
							} else {
								if (convertor == null) {
									log("No convertor available for target type %s of %s", null, compType, f.getName());
								} else {
									Object[] ta = new Object[va.length];
									try {
										for (int i = 0; i < va.length; i++) {
											v = va[i].toString(); // no null
											// elements
											ta[i] = convertor.convert(v, getTimeZone(), getLocale());
											if (validator != null)
												validator.validate(ta[i]);
											// TODO validate length and do other adjustments accordingly normalizeCodes()
										}
										if (fieldClass.isArray())
											f.set(model, ta);
										else
											f.set(model, Arrays.asList(ta));
									} catch (Exception e) {
										log("Problem in converting to %s of %s for field %s", e, compType, v,
												f.getName());
									}
								}
							}
						}
					} else { // scalar
						v = getParameterValue(name, normalizeCodes.indexOf('Z') >= 0 ? null : ff.defaultTo(),
								index > 0 ? index : 0);
						if (normalizeCodes.length() > 0)
							v = normalize(v, normalizeCodes, f.getAnnotation(DBField.class));
						if (v == null)
							continue;
						if (ff.required() && v.length() == 0)
							throw new IllegalArgumentException(
									String.format(getResourceString(Variable.REQUIRED_MSG, Variable.DEF_REQUIRED_MSG),
											getResourceString(f.getName(), f.getName())));
						if (validator != null)
							validator.validate(v);

						if (fieldClass == String.class)
							try {
								f.set(model, v);
							} catch (Exception e) {
								log("Problem in setting field %s", e, f.getName());
							}
						else {							
							if (convertor != null) {								
								try {
									f.set(model, convertor.convert(v, getTimeZone(), getLocale()));
								} catch (Exception e) {
									log("Problem in setting or converting field %s", e, f.getName());
								}
							} else {
								log("A converter can be needed for %s of %s (%s %s)", null, f.getType(), f.getName(),
										fieldClass, v);
								// if (fieldClass.isPrimitive()) {
								try {
									if (v.length() > 0) {
										if (fieldClass == int.class || fieldClass == Integer.class) {
											f.set(model, Integer.parseInt(v));
										} else if (fieldClass == long.class || fieldClass == Long.class) {
											f.set(model, Long.parseLong(v));
										} else if (fieldClass == double.class || fieldClass == Double.class) {
											f.set(model, Double.parseDouble(v));
										} else if (fieldClass == float.class || fieldClass == Float.class) {
											f.set(model, Float.parseFloat(v));
										} else if (fieldClass == boolean.class || fieldClass == Boolean.class) {
											f.set(model, new Boolean(v));
										} else if (fieldClass == char.class) {
											f.set(model, new Character(v.charAt(0)));
										} else if (fieldClass == File.class) {
											
											log("Supporting file is coming soon...", null);
										} else if (fieldClass == Date.class) {
											f.set(model, DateTime.parseJsonDate(v, null));
										} else if (fieldClass.isEnum()) {
											f.set(model, fieldClass.getMethod("valueOf", String.class).invoke(null, v));
										} else
											log("Can't do conversion from %s for %s", null, fieldClass, f.getName());
									} else {
										if (fieldClass == boolean.class || fieldClass == Boolean.class)
											f.set(model, false);
										else if (fieldClass == char.class)
											f.set(model, new Character((char) 0));
										else if (fieldClass == File.class)
											f.set(model, null);
										else if (fieldClass.isEnum())
											f.set(model, null);
										else if (fieldClass.isAssignableFrom(Number.class) || fieldClass == int.class || fieldClass == long.class)
											f.set(model, 0);
										else if (fieldClass == float.class || fieldClass == double.class)
											f.set(model, 0.0);
										else
											f.set(model, null);
									}
								} catch (Exception e) {
									log("Problem in converting to %s of %s for field %s", e, fieldClass, v,
											f.getName());
								}
							}
						}
					}
				} catch (IllegalArgumentException ve) {
					if (reportValidation(f.getName(), v, ve)) {
						if (ff.validationMessageTarget().length() > 0) {
							// TODO: ve.getLocalizedMessage()
							try {
								model.getClass().getField(ff.validationMessageTarget()).set(model, ve.getMessage());
							} catch (Exception e) {
								throw new RuntimeException("Invalid annotation ref:", e);
							}
						} else {
							if (validationException == null)
								validationException = ve;
							else
								validationException = (IllegalArgumentException) ve.initCause(validationException);
						}
					}
				}
			}
		}
		if (validationException != null)
			throw validationException;
		return model;
	}

	/**
	 * injects model and other references to objects like validator
	 * 
	 * @param obj
	 *            usually just created object with Inject annotations
	 */
	public void inject(Object obj) {

	}

	/**
	 * normalize string accordingly normalization codes, all codes get applied
	 * unless controversial
	 * 
	 * @param v
	 * @param normalizeCodes
	 * @return normalized string T/t trims extra white spaces from both ends<br>
	 *         U/u convert to upper case<br>
	 *         L/l - convert to lower case<br>
	 *         C/c - convert to title case, all first letters of words get
	 *         capital, however rest letters remain the same
	 */
	protected String normalize(String v, String normalizeCodes, DBField dbf) {
		if (normalizeCodes == null || normalizeCodes.length() == 0 || v == null || v.length() == 0)
			return v;
		if (normalizeCodes.indexOf('T') >= 0)
			v = v.trim();
		if (normalizeCodes.indexOf('E') >= 0 && dbf.size() > 4)
			v = DataConv.ellipsis(v, dbf.size(), 0);
		if (normalizeCodes.indexOf('U') >= 0)
			v = v.toUpperCase(getLocale());
		else {
			if (normalizeCodes.indexOf('L') >= 0)
				v = v.toLowerCase(getLocale());
			if (normalizeCodes.indexOf('C') >= 0 && v.length() > 0) {
				char[] cv = v.toCharArray();
				cv[0] = Character.toTitleCase(cv[0]);
				boolean wasBl = false;
				for (int ci = 1; ci < cv.length; ci++) {
					if (cv[ci] == ' ')
						wasBl = true;
					else {
						if (wasBl) {
							cv[ci] = Character.toTitleCase(cv[ci]);
							wasBl = false;
						}
					}
				}
				v = new String(cv);
			}
		}
		return v;
	}

	/**
	 * override the method for getting validation exceptions <br>
	 * Returning false allows to bypass further validation reporting
	 * 
	 * @param name
	 * @param value
	 * @param problem
	 * @return
	 */
	protected boolean reportValidation(String name, String value, Exception problem) {
		return true;
	}

	public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
		if (path == null || path.length() == 0) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else if ("http://".regionMatches(true, 0, path, 0, "http://".length())
				|| "https://".regionMatches(true, 0, path, 0, "https://".length())) // TODO consider using pattern
			resp.sendRedirect(encodeCRLF(path));
		else {
			if (req.getAttribute(Constant.Request.ATTR_USE_FORWARD) != null)
				try {
					req.getRequestDispatcher(path).forward(req, resp);
					return;
				} catch (NullPointerException npe) {
					System.err.println("Can't locate servletfor forwarding to path: " + path);
					resp.sendError(resp.SC_INTERNAL_SERVER_ERROR, "Can't locate servlet for forwarding to path" + path);
					return;
				} catch (ServletException e) {
					// correct way is transfer in IOException
				}
			if (path.length() > 0 && path.charAt(0) == '/')
				resp.sendRedirect(encodeCRLF(path));
			else
				resp.sendRedirect(req.getContextPath() + req.getServletPath() + '/' + encodeCRLF(path));
		}
	}

	public static String encodeCRLF(String url) {
		return url.replaceAll("\r", "%0d").replaceAll("\n", "%0a");
	}

	/**
	 * Returns default charset
	 * 
	 * @return default char set used for page generation The method has to be
	 *         overridden on level of application. It returns null.
	 */
	protected String getCharSet() {
		// charset
		// from
		// session
		// Accept-Language: da, en-gb;q=0.8, en;q=0.7
		// Accept-Charset: iso-8859-5, unicode-1-1;q=0.8
		return null;
	}

	/**
	 * returns encoding provided in request
	 * 
	 */
	public String getEncoding() {
		String contentType = req.getContentType();
		if (contentType != null)
			contentType = contentType.toLowerCase();
		else
			return null;
		return req.getCharacterEncoding();
	}

	/**
	 * An internal method checks if view requester signed in
	 * 
	 * @param override
	 *            allows to override result
	 * @return true if a requester signed in, or overridden
	 * @exception ServletException
	 *                gives extra flexibility to control flow in subclasses
	 */
	protected boolean isAllowed(boolean override) throws ServletException {
		if (override)
			return true;

		HttpSession session = req.getSession(false);
		if (session == null)
			return false;

		// Note no reason for session.isNew() == false
		return req.getServletPath().equalsIgnoreCase((String) session.getAttribute(Constant.Session.SIGNED));
	}

	/**
	 * allows to customize request processing based on assumption it is Ajax
	 * call
	 * 
	 * @param pathInfo
	 * @return true if a request can be categorized by Ajax by criteria
	 */
	protected boolean isAjax(String pathInfo) {
		return pathInfo != null && pathInfo.startsWith("/ajax");
	}

	/**
	 * Allows to redefine default Ajax handler
	 * 
	 * @return default Ajax handler name
	 */
	protected String getDefaultAjaxMethodName() {
		return "Ajax";
	}

	/**
	 * gives control to initialize state of service before using it
	 * 
	 */
	protected void start() {

	}

	/**
	 * gives control to finalize state of service after using it
	 * 
	 */
	protected void finish() {

	}

	protected void setAllowed(boolean on) {
		if (on) {
			HttpSession session = req.getSession();
			session.setAttribute(Constant.Session.SIGNED, req.getServletPath());
		} else {
			HttpSession session = req.getSession(false);
			if (session != null)
				session.removeAttribute(Constant.Session.SIGNED);
		}
	}

	public String getResourceName() {
		String result = getClass().getName();
		int p = result.lastIndexOf('.');
		if (p > 0)
			return result.substring(p + 1).toLowerCase();
		return result.toLowerCase();
	}

	public String getResourceString(String _key, String _defaultValue) {
		String result = null;
		if (useLabels()) {
			if (textResource == null)
				getResource();
			if (textResource != null)
				try {
					result = textResource.getString(_key);
				} catch (java.util.MissingResourceException mre) {
				}
		}
		if (result == null || result.length() == 0)
			return _defaultValue;
		return result;
	}

	public ResourceBundle getResource() {
		if (textResource == null) {
			synchronized (this) {
				if (textResource == null)
					try {
						textResource = (PropertyResourceBundle) getResourceManager(ResourceManager.RESOURCE_RES)
								.getResource(getResourceName(), this);
					} catch (ResourceException e) {
						log(String.format("Can't read resources %s in locale %s", getResourceName(), getLocale()), e);
					}
			}
		}
		return textResource;
	}

	public ResourceBundle getLocalizedResource(final Locale _locale) {
		try {
			return (PropertyResourceBundle) getResourceManager(ResourceManager.RESOURCE_RES)
					.getResource(getResourceName(), new ResourceManager.LocalizedRequester() {

						public Locale getLocale() {
							return _locale;
						}

						public String getEncoding() {
							return BasePageService.this.getEncoding();
						}

						public TimeZone getTimeZone() {
							return BasePageService.this.getTimeZone();
						}
					});
		} catch (ResourceException e) {
			log(String.format("Can't read resources in locale %s", _locale), e);
		}
		return getResource();
	}

	public Properties getProperties() {
		return frontController.getProperties();
	}

	public String getHeader(String name) {
		return req.getHeader(name);
	}

	public ResourceManager getResourceManager(ResourceType type) {
		if (frontController instanceof Main)
			return ((Main) frontController).getResourceManager(type);
		return ResourceManager.getResourceManager(type);
	}

	public void log(String message, Throwable t, Object... details) {
		if (details != null && details.length > 0)
			try {
				message = String.format(message, details);
			} catch (Exception e) {
				message = "The message " + message + " couldn't be formatted properly, 'cause " + e;
			}
		frontController.log(message, t);
	}

	protected Map<String, Object> getErrorInfo(Throwable t) {
		Map<String, Object> result = new HashMap<String, Object>(4);
		result.put(Constant.Variable.ERROR, t);
		result.put(Constant.Variable.SERVLET_INFO, frontController.getServletInfo());
		result.put(Constant.Variable.SERVLET, toString());
		result.put(Constant.Variable.REFERER, req.getHeader(Constant.HTTP.REFERER));
		CharArrayWriter caw;
		t.printStackTrace(new PrintWriter(caw = new CharArrayWriter(), true));
		result.put(Constant.Variable.TRACELOG, caw.toString());
		return result;
	}

	protected ArrayEntryMap fillMultipartData(HttpServletRequest _req, HttpServletResponse _resp) throws IOException {
		// TODO this method requires major redesign, providing more powerful
		// mechanism for attachments, like returning class Attachment {
		// getContentType() getDataType getData  getFile
		//  it has to be also in a separate class
		String contentType = _req.getContentType();
		if (contentType == null || contentType.toLowerCase().indexOf(Constant.HTTP.MULTIPARTDATA) < 0)
			return null;
		String enc = req.getCharacterEncoding();
		if (enc == null)
			enc = Constant.CharSet.ASCII;
		ArrayEntryMap result = new ArrayEntryMap();
		int maxMemPartUse = 2 * 1024 * 1024;
		try {
			for (Part p : _req.getParts()) {
				String name = p.getName();
				String disp = p.getHeader(Constant.HTTP.CONTENT_DISP);
				if (disp.indexOf(Constant.HTTP.FORM_DATA) >= 0) {
					InputStream is = null;
					boolean free = true;
					try {
						int fp = disp.indexOf(Constant.HTTP.FILENAME_EQ_QT);
						if (fp >= 0) {
							int ef = disp.indexOf('"', fp + Constant.HTTP.FILENAME_EQ_QT.length());
							if (ef < 0) {
								log("Broken file path syntax in " + disp + ", the part is skipped", null);
								continue;
							}
							String filePath = disp.substring(fp + Constant.HTTP.FILENAME_EQ_QT.length(), ef);
							result.put(name + '+' + Constant.HTTP.FILENAME, filePath);
							result.put(name + '+' + Constant.HTTP.CONTENT_TYPE,
									p.getHeader(Constant.HTTP.CONTENT_TYPE));
							//Attachment attachment = new Attachment(filePath, p.getHeader(Constant.HTTP.CONTENT_TYPE));
							//result.put(name, attachment);
							long fileSize = p.getSize();
							if (fileSize == 0) {
								InputStream cis = null;
								FileOutputStream fos = null;
								try {
									URLConnection uc = new URL(filePath).openConnection();
									cis = uc.getInputStream();
									fileSize = uc.getContentLength();
									if (fileSize < 0)
										try {
											//fileSize = uc.getContentLengthLong();
											fileSize = Long.parseLong(uc.getHeaderField("CONTENT-LENGTH"));
										} catch (Exception e) {

										}
									result.put(name + '+' + Constant.HTTP.CONTENT_TYPE, uc.getContentType());
									if (fileSize > 0 && fileSize < maxMemPartUse) {
										byte[] val = new byte[(int) fileSize];
										Stream.streamToBytes(val, cis);
										result.put(name, val);
									} else {
										File uploadFile = File.createTempFile(name, "aldan3-attach");
										uploadFile.deleteOnExit();
										Stream.copyStream(cis, fos = new FileOutputStream(uploadFile));
										result.put(name, uploadFile);
									}
								} catch (MalformedURLException mfe) {

								} finally {
									if (fos != null)
										try {
											fos.close();
										} catch (IOException ioe) {

										}
									if (cis != null)
										try {
											cis.close();
										} catch (IOException ioe) {

										}
								}
							} else if (fileSize < maxMemPartUse) { // TODO configurable					
								byte[] val = new byte[(int) p.getSize()];
								Stream.streamToBytes(val, is = p.getInputStream());
								result.put(name, val);
								//attachment.setData(val);
							} else {
								File uploadFile = File.createTempFile(name, "aldan3-attach");
								if (uploadFile.delete()) // can't overwrite
									p.write(uploadFile.getPath());
								uploadFile.deleteOnExit();
								result.put(name, uploadFile);
								//attachment.setData(uploadFile);
								free = false;
							}
						} else {
							result.put(name, Stream.streamToString(is = p.getInputStream(), enc, 0));
						}
					} finally {
						if (is != null)
							try {
								is.close();
							} catch (Exception e) {
							}
						if (free)
							try {
								p.delete();
							} catch (Exception e) {
							}
					}
				}
			}
			return result;
		} catch (Error e) {
			log("Aldan3 implementation of upload processing will be applied since:" + e, null);
		} catch (Exception e) {
			log("Aldan3 implementation of upload processing will be applied since:" + e, null);
		}
		//log("processing multiparts", null);
		ServletInputStream sis = null;
		try {
			sis = _req.getInputStream();
		} catch (IllegalStateException ise) {
			throw new IOException("Input stream is unaccessible");
		}
		int bp = contentType.indexOf(Constant.HTTP.BOUNDARY_EQ);
		if (bp < 0) {
			log("No boundary found for multipart form data %s", null, contentType);
			return null;
		}
		String boundary = contentType.substring(bp + Constant.HTTP.BOUNDARY_EQ.length()); // it
		// can be not last attribute
		int boundaryLength = boundary.length();
		int contentLength = _req.getContentLength();
		if (contentLength <= 0)
			return null;
		// TODO: move this code to Dispatcher
		int maxReqLength = 30 * 1024 * 1024;
		try {
			maxReqLength = Integer.parseInt(frontController.getProperty(Constant.Property.MAX_UPLOAD_SIZE));
		} catch (Exception nfe) {
		}
		if (contentLength > maxReqLength) {
			log("Upload size " + contentLength + " exceeds max allowed: " + maxReqLength
					+ ", specify or correct max size property: " + Constant.Property.MAX_UPLOAD_SIZE, null);
			sis.skip(contentLength);
			return null;
		}
		// TODO: do not allocate buffer for all content length, just keep
		// reading
		byte[] buffer = null;
		try {
			buffer = new byte[contentLength];
		} catch (OutOfMemoryError oum) {
			log("Can't allocate a buffer of " + contentLength
					+ " bytes, increase a heap size or review section <multipart-config> of web.xml", oum);
			sis.skip(contentLength);
			return null;
		}
		int contentRead = 0;
		main_loop: do {
			if (contentRead > contentLength)
				break main_loop;
			// read --------------boundary
			int ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
			if (ec < 0)
				break main_loop;
			String s = new String(buffer, contentRead, ec, enc);
			contentRead += ec;
			int p = s.indexOf(boundary);
			if (p >= 0) {
				if (s.regionMatches(p + boundaryLength, Constant.HTTP.BOUNDARY_END_SFX, 0,
						Constant.HTTP.BOUNDARY_END_SFX.length()))
					break; // it shouldn't happen here, but it's Ok
				// skip the boundary, if it happens, because it's first
				ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
				s = new String(buffer, contentRead, ec, enc);
				contentRead += ec;
			}
			// s contains here first line of a part
			int dp, ep;
			String header, name = null, filename = null, token, partContentType = null;
			do {
				dp = s.indexOf(':');
				if (dp < 0) // throw new IOException( ..
					break main_loop;

				header = s.substring(0, dp);
				s = s.substring(dp + 2);
				if (Constant.HTTP.CONTENT_DISP.equalsIgnoreCase(header)) {
					StringTokenizer ast = new StringTokenizer(s, ";");
					if (ast.hasMoreTokens()) {
						token = ast.nextToken();
						if (token.indexOf(Constant.HTTP.FORM_DATA) < 0)
							break main_loop; // throw new IOException( ..

						while (ast.hasMoreTokens()) {
							token = ast.nextToken();
							dp = token.indexOf(Constant.HTTP.FILENAME_EQ_QT);
							if (dp >= 0) {
								ep = token.indexOf('"', dp + Constant.HTTP.FILENAME_EQ_QT.length());
								if (ep < 0 || filename != null)
									break main_loop;
								filename = token.substring(dp + Constant.HTTP.FILENAME_EQ_QT.length(), ep);
								continue;
							}
							dp = token.indexOf(Constant.HTTP.NAME_EQ_QT);
							if (dp >= 0) {
								ep = token.indexOf('"', dp + Constant.HTTP.NAME_EQ_QT.length());
								if (ep < 0 || ep == dp + Constant.HTTP.NAME_EQ_QT.length() || name != null)
									break main_loop; // throw new
								// IOException( ..
								name = token.substring(dp + Constant.HTTP.NAME_EQ_QT.length(), ep);
								continue;
							}
						}
					}
					if (filename != null)
						result.put(name + '+' + Constant.HTTP.FILENAME, filename);
				} else if (Constant.HTTP.CONTENT_TYPE.equalsIgnoreCase(header)) {
					partContentType = s;
				}
				ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
				if (ec < 0)
					break main_loop; // throw new IOException( ..
				if (ec == 2 && buffer[contentRead] == 0x0D && buffer[contentRead + 1] == 0x0A) {
					contentRead += ec;
					break; // empty line read, skip it
				}
				s = new String(buffer, contentRead, ec, enc);
			} while (true);
			if (name == null)
				break main_loop; // throw new IOException( ..
			int marker = contentRead;
			if (partContentType == null || partContentType.indexOf("text/") >= 0
					|| partContentType.indexOf("application/") >= 0 || partContentType.indexOf("message/") >= 0
					|| partContentType.indexOf("unknown") >= 0) { // read
				// everything
				do {
					ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
					if (ec < 0)
						break main_loop;
					s = new String(buffer, contentRead, ec, enc);
					p = s.indexOf(boundary);
					if (p >= 0) { // we met a boundry
						// finish current part
						if (contentRead - marker <= 2) {
							// no file content in the stream, probably it's a
							// remote file
							try {
								URLConnection uc = new URL(filename).openConnection();
								if (uc.getContentType().indexOf("image/") >= 0) { // support
									// only
									// images
									// for
									// now
									int cl = uc.getContentLength();
									if (cl > 0 && cl < maxReqLength) {
										InputStream uis = uc.getInputStream();
										if (uis != null) {
											byte[] im = new byte[cl];
											cl = 0;
											int rc;
											do {
												rc = uis.read(im, cl, im.length - cl);
												if (rc < 0)
													break;
												cl += rc;
											} while (rc > 0);
											uis.close();
											result.put(name, im);
											// result.put(name, new
											// Attachment(filename, contentType,
											// im));
										}
									} else { // length unknown but we can try
										// catch it
										// TODO: use util function to read a bin
										// file
										InputStream uis = uc.getInputStream();
										if (uis != null) {
											byte[] buf = new byte[2048];
											byte[] im = new byte[0];
											try {
												do {
													cl = uis.read(buf);
													if (cl < 0)
														break;
													byte[] wa = new byte[im.length + cl];
													System.arraycopy(im, 0, wa, 0, im.length);
													System.arraycopy(buf, 0, wa, im.length, cl);
													im = wa;
												} while (true);
											} finally {
												uis.close();
											}
											result.put(name, im);
										}
									}
								}
							} catch (MalformedURLException mfe) {
							}
						} else {
							if (partContentType != null && partContentType.indexOf("application/") >= 0) {
								byte[] im = new byte[contentRead - marker - 2];
								System.arraycopy(buffer, marker, im, 0, contentRead - marker - 2/* crlf */);
								result.put(name, im);
							} else {
								String ss;
								result.put(name,
										ss = new String(buffer, marker, contentRead - marker - 2/* crlf */, enc));
								// System.err.println("====PUT(" + name + "," +
								// ss + ")");
							}
						}
						if (s.regionMatches(p + boundaryLength, Constant.HTTP.BOUNDARY_END_SFX, 0,
								Constant.HTTP.BOUNDARY_END_SFX.length()))
							break main_loop; // it shouldn't happen here, but
						// it's Ok
						contentRead += ec;
						break;
					}
					contentRead += ec;
				} while (true);
			} else if (partContentType.indexOf("image/") >= 0 || partContentType.indexOf("audio/") >= 0) {
				do {
					ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
					if (ec < 0)
						throw new IOException("Premature ending of input stream");

					s = new String(buffer, contentRead, ec, enc);
					p = s.indexOf(boundary);
					if (p >= 0) { // we met a bounder
						byte[] im = new byte[contentRead - marker - 2];
						System.arraycopy(buffer, marker, im, 0, contentRead - marker - 2);
						result.put(name, im);
						if (s.regionMatches(p + boundaryLength, Constant.HTTP.BOUNDARY_END_SFX, 0,
								Constant.HTTP.BOUNDARY_END_SFX.length()))
							break main_loop; // it shouldn't happen here, but
						// it's Ok
						contentRead += ec;
						break;
					}
					contentRead += ec;
				} while (true);
			} else {
				// TODO skip by the end of the section and continue
				throw new IOException("Unsupported content type '" + partContentType + '\'');
				// break main_loop;
			}
		} while (true);
		return result;
	}
}