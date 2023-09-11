package com.sapereapi.model.energy.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.agent.energy.manager.RequestSelectionStrategy;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.util.UtilDates;

public class BasicProducerPolicy implements IProducerPolicy {
	protected PricingTable defaultPricingTable;
	protected int requestSelectionPolicy;
	protected static SapereLogger logger = SapereLogger.getInstance();

	public final static RequestSelectionStrategy SORTBY_DISTANCE_PRIORITY_WARNING_POWER = new RequestSelectionStrategy(
			new Comparator<EnergyRequest>() {
				// At first, compare the distance
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.compareDistance(req2);
				}
			}, new Comparator<EnergyRequest>() {
				// If all the above criteria are equal, compare the priority
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.comparePriorityDesc(req2);
				}
			}, new Comparator<EnergyRequest>() {
				// If all the above criteria are equal, compare the warning duration
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.compareWarningDesc(req2);
				}
			}, new Comparator<EnergyRequest>() {
				// If all the above criteria are equal, compare the power
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.comparePower(req2);
				}
			}, new Comparator<EnergyRequest>() {
				// If all the above criteria are equal, compare the consumer name
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.getIssuer().compareTo(req2.getIssuer());
				}
			});

	public final static RequestSelectionStrategy DISTANCE_PRIORITY = new RequestSelectionStrategy(
			new Comparator<EnergyRequest>() {
				// At first, sort by distance
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.compareDistance(req2);
				}
			}, new Comparator<EnergyRequest>() {
				// If all the above criteria are equal, sort by priority
				public int compare(EnergyRequest req1, EnergyRequest req2) {
					return req1.comparePriorityDesc(req2);
				}
			});

	public List<EnergyRequest> sortRequests(Collection<EnergyRequest> listWaitingRequest) {
		List<EnergyRequest> requestList = new ArrayList<EnergyRequest>();
		// Collection<EnergyRequest> listWaitingRequest = getWaitingRequest();
		requestList.addAll(listWaitingRequest);
		Collections.shuffle(requestList);

		// Define the sorting strategy
		if (requestSelectionPolicy == POLICY_RANDOM) {
			// Sort only by request priority
			// Collections.sort(requestList, requestComparatorPriority);
			requestList = DISTANCE_PRIORITY.sortList(requestList);
		} else if (requestSelectionPolicy == POLICY_PRIORIZATION) {
			// Sort by request priority, warning level desc, power asc
			// Collections.sort(requestList,
			// requestComparatorDistancePriorityAndWarningAndPower);
			requestList = SORTBY_DISTANCE_PRIORITY_WARNING_POWER.sortList(requestList);
		} else if (requestSelectionPolicy == POLICY_MIX) {
			// Get the max warning level
			int maxWarningDuraitonSec = 0;
			for (EnergyRequest req : requestList) {
				if (req.getWarningDurationSec() > maxWarningDuraitonSec) {
					maxWarningDuraitonSec = req.getWarningDurationSec();
				}
			}
			if (maxWarningDuraitonSec > 7) {
				// Sort by request priority, warning level desc, power asc
				// Collections.sort(requestList,
				// requestComparatorDistancePriorityAndWarningAndPower);
				requestList = SORTBY_DISTANCE_PRIORITY_WARNING_POWER.sortList(requestList);
			} else {
				// Apply random policy : sort only by request priority
				// Collections.sort(requestList, requestComparatorPriority);
				requestList = DISTANCE_PRIORITY.sortList(requestList);
			}
		}
		return requestList;
	}

	// Request comparator : priority and power
	/*
	 * private static final Comparator<EnergyRequest>
	 * requestComparatorPriorityAndPower = new Comparator<EnergyRequest>() { public
	 * int compare(EnergyRequest req1, EnergyRequest req2) { return
	 * req1.comparePriorityDescAndPower(req2); } };
	 */

	/*
	 * private static final Comparator<Contract> contractComparator = new
	 * Comparator<Contract>() { public int compare(Contract contract1, Contract
	 * contract2) { return contract1.comparePower(contract2); } };
	 * 
	 * private static final Comparator<EnergySupply> supplyComparator = new
	 * Comparator<EnergySupply>() { public int compare(EnergySupply supply1,
	 * EnergySupply supply2) { int comaprePower = supply1.comparePower(supply2);
	 * if(comaprePower==0) { return supply1.comparTimeLeft(supply2); } else { return
	 * comaprePower; } } };
	 */

	private PricingTable initSimplePricingTable(NodeContext nodeContext, Double rate) {
		Date current = nodeContext.getCurrentDate();
		Date end = UtilDates.shiftDateDays(current, 366 * 1000);
		// Map<Integer, Double> simplePicingTable = new HashMap<Integer, Double>();
		PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
		pricingTable.addPrice(current, end, rate);
		return pricingTable;
	}

	public BasicProducerPolicy(PricingTable defaultPricingTable, int _requestSelectionPolicy) {
		super();
		this.defaultPricingTable = defaultPricingTable;
		this.requestSelectionPolicy = _requestSelectionPolicy;
	}

	public BasicProducerPolicy(NodeContext nodeContext, Double rate, int _requestSelectionPolicy) {
		super();
		this.defaultPricingTable = initSimplePricingTable(nodeContext, rate);
		this.requestSelectionPolicy = _requestSelectionPolicy;
	}

	@Override
	public PricingTable getPricingTable(EnergyAgent producerAgent) {
		return defaultPricingTable;
	}

	@Override
	public boolean hasDefaultPrices() {
		return defaultPricingTable.hasNotNullValue();
	}

}
