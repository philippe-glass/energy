package com.sapereapi.agent.energy.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class OffersProcessingManager {
	private CompositeOffer globalOffer = null;
	private Map<String, SingleOffer> tableSingleOffers = null;
	private int debugLevel = 0;
	private static SapereLogger logger = SapereLogger.getInstance();

	/*
	// Offer comparator : by time cover
	private final static Comparator<SingleOffer> offerComparatorTimeCover = new Comparator<SingleOffer>() {
		public int compare(SingleOffer offer1, SingleOffer offer2) {
			// to sort in descending order
			return -1*offer1.compareTimeCover(offer2);
		}
	};
	// Offer comparator : by issuer distance
	private final static Comparator<SingleOffer> offerComparatorDistance = new Comparator<SingleOffer>() {
		public int compare(SingleOffer offer1, SingleOffer offer2) {
			// to sort in descending order
			return offer1.compareDistance(offer2);
		}
	};

	// Offer comparator : by issuer distance
	private final static Comparator<SingleOffer> offerComparatorDistanceAndEnvImpact = new Comparator<SingleOffer>() {
		public int compare(SingleOffer offer1, SingleOffer offer2) {
			// to sort in descending order
			return offer1.compareDistanceAndEnvImpact(offer2);
		}
	};*/

	public final static OfferSelectionStrategy SORTBY_TIMECOVER = new OfferSelectionStrategy(
			new Comparator<SingleOffer>() {
				// If all the above criteria are equal, compare the time cover
				 public int compare(SingleOffer offer1, SingleOffer offer2) {
						return -1*offer1.compareTimeCover(offer2);
				 }}
			);

	public final static OfferSelectionStrategy SORTBY_DISTANCE_ENVIMPACT_TIMECOVER = new OfferSelectionStrategy(
			/*
			new Comparator<SingleOffer>() {
				// At first, compare the distance
				public int compare(SingleOffer offer1, SingleOffer offer2) {
					return offer1.compareDistance(offer2);
					}}
			,*/new Comparator<SingleOffer>() {
				// If all the above criteria are equal, compare the environmental impact
				 public int compare(SingleOffer offer1, SingleOffer offer2) {
						return offer1.compareEnvironmentImpact(offer2);
					}}
			,new Comparator<SingleOffer>() {
				// If all the above criteria are equal, compare the time cover
				 public int compare(SingleOffer offer1, SingleOffer offer2) {
						return -1*offer1.compareTimeCover(offer2);
				 }}
			);

	public OffersProcessingManager(EnergyAgent consumerAgent, int _debugLevel) {
		super();
		this.tableSingleOffers = new HashMap<String, SingleOffer>();
		this.globalOffer = new CompositeOffer(consumerAgent.getEnergyRequest());
		debugLevel = _debugLevel;
	}

	public boolean hasSingleOffer() {
		return (tableSingleOffers.size()>0);
	}

	public CompositeOffer generateGlobalOffer(EnergyAgent consumerAgent,EnergyRequest missing) {
		String agentName = consumerAgent.getAgentName();
		//EnergyRequest need = consumerAgent.getEnergyRequest();
		List<SingleOffer> offerList = new ArrayList<>();
		for (SingleOffer singleOffer : tableSingleOffers.values()) {
			if (!singleOffer.hasExpired(0) && singleOffer.getPower()>0 && (singleOffer.isComplementary() == missing.isComplementary())) {
				if("_Consumer_2".equals(agentName) && tableSingleOffers.size()>0) {
					logger.info("generateGlobalOffer " + agentName + " For debug");
				}
				offerList.add(singleOffer);
			}
		}
		// Mix the offers randomly
		Collections.shuffle(offerList);
		// Apply the priority rules to classify offers (for example : take the  nearest first)
		if(PriorityLevel.HIGH.equals(missing.getPriorityLevel()) && offerList.size()>1) {
			//Collections.sort(offerList, offerComparatorTimeCover);
			offerList = SORTBY_TIMECOVER.sortList(offerList);
		} else {
			//Collections.sort(offerList, offerComparatorDistanceAndEnvImpact);
			offerList = SORTBY_DISTANCE_ENVIMPACT_TIMECOVER.sortList(offerList);
			boolean logOffers = false;
			if(logOffers) {
				// Log for debug
				logger.info("tableSingleOffers.size() = " + tableSingleOffers.size());
				int offerIdx = 1;
				for(SingleOffer nextOffer : offerList) {
					logger.info("generateGlobalOffer offer " + offerIdx + " : envImpact " + nextOffer.getDeviceProperties().getEnvironmentalImpactLevel() + ", dist : " + nextOffer.getIssuerDistance() + " " + nextOffer);
					offerIdx++;
				}
			}
		}

		globalOffer = new CompositeOffer(missing);
		for (SingleOffer nextOffer : offerList) {
			this.globalOffer.addSingleOffer(nextOffer);
			if(!this.globalOffer.checkLocationId()) {
				logger.error("generateGlobalOffer : for debug locationid is null");
			}
		}
		return globalOffer;
	}

	public void clearSingleOffers() {
		tableSingleOffers.clear();
	}

	public void cleanExpiredOffers(EnergyAgent consumerAgent) {
		if(consumerAgent.isDisabled() || consumerAgent.hasExpired() || consumerAgent.isSatisfied()) {
			tableSingleOffers.clear();
		} else {
			String offerKey = null;
			while ((offerKey = SapereUtil.getExpiredOfferKey(tableSingleOffers, 0)) != null) {
				// For debug: check if the offer is acquitted
				SingleOffer offer = tableSingleOffers.get(offerKey);
				if(!offer.getAcquitted()) {
					logger.warning("cleanObsoleteData " + consumerAgent.getAgentName() + " this offer is not acquitted " + offer);
				}
				this.tableSingleOffers.remove(offerKey);
			}
		}
	}

	public void addSingleOffer(SingleOffer newOffer) {
		tableSingleOffers.put(newOffer.getProducerAgent(), newOffer);
	}

	public void logOffers(String msgTag) {
		if(tableSingleOffers.size()>0) {
			double offerTotal = 0;
			for(SingleOffer offer : this.tableSingleOffers.values()) {
				offerTotal+=offer.getPower();
			}
			logger.warning(msgTag + " received total :" + UtilDates.df3.format(offerTotal)
				+ " received content : "+ this.tableSingleOffers.values());
		}
	}

	public Double getOffersTotal() {
		Double result = Double.valueOf(0);
		for (SingleOffer offer : this.tableSingleOffers.values()) {
			if (!offer.hasExpired(0)) {
				result+=offer.getPower();
			}
		}
		return result;
	}

	public Map<String, Double> getOffersRepartition() {
		Map<String, Double> result = new HashMap<String, Double>();
		for (SingleOffer offer : this.tableSingleOffers.values()) {
			if (!offer.hasExpired(0)) {
				boolean isLocal = Sapere.getInstance().isLocalAgent(offer.getProducerAgent());
				String agentName = offer.getProducerAgent() + (isLocal? "" : "*");
				result.put(agentName, offer.getPower());
			}
		}
		return result;
	}
}
