package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.model.TimeSlot;

import eu.sapere.middleware.lsa.IPropertyObject;

public interface IEnergyObject extends IPropertyObject {
	public ProsumerProperties getIssuerProperties();

	public String getIssuer();

	public Date getBeginDate();

	public void setBeginDate(Date aDate);

	public Date getEndDate();

	public void setEndDate(Date aDate);

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

	public long getTimeShiftMS();

	public Date getCurrentDate();

	public boolean isInActiveSlot(Date aDate);

	public PowerSlot getForcastPowerSlot(Date aDate);
}
