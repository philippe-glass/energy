package com.sapereapi.model.energy.policy;

import java.util.Date;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.UtilDates;

public class AltruisticProducerPolicy extends BasicProducerPolicy {
	protected double creditThreasholdHW = -1.5*1000000000;

	public AltruisticProducerPolicy(NodeContext nodeContext, Double rate, int _requestSelectionPolicy, boolean useAwardCredits, double creditThreasholdHW) {		
		super(nodeContext, PricingTable.initSimplePricingTable(nodeContext, rate), _requestSelectionPolicy, useAwardCredits);
		this.creditThreasholdHW =  creditThreasholdHW;
	}

	@Override
	public boolean confirmSupply(EnergyAgent producerAgent, EnergyRequest request) {
		double awardCredit = request.getAwardsCredit();
		//double demandedWH = request.getWH();
		if (awardCredit < 0) {
			//double absOfThreasholdHW = 0.5* Math.min(request.getWH(), request.getPower() / 1.0);
			//double threasholdHW = -1*absOfThreasholdHW;
			//threasholdHW = -1.5;
			//double threasholdHW = 1000.0;
			logger.info("confirmSupply : " + producerAgent.getAgentName() + " request.getAwardsCredit() = "
					+ request.getAwardsCredit() + ", threasholdHW = " + UtilDates.df3.format(creditThreasholdHW));
			if (awardCredit < creditThreasholdHW) {
				logger.info("confirmSupply : " + producerAgent.getAgentName() + " : refuses the request " + request + " , threasholdHW = " + UtilDates.df3.format(creditThreasholdHW));
				return false;
			}
		}
		return true;
	}

	@Override
	public PricingTable getPricingTable(EnergyAgent producerAgent) {
		Date current = getCurrentDate();
		return getRefreshDefaultPricingTable(current);
	}
}
