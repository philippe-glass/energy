package com.sapereapi.model.energy.policy;

import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;

public interface IConsumerPolicy extends IEnergyAgentPolicy {
	public void setProducerPricingTable(EnergySupply energySupply);
	public EnergyRequest updateRequest(EnergyRequest currentRequest);
}
