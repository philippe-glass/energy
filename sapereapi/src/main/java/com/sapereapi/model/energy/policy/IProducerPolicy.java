package com.sapereapi.model.energy.policy;

import java.util.Collection;
import java.util.List;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.pricing.PricingTable;

public interface IProducerPolicy extends IEnergyAgentPolicy {
	PricingTable getDefaultPricingTable();
	PricingTable getPricingTable(EnergyAgent producerAgent);
	void setDefaultPricingTable(PricingTable defaultPricingTable);
	boolean hasDefaultPrices();
	List<EnergyRequest> sortRequests(Collection<EnergyRequest> listWaitingRequest);
	public boolean confirmSupply(EnergyAgent produserAgent, EnergyRequest request);
	public boolean confirmDonationOfAvailableEnergy(EnergyAgent produserAgent, double availableWH);
	public SingleOffer priceOffer(EnergyAgent producerAgent, SingleOffer offer);

	public final static int POLICY_RANDOM = 10;
	public final static int POLICY_PRIORITIZATION = 11;
	public final static int POLICY_MIX = 12;
}
