package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.manager.ConsumersProcessingMangager;
import com.sapereapi.agent.energy.manager.ContractProcessingManager;
import com.sapereapi.agent.energy.manager.OffersProcessingManager;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.ChangeRequest;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergyStorage;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtraSupply;
import com.sapereapi.model.energy.IEnergyRequest;
import com.sapereapi.model.energy.IEnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.award.AwardItem;
import com.sapereapi.model.energy.award.AwardsTable;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IEnergyAgentPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.prosumerflow.ProsumerEnergyRequest;
import com.sapereapi.model.energy.prosumerflow.ProsumerEnergySupply;
import com.sapereapi.model.energy.reschedule.RescheduleItem;
import com.sapereapi.model.energy.reschedule.RescheduleTable;
import com.sapereapi.model.protection.ProtectedConfirmationTable;
import com.sapereapi.model.protection.ProtectedContract;
import com.sapereapi.model.protection.ProtectedSingleOffer;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;

public class ProsumerAgent extends EnergyAgent implements IEnergyAgent {
	private static final long serialVersionUID = 1L;
	private ProsumerEnergyRequest globalNeed = null;
	private ProsumerEnergySupply globalProduction = null;
	private IConsumerPolicy consumerPolicy = null;
	private IProducerPolicy producerPolicy = null;
	private int useOffersDecay = 0;
	private int useOfferInitialDecay = 0;
	private OffersProcessingManager offersProcessingManager = null;		// management of offers sent by producers
	private ContractProcessingManager contractProcessingManager = null;	// managing contracts as a consumer (primary and secondary)
	private ConsumersProcessingMangager consumersProcessingManager = null;	// managing consumer requests/contracts
	public final static int CONFIRMATION_INIT_DECAY = 3;

	public ProsumerAgent(int id
			,AgentAuthentication authentication
			,EnergySupply globalSupply
			,EnergyRequest globalNeed
			,IEnergyAgentPolicy producerPolicy
			,IEnergyAgentPolicy consumerPolicy
			,NodeContext nodeContext) throws HandlingException {
		super(id, authentication.getAgentName(), authentication
				,getInputTags(globalSupply, globalNeed, nodeContext)
				,getOutputTags(globalSupply, globalNeed, nodeContext)
				,nodeContext);
		this.initFields(globalSupply, globalNeed, producerPolicy, consumerPolicy, nodeContext);
	}

	private static String[] getInputTags(IEnergySupply _globalSupply, IEnergyRequest _globalNeed, NodeContext nodeContext) {
		List<String> inputTags = new ArrayList<String>();
		// common tags :
		String[] commonTags =  {"DISABLED", "WARNING", "RESCHEDULE"};
		inputTags.addAll(Arrays.asList(commonTags));
		if(nodeContext.isAwardsActivated()) {
			inputTags.add("AWARDS");
		}
		if (_globalSupply != null) {
			// tags specific to producers
			String[] tagsSupplier = { "REQ", "CONTRACT1", "CONTRACT2", "SATISFIED" };
			for (String tag : tagsSupplier) {
				if (!inputTags.contains(tag)) {
					inputTags.add(tag);
				}
			}
		}
		if (_globalNeed != null) {
			// tags specific to consumers
			String[] tagConsumer = { "PROD", "OFFER", "PROD_CONFIRM" };
			for (String tag : tagConsumer) {
				if (!inputTags.contains(tag)) {
					inputTags.add(tag);
				}
			}
		}
		return SapereUtil.toStrArray(inputTags);
	}

	private static String[] getOutputTags(IEnergySupply _globalSupply, IEnergyRequest _globalNeed, NodeContext nodeContext) {
		List<String> outputTags = new ArrayList<String>();
		// common tags :
		String[] commonTags =  { "EVENT", "DISABLED" };
		outputTags.addAll(Arrays.asList(commonTags));
		if (_globalSupply != null) {
			// tags specific to producers
			String[] tagsSupplier = { "PROD", "OFFER", "PROD_CONFIRM"};
			for (String tag : tagsSupplier) {
				if (!outputTags.contains(tag)) {
					outputTags.add(tag);
				}
			}
		}
		if (_globalNeed != null) {
			// tags specific to consumers
			String[] tagConsumer = { "REQ", "SATISFIED", "CONTRACT1", "CONTRACT2"};
			for (String tag : tagConsumer) {
				if (!outputTags.contains(tag)) {
					outputTags.add(tag);
				}
			}
		}
		return SapereUtil.toStrArray(outputTags);
	}


	private void updateLsaTags(NodeContext nodeContext) {
		String[] lsaInputTags = getInputTags(this.globalProduction, this.globalNeed, nodeContext);
		String[] lsaOutputTags = getOutputTags(this.globalProduction, this.globalNeed, nodeContext);
		super.updateLsaPropertyTags(lsaInputTags, lsaOutputTags);
	}

	// merged
	@Override
	public void initFields(EnergySupply _globalProduction, EnergyRequest _globalNeed, IEnergyAgentPolicy _producerPolicy, IEnergyAgentPolicy _consumerPolicy, NodeContext nodeContext) {
		if(_globalProduction != null && _globalProduction.getIssuerProperties() != null && _globalProduction.getIssuerProperties().getLocation() == null) {
			logger.error("initFields :_globalProduction  parameter has no location");
		}
		if(_globalNeed != null && _globalNeed.getIssuerProperties() != null && _globalNeed.getIssuerProperties().getLocation() == null) {
			logger.error("initFields : _globalNeed parameter has no location");
		}
		this.initGlobalProduction(_globalProduction);
		this.initGlobalNeed(_globalNeed);	// Begin-Producer End
		int decay = 1 + getTimeLeftSec(true);
		this.addDecay(decay);
		if(_consumerPolicy instanceof IConsumerPolicy) {
			this.consumerPolicy = (IConsumerPolicy) _consumerPolicy;
		} else {
			this.consumerPolicy = null;
		}
		// Begin-producer
		if(_producerPolicy instanceof IProducerPolicy) {
			this.producerPolicy = (IProducerPolicy) _producerPolicy;
		} else if (_producerPolicy == null){
			//this.producerPolicy = null;
			throw new RuntimeException("initFields : producerPolicy is not set");
		} else {
			throw new RuntimeException("initFields : producerPolicy has class class " + _producerPolicy.getClass());
		}
		// End-producer
		offersProcessingManager = new OffersProcessingManager(this, debugLevel);
		//this.tableSingleOffers = new HashMap<String, SingleOffer>();
		//this.globalOffer = new GlobalOffer(this);

		this.consumersProcessingManager = new ConsumersProcessingMangager(this);	// Begin-producer end
		debugLevel = 0;
		//receivedConfirmations = new HashMap<String, ConfirmationItem>();
		contractProcessingManager = new ContractProcessingManager(this);
		//completeOutputPropertyIfNeeded();
		/*
		if(nodeContext().getNodePredicitonSetting().isModelAggregationActivated()
		|| nodeContext().getClusterPredictionSetting().isModelAggregationActivated()) {
		}*/
		award = new AwardItem(agentName, null, 0.0, 0.0, 0.0);
		storage = new EnergyStorage(this.nodeContext);
		logger.info("Prosumer " + agentName + " end constructor : For debug");
	}

