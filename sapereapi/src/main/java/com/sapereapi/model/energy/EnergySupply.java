package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class EnergySupply implements IEnergyObject, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	protected String issuer;
	protected String issuerLocation;
	protected int issuerDistance;
	protected Double power; 	// current electric power in watts
	protected Double powerMin;	// minimal electric power in watts
	protected Double powerMax;	// maximal electric power in watts
	protected Date beginDate;
	protected Date endDate;
	protected Long eventId;
	protected DeviceProperties deviceProperties;
	protected PricingTable pricingTable;
	protected Boolean disabled = Boolean.FALSE;
	protected Boolean isComplementary = Boolean.FALSE;
	protected Long timeShiftMS;

	public final static double DEFAULT_POWER_MARGIN_RATIO = 1.0 * 0.05;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getIssuerLocation() {
		return issuerLocation;
	}

	public void setIssuerLocation(String issuerLocation) {
		this.issuerLocation = issuerLocation;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double _power) {
		this.power = _power;
	}

	public Double getPowerMin() {
		return powerMin;
	}

	public void setPowerMin(Double powerMin) {
		this.powerMin = powerMin;
	}

	public Double getPowerMax() {
		return powerMax;
	}

	public void setPowerMax(Double powerMax) {
		this.powerMax = powerMax;
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

	public Double getPowerMargin() {
		return powerMax - power;
	}

	public boolean isComplementary() {
		return isComplementary;
	}

	public boolean getIsComplementary() {
		return isComplementary;
	}

	public boolean isMain() {
		return !isComplementary;
	}

	public boolean getIsMain() {
		return !isComplementary;
	}

	public void setIsComplementary(Boolean complementary) {
		this.isComplementary = complementary;
	}

	public Double getDuration() {
		double duration = UtilDates.computeDurationMinutes(beginDate, endDate);
		return duration;
	}

	public Double getKWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		return (durationHours * power) / 1000;
	}

	public Double getWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		return (durationHours * power);
	}

	public void checkBeginNotPassed() {
		Date current = getCurrentDate();
		if (current.after(beginDate)) {
			beginDate = current;
		}
		if(beginDate.after(endDate)) {
			SapereLogger.getInstance().error("checkBeginNotPassed : begin date is after end date");
		}
	}

	public boolean isStartInFutur() {
		Date current = getCurrentDate();
		return current.before(beginDate);
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public DeviceProperties getDeviceProperties() {
		return deviceProperties;
	}

	public void setDeviceProperties(DeviceProperties deviceProperties) {
		this.deviceProperties = deviceProperties;
	}

	public int getIssuerDistance() {
		return issuerDistance;
	}

	public void setIssuerDistance(int issuerDistance) {
		this.issuerDistance = issuerDistance;
	}

	public PricingTable getPricingTable() {
		return pricingTable;
	}

	public void setPricingTable(PricingTable pricingTable) {
		this.pricingTable = pricingTable;
	}

	public Double getRate(Date aDate) {
		if(pricingTable == null) {
			return null;
		}
		return pricingTable.getRate(aDate);
	}


	public PowerSlot getPowerSlot() {
		return new PowerSlot(this.power, this.powerMin, this.powerMax);
	}

	public PowerSlot getForcastPowerSlot(Date aDate) {
		if(this.isInActiveSlot(aDate)) {
			return new PowerSlot(this.power, this.powerMin, this.powerMax);
		}
		return new PowerSlot();
	}

	public TimeSlot getTimeSlot() {
		return new TimeSlot(this.beginDate, this.endDate);
	}

	public Long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	@Override
	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}

	public EnergySupply(String _issuer, String _issuerLocation, Boolean _isComplementary, Double _power, Double _powerMin, Double _powerMax,Date beginDate, Date endDate,
			DeviceProperties _deviceProperties, PricingTable _pricingTable, Long _timeShiftMS) {
		super();
		this.issuer = _issuer;
		this.issuerLocation = _issuerLocation;
		this.power = _power;
		this.powerMin = (_powerMin==null || _powerMin==0.0)? _power : _powerMin;
		this.powerMax = (_powerMax==null || _powerMax==0.0)? _power : _powerMax;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.deviceProperties = _deviceProperties;
		//this.isComplementary = Boolean.FALSE;
		this.isComplementary = _isComplementary;
		this.timeShiftMS = _timeShiftMS;
		this.pricingTable = _pricingTable;
		try {
			checkPowers();
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
	}

	public void checkDates() throws Exception {
		if(this.beginDate.after(endDate)) {
			throw new Exception("EnergySupply checkDates " + this.issuer + " : beginDate " + UtilDates.formatTimeOrDate(beginDate) + " is after " + UtilDates.formatTimeOrDate(endDate));
		}
	}

	public void checkPowers() throws Exception {
		// Checkup
		if(SapereUtil.round(powerMin,3) > SapereUtil.round(powerMax,3)) {
			throw new Exception("EnergySupply constructor : power min " + UtilDates.df2.format(powerMin) + " cannot be higher  than powerMax " + UtilDates.df2.format(powerMax));
		}
		if(SapereUtil.round(power,3) > SapereUtil.round(powerMax,3)) {
			throw new Exception("EnergySupply constructor : power  " +  UtilDates.df2.format(power) + " cannot be higher than powerMax " +  UtilDates.df2.format(powerMax));
		}
		if(SapereUtil.round(power,3) < SapereUtil.round(powerMin,3)) {
			throw new Exception("EnergySupply constructor : power  " +  UtilDates.df2.format(power) + " cannot be lower than powerMin " +  UtilDates.df2.format(powerMin));
		}
	}

	public boolean hasExpired() {
		Date current = getCurrentDate();
		return !current.before(this.endDate);
	}

	public boolean isActive() {
		Date current = getCurrentDate();
		return (!current.before(beginDate)) && current.before(this.endDate);
	}

	public boolean isInActiveSlot(Date aDate) {
		return aDate != null && (!aDate.before(beginDate)) && aDate.before(this.endDate);
	}

	public boolean isBeginDateOK(Date maxBeginDate) {
		return !this.beginDate.after(maxBeginDate);
	}

	public long getTotalDurationSec() {
		return (long) UtilDates.computeDurationSeconds(beginDate, endDate);
	}

	public long getTotalDurationMS() {
		return endDate.getTime() - beginDate.getTime();
	}

	public long getTimeLeftMS(boolean addWaitingBeforeStart) {
		long currentMS = (getCurrentDate()).getTime();
		long beginMS = beginDate.getTime();
		if(addWaitingBeforeStart || (currentMS > beginMS))  {
			return Math.max(0, endDate.getTime() - currentMS);
		} else {
			return Math.max(0, endDate.getTime() - beginMS);
		}
	}

	public int getTimeLeftSec(boolean addWaitingBeforeStart) {
		long timeLeftMS = getTimeLeftMS(addWaitingBeforeStart);
		if (timeLeftMS > 0) {
			return (int) timeLeftMS / 1000;
		}
		return 0;
	}

	public int comparePower(EnergySupply other) {
		double powerDiff = this.power - other.getPower();
		return (int) (powerDiff);
	}

	public int comparTimeLeft(EnergySupply other) {
		return (this.getTimeLeftSec(false) - other.getTimeLeftSec(false));
	}

	public int compareDistance(EnergySupply other) {
		return this.issuerDistance - other.getIssuerDistance();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.issuer);
		if(this.isComplementary()) {
			result.append(" [COMPLEMENTARY] ");
		}
		result.append(":");
		result.append(UtilDates.df.format(power)).append(" W from ");
		result.append(UtilDates.formatTimeOrDate(beginDate));
		result.append(" to ");
		result.append(UtilDates.formatTimeOrDate(endDate));
		if(this.disabled) {
			result.append("# DISABLED #");
		}
		return result.toString();
	}

	@Override
	public EnergySupply clone() {
		PricingTable copyPricingTable = pricingTable==null ? null : pricingTable.clone();
		DeviceProperties cloneDeviceProperties = deviceProperties.clone();
		EnergySupply result = new EnergySupply(issuer, issuerLocation, isComplementary, power,powerMin, powerMax
				,beginDate == null ? null : new Date(beginDate.getTime())
				,endDate == null ? null : new Date(endDate.getTime())
				,cloneDeviceProperties, copyPricingTable, timeShiftMS);
		result.setEventId(eventId);
		//result.setIsComplementary(isComplementary);
		result.setIssuerDistance(issuerDistance);
		return result;
	}

	public EnergyRequest generateRequest() {
		// Create energy request with the default values
		double _delayToleranceMinutes = UtilDates.computeDurationMinutes(getBeginDate(), getEndDate());
		EnergyRequest request = new EnergyRequest(issuer, issuerLocation, isComplementary, power, powerMin, powerMax, beginDate, endDate
				, _delayToleranceMinutes, PriorityLevel.LOW, deviceProperties, pricingTable, timeShiftMS);
		//request.setIsComplementary(isComplementary);
		return request;
	}

	public boolean hasChanged(EnergySupply newContent) {
		if (newContent == null) {
			return true;
		}
		if (!this.issuer.equals(newContent.getIssuer())) {
			return true;
		}
		if(Math.abs(power - newContent.getPower()) > 0.0001) {
			return true;
		}
		if(Math.abs(powerMin -  newContent.getPowerMin()) > 0.0001) {
			return true;
		}
		if(Math.abs(powerMax - newContent.getPowerMax()) > 0.0001) {
			return true;
		}
		Date longDate = UtilDates.shiftDateDays(getCurrentDate(), 1000 );
		if (!this.endDate.equals(newContent.getEndDate()) && endDate.before(longDate)) {
			return true;
		}
		return false;
	}

	@Override
	public EnergySupply aggregate(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication) {
		// TODO Auto-generated method stub
		return null;
	}
}