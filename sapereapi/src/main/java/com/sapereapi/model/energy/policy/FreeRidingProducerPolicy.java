package com.sapereapi.model.energy.policy;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.DeviceCategory;

public class FreeRidingProducerPolicy extends BasicProducerPolicy {

	public FreeRidingProducerPolicy(NodeContext nodeContext, Double rate, int _requestSelectionPolicy, boolean useAwardCredits) {
		super(nodeContext, PricingTable.initSimplePricingTable(nodeContext, rate), _requestSelectionPolicy, useAwardCredits);
	}

	@Override
	public boolean confirmSupply(EnergyAgent produserAgent, EnergyRequest request) {
		return request.getPower() <= 10 && DeviceCategory.ICT.equals(request.getIssuerProperties().getDeviceProperties().getCategory());
	}

}
