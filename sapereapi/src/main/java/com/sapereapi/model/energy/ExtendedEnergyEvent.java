package com.sapereapi.model.energy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.ProsumerRole;

import eu.sapere.middleware.log.AbstractLogger;

public class ExtendedEnergyEvent extends EnergyEvent {
	private static final long serialVersionUID = 21L;
	private Date effectiveEndDate;
	private ProsumerItem linkedConsumer;
	private Map<String, ProsumerItem> mapProviders = new HashMap<String, ProsumerItem>();

	public ExtendedEnergyEvent(EventType type, ProsumerProperties prosumerProperties, Boolean isComplementary
			, PowerSlot powerSlot, Date beginDate, Date endDate, String comment, Double firstRate) {
		super(type, prosumerProperties, isComplementary, powerSlot, beginDate, endDate, comment, firstRate);
		mapProviders = new HashMap<String, ProsumerItem>();
	}

	public Date getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(Date effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	public ProsumerItem getLinkedConsumer() {
		return linkedConsumer;
	}

	public void setLinkedConsumer(ProsumerItem linkedConsumer) {
		this.linkedConsumer = linkedConsumer;
	}

	public Map<String, ProsumerItem> getMapProviders() {
		return mapProviders;
	}

	public void setMapProviders(Map<String, ProsumerItem> mapProviders) {
		this.mapProviders = mapProviders;
	}

	public void addProvider(ProsumerItem prosumerItem) {
		if(ProsumerRole.PRODUCER.equals(prosumerItem.getRole())) {
			mapProviders.put(prosumerItem.getAgentName(), prosumerItem);
		}
	}

	public ExtendedEnergyEvent copy(boolean copyIds) {
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(copyIds);
		ExtendedEnergyEvent copy = new ExtendedEnergyEvent(type, cloneIssuerProperties, isComplementary, getPowerSlot(),
				beginDate, endDate, comment, firstRate);
		if(copyIds) {
			copy.setId(id);
			copy.setHistoId(histoId);
		}
		if (linkedConsumer != null) {
			copy.setLinkedConsumer(linkedConsumer.clone());
		}
		copy.setEffectiveEndDate(effectiveEndDate);
		return copy;
	}

	@Override
	public ExtendedEnergyEvent copyForLSA(AbstractLogger logger) {
		ExtendedEnergyEvent copy = copy(false);
		return copy;
	}
}
