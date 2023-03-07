package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.model.TimeSlot;

import eu.sapere.middleware.lsa.values.IAggregateable;

public interface IEnergyObject extends IAggregateable {
	public String getIssuer();

	public String getIssuerLocation();

	public int getIssuerDistance();

	public Date getBeginDate();

	public Date getEndDate();

	public Double getPower();

	public Double getPowerMin();

	public Double getPowerMax();

	public Double getPowerMargin();

	public PowerSlot getPowerSlot();

	public TimeSlot getTimeSlot();

	public Double getDuration();

	public boolean hasExpired();

	public boolean isActive();

	public boolean isComplementary();

	public IEnergyObject clone();

	public Long getTimeShiftMS();

	public Date getCurrentDate();

	public boolean isInActiveSlot(Date aDate);

	public PowerSlot getForcastPowerSlot(Date aDate);
}
