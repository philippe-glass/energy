package com.sapereapi.agent.energy.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class ContractProcessingData {
	private Boolean isComplementary = null;
	private long timeShiftMS = 0;
	private EnergyEvent startEvent = null;
	private EnergyEvent stopEvent = null;
	private EnergyEvent expiryEvent = null;
	private Contract currentContract = null;
	private Map<String, ConfirmationItem> receivedConfirmations = null;
	private static SapereLogger logger = SapereLogger.getInstance();

	public ContractProcessingData(Boolean isComplementary, long _timeShiftMS) {
		super();
		this.isComplementary = isComplementary;
		this.timeShiftMS = _timeShiftMS;
		this.receivedConfirmations = new HashMap<String, ConfirmationItem>();
	}

	public Boolean isComplementary() {
		return isComplementary;
	}

	public Contract getCurrentContract() {
		return currentContract;
	}

	public void setCurrentContract(Contract currentContract) {
		this.currentContract = currentContract;
	}

	public void setCurrentContractMerged(boolean isMerged) {
		if(currentContract != null) {
			currentContract.setMerged(isMerged);
		}
	}
	public EnergyEvent getStartEvent() {
		return startEvent;
	}

	public EnergyEvent getStopEvent() {
		return stopEvent;
	}

	public EnergyEvent getExpiryEvent() {
		return expiryEvent;
	}

	public boolean isContractOnGoing() {
		return currentContract != null && currentContract.isOnGoing();
	}

	public boolean isContractWaitingValidation() {
		return currentContract != null && currentContract.isWaitingValidation();
	}

	public boolean needOffer() {
		if (currentContract == null) {
			return true;
		}
		return currentContract.hasExpired() || currentContract.hasDisagreement();
	}

	public void resetCurrentContractIfInvalid() {
		// Delete contract if invalid
		if (currentContract != null) {
			if (currentContract.hasDisagreement() || currentContract.hasExpired()) {
				currentContract = null;
			}
		}
	}

	public boolean hasProducer(String producer) {
		return currentContract != null && currentContract.hasProducer(producer);
	}

	public Set<String> getProducerAgents() {
		if (currentContract == null) {
			return new HashSet<String>();
		}
		return currentContract.getProducerAgents();
	}

	private EnergyEvent auxCreateEvent(EventType type, EnergyAgent consumerAgent, TimeSlot timeSlot, String comment) {
		EnergyEvent result = new EnergyEvent(type, consumerAgent.getEnergySupply(), comment);
		result.setBeginDate(timeSlot.getBeginDate());
		result.setEndDate(timeSlot.getEndDate());
		return result;
	}

	/**
	 * Power provided by producer
	 * 
	 * @return
	 */
	public PowerSlot getOngoingContractsPower(String locationFilter) {
		if (isContractOnGoing()) {
			return currentContract.computePowerSlot(locationFilter);
		}
		return new PowerSlot();
	}

	public PowerSlot getForcastOngoingContractsPower(String locationFilter, Date aDate) {
		if (isContractOnGoing()) {
			return currentContract.getForcastPowerSlot(locationFilter, aDate);
		}
		return new PowerSlot();
	}

	public Double getOngoingContractedPower() {
		Double power = Double.valueOf(0);
		if (isContractOnGoing()) {
			power += currentContract.getPower();
		}
		return power;
	}

	public Double getContractedPower() {
		Double power = Double.valueOf(0);
		// if (isContractOnGoing()) {
		if (currentContract != null) {
			power += currentContract.getPower();
		}
		return power;
	}

	public Map<String, PowerSlot> getOngoingContractsRepartition() {
		Map<String, PowerSlot> result = new HashMap<String, PowerSlot>();
		if (this.isContractOnGoing()) {
			for (String producer : currentContract.getProducerAgents()) {
				PowerSlot power = currentContract.getPowerSlotFromAgent(producer);
				if (power != null) {
					boolean isLocal = Sapere.getInstance().isLocalAgent(producer);
					String agentName = producer + (isLocal ? "" : "*");
					result.put(agentName, power);
				}
			}
		}
		return result;
	}

	public Contract getCloneOfCurrentContract() {
		if (this.currentContract == null) {
			return null;
		} else {
			return currentContract.clone();
		}
	}

	public EnergyEvent generateStartEvent(EnergyAgent consumerAgent) {
		startEvent = auxCreateEvent(EventType.CONTRACT_START, consumerAgent, currentContract.getTimeSlot(), "");
		startEvent = EnergyDbHelper.registerEvent2(startEvent, currentContract);
		// Remove other events
		stopEvent = null;
		expiryEvent = null;
		// Post event in LSA
		if(startEvent.isComplementary()) {
			logger.info("generateStartEvent for debug : complt event");
		}
		consumerAgent.postEvent(startEvent);
		return startEvent;
	}

	public EnergyEvent generateUpdateEvent(EnergyAgent consumerAgent, WarningType warningType) {
		if (startEvent != null && currentContract != null) {
			if(!currentContract.checkLocationId())  {
				logger.error("generateUpdateEvent contract isseur has no location id");
			}
			EnergyEvent originStartEvent = startEvent.clone();
			startEvent = auxCreateEvent(EventType.CONTRACT_UPDATE, consumerAgent, currentContract.getTimeSlot(), "");
			startEvent.setOriginEvent(originStartEvent);
			if(originStartEvent!=null) {
				PowerSlot powerSlotBefore = originStartEvent.getPowerSlot();
				PowerSlot powerUpdate = startEvent.getPowerSlot().clone();
				powerUpdate.substract(powerSlotBefore);
				startEvent.setPowerUpateSlot(powerUpdate);
			}
			startEvent.setWarningType(warningType);
			startEvent = EnergyDbHelper.registerEvent2(startEvent, currentContract);
			// Remove other events
			stopEvent = null;
			expiryEvent = null;
			// Post event in LSA
			consumerAgent.postEvent(startEvent);
			return startEvent;
		}
		return null;
	}

	public EnergyEvent generateStopEvent(EnergyAgent consumerAgent, RegulationWarning warning, String log) {
		Date timeStop = UtilDates.getCurrentSeconde(timeShiftMS);
		if (warning != null && warning.getChangeRequest() != null) {
			timeStop = warning.getChangeRequest().getBeginDate();
		}
		stopEvent = auxCreateEvent(EventType.CONTRACT_STOP, consumerAgent, new TimeSlot(timeStop, timeStop), log);
		if (startEvent != null) {
			stopEvent.setOriginEvent(startEvent.clone());
		}
		if (warning != null) {
			stopEvent.setWarningType(warning.getType());
		}
		stopEvent = EnergyDbHelper.registerEvent2(stopEvent);
		// Remove other events
		startEvent = null;
		expiryEvent = null;
		// Post event in LSA
		consumerAgent.postEvent(stopEvent);
		return stopEvent;
	}

	public EnergyEvent generateExpiryEvent(EnergyAgent consumerAgent) {
		if (currentContract != null) {
			Date evtDate = UtilDates.getNewDate(timeShiftMS);
			evtDate = currentContract.getEndDate();
			expiryEvent = auxCreateEvent(EventType.CONTRACT_EXPIRY, consumerAgent, new TimeSlot(evtDate, evtDate), "");
			if (startEvent != null) {
				expiryEvent.setOriginEvent(startEvent.clone());
			}
			expiryEvent = EnergyDbHelper.registerEvent2(expiryEvent);
			// Post event in LSA
			consumerAgent.postEvent(expiryEvent);
			// Remove other events
			startEvent = null;
			stopEvent = null;
			return expiryEvent;
		}
		return null;
	}

	public void stopCurrentContract(EnergyAgent consumerAgent, RegulationWarning warning, String logCancel) {
		if (currentContract == null) {
			return;
		}
		if (currentContract.isWaitingValidation() && logCancel != null) {
			EnergyDbHelper.setSingleOfferCanceled(currentContract, logCancel);
		}
		boolean isOldContractOnGoing = currentContract.isOnGoing();
		// Contract oldContract = getCloneOfCurrentContract();
		receivedConfirmations.clear();
		currentContract.stop(consumerAgent.getAgentName());
		if (currentContract.hasDisagreement()) {
			try {
				// Add property to indicate that the contract is canceled
				// refreshContractProperties(consumerAgent);
				// Generate a stop event is the contract was on going
				if (isOldContractOnGoing) {
					if (getStartEvent() != null && getStopEvent() == null) {
						EnergyEvent stopEvent = generateStopEvent(consumerAgent, warning, logCancel);
						logger.info("Contract interuption " + currentContract + " " + stopEvent);
					}
				}
			} catch (Throwable e) {
				logger.error(e);
			}
		}
		// TODO check if there is no bug
		currentContract = null;
	}

	public void postExpiryEvent(EnergyAgent consumerAgent) {
		if (getExpiryEvent() == null && getStartEvent() != null) {
			try {
				generateExpiryEvent(consumerAgent);
				logger.info("Contract expiration : unset start event " + expiryEvent);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	public ConfirmationItem getReceivedConfirmationItem(String producer) {
		return this.receivedConfirmations.get(producer);
	}

	public boolean hasReceivedConfirmationItem(String producer) {
		return this.receivedConfirmations.containsKey(producer);
	}

	public boolean addConfirmationItem(EnergyAgent consumerAgent, String producer, ConfirmationItem confirmation) {
		// Add received confirmaitons in memory
		receivedConfirmations.put(producer, confirmation);
		String agentName = consumerAgent.getAgentName();
		String comment = confirmation.getComment();
		Boolean isOK = confirmation.getIsOK();
		boolean hasChanged = false;
		// hanle confirmation item
		if (Boolean.TRUE.equals(isOK)) {
			if (!currentContract.hasAgreement(producer)) {
				// Contract validation
				currentContract.addProducerAgreement(consumerAgent, producer, true);
				if (currentContract.hasAllAgreements() && getStartEvent() == null) {
					logger.info(" --- validaiton of contract= " + currentContract.getConsumerAgent());
					currentContract.checkBeginNotPassed();
					// Add event to indicate that the contract is valided
					EnergyEvent startEvent = generateStartEvent(consumerAgent);
					String sOfferIds = currentContract.getSingleOffersIdsStr();
					// SET contract eventid in single_offer
					EnergyDbHelper.setSingleOfferLinkedToContract(currentContract, startEvent);
					logger.info("Step2a : startEvent = " + startEvent + " current instance = " + this + " sOfferIds="
							+ sOfferIds);
					// eventToPost = startEvent.clone();
				}
				hasChanged = true;
			}
		} else if (Boolean.FALSE.equals(isOK)) {
			if (!currentContract.hasDisagreement(producer)) {
				// Cancel the contract
				logger.warning(agentName + " receive invalidation from " + producer + " " + comment
						+ " : cancel the contract " + currentContract);
				if (!currentContract.hasAllAgreements()) {
					// Contract in waiting status
					logger.warning(agentName + " the following offers will not be linked to a contract event : "
							+ currentContract.getSingleOffersIdsStr());
				}
				stopCurrentContract(consumerAgent, null, comment);
				hasChanged = true;
			}
		}
		return hasChanged;
	}

	// Check producers confirmations
	public Set<String> getProducersWithNoRecentConfirmations(EnergyAgent consumerAgent) {
		Set<String> result = new HashSet<String>();
		if (isContractOnGoing()) {
			result = SapereUtil.getAgentNamesWithNoRecentConfirmations(consumerAgent,
					getProducerAgents(), this.receivedConfirmations, 5, logger);
		}
		return result;
	}

	public String getLabel() {
		return (this.isComplementary ? "Second " : "main ") + " table";
	}

	// Checkup for debug
	public void checkup_debug(EnergyAgent consumerAgent, Contract contractInLsa, Contract complementaryContract) {
		if (currentContract == null) {
			if (contractInLsa != null) {
				logger.warning("checkup_debug " + getLabel() + " Contract in LSA not up to date-1");
			}
		} else if (currentContract.hasChanged(contractInLsa)) {
			logger.warning("checkup_debug " + getLabel() + " Contract in LSA not up to date-2 " + currentContract
					+ "  IN LSA: " + contractInLsa);
		}
	}

	public boolean checkCanMerge(Contract otherContract)  {
		boolean result = false;
		if(currentContract!=null) {
			try {
				result = currentContract.checkCanMerge(otherContract);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return result;
		//return false;
	}

	public boolean mergeContract(EnergyAgent consumerAgent, Contract otherContract) throws Exception {
		boolean result = false;
		if(currentContract!=null) {
			result = currentContract.merge(otherContract);
			if(currentContract.hasGap()) {
				logger.error("mergeContract currentContract has gap after merge : " + currentContract);
			}
			generateUpdateEvent(consumerAgent, WarningType.CONTRACT_MERGE);
		}
		return result;
	}
}
