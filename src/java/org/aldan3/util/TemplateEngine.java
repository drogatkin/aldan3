/* aldan3 - TemplateEngine.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: TemplateEngine.java,v 1.48 2014/03/22 03:33:18 cvs Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.aldan3.model.Log;
import org.aldan3.model.ProcessException;
import org.aldan3.model.TemplateProcessor;
import org.aldan3.servlet.Constant;
import org.aldan3.util.inet.HttpUtils;

/**
 * <h2>Rules of substitution:</h2>
 * <b> 1. Any entry</b> &#064;[C]name@ is considering as a variable name in definitions map and replaced by string value. if value not found in the Map, the
 * Template engine will try this variable from system properties with empty string as default. <br>
 * if name starts with one of the following symbols '.', ':', '^' then auto value conversion rules applied, and first the special symbol is excluded from the name: <br>
 * <ul>
 * <li>. no conversion happens, toString() function applied to value
 * <li>: URL encoding used to value after toString() applied, this suitable for values used in URLs
 * <li>^ valid Java string encoding used to value after toString() applied, this suitable for values used in JavaScript literal constants
 * <li>~ as '^' with applying HTML encoding first
 * <li>any other allowed char, html encoding used for value after toString()
 * </ul>
 * <br>
 * name can include also formatting mask, in this case mask included in name after char '?', next symbol need to be 'I', 'C', 'N' for decimal number formats or
 * 'D' for dates, or 'F' for formatting using a Formatter. An attempt to consider a value as Date or int, float will be made first, in case of problems a string
 * value will be converted to float or Date, if nothing work, then empty value will be substituted (no errors indicators. Few examples:<br>
 * &#064;order_date?D'MM/dd/yy@<br>
 * Limitation, not supported yet: &#064;order_date?FDATE_ONLY@ <br>
 * If format string starts with ', then it considered as literal, otherwise first was checked as a name of a variable. <br>
 * &#064;order_date?D'MM/dd/yyyy@
 * <p>
 * <b>2. Any entry in form</b> &#064;name(text/html/template)@ is considering as one other the following objects under name in the current map:<br>
 * <ol>
 * <li>Map[]
 * <li>Collection&lt;Map&gt;
 * <li>List&lt;Map&gt;
 * <li>Iterator&lt;Map&gt;
 * <li>Enumeration&lt;Map&gt;
 * <li>Map&lt;String,Map&gt;
 * <li>Map
 * <li>ResultSet
 * </ol>
 * a whole fragment in parenthesis is expanded for all maps in one of collections above. Inner map can also include another inner map. If an inner map is not
 * found it's substituted by empty string. If a map entry isn't a map, then it's substituted by string value of the map entry. <br>
 * After finishing processing multi elements substitution, an attempt to call <code>destroy()</code> method will happen.
 * <br>
 * If Collection object contains arbitrary elements (not map), then such elements will be wrapped
 * in automatically created Map with the following entries:
 * <ul>
 * <li><code>element</code> current element of collection
 * <li><code>index</code> current index of element
 * <li><code>label</code> current labels resource bundle
 * <li><code>parent</code> aggregation object the collection or model object came from
 * <li><code>commonlabel</code> common resource bundle
 * </ul>
 * <br>
 * <pre>
 * An example:
 *   &#064;ROWS(
 * &lt;tr&gt;
 *   &#064;element.getNames*()*(&lt;td&gt;@.parent.getValue*(java.lang.String^@element@)*@&lt;/td&gt;)@
 * &lt;/tr&gt;
 * )@
 * </pre>
 * <p>
 * <b>3. Access to elements of array or range syntax</b><br>
 * &#064;name[start..end,step](text/html/template)@<br>
 * <b>start</b> index is 0 based. <b>end</b> index is inclusive. <b>end</b> index and <b>step</b> are optional. step can be negative,
 *  in this case end should be less than start. Using variables and method calls is allowed for range. Step has to be constant.
 * <p>
 * <b>4. Conditional substitution</b><br>
 * <pre>
 * &#064;name{
 *  &#064;condition1( text/html/template )@
 *  &#064;condition2( text/html/template )@
 *  ....
 *  &#064;conditionN( text/html/template )@
 *  &#064;( text/html/template )@
 * }@
 * </pre>
 * <br>
 * condition can be single value, range, or set of values, examples:<br>
 * red,green,<br>
 * ..100<br>
 * 1..<br>''<br>
 * to specify empty string use "", to specify NULL just omit a value<br> " in value need to be doubled. <br>
 * all conditions are checked so several branches can be substituted when they have the same condition true. Range works only for numbers, so number value or
 * string converted to number can be used. Dates will be converted to long for ranges. Beginning or end of range can be omitted, in this case less than equal or
 * greater than equal condition used. Set or single values compared as strings, to use case insensitive comparison a value has to start with ^. if actual value
 * start with ^ and no case insensitive comparison required then it has to be doubled. ` is used for specifying regular expression in syntax of
 * java.util.regex.Pattern. ` has to doubled if a case string starts with it. Conditional substitution can be nested, or used in loops.<br>
 * When condition is value or function call  &#064; has to be added from both sides, example:<br>
 * <pre>
 *  &#064;element.get*(name)*{
 *     &#064;&#064;current.getName*()*&#064;(text/html/template)&#064;
 *  } &#064;
 * </pre>.
 * <p>
 * <b>5. Includes</b><br>
 * A template can include other templates, using the following construction:<br>
 * &#064;*template_name@ where '*'(star) means include file without processing <br>
 * &#064;%template_name@ where '%'(percent) means process file, then include result. If a template name is in quotes, then it considered as a name of template,
 * otherwise a value of var takes place of a template.<br>
 * For example: &#064;%'privacy.txt'@, &#064;%privacy_templ@ <br>
 * <p>
 * <b>6. Member names</b><br>
 * In some cases name in format &#064;name1.name2@ can be interpreted as get value from member name2 of an object refereed by name1. For compatibility, 1st
 * attempt to retrieve an object name1.name2, then trying to get object name1. If such object exists, next attempt to get value of member name2.toString(), if no
 * such value exists, or not accessible, then use object.toString(). Format rules can be applied in this case too.<br>
 * If object of type of resource bundle, then this syntax will give an access to resource with a name after '.'.
 * <p>
 * <b>7. Calling methods</b> &#064;name.method*(parameters)*@ ... parameter -
 * <ol>
 * <li> digit+
 * <li> literal* except &#064;, (
 * <li> &#064;literal+@
 * <li> 'literal' any literal, ' has to be doubled
 * <li> Java class name^ any above
 * </ol>
 * <p>
 * Another method or field can be taken from result, like &#064;contcat.getName*()*.getLast*()*@
 * <p>
 * name can be name of class, in this case static public method has to be defined.
 * <p>
 * Examples:<br>
 * &#064;authors.get*(@name@,USA,java.lang.String^1982,'it''s',java.util.Map^@region@,23)*@
 * <h3>Substitution context</h3>
 * <ol>
 * <li>request (implemented using <i>request.</i> prefix and ThreadLocal)
 * <li>session (use <i>session.</i> prefix)
 * <li>application (related to properties, resolved without prefix)
 * </ol>
 */
public class TemplateEngine implements TemplateProcessor {
	// TODO list for the engine
	// 1. review and refactor the code of processing parameters
	// 2. type cast of function return

	public static interface Predefined {
		public static final String V_ELEMENT = "element";

		/**
		 * auto generated element of substitution map and represents a curent index in itrative processing
		 */
		public static final String V_INDEX = "index";

		/**
		 * auto generated element of substitution map and represents labels resource object
		 */
		public static final String V_LABEL = "label";

		/**
		 * auto generated element of substitution map and represents an aggregation object which elements expanded
		 */
		public static final String V_PARENT = "parent";

		/** gives access to session data
		 * 
		 */
		public static final String V_SESSION = "session";

		/** gives access to request data
		 * 
		 */
		public static final String V_REQUEST = "request";

	}

	private String SHARED_DATA = "common" + Predefined.V_LABEL;

	private ResourceManager resourceManager;

	private Log log;

	private final static boolean __statedebug = false;

	public static boolean __debug = __statedebug;

	private static int callLevel;

	public static class CurrentRequest {
		private static ThreadLocal<HttpServletRequest> ref = new ThreadLocal<HttpServletRequest>();

		public static/* synchronized */void setRequest(HttpServletRequest req) {
			ref.set(req);
		}

		public static HttpServletRequest getRequest() {
			return ref.get();
		}
	}

	// TODO replace Writer with Appendable 
	public synchronized TemplateEngine init(ResourceManager rm, Log log, String sharedDataName) {
		resourceManager = rm;
		this.log = log;
		if (sharedDataName != null)
			SHARED_DATA = sharedDataName;
		return this;
	}

	public char[] getResource(String _templateName, Locale _locale, TimeZone _timezone, String _enc)
			throws ResourceException {
		if (resourceManager == null)
			throw new ResourceException("Resource manager has not been initialize");
		char[] tmplBuf = (char[]) resourceManager
				.getResource(_templateName, new LocRequester(_locale, _timezone, _enc));
		if (tmplBuf == null)
			throw new ResourceException();
		return tmplBuf;
	}

	protected Writer processResource(Writer _w, String _template, Map _definitions, Properties _properties,
			Locale _locale, TimeZone _timezone, String _enc) throws IOException {
		try {
			char[] tmplBuf = getResource(_template, _locale, _timezone, _enc);
			// TODO consider passing session not as model variables
			return process(_w, tmplBuf, 0, tmplBuf.length, _definitions, (HttpSession) _definitions
					.get(Constant.Variable.SESSION), _properties, _locale, _timezone);
		} catch (ResourceException re) {
			log(Log.WARNING, "Template '" + _template + "' not found, and not processed.", null);
		}
		return _w;
	}

	protected Writer includeResource(Writer _w, String _template, Locale _locale, String _enc) throws IOException {
		try {
			char[] tmplBuf = getResource(_template, _locale, null, _enc);
			_w.write(tmplBuf);
		} catch (ResourceException re) {
			log(Log.WARNING, "Template '" + _template + "' not found, and not included.", null);
		}
		return _w;
	}

