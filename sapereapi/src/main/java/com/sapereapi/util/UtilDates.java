package com.sapereapi.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UtilDates {
	public final static SimpleDateFormat format_date_time = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
	public final static SimpleDateFormat format_time = new SimpleDateFormat("HH:mm:ss");
	public final static SimpleDateFormat format_sql = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public final static SimpleDateFormat format_sql_day = new SimpleDateFormat("yyyy-MM-dd");
	public final static SimpleDateFormat format_day = new SimpleDateFormat("yyyyMMdd");
	public final static SimpleDateFormat format_sessionid = new SimpleDateFormat("yyyyMMdd_HHmmss");
	public final static String CR = System.getProperty("line.separator"); // Cariage return

	public final static DecimalFormat df = new DecimalFormat("#.##");
	public final static DecimalFormat df2 = new DecimalFormat("#.#####");

	public final static int MS_IN_MINUTE = 1000 * 60;
	public final static int MS_IN_HOUR = 1000 * 60 * 60;

	public static boolean activateDateShift = true;


	public static String getCurrentTimeStr() {
		Date current = new Date();
		return format_time.format(current);
	}

	public static String generateSessionId() {
		Random random = new Random();
		int rand3 = random.nextInt(10000);
		return format_sessionid.format(new Date()) + "_" + String.format("%04d", rand3);
	}


	public static Date getNewDate(Long timeShiftMS) {
		if(timeShiftMS == null || timeShiftMS.doubleValue() == 0) {
			return new Date();
		}
		Long timeMS = new Date().getTime() + timeShiftMS;
		return new Date(timeMS);
	}

	public static Date getNewDate(Map<Integer, Integer> dateShift) {
		if (dateShift.isEmpty()) {
			return new Date();
		}
		Calendar aCalandar = Calendar.getInstance();
		for (Integer field : dateShift.keySet()) {
			Integer shift = dateShift.get(field);
			aCalandar.add(field, shift);
		}
		return aCalandar.getTime();
	}

	public static long computeTimeShiftMS(Map<Integer, Integer> dateShift) {
		if(!activateDateShift) {
			return 0;
		}
		if(dateShift == null || dateShift.isEmpty()) {
			return 0;
		}
		Calendar aCalandar = Calendar.getInstance();
		long current = aCalandar.getTimeInMillis();
		for (Integer field : dateShift.keySet()) {
			Integer shift = dateShift.get(field);
			aCalandar.add(field, shift);
		}
		return aCalandar.getTimeInMillis() - current;
	}

	public static void updateDateShifts(Map<Integer, Integer> dateShifts, Date currentDate, List<Integer> fields) {
		for (Integer nextField : fields) {
			updateDateShifts(dateShifts, currentDate, nextField);
		}
	}

	public static void updateDateShifts(Map<Integer, Integer> dateShifts, Date currentDate, Integer field) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int defaultValue = calendar.get(field);
		calendar.setTime(currentDate);
		int value = calendar.get(field);
		int shift = value - defaultValue;
		if (shift == 0) {
			dateShifts.remove(field);
		} else {
			dateShifts.put(field, shift);
		}
	}

	public static String formatTimeOrDate(Date aDate) {
		if (aDate == null) {
			return "";
		}
		String currentDay = format_day.format(new Date());
		if (currentDay.equals(format_day.format(aDate))) {
			return format_time.format(aDate);
		} else {
			return format_date_time.format(aDate);
		}
	}

	public static int getHourOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	public static Date getCurrentSeconde(long timeShiftMS) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(new Date().getTime() + timeShiftMS);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public static Date getCurrentMinute(long timeShiftMS) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(new Date().getTime() + timeShiftMS);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public static Date getNextMinute(long timeShiftMS) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(new Date().getTime() + timeShiftMS);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		// shift to next minute
		cal.add(Calendar.MINUTE, 1);
		return cal.getTime();
	}

	public static Date getCurrentHour() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date result = new Date(cal.getTimeInMillis());
		return result;
	}


	public static Date removeTime(Date aDate) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(aDate);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		Date result = new Date(calendar.getTimeInMillis());
		return result;
	}

	public static Date removeMinutes(Date aDate) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(aDate);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		Date result = new Date(calendar.getTimeInMillis());
		return result;
	}

	public static Date getSpecificTime(Date dayDate, int hours, int minutes) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dayDate);
		calendar.set(Calendar.HOUR_OF_DAY, hours);
		calendar.set(Calendar.MINUTE, minutes);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		//Date result = calendar.getTime();
		return calendar.getTime();
	}

	public static Date getCeilHourStart(Date aDate) {
		long dateMS = aDate.getTime();
		long restMS = dateMS % MS_IN_HOUR;
		if (restMS > 0) {
			dateMS = dateMS - restMS + MS_IN_HOUR;
		}
		return new Date(dateMS);
	}


	/**
	 * Shifts the given Date to the same time at the next sconds. This uses the
	 * current time zone.
	 */
	public static Date shiftDateSec(Date d, double nbSeconds) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.SECOND, (int) nbSeconds);
		Date result = new Date(c.getTimeInMillis());
		return result;
	}

	/**
	 * Shifts the given Date to the same time at the next minute. This uses the
	 * current time zone.
	 */
	public static Date shiftDateMinutes(Date d, double nbMinutes) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.MINUTE, (int) nbMinutes);
		Date result = new Date(c.getTimeInMillis());
		return result;
	}

	public static Date shiftDateMonth(Date d, double nbMonth) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.MONTH, (int) nbMonth);
		Date result = new Date(c.getTimeInMillis());
		return result;
	}

	public static Date shiftDateDays(Date d, double nbDay) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.DATE, (int) nbDay);
		Date result = new Date(c.getTimeInMillis());
		return result;
	}

	public static double computeDurationSeconds(Date beginDate, Date endDate) {
		double deltaMS = endDate.getTime() - beginDate.getTime();
		return deltaMS / 1000;
	}

	public static double computeDurationMinutes(Date beginDate, Date endDate) {
		double deltaMS = endDate.getTime() - beginDate.getTime();
		return deltaMS / MS_IN_MINUTE;
	}

	public static double computeDurationHours(Date beginDate, Date endDate) {
		long deltaMS = endDate.getTime() - beginDate.getTime();
		return deltaMS / MS_IN_HOUR;
	}


}
