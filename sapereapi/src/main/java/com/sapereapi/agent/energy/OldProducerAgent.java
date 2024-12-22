package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.manager.ConsumersProcessingMangager;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.ChangeRequest;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.award.AwardsTable;
import com.sapereapi.model.energy.policy.IEnergyAgentPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.reschedule.RescheduleTable;
import com.sapereapi.model.protection.ProtectedContract;
import com.sapereapi.model.protection.ProtectedSingleOffer;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;

public class OldProducerAgent extends EnergyAgent implements IEnergyAgent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private EnergySupply globalSupply = null;
	private IProducerPolicy producerPolicy = null;
	//private int requestSelctionPolicy = BasicProducerPolicy.POLICY_PRIORIZATION;	// TODO : integrate this notion in IProducerPolicy
	// POLICY_PRIORIZATION, POLICY_RANDOM, POLICY_MIX
	private ConsumersProcessingMangager consumersProcessingManager = null;

	public OldProducerAgent(int _id, AgentAuthentication authentication, EnergySupply _globalSupply, IEnergyAgentPolicy producerPolicy, NodeContext nodeContext) {
		super(_id, authentication.getAgentName()
				,authentication, getInputTags()	,getOutputTags(), nodeContext);
		initFields(_globalSupply, null, producerPolicy, null, nodeContext);
	}

	private static String[] getInputTags() {
		return new String[] { "REQ", "CONTRACT1", "CONTRACT2", "SATISFIED", "WARNING", "RESCHEDULE" };
	}

	private static String[] getOutputTags() {
		return new String[] { "PROD", "EVENT", "OFFER", "PROD_CONFIRM", "DISABLED" };
	}

	@Override
	public void initFields(EnergySupply _globalSupply, EnergyRequest _need, IEnergyAgentPolicy _producerPolicy, IEnergyAgentPolicy _consumerPolicy, NodeContext nodeContext) {
		this.initGlobalProduction(_globalSupply);
		if(_producerPolicy instanceof IProducerPolicy) {
			this.producerPolicy = (IProducerPolicy) _producerPolicy;
		} else {
			//this.producerPolicy = null;
			throw new RuntimeException("initFields : _producerPolicy is not set");
		}
		int decay = 1 + getTimeLeftSec(true);
		this.addDecay(decay);
		this.consumersProcessingManager = new ConsumersProcessingMangager(this);
		logger.info("ProducerAgent " + agentName + " end constructor : For debug");
	}

	@Override
	public void reinitialize(int _id, AgentAuthentication _authentication, EnergySupply _globalSupply, EnergyRequest _need
			, IEnergyAgentPolicy _consumerPolicy, IEnergyAgentPolicy _producerPolicy, NodeContext nodeContext) {
		super.auxReinitializeLSA(_id, _authentication, getInputTags(), getOutputTags(), nodeContext);
		initFields(_globalSupply, null, _producerPolicy, null, nodeContext);
		logger.info("ProducerAgent " + agentName + " end reinitialize : For debug");
	}

	@Override
	public void initGlobalProduction(EnergySupply supply) {
		this.globalSupply = supply;
	}

	@Override
	public EnergySupply getGlobalProduction() {
		return this.globalSupply;
	}

	@Override
	public EnergyRequest getGlobalNeed() {
		return this.globalSupply.generateRequest();
	}

	public IProducerPolicy getProducerPolicy() {
		return producerPolicy;
	}

	@Override
	public boolean isProducer() {
		return true;
	}

	@Override
	public boolean isConsumer() {
		return false;
	}

	@Override
	public boolean isSatisfied() {
		return false;
	}

	@Override
	public Double computeAvailablePower() {
		if(hasExpired() || isDisabled()) {
			return  0.0;
		}
		PowerSlot contractsPowerSlot = getOngoingContractsPowerSlot(null);
		return Math.max(0,globalSupply.getPower() - contractsPowerSlot.getMax());
	}

	@Override
	public Double computeMissingPower() {
		return  Double.valueOf(0);
	}

	@Override
	public EventType getStartEventType() {
		return EventType.PRODUCTION_START;
	}

	@Override
	public EventType getUpdateEventType() {
		return EventType.PRODUCTION_UPDATE;
	}

	@Override
	public EventType getSwitchEventType() {
		return EventType.PRODUCTION_SWITCH;
	}

	@Override
	public EventType getExpiryEventType() {
		return EventType.PRODUCTION_EXPIRY;
	}

	@Override
	public EventType getStopEventType() {
		return EventType.PRODUCTION_STOP;
	}

	@Override
	public PowerSlot getWaitingContratsPowerSlot() {
		return consumersProcessingManager.getWaitingContratsPowerSlot(this);
	}

	@Override
	public List<String> getConsumersOfWaitingContrats() {
		return consumersProcessingManager.getConsumersOfWaitingContrats();
	}

	@Override
	public Set<String> getLinkedAgents() {
		return consumersProcessingManager.getLinkedAgents();
	}

	@Override
	public PowerSlot getForcastOngoingContractsPowerSlot(String location, Date aDate) {
		return consumersProcessingManager.getForcastOngoingContractsPowerSlot(location, this, aDate);
	}

	@Override
	public PowerSlot getOngoingContractsPowerSlot(String location) {
		return consumersProcessingManager.getOngoingContractsPowerSlot(location, this);
	}

	@Override
	public Map<String, PowerSlot> getOngoingContractsRepartition() {
		return consumersProcessingManager.getOngoingContractsRepartition(this);
	}

	@Override
	public Double getOffersTotal() {
		return consumersProcessingManager.getOffersTotal();
	}
	@Override
	public Map<String, Double> getOffersRepartition() {
		return consumersProcessingManager.getOffersRepartition();
	}

	@Override
	public void disableAgent(RegulationWarning warning) throws HandlingException {
		this.globalSupply.setDisabled(true);
		super.disableAgent(warning, new String[] {"PROD_CONFIRM", "OFFER"});
		consumersProcessingManager.stopAllContracts(this);
		// Empty consumersProcessingManager ?
	}

	@Override
	public void onBondNotification(BondEvent event) {
		try {
			if(this.hasExpired() ) {
				 //NodeManager.instance().getNotifier().unsubscribe(this.agentName);
				 logger.info("onBondNotification : " + this.agentName + " has expired");
			}
			// For debug : check availability
			Lsa bondedLsa = event.getBondedLsa();
			if(debugLevel>10 /*|| this.hasHighWarningDuration()*/) {
				//logger.info(" ** ProducerAgent " + agentName + " bonding ** " + " Q: " + query + " bond agent : "	+ bondedLsa.getAgentName());
			}
			if ("_Prod_10".equals(agentName) && bondedLsa.getAgentName().startsWith("Consumer_2")/**/) {
				// For debug
				logger.info(this.agentName + " for DEBUG");
			}
			lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
			bondedLsa = completeInvolvedLocations(bondedLsa, logger);
			// Check if there is a contract to bind
			consumersProcessingManager.addBondedRequest(bondedLsa);
			//consumersProcessingManager.addLogReceivedRequest(bondedLsa);
			// Bond contract agent
			AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
			// Check if there is a consumer request to handle
			if (AgentType.CONSUMER.equals(bondAgentType)) {
				String consumer = bondedLsa.getAgentName();
				if (this.isActive()) {
					if(bondedLsa.isPropagated()) {
						logger.info("onBondNotification : eventLsa.getAgentName() = " + bondedLsa.getAgentName());
					}
					Object oRequested = bondedLsa.getOnePropertyValueByName("REQ");
					if(this.hasHighWarningDuration() && oRequested!= null) {
						logger.info("onBondNotification " + this.agentName  + " REQ prop = " + oRequested);
					}
					Property pDisabled = bondedLsa.getOnePropertyByName("DISABLED");
					if (pDisabled!= null && pDisabled.getValue() instanceof RegulationWarning ) {
						// handle consumer disabled property
						// Remove waiting offer, request, contract
						consumersProcessingManager.removeConsumer(consumer, "consumer disabled ");
					} else {
						if (oRequested != null && oRequested instanceof EnergyRequest) {
							// handle consumer request
							consumersProcessingManager.handleConsumerRequest(this, (EnergyRequest) oRequested, consumer, bondedLsa);
						}
						Object oMainContract = bondedLsa.getOnePropertyValueByName("CONTRACT1");
						Object oSecondContract = bondedLsa.getOnePropertyValueByName("CONTRACT2");
						Object oSatisfied = bondedLsa.getOnePropertyValueByName("SATISFIED");
						if (oMainContract != null && oMainContract instanceof ProtectedContract) {
							// handle consumer main contract
							ProtectedContract mainContract = ((ProtectedContract) oMainContract).clone();
							consumersProcessingManager.handleConsumerContract(this, mainContract, consumer, debugLevel);
						}
						if(oSecondContract != null && oSecondContract instanceof ProtectedContract) {
							// handle consumer complementary contract
							ProtectedContract secondContract = ((ProtectedContract) oSecondContract).clone();
							consumersProcessingManager.handleConsumerContract(this, secondContract, consumer, debugLevel);
						}
						if(oSatisfied!=null && "1".equals(oSatisfied)) {
							// handle consumer confirmation
							consumersProcessingManager.addReceivedConfirmation(this, consumer);
						}
					}
				}
			} else if (AgentType.REGULATOR.equals(bondAgentType)) {
				Property pWarning = bondedLsa.getOnePropertyByName("WARNING");
				if(pWarning!=null && pWarning.getValue() instanceof RegulationWarning) {
					RegulationWarning warning = (RegulationWarning) pWarning.getValue();
					// handle regulation warning
					handleWarning(warning);
				}
				Property pReschedule = bondedLsa.getOnePropertyByName("RESCHEDULE");
				if(pReschedule!=null && pReschedule.getValue() instanceof RescheduleTable) {
					RescheduleTable rescheduleTable = (RescheduleTable) pReschedule.getValue();
					// handle reschedule
					handleReschedule(rescheduleTable);
				}
			}
		} catch(Throwable t) {
			logger.error(t);
			logger.error("### Exception thronw in  " +  this.agentName + " bond handling of " + event  + " : " + t.getMessage());
		}
	}

	@Override
	public void handleWarningOverConsumption(RegulationWarning warning) {
		for(String consumer : consumersProcessingManager.getTableValidContracts().keySet()) {
			if(warning.hasAgent(consumer)) {
				consumersProcessingManager.stopContracts(this, consumer, "The agent received a regulation warning : "+warning.getType());
			}
		}
	}

	@Override
	public void handleWarningOverProduction(RegulationWarning warning) throws HandlingException {
		if(warning.hasAgent(agentName) && !this.isDisabled()) {
			logger.info("handleWarningOverProduction " + agentName + "The agent received a regulation warning : "+warning.getType());
			disableAgent(warning);
		}
	}

	@Override
	public void handleWarningUserInteruption(RegulationWarning warning) throws HandlingException {
		if(warning.hasAgent(agentName) && !this.isDisabled()) {
			logger.info("handleWarningUserInteruption " + agentName + "The agent received a regulation warning : "+warning.getType());
			disableAgent(warning);
		}
	}

	@Override
	public void handleWarningChangeRequest(RegulationWarning warning) {
		ChangeRequest changeRequest = warning.getChangeRequest();
		if(changeRequest!=null) {
			if(warning.hasAgent(agentName) && !this.isDisabled()) {
				// The change request concerns the agent itelf
				if(ProsumerRole.CONSUMER.equals(changeRequest.getProsumerRole())) {
					updateSupply(changeRequest.getSupply());
				}
			}
			Map<String, List<ReducedContract>> validContracts = consumersProcessingManager.getTableValidContracts();
			logger.info("handleWarningChangeRequest " + agentName + " " + warning + ", validContracts for consumers  = " + validContracts.keySet() );
			for(String consumer : validContracts.keySet()) {
				if(warning.hasAgent(consumer) && warning.getChangeRequest()!=null) {
					// The change request concerns on of the provided consumers
					List<ReducedContract> listContracts = validContracts.get(consumer);
					for(ReducedContract contract : listContracts) {
						//ReducedContract contract = validContracts.get(consumer);
						// Stop the contract is the requested power has increased
						if(!nodeContext.isComplementaryRequestsActivated() && changeRequest.getSupply().getPower() > contract.getRequest().getPower()) {
							String comment = "The agent " + consumer + " received a request for a change with an increase in requested power : ";
							comment = comment + "new demand " + UtilDates.df3.format(changeRequest.getSupply().getPower())  + " contract tt power : " +  UtilDates.df3.format(contract.getRequest().getPower()) + " contract = " + contract;
							consumersProcessingManager.stopContract(this, consumer, contract.isComplementary(), comment);
						} else {
						}
					}
				}
			}
		}
	}

	@Override
	public void handleAwards(AwardsTable awardsTable) throws HandlingException {
		// do nothing
	}

	@Override
	public void onDecayedNotification(DecayedEvent event) {
		try {
			if(producerPolicy.hasDefaultPrices() && !this.hasExpired()) {
				logger.info("onDecayedNotification : " + agentName + " producerPolicy has default price" );
				// Map<String, List<ReducedContract>> mapContracts = consumersProcessingManager.getTableValidContracts();
				PricingTable newPricingTable = producerPolicy.getPricingTable(this);
				if(newPricingTable != null) {
					this.globalSupply.setPricingTable(newPricingTable);
					// refresh PROD property LSA
					replacePropertyWithName(new Property("PROD", this.globalSupply));
				}
			}
			// For debug : check availability
			if(!firstDecay && timeLastDecay!=null) {
				Date current = getCurrentDate();
				if( current.getTime() - timeLastDecay.getTime()> 2*1000) {
					logger.warning("onDecayedNotification " + this.agentName + " : last decay at " + UtilDates.format_time.format(timeLastDecay));
				}
			}
			timeLastDecay = getCurrentDate();
			// check agent expiration
			checkAgentExpiration(event);
			if(this.hasExpired()) {
				//  Stop all ongoing contracts
				for(ReducedContract contract : consumersProcessingManager.getValidContracts()) {
					consumersProcessingManager.sendConfirmation(this,contract, false, agentName + " : expiration");
				}
			}
			if(this.isDisabled()) {
				// Stop all contracts
				consumersProcessingManager.stopAllContracts(this);
				/*
				for(String consumer : consumersProcessingManager.getTableValidContracts().keySet()) {
					consumersProcessingManager.stopContract(this, consumer, this.agentName + " is disabled");
				}*/
			}
			// add Prod tag if needed
			if(lsa.getPropertiesByName("PROD").isEmpty()) {
				addProperty(new Property("PROD", this.globalSupply));
			}

			cleanExpiredData();

			// send confirmations to consumers agents
			consumersProcessingManager.confirmAvailabilityToConsumers(this);

			// Check received confirmation from consumers agents
			consumersProcessingManager.checkReceivedConfirmations(this);

			// Retrieve the node availability from the last stored node total
			lastNodeTotal = EnergyDbHelper.retrieveLastNodeTotal();
			tryReactivation();
			int nbNewOFfers = 0;
			if(!this.isDisabled() && !this.hasExpired()) {
				nbNewOFfers = consumersProcessingManager.generateNewOffers(this, lastNodeTotal);
			}
			if(nbNewOFfers>0) {
				logger.info("nbNewOFfers = " + nbNewOFfers);
			}
			firstDecay = false;
			// For debug : check availability
			// Post not sent properties
			checkWaitingProperties();
			// For spreading
			activateSpreading();
		} catch(Throwable t) {
			logger.error(t);
			logger.error("### Exception thronw in  " +  this.agentName + " decayhandling of " + event + " " + t.getMessage());
		}
	}

	@Override
	public void cleanExpiredData() throws HandlingException {
		// Remove posted offers
		String offerKey = getOfferKeyToRemove();
		while(offerKey!=null) {
			// For debug
			debug_checkOfferAcquitted(offerKey);
			lsa.removePropertiesByQueryAndName(offerKey, "OFFER");
			offerKey = getOfferKeyToRemove();
		}

		// Clean expired confirmations
		consumersProcessingManager.cleanConfirmationTable(this);

		// Clean event
		cleanEventProperties();

		// Clean exprited request,offers,contracts
		consumersProcessingManager.cleanExpiredData(this, debugLevel);

		// Clean expired warnings
		cleanExpiredWarnings();
	}


	// specific
	private void updateSupply(EnergySupply changeRequest) {
		//disableAgent(warning);
		try {
			if(!globalSupply.hasChanged(changeRequest)) {
				logger.info("updateSupply " + changeRequest + " nothing is changed" );
				// nothing to do
				return;
			}
			double powerBefore =  globalSupply.getPower();
			this.globalSupply = changeRequest.clone();
			// Refresh PROD property
			replacePropertyWithName(new Property("PROD", this.globalSupply));
			Map<String, List<ReducedContract>> validContracts = consumersProcessingManager.getTableValidContracts();
			if(changeRequest.getPower() < powerBefore) {
				// Stop contract if not the agent has not enougth availability
				while(consumersProcessingManager.computeAvailablePower(this, false, false) < 0 && validContracts.size() > 0) {
					// Remove contract
					List<String> consumers = new ArrayList<>();
					consumers.addAll(validContracts.keySet());
					Collections.shuffle(consumers);
					// TODO : sort contracts by request priority and power
					String consumer = consumers.get(0);
					double difference = powerBefore - changeRequest.getPower();
					consumersProcessingManager.stopContracts(this, consumer, "The producer agent " + agentName
							+ " has received a change request with a decrease in power output of " + UtilDates.df5.format(difference) + " Watts. "+ WarningType.CHANGE_REQUEST);
					validContracts = consumersProcessingManager.getTableValidContracts();
				}
			}
			generateUpdateEvent(WarningType.CHANGE_REQUEST);
		} catch (Throwable e) {
			logger.error(e);
		}
	}

	// specific
	private void debug_checkOfferAcquitted(String consumer) throws HandlingException {
		ProtectedSingleOffer protectedOffer = (ProtectedSingleOffer) lsa.getOnePropertyValueByQueryAndName(consumer, "OFFER");
		if(protectedOffer!=null && protectedOffer.hasAccesAsProducer(this)) {
			try {
				SingleOffer offer = protectedOffer.getSingleOffer(this);
				if(offer!=null) {
					boolean isAcquitted = EnergyDbHelper.isOfferAcuitted(offer.getId());
					if(!isAcquitted) {
						// The offer is not acquitted
						logger.warning(this.agentName + " debug_checkOfferAcquitted : the offer " + offer + " is not acquitted");
					}
				}
			} catch (PermissionException e) {
				logger.error(e);
			}
		}
	}

	// specific
	private String getOfferKeyToRemove() {
		for (Property p : lsa.getProperties()) {
			if (true && "OFFER".equals(p.getName()) && p.getValue() instanceof ProtectedSingleOffer) {
				return p.getQuery();
			}
		}
		return null;
	}



	// specific :  should be common
	public void logAgent() {
		if(this.isActive()) {
			consumersProcessingManager.logContent1(this);
		}
	}

	// specific : should be common
	public void checkup() {
		consumersProcessingManager.checkup(this);
	}

	@Override
	public void setEventId(long eventId) {
		if (globalSupply != null) {
			globalSupply.setEventId(eventId);
		}
	}

	@Override
	public void setBeginDate(Date aDate) {
		if (globalSupply != null) {
			globalSupply.setBeginDate(aDate);
		}
	}

	@Override
	public void setEndDate(Date aDate) {
		if (globalSupply != null) {
			globalSupply.setEndDate(aDate);
		}
	}

	@Override
	public void setDisabled(boolean bDisabled) {
		if (globalSupply != null) {
			globalSupply.setDisabled(bDisabled);
		}
	}

	@Override
	public EnergyEvent generateEvent(EventType eventType, String log) {
		if (globalSupply != null) {
			return globalSupply.generateEvent(eventType, log);
		}
		return null;
	}
}