	public void process(Writer pw, String template, Object model, Properties properties, Locale locale, TimeZone tz)
			throws ProcessException {
		try {
			processResource(pw, template, (Map) model, properties, locale, tz, null);
		} catch (IOException e) {
			throw (ProcessException) new ProcessException().initCause(e);
		}
	}

	public Writer process(Writer _result, char[] _buf, int _offset, int _length, Map _definitions,
			HttpSession _session, Properties _properties, Locale _locale, TimeZone _timezone) throws IOException {
		// TODO: use Appendable instead of Writer
		assert _properties != null;
		Writer result = _result == null ? new StringWriter(_length + 50) : _result;
		if (_offset >= _buf.length || _length == 0)
			return result;
		int cp = _offset;
		int sp = cp;
		int fp = -1; // position of ? in formatted fields
		int nep = -1; // position of . in member access fields
		int ffdp = -1; // first format dot position, like @micha?Dlabel.format@
		int nepc = -1; // dot (.) position in chained call
		int varNep = -1; // last dot (.) pos relative to var name
		int st = IN_TEXT;
		int cn = 0; // counter of pairs (, ) in @@ context as cases
		int sn = -1; // counter of pairs {, } in @@ context
		int mn = -1; // counter of pairs *(, )* in @@ context

		String varName = null, purVarName = null;
		char modifier = 0;
		int index = -1; // *
		int endIndex = -1;
		int step = 0;
		// for switch
		StateStack stateStack = new StateStack();
		VarValue selector = null;
		VarValue callVal = null;
		VarValue indexVal = null;
		boolean expandCase = false;
		boolean wasExpanded = false;
		StringBuffer buf = null;
		SimpleDateFormat sdf = _locale == null ? new SimpleDateFormat() : new SimpleDateFormat("", _locale);
		if (_timezone != null)
			sdf.setTimeZone(_timezone);
		// if (_offset > 0)
		// log(LOG_PREF_ERROR, "Called with:\n"+new String(_buf,_offset,_length), new Exception());
		do {
			if (__statedebug)
				debug("- " + _buf[cp] + "  cn=" + cn + ",mn=" + mn + ",sn=" + sn + ' ' + STATE_ABREV[st] + ", stack: "
						+ STATE_ABREV[stateStack.peek()]);
			switch (st) {
			case IN_TEXT:
				if (_buf[cp] == '@') {
					st = AT_AT;
				}
				break;
			case AT_AT:
				if (_buf[cp] == '@') {
					st = IN_TEXT;
				} else {
					st = IN_VAR;
					fp = -1;
					nep = varNep = -1;
					ffdp = -1;
					nepc = -1;
				}
				result.write(_buf, sp, cp - sp - 1);
				sp = cp;
				break;
			case IN_VAR_VAL_CALC: // 
				if (_buf[cp] == '.') {
					nepc = cp;
					if (fp > 0 && ffdp < 0)
						ffdp = cp; // var in format??
					st = IN_VAR;
					break;
				} // else pass through
			case IN_VAR:
				if (_buf[cp] == '@') {
					st = IN_TEXT;
					// the following fragment used for providing backward
					// compatibility
					// with names including encoding type symbol. The code
					// inlined for better
					// performance and should be updated in all places
					// simultaneously (at once)
					modifier = _buf[sp];
					varName = fp < 0 ? new String(_buf, sp, cp - sp) : new String(_buf, sp, fp - sp);
					Object val = callVal != null ? callVal.objValue : _definitions.get(varName);
					callVal = null;
					if (val == null) // include check done inside
						val = getFieldValue(_buf, sp, fp < 0 ? cp : fp, nep, _definitions, _session);

					if (val == null) {
						if (varName.length() > 1 && modifier == ':' || modifier == '.' || modifier == '^'
								|| modifier == '%' || modifier == '*' || modifier == '~') {
							sp++;
							purVarName = fp < 0 ? new String(_buf, sp, cp - sp) : new String(_buf, sp, fp - sp);
							val = _definitions.get(purVarName);
							if (val == null) {
								val = _properties.getProperty(purVarName, null);
								if (val == null)
									val = _properties.getProperty(varName, null);
							}
							if (val == null) {
								// log(LOG_PREF_DEBUG, "tname: "+new String(_buf, sp+1,
								// cp-sp-2));
								if (modifier == '*'
										&& (_buf[sp] == '\'' && _buf[cp - 1] == '\'' || _buf[sp] == '"'
												&& _buf[cp - 1] == '"')) {
									includeResource(result, new String(_buf, sp + 1, cp - sp - 2), _locale, null);
									val = null;
								} else if (modifier == '%'
										&& (_buf[sp] == '\'' && _buf[cp - 1] == '\'' || _buf[sp] == '"'
												&& _buf[cp - 1] == '"')) {
									processResource(result, new String(_buf, sp + 1, cp - sp - 2), _definitions,
											_properties, _locale, _timezone, null);
									val = null;
								}
							}
						} else
							// TODO: consider also looking in some other places
							val = _properties.getProperty(varName, "");
					}
					// -- endof inline code
					// TODO reconsider the logic to keep top performance
					// TODO decide about charset
					if (val instanceof String == false) {
						if (val instanceof InputStream)
							val = new InputStreamReader((InputStream) val, Constant.CharSet.UTF8);
						if (val instanceof Reader) {
							// note no modifier filter is applied here
							IOException err = null;
							try {
								Stream.copyStream((Reader) val, result, -1);
							} catch (IOException ioe) {
								log(Log.ERROR, "An exception at copying stream " + ioe, null);
								err = ioe;
							} finally {
								try {
									((Reader) val).close();
								} catch (IOException ioe) {

								}
								if (err != null) {
									val = err.getMessage();
									modifier = 0; // apply encoding
								}
							}
						}
					}
					if (val != null) {
						if (modifier == '*') { // process page include
							includeResource(result, val.toString(), _locale, null);
						} else if (modifier == '%') {
							processResource(result, val.toString(), _definitions, _properties, _locale, _timezone, null);
						} else {
							if (fp > 0 && val.toString().length() > 0) { // process format string
								Format f = null;
								String pt = null;
								if (_buf[fp + 2] != '\'') {
									// System.err.println("Called for formatting
									// with fp: "+fp+"+2, cp "+cp+"-1 ffdp
									// "+ffdp+", "+new String(_buf, fp, cp-fp));
									pt = (String) getFieldValue(_buf, fp + 2, cp, ffdp, _definitions, _session);
									if (pt == null)
										pt = new String(_buf, fp + 2, cp - fp - 2);
								} else
									pt = new String(_buf, fp + 3, cp - fp - 3);
								// try pattern as var name first
								try {
									if (_buf[fp + 1] == 'F') {
										StringBuffer sb = new StringBuffer();
										if (_locale == null)
											new Formatter(sb).format(pt, val);
										else
											new Formatter(sb).format(_locale, pt, val);
										// System.err.printf("Formatting %c, using %s = %s\n",val,pt,sb);
										val = sb;
									} else {
										if (_buf[fp + 1] == 'D') {
											if (pt.length() > 0)
												sdf.applyPattern(pt);
											f = sdf;
										} else {
											if (val instanceof Number == false) {
												if (val instanceof String)
													val = Double.valueOf((String) val);
												else
													val = Double.valueOf(val.toString());
											}
											if (_buf[fp + 1] == 'I')
												f = _locale != null ? NumberFormat.getInstance(_locale) : NumberFormat
														.getInstance();
											else if (_buf[fp + 1] == 'C')
												f = _locale != null ? NumberFormat.getCurrencyInstance(_locale)
														: NumberFormat.getCurrencyInstance();
											else if (_buf[fp + 1] == 'N')
												f = _locale != null ? NumberFormat.getInstance(_locale) : NumberFormat
														.getInstance();

											if (f != null && pt.length() > 0)
												((DecimalFormat) f).applyLocalizedPattern(pt);
										}
										val = f.format(val);
									}
								} catch (Exception e) {
									log(Log.ERROR, "An exception at an attempt to format using " + pt + " value '"
											+ val + "' for var " + varName + "/" + purVarName + ". " + e, null);
								}
							}
							// TODO using toString for aggregate type is
							// dangerous,
							// since Maps can include cyclic dependencies
							if (__debug)
								if (val instanceof Map || val instanceof Collection)
									debug("Not safe usage aggregate type " + val.getClass());
							// TODO introduce encode(Object o, char type) 
							result.write(encodeForView(val, modifier));
						}
					}
					sp = cp + 1; // ??
					// after @
				} else if (_buf[cp] == '(') { // @expression(...
					// TODO keep just varname buffer position constraints for easy discovering late
					varName = new String(_buf, sp, cp - sp);

					modifier = _buf[sp];
					if (varName.length() > 1 && (modifier == ':' || modifier == '.' || modifier == '^' || modifier == '~') ) {
						sp++;
						purVarName = new String(_buf, sp, cp - sp);
					} else
						purVarName = null;
					if (nep > 0)
						varNep = nep - sp;
					st = IN_ARG;
					sp = cp + 1;
					index = 0;
					cn++;
					mn = 0;
					endIndex = Integer.MAX_VALUE;
					step = 1;
				} else if (_buf[cp] == '{') {
					varName = new String(_buf, sp, cp - sp);
					modifier = _buf[sp];
					if (modifier == ':' || modifier == '.' || modifier == '^' || modifier == '~')
						log(Log.ERROR, "Using encode function modifier is not allowed for selectors " + varName + '.',
								null);
					Object val = callVal != null ? callVal.objValue : _definitions.get(varName);
					callVal = null;
					if (val == null)
						val = getFieldValue(_buf, sp, cp, nep, _definitions, _session);
					if (val == null)
						val = _properties.getProperty(varName, "");
					selector = new VarValue(val, null);
					if (__debug)
						debug(String.format("Met %s for %s", selector, varName));
					st = IN_CASE_BEG;
					wasExpanded = false;
					sn = 1;
				} else if (_buf[cp] == '[') {
					varName = new String(_buf, sp, cp - sp);
					modifier = _buf[sp];
					if (varName.length() > 1 && (modifier == ':' || modifier == '.' || modifier == '^' || modifier == '~')) {
						sp++;
						purVarName = new String(_buf, sp, cp - sp);
					} else
						purVarName = null;
					if (nep > 0)
						varNep = nep - sp;
					st = IN_INDEX;
					index = 0;
				} else if (_buf[cp] == '?') {
					fp = cp;
				} else if (_buf[cp] == '.') {
					if (fp < 0)
						nep = cp; // name end position, last dot before format
					else if (ffdp < 0)
						ffdp = cp;
				} else if (_buf[cp] == '*' && nep > sp) { // name.method*parameters
					//if (nep > 0)
					st = IN_WAIT_FOR_CALL_ST;
				}
				break;
			case IN_ARG:
				if (_buf[cp] == ')') {// suspect @expression(..)...
					st = AT_END_ARG;
				} else if (_buf[cp] == '@')
					st = IN_ARG_IN_VAR;
				break;
			case IN_ARG_IN_VAR:
				if (_buf[cp] == '*') {
					st = IN_ST_MET_ARG;
				} else if (_buf[cp] == '(') {
					st = IN_ARG;
					cn++;
				} else if (_buf[cp] == '@') {
					st = IN_ARG;
				} else if (_buf[cp] == '[') {
					st = IN_RANGE_ARG;
				} else if (_buf[cp] == '{') {
					st = IN_ARG_SWITCH;
					sn = 1;
				}
				break;
			case IN_RANGE_ARG:
				if (_buf[cp] == '@')
					st = IN_RANGE_ARG_VAR;
				else if (_buf[cp] == ']')
					st = IN_ARG_IN_VAR;
				break;
			case IN_RANGE_ARG_VAR:
				if (_buf[cp] == '@')
					st = IN_RANGE_ARG;
				else if (_buf[cp] == '*')
					st = IN_RANGE_ARG_VAR_CALL;
				break;
			case IN_RANGE_ARG_VAR_CALL:
				if (_buf[cp] == '(') {
					mn++;
					stateStack.pushState(IN_RANGE_ARG_VAR);
					st = IN_MET_ARG;
				} else
					st = IN_RANGE_ARG_VAR;
				break;
			// methods args parsing
			// parsing @var.met*('xxx', @var2.met*(...)*@,..)*@
			case IN_ST_MET_ARG:
				if (_buf[cp] == '(') {
					mn++;
					stateStack.pushState(IN_EN_MET_CL_ARG);
					st = IN_MET_ARG;
				} else {
					if (mn > 0)
						st = IN_MET_ARG_INNER_VAR;
					else
						st = IN_ARG_IN_VAR;
				}
				break;
			case IN_MET_ARG:
				if (_buf[cp] == ')')
					st = IN_EN_MET_ARG;
				else if (_buf[cp] == '@')
					st = IN_MET_ARG_INNER_VAR_BEG;
				else if (_buf[cp] == '*')
					st = IN_ST_MET_ARG;
				break;
			case IN_EN_MET_ARG:
				if (_buf[cp] == '*') {
					mn--;
					if (mn == 0) {
						st = stateStack.popState();// IN_EN_MET_CL_ARG;
						// log(LOG_PREF_ERROR, "poping "+st+" in context: "+new String(_buf, cp-1, 30));
					}
				} else
					st = IN_MET_ARG;
				break;
			case IN_FRMT_CL_ARG:
			case IN_EN_MET_CL_ARG:
				if (_buf[cp] == '@') {
					st = IN_ARG;
				} else if (_buf[cp] == '{') {
					st = IN_ARG_SWITCH;
					sn = 1;
				} else if (_buf[cp] == '(') {
					cn++;
					st = IN_ARG;
				} else if (_buf[cp] == '?') {
					fp = cp;
					st = IN_FRMT_CL_ARG;
				} else {
					st = st == IN_FRMT_CL_ARG ? IN_FRMT_CL_ARG : IN_MET_ARG;
				}
				break;
			case IN_MET_ARG_INNER_VAR_BEG:
				if (_buf[cp] == '@')
					st = IN_MET_ARG;
				else
					st = IN_MET_ARG_INNER_VAR;
			case IN_MET_ARG_INNER_VAR:
				if (_buf[cp] == '*')
					st = IN_ST_MET_ARG;
				else if (_buf[cp] == '@')
					st = IN_MET_ARG;
				break;
			// end methods args parsing
			case AT_END_ARG: // )
				if (_buf[cp] == '@') { // ..)@
					cn--; // pairs @(, )@ ; @..)*@
					if (cn == 0) {
						// working with blocks @[C]name(block)@
						Object o = callVal != null ? callVal.objValue : _definitions.get(varName);
						callVal = null;
						if (o == null && purVarName != null)
							o = _definitions.get(purVarName);
						if (o == null && varNep > 0) {
							char[] varBufName = purVarName == null ? varName.toCharArray() : purVarName.toCharArray();
							o = getFieldValue(varBufName, 0, varBufName.length, varNep, _definitions, _session);
						}
						// Note: we do not use properties for blocks
						if (step == 0)
							step = 1;
						if (index < 0) {
							log(Log.WARNING, "Start index is negative  " + index + ", zero assumed", null);
							index = 0;
						}
						if (o != null) {
							if (o instanceof List) {
								List al = (List) o;
								if (step > 0)
									if (endIndex >= al.size())
										endIndex = al.size() - 1;
								Map tm = null;
								for (int i = index; i <= endIndex; i += step) { // *
									Object co = al.get(i);
									if (co == null)
										continue;
									if (co instanceof Map == false) {
										co = fillElementMap(tm, co, i, _definitions, varName);
									}
									process(result, _buf, sp, cp - sp - 1, (Map) co, _session, _properties, _locale,
											_timezone);
								}
							} else if (o instanceof Map)
								process(result, _buf, sp, cp - sp - 1, (Map) o, _session, _properties, _locale,
										_timezone);
							else {
								if (o instanceof Collection)
									o = ((Collection) o).toArray();
								else if (o instanceof Set)
									o = ((Set) o).toArray();
								// TODO: consider a unified approach of conversion
								// Object to []
								// TODO: or consider only iterator way next()
								if (o instanceof Object[]) {
									Object[] ao = (Object[]) o;
									// TODO: make the calculation a procedure
									if (step > 0)
										if (endIndex >= ao.length)
											endIndex = ao.length - 1;
									Map tm = null;
									for (int i = index; i <= endIndex; i += step) { // *
										if (ao[i] == null)
											continue;
										if (ao[i] instanceof Map == false) {
											tm = fillElementMap(tm, ao[i], i, _definitions, varName);
										} else
											tm = (Map) ao[i];
										process(result, _buf, sp, cp - sp - 1, tm, _session, _properties, _locale,
												_timezone);
									}
								} else if (o instanceof char[]) {
									char[] charArray = (char[]) o;
									if (step > 0)
										if (endIndex >= charArray.length)
											endIndex = charArray.length - 1;
									for (int i = index; i <= endIndex; i += step) { // *
										result.write(_buf, sp, cp - sp - 1);
										result.write(modifier == '.' ? String.valueOf(charArray[i])
												: (modifier == ':' ? URLEncoder.encode("" + charArray[i],
														Constant.CharSet.ASCII) : HttpUtils.htmlEncode(""
														+ charArray[i])));
									}
									// } else if (o instanceof Collection) {
									// if (step < 0) // reportError("Negative
									// step
									// not allowed
									// log(LOG_PREF_WARNING, "Negative step ("+step+")
									// not
									// allowed for
									// collection/enumeration/iterator.");

								} else if (o instanceof Iterator) {
									if (step != 1)
										log(Log.WARNING, "Only step 1 (used: " + step
												+ ") is allowed for collection/enumeration/iterator.", null);
									//log(Log.DEBUG, "Range "+index+" - "+endIndex, null); 
									Iterator it = (Iterator) o;
									for (int i = 0; i < index && it.hasNext(); i++)
										it.next();
									Map tm = null;
									for (int i = index; i <= endIndex && it.hasNext(); i++) {
										Object co = it.next();
										if (co == null)
											continue;
										if (co instanceof Map == false) {
											co = fillElementMap(tm, co, i, _definitions, varName);
										}
										process(result, _buf, sp, cp - sp - 1, (Map) co, _session, _properties,
												_locale, _timezone);
									}
								} else if (o instanceof Enumeration) {
									if (step < 0) // reportError("Negative
										// step
										// not allowed
										log(Log.WARNING, "Negative step (" + step
												+ ") not allowed for collection/enumeration/iterator.", null);
									Enumeration en = (Enumeration) o;
									for (int i = 0; i < index && en.hasMoreElements(); i++)
										en.nextElement();
									Map tm = null;
									for (int i = index; i <= endIndex && en.hasMoreElements(); i++) {
										Object co = en.nextElement();
										if (co == null)
											continue;
										if (co instanceof Map == false) {
											co = fillElementMap(tm, co, i, _definitions, varName);
										}
										process(result, _buf, sp, cp - sp - 1, (Map) co, _session, _properties,
												_locale, _timezone);
									}
								} else if (o instanceof ResultSet) {
									ResultSet rs = (ResultSet) o;
									try {
										ResultSetMetaData rsmd = rs.getMetaData();
										int cc = rsmd.getColumnCount();
										if (ResultSet.TYPE_FORWARD_ONLY == rs.getType()) {
											if (step < 0) // reportError("Negative
												// step not allowed
												log(Log.WARNING, "Negative step (" + step
														+ ") not allowed for collection/enumeration/iterator.", null);
											// TBD:
										} else {
											rs.absolute(index);
											Map m = new HashMap();
											if (step > 0) {
												for (int i = index; rs.next() && i <= endIndex; i++) {
													m.clear();
													for (int ci = 1; ci <= cc; ci++) {
														String colName = rsmd.getColumnName(ci);
														m.put(colName, rs.getObject(ci));
														// TODO: consider also
														// put
														// another column attrs
													}
													process(result, _buf, sp, cp - sp - 1, m, _session, _properties,
															_locale, _timezone);
												}
											} else {
												for (int i = index; rs.previous() && i >= endIndex; i--) {
													m.clear();
													for (int ci = 1; ci <= cc; ci++) {
														String colName = rsmd.getColumnName(ci);
														m.put(colName, rs.getObject(ci));
														// TODO: consider also
														// put
														// another column attrs
													}
													process(result, _buf, sp, cp - sp - 1, m, _session, _properties,
															_locale, _timezone);
												}
											}
										}
									} catch (SQLException se) {
										result.write("" + se);
									} finally {
										try {
											rs.close();
										} catch (SQLException se1) {
										}
										try {
											rs.getStatement().close();
										} catch (SQLException se1) {
										}
										try {
											rs.getStatement().getConnection().close();
										} catch (SQLException se1) {
										}
										// TODO: consider smarter cleanup
										// procedure
									}
								} else {
									//HashMap hm = new HashMap();
									//fillElementMap(hm, o, -1, _definitions, "");
									//-hm.put(Predefined.V_PARENT, o);
									process(result, _buf, sp, cp - sp - 1, _definitions, _session, _properties,
											_locale, _timezone);
									//result.write(_buf, sp, cp - sp - 1);
									//result.write(modifier == '.' ? o.toString() : (modifier == ':' ? HttpUtils
									//	.urlEncode(o.toString()) : (modifier == '^' ? HttpUtils.toJavaString(o
									//.toString()) : HttpUtils.htmlEncode(o.toString()/*
									// * , encodeLeadingSpaces
									// */))));
								}
								// try to cleanup after vectors (consider finally)
								// TODO: add for in names and call when requested
								try {
									o.getClass().getMethod("destroy").invoke(o);
								} catch (Exception ie) {
								}
							}
						} // no substitution for nulls
						sp = cp + 1;
						st = IN_TEXT;
						break;
					}
				} else if (_buf[cp] == '*') {
					log(Log.ERROR, "Unexpected '*' after ')' can break parsing.", null);
				}
				st = IN_ARG;
				break;
			case IN_INDEX: // *
				if (_buf[cp] == ']') {
					st = IN_TRANSITION;
				} else if (Character.isDigit(_buf[cp]))
					endIndex = index = index * 10 + _buf[cp] - '0';
				else if (_buf[cp] == '@') {
					st = IN_INDEX_VAR;
					indexVal = null;
					nep = -1;
					sp = cp + 1;
				} else if (_buf[cp] == '.') {
					endIndex = Integer.MAX_VALUE;
					st = IN_TO_ENDINDEX;
				} else
					st = IN_TEXT; // forgiving parser
				break;
			case IN_TRANSITION: // *
				if (_buf[cp] == '(') {
					st = IN_ARG;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (Character.isWhitespace(_buf[cp]) == false)
					st = IN_TEXT;
				break;
			case IN_TO_ENDINDEX:
				if (_buf[cp] == '.') {
					st = IN_TO_ENDINDEX_TRANS;
				} else
					st = IN_TEXT; // do not allow floats anyway
				break;
			case IN_TO_ENDINDEX_TRANS:
				if (_buf[cp] == ']') {
					st = IN_TRANSITION;
				} else if (Character.isDigit(_buf[cp])) {
					endIndex = _buf[cp] - '0';
					st = IN_ENDINDEX;
				} else if (_buf[cp] == '@') {
					indexVal = null;
					st = IN_ENDINDEX_VAR;
					sp = cp + 1;
				} else if (_buf[cp] == ',') {
					st = IN_STEP;
					step = 0;
				} else
					st = IN_TEXT;
				break;
			case IN_ENDINDEX:
				if (_buf[cp] == ']') {
					st = IN_TRANSITION;
				} else if (Character.isDigit(_buf[cp]))
					endIndex = endIndex * 10 + _buf[cp] - '0';
				else if (_buf[cp] == ',') {
					st = IN_STEP;
					step = 0;
				} else
					st = IN_TEXT;
				break;
			case IN_ENDINDEX_VAR:
				if (_buf[cp] == '@') {
					st = IN_ENDINDEX_VAR_TRANS;
					String varIndexName = new String(_buf, sp, cp - sp);
					Object val = _definitions.get(varIndexName);
					if (val == null)
						val = _properties.getProperty(varIndexName, null);
					VarValue val2 = indexVal == null ? new VarValue(val, null) : indexVal;
					if (val2.isString() == false)
						endIndex = val2.intVal;
					else
						endIndex = Integer.MAX_VALUE;
					//System.err.println("End index calculated as :"+endIndex+" for "+varIndexName+", and index val:"+indexVal);
				} else if (_buf[cp] == '.') {
					nep = cp;
				} else if (_buf[cp] == '*' && nep > sp) { // name.method*(parameters
					st = IN_WAIT_FOR_ARG_PAR_ENDX;
				}
				break;
			case IN_WAIT_FOR_ARG_PAR_ENDX:
			case IN_WAIT_FOR_ARG_PAR_DX:
				if (_buf[cp] == '(') {
					indexVal = processMethodCall(_buf, cp + 1, sp, nep, cp - 1, _definitions, _session, null, _locale,
							_timezone);
					cp = indexVal.parsingPos;
				}
				st = st == IN_WAIT_FOR_ARG_PAR_ENDX ? IN_ENDINDEX_VAR : IN_INDEX_VAR;
				break;

			case IN_ENDINDEX_VAR_TRANS:
				if (_buf[cp] == ']')
					st = IN_TRANSITION;
				else if (_buf[cp] == ',') {
					st = IN_STEP;
					step = 0;
				} else if (Character.isWhitespace(_buf[cp]) == false)
					st = IN_TEXT;
				break;
			case IN_STEP: // step can't be var for now
				if (_buf[cp] == ']') {
					st = IN_TRANSITION;
				} else if (Character.isDigit(_buf[cp]))
					step = step * 10 + _buf[cp] - '0';
				else
					st = IN_TEXT;
				break;
			case IN_INDEX_VAR:
				if (_buf[cp] == '@') {
					endIndex = Integer.MAX_VALUE;
					st = IN_TO_ENDINDEX_VAR;
					String varIndexName = new String(_buf, sp, cp - sp);
					Object val = _definitions.get(varIndexName);
					if (val == null)
						val = _properties.getProperty(varIndexName, null);
					VarValue val2 = indexVal == null ? new VarValue(val, null) : indexVal;
					if (val2.isString() == false)
						index = val2.intVal;
					else
						index = 0;
				} else if (_buf[cp] == '.') {
					nep = cp;
				} else if (_buf[cp] == '*' && nep > sp) { // name.method*(parameters
					st = IN_WAIT_FOR_ARG_PAR_DX;
				}
				break;
			case IN_TO_ENDINDEX_VAR:
				if (_buf[cp] == ']') {
					st = IN_TRANSITION;
				} else if (_buf[cp] == '.') {
					endIndex = Integer.MAX_VALUE;
					st = IN_TO_ENDINDEX;
				} else
					st = IN_TEXT; // forgiving parser
				break;
			case IN_CASE_BEG:
				if (_buf[cp] == '@') { // @ case
					st = IN_CASE_ENTER;
				} else if (_buf[cp] == '}') {
					st = AT_END_SWITCH;
				} else if (Character.isWhitespace(_buf[cp]) == false) {
					st = IN_TEXT;
				}
				break;
			case IN_CASE_ENTER:
				expandCase = false;
				if (_buf[cp] == '(') {
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					expandCase = !wasExpanded; // default
					break;
				}
				// fall through
			case IN_CASE:
				if (_buf[cp] == '@') { // @ var
					st = IN_CASE_VAR;
					sp = cp + 1;
					nep = -1; // reset
				} else if (_buf[cp] == '(') {
					VarValue val2 = new VarValue((buf != null) ? buf.toString() : new String(_buf, sp, cp - sp), null);
					buf = null;
					expandCase = selector.matches(val2);
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
				} else if (Character.isDigit(_buf[cp])) {
					index = _buf[cp] - '0';
					st = IN_CASE_NUM_START;
					sp = cp;
				} else if (_buf[cp] == '.') { // can be range or str
					// assume that . can start range only
					index = Integer.MIN_VALUE;
					st = IN_CASE_RANGE;
				} else if (_buf[cp] == '"') {
					st = IN_CASE_Q_STR;
					buf = new StringBuffer(10);
					sp = cp + 1;
				} else if (_buf[cp] == ',') {
					// null case
					expandCase = selector.isEmpty();
					st = expandCase ? IN_BYPASS_CASE : IN_CASE;
				} else if (Character.isWhitespace(_buf[cp]) == false) {
					st = IN_CASE_STR;
					sp = cp;
				} // else ignore white space
				break;
			case IN_CASE_VAR: // case variable or field access
				if (_buf[cp] == '@') {
					modifier = _buf[sp];
					if (modifier == ':' || modifier == '.' || modifier == '^' || modifier == '~')
						log(Log.WARNING, "Using encode function modifier is not allowed for cases " + varName, null);
					varName = new String(_buf, sp, cp - sp);
					Object val = nep > sp + 1 ? getFieldValue(_buf, sp, cp, nep, _definitions, _session) : _definitions
							.get(varName);
					if (val == null)
						val = _properties.getProperty(varName, "");
					VarValue val2 = new VarValue(val, null);
					if (val2.isString() == false && val2.isEmpty == false)
						index = val2.intVal;
					expandCase = selector.matches(val2);
					st = expandCase ? IN_BYPASS_CASE : IN_CASE_TRANS;
				} else if (_buf[cp] == '*') { // suspecting call
					st = IN_CASE_VAR_IN_CALL_CASE;
				} else if (_buf[cp] == '.')
					nep = cp;
				break;
			case IN_CASE_VAR_IN_CALL_CASE: // case methods call
				if (_buf[cp] == '(') {
					VarValue val2 = processMethodCall(_buf, cp + 1, sp, nep, cp - 1, _definitions, _session, null,
							_locale, _timezone);
					cp = val2.parsingPos;
					if (val2.isString() == false && val2.isEmpty == false)
						index = val2.intVal;
					expandCase = selector.matches(val2);
					if (__debug)
						debug("Case value method returned =" + val2.objValue + ", matched to: " + selector + " = "
								+ expandCase);
					st = expandCase ? IN_BYPASS_CASE : IN_CASE_TRANS;
					// TODO introduce IN_CASE_TRANS_AFTER_CALL to skip ending '@'
					// as work around IN_CASE_TRANS skips '@'
				} else
					st = IN_CASE_VAR;
				break;
			case IN_CASE_NUM_START:
				if (Character.isDigit(_buf[cp])) {
					index = index * 10 + _buf[cp] - '0';
				} else if (_buf[cp] == '.')
					st = IN_CASE_RANGE;
				else if (_buf[cp] == ',') {
					expandCase = selector.isString() == false && selector.intVal == index;
					st = expandCase ? IN_BYPASS_CASE : IN_CASE;
				} else if (_buf[cp] == '(') {
					expandCase = selector.isString() == false && selector.intVal == index;
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else
					st = IN_CASE_STR;
				break;
			case IN_CASE_Q_STR: // in quoted str
				if (_buf[cp] == '"') {
					st = IN_CASE_Q_STR_QT; // can be end of str
					if (cp - sp > 0)
						buf.append(_buf, sp, cp - sp);
				}
				break;
			case IN_CASE_Q_STR_QT:
				if (_buf[cp] == '"') {
					st = IN_CASE_Q_STR;
					buf.append('"');
					sp = cp + 1;
				} else if (_buf[cp] == ',')
					st = IN_CASE;
				else if (_buf[cp] == '(') {
					VarValue val2 = new VarValue((buf != null) ? buf.toString() : new String(_buf, sp, cp - sp), null);
					buf = null;
					expandCase = selector.matches(val2);
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (Character.isWhitespace(_buf[cp])) {
					st = IN_CASE_STR;
				} else
					st = IN_TEXT;
				break;
			case IN_CASE_STR:
				if (_buf[cp] == ',') {
					VarValue val2 = new VarValue((buf != null) ? buf.toString() : new String(_buf, sp, cp - sp), null);
					buf = null;
					expandCase = selector.matches(val2);
					st = expandCase ? IN_BYPASS_CASE : IN_CASE;
				} else if (_buf[cp] == '(') {
					VarValue val2 = new VarValue((buf != null) ? buf.toString() : new String(_buf, sp, cp - sp), null);
					buf = null;
					expandCase = selector.matches(val2);
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (_buf[cp] == '.') {
					VarValue val2 = new VarValue(new String(_buf, sp, cp - sp), null);
					if (val2.isString() == false) {
						index = val2.intVal;
						st = IN_CASE_RANGE;
					}
				}
				break;
			case IN_CASE_TRANS: // after var ended
				if (_buf[cp] == '.')
					st = IN_CASE_RANGE;
				else if (_buf[cp] == ',')
					st = IN_CASE;
				else if (_buf[cp] == '(') {
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (_buf[cp] == '@') {
					// stay here can be end of funct
				} else if (Character.isWhitespace(_buf[cp]) == false) {
					st = IN_TEXT;
				}
				break;
			case IN_CASE_RANGE:
				if (_buf[cp] == '.')
					st = IN_CASE_NUM_END;
				else
					st = IN_TEXT;
				break;
			case IN_CASE_NUM_END:
				if (_buf[cp] == '@') {
					sp = cp + 1;
					st = IN_CASE_NUM_END_VAR;
				} else if (Character.isDigit(_buf[cp])) {
					endIndex = _buf[cp] - '0';
					st = IN_CASE_NUM_END_NUM;
					sp = cp; // could be date
					// TODO: since end num can be date, then add support
				} else if (_buf[cp] == '(') {
					// endIndex = Integer.MAX_VALUE;
					expandCase = selector.isString() == false && selector.intVal >= index;
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				}
				break;
			case IN_CASE_NUM_END_NUM:
				if (Character.isDigit(_buf[cp])) {
					endIndex = endIndex * 10 + _buf[cp] - '0';
				} else if (_buf[cp] == ',') {
					expandCase = selector.isString() == false && selector.intVal >= index
							&& selector.intVal <= endIndex;
					st = expandCase ? IN_BYPASS_CASE : IN_CASE;
				} else if (_buf[cp] == '(') {
					expandCase = selector.isString() == false && selector.intVal >= index
							&& selector.intVal <= endIndex;
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (_buf[cp] == '/') {
					st = IN_CASE_NUM_END_DATE;
				} else
					st = IN_TEXT;
				break;
			case IN_CASE_NUM_END_VAR:
				if (_buf[cp] == '@') {
					varName = new String(_buf, sp, cp - sp);
					Object val = _definitions.get(varName);
					if (val == null)
						val = _properties.getProperty(varName, "");
					VarValue val2 = new VarValue(val, null);
					if (val2.isString() == false && val2.isEmpty == false) {
						endIndex = val2.intVal;
						expandCase = selector.isString() == false && index <= selector.intVal
								&& selector.intVal <= endIndex;
					}
					st = expandCase ? IN_BYPASS_CASE : IN_CASE_NUM_END_VAR_END;
				}
				break;
			case IN_CASE_NUM_END_VAR_END:
				if (_buf[cp] == ',')
					st = IN_CASE;
				else if (_buf[cp] == '(') {
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (Character.isWhitespace(_buf[cp]) == false) {
					st = IN_TEXT;
				}
				break;
			case IN_CASE_NUM_END_DATE:
				if (_buf[cp] == '(') {
					VarValue val2 = new VarValue(new String(_buf, sp, cp - sp), null);
					if (val2.isString() == false) {
						endIndex = val2.intVal;
						expandCase = selector.isString() == false && selector.intVal >= index;
					}
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (_buf[cp] == ',') {
					VarValue val2 = new VarValue(new String(_buf, sp, cp - sp), null);
					if (val2.isString() == false) {
						endIndex = val2.intVal;
						expandCase = selector.isString() == false && selector.intVal >= index;
						st = expandCase ? IN_BYPASS_CASE : IN_CASE;
					} else
						st = IN_CASE;
				}
				break;
			case IN_BYPASS_CASE:
				if (_buf[cp] == '"')
					st = IN_BYPASS_CASE_QT;
				else if (_buf[cp] == '(') {
					st = IN_ARG_CASE;
					sp = cp + 1;
					cn++;
					mn = 0;
				} else if (_buf[cp] == '*') {
					st = IN_CASE_VAR_BYPASS_CALL_ARG;
				}
				break;
			case IN_CASE_VAR_BYPASS_CALL_ARG:
				if (_buf[cp] == '(') {
					mn++;
					stateStack.pushState(IN_BYPASS_CASE);
					st = IN_MET_ARG;
				} else
					st = IN_BYPASS_CASE;
			case IN_BYPASS_CASE_QT:
				if (_buf[cp] == '"')
					st = IN_BYPASS_CASE;
				break;
			case IN_ARG_CASE: // @case(...
				if (_buf[cp] == ')')
					st = AT_END_ARG_CASE;
				else if (_buf[cp] == '}')
					st = AT_END_ARG_CASE_CASE; // }@
				else if (_buf[cp] == '@')
					st = IN_ARG_IN_VAR_CASE;
				break;
			case IN_ARG_IN_VAR_CASE: // var(....@
				if (_buf[cp] == '(') {
					st = IN_ARG_CASE;
					cn++;
				} else if (_buf[cp] == '*') {
					st = IN_ARG_IN_CALL_CASE;
				} else if (_buf[cp] == '@') {
					st = IN_ARG_CASE;
				} else if (_buf[cp] == '{') {
					sn++;
					st = IN_ARG_CASE;
				}
				break;
			case IN_ARG_IN_CALL_CASE:
				if (_buf[cp] == '(') {
					mn++;
					stateStack.pushState(IN_ARG_IN_VAR_CASE);
					st = IN_MET_ARG;
				} else
					st = IN_ARG_IN_VAR_CASE;
				break;
			case AT_END_ARG_CASE_CASE:
				if (_buf[cp] == '@') {
					sn--;
					if (sn < 0) {
						if (__debug)
							debug("sn<0 " + sn);
						sn = 0;
					}
				}
				st = IN_ARG_CASE;
				break;
			case AT_END_ARG_CASE:
				if (_buf[cp] == '@') {
					cn--;
					if (cn == 0) {
						st = IN_CASE_BEG;
						//System.err.println("Expand:"+expandCase+" of\n"+new String(_buf, sp, cp - sp - 1));
						if (expandCase) {
							process(result, _buf, sp, cp - sp - 1, _definitions, _session, _properties, _locale,
									_timezone);
							wasExpanded = true;
						}
						break;
					}
				} else if (_buf[cp] == ')') 
					break;
				st = IN_ARG_CASE;
				break;
			case AT_END_SWITCH:
				if (_buf[cp] == '@') {
					sn--;
				}
				if (sn != 0)
					if (__debug)
						debug("Not reached 0 for pairs {} " + sn);
				sp = cp + 1;
				st = IN_TEXT;
				break;
			case IN_ARG_SWITCH: // bypass everything until }@
				if (_buf[cp] == '}') {
					st = IN_ARG_SWITCH_END;
					// processing
				} else if (_buf[cp] == '@') { // another @xxx{ can be started
					// so we need to count it as
					// well
					st = IN_ARG_SWITCH_NAME;
				} else if (_buf[cp] == '{') {
					sn++; //???
					//stateStack.pushState(IN_ARG_SWITCH_NAME);
					st = IN_ARG_SWITCH;
				}
				break;
			case IN_ARG_SWITCH_END:
				if (_buf[cp] == '@') {
					sn--; // closed nested @xxx{zzzz}@
					if (sn == 0)
						st = IN_ARG;
					else
						st = IN_ARG_SWITCH_NAME;//IN_ARG_SWITCH;
				} else
					st = IN_ARG_SWITCH;
				break;
			case IN_ARG_SWITCH_NAME:
				if (_buf[cp] == '@')
					st = IN_ARG_SWITCH;
				else if (_buf[cp] == '{') {
					sn++;
					st = IN_ARG_SWITCH;
				}
				break;
			// cases related to method call parameters processing:
			case IN_WAIT_FOR_CALL_ST:
				if (_buf[cp] == '(') {
					callVal = processMethodCall(_buf, cp + 1, sp, nepc > 0 ? nepc : nep, cp - 1, _definitions,
							_session, callVal != null && callVal.isEmpty() == false && nepc > 0 ? callVal.objValue
									: null, _locale, _timezone);
					cp = callVal.parsingPos;
					if (__debug)
						debug("Method returned =" + callVal.objValue);
					if (callVal.isEmpty())
						callVal.objValue = "";
					st = IN_VAR_VAL_CALC;
				} else
					// wrong assume
					st = IN_VAR;
				break;
			default:
				log(Log.ERROR, "Error state:" + st, new Exception(""));
			}
			cp++;
			if (cp >= (_offset + _length) || cp >= _buf.length) {
				if (cp > sp - 1)
					result.write(_buf, sp, cp - sp);
				break;
			}
		} while (true);
		return result;
	}

	private Map fillElementMap(Map _m, Object _element, int _i, Map _definitions, String _parent) {
		if (_m == null)
			_m = new HashMap(5);
		_m.put(Predefined.V_ELEMENT, _element);
		_m.put(Predefined.V_INDEX, new Integer(_i));
		_m.put(SHARED_DATA, _definitions.get(SHARED_DATA));
		_m.put(Predefined.V_LABEL, _definitions.get(Predefined.V_LABEL));
		int dp = _parent.indexOf('.');
		if (dp > 0)
			_m.put(Predefined.V_PARENT, _definitions.get(_parent.substring(0, dp)));
		else
			_m.put(Predefined.V_PARENT, _definitions.get(_parent));
		return _m;
	}

	protected static class StateStack {
		int[] stateStack = new int[0];

		protected void pushState(int _state) {
			int[] newStack = new int[stateStack.length + 1];
			System.arraycopy(stateStack, 0, newStack, 0, stateStack.length);
			newStack[stateStack.length] = _state;
			stateStack = newStack;
		}

		protected int popState() {
			if (stateStack.length < 1)
				throw new EmptyStackException();

			int[] newStack = new int[stateStack.length - 1];
			System.arraycopy(stateStack, 0, newStack, 0, newStack.length);
			int result = stateStack[stateStack.length - 1];
			stateStack = newStack;
			return result;
		}

		protected int peek() {
			return stateStack.length == 0 ? 0 : stateStack[stateStack.length - 1];
		}
	}

	protected Object getFieldValue(char[] _buf, int _sp, int _ep, int _dp, Map _map, HttpSession _session) {
		if (_dp < _sp + 1)
			return null;
		String name = new String(_buf, _sp, _dp - _sp);
		char modifier = _buf[_sp];
		Object target = _map.get(name);
		if (target == null)
			if (name.length() > 1 && (modifier == ':' || modifier == '.' || modifier == '^' || modifier == '~'))
				target = _map.get(name.substring(1));
		String mname = new String(_buf, _dp + 1, _ep - _dp - 1); // part after dot
		if (target == null) { // temporary limitation no member access of session variables, like session.user.lastname
			target = checkInSession(name, mname, _session);
			if (target != null)
				return target;
			target = checkInRequest(name, mname);
			if (target != null)
				return target;
		}
		// System.err.println(CLASS_ID+": Field :"+mname);
		if (target instanceof ResourceBundle)
			try {
				return ((ResourceBundle) target).getObject(mname);
			} catch (MissingResourceException mre) {
				return mname;
			}
		if (modifier == '%' || modifier == '*' || modifier == ':'
				|| modifier == '.' || modifier == '^' || modifier == '~')
			name = name.substring(1);
		if (Character.isJavaIdentifierStart(name.charAt(0)))
			try {
				Class tclass = target == null ? Class.forName(name, true, Thread.currentThread().getContextClassLoader()) : target
						.getClass();
				return tclass.getField(mname).get(target);
			} catch (Exception e) {
				log(Log.ERROR, "At getting field :" + mname + " " + e, null);
			}

		return target;
	}

	protected static Object checkInSession(String _leadName, String _attrName, HttpSession _session) {
		if (_session == null)
			return null;
		if (_leadName.indexOf(Predefined.V_SESSION) == 0) { // not very smart though, better find another way to figure
			if (_attrName != null && _attrName.length() > 0)
				return _session.getAttribute(_attrName);
			return _session;
		}
		return null;
	}

	protected static Object checkInRequest(String _leadName, String _attrName) {
		HttpServletRequest req = CurrentRequest.getRequest();
		if (req == null)
			return null;
		if (_leadName.indexOf(Predefined.V_REQUEST) >= 0) { // not very smart though, better find another way to figure
			return req.getAttribute(_attrName);
		}
		return null;
	}
	
	static String encodeForView(Object val, char modifier) {
		if (val == null)
			return "";
		switch (modifier) {
		case '.':
			return val.toString();
		case ':':
			return HttpUtils.urlEncode(val.toString());
		case '^':
			return HttpUtils.toJavaString(val.toString());
		case '~':
			return HttpUtils.toJavaString(HttpUtils.htmlEncode(val.toString()));
		}
		return HttpUtils.htmlEncode(val.toString()/* , encodeLeadingSpaces */);
	}

	/**
	 * Helper parser to parse argument and make a call, it takes care of recursive calling methods in parameters
	 * 
	 * @param char[]
	 *            buffer where a call needs to be parsed
	 * @param int
	 *            offset in a buffer of starting arguments list
	 * @param int
	 *            offset in a buffer of starting var name
	 * @param int
	 *            offset in a buffer of dot in var name
	 * @param int
	 *            offset in a buffer of end method name
	 * @param Map
	 *            available variables for parameter substitution //
	 * @param Properties
	 *            available properties for parameters
	 * @param Object
	 *            value of temporary variable when chained call happens
	 * @param Locale
	 *            a locale used for substitution
	 * @param TimeZone
	 *            time zone used for substitution
	 * @return VarValue result of a calling method
	 * @exception return
	 *                will contain exception if happened
	 */
	protected VarValue processMethodCall(char[] _buf, int _offset, int _ns, int _dp, int _ne, /*int _castEn,*/ Map _definitions,
			HttpSession _session, Object _o, Locale _locale, TimeZone _timezone) {
		// TODO add cast to a particular class/interface at a method call, for example @java.nio.file.Path^path.getFileName*()*@
		// TODO add properties to figure parameters too
		List callParams = new ArrayList();
		int cp = _offset;
		int nsp = -1; // start name position
		int dp = -1; // dot pos in name
		VarValue vv = null;
		Object chainVar = null;
		Class castClass = null;
		char quote = 0;
		int st = IN_EXPECT_NEXT_PARAM_ST;
		int saveState = -1;
		String ident;
		if (__statedebug) {
			callLevel++;
			char[] stepIdent = new char[callLevel];
			Arrays.fill(stepIdent, '+');
			ident = "-" + new String(stepIdent) + " ";
		}
		do {
			if (__statedebug) {
				debug(ident + _buf[cp] + ' ' + STATE_ABREV[st]);
			}

			switch (st) {
			case IN_EXPECT_NEXT_PARAM_ST: // start of next param in list
				nsp = cp;
				if (_buf[cp] == '@') { // param var, like @varname....
					st = IN_VAR_PARAM_ST;
					dp = -1;
					nsp++; // start of name
				} else if (_buf[cp] == '\'' || _buf[cp] == '"') { // liter
					// param
					st = IN_Q_LIT_PARAM_ST;
					quote = _buf[cp];
					nsp++;
					castClass = java.lang.String.class;
				} else if (Character.isDigit(_buf[cp]) || _buf[cp] == '-' || _buf[cp] == '+' || _buf[cp] == '.') {
					st = IN_NUM_PARAM_ST;
					if (_buf[cp] == '.') {
						castClass = float.class;
						dp = cp;
					} else {
						dp = -1;
						castClass = int.class;
					}
				} else if (_buf[cp] == ')') { // 
					saveState = st;
					st = IN_EXPECT_END_PARAM_LIST_ST;
				} else {
					st = IN_STR_PARAM_ST;
				}
				break;
			case IN_NUM_PARAM_ST: // number parameter
				if (_buf[cp] == ',') {
					vv = new VarValue(new Integer(new String(_buf, nsp, cp - nsp)), castClass);
					callParams.add(vv);
					castClass = null;
					st = IN_EXPECT_NEXT_PARAM_ST;
					saveState = -1;
				} else if (_buf[cp] == ')') {
					saveState = st;
					st = IN_EXPECT_END_PARAM_LIST_ST;
				} else if (_buf[cp] == 'e' || _buf[cp] == 'E')
					castClass = java.lang.Float.class;
				else if (_buf[cp] == '.') {
					castClass = java.lang.Float.class;
					// check if not first '.' then st = IN_STR_PARAM_ST;
				}
				break;
			case IN_STR_PARAM_ST: // String parameter
				if (_buf[cp] == ',') {
					vv = new VarValue(new String(_buf, nsp, cp - nsp), castClass);
					castClass = null;
					callParams.add(vv);
					st = IN_EXPECT_NEXT_PARAM_ST;
					saveState = -1;
				} else if (_buf[cp] == ')') {
					saveState = st;
					st = IN_EXPECT_END_PARAM_LIST_ST;
				} else if (_buf[cp] == '^') { // was type clarifier
					st = IN_EXPECT_NEXT_PARAM_ST;
					String className = null;
					try {
						// TODO consider using trim for class name
						castClass = Class.forName(className = new String(_buf, nsp, cp - nsp), true, Thread.currentThread().getContextClassLoader());
					} catch (Exception ce) { // ClassNotFoundException
						if ("boolean".equals(className))
							castClass = boolean.class;
						//else if ("int".equals(className))
						//	castClass = int.class;
						else if ("long".equals(className))
							castClass = long.class;
						else if ("double".equals(className))
							castClass = double.class;
						else
							log(Log.ERROR, "Class '" + className + "' not found using current class loader.", ce);
					}
					nsp = cp + 1;
				}
				break;
			case IN_EXPECT_END_PARAM_LIST_ST: // expected end list as )*
				if (_buf[cp] == '*') {
					String val = new String(_buf, nsp, cp - nsp - 1);
					if (saveState != -1 && val.length() > 0) {
						if (saveState == IN_NUM_PARAM_ST) {
							vv = new VarValue(new Integer(val), castClass);
						} else
							vv = new VarValue(val, castClass);
						castClass = null;
						callParams.add(vv);
					}
					saveState = -1;

					String varName = new String(_buf, _ns, _dp - _ns); // local for this class and method
					if (_o == null)
						_o = _definitions.get(varName);
					if (_o == null) {
						if (_buf[_ns] == ':' || _buf[_ns] == '.' || _buf[_ns] == '^' || _buf[_ns] == '~') {
							varName = new String(_buf, _ns + 1, _dp - _ns - 1);
							_o = _definitions.get(varName);
						}
					}
					if (Predefined.V_REQUEST.equals(varName))
						_o = CurrentRequest.getRequest();
					if (_o == null)
						try {
							_o = Class.forName(varName, true, Thread.currentThread().getContextClassLoader());
						} catch (ClassNotFoundException ncfe) {
							if (__debug)
								debug("No such class: " + ncfe);
						}
					if (__debug)
						debug("Var: '" + varName + "' " + _o + ", mtd: " + new String(_buf, _dp + 1, _ne - _dp - 1)
								+ ", params: " + callParams + ", parsing pos:" + cp);
					if (_o == null) {
						vv = new VarValue("Neither <var> nor <class> nor <request> have been found or =null for: "
								+ varName, null);
					} else {
						Class[] pcs = new Class[callParams.size()];
						Object[] pvs = new Object[pcs.length];
						try {							
							for (int i = 0; i < pcs.length; i++) {
								VarValue cv = (VarValue) callParams.get(i);
								pcs[i] = cv.castClass;
								pvs[i] = cv.objValue;
								//log(Log.DEBUG, "Parameter " +cv.objValue+" of "+cv.objValue.getClass(), null);
							}
							//Arrays.toString(pvs)
							Method m;
							if (_o instanceof Class) {
								m = ((Class) _o).getMethod(new String(_buf, _dp + 1, _ne - _dp - 1), pcs);
								_o = null;
							} else {
								m = _o.getClass()
										.getMethod(new String(_buf, _dp + 1, _ne - _dp - 1), pcs);
							}
							if (DataConv.javaVersion() > 10)
								m.trySetAccessible();
							else
								m.setAccessible(true); 
							vv = new VarValue(m.invoke(_o, pvs), castClass);
						} catch (Exception e) {
							if (e instanceof InvocationTargetException) {
								log(Log.ERROR, "An exception in calling the method "
										+ new String(_buf, _dp + 1, _ne - _dp - 1) + " of " + _o + "/" + (_o == null?"NULL":_o.getClass()),
										((InvocationTargetException) e).getTargetException());
								vv = new VarValue(((InvocationTargetException) e).getTargetException(), null);
							} else {
								log(Log.ERROR, "Can't call method '" + new String(_buf, _dp + 1, _ne - _dp - 1) + "' of "
										+ _o + " as " + (_o == null?"NULL":_o.getClass()) + " with "+Arrays.toString(pvs) + " of "+Arrays.toString(pcs), e);
								vv = new VarValue(e, null);
							}
						}
					}
					castClass = null;
					vv.parsingPos = cp;
					return vv;
				} else if (_buf[cp] == ',') {
					String val = new String(_buf, nsp, cp - nsp);
					if (saveState != -1 && val.length() > 0) {
						if (saveState == IN_NUM_PARAM_ST) {
							vv = new VarValue(new Integer(val), castClass);
						} else
							vv = new VarValue(val, castClass);
						castClass = null;
						callParams.add(vv);
					}
					saveState = -1;
					st = IN_EXPECT_NEXT_PARAM_ST;
				} else if (_buf[cp] != ')') {
					st = saveState;
				}
				break;
			case IN_WAIT_PARAM_SEP_ST: // expect closing list or next parameter
				if (_buf[cp] == ',') {
					st = IN_EXPECT_NEXT_PARAM_ST;
				} else if (_buf[cp] == ')') {
					st = IN_EXPECT_END_PARAM_LIST_ST;
				}
				// skip all not match
				break;
			case IN_VAR_PARAM_ST: // var parameter, can be method call as well
				if (_buf[cp] == '.') {
					dp = cp;
				} else if (_buf[cp] == '@') {
					// eval value
					if (dp <= 0) {
						vv = new VarValue(_definitions.get(new String(_buf, nsp, cp - nsp)), castClass);
						callParams.add(vv);
					} else {
						Object val = _definitions.get(new String(_buf, nsp, cp - dp));
						if (val == null)
							val = getFieldValue(_buf, nsp, cp, dp, _definitions, _session);
						callParams.add(vv = new VarValue(val, castClass));
					}
					castClass = null; // reset cast
					saveState = -1;
					st = IN_WAIT_PARAM_SEP_ST;
				} else if (_buf[cp] == '*') {
					st = IN_WAIT_FOR_CALL_ST;
				} else if (_buf[cp] == '(' || _buf[cp] == ')') {
					log(Log.WARNING, "Found '" + _buf[cp] + "' which is not allowed in parameter.", null);
				}
				break;
			case IN_WAIT_FOR_CALL_ST: // expected '(' after '*'
				if (_buf[cp] == '(') {
					vv = processMethodCall(_buf, cp + 1, nsp, dp, cp - 1, _definitions, _session, chainVar, _locale,
							_timezone);
					cp = vv.parsingPos;
					st = IN_EXPECT_NEXT_CALL_OR_CL_ST; // after )* can be @ or .
				} else
					st = IN_VAR_PARAM_ST;
				break;
			case IN_EXPECT_NEXT_CALL_OR_CL_ST: // waiting for @ or . after call
				if (_buf[cp] == '@') {
					st = IN_WAIT_PARAM_SEP_ST;
					if (castClass != null) {
						vv.castClass = castClass;
						vv.doCast(castClass);
						castClass = null;
					}
					callParams.add(vv);
					saveState = -1;
					if (__debug)
						debug("Added called parameter: " + vv);
					chainVar = null;
				} else if (_buf[cp] == '.') {
					// using current vv to do processMethodCall(
					chainVar = null;
					if (vv != null)
						chainVar = vv.objValue;
					dp = cp;
					st = IN_VAR_PARAM_ST;
				} else {
					log(Log.WARNING, "Unexpected symbol '" + _buf[cp] + "' after parameter calculation call.", null);
					st = IN_WAIT_PARAM_SEP_ST;
				}
				break;
			case IN_Q_LIT_PARAM_ST: // start lit quoted param after first quote
				if (_buf[cp] == quote) {
					st = IN_Q_EXP_PARAM_END_ST;
				}
				break;
			case IN_Q_EXP_PARAM_END_ST:
				if (_buf[cp] == ',' || _buf[cp] == ')') {
					vv = new VarValue(new String(_buf, nsp, cp - nsp - 1), castClass);
					// remove quote dup
					castClass = null;
					callParams.add(vv);
					saveState = -1;
					if (_buf[cp] == ')')
						st = IN_EXPECT_END_PARAM_LIST_ST;
					else
						st = IN_EXPECT_NEXT_PARAM_ST;
				} else if (_buf[cp] == quote)
					st = IN_Q_LIT_PARAM_ST;
				else
					st = IN_BROKEN_EXP_ST;
				break;
			}
			cp++;
		} while (cp < _buf.length - 1 /* endPos? */);
		// generally an exception should be here
		vv = new VarValue(new Exception("Incorrect syntax of method call."), null);
		vv.parsingPos = cp;
		if (__statedebug)
			callLevel--;
		return vv;
	}

	protected static final int IN_TEXT = 0;

	protected static final int AT_AT = 1;

	protected static final int IN_VAR = 2;

	protected static final int IN_ARG = 3;

	protected static final int AT_END_ARG = 4;

	protected static final int IN_INDEX = 5; // or start of start index

	protected static final int IN_TRANSITION = 6;

	protected static final int IN_ARG_IN_VAR = 7;

	protected static final int IN_TO_ENDINDEX = 8;

	protected static final int IN_TO_ENDINDEX_TRANS = 9;

	protected static final int IN_ENDINDEX = 10;

	protected static final int IN_STEP = 11;

	protected static final int IN_INDEX_VAR = 12;

	protected static final int IN_TO_ENDINDEX_VAR = 13;

	protected static final int IN_CASE_NUM_END_VAR = 14;

	protected static final int IN_CASE = 15;

	protected static final int IN_CASE_VAR = 16;

	protected static final int IN_CASE_NUM_START = 17;

	protected static final int IN_CASE_Q_STR = 18;

	protected static final int IN_CASE_STR = 19;

	protected static final int IN_BYPASS_CASE = 20;

	protected static final int IN_CASE_TRANS = 21;

	protected static final int IN_BYPASS_CASE_QT = 22;

	protected static final int IN_CASE_RANGE = 23;

	protected static final int IN_CASE_NUM_END = 24;

	protected static final int IN_CASE_NUM_END_NUM = 25;

	protected static final int IN_CASE_Q_STR_QT = 26;

	protected static final int IN_ARG_CASE = 27;

	protected static final int IN_ARG_IN_VAR_CASE = 28;

	protected static final int AT_END_ARG_CASE = 29;

	protected static final int AT_END_SWITCH = 30;

	protected static final int IN_CASE_BEG = 31;

	protected static final int IN_ENDINDEX_VAR = 32;

	protected static final int IN_ENDINDEX_VAR_TRANS = 33;

	protected static final int IN_CASE_NUM_END_DATE = 34;

	protected static final int IN_CASE_NUM_END_VAR_END = 35;

	protected static final int IN_ARG_SWITCH = 36;

	protected static final int IN_ARG_SWITCH_END = 37;

	protected static final int IN_ARG_SWITCH_NAME = 38;

	protected static final int AT_END_ARG_CASE_CASE = 39;

	protected static final int IN_CASE_ENTER = 40;

	protected static final int IN_VAR_PARAM_ST = 41; // starting parameters

	// in method call

	// protected static final int IN_VAR_PARAM_END_ST = 42;
	protected static final int IN_WAIT_PARAM_SEP_ST = 43;

	protected static final int IN_WAIT_FOR_CALL_ST = 44;

	protected static final int IN_NUM_PARAM_ST = 45;

	protected static final int IN_EXPECT_NEXT_PARAM_ST = 46;

	protected static final int IN_Q_LIT_PARAM_ST = 47;

	protected static final int IN_EXPECT_END_PARAM_LIST_ST = 48;

	protected static final int IN_STR_PARAM_ST = 49;

	protected static final int IN_Q_EXP_PARAM_END_ST = 50;

	protected static final int IN_BROKEN_EXP_ST = 51;

	protected static final int IN_ST_MET_ARG = 52;

	protected static final int IN_MET_ARG = 53;

	protected static final int IN_EN_MET_ARG = 54;

	protected static final int IN_EN_MET_CL_ARG = 55;

	protected static final int IN_MET_ARG_INNER_VAR = 56;

	protected static final int IN_MET_ARG_INNER_VAR_BEG = 57;

	protected static final int IN_VAR_VAL_CALC = 58;

	protected static final int IN_ARG_IN_CALL_CASE = 59;

	protected static final int IN_EXPECT_NEXT_CALL_OR_CL_ST = 60;

	protected static final int IN_CASE_VAR_IN_CALL_CASE = 61;

	protected static final int IN_CASE_VAR_BYPASS_CALL_ARG = 62;

	private static final int IN_WAIT_FOR_ARG_PAR_ENDX = 63;

	private static final int IN_RANGE_ARG = 64;

	private static final int IN_RANGE_ARG_VAR = 65;

	private static final int IN_RANGE_ARG_VAR_CALL = 66;

	private static final int IN_WAIT_FOR_ARG_PAR_DX = 67;

	private static final int IN_FRMT_CL_ARG = 68;

	protected static final String STATE_ABREV[] = { "IN_TEXT", "AT_AT", "IN_VAR", "IN_ARG", "AT_END_ARG", "IN_INDEX",
			"IN_TRANSITION", "IN_ARG_IN_VAR", "IN_TO_ENDINDEX", "IN_TO_ENDINDEX_TRANS", "IN_ENDINDEX", "IN_STEP",
			"IN_INDEX_VAR", "IN_TO_ENDINDEX_VAR", "IN_CASE_NUM_END_VAR", "IN_CASE", "IN_CASE_VAR", "IN_CASE_NUM_START",
			"IN_CASE_Q_STR", "IN_CASE_STR", "IN_BYPASS_CASE", "IN_CASE_TRANS", "IN_BYPASS_CASE_QT", "IN_CASE_RANGE",
			"IN_CASE_NUM_END", "IN_CASE_NUM_END_NUM", "IN_CASE_Q_STR_QT", "IN_ARG_CASE", "IN_ARG_IN_VAR_CASE",
			"AT_END_ARG_CASE", "AT_END_SWITCH", "IN_CASE_BEG", "IN_ENDINDEX_VAR", "IN_ENDINDEX_VAR_TRANS",
			"IN_CASE_NUM_END_DATE", "IN_CASE_NUM_END_VAR_END", "IN_ARG_SWITCH", "IN_ARG_SWITCH_END",
			"IN_ARG_SWITCH_NAME", "AT_END_ARG_CASE_CASE", "IN_CASE_ENTER", "IN_VAR_PARAM_ST", "IN_VAR_PARAM_END_ST",
			"IN_WAIT_PARAM_SEP_ST", "IN_WAIT_FOR_CALL_ST", "IN_NUM_PARAM_ST", "IN_EXPECT_NEXT_PARAM_ST",
			"IN_Q_LIT_PARAM_ST", "IN_EXPECT_END_PARAM_LIST_ST", "IN_STR_PARAM_ST", "IN_Q_EXP_PARAM_END_ST",
			"IN_BROKEN_EXP_ST", "IN_ST_MET_ARG", "IN_MET_ARG", "IN_EN_MET_ARG", "IN_EN_MET_CL_ARG",
			"IN_MET_ARG_INNER_VAR", "IN_MET_ARG_INNER_VAR_BEG", "IN_VAR_VAL_CALC", "IN_ARG_IN_CALL_CASE",
			"IN_EXPECT_NEXT_CALL_OR_CL_ST", "IN_CASE_VAR_IN_CALL_CASE", "IN_CASE_VAR_BYPASS_CALL_ARG",
			"IN_WAIT_FOR_ARG_PAR_ENDX", "IN_RANGE_ARG", "IN_RANGE_ARG_VAR", "IN_RANGE_ARG_VAR_CALL",
			"IN_WAIT_FOR_ARG_PAR_DX", "IN_FRMT_CL_ARG" };

	protected void debug(String msg) {
		log(Log.DEBUG, msg, null);
	}

	protected void log(String pref, String msg, Throwable t) {
		if (log == null) {
			System.err.printf("%s [Aldan3.TemplateEngine] %s%n", pref, msg);
			if (t != null)
				t.printStackTrace();
		} else
			log.log(pref, "Aldan3.TemplateEngine", msg, t);
	}

	protected static class LocRequester implements ResourceManager.LocalizedRequester {
		Locale l;

		TimeZone t;

		String e;

		LocRequester(Locale _locale, TimeZone _timezone, String _enc) {
			l = _locale;
			e = _enc;
			t = _timezone;
		}

		public Locale getLocale() {
			return l;
		}

		public String getEncoding() {
			return e;
		}

		public TimeZone getTimeZone() {
			return t;
		}
	}

	// TODO all casting operations have to happen ate requesting value
	protected static class VarValue implements Comparable {
		boolean isEmpty;

		int intVal;
		
		long longVal;

		double dblVal;

		boolean boolVal;

		String strVal;

		Object objValue;

		private Class castClass;

		int parsingPos; // when value returned by parsing

		VarValue(Object _value, Class _castClass) {
			if (_value == null) {
				isEmpty = true;
				castClass = _castClass;
				if (castClass == null)
					castClass = Object.class;
				return;
			}
			isEmpty = false;
			castClass = _value.getClass();
			objValue = _value;
			if (_value instanceof Integer) {
				intVal = ((Integer) _value).intValue();
				castClass = int.class;
			} else if (_value instanceof Long) {
				longVal = ((Long) _value).longValue();
				castClass = long.class;
			} else if (_value instanceof Date)
				intVal = (int) (((Date) _value).getTime() / 1000l);
			else {
				if (_value instanceof String == false)
					strVal = _value.toString();
				else
					strVal = (String) _value;
				if (_castClass != null) {
					castClass = _castClass;
					if (_castClass == boolean.class)
						try {
							objValue = new Boolean(strVal);
							boolVal = ((Boolean) objValue).booleanValue();
							strVal = null;
						} catch (Exception e) {
							strVal = "Can't convert " + strVal + " to boolean " + e;
						}
					else if (_castClass == int.class)
						try {
							intVal = Integer.parseInt(strVal);
							strVal = null;
						} catch (Exception e) {
							strVal = "Can't convert " + strVal + " to int " + e;
						}
					else if (_castClass == long.class)
						try {
							longVal = Long.parseLong(strVal);
							strVal = null;
						} catch (Exception e) {
							strVal = "Can't convert " + strVal + " to long " + e;
						}
				} else {
					try {
						objValue = Integer.valueOf(strVal);
						intVal = ((Integer) objValue).intValue();
						//strVal = null;
						castClass = int.class;
					} catch (Exception nfe) {
						java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MM/dd/yy", Locale.US);
						try {
							Date d = df.parse(strVal);
							longVal = d.getTime();
							intVal = (int) (longVal / 1000l);
							castClass = Date.class;
							//strVal = null;
						} catch (Exception idf) {
							try {
								dblVal = Double.parseDouble(strVal);
								//strVal = null;
								castClass = double.class;
							} catch (NumberFormatException nfe1) {

							}
						}
					}
				}
			}
		}

		void doCast(Class _cast) {
			castClass = _cast;
			if (castClass == String.class) {
				objValue = strVal;
			}
		}
		
		boolean isString() {
			return /* isEmpty == false && */strVal != null;
		}

		boolean isEmpty() {
			return isEmpty;
		}

		public boolean equals(Object _o) {
			if (_o instanceof VarValue == false)
				throw new ClassCastException();
			return (this == _o || (isEmpty && ((VarValue) _o).isEmpty))
					|| (strVal != null && strVal.equals(((VarValue) _o).strVal))
					|| (strVal == null && ((VarValue) _o).strVal == null && intVal == ((VarValue) _o).intVal);
		}

		public boolean matches(Object _o) {
			if (_o instanceof VarValue == false)
				throw new ClassCastException();
			if (this == _o || (isEmpty && ((VarValue) _o).isEmpty))
				return true;
			if (strVal == null) {
				if (((VarValue) _o).strVal == null && intVal == ((VarValue) _o).intVal)
					return true;
				else
					return false;
			}
			// because both not empty
			String s = ((VarValue) _o).strVal;
			if (s == null)
				return false;
			int l = s.length();
			if (l == 0)
				return strVal.equals(s);
			char c = s.charAt(0);
			if (c == '^')
				if (l > 1 && s.charAt(1) != '^')
					return strVal.equalsIgnoreCase(s.substring(1));
				else
					return strVal.equals(s.substring(1));
			else if (c == '`')
				if (l > 1 && s.charAt(1) != '`')
					return strVal.matches(s.substring(1));
				else
					return strVal.equals(s.substring(1));
			return strVal.equals(s);
		}

		public int compareTo(Object _o) {
			if (strVal != null || ((VarValue) _o).strVal != null)
				throw new IllegalArgumentException();
			return ((VarValue) _o).intVal - intVal;
		}

		public String toString() {
			return "Str val:" + strVal + ", int val: " + intVal + ", bool value:" +boolVal+ ", obj val:"+objValue+", class val:" + (objValue==null?"":objValue.getClass().getName())+", cast:"+castClass;
		}
	}
}