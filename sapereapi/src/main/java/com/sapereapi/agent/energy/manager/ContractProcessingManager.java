package com.sapereapi.agent.energy.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.exception.UnauthorizedModificationException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.protection.ProtectedConfirmationTable;
import com.sapereapi.model.protection.ProtectedContract;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.lsa.Property;

public class ContractProcessingManager {
	private ContractProcessingData mainProcessingData = null;
	private ContractProcessingData secondProcessingData = null;
	private int MAX_STOP_DECAY = 3;
	private final static int CONTRACT_VALIDATION_DEALY_SEC = 10;
	private Date lastPostTime = null;

	int stopDecay = 0;
	int cContractDecay = 0;
	int mergeContractsDecay = 0;
	private long timeShiftMS = 0;
	private static SapereLogger logger = SapereLogger.getInstance();

	public ContractProcessingManager(EnergyAgent consumerAgent) {
		super();
		timeShiftMS = consumerAgent.getTimeShiftMS();
		mainProcessingData = new ContractProcessingData(false, timeShiftMS);
		secondProcessingData  = new ContractProcessingData(true, timeShiftMS);
	}

	public boolean isContractOnGoing(boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.isContractOnGoing();
		} else {
			return mainProcessingData.isContractOnGoing();
		}
	}

	public boolean hasContractWaitingValidation() {
		return secondProcessingData.isContractWaitingValidation()
				|| mainProcessingData.isContractWaitingValidation();
	}

	public boolean isContractWaitingValidation(boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.isContractWaitingValidation();
		} else {
			return mainProcessingData.isContractWaitingValidation();
		}
	}

	public boolean isConsumerSatisfied(EnergyAgent consumerAgent) {
		if(consumerAgent.hasExpired()) {
			return false;
		}
		if(consumerAgent.isStartInFutur()) {
			return true;
		}
		double providedPower = getOngoingContractsPower();
		double neededPower = consumerAgent.getEnergyRequest().getPower();
		return providedPower >= neededPower - 0.0001;
	}

	public boolean hasContractWaitingValidation(EnergyAgent consumerAgent) {
		if(consumerAgent.hasExpired()) {
			return false;
		}
		return hasContractWaitingValidation();
		//return !lsa.getPropertiesByName("CONTRACT1").isEmpty();
	}

	public PowerSlot getForcastOngoingContractsPower(String locationFilter, Date aDate) {
		return SapereUtil.add(
			mainProcessingData.getForcastOngoingContractsPower(locationFilter, aDate),
			secondProcessingData.getForcastOngoingContractsPower(locationFilter, aDate));
	}

	/**
	 * Power provided by producer
	 * 
	 * @return
	 */
	public PowerSlot getOngoingContractsPower(String locationFilter) {
		return SapereUtil.add(
			mainProcessingData.getOngoingContractsPower(locationFilter),
			secondProcessingData.getOngoingContractsPower(locationFilter));
	}

	public Double getContractsPower() {
		return mainProcessingData.getContractedPower()
				+ secondProcessingData.getContractedPower();
	}

	public Double getOngoingContractsPower() {
		return mainProcessingData.getOngoingContractedPower()
				+ secondProcessingData.getOngoingContractedPower();
	}

	public Map<String, PowerSlot> getOngoinContractsRepartition() {
		return SapereUtil.mergeMapStrPowerSlot(
				mainProcessingData.getOngoingContractsRepartition(),
				secondProcessingData.getOngoingContractsRepartition());
	}

	public void postExpiryEvent(EnergyAgent consumerAgent, boolean isComplementary) {
		if(isComplementary) {
			secondProcessingData.postExpiryEvent(consumerAgent);
		} else {
			mainProcessingData.postExpiryEvent(consumerAgent);
		}
	}

	public void cleanContracts(EnergyAgent consumerAgent) {
		cleanContract(consumerAgent, false);
		cleanContract(consumerAgent, true);
	}

	public void cleanContract(EnergyAgent consumerAgent, boolean isComplementary) {
		consumerAgent.cleanEventProperties();
		Contract currentContract = getCurrentContract(isComplementary);
		String contractPropertyTag = isComplementary? "CONTRACT2" : "CONTRACT1";
		if (currentContract == null) {
			// Do nothing
		} else {
			if (currentContract.hasAllAgreements() && !currentContract.hasExpired()) {
			} else if (currentContract.hasExpired()) {
				consumerAgent.getLsa().removePropertiesByName(contractPropertyTag);
				// Post expiration event
				postExpiryEvent(consumerAgent, isComplementary);
				currentContract = null;
				consumerAgent.getLsa().removePropertiesByName(contractPropertyTag);
			} else if (currentContract.hasDisagreement()) {
				currentContract = null;
				consumerAgent.getLsa().removePropertiesByName(contractPropertyTag);
			} else if (currentContract.validationHasExpired()) {
				// stop the contract if the validation has expired
				this.stopCurrentContract(consumerAgent, isComplementary, null, consumerAgent.getAgentName() + " : contract validation has expired");
			}
			if(currentContract == null && !isComplementary) {
			}
		}
		// Clean CONTRACT property
		/*
		if (!consumerAgent.getLsa().getPropertiesByName(contractPropertyTag).isEmpty()) {
			if (cContractDecay <= 0 && true) {
				consumerAgent.getLsa().removePropertiesByName(contractPropertyTag);
			} else {
				cContractDecay--;
			}
		}*/
	}

	public Set<String> getProducersWithNoRecentConfirmations(EnergyAgent consumerAgent) {
		return SapereUtil.mergeSetStr(
			mainProcessingData.getProducersWithNoRecentConfirmations(consumerAgent),
			secondProcessingData.getProducersWithNoRecentConfirmations(consumerAgent));
	}

	// Check producers confirmations
	public void checkProducersConfirmations(EnergyAgent consumerAgent) {
		Set<String> producersWithNoRecentConfirmations = getProducersWithNoRecentConfirmations(consumerAgent);
		for(String producer : producersWithNoRecentConfirmations) {
			checkProducerInSpace(consumerAgent, producer);
		}
	}

	private void checkProducerInSpace(EnergyAgent consumerAgent, String producerName) {
		if (!Sapere.getInstance().isInSpace(producerName)) {
			// The contract should be broken
			// Lsa chosenLSA = tableChosenLsa.get(producerName);
			RegulationWarning warning = new RegulationWarning(WarningType.NOT_IN_SPACE, getCurrentDate(), this.timeShiftMS);
			if( mainProcessingData.isContractOnGoing() 		|| mainProcessingData.isContractWaitingValidation()
			||  secondProcessingData.isContractOnGoing() 	|| secondProcessingData.isContractWaitingValidation()) {
				stopCurrentContracts(consumerAgent, warning, producerName + " : is not in space");
			}
		}
	}

	// specific
	public Contract getContractProperty(EnergyAgent consumerAgent, boolean isComplementary) {
		Property pContract = consumerAgent.getLsa().getOnePropertyByName(isComplementary? "CONTRACT2" : "CONTRACT1");
		if(pContract!=null && pContract.getValue() instanceof ProtectedContract) {
			ProtectedContract protectedContract = (ProtectedContract) pContract.getValue();
			try {
				Contract contract = protectedContract.getContract(consumerAgent);
				return contract;
			} catch (PermissionException e) {
				logger.error(e);
			}
		}
		return null;
	}

	public boolean refreshContractsProperties(EnergyAgent consumerAgent) {
		boolean hasChanged1 = refreshContractProperties(consumerAgent, false);
		boolean hasChanged2 = refreshContractProperties(consumerAgent, true);
		return hasChanged1 || hasChanged2;
	}

	public boolean refreshContractProperties(EnergyAgent consumerAgent, boolean isComplementary) {
		String contractPropertyTag = isComplementary? "CONTRACT2" : "CONTRACT1";
		Contract currentContract = getCurrentContract(isComplementary);
		Contract oldContent = getContractProperty(consumerAgent, isComplementary);
		if(currentContract==null) {
			boolean hasChanged = false;
			if(oldContent != null) {
				consumerAgent.getLsa().removePropertiesByName(contractPropertyTag);
				hasChanged = true;
			}
			return hasChanged;
		}
		boolean hasChanged = (oldContent == null) || oldContent.hasChanged(currentContract);
		boolean toPost = (lastPostTime == null);
		if (lastPostTime != null) {
			Date postDeadline = UtilDates.shiftDateSec(lastPostTime, 10);
			Date current = getCurrentDate();
			toPost = current.after(postDeadline);
		}
		try {
			if (currentContract.isWaitingValidation()) {
				logger.info("setContractProperties : currentContract is waiting for validation ");
			}
			if (hasChanged || toPost) {
				ProtectedContract protectedContract = new ProtectedContract(currentContract.clone());
				consumerAgent.getLsa().removePropertiesByNames(new String[] { contractPropertyTag });
				if (currentContract.hasDisagreement()) {
					stopDecay = MAX_STOP_DECAY;
				}
				// Add property to indicate that the contract is canceled
				// Lsa chosenLSA = tableChosenLsa.get(consumer);
				consumerAgent.getLsa().removePropertiesByName(contractPropertyTag);
				consumerAgent.getLsa().addProperty(new Property(contractPropertyTag, protectedContract));
				/*
				if(!isComplementary &&  false) {
					// For test !!!
					consumerAgent.getLsa().replacePropertyWithName(new Property("TEST_CONSUMPTION", currentContract.getPower()));
				}*/
				cContractDecay = MAX_STOP_DECAY;
				lastPostTime = getCurrentDate();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return hasChanged;
	}

	public void stopCurrentContracts(EnergyAgent consumerAgent, RegulationWarning warning, String logCancel) {
		stopCurrentContract(consumerAgent, false, warning, logCancel);
		stopCurrentContract(consumerAgent, true, warning, logCancel);
	}

	public void stopCurrentContract(EnergyAgent consumerAgent, boolean isComplementary, RegulationWarning warning, String logCancel) {
		if(isComplementary) {
			secondProcessingData.stopCurrentContract(consumerAgent, warning, logCancel);
		} else {
			mainProcessingData.stopCurrentContract(consumerAgent, warning, logCancel);
		}
		// set REQ property
		consumerAgent.getLsa().removePropertiesByName("SATISFIED");
		consumerAgent.getLsa().removePropertiesByName(isComplementary ? "CONTRACT2" : "CONTRACT1");
		if(!consumerAgent.isDisabled()) {
			consumerAgent.getLsa().addProperty(new Property("REQ", generateMissingRequest(consumerAgent)));
		}
		if(!isComplementary) {
			if(secondProcessingData.isContractOnGoing() || secondProcessingData.isContractWaitingValidation()) {
				// Stop the complementary contract
				stopCurrentContract(consumerAgent, true, null, consumerAgent.getAgentName() + " : the main contract has been stopped");
			}
		}
	}

	public List<Contract> getContracts() {
		List<Contract> result = new ArrayList<>();
		Contract mainContract = mainProcessingData.getCurrentContract();
		if(mainContract!=null) {
			result.add(mainContract);
		}
		Contract complementaryContract = secondProcessingData.getCurrentContract();
		if(complementaryContract != null) {
			result.add(complementaryContract);
			if(mainContract == null) {
				logger.error("getContracts complementary contract " + complementaryContract + " should be null" );
			}
		}
		return result;
	}

	public void setCurrentContract(Contract currentContract) {
		if(currentContract.isComplementary()) {
			secondProcessingData.setCurrentContract(currentContract);
			// The main contract is not merged anymore
			if(mainProcessingData.getCurrentContract() !=null && mainProcessingData.getCurrentContract().isMerged()) {
				mainProcessingData.setCurrentContractMerged(false);
			}
		} else {
			this.mainProcessingData.setCurrentContract(currentContract);
		}
	}

	public Contract getCurrentContract(boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.getCurrentContract();
		} else {
			return mainProcessingData.getCurrentContract();
		}
	}

	public boolean needOffer(boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.needOffer();
		} else {
			return mainProcessingData.needOffer();
		}
	}
	public EnergyEvent generateStartEvent(EnergyAgent consumerAgent, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.generateStartEvent(consumerAgent);
		} else {
			return mainProcessingData.generateStartEvent(consumerAgent);
		}
	}

	public EnergyEvent generateUpdateEvent(EnergyAgent consumerAgent, WarningType warningType, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.generateUpdateEvent(consumerAgent, warningType);
		} else {
			return mainProcessingData.generateUpdateEvent(consumerAgent, warningType);
		}
	}

	public EnergyEvent generateStopEvent(EnergyAgent consumerAgent, RegulationWarning warning, boolean isComplementary, String comment) {
		if(isComplementary) {
			return secondProcessingData.generateStopEvent(consumerAgent, warning, comment);
		} else {
			return mainProcessingData.generateStopEvent(consumerAgent, warning, comment);
		}
	}

	public EnergyEvent generateExpiryEvent(EnergyAgent consumerAgent, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.generateExpiryEvent(consumerAgent);
		} else {
			return mainProcessingData.generateExpiryEvent(consumerAgent);
		}
	}

	public void handleRequestChanges(EnergyAgent consumerAgent, String tag) {
		EnergyRequest newRequest = consumerAgent.getEnergyRequest();
		Contract mainContract = getCurrentContract(false);
		Contract secondContrat = getCurrentContract(true);
		if(mainContract!=null && secondContrat==null) {
			Contract mainContractBefore = mainContract.clone();
			boolean changeOnRequest = mainContract.hasChangeOnRequest(newRequest);
			//logger.info("handleRequestChanges changeOnRequest = " + changeOnRequest);
			if(changeOnRequest) {
				logger.info("checkupRequestChanges " + consumerAgent.getAgentName() +" [" + tag  +"] difference found between the agent request and the contract request : " + mainContract);
				if (isContractWaitingValidation(false)) {
					String comment = consumerAgent.getAgentName() + " : the consumer agent has received a change request [W= " + newRequest.getPower()
					+ "] : the waiting contrat " + mainContract + " is canceled";
					stopCurrentContracts(consumerAgent, new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), this.timeShiftMS), comment);
				} else if (isContractOnGoing(false)) {
					// Stop the second contract if ongoing
					if(isContractOnGoing(true)) {
						stopCurrentContract(consumerAgent, true, new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), this.timeShiftMS), " receives update on request");
					}
					// Update contract
					try {
						boolean hasChanged = mainContract.modifyRequest(newRequest, null, logger);
						if(mainContract.hasGap()) {
							logger.error("handleRequestChanges : mainContract has gap : " + mainContract
									+ ", hasChanged = "  + hasChanged
									+ ", newRequest = " + newRequest + ", contractBefore = "+mainContractBefore
							);
						}
						if (hasChanged) {
							logger.info("handleRequestChanges : " + consumerAgent.getAgentName() + " receives contract update : "
									+ mainContract);
							generateUpdateEvent(consumerAgent, WarningType.CHANGE_REQUEST, false);
							// update CONTRACT property
							refreshContractsProperties(consumerAgent);
						}
					} catch (UnauthorizedModificationException e) {
						logger.warning("UnauthorizedModificationException thrown in handleRequestChanges : " + e.getMessage());
						boolean stopContract = true;
						if(Sapere.getNodeContext().isComplementaryRequestsActivated()) {
							EnergyRequest requestInLsa = getRequestProperty(consumerAgent);
							if(requestInLsa==null || !requestInLsa.isComplementary()) {
								// Generate a complemantary request with the missing power
								EnergyRequest complementaryRequestToSet = this.generateMissingRequest(consumerAgent);
								if(complementaryRequestToSet!=null && complementaryRequestToSet.isComplementary()) {
									try {
										if(false) {
											boolean hasChanged2 = mainContract.modifyRequest(newRequest, complementaryRequestToSet, logger);
											if(mainContract.hasGap()) {
												logger.error("handleRequestChanges (step2) : mainContract has gap : " + mainContract 
														+ ", hasChanged2 = "  + hasChanged2
														+ ", newRequest = " + newRequest + ", contractBefore = "+mainContractBefore
												);
											}
										}
										consumerAgent.getLsa().removePropertiesByName("REQ");
										consumerAgent.getLsa().addProperty(new Property("REQ", generateMissingRequest(consumerAgent)));
										logger.info("complementary request generated");
										requestInLsa = getRequestProperty(consumerAgent);
									} catch (Exception e1) {
										logger.error(e1);
										stopCurrentContracts(consumerAgent, new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), this.timeShiftMS), e1.getMessage());
									}
								}
							}
							if(requestInLsa!=null && requestInLsa.isComplementary()) {
								stopContract = false;
							}
						}
						if(stopContract){
							// Modification cannot be donne : stop the Contract
							logger.info("checkupRequestChanges " + consumerAgent.getAgentName() + " the ongoing contract will be stopped" );
							//consumerAgent.getLsa().removePropertiesByNames(new String[] { "SATISFIED" });
							stopCurrentContracts(consumerAgent, new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), this.timeShiftMS), e.getMessage());
						}
					} catch (Throwable e) {
						logger.error(e);
					}
				} else {
					logger.info("checkupRequestChanges " + consumerAgent.getAgentName() + " main contract = " + mainContract);
				}
			}
			if(mainContract.hasGap()) {
				double gap = mainContract.computeGap();
				logger.error("handleRequestChanges (end) : mainContract has gap : " + mainContract
						+ ", mainContractBefore = " + mainContractBefore
						+ ", newRequest = " + newRequest + ""
						+ ", gap = " + gap);
			}
		}
	}

	public void generateNewContract(EnergyAgent consumerAgent, CompositeOffer globalOffer) {
		Date validationDeadline = UtilDates.shiftDateSec(getCurrentDate(), CONTRACT_VALIDATION_DEALY_SEC);
		Contract newContract = new Contract(globalOffer, validationDeadline);
		Date test = UtilDates.removeTime(new Date());
		if(newContract.getBeginDate().after(test)) {
			logger.info("generateNewContract : for debug ");
		}
		newContract.addAgreement(consumerAgent, true);
		setCurrentContract(newContract);
		refreshContractProperties(consumerAgent, newContract.isComplementary());
		refreshLsaProperties(consumerAgent);
		EnergyDbHelper.setSingleOfferAccepted(globalOffer);
		logger.info(" consumer " + consumerAgent.getAgentName() + " set offer accepted : id:" + newContract.getSingleOffersIdsStr());
	}

	public void handleProducerConfirmation(EnergyAgent consumerAgent, String producer, ProtectedConfirmationTable producerConfirmTable) {
		// handle confirmation for both main and complementary processing data
		this.handleProducerConfirmation(consumerAgent, producer, false, producerConfirmTable);
		this.handleProducerConfirmation(consumerAgent, producer, true, producerConfirmTable);
	}

	public boolean handleConfirmationItem(EnergyAgent consumerAgent, String producer, ConfirmationItem confirmationItem) {
		boolean result = false;
		if(confirmationItem.isComplementary()) {
			result = secondProcessingData.addConfirmationItem(consumerAgent, producer, confirmationItem);
			if(result && secondProcessingData.isContractOnGoing()) {
				if(mainProcessingData.checkCanMerge(secondProcessingData.getCurrentContract())) {
					mergeContractsDecay = 3;
				}
			}
		} else {
			result = mainProcessingData.addConfirmationItem(consumerAgent, producer, confirmationItem);
		}
		if(result) {
			refreshContractProperties(consumerAgent, confirmationItem.isComplementary());
		}
		return result;
	}

	public void handleProducerConfirmation(EnergyAgent consumerAgent, String producer, boolean isComplementary,
			ProtectedConfirmationTable producerConfirmTable) {
		String comment = "";
		Contract currentContract = getCurrentContract(isComplementary);
		String agentName = consumerAgent.getAgentName();
		if (currentContract == null) {
			// Nothing to do
		} else if (currentContract.validationHasExpired()) {
			logger.warning("addProducerConfirmation " + agentName  + " validationHasExpired(2) : invalidate contract");
			comment = agentName + " : the validation has expired";
			stopCurrentContract(consumerAgent, isComplementary, null, comment);
		} else if (this.hasProducer(producer, isComplementary)) {
			try {
				if (producerConfirmTable.hasAccesAsConsumer(consumerAgent)) {
					ConfirmationItem confirmation = producerConfirmTable.getConfirmationItem(consumerAgent, isComplementary);
					if(confirmation!=null) {
						if (currentContract.isWaitingValidation()) {
							logger.info("addProducerConfirmation " + agentName + " Current contract waiting validation " + producer);
						}
						if(confirmation.isComplementary() == isComplementary) {
							Date confirmationDate = confirmation.getDate();
							if(confirmationDate.before(currentContract.getBeginDate())) {
								logger.warning("addProducerConfirmation " + consumerAgent.getAgentName() + " this confirmation ["
										+ confirmation + " at " + UtilDates.formatTimeOrDate(confirmationDate, timeShiftMS) + "] of " + producer
										+ " is before the contract begin date : it will not be taken into account ");
							} else {
								comment = confirmation.getComment();
								// Add received confirmaitons in memory
								boolean hasChanged = handleConfirmationItem(consumerAgent, producer, confirmation);
								if(hasChanged) {
									refreshContractsProperties(consumerAgent);
									refreshLsaProperties(consumerAgent);
								}
							}
						}
					}
				}
			} catch (PermissionException e) {
				logger.error(e);
			}
		}
	}

