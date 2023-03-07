package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.manager.ContractProcessingManager;
import com.sapereapi.agent.energy.manager.OffersProcessingManager;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.AgentState;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.RescheduleItem;
import com.sapereapi.model.energy.RescheduleTable;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IEnergyAgentPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.protection.ProtectedConfirmationTable;
import com.sapereapi.model.protection.ProtectedSingleOffer;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;

public class ConsumerAgent extends EnergyAgent implements IEnergyAgent {
	private static final long serialVersionUID = 1L;
	private EnergyRequest need;
	private OffersProcessingManager offersProcessingManager = null;
	private ContractProcessingManager contractProcessingManager = null;
	private IConsumerPolicy consumerPolicy = null;
	private int useOffersDecay = 0;
	private int useOfferInitialDecay = 0;
	public final static int CONFIRMATION_INIT_DECAY = 3;

	public ConsumerAgent(int _id, AgentAuthentication authentication, EnergyRequest _need, IEnergyAgentPolicy _consumerPolicy) throws Exception {
		super(_id, authentication.getAgentName()
			, authentication
			, new String[] {"PROD", "OFFER", "PROD_CONFIRM", "DISABLED", "WARNING", "RESCHEDULE"}
			, new String[] { "REQ", "EVENT", "SATISFIED", "CONTRACT1", "CONTRACT2", "DISABLED"}
			, _need);
		this.initFields(_need, _consumerPolicy);
	}

	@Override
	public void initFields(EnergySupply _globalSupply, IEnergyAgentPolicy _consumerPolicy) {
		this.initEnergySupply(_globalSupply);
		int decay = 1 + getTimeLeftSec(true);
		this.addDecay(decay);
		if(_consumerPolicy instanceof IConsumerPolicy) {
			this.consumerPolicy = (IConsumerPolicy) _consumerPolicy;
		} else {
			this.consumerPolicy = null;
		}
		offersProcessingManager = new OffersProcessingManager(this, debugLevel);
		//this.tableSingleOffers = new HashMap<String, SingleOffer>();
		//this.globalOffer = new GlobalOffer(this);
		debugLevel = 0;
		//receivedConfirmations = new HashMap<String, ConfirmationItem>();
		contractProcessingManager = new ContractProcessingManager(this);
		completeOutputPropertyIfNeeded();
		if(Sapere.activateAggregation) {
			//addStandardAggregation(MapStandardOperators.OP_TEST1, "TEST_CONSUMPTION");
			//addCustomizedAggregation(null, "CONTRACT1", false);
		}
	}

	@Override
	public void reinitialize(int _id, AgentAuthentication _authentication, EnergySupply _globalSupply, IEnergyAgentPolicy _consumerPolicy) {
		super.reinitialize(_id, _authentication, _globalSupply);
		initFields(_globalSupply, _consumerPolicy);
	}

	@Override
	public boolean isProducer() {
		return false;
	}

	@Override
	public boolean isConsumer() {
		return true;
	}

	@Override
	public EnergySupply getEnergySupply() {
		return this.need;
	}

	@Override
	public EnergyRequest getEnergyRequest() {
		return this.need;
	}

	@Override
	public void initEnergySupply(EnergySupply supply) {
		if(supply instanceof EnergyRequest) {
			this.need = (EnergyRequest) supply;
		} else {
			// Create energy request with the default values
			this.need = supply.generateRequest();
		}
	}

