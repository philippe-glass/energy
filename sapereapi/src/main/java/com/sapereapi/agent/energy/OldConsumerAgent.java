package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.manager.ContractProcessingManager;
import com.sapereapi.agent.energy.manager.OffersProcessingManager;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.ChangeRequest;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.award.AwardsTable;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IEnergyAgentPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.reschedule.RescheduleItem;
import com.sapereapi.model.energy.reschedule.RescheduleTable;
import com.sapereapi.model.protection.ProtectedConfirmationTable;
import com.sapereapi.model.protection.ProtectedSingleOffer;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;

public class OldConsumerAgent extends EnergyAgent implements IEnergyAgent {
	private static final long serialVersionUID = 1L;
	private EnergyRequest need;
	private OffersProcessingManager offersProcessingManager = null;
	private ContractProcessingManager contractProcessingManager = null;
	private IConsumerPolicy consumerPolicy = null;
	private int useOffersDecay = 0;
	private int useOfferInitialDecay = 0;
	public final static int CONFIRMATION_INIT_DECAY = 3;

	public OldConsumerAgent(int _id, AgentAuthentication authentication, EnergyRequest _need, IEnergyAgentPolicy _consumerPolicy, NodeContext nodeContext) throws HandlingException {
		super(_id, authentication.getAgentName(), authentication
			, getInputTags(), getOutputTags(), nodeContext);
		this.initFields(null, _need, null, _consumerPolicy, nodeContext);
	}

	private static String[] getInputTags() {
		return new String[] { "PROD", "OFFER", "PROD_CONFIRM", "DISABLED", "WARNING", "RESCHEDULE" };
	}

	private static String[] getOutputTags() {
		return new String[] { "REQ", "EVENT", "SATISFIED", "CONTRACT1", "CONTRACT2", "DISABLED" };
	}

	@Override
	public void initFields(EnergySupply _globalSupply, EnergyRequest _need, IEnergyAgentPolicy _producerPolicy, IEnergyAgentPolicy _consumerPolicy, NodeContext nodeContext) {
		this.need = _need;
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
		//completeOutputPropertyIfNeeded();
		if(nodeContext.getNodePredicitonSetting().isModelAggregationActivated()
		|| nodeContext.getClusterPredictionSetting().isModelAggregationActivated()) {
			//addStandardAggregation(MapStandardOperators.OP_TEST1, "TEST_CONSUMPTION");
			//addCustomizedAggregation(null, "CONTRACT1", false);
		}
	}

	public IConsumerPolicy getConsumerPolicy() {
		return consumerPolicy;
	}

