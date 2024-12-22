package com.sapereapi.model.energy;

import java.util.Date;
import java.util.List;

import com.sapereapi.model.energy.pricing.ComposedRate;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.prosumerflow.ProsumerEnergySupply;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class EnergySupply extends EnergyFlow implements IEnergySupply {
	private static final long serialVersionUID = 1L;
	protected Long eventId;
	protected PricingTable pricingTable;

	public final static double DEFAULT_POWER_MARGIN_RATIO = 1.0 * 0.05;

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

	public EnergySupply(ProsumerProperties issuerProperties, Boolean isComplementary, PowerSlot powerSlot,
			Date beginDate, Date endDate, PricingTable _pricingTable) {
		super(issuerProperties, isComplementary, powerSlot, beginDate, endDate);
		this.pricingTable = _pricingTable;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("[evId:").append(eventId).append("] ");
		result.append(getIssuer());
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

	public EnergySupply copy(boolean copyIds) {
		PricingTable copyPricingTable = pricingTable==null ? null : pricingTable.copy(copyIds);
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(copyIds);
		EnergySupply result = new EnergySupply(cloneIssuerProperties
				, isComplementary, getPowerSlot()
				,beginDate == null ? null : new Date(beginDate.getTime())
				,endDate == null ? null : new Date(endDate.getTime())
				, copyPricingTable);
		if(copyIds) {
			result.setEventId(eventId);
		}
		return result;
	}

	@Override
	public EnergySupply clone() {
		return copy(true);
	}

	@Override
	public EnergySupply copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	public EnergyRequest generateRequest() {
		// Create energy request with the default values
		double delayToleranceMinutes = UtilDates.computeDurationMinutes(getBeginDate(), getEndDate());
		//PricingTable clonePricingTable = (pricingTable == null) ? null : pricingTable.clone();
		EnergyRequest request = new EnergyRequest(issuerProperties.clone(), isComplementary, getPowerSlot(), beginDate, endDate
				, delayToleranceMinutes, PriorityLevel.LOW);
		return request;
	}

	public boolean hasChanged(EnergySupply newContent) {
		if (newContent == null) {
			return true;
		}
		String issuer = getIssuer();
		String newContentIssuer = newContent.getIssuer();
		if (!issuer.equals(newContentIssuer)) {
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
		Date longDate = UtilDates.shiftDateDays(getCurrentDate(), 1000 );
		if (!this.endDate.equals(newContent.getEndDate()) && endDate.before(longDate)) {
			return true;
		}
		return false;
	}

	public EnergySupply aggregate1(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication, AbstractLogger logger) {
		// TODO Auto-generated method stub
		return null;
	}

	public ProsumerEnergySupply generateProsumerEnergySupply() {
		ProsumerProperties cloneIssuerProperties = issuerProperties == null ? null : issuerProperties.clone();
		PricingTable clonePricingTable = pricingTable == null ? null : pricingTable.clone();
		return new ProsumerEnergySupply(cloneIssuerProperties, isComplementary, getPowerSlot(), beginDate, endDate, null, clonePricingTable);
	}
	/*
	public void completeLocationId(Map<String, NodeLocationg> mapNeighborNodeLocations) {
		if(issuerLocation.getId() == null) {
			String nodeName = issuerLocation.getName();
			NodeLocation correctionLocation = mapNeighborNodeLocations.get(nodeName);
			issuerLocation.setId(correctionLocation.getId());
		}
	}*/

	/*
	public EnergyEvent generateEvent(EventType type, String comment) {
		ProsumerProperties cloneIssuerProperties = issuerProperties == null ? null : issuerProperties.clone();
		PricingTable clonePricingTable = pricingTable == null ? null : pricingTable.clone();
		EnergyEvent event = new EnergyEvent(type, cloneIssuerProperties, isComplementary, power, powerMin, powerMax,
				beginDate, endDate, clonePricingTable, comment);
		return event;
	}
	*/

}