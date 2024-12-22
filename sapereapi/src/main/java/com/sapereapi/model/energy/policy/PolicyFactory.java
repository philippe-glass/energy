package com.sapereapi.model.energy.policy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.UtilDates;

public class PolicyFactory {
	private static NodeContext nodeContext = null; // Node context
	public final static String POLICY_LOWEST_DEMAND = "LowestDemand";
	public final static String POLICY_FREE_RIDING = "FreeRiding";
	public final static String POLICY_ALTRUIST =  "Altruist";
	public static final String POLICY_LOWEST_PRICE = "LowestPrice";

	private static int priceDurationMinutes = 3;

	public static NodeContext getNodeContext() {
		return nodeContext;
	}

	public static void setNodeContext(NodeContext nodeContext) {
		PolicyFactory.nodeContext = nodeContext;
	}

	public static int getPriceDurationMinutes() {
		return priceDurationMinutes;
	}

	public static void setPriceDurationMinutes(int priceDurationMinutes) {
		PolicyFactory.priceDurationMinutes = priceDurationMinutes;
	}

	private static PricingTable initPricingTable() {
		Map<Integer, Double> simplePicingTable = new HashMap<Integer, Double>();
		if (nodeContext != null) {
			Date current = nodeContext.getCurrentDate();
			// Date end = UtilDates.shiftDateMinutes(current, 60);
			int time = 0;

			// simplePicingTable.put(time, 10.0);
			// time += 1;
			// simplePicingTable.put(time, 10.0);
			// time += 1;
			simplePicingTable.put(time, 6.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 7.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 8.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 9.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 10.0);
			time += 20 * priceDurationMinutes;
			simplePicingTable.put(time, 10.0);
			// Date lastStepDate = null;
			PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
			SortedSet<Integer> keys = new TreeSet<>(simplePicingTable.keySet());
			for (int step : keys) {
				Double rate = simplePicingTable.get(step);
				Date nextStepDate = UtilDates.shiftDateMinutes(current, step);
				pricingTable.putRate(nextStepDate, rate, null);
				// lastStepDate = nextStepDate;
			}
			return pricingTable;
		}
		return new PricingTable(0);
	}


	public IProducerPolicy initDefaultProducerPolicy() {
		BasicProducerPolicy result = new BasicProducerPolicy(nodeContext, 0.0, IProducerPolicy.POLICY_PRIORITIZATION, false);
		return result;
	}

	public IProducerPolicy initProducerPolicy(AgentInputForm agentInputForm) {
		String id = agentInputForm.getProducerPolicyId();
		boolean useAwardCredits = agentInputForm.isUseAwardCredits();
		IProducerPolicy result = initProducerPolicy(id, useAwardCredits);
		return result;
	}

	public IProducerPolicy initProducerPolicy(String id, boolean useAwardCredits) {
		if(POLICY_LOWEST_DEMAND.equals(id)) {
			PricingTable pricingTable = initPricingTable();
			return new LowestDemandPolicy(nodeContext, pricingTable, IProducerPolicy.POLICY_PRIORITIZATION, useAwardCredits);
		} else if(POLICY_FREE_RIDING.equals(id)) {
			return new FreeRidingProducerPolicy(nodeContext, 0.0, IProducerPolicy.POLICY_PRIORITIZATION, useAwardCredits);
		} else if(POLICY_ALTRUIST.equals(id)) {
			return new AtruistProducerPolicy(nodeContext, 1.0, IProducerPolicy.POLICY_PRIORITIZATION, useAwardCredits, -99999);
		}
		return initDefaultProducerPolicy();
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