	@Override
	public void reinitialize(int _id, AgentAuthentication _authentication, EnergySupply _globalSupply, EnergyRequest _need
			, IEnergyAgentPolicy _consumerPolicy, IEnergyAgentPolicy _producerPolicy, NodeContext nodeContext) {
		super.auxReinitializeLSA(_id, _authentication, getInputTags(), getOutputTags(), nodeContext);
		initFields(null, _need, null, _consumerPolicy, nodeContext);
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
	public EnergySupply getGlobalProduction() {
		return this.need.generateSupply();
	}

	@Override
	public EnergyRequest getGlobalNeed() {
		return this.need;
	}

	@Override
	public void initGlobalProduction(EnergySupply supply) {
		/*
		if(supply instanceof EnergyRequest) {
			this.need = (EnergyRequest) supply;
		} else {
			// Create energy request with the default values
			this.need = supply.generateRequest();
		}*/
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
			if(hasTargetedProperty(bondedLsa) || false) {
				Lsa chosenLSA = bondedLsa;
				try {
					bondedLsa = completeInvolvedLocations(bondedLsa, logger);
					AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
					if (AgentType.PRODUCER.equals(bondAgentType)) {
						// Get offer of production agent
						String producer = chosenLSA.getAgentName();
						Property pOffer = bondedLsa.getOnePropertyByQueryAndName(this.agentName, "OFFER");
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
							handleProducerOffer(protectedOffer, bondedLsa);
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
			}
		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception thrown in bond evt handler " + agentName + " " + event + " " + e.getMessage());
		}
	}

	@Override
	public void handleWarningOverConsumption(RegulationWarning warning) throws HandlingException {
		if(warning.hasAgent(agentName)) {
			if(!this.isDisabled()) {
				// Disable consumer
				disableAgent(warning);
			}
		}
	}

	@Override
	public void handleWarningOverProduction(RegulationWarning warning) throws HandlingException {
	}

	@Override
	public void handleWarningUserInteruption(RegulationWarning warning) throws HandlingException {
		if(warning.hasAgent(agentName)) {
			if(!this.isDisabled()) {
				// Disable consumer
				disableAgent(warning);
			}
		}
	}

	@Override
	public void handleWarningChangeRequest(RegulationWarning warning) throws HandlingException {
		if(warning.hasAgent(agentName)) {
			if(warning.getChangeRequest()!=null) {
				updateRequest(warning.getChangeRequest());
			}
		}
	}

	@Override
	public void disableAgent(RegulationWarning warning) throws HandlingException {
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
	public EventType getSwitchEventType() {
		return EventType.REQUEST_SWITCH;
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
				if(this.getOffersTotal() ==0) {
					EnergyRequest newRequest = consumerPolicy.updateRequest(this.need);
					if(this.need.hasChanged(newRequest)) {
						EnergyEvent startEvent = getStartEvent();
						logger.info("before updateRequest " + newRequest + ", startEvent = " + startEvent);
						if(startEvent != null) {
							logger.info("before updateRequest : for debug ");
						}
						ChangeRequest changeRequest = new ChangeRequest(agentName, newRequest, ProsumerRole.CONSUMER);
						updateRequest(changeRequest);
					}
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

			// For spreading
			activateSpreading();

			// For debug : TO DELETE
			//this.replacePropertyWithName(new Property("DEBUG_NEED", need));
			// End TO DELETE
		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception thrown in ConsumerAgent.onDecayedNotification " + this.agentName + " " + event + " " + e.getMessage());
		}
	}


	@Override
	public void cleanExpiredData() {
		offersProcessingManager.cleanExpiredOffers(this);

		// Clean expired warnings
		cleanExpiredWarnings();

		// Delete contract if invalid
		contractProcessingManager.resetCurrentContractIfInvalid(this);
	}

	// specific
	private void handleProducerOffer(ProtectedSingleOffer protectedOffer, Lsa bondedLsa) {
		try {
			SingleOffer receivedOffer =  protectedOffer.getSingleOffer(this);
			SingleOffer newOffer = receivedOffer.clone();
			ProsumerProperties offerIssuer = newOffer.getIssuerProperties();
			if(!NodeManager.isLocal(offerIssuer.getLocation())) {
				logger.info("handleProducerOffer " + this.agentName + " : receive offer from other node " + offerIssuer.getLocation().getName() + " : " + newOffer);
				if(!newOffer.checkLocation()) {
					// TODO : set locationId
					logger.error("handleProducerOffer offer " + newOffer + " has no locationid");
				}
				if(newOffer.hasExpired()) {
					logger.error("handleProducerOffer this offer has expired " + newOffer.getExpirationDurationSec() + " seconds ago");
				}
			}
			int isserDistance = bondedLsa.getSourceDistance();
			newOffer.getIssuerProperties().setDistance(isserDistance);
			boolean isComplementary = newOffer.isComplementary();
			boolean offerUsed = false;
			if (contractProcessingManager.needOffer(isComplementary)) {
				//if(!SapereUtil.hasProperty(lsa, agentName, "CONTRACT1")) {
				// Check the offer validity
				if(newOffer.hasExpired()) {
					logger.error("handleProducerOffer " + this.agentName + " newOffer hasExpired : "
						+ ", newOffer.deadline = " + UtilDates.format_time.format(newOffer.getDeadline())
						+ ", currentTime = " + UtilDates.format_time.format(getCurrentDate())
						+ ", newOffer.getCreationTime = " + UtilDates.format_time.format(newOffer.getCreationTime())
						);
				} else if (need.isOK(newOffer)) {
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

	@Override
	public void handleAwards(AwardsTable awardsTable) throws HandlingException {
		// do nothing
	}

	// specific
	@Override
	public void handleReschedule(RescheduleTable rescheduleTable) throws HandlingException {
		super.handleReschedule(rescheduleTable);
		for(String producer : contractProcessingManager.getProducerAgents()) {
			if(rescheduleTable.hasItem(producer)) {
				RescheduleItem item = rescheduleTable.getItem(producer) ;
			}
		}
	}

	private void auxUpdateRequest(ChangeRequest changeRequest) {
		try {
			changeRequest.getSupply().checkPowers();
			changeRequest.getSupply().checkDates();
			EnergyRequest newRequest = changeRequest.getRequest().clone();
			this.need.setBeginDate(newRequest.getBeginDate());
			this.need.checkBeginNotPassed();
			logger.info("auxUpdateRequest : after checkBeginNotPassed");
			changeRequest.getSupply().checkDates();
			this.need.setEndDate(newRequest.getEndDate());
			this.need.setPowerSlot(newRequest.getPowerSlot().clone());
			this.need.checkPowers();
			this.addDecay(1 + 30 * need.getTimeLeftSec(true));
			this.need.setDelayToleranceMinutes(newRequest.getDelayToleranceMinutes());
			this.need.setPriorityLevel(newRequest.getPriorityLevel());
		}
		catch (Exception e) {
			logger.error(e);
		}
	}

	// specific
	private void updateRequest(ChangeRequest changeRequest) throws HandlingException {
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
			auxUpdateRequest(changeRequest);
			// Generate update event
			try {
				if(this.need.isStartInFutur()) {
					// Generate stop event and set waitingStartEvent
					RegulationWarning warning = new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), getTimeShiftMS());
					EnergyEvent stopEvent = generateStopEvent(warning, "ChangeRequest with start in futur");
					waitingStartEvent = need.generateEvent(getStartEventType(), "");
					logger.info("updateRequest " + this.agentName + " after generateStopEvent : stopEvent = " + stopEvent);
				} else {
					// udpdate start event
					generateUpdateEvent(WarningType.CHANGE_REQUEST);
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

	private void generateNewContract() throws HandlingException {
		int warningDuration = need.getWarningDurationSec();
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
			logger.warning(msgTag + " begin : requested = " + UtilDates.df3.format(need.getPower()) + " hasConfirmation = " + hasAlreadyNewContract + " has offers = " + offersProcessingManager.hasSingleOffer());
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
					logger.error("after generateGlobalOffer : for debug : location is null");
				}
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

	// specific
	private void refreshRequestWarnings() {
		double nodeTotalAvailable = (lastNodeTotal==null)? 0 : lastNodeTotal.getAvailable();
		Date current = getCurrentDate();
		double missing = this.contractProcessingManager.getMissing(this);
		EnergyRequest reqProperty = this.contractProcessingManager.getRequestProperty(this);
		if(this.isSatisfied()) {
			need.resetWarningCounter(current);
			if(reqProperty != null) {
				reqProperty.resetWarningCounter(current);
			}
		} else if(missing > 0 && missing <= nodeTotalAvailable) {
			need.incrementWarningCounter(current);
			if(reqProperty != null) {
				// Syncrhonize the information of warning duration in need attribute and in the "REQ" property
				if(reqProperty.getWarningDurationSec() != need.getWarningDurationSec()) {
					logger.info("refreshRequestWarnings increment warning on req property : " + reqProperty);
					reqProperty.incrementWarningCounter(current);
				}
				if(reqProperty.getWarningDate()!=null && !reqProperty.getWarningDate().equals(need.getWarningDate())) {
					logger.error("warning dates are different " + SapereUtil.CR + "reqProperty : " + reqProperty + SapereUtil.CR + "need : " + need);
				}
				// For debug
				String sConsumerWarning = (lastNodeTotal==null)? "" : lastNodeTotal.getMaxWarningConsumer();
				long warningDurationLastTotal = (lastNodeTotal==null)? 0 : lastNodeTotal.getMaxWarningDuration();
				long gapMS = (lastNodeTotal==null)? 0 : getCurrentDate().getTime() - lastNodeTotal.getDate().getTime();
				boolean isInHighWarning = (warningDurationLastTotal>=8) && (agentName.equals(sConsumerWarning));
				long refreshedWarningDuration = (int) (gapMS/1000) + warningDurationLastTotal;
				if(isInHighWarning) {
					if(need.getWarningDurationSec() != reqProperty.getWarningDurationSec()) {
						logger.warning("--- refreshRequestWarnings " + agentName + " after incrementWarningCounter :"
							+ ", req property duration = " + reqProperty.getWarningDurationSec()
							+ ", req prop = " + reqProperty
							+ ", need     = " + need
								);
					}
					logger.warning("--- refreshRequestWarnings " + agentName + " after incrementWarningCounter : missing = " + UtilDates.df3.format(missing)
							+ ", nodeTotalAvailable = " + UtilDates.df3.format(nodeTotalAvailable)
							+ ", need warning duration = " + need.getWarningDurationSec()
							+ ", warningDuration (from node) =  " + refreshedWarningDuration
							+ ", gap since last total [MS] = " + gapMS
							);
				}
				// End for debug
			}
		} else {
			need.resetWarningCounter(current);
			if(reqProperty != null) {
				reqProperty.resetWarningCounter(current);
			}
		}
	}

	@Override
	public IProducerPolicy getProducerPolicy() {
		return null;
	}

	@Override
	public void setEventId(long eventId) {
		if (need != null) {
			need.setEventId(eventId);
		}
	}

	@Override
	public void setBeginDate(Date aDate) {
		if (need != null) {
			need.setBeginDate(aDate);
		}
	}

	@Override
	public void setEndDate(Date aDate) {
		if (need != null) {
			need.setEndDate(aDate);
		}
	}

	@Override
	public void setDisabled(boolean bDisabled) {
		if (need != null) {
			need.setDisabled(bDisabled);
		}
	}

	@Override
	public EnergyEvent generateEvent(EventType eventType, String log) {
		if (need != null) {
			return need.generateEvent(eventType, log);
		}
		return null;
	}
}
