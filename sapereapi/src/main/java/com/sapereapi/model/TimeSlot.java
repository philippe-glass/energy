package com.sapereapi.model;

import java.util.Date;

import com.sapereapi.util.UtilDates;

public class TimeSlot {
	private Date beginDate;
	private Date endDate;

	public TimeSlot() {
		super();
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public TimeSlot(Date beginDate, Date enDate) {
		super();
		this.beginDate = beginDate;
		this.endDate = enDate;
	}

	public boolean isInSlot(Date aDate) {
		return !aDate.before(beginDate) && aDate.before(endDate);
	}

	public boolean hasExpired(Date aDate) {
		return !aDate.before(endDate);
	}

	@Override
	public String toString() {
		return "TimeSlot [" + UtilDates.format_time.format(beginDate) + " - " + UtilDates.format_time.format(endDate)
				+ "]";
	}


	@Override
	public int hashCode() {
		long timeBegin = beginDate.getTime();
		long timeEnd = endDate.getTime();
		long delta = timeBegin - timeEnd;
		return (int) ((timeBegin/1000) + delta);
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof TimeSlot)) {
			return false;
		}
		TimeSlot other = (TimeSlot) obj;
		return this.beginDate.equals(other.getBeginDate())
				&& this.endDate.equals(other.getEndDate());
	}

	public TimeSlot clone() {
		return new TimeSlot(beginDate, endDate);
	}
}
