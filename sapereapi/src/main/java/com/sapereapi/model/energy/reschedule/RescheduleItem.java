package com.sapereapi.model.energy.reschedule;

import java.util.Date;

import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.UtilDates;

public class RescheduleItem {
	private String agentName;
	private Date stopBegin;
	private Date stopEnd;
	private double power;
	private WarningType warningType;
	protected Long timeShiftMS;

	public RescheduleItem(String agentName, WarningType warningType, Date stopBegin, Date stopEnd, double power, long _timeShiftMS) {
		super();
		this.agentName = agentName;
		this.warningType = warningType;
		this.stopBegin = stopBegin;
		this.stopEnd = stopEnd;
		this.power = power;
		this.timeShiftMS = _timeShiftMS;
	}
	public String getAgentName() {
		return agentName;
	}
	public Date getStopBegin() {
		return stopBegin;
	}
	public Date getStopEnd() {
		return stopEnd;
	}
	public double getPower() {
		return power;
	}
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	public void setStopBegin(Date stopBegin) {
		this.stopBegin = stopBegin;
	}
	public void setStopEnd(Date stopEnd) {
		this.stopEnd = stopEnd;
	}
	public void setPower(double power) {
		this.power = power;
	}
	public WarningType getWarningType() {
		return warningType;
	}
	public void setWarningType(WarningType warningType) {
		this.warningType = warningType;
	}
	public boolean hasExpired() {
		Date current = getCurrentDate();
		return current.after(stopEnd);
	}
	public boolean hasExpired(int marginSec) {
		Date current = getCurrentDate();
		return current.after(UtilDates.shiftDateSec(stopEnd,marginSec));
	}

	@Override
	public RescheduleItem clone() {
		RescheduleItem result = new RescheduleItem(agentName, warningType, stopBegin, stopEnd, power, timeShiftMS);
		return result;
	}

	public boolean isInSlot(Date aDate) {
		return !aDate.before(stopBegin) && aDate.before(stopEnd);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("Reschedule ");
		result.append(this.agentName).append("(");
		result.append(UtilDates.df3.format(power)).append("W) from ");
		result.append(UtilDates.formatTimeOrDate(stopBegin, timeShiftMS));
		result.append(" to ");
		result.append(UtilDates.formatTimeOrDate(stopEnd, timeShiftMS));
		return result.toString();
		/*
		return "RescheduleItem [agentName=" + agentName + ", stopBegin=" + stopBegin + ", stopEnd=" +   stopEnd
				+ ", power=" + power + ", warningType=" + warningType + "]";
		*/
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}
}