/*
	public void resetCurrentContract() {
		this.currentContract = null;
	}
*/
	public void resetCurrentContractIfInvalid() {
		// Delete contract if invalid
		mainProcessingData.resetCurrentContractIfInvalid();
		secondProcessingData.resetCurrentContractIfInvalid();
	}

	public boolean hasProducer(String producer) {
		return secondProcessingData.hasProducer(producer) || mainProcessingData.hasProducer(producer);
	}

	public boolean hasProducer(String producer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingData.hasProducer(producer);
		} else {
			return mainProcessingData.hasProducer(producer);
		}
	}

	public Set<String> getProducerAgents() {
		return SapereUtil.mergeSetStr(
			mainProcessingData.getProducerAgents(),
			secondProcessingData.getProducerAgents());
	}

	public double getMissing(EnergyAgent consumerAgent) {
		EnergyRequest need = consumerAgent.getEnergyRequest();
		if(need.isStartInFutur()) {
			return 0;
		} else {
			double powerProvided = mainProcessingData.getOngoingContractedPower();
			double missing = Math.max(0, need.getPower() - powerProvided);
			return missing;
		}
	}

	public EnergyRequest generateMissingRequest(EnergyAgent consumerAgent) {
		double missing = getMissing(consumerAgent);
		if(missing > 0) {
			EnergyRequest need = consumerAgent.getEnergyRequest();
			if(Sapere.getNodeContext().isComplementaryRequestsActivated()) {
				double powerProvided = mainProcessingData.getOngoingContractedPower();
				if(powerProvided > 0) {
					return need.generateComplementaryRequest(missing);
				}
			}
			return need;
		}
		return null;
	}

	public EnergyRequest getRequestProperty(EnergyAgent consumerAgent) {
		Property pReq = consumerAgent.getLsa().getOnePropertyByName("REQ");
		if(pReq!=null && pReq.getValue() instanceof EnergyRequest) {
			return (EnergyRequest) pReq.getValue();
		}
		return null;
	}

	public double getComplentaryRequestedPower(EnergyAgent consumerAgent) {
		EnergyRequest request = getRequestProperty(consumerAgent);
		if(request != null && request.isComplementary()) {
			return request.getPower();
		}
		return 0.0;
	}

	// specific
	public void refreshLsaProperties(EnergyAgent consumerAgent) {
		// Clean event
		//cleanEventProerty();
		EnergyRequest lsaRequest = this.getRequestProperty(consumerAgent);
		Property pSatisfied = consumerAgent.getLsa().getOnePropertyByName("SATISFIED");
		Contract lsaMainContract = getContractProperty(consumerAgent, false);
		Contract lsaSecondContract = getContractProperty(consumerAgent, true);
		if(consumerAgent.isDisabled()) {
			// Agent is disabled
			if(lsaRequest!=null || pSatisfied !=null) {
				consumerAgent.getLsa().removePropertiesByNames(new String[] {"REQ", "SATISFIED", "CONTRACT1", "CONTRACT2"});
			}
		} else if (isConsumerSatisfied(consumerAgent)) {
			// Agent is satified: remove REQ property
			if (lsaRequest != null ) {
				consumerAgent.getLsa().removePropertiesByNames(new String[] {"REQ"});
			}
			// Set SATISFIED property
			if(pSatisfied == null) {
				consumerAgent.getLsa().addProperty(new Property("SATISFIED", "1"));
			}
			// Check if the main contract is set
			if(mainProcessingData.isContractOnGoing() && lsaMainContract == null) {
				refreshContractProperties(consumerAgent, mainProcessingData.isComplementary());
			}
			// Check if the secondary contract is set
			if(secondProcessingData.isContractOnGoing() && lsaSecondContract == null) {
				refreshContractProperties(consumerAgent, secondProcessingData.isComplementary());
			}
		} else {
			// Agent not satisfied : SATISFIED property should be unset
			if(pSatisfied != null) {
				consumerAgent.getLsa().removePropertiesByName("SATISFIED");
			}
			if(hasContractWaitingValidation(consumerAgent)) {
				// Agent is not satisified but has already a contract waiting for validation : remove the request to stop offers generaiton
				if (lsaRequest != null) {
					consumerAgent.getLsa().removePropertiesByName("REQ");
				}
				if(	(mainProcessingData.isContractWaitingValidation() && lsaMainContract == null)
				|| 	(secondProcessingData.isContractWaitingValidation() && lsaSecondContract == null)) {
					// add CONTRACT1 property
					refreshContractProperties(consumerAgent, false);
				}
			} else {
				// Agent is not satisfied and has no pending contract
				// The agent has already a 1st supply and can have a complementary supply
				EnergyRequest requestMissing = this.generateMissingRequest(consumerAgent);
				if(requestMissing != null) {
					if(lsaRequest == null
						|| (requestMissing.isComplementary() != requestMissing.isComplementary())
						|| (Math.abs(requestMissing.getPower() -  lsaRequest.getPower()) > 0.0001)) {
						consumerAgent.getLsa().removePropertiesByName("REQ");
						consumerAgent.getLsa().addProperty(new Property("REQ", requestMissing));
					}
				}
			}
		}
		// Checkup for debug
		Contract mainContractInLsa = getContractProperty(consumerAgent, false);
		Contract complContractInLsa = getContractProperty(consumerAgent, true);
		mainProcessingData.checkup_debug(consumerAgent, mainContractInLsa, secondProcessingData.getCurrentContract());
		secondProcessingData.checkup_debug(consumerAgent, complContractInLsa, null);
		// checkup gaps
		double gap1 = (mainContractInLsa == null)? 0.0 : mainContractInLsa.computeGap();
		double gap2 = (complContractInLsa == null)? 0.0 :complContractInLsa.computeGap();
		if (Math.abs(gap1) > 0.0001) {
			if(complContractInLsa==null) {
				// In this case, wee should have only 1 contract
				logger.error("### refreshLsaProperties " + consumerAgent.getAgentName() + " main contract has gap " + UtilDates.df2.format(gap1)
					+ ", request : " + mainContractInLsa.getRequest()
					+ ", total supplied : " + mainContractInLsa.getPower()
					+ ", by supplier : " + mainContractInLsa.getMapPower()
					+ "");
			} else {
				double providedSdContract = complContractInLsa.getPower();
				if(Math.abs(gap1 - providedSdContract) > 0.0001) {
					logger.warning("refreshLsaProperties " + consumerAgent.getAgentName() + " main contract has gap " + UtilDates.df2.format(gap1) + " providedSdContract = " + providedSdContract);
				}
			}
		}
		if (Math.abs(gap2) > 0.0001) {
			logger.warning("refreshLsaProperties " + consumerAgent.getAgentName() + "### complementary contract has gap " + UtilDates.df2.format(gap2));
		}

		if(!consumerAgent.isSatisfied() && !hasContractWaitingValidation() && !consumerAgent.isDisabled()) {
			// checkup for debug
			lsaRequest = this.getRequestProperty(consumerAgent);
			if(lsaRequest==null) {
				logger.error("refreshLsaProperties final checkup "+ consumerAgent.getAgentName() + " : no request in LSA");
			} else {
				double missing = getMissing(consumerAgent);
				if(Math.abs(lsaRequest.getPower() - missing) > 0.0001) {
					logger.error("refreshLsaProperties final checkup " + consumerAgent.getAgentName()  + " : the request power do not correspond to the real need (" + missing +"). lsaRequest = " + lsaRequest);
				}
			}
		}
	}

	/**
	 * merge main and complementary contracts if both are ongoing
	 * @param consumerAgent
	 * @return
	 */
	public boolean mergeContracts(EnergyAgent consumerAgent) {
		boolean result = false;
		if(consumerAgent.isSatisfied() && secondProcessingData.isContractOnGoing()) {
			if(mergeContractsDecay <= 0) {
				// Check if the main an complementary contract can be merged into one contract
				if(mainProcessingData.checkCanMerge(secondProcessingData.getCurrentContract())) {
					try {
						// Merge is possible
						Contract secondContract = secondProcessingData.getCloneOfCurrentContract();
						// Stop complementary contract
						secondProcessingData.stopCurrentContract(consumerAgent, new RegulationWarning(WarningType.CONTRACT_MERGE, getCurrentDate(), this.timeShiftMS)
								, "merge of the main contract and this complementary contract in the main contract");
						// Merge the 2 contracts into the main contract
						result = mainProcessingData.mergeContract(consumerAgent, secondContract);
						if(result) {
							// TODO add merge event ?
							refreshContractsProperties(consumerAgent);
						}
					} catch (Exception e) {
						logger.error(e);
					}
				}
			} else {
				mergeContractsDecay--;
			}
		}
		return result;
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}
}