	// merged
	public IConsumerPolicy getConsumerPolicy() {
		return consumerPolicy;
	}

	// merged
	@Override
	public IProducerPolicy getProducerPolicy() {
		return producerPolicy;
	}

	// merged
	@Override
	public void reinitialize(int id, AgentAuthentication authentication, EnergySupply globalSupply, EnergyRequest globalNeed
			, IEnergyAgentPolicy _producerPolicy , IEnergyAgentPolicy _consumerPolicy, NodeContext nodeContext) {
		super.auxReinitializeLSA(id, authentication, getInputTags(globalSupply, globalNeed, nodeContext),
				getOutputTags(globalSupply, globalNeed, nodeContext), nodeContext);
		initFields(globalSupply, globalNeed, _producerPolicy, _consumerPolicy, nodeContext);
		checkIssuerLocation("reinitialize end");
	}

	// merged
	@Override
	public boolean isProducer() {
		if(globalProduction != null && globalNeed == null) {
			return true;
		}
		if(globalProduction != null && globalNeed != null ) {
			return globalProduction.getPower() > 0.0;
		}
		return (globalProduction != null) && globalProduction.getIssuerProperties().getDeviceProperties().getCategory().isProducer();
	}

	// merged
	@Override
	public boolean isConsumer() {
		if(globalNeed != null && globalProduction == null) {
			return true;
		}
		if(globalNeed != null && globalProduction != null ) {
			return globalNeed.getPower() > 0.0;
		}
		return (globalNeed != null) && !globalNeed.getIssuerProperties().getDeviceProperties().getCategory().isProducer();
	}

	public ProsumerRole computeRole() {
		if (isProducer()) {
			return ProsumerRole.PRODUCER;
		} else if (isConsumer()) {
			return ProsumerRole.CONSUMER;
		}
		return null;
	}

	// merged
	@Override
	public EnergySupply getGlobalProduction() {
		if(globalProduction != null) {
			return this.globalProduction.generateSimpleSupply();
		}
		return null;
	}

	@Override
	public EnergyRequest getGlobalNeed() {
		if(globalNeed != null) {
			return this.globalNeed.generateSimpleRequest();
		}
		return null;
	}

	// merged
	@Override
	public void initGlobalProduction(EnergySupply supply) {
		if(supply != null) {
			this.globalProduction = supply.generateProsumerEnergySupply();
		}
	}

	public void initGlobalNeed(EnergyRequest need) {
		if(need != null) {
			this.globalNeed = need.generateProsumerEnergyRequest();
		}
	}

	// no merge to do
	// specific to consumer
	public boolean isOfferOK(CompositeOffer globalOffer, boolean isComplementary) {
		EnergyRequest missing = contractProcessingManager.generateMissingRequest(this);
		//double missing = this.need.getPower();
		/*
		if(globalOffer.isComplementary()) {
			missing = Math.max(0, missing - contractProcessingManager.getOnGoingContractsPower());
		}*/
		return globalOffer.isActive()
				&& (globalOffer.isComplementary() == isComplementary)
				&& globalOffer.getPower() >= (missing.getPower() - 0.0001)
				&& (globalOffer.getPower() > 0);
	}

	// merged
	@Override
	public void onBondNotification(BondEvent event) {
		//checkIsserLocation("onBondNotification begin");
		try {
			Lsa bondedLsa = event.getBondedLsa();
			if("_Prosumer_N2_1".equals(agentName)) {
				logger.info("onBondNotification " + this.agentName + " For debug");
			}
			// Begin-Producer
			if(this.hasExpired() ) {
				 //NodeManager.instance().getNotifier().unsubscribe(this.agentName);
				 logger.info("onBondNotification : " + this.agentName + " has expired");
			}
			lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
			if(hasTargetedProperty(bondedLsa) || false) {
				Lsa chosenLSA = bondedLsa;
				String bondAgentName = chosenLSA.getAgentName(); // Begin-Producer End
				try {
					bondedLsa = completeInvolvedLocations(bondedLsa, logger);
					// Check if there is a contract to bind
					consumersProcessingManager.addBondedRequest(bondedLsa);	// Begin-Producer End
					AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
					// Begin-Producer-merge
					if (this.isActive()) {
						if(bondedLsa.isPropagated()) {
							logger.info("onBondNotification : eventLsa.getAgentName() = " + bondedLsa.getAgentName());
						}
						if (AgentType.PROSUMER.equals(bondAgentType)) {
							String prosumer = bondAgentName;
							Property pDisabled = chosenLSA.getOnePropertyByName("DISABLED");
							boolean prosumerIsDisabled = pDisabled!= null && pDisabled.getValue() instanceof RegulationWarning ;
							if (prosumerIsDisabled) {
								RegulationWarning warning = (RegulationWarning) pDisabled.getValue();
								// handle producer disabled property
								// Stop Contract if a producer is disabled
								// Remove waiting offer, request, contract
								if(contractProcessingManager.hasProducer(prosumer)) {
									contractProcessingManager.stopCurrentContracts(this, warning, this.agentName + " received a warning : " + warning);
								}
								consumersProcessingManager.removeConsumer(prosumer, "consumer disabled ");
							} else {
								// Handle properties from producers
								// Handle offers from producers
								Property pOffer = bondedLsa.getOnePropertyByQueryAndName(this.agentName, "OFFER");
								// Get offer of production agent
								if(pOffer!=null && pOffer.getValue() instanceof ProtectedSingleOffer) {
									// handle producer offer
									ProtectedSingleOffer protectedOffer = (ProtectedSingleOffer)  pOffer.getValue();
									handleProducerOffer(protectedOffer, bondedLsa);
								}
								// Handle confirmations from producers
								Object oConfirm = chosenLSA.getOnePropertyValueByName("PROD_CONFIRM");
								if (oConfirm instanceof ProtectedConfirmationTable) {
									ProtectedConfirmationTable producerConfirmTable = (ProtectedConfirmationTable) oConfirm;
									// handle producer confirmation
									contractProcessingManager.handleProducerConfirmation(this,  prosumer,  producerConfirmTable) ;
								}
								if(this.consumerPolicy != null) {
									// retrieve pricing table from producers
									Property pProd = chosenLSA.getOnePropertyByName("PROD");
									if(pProd!=null && pProd.getValue() instanceof EnergySupply) {
										EnergySupply supply = (EnergySupply)pProd.getValue();
										consumerPolicy.setProducerPricingTable(supply);
									}
								}
								// Handle properties from consumer
								// Check if there is a consumer request to handle
								// Handle requests
								Object oRequested = chosenLSA.getOnePropertyValueByName("REQ");
								if(oRequested instanceof EnergyRequest) {
									if(this.hasHighWarningDuration()) {
										logger.info("onBondNotification " + this.agentName  + " REQ prop = " + oRequested);
									}
									// handle consumer request
									consumersProcessingManager.handleConsumerRequest(this, (EnergyRequest) oRequested, prosumer, chosenLSA);
								}
								Object oMainContract = chosenLSA.getOnePropertyValueByName("CONTRACT1");
								Object oSecondContract = chosenLSA.getOnePropertyValueByName("CONTRACT2");
								Object oSatisfied = chosenLSA.getOnePropertyValueByName("SATISFIED");
								if (oMainContract instanceof ProtectedContract) {
									// handle consumer main contract
									ProtectedContract mainContract = ((ProtectedContract) oMainContract).clone();
									consumersProcessingManager.handleConsumerContract(this, mainContract, prosumer, debugLevel);
								}
								if(oSecondContract instanceof ProtectedContract) {
									// handle consumer complementary contract
									ProtectedContract secondContract = ((ProtectedContract) oSecondContract).clone();
									consumersProcessingManager.handleConsumerContract(this, secondContract, prosumer, debugLevel);
								}
								if(oSatisfied!=null && "1".equals(oSatisfied)) {
									// handle consumer confirmation
									consumersProcessingManager.addReceivedConfirmation(this, prosumer);
								}
							}
						}
						// Begin-Producer-merge
					}
					if (AgentType.REGULATOR.equals(bondAgentType)) {
						// Begin-Producer-merge
						Object oReschedule = chosenLSA.getOnePropertyValueByName("RESCHEDULE");
						if(oReschedule instanceof RescheduleTable) {
							// handle reschedule
							RescheduleTable rescheduleTable = (RescheduleTable) oReschedule;
							this.handleReschedule(rescheduleTable);
						}
						Object oAwardTable = chosenLSA.getOnePropertyValueByName("AWARDS");
						if(oAwardTable instanceof AwardsTable) {
							// handle reception of new awards (or penalties ...)
							AwardsTable awardsTable = (AwardsTable) oAwardTable;
							handleAwards(awardsTable);
						}
						List<Property> listWarningProp = chosenLSA.getPropertiesByName("WARNING");
						if(listWarningProp.size() > 1) {
							logger.info("onBondNotification : " + this.agentName + " listWarningProp.size = " + listWarningProp.size());
						}
						for(Property pWarning : listWarningProp) {
							if(pWarning!=null && pWarning.getValue() instanceof RegulationWarning) {
								RegulationWarning warning = (RegulationWarning) pWarning.getValue();
								// handle regulation warning
								handleWarning(warning);
							}
						}
						// End-Producer-merge
					}
					//if (AgentType.LEARNING_AGENT.equals(bondAgentType)) {
					//	Object oTotal = chosenLSA.getOnePropertyValueByName("TOTAL");
					//	if(oTotal instanceof NodeTotal) {
					//		lastNodeTotal2 = (NodeTotal) oTotal;
					//	}
					//}
				} catch (Throwable e) {
					logger.error(e);
					logger.error("### Exception thronw in  " +  this.agentName + " bond handling of " + event  + " : " + e.getMessage());

				}
			}
		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception thrown in bond evt handler " + agentName + " " + event + " " + e.getMessage());
		}
		//checkIsserLocation("onBondNotification end");
	}


