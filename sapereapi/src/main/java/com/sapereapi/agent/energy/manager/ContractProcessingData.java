package com.sapereapi.agent.energy.manager;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.referential.EventMainCategory;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class ContractProcessingData {
	private Boolean isComplementary = null;
	private long timeShiftMS = 0;
	private Map<Date, EnergyEvent> mapLastEvents = new TreeMap<Date, EnergyEvent>(Collections.reverseOrder());
	private Contract currentContract = null;
	private Map<String, ConfirmationItem> receivedConfirmations = null;
	protected boolean isAwardsActivated = false;
	private Map<Date, Double> awardCreaditUsage = new HashMap<Date, Double>();
	private Date eventDateLastRefresh = null;
	private Date lastRefreshAwardCreditUsage = null;
	private static SapereLogger logger = SapereLogger.getInstance();

	public ContractProcessingData(Boolean isComplementary, NodeContext nodeContext) {
		super();
		this.isComplementary = isComplementary;
		this.timeShiftMS = nodeContext.getTimeShiftMS();
		this.isAwardsActivated = nodeContext.isAwardsActivated();
		this.receivedConfirmations = new HashMap<String, ConfirmationItem>();
		this.awardCreaditUsage.clear();
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

	public EnergyEvent getLastEvent() {
		if(mapLastEvents.size() > 0) {
			return mapLastEvents.values().iterator().next();
		}
		return null;
	}

	public EnergyEvent getLastEvent(EventMainCategory evtCategory) {
		EnergyEvent lastEvent = getLastEvent();
		if (lastEvent != null && evtCategory != null) {
			boolean categoryMatches = evtCategory.equals(lastEvent.getType().getMainCategory());
			if (categoryMatches) {
				return lastEvent;
			}
		}
		return null;
	}

	public EnergyEvent getLastEvent(EventMainCategory[] evtCategories) {
		EnergyEvent lastEvent = getLastEvent();
		if (lastEvent != null && evtCategories.length > 0) {
			for (EventMainCategory evtCategory : evtCategories) {
				boolean categoryMatches = evtCategory.equals(lastEvent.getType().getMainCategory());
				if (categoryMatches) {
					return lastEvent;
				}
			}
		}
		return null;
	}

	public EnergyEvent getStartEvent() {
		EventMainCategory[] evtCategories = { EventMainCategory.START, EventMainCategory.UPDATE , EventMainCategory.SWITCH };
		return getLastEvent(evtCategories);
	}

	public EnergyEvent getStopEvent() {
		return getLastEvent(EventMainCategory.STOP);
	}

	public EnergyEvent getExpiryEvent() {
		return getLastEvent(EventMainCategory.EXPIRY);
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

	public void resetCurrentContractIfInvalid(EnergyAgent agent, PowerSlot mainContractPower) {
		// Delete contract if invalid
		if (currentContract != null) {
			if(currentContract.hasExpired()) {
				// post expiry event
				postExpiryEvent(agent, mainContractPower);
				currentContract = null;
			} else if (currentContract.hasDisagreement()) {
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

	/*
	private EnergyEvent oldAuxCreateEvent(EventType type, EnergyAgent consumerAgent, TimeSlot timeSlot, String comment, PowerSlot mainContractPower) {
		EnergyEvent result = consumerAgent.generateEvent(type, comment);
		result.setBeginDate(timeSlot.getBeginDate());
		result.setEndDate(timeSlot.getEndDate());
		double firstRate = (currentContract == null) ? 0.0 : currentContract.getFirstRateValue();
		result.setFirstRate(firstRate);
		if(currentContract != null) {
			if(!currentContract.isComplementary()) {
				double powerToSet = currentContract.getPower();
				if(Math.abs(powerToSet - result.getPower()) >= 0.001) {
					double needPower = consumerAgent.getGlobalNeed().getPower();
					logger.error("auxCreateEvent gap between created contract and consumer need : powerToSet " + powerToSet + ", need = " + needPower
					+ ", currentContract = " + currentContract + ", evt = " + result);
				}
				result.setPower(currentContract.getPower());
				result.setPowerMin(currentContract.getPowerMin());
				result.setPowerMax(currentContract.getPowerMax());
			}
		}
		return result;
	}
	*/

	private EnergyEvent auxCreateEvent(EventType type, Date eventDate, String comment, PowerSlot mainContractPower) throws HandlingException {
		if(currentContract == null) {
			throw new HandlingException("auxCreateEvent : currentContract is null");
		}
		PowerSlot powerSlot = currentContract.getPowerSlot();
		if(currentContract.isComplementary() && mainContractPower != null) {
			PowerSlot toAdd = mainContractPower;
			powerSlot.add(toAdd);
		}
		EnergyEvent result = new EnergyEvent(type, currentContract.getIssuerProperties(), currentContract.isComplementary(), powerSlot
				, currentContract.getBeginDate(), currentContract.getEndDate(), comment, currentContract.getFirstRateValue());
		if(eventDate != null) {
			result.setBeginDate(eventDate);
			result.setEndDate(eventDate);
		}
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

	private void pushEvent(EnergyEvent newEvent) {
		Date newDate = newEvent.getBeginDate();
		if(mapLastEvents.size() > 0) {
			logger.info("pushEvent : for debug : lastEvents = " + mapLastEvents);
			Date firstDate = mapLastEvents.keySet().iterator().next();
			if(newDate.before(firstDate)) {
				logger.error("pushEvent : new date " + UtilDates.format_time.format(newDate) + " is before last event date " + UtilDates.format_time.format(firstDate));
				logger.error("newEvent : " + newEvent);
				logger.error("mapLastEvents : " + mapLastEvents);
				mapLastEvents.clear();
			}
		}
		mapLastEvents.put(newDate, newEvent);
		int maxSize = 5;
		while(mapLastEvents.size() > maxSize) {
			Iterator<Date> dateIt = mapLastEvents.keySet().iterator();
			Date idxToRemove = dateIt.next();
			while(dateIt.hasNext()) {
				idxToRemove = dateIt.next();
			}
			mapLastEvents.remove(idxToRemove);
		}
	}

	public EnergyEvent generateStartEvent(EnergyAgent consumerAgent, PowerSlot mainContractPower) throws HandlingException {
		currentContract.checkDates(logger, "generateStartEvent : begin");
		EnergyEvent startEvent = auxCreateEvent(EventType.CONTRACT_START, null, "", mainContractPower);
		startEvent = EnergyDbHelper.registerEvent2(startEvent, currentContract, "generateStartEvent by " + consumerAgent.getAgentName());
		currentContract.setEventId(startEvent.getId());
		if(startEvent.isComplementary()) {
			logger.info("generateStartEvent for debug : complt event");
		}
		pushEvent(startEvent);
		// Post event in LSA
		consumerAgent.postEvent(startEvent);
		return startEvent;
	}

	public EnergyEvent generateUpdateEvent(EnergyAgent consumerAgent, WarningType warningType, PowerSlot mainContractPower) throws HandlingException {
		EnergyEvent startEvent = getStartEvent();
		if (startEvent != null && currentContract != null) {
			//refreshCreditUsedHWcurrentContract("generateUpdateEvent");
			if(!currentContract.checkLocation())  {
				logger.error("generateUpdateEvent contract isseur has no location id");
			}
			EnergyEvent originEvent = startEvent.clone();
			EnergyEvent updateEvent = auxCreateEvent(EventType.CONTRACT_UPDATE,  null, "", mainContractPower);
			updateEvent.setOriginEvent(originEvent);
			if(originEvent!=null) {
				PowerSlot powerSlotBefore = originEvent.getPowerSlot();
				PowerSlot powerUpdate = updateEvent.getPowerSlot().clone();
				powerUpdate.substract(powerSlotBefore);
				updateEvent.setPowerUpdateSlot(powerUpdate);
			}
			updateEvent.setWarningType(warningType);
			updateEvent = EnergyDbHelper.registerEvent2(updateEvent, currentContract, "generateUpdateEvent by " + consumerAgent.getAgentName());
			currentContract.setEventId(updateEvent.getId());
			if(updateEvent.getIssuerProperties() != null && updateEvent.getIssuerProperties().getLocation() == null) {
				logger.error("generateUpdateEvent : updateEvent location is null");
			}
			pushEvent(updateEvent);
			// Post event in LSA
			consumerAgent.postEvent(updateEvent);
			return updateEvent;
		}
		return null;
	}

	public EnergyEvent generateStopEvent(EnergyAgent consumerAgent, RegulationWarning warning, String log, PowerSlot mainContractPower) throws HandlingException {
		Date timeStop = UtilDates.getCurrentSeconde(timeShiftMS);
		if (warning != null && warning.getChangeRequest() != null && warning.getChangeRequest().getSupply() != null) {
			timeStop = warning.getChangeRequest().getSupply().getBeginDate();
		}
		EnergyEvent stopEvent = auxCreateEvent(EventType.CONTRACT_STOP, timeStop, log, mainContractPower);
		EnergyEvent startEvent = getStartEvent();
		if (startEvent != null) {
			stopEvent.setOriginEvent(startEvent.clone());
		}
		if (warning != null) {
			stopEvent.setWarningType(warning.getType());
		}
		stopEvent = EnergyDbHelper.registerEvent2(stopEvent, "generateStopEvent by " + consumerAgent.getAgentName());
		pushEvent(stopEvent);
		// Post event in LSA
		consumerAgent.postEvent(stopEvent);
		return stopEvent;
	}

	public EnergyEvent generateExpiryEvent(EnergyAgent consumerAgent, PowerSlot mainContractPower) throws HandlingException {
		if (currentContract != null) {
			refreshCreditUsedHWcurrentContract("generateExpiryEvent");
			Date evtDate = UtilDates.getNewDateNoMilliSec(timeShiftMS);
			evtDate = currentContract.getEndDate();
			EnergyEvent expiryEvent = auxCreateEvent(EventType.CONTRACT_EXPIRY, evtDate, "", mainContractPower);
			EnergyEvent startEvent = getStartEvent();
			if (startEvent != null) {
				expiryEvent.setOriginEvent(startEvent.clone());
			}
			expiryEvent = EnergyDbHelper.registerEvent2(expiryEvent, "generateExpiryEvent by " + consumerAgent.getAgentName());
			pushEvent(expiryEvent);
			// Post event in LSA
			consumerAgent.postEvent(expiryEvent);
			return expiryEvent;
		}
		return null;
	}

	public void stopCurrentContract(EnergyAgent consumerAgent, RegulationWarning warning, String logCancel, PowerSlot mainContractPower) throws HandlingException {
		if (currentContract == null) {
			return;
		}
		if (currentContract.isWaitingValidation() && logCancel != null) {
			EnergyDbHelper.setSingleOfferCanceled(currentContract, logCancel);
		}
		boolean isOldContractOnGoing = currentContract.isOnGoing();
		refreshCreditUsedHWcurrentContract("stopCurrentContract");

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
						EnergyEvent stopEvent = generateStopEvent(consumerAgent, warning, logCancel, mainContractPower);
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

	public void postExpiryEvent(EnergyAgent consumerAgent, PowerSlot mainContractPower) {
		if (getExpiryEvent() == null && getStartEvent() != null) {
			try {
				generateExpiryEvent(consumerAgent, mainContractPower);
				logger.info("Contract expiration : unset start event ");
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

	public boolean addConfirmationItem(EnergyAgent consumerAgent, String producer, ConfirmationItem confirmation, PowerSlot mainContractPower) throws HandlingException {
		// Add received confirmations in memory
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
				if (currentContract.hasAllAgreements()) {
					boolean createEvent = false;
					EnergyEvent startEvent = getStartEvent();
					if(startEvent == null) {
						createEvent = true;
					} else {
						logger.info("addConfirmationItem --- contract validated by startEvent is not null : ");
						logger.info(", startEvent = " + startEvent);
						createEvent = startEvent.hasExpired();
					}
					if (createEvent) {
						logger.info("addConfirmationItem --- validaiton of contract= " + currentContract.getConsumerAgent());
						currentContract.checkBeginNotPassed();
						// Add a CONTRACT_START event to indicate that the contract has been validated
						startEvent = generateStartEvent(consumerAgent, mainContractPower);
						String sOfferIds = currentContract.getSingleOffersIdsStr();
						// Set the ling to contract eventid in the corresponding offers
						EnergyDbHelper.setSingleOfferLinkedToContract(currentContract, startEvent);
						logger.info("addConfirmationItem Step2a : startEvent = " + startEvent + " current instance = " + this + " sOfferIds="
								+ sOfferIds);
					}
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
				stopCurrentContract(consumerAgent, null, comment, mainContractPower);
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

	public boolean mergeContract(EnergyAgent consumerAgent, Contract otherContract) throws HandlingException {
		boolean result = false;
		if(currentContract!=null) {
			this.refreshCreditUsedHWcurrentContract("mergeContract");
			result = currentContract.merge(otherContract);
			if(currentContract.hasGap()) {
				logger.error("mergeContract currentContract has gap after merge : " + currentContract);
			}
			generateUpdateEvent(consumerAgent, WarningType.CONTRACT_MERGE, null);
		}
		return result;
	}

	public Date getLasEventDate() {
		EnergyEvent lastEvent = this.getLastEvent();
		if(lastEvent != null) {
			return lastEvent.getBeginDate();
		}
		return null;
	}

	public boolean manageAwardCreditUsage(String step) {
		if (isAwardsActivated) {
			Date lastEventDate = this.getLasEventDate();
			// Check if there is an event has been created recently
			boolean toRefresh = lastEventDate != null && eventDateLastRefresh != null
					&& lastEventDate.after(eventDateLastRefresh);
			String step2 = step + (toRefresh? " [reventEvent]" : "");
			if (!toRefresh) {
			//  check if the current contract is about to expire
				toRefresh = isContractIsAboutToExpire();
				step2 = step + (toRefresh? " [contractIsAboutToExpire]" : "");
			}
			if (lastRefreshAwardCreditUsage != null && !toRefresh) {
				// check if no refresh has been done for more than x minutes
				Date minLastUpdate = UtilDates.shiftDateMinutes(getCurrentDate(), -10);
				toRefresh = lastRefreshAwardCreditUsage.before(minLastUpdate);
				step2 = step + (toRefresh? " [noRecentRefresh]" : "");
			}
			eventDateLastRefresh = getLasEventDate();
			if (toRefresh) {
				computeAllAwardCreditUsage(step2);
			}
			return toRefresh;
		}
		return false;
	}

	public boolean isContractIsAboutToExpire() {
		return currentContract != null && currentContract.isOnGoing() && currentContract.isAboutToExpire(3);
	}

	public void refreshCreditUsedHWcurrentContract(String step) {
		if (isAwardsActivated) {
			if (currentContract != null) {
				logger.info("refreshCreditUsedHWcurrentContract " + currentContract.getConsumerAgent() + " " + step);
				double lastCreaditUsage = currentContract.computeCreditUsedWH(step, logger);
				if (Math.abs(lastCreaditUsage) >= 0.001) {
					this.awardCreaditUsage.put(currentContract.getBeginDate(), lastCreaditUsage);
				}
				logger.info("refreshCreditUsedHWcurrentContract " + currentContract.getConsumerAgent() + " "
						+ step + ", lastCreaditUsage = " + SapereUtil.roundPower(lastCreaditUsage));
				this.lastRefreshAwardCreditUsage = getCurrentDate();
			}
		}
	}

	public Map<Date, Double> computeAllAwardCreditUsage(String step) {
		Map<Date, Double> result = new TreeMap<Date, Double>();
		if (isAwardsActivated) {
			refreshCreditUsedHWcurrentContract(step + " > computeAllAwardCreditUsage ");
			for (Date contractDate : awardCreaditUsage.keySet()) {
				result.put(contractDate, awardCreaditUsage.get(contractDate));
			}
		}
		return result;
	}

	public void clearAllAwardCreditUsage() {
		awardCreaditUsage.clear();
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(this.timeShiftMS);
	}
}
