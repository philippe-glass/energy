package com.sapereapi.model.energy.policy;

import java.util.Date;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.util.UtilDates;

public class LowestDemandPolicy extends BasicProducerPolicy {
	//private PricingTable defaultPricingTable;
	//protected static SapereLogger logger = SapereLogger.getInstance();

	// private Map<String, ReducedContract> _mapContracts = new HashMap<>();

	public LowestDemandPolicy(PricingTable defaultPricingTable, int _requestSelectionPolicy) {
		super(defaultPricingTable, _requestSelectionPolicy);
	}

	/*
	 * public void setContract(ReducedContract contract) {
	 * mapContracts.put(contract.getConsumerAgent(), contract); }
	 */
	@Override
	public PricingTable getPricingTable(EnergyAgent producerAgent) {
		if (producerAgent.isProducer()) {
			double producedCurrent = producerAgent.getEnergySupply().getPower();
			double ongoingContractsCurrent = producerAgent.getOngoingContractsPowerSlot(null).getCurrent();
			double waitingContractsCurrent = producerAgent.getWaitingContratsPowerSlot().getCurrent();
			double offersTotalCurrent = producerAgent.getOffersTotal();
			double usedCurrent = ongoingContractsCurrent + waitingContractsCurrent + offersTotalCurrent;
			if (producedCurrent > 0 && usedCurrent > 0) {
				Date current = producerAgent.getCurrentDate();
				PricingTable result = new PricingTable();
				for (TimeSlot nextTimeSlot : defaultPricingTable.getTimeSlots()) {
					Date beginDate = nextTimeSlot.getBeginDate();
					Date endDate = nextTimeSlot.getEndDate();
					if (endDate.before(current)) {
						// nothing to do
					} else {
						double produced = 0;
						double used = 0;
						if (nextTimeSlot.isInSlot(current)) {
							produced = producedCurrent;
							used = usedCurrent;
							// State at current time

						} else {
							// Forcasted state
							produced = producerAgent.getEnergySupply().getForcastPowerSlot(beginDate).getCurrent();
							double ongoingContracts = producerAgent.getForcastOngoingContractsPowerSlot(null, beginDate)
									.getCurrent();
							used = ongoingContracts;
						}
						double defaultRate = defaultPricingTable.getRate(beginDate);
						double rateToApply = defaultRate;
						if (produced > 0 && used > 0) {
							double availability = Math.max(0, produced - used);
							double availabilityRatio = availability / produced;
							if (availabilityRatio < 0.9) {
								double divisor = Math.max(0.1, availabilityRatio);
								double riseFactor = 1 / divisor;
								logger.info(this.getClass().getSimpleName() + ".getPricingTable : apply rise factor of " + riseFactor + " at " + UtilDates.format_date_time.format(beginDate));
								rateToApply = defaultRate * riseFactor;
							}
						}
						result.addPrice(beginDate.before(current)? current : beginDate, endDate, rateToApply);
					}
				}
				return result;
			}
		}
		return defaultPricingTable;
	}
}