	// merged
	@Override
	public void handleWarningOverConsumption(RegulationWarning warning) throws HandlingException {
		// Begin-Producer
		for (String consumer : consumersProcessingManager.getTableValidContracts().keySet()) {
			if (warning.hasAgent(consumer)) {
				consumersProcessingManager.stopContracts(this, consumer,
						"The agent received a regulation warning : " + warning.getType());
			}
		}
		// End-Producer
		if (warning.hasAgent(agentName)) {
			if (!this.isDisabled()) {
				// Disable prosumer
				disableAgent(warning);
			}
		}
	}

	// merged
	@Override
	public void handleWarningOverProduction(RegulationWarning warning) throws HandlingException {
		// Begin-Producer
		if(warning.hasAgent(agentName) && !this.isDisabled()) {
			logger.info("handleWarningOverProduction " + agentName + "The agent received a regulation warning : "+warning.getType());
			disableAgent(warning);
		}
		// End-Producer
	}

	// merged
	@Override
	public void handleWarningUserInteruption(RegulationWarning warning) throws HandlingException {
		// Begin-Producer-merge
		if(warning.hasAgent(agentName)) {
			if(!this.isDisabled()) {
				logger.info("handleWarningUserInteruption " + agentName + "The agent received a regulation warning : "+warning.getType());
				// Disable prosuer
				disableAgent(warning);
			}
		}
		// End-Producer-merge
	}

	// merge done
	@Override
	public void handleWarningChangeRequest(RegulationWarning warning) throws HandlingException {
		//checkIsserLocation("handleWarningChangeRequest begin");
		if(warning.getChangeRequest()!=null) {
			ChangeRequest changeRequest = warning.getChangeRequest();
			if(warning.hasAgent(agentName)) {
				ProsumerRole currentRole = computeRole();
				boolean switchRole = !changeRequest.getProsumerRole().equals(currentRole);
				if (switchRole) {
					logger.info("handleWarningChangeRequest " + this.agentName + " switch from " + currentRole + " to "
							+ changeRequest.getProsumerRole());
				}
				if(ProsumerRole.CONSUMER.equals(changeRequest.getProsumerRole())) {
					updateRequest(changeRequest.getRequest());
				} else if(ProsumerRole.PRODUCER.equals(changeRequest.getProsumerRole())) {
					updateSupply(changeRequest.getSupply(), warning);
				}
				if(switchRole) {
					updateLsaTags(nodeContext);
				}
			}
			// Begin-Producer
			Map<String, List<ReducedContract>> validContracts = consumersProcessingManager.getTableValidContracts();
			logger.info("handleWarningChangeRequest " + agentName + " " + warning + ", validContracts for consumers  = " + validContracts.keySet() );
			for (String consumer : validContracts.keySet()) {
				if (warning.hasAgent(consumer) && warning.getChangeRequest() != null) {
					// The change request concerns on of the provided consumers
					List<ReducedContract> listContracts = validContracts.get(consumer);
					for (ReducedContract contract : listContracts) {
						// ReducedContract contract = validContracts.get(consumer);
						// Stop the contract is the requested power has increased
						if (!nodeContext.isComplementaryRequestsActivated()
								&& changeRequest.getSupply().getPower() > contract.getRequest().getPower()) {
							String comment = "The agent " + consumer
									+ " received a request for a change with an increase in requested power : ";
							comment = comment + "new demand " + UtilDates.df3.format(changeRequest.getSupply().getPower())
									+ " contract tt power : "
									+ UtilDates.df3.format(contract.getRequest().getPower()) + " contract = "
									+ contract;
							consumersProcessingManager.stopContract(this, consumer, contract.isComplementary(),
									comment);
						} else {
						}
					}
				}
			}
			// End-producer
		}
		//checkIsserLocation("handleWarningChangeRequest end");
	}

	@Override
	public void handleAwards(AwardsTable awardsTable) throws HandlingException {
		if (awardsTable.hasAgentAwardItem(agentName)) {
			AwardItem newAward = awardsTable.getAward(agentName);
			if (award.getCalculationDate() == null
					|| award.getCalculationDate().before(newAward.getCalculationDate())) {
				this.award.add(newAward);
				this.replacePropertyWithName(new Property("_DEBUG_AWARD_", award));
			}
			logger.info("handleAwards  " + this.agentName + " : award = " + this.award);
		}
	}

