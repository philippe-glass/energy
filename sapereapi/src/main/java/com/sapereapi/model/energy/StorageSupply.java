package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class StorageSupply implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	protected Double power; // current electric power in watts
	protected Date beginDate;
	protected Date endDate;
	protected long timeShiftMS;

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
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

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public StorageSupply(Double power, Date beginDate, Date endDate, long timeShiftMS) {
		super();
		this.power = power;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.timeShiftMS = timeShiftMS;
	}

	public Double getKWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		return (durationHours * power) / 1000;
	}

	public Double getWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		return (durationHours * power);
	}

	public Double getRemainWH() {
		Date current = getCurrentDate();
		double remainHours = UtilDates.computeDurationHours(current, endDate);
		return (remainHours * power);
	}

	public boolean isActive() {
		Date current = getCurrentDate();
		return (!current.before(beginDate)) && current.before(this.endDate);
	}

	public double getCurrentPower() {
		if (this.isActive()) {
			return power;
		}
		return 0;
	}

	public StorageSupply clone() {
		return new StorageSupply(power, beginDate, endDate, this.timeShiftMS);
	}

	public Date getCurrentDate() {
		return UtilDates.removeMilliSeconds(UtilDates.getNewDate(getTimeShiftMS()));
	}

	public boolean hasChanged(StorageSupply newContent) {
		if (newContent == null) {
			return true;
		}
		Date longDate = UtilDates.shiftDateDays(getCurrentDate(), 1000);
		if (!this.endDate.equals(newContent.getEndDate()) && endDate.before(longDate)) {
			return true;
		}
		if (Math.abs(power - newContent.getPower()) > 0.0001) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("StorageSupply : {").append(SapereUtil.roundPower(power))
			.append(" W from ").append(UtilDates.format_time.format(beginDate)).append(" to ").append(UtilDates.format_time.format(endDate)).append("}");
		return result.toString();
	}
}
