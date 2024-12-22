package com.sapereapi.model.energy.policy;

import java.util.Date;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.UtilDates;

public class LowestDemandPolicy extends BasicProducerPolicy {
	//private PricingTable defaultPricingTable;
	//protected static SapereLogger logger = SapereLogger.getInstance();

	// private Map<String, ReducedContract> _mapContracts = new HashMap<>();

	public LowestDemandPolicy(NodeContext nodeContext, PricingTable defaultPricingTable, int requestSelectionPolicy, boolean useAwardCredits) {
		super(nodeContext, defaultPricingTable, requestSelectionPolicy, useAwardCredits);
		this.defaultPricingTable = defaultPricingTable;
	}

	/*
	 * public void setContract(ReducedContract contract) {
	 * mapContracts.put(contract.getConsumerAgent(), contract); }
	 */
	@Override
	public PricingTable getPricingTable(EnergyAgent producerAgent) {
		if (producerAgent.isProducer()) {
			double producedCurrent = producerAgent.getGlobalProduction().getPower();
			double ongoingContractsCurrent = producerAgent.getOngoingContractsPowerSlot(null).getCurrent();
			double waitingContractsCurrent = producerAgent.getWaitingContratsPowerSlot().getCurrent();
			double offersTotalCurrent = producerAgent.getOffersTotal();
			double usedCurrent = ongoingContractsCurrent + waitingContractsCurrent + offersTotalCurrent;
			if (producedCurrent > 0 && usedCurrent > 0) {
				Date current = producerAgent.getCurrentDate();
				PricingTable result = new PricingTable(producerAgent.getTimeShiftMS());
				PricingTable refreshedDefaultPrincingTable = defaultPricingTable.getRefreshedTable(current);
				boolean firstSlot = true;
				for (Date nextDate : refreshedDefaultPrincingTable.getDates()) {
					if (nextDate.before(current)) {
						// nothing to do
					} else {
						double produced = 0;
						double used = 0;
						if (firstSlot) { // Current slot
							produced = producedCurrent;
							used = usedCurrent;
							// State at current time
							firstSlot = false;

						} else {
							// Forcasted state
							//Date beginDate = nextDate;
							produced = producerAgent.getGlobalProduction().getForcastPowerSlot(nextDate).getCurrent();
							double ongoingContracts = producerAgent.getForcastOngoingContractsPowerSlot(null, nextDate)
									.getCurrent();
							used = ongoingContracts;
						}
						double defaultRate = refreshedDefaultPrincingTable.getRate(nextDate).getDefaultValue();
						double rateToApply = defaultRate;
						if (produced > 0 && used > 0) {
							double availability = Math.max(0, produced - used);
							double availabilityRatio = availability / produced;
							if (availabilityRatio < 0.9) {
								double divisor = Math.max(0.1, availabilityRatio);
								double riseFactor = 1 / divisor;
								logger.info(this.getClass().getSimpleName() + ".getPricingTable : apply rise factor of " + riseFactor + " at " + UtilDates.format_date_time.format(nextDate));
								rateToApply = defaultRate * riseFactor;
							}
						}
						result.putRate(nextDate , rateToApply, null);
					}
				}
				return result;
			}
		}
		return defaultPricingTable;
	}
}