	// Specific to producer
	// No merge to do
	private void updateSupply(EnergySupply newSupply, RegulationWarning warning) {
		//disableAgent(warning);
		if(newSupply.getIssuerProperties().getLocation() == null) {
			newSupply.getIssuerProperties().setLocation(this.nodeContext.getNodeLocation().clone());
		}
		//checkIssuerLocation("updateSupply begin");
		try {
			boolean wasConsumer = isConsumer();
			if (globalProduction != null && !globalProduction.hasChanged(newSupply)) {
				logger.info("updateSupply " + newSupply + " nothing has changed");
				// nothing to do
				return;
			}
			double powerBefore = (globalProduction == null) ? 0 : globalProduction.getPower();
			boolean switchToProducer = wasConsumer;
			if(switchToProducer) {
				//"PROD", "OFFER", "PROD_CONFIRM", "REQ", "CONTRACT1", "CONTRACT2", "SATISFIED", "DISABLED", "EVENT"
				// The prosumer won't be consumer any more : stop its contrats as a consumer
				this.contractProcessingManager.stopCurrentContracts(this, warning, agentName + " switch from consumer to producer");				
			}
			this.globalProduction = newSupply.generateProsumerEnergySupply();
			// For the moment : no supply and request at the same time
			this.globalNeed = null;
			// Refresh PROD property
			if(switchToProducer) {
				lsa.removePropertiesByNames(new String[] {"REQ", "SATISFIED"});
			}
			//checkIssuerLocation("updateSupply 2");
			Map<String, List<ReducedContract>> validContracts = consumersProcessingManager.getTableValidContracts();
			if(newSupply.getPower() < powerBefore) {
				// Stop contract if not the agent has not enougth availability
				while(consumersProcessingManager.computeAvailablePower(this, false, false) < 0 && validContracts.size() > 0) {
					// Remove contract
					List<String> consumers = new ArrayList<>();
					consumers.addAll(validContracts.keySet());
					Collections.shuffle(consumers);
					// TODO : sort contracts by request priority and power
					String consumer = consumers.get(0);
					double difference = powerBefore - newSupply.getPower();
					consumersProcessingManager.stopContracts(this, consumer, "The producer agent " + agentName
							+ " has received a change request with a decrease in power output of " + UtilDates.df5.format(difference) + " Watts. "+ WarningType.CHANGE_REQUEST);
					validContracts = consumersProcessingManager.getTableValidContracts();
				}
			}
			if(switchToProducer) {
				generateSwitchEvent(WarningType.CHANGE_REQUEST);
			} else {
				generateUpdateEvent(WarningType.CHANGE_REQUEST);
			}
			// Refresh PROD property
			replacePropertyWithName(new Property("PROD", this.globalProduction.clone()));
		} catch (Throwable e) {
			logger.error(e);
		}
		//checkIssuerLocation("updateSupply end");
	}

	// No merge to do
	private void auxUpdateRequest(EnergyRequest changeRequest) {
		//checkIssuerLocation("auxUpdateRequest begin");
		//logger.info("auxUpdateRequest " + agentName + " changeRequest = " + changeRequest);
		if(changeRequest.getIssuerProperties().getLocation() == null) {
			changeRequest.getIssuerProperties().setLocation(this.nodeContext.getNodeLocation().clone());
		}
		try {
			if(globalNeed == null) {
				globalNeed = changeRequest.generateProsumerEnergyRequest();
			} else {
				changeRequest.checkPowers();
				changeRequest.checkDates();
				EnergyRequest changeRequestCopy = changeRequest.clone();
				this.globalNeed.setBeginDate(changeRequestCopy.getBeginDate());
				this.globalNeed.checkBeginNotPassed();
				logger.info("auxUpdateRequest : after checkBeginNotPassed");
				changeRequest.checkDates();
				this.globalNeed.setEndDate(changeRequestCopy.getEndDate());
				this.globalNeed.setInternalPowerSlot(changeRequestCopy.getPowerSlot());
				this.globalNeed.checkPowers();
				this.addDecay(1 + 30 * globalNeed.getTimeLeftSec(true));
				this.globalNeed.setDelayToleranceMinutes(changeRequestCopy.getDelayToleranceMinutes());
				this.globalNeed.setPriorityLevel(changeRequestCopy.getPriorityLevel());
			}
			// For the moment : no supply and request at the same time
			this.globalProduction = null;
			logger.info("auxUpdateRequest " + agentName + " : changeRequest = " + changeRequest + ", globalNeed = " + globalNeed);
		}
		catch (Exception e) {
			logger.error(e);
		}
		//checkIssuerLocation("auxUpdateRequest end");
	}

	// merge done
	@Override
	public void disableAgent(RegulationWarning warning) throws HandlingException {
		if(globalNeed != null) {
			globalNeed.setDisabled(true);
		}
		// Begin-Producer
		if(globalProduction != null) {
			this.globalProduction.setDisabled(true);
		}
		// End-Producer
		contractProcessingManager.stopCurrentContracts(this, warning, agentName + " received a warning : " + warning);
		consumersProcessingManager.stopAllContracts(this); // Begin-producer End
		super.disableAgent(warning, new String[] {"REQ", "SATISFIED", "CONTRACT1", "CONTRACT2", "PROD_CONFIRM", "OFFER" }); 
		// Begin-producer-merge End
	}

	// merge done
	@Override
	public Set<String> getLinkedAgents() {
		// Begin-Producer
		if (isProducer()) {
			return consumersProcessingManager.getLinkedAgents();
		} else {
			// End-Producer
			Set<String> result = new HashSet<>();
			if (isSatisfied()) {
				for (String agent : contractProcessingManager.getProducerAgents()) {
					result.add(agent);
				}
			}
			return result;
		}
	}

	// merge done
	@Override
	public boolean isSatisfied() {
		// Begin-Producer
		if (isProducer()) {
			return false;
		} else {
			// End-Producer
			return contractProcessingManager.isConsumerSatisfied(this);
		}
	}

	// merge : nothing to do
	// Specific to consumer
	public boolean isInWarning() {
		if(globalNeed!=null) {
			return globalNeed.getWarningDurationSec()>0;
		}
		return false;
	}

	// merge done
	@Override
	public PowerSlot getForcastOngoingContractsPowerSlot(String location, Date aDate) {
		// Begin-Producer
		if(isProducer()) {
			return consumersProcessingManager.getForcastOngoingContractsPowerSlot(location, this, aDate);
		} else {
			// End-Producer
			return contractProcessingManager.getForcastOngoingContractsPower(location, aDate);
		}
	}

	// no merge to do
	public double getContractsPowerAsConsumer() {
		return contractProcessingManager.getContractsPower();
	}

	/**
	 * Power provided by producer
	 *
	 * @return
	 */
	// merge done
	@Override
	public PowerSlot getOngoingContractsPowerSlot(String locationFilter) {
		// Begin-Producer
		if(isProducer()) {
			return consumersProcessingManager.getOngoingContractsPowerSlot(locationFilter, this);
		} else {
			// End-Producer
			return contractProcessingManager.getOngoingContractsPower(locationFilter);
		}
	}

	// merge done
	@Override
	public Map<String, PowerSlot> getOngoingContractsRepartition() {
		// Begin-Producer
		if (isProducer()) {
			return consumersProcessingManager.getOngoingContractsRepartition(this);
		} else {
			// End-Producer
			return contractProcessingManager.getOngoinContractsRepartition();
		}
	}

