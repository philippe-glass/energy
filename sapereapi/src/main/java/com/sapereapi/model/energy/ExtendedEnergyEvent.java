package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.model.referential.EventType;

import eu.sapere.middleware.node.NodeConfig;

public class ExtendedEnergyEvent extends EnergyEvent {
	private static final long serialVersionUID = 21L;
	private Date effectiveEndDate;
	private String linkedConsumer;
	private NodeConfig linkedConsumerLocation;

	public ExtendedEnergyEvent(EventType type, EnergySupply energySupply, String _comment) {
		super(type, energySupply, _comment);
	}

	public ExtendedEnergyEvent(EventType type, String _agent, NodeConfig _location, int _distance, Boolean _isComplementary, Double _power, Double _powerMin, Double _powerMax, Date beginDate, Date endDate, DeviceProperties deviceProperties, PricingTable pricingTable, String _comment, Long timeShiftMS) {
		super(type, _agent, _location, _distance, _isComplementary, _power, _powerMin, _powerMax, beginDate, endDate, deviceProperties, pricingTable, _comment, timeShiftMS);
	}

	public Date getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(Date effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	public String getLinkedConsumer() {
		return linkedConsumer;
	}

	public void setLinkedConsumer(String linkedConsumer) {
		this.linkedConsumer = linkedConsumer;
	}

	public NodeConfig getLinkedConsumerLocation() {
		return linkedConsumerLocation;
	}

	public void setLinkedConsumerLocation(NodeConfig _linkedConsumerLocation) {
		this.linkedConsumerLocation = _linkedConsumerLocation;
	}

	/* */
	@Override
	public ExtendedEnergyEvent copyForLSA() {
		EnergyEvent copyEvent = super.copyForLSA();
		ExtendedEnergyEvent copy = new ExtendedEnergyEvent(copyEvent.getType(), copyEvent, copyEvent.getComment());
		copy.setIssuerLocation(linkedConsumerLocation);
		copy.setLinkedConsumer(linkedConsumer);
		copy.setEffectiveEndDate(effectiveEndDate);
		return copy;
	}
}
