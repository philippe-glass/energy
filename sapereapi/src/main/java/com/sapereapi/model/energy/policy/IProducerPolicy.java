package com.sapereapi.model.energy.policy;

import java.util.Collection;
import java.util.List;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.PricingTable;

public interface IProducerPolicy extends IEnergyAgentPolicy {
	PricingTable getPricingTable(EnergyAgent producerAgent);
	boolean hasDefaultPrices();
	List<EnergyRequest> sortRequests(Collection<EnergyRequest> listWaitingRequest);

	public final static int POLICY_RANDOM = 10;
	public final static int POLICY_PRIORIZATION = 11;
	public final static int POLICY_MIX = 12;
}