	// merge done
	@Override
	public Map<String, Double> getOffersRepartition() {
		// Begin-Producer
		if (isProducer()) {
			return consumersProcessingManager.getOffersRepartition();
		} else {
			// End-Producer
			return offersProcessingManager.getOffersRepartition();
		}
	}

	// merge done
	@Override
	public Double getOffersTotal() {
		// Begin-Producer
		if (isProducer()) {
			return consumersProcessingManager.getOffersTotal();
		} else {
			// End-Producer
			return offersProcessingManager.getOffersTotal();
		}
	}

	// merge done
	@Override
	public List<String> getConsumersOfWaitingContrats() {
		// Begin-Producer
		if (isProducer()) {
			return consumersProcessingManager.getConsumersOfWaitingContrats();
		} else {
			// End-Producer
			return new ArrayList<String>();
		}
	}

	// merge done
	@Override
	public PowerSlot getWaitingContratsPowerSlot() {
		// Begin-Producer
		if (isProducer()) {
			return consumersProcessingManager.getWaitingContratsPowerSlot(this);
		} else {
			// End-Producer
			return new PowerSlot();
		}
	}

	// merge done
	@Override
	public Double computeAvailablePower() {
		// Begin-Producer
		if (hasExpired() || isDisabled()) {
			return 0.0;
		}
		if (isProducer()) {
			PowerSlot contractsPowerSlot = getOngoingContractsPowerSlot(null);
			return Math.max(0, globalProduction.getPower() - contractsPowerSlot.getMax());
		}
		// End-Producer
		return 0.0;
	}

	// merge done
	@Override
	public Double computeMissingPower() {
		if(hasExpired() || isDisabled() || isStartInFutur()) {
			return 0.0;
		}
		// Begin-Producer
		if(!isConsumer()) {
			return 0.0;
		}
		// End-Producer
		PowerSlot contractsPowerSlot = getOngoingContractsPowerSlot(null);
		return Math.max(0, globalNeed.getPower() - contractsPowerSlot.getCurrent());
	}

	// merge done
	@Override
	public EventType getStartEventType() {
		// Begin-Producer
		if(isProducer()) {
			return EventType.PRODUCTION_START;
		} else {
			// End-Producer
			return EventType.REQUEST_START;
		}
	}

	// merge done
	@Override
	public EventType getUpdateEventType() {
		// Begin-Producer
		if (isProducer()) {
			return EventType.PRODUCTION_UPDATE;
		} else {
			// End-Producer
			return EventType.REQUEST_UPDATE;
		}
	}

	@Override
	public EventType getSwitchEventType() {
		if (isProducer()) {
			return EventType.PRODUCTION_SWITCH;
		} else {
			return EventType.REQUEST_SWITCH;
		}
	}

	// merge done
	@Override
	public EventType getExpiryEventType() {
		// Begin-Producer
		if (isProducer()) {
			return EventType.PRODUCTION_EXPIRY;
		} else {
			// End-Producer
			return EventType.REQUEST_EXPIRY;
		}
	}

	// merge done
	@Override
	public EventType getStopEventType() {
		// Begin-Producer
		if (isProducer()) {
			return EventType.PRODUCTION_STOP;
		} else {
			// End-Producer
			return EventType.REQUEST_STOP;
		}
	}

	public void applyProsumerPolicies() throws HandlingException {
		//checkIssuerLocation("applyProsumerPolicies begin");
		if(globalNeed != null) {
			// update award credit on global need
			globalNeed.setAwardsCredit(this.award.computeBalance());
			if(consumerPolicy != null && !this.hasExpired() && (this.isStartInFutur() || getContractsPowerAsConsumer() == 0)) {
				if(this.getOffersTotal() ==0) {
					EnergyRequest newRequest = consumerPolicy.updateRequest(this.globalNeed.generateSimpleRequest());
					if(this.globalNeed.hasChanged(newRequest)) {
						EnergyEvent startEvent = getStartEvent();
						logger.info("applyProsumerPolicies before updateRequest " + newRequest + ", startEvent = " + startEvent);
						if(startEvent != null) {
							logger.info("applyProsumerPolicies before updateRequest : for debug ");
						}
						updateRequest(newRequest);
					}
				}
			}
			if(nodeContext.isEnergyStorageActivated()) {
				// Use energy from storage
				if(this.storage.computeBalanceSavedWH() > 2.0 && this.globalNeed.getPower() > 10.0 && !globalNeed.hasAdditionalPower()) {
					double needPowerBefore = globalNeed.getPower();
					ExtraSupply withDrawForNeed = storage.withdrawEnergy(getCurrentDate(), globalNeed.getEndDate(), globalNeed.getRemainWH(), ProsumerRole.CONSUMER);
					logger.info("applyProsumerPolicies " + this.agentName + " : globalNeed before " + globalNeed );
					globalNeed.setExtraSupply(withDrawForNeed);
					logger.info("applyProsumerPolicies " + this.agentName + " : globalNeed after  " + globalNeed );
					double needPowerAfter = globalNeed.getPower();
					if(Math.abs(needPowerAfter - needPowerBefore)  >= 0.0001 ) {
						logger.info("applyProsumerPolicies " + this.agentName + " : use of additional power from battery " + withDrawForNeed
								+ ", needPowerBefore = " + needPowerBefore + ", needPowerAfter = " + needPowerAfter);
						generateUpdateEvent(WarningType.BATTERY_USAGE);
						contractProcessingManager.handleRequestChanges(this, "use of energy storage");
					}
				}
				this.replacePropertyWithName(new Property("_DEBUG_STORAGE_", storage));
			}
		}
		// Begin-Producer
		if (globalProduction != null && producerPolicy.hasDefaultPrices() && !this.hasExpired()) {
			logger.info("applyProsumerPolicies : " + agentName + " producerPolicy has default price");
			// Map<String, List<ReducedContract>> mapContracts =
			// consumersProcessingManager.getTableValidContracts();
			PricingTable newPricingTable = producerPolicy.getPricingTable(this);
			if (globalProduction != null && newPricingTable != null) {
				this.globalProduction.setPricingTable(newPricingTable);
				// refresh PROD property LSA
				replacePropertyWithName(new Property("PROD", this.globalProduction));
			}
		}
		//checkIssuerLocation("applyProsumerPolicies end");
	}

	public void checkIssuerLocation(String step) {
		if(globalProduction != null && globalProduction.getIssuerProperties() != null && globalProduction.getIssuerProperties().getLocation() == null) {
			logger.error("checkIsserLocation " + this.agentName + " " + step + " : location is null in globalProduction");
			globalProduction.getIssuerProperties().setLocation(nodeContext.getNodeLocation().clone());
		}
		if(globalNeed != null && globalNeed.getIssuerProperties() != null && globalNeed.getIssuerProperties().getLocation() == null) {
			logger.error("checkIsserLocation " + this.agentName + " " + step + " location is null in globalNeed");
			globalNeed.getIssuerProperties().setLocation(nodeContext.getNodeLocation().clone());
		}
	}