	public void reward(Lsa lsaResult, int reward) {
		logger.info("rewarded-->" + lsaResult.getAgentName() + " by " + reward);
		logger.info("lsaResult: " + lsaResult.toVisualString());

		if (lsaResult.getAgentName().contains("*")) {
			lsaResult.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Reward);
			sendTo(lsaResult, lsaResult.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString());
		} else
			this.rewardLsa(lsaResult, agentName, reward, 0.0);
	}

	// specific
	public boolean isOK(CompositeOffer globalOffer, boolean isComplementary) {
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


	@Override
	public void onBondNotification(BondEvent event) {
		try {
			Lsa bondedLsa = event.getBondedLsa();
			if(debugLevel>10) {
				if(bondedLsa.getAgentName().startsWith("Prod")) {
					logger.info(" Bond event : ** ConsumerAgent ** " + agentName + " - " + bondedLsa.getAgentName());
					logger.info("Current contract = " + contractProcessingManager.getCurrentContract(false));
				}
			}
			if("__Consumer_N1_2".equals(agentName)) {
				logger.info("onBondNotification " + this.agentName + " For debug");
			}
			int action = getActionToTake(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString()); // add greedy
			if(hasWantedProperty(bondedLsa) || false) {
					String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
					Lsa chosenLSA = bondedLsa;
					String state = chosenLSA.getSyntheticProperty(SyntheticPropertyName.STATE).toString();
					this.tableChosenLsa.put(bondedLsa.getAgentName(), chosenLSA);
					if (action == 0) {
						addState(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString(), action, 0, 0);
						lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(),
								null, query, chosenLSA.getAgentName(), state,
								chosenLSA.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));
					} else if (action == 1) {
						try {
							state = SapereUtil.addOutputsToLsaState(bondedLsa, new String[] {});
							query = this.agentName;
							AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
							if (AgentType.PRODUCER.equals(bondAgentType)) {
								//bondedLsa.getPropertiesByQuery(query);
								// Get offer of production agent
								String producer = SapereUtil.removeStar(chosenLSA.getAgentName());
								Property pOffer = bondedLsa.getOnePropertyByQueryAndName(query, "OFFER");
								Property pDisabled = chosenLSA.getOnePropertyByName("DISABLED");
								Property pConfirm = chosenLSA.getOnePropertyByName("PROD_CONFIRM");
								if (pDisabled!= null && pDisabled.getValue() instanceof RegulationWarning ) {
									RegulationWarning warning = (RegulationWarning) pDisabled.getValue();
									// handle producer disabled property
									// Stop Contract if a producer is disabled
									if(contractProcessingManager.hasProducer(producer)) {
										contractProcessingManager.stopCurrentContracts(this, warning, this.agentName + " received a warning : " + warning);
									}
								}
								if(pOffer!=null && pOffer.getValue() instanceof ProtectedSingleOffer) {
									// handle producer offer
									ProtectedSingleOffer protectedOffer = (ProtectedSingleOffer)  pOffer.getValue();
									handleProducerOffer(protectedOffer);
								}
								if (pConfirm != null && pConfirm.getValue() instanceof ProtectedConfirmationTable) {
									ProtectedConfirmationTable producerConfirmTable = (ProtectedConfirmationTable) pConfirm.getValue();
									// handle producer confirmation
									contractProcessingManager.handleProducerConfirmation(this,  producer,  producerConfirmTable) ;
								}
								if(this.consumerPolicy != null) {
									Property pProd = chosenLSA.getOnePropertyByName("PROD");
									if(pProd!=null && pProd.getValue() instanceof EnergySupply) {
										EnergySupply supply = (EnergySupply)pProd.getValue();
										consumerPolicy.setProducerPricingTable(supply);
									}
								}
							} else if (AgentType.REGULATOR.equals(bondAgentType)) {
								// TODO : use lsa.getOnePropertyByQueryAndName
								Property pReschedule = chosenLSA.getOnePropertyByQueryAndName(agentName, "RESCHEDULE");
								if(pReschedule!=null && pReschedule.getValue() instanceof RescheduleTable) {
									// handle reschedule
									RescheduleTable rescheduleTable = (RescheduleTable) pReschedule.getValue();
									this.handleReschedule(rescheduleTable);
								}
								Property pWarning = chosenLSA.getOnePropertyByName("WARNING");
								if(pWarning!=null && pWarning.getValue() instanceof RegulationWarning) {
									RegulationWarning warning = (RegulationWarning) pWarning.getValue();
									// handle regulation warning
									handleWarning(warning);
								}
							}
						} catch (Throwable e) {
							logger.error(e);
						}
						this.removeBondedLsasOfQuery(query);
					}
			}
		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception thrown in bond evt handler " + agentName + " " + event + " " + e.getMessage());
		}
	}

	@Override
	public void handleWarningOverConsumption(RegulationWarning warning) {
		if(warning.hasAgent(agentName)) {
			if(!this.isDisabled()) {
				// Disable consumer
				disableAgent(warning);
			}
		}
	}

	@Override
	public void handleWarningOverProduction(RegulationWarning warning) {
	}

	@Override
	public void handleWarningUserInteruption(RegulationWarning warning) {
		if(warning.hasAgent(agentName)) {
			if(!this.isDisabled()) {
				// Disable consumer
				disableAgent(warning);
			}
		}
	}

	@Override
	public void handleWarningChangeRequest(RegulationWarning warning) {
		if(warning.hasAgent(agentName)) {
			if(warning.getChangeRequest()!=null) {
				updateRequest(warning.getChangeRequest());
			}
		}
	}

	@Override
	public void disableAgent(RegulationWarning warning) {
		need.setDisabled(true);
		contractProcessingManager.stopCurrentContracts(this,warning, agentName + " received a warning : " + warning);
		super.disableAgent(warning, new String[] {"REQ", "SATISFIED", "CONTRACT1", "CONTRACT2"});
	}

	@Override
	public Set<String> getLinkedAgents() {
		Set<String> result = new HashSet<>();
		if (isSatisfied()) {
			for (String agent : contractProcessingManager.getProducerAgents()) {
				result.add(agent);
			}
		}
		return result;
	}

	@Override
	public boolean isSatisfied() {
		return contractProcessingManager.isConsumerSatisfied(this);
	}

	// Specific
	public boolean isInWarning() {
		if(need!=null) {
			return need.getWarningDurationSec()>0;
		}
		return false;
	}

	@Override
	public PowerSlot getForcastOngoingContractsPowerSlot(String location, Date aDate) {
		return contractProcessingManager.getForcastOngoingContractsPower(location, aDate);
	}

	public double getContractsPower() {
		return contractProcessingManager.getContractsPower();
	}

	/**
	 * Power provided by producer
	 * 
	 * @return
	 */
	@Override
	public PowerSlot getOngoingContractsPowerSlot(String locationFilter) {
		return contractProcessingManager.getOngoingContractsPower(locationFilter);
	}

	@Override
	public Map<String, PowerSlot> getOngoingContractsRepartition() {
		return contractProcessingManager.getOngoinContractsRepartition();
	}

	@Override
	public Map<String, Double> getOffersRepartition() {
		return offersProcessingManager.getOffersRepartition();
	}

	@Override
	public Double getOffersTotal() {
		return offersProcessingManager.getOffersTotal();
	}

	@Override
	public List<String> getConsumersOfWaitingContrats() {
		return new ArrayList<String>();
	}

	@Override
	public PowerSlot getWaitingContratsPowerSlot() {
		return new PowerSlot();
	}

	// specific
	public EnergyRequest getNeed() {
		return need;
	}

	@Override
	public Double computeAvailablePower() {
		return 0.0;
	}

	@Override
	public Double computeMissingPower() {
		if(hasExpired() || isDisabled() || isStartInFutur()) {
			return 0.0;
		}
		PowerSlot contractsPowerSlot = getOngoingContractsPowerSlot(null);
		return Math.max(0, need.getPower() - contractsPowerSlot.getCurrent());
	}

	@Override
	public EventType getStartEventType() {
		return EventType.REQUEST_START;
	}

	@Override
	public EventType getUpdateEventType() {
		return EventType.REQUEST_UPDATE;
	}

	@Override
	public EventType getExpiryEventType() {
		return EventType.REQUEST_EXPIRY;
	}

	@Override
	public EventType getStopEventType() {
		return EventType.REQUEST_STOP;
	}

	@Override
	public void onDecayedNotification(DecayedEvent event) { // change to return only one result
		try {
			if(consumerPolicy != null && !this.hasExpired() && (this.isStartInFutur() || getContractsPower() == 0)) {
				EnergyRequest newRequest = consumerPolicy.updateRequest(this.need);
				if(this.need.hasChanged(newRequest)) {
					logger.info("before updateRequest " + newRequest + ", startEvent = " + startEvent);
					if(startEvent != null) {
						logger.info("before updateRequest : for debug ");
					}
					updateRequest(newRequest);
				}
			}
			// check agent expiration
			checkAgentExpiration(event);

			lastNodeTotal = EnergyDbHelper.retrieveLastNodeTotal();
			refreshRequestWarnings();
			tryReactivation();

			// Clean contract
			contractProcessingManager.cleanContracts(this);
			// check if the contract needs to be updated due to a change in request  
			contractProcessingManager.handleRequestChanges(this, "onDecayedNotification");

			// Check producers confirmations
			contractProcessingManager.checkProducersConfirmations(this);

			//contractProcessingManager.refreshLsaProperties(this);
			// Remove obsolete data ( offers, warnings )
			cleanExpiredData();
			// Generate global offer
			generateNewContract();
			// merge main and complementary contracts if both are ongoing
			contractProcessingManager.mergeContracts(this);

			// Refresh properties posted in LSA
			contractProcessingManager.refreshLsaProperties(this);

			// For propagation
			addGradient(3);
		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception throw in decay handler of agent " + this.agentName + " " + event + " " + e.getMessage());
		}
	}


	@Override
	public void cleanExpiredData() {
		offersProcessingManager.cleanExpiredOffers(this);

		// Clean expired warnings
		cleanExpiredWarnings();

		// Delete contract if invalid
		contractProcessingManager.resetCurrentContractIfInvalid();

	}

	// specific
	private void handleProducerOffer(ProtectedSingleOffer protectedOffer) {
		try {
			SingleOffer receivedOffer =  protectedOffer.getSingleOffer(this);
			SingleOffer newOffer = receivedOffer.clone();
			newOffer.setIssuerDistance(Sapere.getInstance().getDistance(newOffer.getIssuerLocation(), newOffer.getIssuerDistance()));
			boolean isComplementary = newOffer.isComplementary();
			boolean offerUsed = false;
			if (contractProcessingManager.needOffer(isComplementary)) {
				//if(!SapereUtil.hasProperty(lsa, agentName, "CONTRACT1")) {
				// Check the offer validity
				if (need.isOK(newOffer)) {
					boolean hasNoOffer = !offersProcessingManager.hasSingleOffer();
					offersProcessingManager.addSingleOffer(newOffer);
					offerUsed = true;
					logger.info(" consumer " + this.agentName + " set offer used : id:" + newOffer.getId() + "  "  + newOffer.getProducerAgent() + " W = " + SapereUtil.round(newOffer.getPower(),3));
					if(hasNoOffer) {
						this.useOffersDecay = useOfferInitialDecay;
					}
				}
				//}
			}
			EnergyDbHelper.setSingleOfferAcquitted(newOffer, startEvent, offerUsed);
			newOffer.setAcquitted(true);
			if(!offerUsed /* && !isSatisfied()*/) {
				logger.info(this.agentName + " For debug : satisfied = " + isSatisfied());
			}
		} catch (PermissionException e) {
			logger.error(e);
		}
	}

	// specific
	@Override
	public void handleReschedule(RescheduleTable rescheduleTable) {
		super.handleReschedule(rescheduleTable);
		for(String producer : contractProcessingManager.getProducerAgents()) {
			if(rescheduleTable.hasItem(producer)) {
				RescheduleItem item = rescheduleTable.getItem(producer) ;
			}
		}
	}

	private void auxUpdateRequest(EnergySupply changeRequest) {
		try {
			changeRequest.checkPowers();
			changeRequest.checkDates();
			EnergySupply changeRequestCopy = changeRequest.clone();
			this.need.setBeginDate(changeRequestCopy.getBeginDate());
			this.need.checkBeginNotPassed();
			logger.info("auxUpdateRequest : after checkBeginNotPassed");
			changeRequest.checkDates();
			this.need.setEndDate(changeRequestCopy.getEndDate());
			this.need.setPower(changeRequestCopy.getPower());
			this.need.setPowerMin(changeRequestCopy.getPowerMin());
			this.need.setPowerMax(changeRequestCopy.getPowerMax());
			this.need.checkPowers();
			this.addDecay(1 + 30 * need.getTimeLeftSec(true));
			if(changeRequestCopy instanceof EnergyRequest) {
				EnergyRequest request = ((EnergyRequest) changeRequestCopy).clone();
				this.need.setDelayToleranceMinutes(request.getDelayToleranceMinutes());
				this.need.setPriorityLevel(request.getPriorityLevel());
			}
		}
		catch (Exception e) {
			logger.error(e);
		}
	}

	// specific
	private void updateRequest(EnergySupply changeRequest) {
		if(this.hasExpired() || this.isDisabled()) {
			logger.warning("updateRequest " + this.agentName + " cannot be updated : agent expired or disabled");
			return;
		} else if(this.startEvent == null)  {
			if(this.waitingStartEvent != null) {
				// Update request
				auxUpdateRequest(changeRequest);
			} else {
				logger.warning("updateRequest " + this.agentName + " cannot be updated : startEvent is null");
			}
			return;
		} else {
			// Update request
			auxUpdateRequest(changeRequest);
			// Generate update event
			try {
				if(this.need.isStartInFutur()) {
					// Generate stop event and set waitingStartEvent
					RegulationWarning warning = new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), getTimeShiftMS());
					stopEvent = generateStopEvent(warning, "ChangeRequest with start in futur");
					waitingStartEvent = new EnergyEvent( getStartEventType(), getEnergySupply(), "");
					logger.info("updateRequest " + this.agentName + " after generateStopEvent : stopEvent = " + startEvent);
				} else {
					// udpdate start event
					this.startEvent = generateUpdateEvent(WarningType.CHANGE_REQUEST);
				}
			} catch (Exception e) {
				logger.error(e);
			}
			logger.info("updateRequest " + this.agentName + " before checkupRequestChanges" );
			contractProcessingManager.handleRequestChanges(this, "updateRequest");
		}
	}


	// specific
	/*
	public EnergyRequest generateRequest() {
		return contractProcessingManager.generateRequest(this);
	}*/

	private void generateNewContract() {
		int warningDuration = need.getWarningDurationSec();
		boolean isInHighWarning = warningDuration>=8;
		boolean isComplementary = (contractProcessingManager.getOngoingContractsPower() > 0);
		boolean hasAlreadyNewContract =  contractProcessingManager.isContractWaitingValidation(isComplementary);
		String msgTag = "handleReceivedOffers " + this.agentName + " [warningDuration=" + warningDuration + "] ";
		String contractTag = isComplementary?"CONTRACT2" :"CONTRACT1";
		if(isInHighWarning) {
			// For debug
			logger.warning(msgTag + " begin : requested = " + UtilDates.df.format(need.getPower()) + " hasConfirmation = " + hasAlreadyNewContract + " has offers = " + offersProcessingManager.hasSingleOffer());
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
				if(isInHighWarning) {
					logger.warning(msgTag + " after generateGlobalOffer globalOffer = " + globalOffer);
				}
				// Check if the global offer can met the demand
				if(this.isOK(globalOffer, isComplementary)) {
					if(isInHighWarning) {
						logger.warning(msgTag + " globalOffer is OK");
					}
					if(!lsa.getPropertiesByName(contractTag).isEmpty()) {
						logger.info(msgTag +  " contract is set");
					}
					// The Offer is OK : Confirm global offer to generate a contract
					AgentState state = new AgentState(Arrays.asList(getInput()), new ArrayList<String>());
					state.addOutput(contractTag);
					lsa.addSyntheticProperty(SyntheticPropertyName.STATE, state.toString());
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

	// specific
	private void refreshRequestWarnings() {
		double nodeTotalAvailable = (lastNodeTotal==null)? 0 : lastNodeTotal.getAvailable();
		Date current = UtilDates.getNewDate(timeShiftMS);
		double missing = this.contractProcessingManager.getMissing(this);
		if(this.isSatisfied()) {
			need.resetWarningCounter(current);
		} else if(missing > 0 && missing <= nodeTotalAvailable) {
			need.incrementWarningCounter(current);
		} else {
			need.resetWarningCounter(current);
		}
	}

	@Override
	public IProducerPolicy getProducerPolicy() {
		return null;
	}

}
