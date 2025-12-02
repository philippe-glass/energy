package com.sapereapi.model.energy.policy;

import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.pricing.PricingTable;

public class PolicyFactory {
	private static NodeContext nodeContext = null; // Node context
	public final static String POLICY_LOWEST_DEMAND = "LowestDemand";
	public final static String POLICY_FREE_RIDING = "FreeRiding";
	public final static String POLICY_ALTRUIST =  "Altruist";
	public static final String POLICY_LOWEST_PRICE = "LowestPrice";


	public static NodeContext getNodeContext() {
		return nodeContext;
	}

	public static void setNodeContext(NodeContext nodeContext) {
		PolicyFactory.nodeContext = nodeContext;
	}

	public IProducerPolicy initDefaultProducerPolicy() {
		return initDefaultProducerPolicy(0.0);
	}

	public IProducerPolicy initDefaultProducerPolicy(Double defulatRate) {
		PricingTable pricingTable = PricingTable.initSimplePricingTable(nodeContext, defulatRate);
		return new BasicProducerPolicy(nodeContext, pricingTable, IProducerPolicy.POLICY_PRIORITIZATION, false);
	}

	public IProducerPolicy initProducerPolicy(AgentInputForm agentInputForm) {
		String id = agentInputForm.getProducerPolicyId();
		boolean useAwardCredits = agentInputForm.isUseAwardCredits();
		IProducerPolicy result = initProducerPolicy(id, useAwardCredits, 1.0);
		return result;
	}

	public IProducerPolicy initProducerPolicy(String id, boolean useAwardCredits, double defulatRate) {
		if(POLICY_LOWEST_DEMAND.equals(id)) {
			return new LowestDemandPolicy(nodeContext, defulatRate, IProducerPolicy.POLICY_PRIORITIZATION, useAwardCredits);
		} else if(POLICY_FREE_RIDING.equals(id)) {
			return new FreeRidingProducerPolicy(nodeContext, defulatRate, IProducerPolicy.POLICY_PRIORITIZATION, useAwardCredits);
		} else if(POLICY_ALTRUIST.equals(id)) {
			double creditThreasholdHW = -2; // -2; // -99999
			return new AltruisticProducerPolicy(nodeContext, defulatRate, IProducerPolicy.POLICY_PRIORITIZATION, useAwardCredits, creditThreasholdHW);
		}
		return initDefaultProducerPolicy(defulatRate);
	}

	public IConsumerPolicy initConsumerPolicy(AgentInputForm agentInputForm) {
		String id = agentInputForm.getConsumerPolicyId();
		return initConsumerPolicy(id);
	}

	public IConsumerPolicy initConsumerPolicy(String id) {
		if(POLICY_LOWEST_PRICE.equals(id)) {
			return new LowestPricePolicy();
		}
		return initDefaultConsumerPolicy();
	}

	public IConsumerPolicy initDefaultConsumerPolicy() {
		IConsumerPolicy result = new EmptyPricePolicy();
		return result;
	}

}