	// merge done
	@Override
	public void onDecayedNotification(DecayedEvent event) { // change to return only one result
		try {
			//checkIssuerLocation("onDecayedNotification begin");
			if("_Prosumer_N1_23".equalsIgnoreCase(agentName)) {
				logger.info("onDecayedNotification for debug");
			}
			// Remove obsolete data ( offers, warnings )
			cleanExpiredData();

			// check agent expiration
			checkAgentExpiration(event);

			// Apply producer and consumer policies
			applyProsumerPolicies();

			// For debug : check availability
			long deltaMS = 0;
			if(!firstDecay && timeLastDecay!=null) {
				Date current = getCurrentDate();
				deltaMS = current.getTime() - timeLastDecay.getTime();
				if(deltaMS > 2*1000) {
					logger.warning("onDecayedNotification " + this.agentName + " : last decay at " + UtilDates.format_time.format(timeLastDecay));
				}
			}
			timeLastDecay = getCurrentDate();
			firstDecay = false;
			if(isProducer()) {
				if(this.hasExpired()) {
					logger.info("onDecayedNotification for debug : producer " + agentName + " has expired");
					//  Stop all ongoing contracts
					for(ReducedContract contract : consumersProcessingManager.getValidContracts()) {
						String contractIssuer = contract.getIssuerProperties() == null ? null : contract.getIssuerProperties().getAgentName();
						logger.info("onDecayedNotification try to stop contract of " + contractIssuer);
						consumersProcessingManager.sendConfirmation(this,contract, false, agentName + " : expiration");
					}
				} else {
					// send confirmations to consumers agents
					consumersProcessingManager.confirmAvailabilityToConsumers(this);
					// add Prod tag if needed
					if (lsa.getPropertiesByName("PROD").isEmpty()) {
						addProperty(new Property("PROD", this.globalProduction));
					}
					// Update savedEnergyWH
					if(storage.isActivated()) {
						double toAddInWH = (this.computeAvailablePower() * deltaMS) / (3600*1000);
						if(toAddInWH > 0) {
							storage.addSavedWH(toAddInWH);
							//this.savedEnergyWH+=toAddInWH;
							logger.warning("onDecayedNotification " + this.agentName + " : toAddInWH = " + SapereUtil.roundPower(toAddInWH)
								+ ", savedEnergyWH = " + SapereUtil.roundPower(storage.getTotalSavedWH()));
						}
					}
				}
			}
			// Check received confirmation from consumers agents
			if(isConsumer()) {
				consumersProcessingManager.checkReceivedConfirmations(this);
			}
			if(this.isDisabled()) {
				// Stop all contracts
				consumersProcessingManager.stopAllContracts(this);
			}
			// Retrieve the node availability from the last stored node total
			lastNodeTotal = EnergyDbHelper.retrieveLastNodeTotal();
			if(!this.isDisabled() && !this.hasExpired() && this.isProducer()) {
				int nbNewOFfers = consumersProcessingManager.generateNewOffers(this, lastNodeTotal);
				if(nbNewOFfers>0) {
					logger.info("nbNewOFfers = " + nbNewOFfers);
				}
			}
			// End-Producer

			refreshRequestWarnings();
			tryReactivation();

			if (isConsumer()) {
				// Clean contract
				contractProcessingManager.cleanContracts(this);
				// check if the contract needs to be updated due to a change in request
				contractProcessingManager.handleRequestChanges(this, "onDecayedNotification");

				// Check producers confirmations
				contractProcessingManager.checkProducersConfirmations(this);

				// Generate new contract if needed
				generateNewContract();

				// merge main and complementary contracts if both are ongoing
				contractProcessingManager.mergeContracts(this);

				// Refresh properties posted in LSA
				contractProcessingManager.refreshLsaProperties(this);
			}

			// Post not sent properties
			checkWaitingProperties();	// Begin-Producer End

			// For spreading
			activateSpreading();

		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception thrown in ConsumerAgent.onDecayedNotification " + this.agentName + " " + event + " " + e.getMessage());
		}
		//checkIssuerLocation("onDecayedNotification end");
	}

	// Specific to producer
	// No merge to do
	/**
	 * Remove offers submited on LSA
	 */
	public void cleanOfferProperties() {
		String offerKey = getOfferKeyToRemove();
		while (offerKey != null) {
			// For debug
			try {
				debug_checkOfferAcquitted(offerKey);
			} catch (HandlingException e) {
				logger.error(e);
			}
			lsa.removePropertiesByQueryAndName(offerKey, "OFFER");
			offerKey = getOfferKeyToRemove();
		}
	}

	/**
	 * Remove expired contracts
	 */
	public void cleanContractProperties() {
		String[] contractPropertyNames = { "CONTRACT1", "CONTRACT2" };
		for (String propName : contractPropertyNames) {
			Object oContract = lsa.getOnePropertyValueByName(propName);
			if (oContract instanceof ProtectedContract) {
				// handle consumer main contract
				ProtectedContract protectedContract = ((ProtectedContract) oContract).clone();
				if (protectedContract.hasExpired()) {
					lsa.removePropertiesByName(propName);
				}
			}
		}
	}

	private void manageAwardCreditUsage() {
		if (isConsumer()) {
			boolean toUpdate = contractProcessingManager.checkAwardCreditUsage();
			// Retrieve award usage if the contract is about to expire
			if (toUpdate) {
				Map<Date, Double> usedCredits = contractProcessingManager.computeAllAwardCreditUsage("OnDecay > cleanExpiredData > manageAwardCreditUsage ");
				// update award
				if (!usedCredits.isEmpty()) {
					for (Date contractDate : usedCredits.keySet()) {
						Double nextUsedCredit = usedCredits.get(contractDate);
						this.award.addUsage(contractDate, nextUsedCredit);
						logger.info("manageAwardCreditUsage " + agentName + " nextUsedCredit = "
								+ SapereUtil.roundPower(nextUsedCredit));
					}
					//double awardCreditToSet = this.globalNeed.getAwardsCredit() - usedCredits;
					// globalNeed.setAwardsCredit(awardCreditToSet);
					//this.award.addUsage(getCurrentDate(), awardCreditToSet);
					// TODO reset
					//contractProcessingManager.clearAllAwardCreditUsage();
				}
			}
		}
	}

	// merge done
	@Override
	public void cleanExpiredData() {
		// Check received confirmation from consumers agents
		manageAwardCreditUsage();

		offersProcessingManager.cleanExpiredOffers(this);

		// Delete contract if invalid
		contractProcessingManager.resetCurrentContractIfInvalid(this);

		// Clean exprited request,offers,contracts
		consumersProcessingManager.cleanExpiredData(this, debugLevel); // Begin-producer End

		// Clean expired confirmations
		consumersProcessingManager.cleanConfirmationTable(this); // Begin-producer End

		// Clean expired warnings
		cleanExpiredWarnings();

		// Remove submited offers
		cleanOfferProperties(); // Begin-producer End

		// Remove expired contracts
		cleanContractProperties();

		// Clean event
		cleanEventProperties(); // Begin-producer End

	}

	// No merge to do
	private String getOfferKeyToRemove() {
		for (Property p : lsa.getProperties()) {
			if (true && "OFFER".equals(p.getName()) && p.getValue() instanceof ProtectedSingleOffer) {
				return p.getQuery();
			}
		}
		return null;
	}

	// Specific to producer
	// No merge to do
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

