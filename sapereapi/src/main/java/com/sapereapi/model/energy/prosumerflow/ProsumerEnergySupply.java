package com.sapereapi.model.energy.prosumerflow;

import java.util.Date;
import java.util.List;

import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.IEnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.StorageSupply;
import com.sapereapi.model.energy.pricing.ComposedRate;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class ProsumerEnergySupply extends ProsumerEnergyFlow implements IEnergySupply {
	private static final long serialVersionUID = 1L;
	protected Long eventId;
	protected PricingTable pricingTable;

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public PricingTable getPricingTable() {
		return pricingTable;
	}

	public void setPricingTable(PricingTable pricingTable) {
		this.pricingTable = pricingTable;
	}

	public ComposedRate getRate(Date aDate) {
		if(pricingTable == null) {
			return null;
		}
		return pricingTable.getRate(aDate);
	}

	public ProsumerEnergySupply(ProsumerProperties issuerProperties, Boolean isComplementary,
			PowerSlot internalPowerSlot, Date beginDate, Date endDate, StorageSupply storageSupply,
			PricingTable _pricingTable, Boolean disabled) {
		super(issuerProperties, isComplementary, internalPowerSlot, beginDate, endDate, storageSupply, disabled);
		this.pricingTable = _pricingTable;
	}

	@Override
	public Double getPower() {
		return getInternalPower() + getAdditionalPower();
	}

	@Override
	public Double getPowerMin() {
		return getInternalPowerMin() +  getAdditionalPower();
	}

	@Override
	public Double getPowerMax() {
		return getInternalPowerMax() +  getAdditionalPower();
	}

	@Override
	public PowerSlot getPowerSlot() {
		PowerSlot result = internalPowerSlot.clone();
		result.add(getAdditionalPowerSlot());
		return result;
	}

	@Override
	public Double getWH() {
		Double result = getInternalWH();
		if (storageSupply != null) {
			result += this.storageSupply.getWH();
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("[evId:").append(eventId).append("] ");
		result.append(this.getIssuer());
		if(this.isComplementary()) {
			result.append(" [COMPLEMENTARY] ");
		}
		result.append(":");
		result.append(UtilDates.df3.format(getPower())).append(" W from ");
		result.append(UtilDates.formatTimeOrDate(beginDate, getTimeShiftMS()));
		result.append(" to ");
		result.append(UtilDates.formatTimeOrDate(endDate, getTimeShiftMS()));
		if(this.disabled) {
			result.append("# DISABLED #");
		}
		if(pricingTable != null && pricingTable.getSize() > 0) {
			result.append(" pricingTable:{").append(pricingTable).append("}");
		}
		return result.toString();
	}

	public ProsumerEnergySupply copy(boolean copyIds) {
		PricingTable copyPricingTable = pricingTable==null ? null : pricingTable.copy(copyIds);
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(copyIds);
		StorageSupply cloneStorageSupply = storageSupply == null ? null : storageSupply.clone();
		ProsumerEnergySupply result = new ProsumerEnergySupply(cloneIssuerProperties
				, isComplementary, getInternalPowerSlot()
				,beginDate == null ? null : new Date(beginDate.getTime())
				,endDate == null ? null : new Date(endDate.getTime())
				, cloneStorageSupply, copyPricingTable, disabled);
		result.setDisabled(disabled);
		if(copyIds) {
			result.setEventId(eventId);
		}
		return result;
	}

	@Override
	public ProsumerEnergySupply clone() {
		return copy(true);
	}

	@Override
	public ProsumerEnergySupply copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	public ProsumerEnergyRequest generateRequest() {
		// Create energy request with the default values
		double delayToleranceMinutes = UtilDates.computeDurationMinutes(getBeginDate(), getEndDate());
		StorageSupply cloneStorageSupply = (this.storageSupply == null) ? null : storageSupply.clone();
		//PricingTable clonePricingTable = (pricingTable == null) ? null : pricingTable.clone();
		ProsumerEnergyRequest request = new ProsumerEnergyRequest(issuerProperties.clone(), isComplementary, getInternalPowerSlot()
				, beginDate, endDate, cloneStorageSupply
				, delayToleranceMinutes, PriorityLevel.LOW, disabled);
		return request;
	}

	public EnergySupply generateSimpleSupply() {
		ProsumerProperties cloneIssuerProperties = issuerProperties.clone();
		PricingTable copyPricingTable = pricingTable==null ? null : pricingTable.clone();
		EnergySupply result = new EnergySupply(cloneIssuerProperties, isComplementary, getPowerSlot(),
				beginDate, endDate, copyPricingTable, disabled);
		result.setEventId(eventId);
		//result.setDisabled(disabled);
		return result;
	}

	public boolean hasChanged(ProsumerEnergySupply newContent) {
		if(super.hasChanged(newContent)) {
			return true;
		}
		if (newContent == null) {
			return true;
		}
		String issuer = getIssuer();
		String newContentIssuer = newContent.getIssuer();
		if (!issuer.equals(newContentIssuer)) {
			return true;
		}
		Date longDate = UtilDates.shiftDateDays(getCurrentDate(), 1000 );
		if (!this.endDate.equals(newContent.getEndDate()) && endDate.before(longDate)) {
			return true;
		}
		return false;
	}

	public ProsumerEnergySupply aggregate1(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication, AbstractLogger logger) {
		// TODO Auto-generated method stub
		return null;
	}



}