package com.sapereapi.model.learning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

//import simulation.utilities.structures.Array;

public class TimeWindow implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private Set<Integer> daysOfWeek = null;
	private int startHour;
	private int startMinute;
	private int endHour;
	private int endMinute;

	public TimeWindow() {
		super();
	}

	public TimeWindow(int id, Set<Integer> daysOfWeek, int startHour, int startMinute, int endHour,
			int endMinute) {
		super();
		this.id = id;
		this.daysOfWeek = daysOfWeek;
		this.startHour = startHour;
		this.startMinute = startMinute;
		this.endHour = endHour;
		this.endMinute = endMinute;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Integer> getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(Set<Integer> daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}

	public int getStartHour() {
		return startHour;
	}

	public void setStartHour(int startHour) {
		this.startHour = startHour;
	}

	public int getStartMinute() {
		return startMinute;
	}

	public void setStartMinute(int startMinute) {
		this.startMinute = startMinute;
	}

	public int getEndHour() {
		return endHour;
	}

	public void setEndHour(int endHour) {
		this.endHour = endHour;
	}

	public int getEndMinute() {
		return endMinute;
	}

	public void setEndMinute(int endMinute) {
		this.endMinute = endMinute;
	}

	public int getDayOfWeek(Date aDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(aDate);
		// cal.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
		return cal.get(Calendar.DAY_OF_WEEK); // 0 : sunday, 1:monday ....
	}

	public int getCurrentDayOfWeek(long timeShiftMS) {
		Date current = UtilDates.getNewDate(timeShiftMS);
		return getDayOfWeek(current);
	}

	public Date getStartDate(long timeShiftMS) {
		Date current = UtilDates.getNewDate(timeShiftMS);
		return getStartDate(current);
	}

	public Date getEndDate(long timeShiftMS) {
		Date current = UtilDates.getNewDate(timeShiftMS);
		return getEndDate(current);
	}

	public Date getStartDate(Date aDay) {
		int dayOfWeek = getDayOfWeek(aDay);
		if (this.daysOfWeek.contains(dayOfWeek)) {
			return UtilDates.getSpecificTime(aDay, startHour, startMinute);
		}
		return null;
	}

	public Date getEndDate(Date aDay) {
		int dayOfWeek = getDayOfWeek(aDay);
		if (this.daysOfWeek.contains(dayOfWeek)) {
			return UtilDates.getSpecificTime(aDay, endHour, endMinute);
		}
		return null;
	}

	public boolean containsDate(Date aDate) {
		// int dayOfWeek = getDayOfWeek(aDate);
		Date dateMin = getStartDate(aDate);
		if (dateMin != null) {
			Date dateMax = getEndDate(aDate);
			if (dateMax != null) {
				return !aDate.before(dateMin) && aDate.before(dateMax);
			}
		}
		return false;
	}

	public boolean isCurrent(long timeShiftMS) {
		Date current = UtilDates.getNewDate(timeShiftMS);
		return containsDate(current);
	}

	private String formatDaysOfWeek() {
		List<String> slots = new ArrayList<String>();
		int startSlot = -1;
		int endSlot = -1;
		String currentSlot = "";
		for (int i = 0; i <= 7; i++) {
			boolean idDanysOfWeek = this.daysOfWeek.contains(i);
			if (idDanysOfWeek) {
				if (startSlot < 1) {
					startSlot = i;
				}
				endSlot = i;
				currentSlot = startSlot + "-" + endSlot;
			} else {
				startSlot = -1;
				if (currentSlot.length() > 0) {
					slots.add(currentSlot);
				}
				currentSlot = "";
			}
		}
		if (currentSlot.length() > 0 && !slots.contains(currentSlot)) {
			slots.add(currentSlot);
		}
		return SapereUtil.implode(slots, ",");
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(String.format("%02d", startHour)).append(":").append(String.format("%02d", startMinute));
		result.append("-");
		result.append(String.format("%02d", endHour)).append(":").append(String.format("%02d", endMinute));
		result.append(" - dow:").append(formatDaysOfWeek());
		return result.toString();
	}

	public static TimeWindow getTimeWindow(List<TimeWindow> listTimeWindows, Date aDate) {
		for (TimeWindow timeWindow : listTimeWindows) {
			if (timeWindow.containsDate(aDate)) {
				return timeWindow;
			}
		}
		return null;
	}
}
