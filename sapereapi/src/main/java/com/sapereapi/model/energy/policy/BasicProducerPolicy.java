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
import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class BasicProducerPolicy implements IProducerPolicy {
	protected PricingTable defaultPricingTable;
	protected int requestSelectionPolicy;
	protected NodeContext nodeContext;
	protected boolean useAwardCredits = false;
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
					String issuer1 =  req1.getIssuer();
					String issuer2 = req2.getIssuer();
					return issuer1.compareTo(issuer2);
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
		} else if (requestSelectionPolicy == POLICY_PRIORITIZATION) {
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

	public BasicProducerPolicy(NodeContext nodeContext, PricingTable defaultPricingTable, int _requestSelectionPolicy, boolean useAwardCredits) {
		super();
		this.nodeContext = nodeContext;
		this.defaultPricingTable = defaultPricingTable;
		this.requestSelectionPolicy = _requestSelectionPolicy;
		this.useAwardCredits = useAwardCredits;
	}

	public PricingTable getDefaultPricingTable() {
		return defaultPricingTable;
	}

	public void setDefaultPricingTable(PricingTable defaultPricingTable) {
		this.defaultPricingTable = defaultPricingTable;
	}

	@Override
	public PricingTable getPricingTable(EnergyAgent producerAgent) {
		return defaultPricingTable;
	}

	@Override
	public boolean hasDefaultPrices() {
		return defaultPricingTable.hasNotNullValue();
	}

	protected Date getCurrentDate() {
		long timeShiftMS = nodeContext.getTimeShiftMS();
		return UtilDates.getNewDate(timeShiftMS);
	}

	protected PricingTable getRefreshDefaultPricingTable(Date beginDate) {
		return defaultPricingTable.getRefreshedTable(beginDate);
	}

	@Override
	public boolean confirmSupply(EnergyAgent producerAgent, EnergyRequest request) {
		if (request.getAwardsCredit() < 0) {
			return false;
		}
		return true;
	}

	@Override
	public boolean confirmDonationOfAvailableEnergy(EnergyAgent produserAgent, double availableWH) {
		EnergyStorageSetting storageSetting = produserAgent.getStorageSetting();
		if(storageSetting == null || !storageSetting.canSaveEnergy()) {
			boolean isExternalSupply = produserAgent.getGlobalProduction().getIssuerProperties().getDeviceProperties().hasCategoryExternalSupply();
			return !isExternalSupply;
		}
		return false;
	}

	@Override
	public SingleOffer priceOffer(EnergyAgent producerAgent, SingleOffer offer) {
		PricingTable pricingTable =  defaultPricingTable.applyAwardsOnPricingTable(offer, useAwardCredits, logger);
		EnergyRequest request = offer.getRequest();
		double awardCredit = request.getAwardsCredit();
		if (Math.abs(awardCredit) > 0) {
			double demandedWH = request.getWH();
			logger.info("confirmSupply " + producerAgent.getAgentName() + " : awardCredit = "
					+ SapereUtil.roundPower(awardCredit) + ", demandedWH = " + SapereUtil.roundPower(demandedWH)
					+ ", pricingTable = " + pricingTable);
		}
		offer.setPricingTable(pricingTable);
		return offer;
	}
}
