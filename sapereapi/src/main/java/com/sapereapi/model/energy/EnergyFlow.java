package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public abstract class EnergyFlow extends AbstractEnergyFlow implements IEnergyObject, Cloneable {
	private static final long serialVersionUID = 1L;
	protected PowerSlot powerSlot;	// current electric power slot  in watts

	//protected Double power; 	// current electric power in watts
	//protected Double powerMin;	// minimal electric power in watts
	//protected Double powerMax;	// maximal electric power in watts

	public Double getPower() {
		return powerSlot.getCurrent();
	}

	public void setPowerSlot(PowerSlot powerSlot) {
		this.powerSlot = powerSlot;
	}

	public Double getPowerMin() {
		return powerSlot.getMin();
	}

	public Double getPowerMax() {
		return powerSlot.getMax();
	}

	public Double getPowerMargin() {
		return powerSlot.getMargin(); //powerMax - power;
	}

	public void setIsComplementary(Boolean complementary) {
		this.isComplementary = complementary;
	}

	public Double getKWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		return (durationHours * powerSlot.getCurrent()) / 1000;
	}

	public Double getWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		return (durationHours * powerSlot.getCurrent());
	}

	public PowerSlot getPowerSlot() {
		return powerSlot;// new PowerSlot(this.power, this.powerMin, this.powerMax);
	}

	public PowerSlot getForcastPowerSlot(Date aDate) {
		if(this.isInActiveSlot(aDate)) {
			return powerSlot.clone();
		}
		return new PowerSlot();
	}

	public EnergyFlow(ProsumerProperties _issuerProperties, Boolean _isComplementary, PowerSlot powerSlot,
			Date beginDate, Date endDate) {
		super();
		this.issuerProperties = _issuerProperties;
		this.powerSlot = powerSlot;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.isComplementary = _isComplementary;
		try {
			checkPowers();
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
	}

	public void checkPowers() throws HandlingException {
		// Checkup
		if(SapereUtil.roundPower(powerSlot.getMin()) > SapereUtil.roundPower(powerSlot.getMax())) {
			throw new HandlingException("EnergySupply constructor : power min " + UtilDates.df3.format(powerSlot.getMin()) + " cannot be higher  than powerMax " + UtilDates.df3.format(powerSlot.getMax()));
		}
		if(SapereUtil.roundPower(powerSlot.getCurrent()) > SapereUtil.roundPower(powerSlot.getMax())) {
			throw new HandlingException("EnergySupply constructor : power  " +  UtilDates.df3.format(powerSlot.getCurrent()) + " cannot be higher than powerMax " +  UtilDates.df3.format(powerSlot.getMax()));
		}
		if(SapereUtil.roundPower(powerSlot.getCurrent()) < SapereUtil.roundPower(powerSlot.getMin())) {
			throw new HandlingException("EnergySupply constructor : power  " +  UtilDates.df3.format(powerSlot.getCurrent()) + " cannot be lower than powerMin " +  UtilDates.df3.format(powerSlot.getMin()));
		}
	}

	public int comparePower(EnergyFlow other) {
		double auxPowerDifference = this.getPower() - other.getPower();
		return (int) (1000*auxPowerDifference);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(getIssuer());
		if(this.isComplementary()) {
			result.append(" [COMPLEMENTARY] ");
		}
		result.append(":");
		result.append(UtilDates.df3.format(powerSlot.getCurrent())).append(" W from ");
		result.append(UtilDates.formatTimeOrDate(beginDate, getTimeShiftMS()));
		result.append(" to ");
		result.append(UtilDates.formatTimeOrDate(endDate, getTimeShiftMS()));
		return result.toString();
	}


	@Override
	public abstract EnergyFlow clone();

	public boolean hasChanged(EnergyFlow newContent) {
		if(super.hasChanged(newContent)) {
			return true;
		}
		if(Math.abs(getPower() - newContent.getPower()) > 0.0001) {
			return true;
		}
		if(Math.abs(getPowerMin() -  newContent.getPowerMin()) > 0.0001) {
			return true;
		}
		if(Math.abs(getPowerMax() - newContent.getPowerMax()) > 0.0001) {
			return true;
		}
		return false;
	}

	public EnergyEvent generateEvent(EventType type, String comment) {
		ProsumerProperties cloneIssuerProperties = issuerProperties == null ? null : issuerProperties.clone();
		//PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		EnergyEvent event = new EnergyEvent(type, cloneIssuerProperties, isComplementary, getPowerSlot(),beginDate, endDate, comment, 0.0);
		return event;
	}

	public EnergySupply generateSupply() {
		PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(true);
		EnergySupply result = new EnergySupply(cloneIssuerProperties
				, isComplementary, getPowerSlot()
				,beginDate,endDate, pricingTable);
		return result;
	}

	public EnergyRequest generateRequest() {
		// Create energy request with the default values
		double delayToleranceMinutes = UtilDates.computeDurationMinutes(getBeginDate(), getEndDate());
		//PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		EnergyRequest request = new EnergyRequest(issuerProperties, isComplementary, getPowerSlot(), beginDate, endDate
				, delayToleranceMinutes, PriorityLevel.LOW);
		return request;
	}
}