/* aldan3 - DateTime.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: DateTime.java,v 1.3 2009/11/21 08:57:09 dmitriy Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.aldan3.app.Env;

public class DateTime {

	/**
	 * Convenient method to set date hours without using deprecated method
	 * 
	 * @param date
	 * @param hours
	 * @param timeZone
	 * @param locale
	 * @return
	 */
	public static Date setDateHours(Date date, int hours, TimeZone timeZone, Locale locale) {
		Calendar cal = getCalendar(null, locale);
		cal.setTime(date);
		if (timeZone != null)
			cal.setTimeZone(timeZone);
		cal.set(Calendar.HOUR_OF_DAY, hours);
		return cal.getTime();
	}

	public static int getDateHours(Date date, TimeZone timeZone, Locale locale) {
		Calendar cal = getCalendar(timeZone, locale);
		cal.setTime(date);
		return cal.get(Calendar.HOUR_OF_DAY);
	}

	public static String dateToJson(Date date) {
		return dateToJson(date, null);
	}

	public static String dateToJson(Date date, SimpleDateFormat fmtr) {
		if (date == null)
			return "";
		if (fmtr == null)
			if (Env.getJavaVersion() > 7 && false)
				fmtr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);
			else
				fmtr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ", Locale.ENGLISH);
		return fmtr.format(date);
	}

	protected static Calendar getCalendar(TimeZone timeZone, Locale locale) {
		if (timeZone != null)
			if (locale == null)
				return Calendar.getInstance(timeZone);
			else
				return Calendar.getInstance(timeZone, locale);
		else
			return Calendar.getInstance();
	}
}
