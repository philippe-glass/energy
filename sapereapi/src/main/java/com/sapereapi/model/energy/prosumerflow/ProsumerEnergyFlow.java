package com.sapereapi.model.energy.prosumerflow;

import java.util.Date;
import java.util.List;


import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.AbstractEnergyFlow;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.StorageSupply;
import com.sapereapi.model.energy.IEnergyObject;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public abstract class ProsumerEnergyFlow extends AbstractEnergyFlow implements IEnergyObject, Cloneable  {
	private static final long serialVersionUID = 1L;
	protected PowerSlot internalPowerSlot;
	//protected Double internalPower; // current electric power in watts
	//protected Double internalPowerMin;
	//protected Double internalPowerMax;
	protected StorageSupply storageSupply;

	public Double getInternalPower() {
		return internalPowerSlot.getCurrent();
	}

	public Double getInternalPowerMin() {
		return internalPowerSlot.getMin();
	}

	public Double getInternalPowerMax() {
		return internalPowerSlot.getMax();
	}

	public Double getAdditionalPower() {
		return storageSupply == null ? 0 : storageSupply.getCurrentPower();
	}

	public PowerSlot getAdditionalPowerSlot() {
		return storageSupply == null ? null : PowerSlot.create(storageSupply.getCurrentPower());
	}

	public boolean hasAdditionalPower() {
		return storageSupply != null && storageSupply.getCurrentPower() > 0;
	}

	public StorageSupply getStorageSupply() {
		return storageSupply;
	}

	public void setStorageSupply(StorageSupply storageSupply) {
		this.storageSupply = storageSupply;
	}

	public boolean updateStorageSupply(StorageSupply storageSupply, boolean updateBeginDate) {
		double additionalBefore = getAdditionalPower();
		this.storageSupply = storageSupply;
		if(updateBeginDate) {
			double additionalAfter = getAdditionalPower();
			if(Math.abs(additionalAfter - additionalBefore) > 0.0001) {
				// change in additional supply : update the begin date
				this.beginDate = getCurrentDate();
				return true;
			}
		}
		return false;
	}

	public Double getPowerMargin() {
		return internalPowerSlot.getMargin();
	}

	public PowerSlot getInternalPowerSlot() {
		return internalPowerSlot;
	}

	public void setInternalPowerSlot(PowerSlot internalPowerSlot) {
		this.internalPowerSlot = internalPowerSlot;
	}

	public void removeInactiveStorageSupply() {
		if (storageSupply != null && !storageSupply.isActive()) {
			storageSupply = null;
		}
	}

	protected Double getInternalWH() {
		Double durationHours = UtilDates.computeDurationHours(beginDate, endDate);
		Double result = durationHours * getInternalPower();
		return result;
	}

	protected Double getRemainInternalWH() {
		Double durationHours = UtilDates.computeDurationHours(getCurrentDate(), endDate);
		Double result = durationHours * getInternalPower();
		return result;
	}

	public abstract Double getWH();

	public PowerSlot getForcastPowerSlot(Date aDate) {
		if (this.isInActiveSlot(aDate)) {
			return getPowerSlot();
		}
		return new PowerSlot();
	}

	public ProsumerEnergyFlow(ProsumerProperties _issuerProperties, Boolean _isComplementary
			, PowerSlot internalPowerSlot, Date beginDate, Date endDate, StorageSupply storageSupply, Boolean disabled) {
		super();
		this.issuerProperties = _issuerProperties;
		this.internalPowerSlot = internalPowerSlot;
		this.storageSupply = storageSupply;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.isComplementary = _isComplementary;
		this.disabled = disabled;
		try {
			checkPowers();
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
	}

	public void checkPowers() throws HandlingException {
		// Checkup
		if (SapereUtil.roundPower(internalPowerSlot.getMin()) > SapereUtil.roundPower(internalPowerSlot.getMax())) {
			throw new HandlingException("EnergySupply constructor : power min " + UtilDates.df3.format(internalPowerSlot.getMin())
					+ " cannot be higher  than powerMax " + UtilDates.df3.format(internalPowerSlot.getMax()));
		}
		if (SapereUtil.roundPower(internalPowerSlot.getCurrent()) > SapereUtil.roundPower(internalPowerSlot.getMax())) {
			throw new HandlingException("EnergySupply constructor : power  " + UtilDates.df3.format(internalPowerSlot.getCurrent())
					+ " cannot be higher than powerMax " + UtilDates.df3.format(internalPowerSlot.getMax()));
		}
		if (SapereUtil.roundPower(internalPowerSlot.getCurrent()) < SapereUtil.roundPower(internalPowerSlot.getMin())) {
			throw new HandlingException("EnergySupply constructor : power  " + UtilDates.df3.format(internalPowerSlot.getCurrent())
					+ " cannot be lower than powerMin " + UtilDates.df3.format(internalPowerSlot.getMin()));
		}
	}

	public int comparePower(ProsumerEnergyFlow other) {
		double auxPowerDifference = this.getPower() - other.getPower();
		return (int) (1000*auxPowerDifference);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getIssuer());
		if (this.isComplementary()) {
			result.append(" [COMPLEMENTARY] ");
		}
		result.append(" : ");
		result.append(UtilDates.df3.format(getPower())).append(" W from ");
		result.append(UtilDates.formatTimeOrDate(beginDate, getTimeShiftMS()));
		result.append(" to ");
		result.append(UtilDates.formatTimeOrDate(endDate, getTimeShiftMS()));
		if (this.disabled) {
			result.append("# DISABLED #");
		}
		return result.toString();
	}

	public boolean hasChanged(ProsumerEnergyFlow newContent) {
		if (super.hasChanged(newContent)) {
			return true;
		}
		if (Math.abs(getInternalPower() - newContent.getInternalPower()) > 0.0001) {
			return true;
		}
		if (Math.abs(getInternalPowerMin() - newContent.getInternalPowerMin()) > 0.0001) {
			return true;
		}
		if (Math.abs(getInternalPowerMax() - newContent.getInternalPowerMax()) > 0.0001) {
			return true;
		}
		if(storageSupply == null && newContent.getStorageSupply() != null) {
			return true;
		}
		if(storageSupply != null && storageSupply.hasChanged(newContent.getStorageSupply())) {
			return true;
		}
		return false;
	}

	public ProsumerEnergyFlow aggregate1(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		// TODO Auto-generated method stub
		return null;
	}

	public EnergyEvent generateEvent(EventType type, String _comment) {
		EnergyEvent event = new EnergyEvent(type, issuerProperties, isComplementary, getPowerSlot(), beginDate, endDate, _comment, 0.0);
		event.setAdditionalPower(getAdditionalPower());
		return event;
	}
}