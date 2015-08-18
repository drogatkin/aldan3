/* aldan3 - widget.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: widget.java,v 1.45 2014/04/07 07:22:25 cvs Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.web.ui;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.aldan3.annot.DBField;
import org.aldan3.annot.FormField;
import org.aldan3.annot.OptionMap;
import org.aldan3.data.DODelegator;
import org.aldan3.data.util.FieldConverter;
import org.aldan3.data.util.FieldFiller;
import org.aldan3.model.DataObject;
import org.aldan3.model.Log;
import org.aldan3.util.inet.HttpUtils;

public class widget {
	public static final String setup(Object model) {
		StringBuffer result = new StringBuffer(512);
		// insert JS include
		Field[] fields = model.getClass().getFields();
		// do field cascading, defaulting, autosuggest settings
		return result.toString();
	}

	public static final String field(String name, Object model) {
		return field(name, model, null, null);
	}

	public static final String field(String name, Object model, TimeZone timeZone) {
		return field(name, model, timeZone, null);
	}

	public static final String field(String name, Object model, TimeZone timeZone, Locale locale) {
		if (model == null)
			return "";
		if (name == null || name.length()==0)
			return model.toString();
		StringBuffer html = new StringBuffer(256);
		try {
			Field f = model.getClass().getField(name);
			FormField fd = f.getAnnotation(FormField.class);
			if (fd == null)
				return name + " isn't form field";
			if (fd.formFieldName().length() > 0)
				name = fd.formFieldName();
			if (f.isAccessible() == false)
				f.setAccessible(true);
			Object value = f.get(model);
			if (value == null && fd.defaultTo().length() > 0)
				value = fd.defaultTo();
			if (fd.converter() != null && fd.converter() != FieldConverter.class) {
				try {
					FieldConverter fc = fd.converter().newInstance();
					if (value == null)
						value = fc.convert(fd.defaultTo(), timeZone, locale);
					//System.err.printf("Converting value after %s using %s %n", value, fd.converter());
					value = fc.deConvert(value, timeZone, locale);
					//System.err.printf("Value after %s%n", value);
				} catch (Exception e) {
					value = "" + e;
					//e.printStackTrace(); //??
				}
			} else if (value == null)
				value = fd.defaultTo();
			if (fd.presentType() == FormField.FieldType.Editable || fd.presentType() == FormField.FieldType.RichText) {
				String presSize = "";
				if (fd.presentSize() > 0)
					presSize = (fd.presentRows() > 1 ? "\" cols=\"" : "\" size=\"") + fd.presentSize();
				if (fd.autosuggest()) {
					presSize += "\" autocomplete=\"off";
				}
				if (fd.presentRows() > 1) {
					html.append("<textarea rows=\"").append(fd.presentRows()).append("\" name=\"")
							.append(name).append(presSize);
					if (fd.presentType() == FormField.FieldType.RichText)
						html.append("\" id=\"nce_").append(name);
					html.append("\" ").append(fd.presentStyle()).append('>');
				} else
					html.append("<input type=\"text\" name=\"").append(name).append(presSize).append("\" value=\"");
				// TODO check if html encode is required for text area
				if (value != null)
					html.append(HttpUtils.htmlEncode(value.toString()));
				if (fd.presentRows() > 1)
					html.append("</textarea>");
				else {
					Class<?> ft = f.getType();
					if (ft.equals(int.class) || ft == long.class || ft == double.class || ft == float.class
							|| ft.isAssignableFrom(Number.class))
						html.append("\" style=\"text-align:right");
					else if (ft == Date.class) {
						html.append("\" onfocus=\"enterDate(this)");
					}
					// the handlers get set in prologue code
					if (false && fd.autosuggest()) {
						html.append("\" onkeyup=\"autosuggest(this)");
					}
					html.append("\">");
				}
				addRequiredMark(html, fd);
			} else if (fd.presentType() == FormField.FieldType.Password) {
				html.append("<input type=\"password\" name=\"" + name + "\" value=\"");
				if (value != null)
					html.append(HttpUtils.htmlEncode(value.toString()));
				addRequiredMark(html.append("\">"), fd);
			} else if (fd.presentType() == FormField.FieldType.Hidden) {
				html.append("<input type=\"hidden\" name=\"" + name + "\" value=\"");
				if (value != null)
					html.append(HttpUtils.htmlEncode(value.toString()));
				html.append("\">");
			} else if (fd.presentType() == FormField.FieldType.File) {
				String accessServ = fd.fillQuery();
				if (accessServ.length()==0)
					accessServ = "Attach";
				html.append("<div><div id=\"!!").append(name).append("\">");
				if (value != null && value.toString().length() >0)
					html.append("&nbsp;<a href=\"" + accessServ + "?id=")
							.append(HttpUtils.htmlEncode(value.toString())).append("&download=1\">")
							.append("download</a>");
				html.append("</div><div style=\"display:inline;padding-left:1em\" id=\"!!%").append(name).append("\">&nbsp;</div>&nbsp;<a id=\"$$").append(name).append("\" href=\"javascript:void(0)\">attach</a>");
				html.append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"");
				if (value != null)
					html.append(HttpUtils.htmlEncode(value.toString()));
				html.append("\">");
				html.append("</div>");
			} else {
				html.append("<input type=\"");
				if (fd.presentType() != FormField.FieldType.Readonly)
					html.append("hidden\" ");
				else
					html.append("text\" readonly ");
				html.append(" name=\"").append(name).append("\" value=\"");
				if (value != null)
					html.append(HttpUtils.htmlEncode(value.toString()));
				if (fd.presentType() == FormField.FieldType.Readonly) {
					if (fd.presentSize() > 0)
						html.append("\" size=\"").append(fd.presentSize());
					html.append("\">");
				} else {
					html.append("\">");
					if (value != null)
						html.append(HttpUtils.htmlEncode(value.toString()));
				}				
			}
			if (fd.validationMessageTarget().length() >0)
				html.append("&nbsp;").append(field(fd.validationMessageTarget(), model, timeZone, locale));
			return html.toString();
		} catch (SecurityException e) {
			return "";
		} catch (IllegalAccessException e) {
			return "Can't access: " + name;
		} catch (NoSuchFieldException e) {
			return "No field: " + name;
		}
	}

	public static final String select(String name, Object model) {
		return select(name, model, null, null);
	}

	public static final String select(String name, Object model, TimeZone timeZone, Locale locale) {
		if (model == null)
			return "";
		if (name == null || name.length() ==0)
			return model.toString();

		try {
			// TODO name prefixing parsing
			StringBuffer html = new StringBuffer();
			Field f = model.getClass().getField(name);
			FormField fd = f.getAnnotation(FormField.class);
			if (fd.presentFiller() == FieldFiller.class && fd.recalculateFiller() == FieldFiller.class)
				return field(name, model, timeZone, locale);
			html.append("<select name=\"").append(name);
			if (fd.presentRows() > 1)
				html.append("\" size=\"").append(fd.presentRows());
			if (f.getType().isArray() || Collection.class.isAssignableFrom(f.getType()))
				html.append("\" multiple>");
			else
				html.append("\">");
			if (fd.presentRows() <= 1) // TODO use model as coordinator select from common labels // TODO localize message
				html.append("<option value=\"\">").append("Select option").append("</option>"); // TODO localize
			Class fillerClass = fd.presentFiller() == FieldFiller.class ? fd.recalculateFiller() : fd.presentFiller();
			FieldFiller ff = (FieldFiller) fillerClass.newInstance();
			Object fillings = ff.fill(model, name);
			if (fillings != null) {
				String mapping[] = getMapping((OptionMap) fillerClass.getAnnotation(OptionMap.class),
						fd.queryResultMap());
				if (f.isAccessible() == false)
					f.setAccessible(true);
				Object val = f.get(model);
				if (fd.converter() != FieldConverter.class) {
					FieldConverter fc = fd.converter().newInstance();
					val = fc.deConvert(val, timeZone, locale);
				}
				if (fillings.getClass().isArray()) {
					for (DataObject o : (DataObject[]) fillings) {
						addOption(html, o, mapping[1], mapping[0], val);
					}
				} else if (Collection.class.isAssignableFrom(fillings.getClass())) {
					for (DataObject o : (Collection<DataObject>) fillings)
						addOption(html, o, mapping[1], mapping[0], val);
				} else if (Iterable.class.isAssignableFrom(fillings.getClass()))
					for (DataObject o : (Iterable<DataObject>) fillings)
						addOption(html, o, mapping[1], mapping[0], val);
			}
			html.append("</select>");
			addRequiredMark(html, fd);
			return html.toString();
		} catch (SecurityException e) {
			return "";
		} catch (InstantiationException ie) {
			return "Can't create oprion list";
		} catch (IllegalAccessException e) {
			return "Can't access: " + name;
		} catch (NoSuchFieldException e) {
			return "No field: " + name;
		}
	}

	public static final String text(String name, Object model, String enclosePattrn) {
		return text(name, model, enclosePattrn, null, null);
	}

	public static final String text(String name, Object model, String enclosePattrn, TimeZone timeZone) {
		return text(name, model, enclosePattrn, timeZone, null);
	}

	public static final String text(String name, Object model, String enclosePattrn, Locale locale) {
		return text(name, model, enclosePattrn, null, locale);
	}

	public static final String text(String name, Object model, String enclosePattrn, TimeZone timeZone, Locale locale) {
		if (model == null)
			return "";
		if (name == null || name.length() == 0)
			return model.toString();

		try {
			Field f = model.getClass().getField(name);
			FormField fd = f.getAnnotation(FormField.class);
			Object val = f.get(model);
			if (fd.converter() != FieldConverter.class) {
				FieldConverter fc = fd.converter().newInstance();
				val = fc.deConvert(val, timeZone, locale);
			}
			if (fd.presentFiller() == FieldFiller.class && fd.recalculateFiller() == FieldFiller.class) {
				if (val == null)
					return "";
				return val.toString();
			}
			Class fillerClass = fd.presentFiller() == FieldFiller.class ? fd.recalculateFiller() : fd.presentFiller();
			FieldFiller ff = (FieldFiller) fillerClass.newInstance();
			Object fillings = ff.fill(model, name);
			StringBuffer html = new StringBuffer();
			if (fillings != null) {
				String mapping[] = getMapping((OptionMap) fillerClass.getAnnotation(OptionMap.class),
						fd.queryResultMap());
				if (f.isAccessible() == false)
					f.setAccessible(true);
				if (fillings.getClass().isArray()) {
					for (DataObject o : (DataObject[]) fillings) {
						addText(html, o, mapping[1], mapping[0], val, enclosePattrn);
					}
				} else if (Collection.class.isAssignableFrom(fillings.getClass())) {
					for (DataObject o : (Collection<DataObject>) fillings)
						addText(html, o, mapping[1], mapping[0], val, enclosePattrn);
				} else if (Iterable.class.isAssignableFrom(fillings.getClass()))
					for (DataObject o : (Iterable<DataObject>) fillings)
						addText(html, o, mapping[1], mapping[0], val, enclosePattrn);
			}
			return html.toString();
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private static final String[] defaultMapping = new String[] { "value", "label", "help" };

	private static String[] getMapping(OptionMap om, String[] queryResultMap) {
		if (om != null) {
			return new String[] { om.valueMap(), om.labelMap() };
		} else if (queryResultMap.length > 0) {
			if (queryResultMap.length == 1)
				return new String[] { queryResultMap[0], queryResultMap[0] };
			else
				return queryResultMap;
		}

		return defaultMapping;
	}

	public static final String radio(String name, Object model) {
		return button(name, model, true);
	}

	public static final String check(String name, Object model) {
		return button(name, model, false);
	}

	public static final String button(String name, Object model, boolean radio) {
		if (model == null)
			return "";
		if (name == null || name.length() == 0)
			return model.toString();

		try {
			// TODO name prefixing parsing
			StringBuffer html = new StringBuffer();
			Field f = model.getClass().getField(name);
			FormField fd = f.getAnnotation(FormField.class);
			Class type = f.getType();
			Class<? extends FieldFiller> fc = fd.presentFiller();
			int rowLen = fd.presentSize();
			if ((type == boolean.class || type == Boolean.class) && fc == FieldFiller.class)
				fc = radio ? BooleanFiller.class : CheckBooleanFiller.class;
			if (fc == FieldFiller.class)
				return field(name, model, null, null);
			FieldFiller ff = (FieldFiller) fc.newInstance();
			String mapping[] = getMapping(fc.getAnnotation(OptionMap.class), fd.queryResultMap());
			Object fillings = ff.fill(model, name);
			if (fillings != null) {
				if (fd.formFieldName().length() > 0)
					name = fd.formFieldName();
				if (f.isAccessible() == false)
					f.setAccessible(true);
				Object val = f.get(model);
				int cntr = 0;
				if (fillings.getClass().isArray()) {
					for (DataObject o : (DataObject[]) fillings) {
						addRadio(html, name, o, mapping[1], mapping[0], val, radio);
					}
				} else if (Collection.class.isAssignableFrom(fillings.getClass())) {
					for (DataObject o : (Collection<DataObject>) fillings) {
						addRadio(html, name, o, mapping[1], mapping[0], val, radio);
						cntr++;
						if (rowLen > 1) {
							cntr++;
							if (cntr % rowLen == 0)
								html.append("<br>");
						}
					}
				} else if (Iterable.class.isAssignableFrom(fillings.getClass()))
					for (DataObject o : (Iterable<DataObject>) fillings)
						addRadio(html, name, o, mapping[1], mapping[0], val, radio);
			} else
				return field(name, model, null, null);
			return html.toString();
		} catch (SecurityException e) {
			return "";
		} catch (NoSuchFieldException e) {
			return "No field: " + name;
		} catch (InstantiationException e) {
			return "Can't create set of buttons list";
		} catch (IllegalAccessException e) {
			return "Can't access: " + name;
		} catch (Exception e) {
			Log.l.error("Problem in rendering button", e);
			e.printStackTrace();
			return "See log";
		}
	}

	public static final String[] fields(Object model) {
		Field[] fields = model.getClass().getFields();
		String[] result = new String[fields.length];
		int c = 0;
		for (Field f : fields) {
			if (f.getAnnotation(FormField.class) != null) {
				result[c++] = f.getName();
			}
		}
		String [] retVal = new String[c];
		System.arraycopy(result, 0, retVal, 0, c);
		return retVal;
		//return Arrays.copyOfRange(result, 0, c);
	}

	public static final String startForm(Object model, String name) {
		return "<form name=\"" + name + "\" method=\"POST\" action=\"" + name + "\">";
	}

	public static final String endForm() {
		return "</form>";
	}

	// TODO more refactoring of below methods
	private static StringBuffer addOption(StringBuffer html, DataObject o, String ln, String vn, Object val) {
		Object v = o.meanFieldFilter(vn) ? o.get(vn) : o.get(ln);
		boolean match = checkMatch(v, val);
		return html.append("<option  value=\"").append(v == null ? "" : HttpUtils.htmlEncode(v.toString()))
				.append(match ? "\" selected>" : "\">")
				.append(o.get(ln) == null ? "" : HttpUtils.htmlEncode(o.get(ln).toString(), true, false))
				.append("</option>");
	}

	private static StringBuffer addRadio(StringBuffer html, String name, DataObject o, String ln, String vn,
			Object val, boolean radio) {
		Object v = o.meanFieldFilter(vn) ? o.get(vn) : o.get(ln);
		boolean match = checkMatch(v, val);
		return html.append("<input name=\"").append(name).append("\" type=\"").append(radio ? "radio" : "checkbox")
				.append("\" value=\"").append(v == null ? "" : HttpUtils.htmlEncode(v.toString()))
				.append(match ? "\" checked>" : "\">")
				.append(o.get(ln) == null ? "" : HttpUtils.htmlEncode(o.get(ln).toString()));
	}

	private static StringBuffer addText(StringBuffer html, DataObject o, String ln, String vn, Object val,
			String enclose) {
		Object v = o.meanFieldFilter(vn) ? o.get(vn) : o.get(ln);
		if (checkMatch(v, val))
			if (enclose == null || enclose.length() == 0)
				return html.append(o.get(ln).toString());
			else
				return html.append(String.format(enclose, o.get(ln)));
		return html;
	}

	private static boolean checkMatch(Object v, Object val) { // v - list value, val - field value
		Class type = val == null ? null : val.getClass();
		if (v != null && type != null) {
			if (type.isArray()) {
				if (val instanceof Object[])
					for (Object e : (Object[]) val) {
						if (v.equals(e)) {
							return true;
						}
					}
				else if (val instanceof int[])
					for (int e : (int[]) val) {
						if (e == ((Number) v).intValue()) {
							return true;
						}
					}
				else if (val instanceof char[] && v instanceof String )
					for (char c : (char[]) val) {
						if (c == ((String)v).charAt(0)) {
							return true;
						}
					}
			} else if (Collection.class.isAssignableFrom(type)) {
				for (Object e : (Collection) val) {
					if (v.equals(e)) {
						return true;
					}
				}
			} else {
				if (val.equals(v))
					return true;

				if (val instanceof String)
					return val.equals(v.toString());
				else if (v instanceof String) {
					String sv = (String) v;
					if (sv.length() > 0) {
						if (val instanceof Number)
							return val.equals(new Integer((String) v));
						else if (val instanceof Character)
							return ((Character) val).charValue() == sv.charAt(0);
						else if (type.isEnum()) {
							return val.toString().equals(v);
						}
					}
				} 
			}
		}
		return false;
	}

	private static StringBuffer addRequiredMark(StringBuffer html, FormField ff) {
		if (ff.required()) {
			html.append("<div style=\"display:inline\" class=\"req_mark\">*</div>");
		}
		return html;
	}

	@OptionMap
	protected static class BooleanFiller implements FieldFiller<DataObject[], Object> {

		@Override
		public DataObject[] fill(Object inObject, String filter) {
			DODelegator<Pair>[] result = new DODelegator[2];
			result[0] = new DODelegator<Pair>(new Pair("no", Boolean.FALSE));
			result[1] = new DODelegator<Pair>(new Pair("yes", Boolean.TRUE));
			return result;
		}
	}

	@OptionMap
	protected static class CheckBooleanFiller implements FieldFiller<DataObject[], Object> {

		@Override
		public DataObject[] fill(Object inObject, String filter) {
			DODelegator<Pair>[] result = new DODelegator[1];
			result[0] = new DODelegator<Pair>(new Pair("", Boolean.TRUE));
			return result;
		}
	}

	protected static final class Pair {
		public Pair(String l, Object v) {
			value = v;
			label = l;
		}

		@DBField
		public Object value;

		@DBField
		public String label;
	}
}
