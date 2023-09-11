package com.sapereapi.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class UtilDates {
	public static TimeZone timezone = TimeZone.getTimeZone("GMT");
	public static SimpleDateFormat format_date_time = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
	public static SimpleDateFormat format_time = new SimpleDateFormat("HH:mm:ss");
	public static SimpleDateFormat format_sql = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static SimpleDateFormat format_sql_day = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat format_day = new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat format_sessionid = new SimpleDateFormat("yyyyMMdd_HHmmss");
	public static SimpleDateFormat format_json_datetime_prev = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static SimpleDateFormat format_json_datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

	static {
		format_json_datetime.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		/*
		format_json_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		String test1 = format_json_datetime.format(new Date());
		String test = "2021-09-04 14:08:00+0000";
		try {
			Date testDate = format_json_datetime.parse(test);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public static TimeZone getTimezone() {
		return timezone;
	}

	public static void setTimezone(TimeZone _timezone) {
		timezone = _timezone;
		format_date_time.setTimeZone(timezone);
		format_time.setTimeZone(timezone);
		format_sql.setTimeZone(timezone);
		format_sql_day.setTimeZone(timezone);
		format_day.setTimeZone(timezone);
		format_sessionid.setTimeZone(timezone);
		format_json_datetime_prev.setTimeZone(timezone);
		format_json_datetime.setTimeZone(timezone);
		Date date = new Date();
		String sDate = format_json_datetime.format(date);
		System.out.print(sDate);
	}

	public final static String CR = System.getProperty("line.separator"); // Cariage return

	public final static DecimalFormat df = new DecimalFormat("#.##");
	public final static DecimalFormat df2 = new DecimalFormat("#.#####");
	public final static DecimalFormat df3 = new DecimalFormat("#.###");

	public final static int MS_IN_MINUTE = 1000 * 60;
	public final static int MS_IN_HOUR = 1000 * 60 * 60;
	public final static int MS_IN_DAY = 1000 * 60 * 60 * 24;

	public static boolean activateDateShift = true;

	private static boolean shiftTimeZones = true;
	public static Map<String,String> mapMonth = new HashMap<>();
	static {
		String[] list_months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
		int monthIdx = 1;
		for(String month : list_months) {
			String smonth = ""+monthIdx;
			if(smonth.length()<2) {
				smonth = "0"+smonth;
			}
			mapMonth.put(month, smonth);
			monthIdx++;
		}
	}

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
		/*
		Calendar calendar = Calendar.getInstance();
		Date bidon1 = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, -187);
		Date bidon2 = calendar.getTime();
		*/
		long timeShiftMS = computeTimeShiftMS(dateShift);
		return getNewDate(timeShiftMS);
	}

	public static long computeTimeZoneShift(Date date1, Date date2) {
		Calendar aCalandar = Calendar.getInstance();
		TimeZone shiftedTimeZone = aCalandar.getTimeZone();
		long tzOffset1 = shiftedTimeZone.getOffset(date1.getTime());
		long tzOffset2 = shiftedTimeZone.getOffset(date2.getTime());
		return tzOffset1 - tzOffset2;
	}

	public static long computeTimeShiftMS(Date date1, Date date2) {
		long tzShift = computeTimeZoneShift(date1, date2);
		return date1.getTime() - date2.getTime() +  tzShift;
	}

	public static long computeTimeShiftMS(Map<Integer, Integer> dateShift) {
		if(!activateDateShift) {
			return 0;
		}
		if(dateShift == null || dateShift.isEmpty()) {
			return 0;
		}
		Calendar aCalandar = Calendar.getInstance();
		Date testCurrent = aCalandar.getTime();
		TimeZone currentTimeZone = aCalandar.getTimeZone();
		long tzOffset1 = currentTimeZone.getOffset(testCurrent.getTime());
		long tzOffset1H = tzOffset1/3600000;
		long current = aCalandar.getTimeInMillis();
		for (Integer field : dateShift.keySet()) {
			Integer shift = dateShift.get(field);
			aCalandar.add(field, shift);
		}
		Date testShiftedDate = aCalandar.getTime();
		TimeZone shiftedTimeZone = aCalandar.getTimeZone();
		long tzOffset2 = shiftedTimeZone.getOffset(testShiftedDate.getTime());
		long tzOffset2H = tzOffset2/3600000;
		long deltaHour = tzOffset2H - tzOffset1H;
		if(deltaHour != 0 && shiftTimeZones) {
			int deltaHourInt = (int) deltaHour;
			aCalandar.add(Calendar.HOUR, -1*deltaHourInt);
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

	public static String formatTimeOrDate(Date aDate, long timeShiftMS) {
		if (aDate == null) {
			return "";
		}
		String currentDay = format_day.format(getNewDate(timeShiftMS));
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
	 * Shifts the given Date to the same time at the next seconds. This uses the
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