	// no merge to do
	// specific to consumer
	private void handleProducerOffer(ProtectedSingleOffer protectedOffer, Lsa bondedLsa) {
		try {
			SingleOffer receivedOffer =  protectedOffer.getSingleOffer(this);
			SingleOffer newOffer = receivedOffer.clone();
			ProsumerProperties offerIssuer = newOffer.getIssuerProperties();
			if(!NodeManager.isLocal(offerIssuer.getLocation())) {
				logger.info("handleProducerOffer " + this.agentName + " : receive offer from other node " + offerIssuer.getLocation().getName() + " : " + newOffer);
				if(!newOffer.checkLocation()) {
					// TODO : set locationId
					logger.error("handleProducerOffer offer has no location");
				}
				if(newOffer.hasExpired()) {
					logger.error("handleProducerOffer this offer has expired " + newOffer.getExpirationDurationSec() + " seconds ago");
				}
			}
			int isserDistance = bondedLsa.getSourceDistance();
			if(newOffer.getIssuerProperties()  != null) {
				newOffer.getIssuerProperties().setDistance(isserDistance);
			}
			boolean isComplementary = newOffer.isComplementary();
			boolean offerUsed = false;
			if (contractProcessingManager.needOffer(isComplementary)) {
				//if(!SapereUtil.hasProperty(lsa, agentName, "CONTRACT1")) {
				// Check the offer validity
				if(newOffer.hasExpired()) {
					logger.error("handleProducerOffer " + this.agentName + " offer has expired : " + newOffer.getExpirationDurationSec() + " seconds ago"
						+ ", newOffer.deadline = " + UtilDates.format_time.format(newOffer.getDeadline())
						+ ", currentTime = " + UtilDates.format_time.format(getCurrentDate())
						+ ", newOffer.getCreationTime = " + UtilDates.format_time.format(newOffer.getCreationTime())
						);
				} else if (globalNeed.isOK(newOffer)) {
					boolean hasNoOffer = !offersProcessingManager.hasSingleOffer();
					offersProcessingManager.addSingleOffer(newOffer);
					offerUsed = true;
					logger.info("handleProducerOffer " + this.agentName + " set offer used : id:" + newOffer.getId() + "  "  + newOffer.getProducerAgent() + " W = " + SapereUtil.roundPower(newOffer.getPower()));
					if(hasNoOffer) {
						this.useOffersDecay = useOfferInitialDecay;
					}
				}
				//}
			}
			EnergyDbHelper.setSingleOfferAcquitted(newOffer, getStartEvent(), offerUsed);
			newOffer.setAcquitted(true);
			if(!offerUsed /* && !isSatisfied()*/) {
				logger.info(this.agentName + " For debug : satisfied = " + isSatisfied());
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	// merge done
	@Override
	public void handleReschedule(RescheduleTable rescheduleTable) throws HandlingException {
		// handle reschedule
		if (rescheduleTable.hasItem(agentName)) {
			RescheduleItem rescheduleItem = rescheduleTable.getItem(agentName);
			if(this.globalProduction != null) {
				if (rescheduleItem.getStopBegin().before(globalProduction.getEndDate())) {
					// Modify contract end date
					globalProduction.setBeginDate(getCurrentDate());
					globalProduction.setEndDate(rescheduleItem.getStopBegin());
					generateUpdateEvent(rescheduleItem.getWarningType());
				}
			}
			if(this.globalNeed != null) {
				if (rescheduleItem.getStopBegin().before(globalNeed.getEndDate())) {
					// Modify contract end date
					globalNeed.setBeginDate(getCurrentDate());
					globalNeed.setEndDate(rescheduleItem.getStopBegin());
					generateUpdateEvent(rescheduleItem.getWarningType());
				}
			}
		}
		for(String producer : contractProcessingManager.getProducerAgents()) {
			if(rescheduleTable.hasItem(producer)) {
				RescheduleItem item = rescheduleTable.getItem(producer) ;
			}
		}
	}



	// merge done
	// specific to consumer
	private void updateRequest(EnergyRequest changeRequest) throws HandlingException {
		//checkIssuerLocation("updateRequest begin");
		if(this.hasExpired() || this.isDisabled()) {
			logger.warning("updateRequest " + this.agentName + " cannot be updated : agent expired or disabled");
			return;
		} else if(this.getStartEvent() == null)  {
			if(this.waitingStartEvent != null) {
				// Update request
				auxUpdateRequest(changeRequest);
			} else {
				logger.warning("updateRequest " + this.agentName + " cannot be updated : startEvent is null");
			}
			return;
		} else {
			// Update request
			boolean wasProducer = isProducer();
			boolean switchToConsumer = wasProducer;
			if (switchToConsumer) {
				// The prosumer won't be producer anymore : stop all contract for which the prosumer is provider
				consumersProcessingManager.stopAllContracts(this); // Begin-producer End
				lsa.removePropertiesByName("PROD");
			}
			auxUpdateRequest(changeRequest);
			if(globalNeed.getDelayToleranceMinutes() == 0.0) {
				logger.error("delayToleranceMinutes is null");
			}
			// Generate update event
			try {
				if (this.globalNeed.isStartInFutur()) {
					// Generate stop event and set waitingStartEvent
					RegulationWarning warning = new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(),
							getTimeShiftMS());
					EnergyEvent stopEvent = generateStopEvent(warning, "ChangeRequest with start in futur");
					logger.info("updateRequest " + this.agentName + " start in futur : after generateStopEvent : stopEvent = " + stopEvent);
					waitingStartEvent = this.generateEvent(getStartEventType(),"");
				} else {
					// udpdate start event
					if(switchToConsumer) {
						generateSwitchEvent(WarningType.CHANGE_REQUEST);
					} else {
						generateUpdateEvent(WarningType.CHANGE_REQUEST);
					}
				}
			} catch (Exception e) {
				logger.error(e);
			}
			logger.info("updateRequest " + this.agentName + " before checkupRequestChanges" );
			contractProcessingManager.handleRequestChanges(this, "updateRequest");
		}
		//checkIsserLocation("updateRequest begin");
	}


	// specific to consumer
	// no merge to do
	private void generateNewContract() throws HandlingException {
		if(globalNeed == null) {
			return;
		}
		int warningDuration = globalNeed.getWarningDurationSec();
		boolean isInHighWarning = warningDuration>=8;
		boolean isComplementary = (contractProcessingManager.getOngoingContractsPower() > 0);
		boolean hasAlreadyNewContract =  contractProcessingManager.isContractWaitingValidation(isComplementary);
		String msgTag = "handleReceivedOffers " + this.agentName + " [warningDuration=" + warningDuration + "] ";
		String contractTag = isComplementary?"CONTRACT2" :"CONTRACT1";
		// For debug
		/*
		String sConsumerWarning = (lastNodeTotal==null)? "" : lastNodeTotal.getMaxWarningConsumer();
		long warningDuration2 = (lastNodeTotal==null)? 0 : lastNodeTotal.getMaxWarningDuration();
		boolean isInHighWarning2 = (warningDuration2>=8) && (agentName.equals(sConsumerWarning));
		if(isInHighWarning2) {
			if(Math.abs(warningDuration2 - warningDuration) > 2) {
				logger.warning("### generateNewContract : " + agentName+ " local warningDuration = " + warningDuration
					+ ", warningDuration from node total = " + warningDuration2);
			}
		}*/
		// End for debug
		if(isInHighWarning) {
			// For debug
			logger.warning(msgTag + " begin : requested = " + UtilDates.df3.format(globalNeed.getPower()) + " hasConfirmation = " + hasAlreadyNewContract + " has offers = " + offersProcessingManager.hasSingleOffer());
			if(!hasAlreadyNewContract) {
				offersProcessingManager.logOffers(msgTag);
			}
			if(!offersProcessingManager.hasSingleOffer()) {
				logger.warning(msgTag + " REQ property = " + lsa.getPropertiesByName("REQ"));
			}
		}
		if(!this.isSatisfied() && !this.isDisabled() && !hasAlreadyNewContract && offersProcessingManager.hasSingleOffer()) {
			if(useOffersDecay > 0) {
				useOffersDecay--;
			} else {
				EnergyRequest missing = contractProcessingManager.generateMissingRequest(this);
				CompositeOffer globalOffer = offersProcessingManager.generateGlobalOffer(this, missing);
				if(!globalOffer.checkLocation()) {
					logger.error("after generateGlobalOffer : for debug : locationId is null");
				}
				if(isInHighWarning) {
					logger.warning(msgTag + " after generateGlobalOffer globalOffer = " + globalOffer);
				}
				// Check if the global offer can met the demand
				if(this.isOfferOK(globalOffer, isComplementary)) {
					if(isInHighWarning) {
						logger.warning(msgTag + " globalOffer is OK");
					}
					if(!lsa.getPropertiesByName(contractTag).isEmpty()) {
						logger.info(msgTag +  " contract is set");
					}
					// The Offer is OK : Confirm global offer to generate a contract
					/*
					AgentState state = new AgentState(Arrays.asList(getInput()), new ArrayList<String>());
					state.addOutput(contractTag);
					lsa.addSyntheticProperty(SyntheticPropertyName.STATE, state.toString());
					*/
					this.contractProcessingManager.generateNewContract(this, globalOffer);
					offersProcessingManager.clearSingleOffers();
				} else {
					if(isInHighWarning) {
						logger.warning(msgTag + " global offer is still not OK : total = " + globalOffer.getPower() + " content = " +  globalOffer);
					}
				}
			}
		} else {
			// For debug
			if(isInHighWarning) {
				if(lsa.getPropertiesByName(contractTag).isEmpty() &&  offersProcessingManager.hasSingleOffer()) {
					// For debug
					logger.warning(msgTag + " step666 : isSatisfied = " + isSatisfied() + "  isDisabled = " + this.isDisabled() );
				}
			}
		}
	}

	// no merge to do
	// specific to consumer
	private void refreshRequestWarnings() {
		if(globalNeed == null) {
			return;
		}
		if("_Prosumer_N1_51".equalsIgnoreCase(agentName)) {
			logger.info("refreshRequestWarnings for debug");
		}
		double nodeTotalAvailable = (lastNodeTotal==null)? 0 : lastNodeTotal.getAvailable();
		Date current = getCurrentDate();
		double missing = this.contractProcessingManager.getMissing(this);
		EnergyRequest reqProperty = this.contractProcessingManager.getRequestProperty(this);
		if(this.isSatisfied()) {
			globalNeed.resetWarningCounter(current);
			if(reqProperty != null) {
				reqProperty.resetWarningCounter(current);
			}
		} else if(missing > 0 && missing <= nodeTotalAvailable) {
			globalNeed.incrementWarningCounter(current);
			if(reqProperty != null) {
				// Syncrhonize the information of warning duration in need attribute and in the "REQ" property
				if(reqProperty.getWarningDurationSec() != globalNeed.getWarningDurationSec()) {
					logger.info("refreshRequestWarnings increment warning on req property : " + reqProperty);
					reqProperty.incrementWarningCounter(current);
				}
				if(reqProperty.getWarningDate()!=null && !reqProperty.getWarningDate().equals(globalNeed.getWarningDate())) {
					logger.error("warning dates are different " + SapereUtil.CR + "reqProperty : " + reqProperty + SapereUtil.CR + "need : " + globalNeed);
				}
				// For debug
				String sConsumerWarning = (lastNodeTotal==null)? "" : lastNodeTotal.getMaxWarningConsumer();
				long warningDurationLastTotal = (lastNodeTotal==null)? 0 : lastNodeTotal.getMaxWarningDuration();
				long gapMS = (lastNodeTotal==null)? 0 : getCurrentDate().getTime() - lastNodeTotal.getDate().getTime();
				boolean isInHighWarning = (warningDurationLastTotal>=8) && (agentName.equals(sConsumerWarning));
				long refreshedWarningDuration = (int) (gapMS/1000) + warningDurationLastTotal;
				if(isInHighWarning) {
					if(globalNeed.getWarningDurationSec() != reqProperty.getWarningDurationSec()) {
						logger.warning("--- refreshRequestWarnings " + agentName + " after incrementWarningCounter :"
							+ ", req property duration = " + reqProperty.getWarningDurationSec()
							+ ", req prop = " + reqProperty
							+ ", need     = " + globalNeed
								);
					}
					logger.warning("--- refreshRequestWarnings " + agentName + " after incrementWarningCounter : missing = " + UtilDates.df3.format(missing)
							+ ", nodeTotalAvailable = " + UtilDates.df3.format(nodeTotalAvailable)
							+ ", need warning duration = " + globalNeed.getWarningDurationSec()
							+ ", warningDuration (from node) =  " + refreshedWarningDuration
							+ ", gap since last total [MS] = " + gapMS
							);
				}
				// End for debug
			}
		} else {
			globalNeed.resetWarningCounter(current);
			if(reqProperty != null) {
				reqProperty.resetWarningCounter(current);
			}
		}
	}


	public void logAgent() {
		if(this.isActive()) {
			consumersProcessingManager.logContent1(this);
		}
	}

	// specific :  should be common
	public void checkup() {
		consumersProcessingManager.checkup(this);
	}

	@Override
	public void setEventId(long eventId) {
		if (isProducer() && globalProduction != null) {
			globalProduction.setEventId(eventId);
		}
		if (isConsumer() && globalNeed != null) {
			globalNeed.setEventId(eventId);
		}
	}

	@Override
	public void setBeginDate(Date aDate) {
		if (isProducer() && globalProduction != null) {
			globalProduction.setBeginDate(aDate);
		}
		if (isConsumer() && globalNeed != null) {
			globalNeed.setBeginDate(aDate);
		}
	}

	@Override
	public void setEndDate(Date aDate) {
		if (isProducer() && globalProduction != null) {
			globalProduction.setEndDate(aDate);
		}
		if (isConsumer() && globalNeed != null) {
			globalNeed.setEndDate(aDate);
		}
	}

	@Override
	public void setDisabled(boolean bDisabled) {
		if (isProducer() && globalProduction != null) {
			globalProduction.setDisabled(bDisabled);
		}
		if (isConsumer() && globalNeed != null) {
			globalNeed.setDisabled(bDisabled);
		}
	}

	@Override
	public EnergyEvent generateEvent(EventType eventType, String log) {
		if (isProducer() && globalProduction != null) {
			return globalProduction.generateEvent(eventType, log);
		}
		if (isConsumer() && globalNeed != null) {
			return globalNeed.generateEvent(eventType, log);
		}
		return null;
	}

}
