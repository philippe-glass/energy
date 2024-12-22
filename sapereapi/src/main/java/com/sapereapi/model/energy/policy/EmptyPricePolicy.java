package com.sapereapi.model.energy.policy;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;

public class EmptyPricePolicy implements IConsumerPolicy {
	protected static SapereLogger logger = SapereLogger.getInstance();

	public EmptyPricePolicy(/* PricingTable producerPricingTale */) {
		super();

	}

	public void setProducerPricingTable(EnergySupply energySupply) {
	}

	@Override
	public EnergyRequest updateRequest(EnergyRequest currentRequest) {
		return currentRequest;
	}

}
