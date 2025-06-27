package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sapereapi.agent.energy.manager.ConsumersProcessingMangager;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.DoublonException;
import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.ConfirmationTable;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyEventTable;
import com.sapereapi.model.energy.EnergyFlow;
import com.sapereapi.model.energy.EnergyStorage;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.award.AwardItem;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.reschedule.RescheduleItem;
import com.sapereapi.model.energy.reschedule.RescheduleTable;
import com.sapereapi.model.protection.ProtectedConfirmationTable;
import com.sapereapi.model.referential.EventMainCategory;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.SpreadingEvent;

public abstract class EnergyAgent extends MicroGridAgent implements IEnergyAgent {
	protected int id;
	protected EnergyEvent waitingStartEvent = null;
	protected Map<Date, EnergyEvent> mapLastEvents = new TreeMap<Date, EnergyEvent>(Collections.reverseOrder());
	private static final long serialVersionUID = 1L;
	protected boolean firstDecay = true;
	protected int eventDecay = 0;
	protected List<RegulationWarning> receivedWarnings = new ArrayList<RegulationWarning>();
	public final static int DISABLED_DURATION_MINUTES = 5;
	public final static int DISABLED_SHORT_DURATION_SEC = 5;
	protected NodeTotal lastNodeTotal = null;
	protected Date timeLastDecay = null;
	protected AwardItem award = null;
	protected EnergyStorage storage = null;

	public EnergyAgent(int _id, String name, AgentAuthentication authentication, String[]lsaInputTags,
			String[] lsaOutputTags, NodeContext nodeContext) {
		super(name, authentication, lsaInputTags, lsaOutputTags, nodeContext);
		initCommonFields(_id, authentication, nodeContext);
	}

	public void initCommonFields(int _id, AgentAuthentication authentication, NodeContext nodeContext) {
		setEpsilon(0); // No greedy policy
		super.initSGAgent(this.agentName, authentication, nodeContext);
		id = _id;
		this.debugLevel = 0;
		// logger.info("EnergyAgent : lsa = " + lsa.toVisualString());
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

	public EnergyEvent getExpiryEvent() {
		return getLastEvent(EventMainCategory.EXPIRY);
	}

	public boolean isStartInFutur() {
		EnergyFlow supply = getProductionOrNeed();
		return supply.isStartInFutur();
	}

	public EnergyFlow getProductionOrNeed() {
		if(this.isConsumer()) {
			return getGlobalNeed();
		} else {
			return getGlobalProduction();
		}
	}

	public EnergyEvent generateStartEvent() throws HandlingException {
		EnergyFlow supplyOrRequest = getProductionOrNeed();
		EventType eventType = getStartEventType();
		if(supplyOrRequest.isStartInFutur()) {
			EnergyEvent startEvent = getStartEvent();
			if(startEvent != null) {
				mapLastEvents.remove(startEvent.getBeginDate());
			}
		} else {
			EnergyEvent startEvent = this.generateEvent(eventType, "");
			try {
				startEvent = EnergyDbHelper.registerEvent(startEvent, "generateStartEvent by " + this.agentName);
			} catch (DoublonException e) {
				startEvent = EnergyDbHelper.retrieveEvent(eventType, agentName, false, supplyOrRequest.getBeginDate());
			}
			waitingStartEvent = null;
			if (startEvent.getId() != null) {
				this.setEventId(startEvent.getId());
			}
			// Post event in LSA
			pushAndPostEvent(startEvent);
			return startEvent;
		}
		return null;
	}

	public EnergyEvent generateUpdateEvent(WarningType warningType, String log) throws HandlingException {
		EventType eventType = getUpdateEventType();
		EnergyEvent startEvent = getStartEvent();
		if (startEvent != null) {
			EnergyEvent updateEvent = this.generateEvent(eventType, log);
			updateEvent.setWarningType(warningType);
			EnergyEvent originStartEvent = startEvent.clone();
			updateEvent.setOriginEvent(originStartEvent);
			PowerSlot powerSlotBefore = originStartEvent.getPowerSlot();
			PowerSlot powerUpdate = updateEvent.getPowerSlot().clone();
			powerUpdate.substract(powerSlotBefore);
			updateEvent.setPowerUpdateSlot(powerUpdate);
			updateEvent = EnergyDbHelper.registerEvent2(updateEvent, "generateUpdateEvent by " + this.agentName);
			if (updateEvent.getId() != null) {
				this.setEventId(updateEvent.getId());
			}
			// Post event in LSA
			pushAndPostEvent(updateEvent);
			return updateEvent;
		}
		return null;
	}

	public EnergyEvent generateSwitchEvent(WarningType warningType) throws HandlingException {
		EventType eventType = getSwitchEventType();
		EnergyEvent startEvent = getStartEvent();
		if (startEvent != null) {
			EnergyEvent switchEvent = this.generateEvent(eventType, "");
			switchEvent.setWarningType(warningType);
			EnergyEvent originStartEvent = startEvent.clone();
			switchEvent.setOriginEvent(originStartEvent);
			PowerSlot powerUpdate = switchEvent.getPowerSlot().clone();
			if (EventObjectType.REQUEST.equals(switchEvent.getEventObjectType())) {
				powerUpdate.multiplyBy(-1.0);
			}
			PowerSlot powerSlotBefore = originStartEvent.getPowerSlot().clone();
			if (EventObjectType.REQUEST.equals(originStartEvent.getEventObjectType())) {
				powerSlotBefore.multiplyBy(-1.0);
			}
			powerUpdate.substract(powerSlotBefore);
			// powerUpdate.add(powerSlotBefore);
			switchEvent.setPowerUpdateSlot(powerUpdate);
			switchEvent = EnergyDbHelper.registerEvent2(switchEvent, "generateSwitchEvent by " + this.agentName);
			if (switchEvent.getId() != null) {
				this.setEventId(switchEvent.getId());
			}
			// Post event in LSA
			boolean alreadyIn = mapLastEvents.containsKey(switchEvent.getBeginDate());
			EnergyEvent lastEvent = getLastEvent();
			logger.info("generateSwitchEvent : for debug : before pushAndPostEvent : switchEvent = " + switchEvent + ", alreadyIn = " + alreadyIn+ ", lastEvent = " + lastEvent);
			pushAndPostEvent(switchEvent);
			return switchEvent;
		}
		return null;
	}

	public EnergyEvent generateExpiryEvent() throws HandlingException {
		EnergyFlow supply = getProductionOrNeed();
		EventType eventType = getExpiryEventType();
		Date expiryDate = supply.getEndDate();
		EnergyEvent expiryEvent = this.generateEvent(eventType, "");
		expiryEvent.setBeginDate(expiryDate);
		expiryEvent.setEndDate(expiryDate);
		EnergyEvent startEvent = getStartEvent();
		if (startEvent != null) {
			expiryEvent.setOriginEvent(startEvent.clone());
		}
		expiryEvent = EnergyDbHelper.registerEvent2(expiryEvent, "generateExpiryEvent by " + this.agentName);
		// Post event in LSA
		pushAndPostEvent(expiryEvent);
		return expiryEvent;
	}

	public EnergyEvent generateStopEvent(RegulationWarning warning, String log) throws HandlingException {
		Date timeStop = warning.getDate();
		// Caution : on ProducerAgent implementation : Date timeStop =
		EventType eventType = getStopEventType();
		EnergyEvent stopEvent = this.generateEvent(eventType, log);
		stopEvent.setBeginDate(timeStop);
		stopEvent.setEndDate(timeStop);
		EnergyEvent startEvent = getStartEvent();
		if (startEvent != null) {
			stopEvent.setOriginEvent(startEvent.clone());
		}
		if (warning != null) {
			stopEvent.setWarningType(warning.getType());
		}
		stopEvent = EnergyDbHelper.registerEvent2(stopEvent, "generateStopEvent by " + this.agentName);
		// Post event in LSA
		pushAndPostEvent(stopEvent);
		return stopEvent;
	}

	public EnergyEventTable getEventTable() {
		Property pEvent = this.lsa.getOnePropertyByName("EVENT");
		if (pEvent != null && pEvent.getValue() instanceof EnergyEventTable) {
			return (EnergyEventTable) pEvent.getValue();
		}
		return null;
	}

	private void pushAndPostEvent(EnergyEvent newEvent) {
		Date newDate = newEvent.getBeginDate();
		if(mapLastEvents.size() > 0) {
			//logger.info("pushAndPostEvent : for debug : lastEvents = " + mapLastEvents);
			Date firstDate = mapLastEvents.keySet().iterator().next();
			if(newDate.before(firstDate)) {
				logger.error("pushAndPostEvent : new date " + UtilDates.format_time.format(newDate) + " is before last event date " + UtilDates.format_time.format(firstDate));
				logger.error("pushAndPostEvent : newEvent : " + newEvent);
				for(Date nextDate : mapLastEvents.keySet()) {
					logger.error("pushAndPostEvent : mapLastEvents[" + UtilDates.format_time.format(nextDate) + "] = " + mapLastEvents.get(nextDate));
				}
				// clear all previous events
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
		postEvent(newEvent);
	}

	public void postEvent(EnergyEvent eventToPost) {
		EnergyEventTable energyEventTable = getEventTable();
		if (energyEventTable == null) {
			energyEventTable = new EnergyEventTable();
		}
		try {
			energyEventTable.putEvent(eventToPost.clone());
			replacePropertyWithName(new Property("EVENT", energyEventTable));
			eventDecay = SapereUtil.EVENT_INIT_DECAY;
			firstDecay = true;
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public EnergyEvent getStopEvent() {
		return getLastEvent(EventMainCategory.STOP);
	}

	public void cleanEventProperties() {
		/*
		if(this.isConsumer()) {
			logger.info("EnergyAgent.cleanEventProperties : For debug : eventDecay = " + eventDecay);
		}*/
		if (eventDecay > 0) {
			eventDecay -= 1;
		} else {
			this.lsa.removePropertiesByName("EVENT");
			if (debugLevel > 0) {
				logger.info("after removing event property : startEvent = " + getStartEvent());
			}
		}
	}

	public boolean hasExpired() {
		EnergyFlow supply = getProductionOrNeed();
		if (supply == null) {
			return false;
		}
		return supply.hasExpired();
	}

	public boolean isDisabled() {
		EnergyFlow supply = getProductionOrNeed();
		if (supply == null) {
			return false;
		}
		return supply.getDisabled();
	}

	public boolean isActive() {
		EnergyFlow supply = getProductionOrNeed();
		if (supply == null) {
			return false;
		}
		return supply.isActive();
	}

	public boolean isInActiveSlot(Date aDate) {
		EnergyFlow supply = getProductionOrNeed();
		if (supply == null) {
			return false;
		}
		return supply.isInActiveSlot(aDate);
	}

	public int getTimeLeftSec(boolean addWaitingBeforeStart) {
		EnergyFlow supply = getProductionOrNeed();
		if (supply == null) {
			return 0;
		}
		return supply.getTimeLeftSec(addWaitingBeforeStart);
	}

	public int getId() {
		return id;
	}

	protected void auxReinitializeLSA(int _id, AgentAuthentication _authentication, String[] inputTags, String[] outputTags, NodeContext nodeContext) {
		initCommonFields(_id, _authentication, nodeContext);
		this.reInitializeLsa(inputTags, outputTags, LsaType.Service);
		// Added
		firstDecay = true;
		logger.info(this.agentName + " reinitialize : lsa = " + lsa.toVisualString() + " this=" + this
				+ " memory address =" + this.hashCode());
	}

	public boolean tryReactivation() throws HandlingException {
		boolean result = false;
		Property pDisabled = lsa.getOnePropertyByName("DISABLED");
		if (pDisabled != null && pDisabled.getValue() instanceof RegulationWarning) {
			// Clean expired disabled property
			RegulationWarning warning = (RegulationWarning) pDisabled.getValue();
			EnergyFlow energySupply = getProductionOrNeed();
			if (warning.hasWaitingExpired()) {
				if (WarningType.OVER_CONSUMPTION.equals(warning.getType())
						|| WarningType.OVER_PRODUCTION.equals(warning.getType())) {
					// Check over consumption
					boolean stillOverflow = true;
					if (lastNodeTotal != null) {
						// Check if the request will not generate over-consumption
						if (WarningType.OVER_CONSUMPTION.equals(warning.getType())) {
							logger.info("tryReactivation " + this.agentName + " nodeTotal.getConsumed() = "
									+ lastNodeTotal.getConsumed() + ", agent need = " + energySupply.getPower());
							stillOverflow = (lastNodeTotal.getConsumed() + energySupply.getPower() > RegulatorAgent
									.getMaxPowerConsumption());
						} else if (WarningType.OVER_PRODUCTION.equals(warning.getType())) {
							logger.info("tryReactivation " + this.agentName + " nodeTotal.getProduced() = "
									+ lastNodeTotal.getProduced() + ", agent power = " + energySupply.getPower());
							// stillOverflow = (nodeTotal.getProduced() + (energySupply.getDisabled() ?
							// energySupply.getPower() : 0) > RegulatorAgent.MAX_PRODUCTION);
							stillOverflow = (lastNodeTotal.getProduced() + energySupply.getPower() > RegulatorAgent
									.getMaxPowerProduction());
						}
					}
					if (stillOverflow) {
						// Agent will be still disabled
						warning.setWaitingDeadline(UtilDates.shiftDateMinutes(getCurrentDate(), DISABLED_DURATION_MINUTES));
						replacePropertyWithName(new Property("DISABLED", warning));
					} else {
						// Agent is no longer disabled
						lsa.removePropertiesByName("DISABLED");
						try {
							this.setBeginDate(getCurrentDate());
							this.setDisabled(false);
							generateStartEvent();
						} catch (Exception e) {
							logger.error(e);
						}
						result = true;
					}
				} else if (WarningType.USER_INTERRUPTION.equals(warning.getType())) {
					// Stop the agent
					lsa.removePropertiesByName("DISABLED");
					this.setEndDate(getCurrentDate());
				} /*
					 * else if (WarningType.CHANGE_REQUEST.equals(warning.getType())) { // TODO :
					 * check // Stop the agent ONLY FOR PRODUCER if(this.isProducer()) {
					 * energySupply.setEndDate(getCurrentDate()); } }
					 */
			}
		} else if (waitingStartEvent != null && !isStartInFutur()) {
			waitingStartEvent = null;
			generateStartEvent();
		}
		return result;
	}

	@Override
	public void onSpreadingEvent(SpreadingEvent event) {
	}

	@Override
	public void handleWarning(RegulationWarning warning) throws HandlingException {
		if (!receivedWarnings.contains(warning)) {
			receivedWarnings.add(warning);
			// Over consumption
			if (WarningType.OVER_CONSUMPTION.equals(warning.getType())) {
				handleWarningOverConsumption(warning);
			} else if (WarningType.OVER_PRODUCTION.equals(warning.getType())) {
				handleWarningOverProduction(warning);
			} else if (WarningType.USER_INTERRUPTION.equals(warning.getType())) {
				handleWarningUserInteruption(warning);
			} else if (WarningType.GENERAL_INTERRUPTION.equals(warning.getType())) {
				handleWarningGeneralInteruption(warning);
			} else if (WarningType.CHANGE_REQUEST.equals(warning.getType())) {
				handleWarningChangeRequest(warning);
			}
		}
	}

	@Override
	public void handleWarningGeneralInteruption(RegulationWarning warning) throws HandlingException {
		if(!this.isDisabled()) {
			// Disable agent
			disableAgent(warning);
		}
		logger.info("handleWarningGeneralInteruption  " + this.agentName);
		stopAgent(warning);
	}

	@Override
	public void handleReschedule(RescheduleTable rescheduleTable) throws HandlingException {
		// handle reschedule
		if (rescheduleTable.hasItem(agentName)) {
			RescheduleItem rescheduleItem = rescheduleTable.getItem(agentName);
			EnergyFlow supplyOrRequest = this.getProductionOrNeed();
			if (rescheduleItem.getStopBegin().before(supplyOrRequest.getEndDate())) {
				// Modify contract end date
				this.setBeginDate(getCurrentDate());
				this.setEndDate(rescheduleItem.getStopBegin());
				generateUpdateEvent(rescheduleItem.getWarningType(), "");
			}
		}
	}

	@Override
	public void setInitialLSA() {
		try {
			if(isStartInFutur()) {
				Date beginDate = getProductionOrNeed().getBeginDate();
				logger.warning("setInitialLSA " + this.agentName + " : start date is in the future " + UtilDates.format_date_time.format(beginDate));
				this.waitingStartEvent = this.generateEvent(getStartEventType(), "");
			} else {
				generateStartEvent();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		this.submitOperation();
	}

	public void cleanExpiredWarnings() {
		// Clean expired warnings
		RegulationWarning expiredWarning = null;
		while ((expiredWarning = SapereUtil.getExpiredWarning(receivedWarnings)) != null) {
			receivedWarnings.remove(expiredWarning);
		}
	}

	protected void disableAgent(RegulationWarning warning, String[] lsaPropToRemove) throws HandlingException {
		for (String prop : lsaPropToRemove) {
			if (!lsa.getPropertiesByName(prop).isEmpty()) {
				lsa.removePropertiesByNames(lsaPropToRemove);
			}
		}
		if (lsa.getPropertiesByName("DISABLED").isEmpty()) {
			RegulationWarning warningToSet;
			warningToSet = warning.clone();
			/*
			 * // For consumers if( WarningType.USER_INTERRUPTION.equals(warning.getType())
			 * || WarningType.CHANGE_REQUEST.equals(warning.getType() )) {
			 * warningToSet.setWaitingDeadline(SapereUtil.shiftDateSec(getCurrentDate(),
			 * DISABLED_SHORT_DURATION_SEC)); } else {
			 * warningToSet.setWaitingDeadline(SapereUtil.shiftDateMinutes(getCurrentDate(),
			 * DISABLED_DURATION_MINUTES)); }
			 * warningToSet.setReceptionDeadline(SapereUtil.shiftDateMinutes(getCurrentDate(),
			 * DISABLED_DURATION_MINUTES));
			 */
			addProperty(new Property("DISABLED", warningToSet));
		}
		EnergyEvent stopEvent = getStopEvent();
		if (stopEvent == null) {
			stopEvent = generateStopEvent(warning, "");
			// eventToPost = stopEvent.clone();
			logger.info(this.agentName + " Request interuption 1 : unset startEvent");
		}
	}

	/**
	 * Check if the agent LSA will expire : int that case, create an expiry event
	 * 
	 * @param event
	 * @return
	 */
	protected boolean checkAgentExpiration(DecayedEvent event) {
		Lsa decayedLsa = event.getLsa();
		boolean hasExpired = this.hasExpired();
		Integer decay = Integer.valueOf("" + decayedLsa.getSyntheticProperty(SyntheticPropertyName.DECAY));
		boolean toLog = "__Prod_1".equals(agentName);
		if (debugLevel > 0 || toLog) {
			logger.info("checkAgentExpiration: " + this.agentName + " decay = " + decay + " Time left sec = "
					+ getTimeLeftSec(true) + " instance = " + this);
		}
		if (hasExpired) {
			if (decay > 1) {
				// the agent should expire now : force decay
				logger.warning(this.agentName + " will expire");
				this.addDecay(1);
			}
			EnergyEvent expiryEvent = getExpiryEvent();
			// Post expiration event
			if (expiryEvent == null) {
				try {
					EnergyEvent startEvent = getStartEvent();
					if (startEvent != null) {
						expiryEvent = generateExpiryEvent();
					}
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
		return hasExpired;
	}

	public void logLsaProperties() {
		logger.info(" ------ LSA properties of " + this.agentName + ": ------ ");
		for (Property prop : this.lsa.getProperties()) {
			logger.info(prop.getName() + " (Q:" + prop.getQuery() + ")" + " : " + prop.getValue());
		}
		logger.info(" ------ End LSA properties : ------ ");
	}

	public boolean hasHighWarningDuration() {
		if (lastNodeTotal != null) {
			long maxDuration = lastNodeTotal.getMaxWarningDuration();
			return (maxDuration >= ConsumersProcessingMangager.WARNING_DURATION_THRESHOLD);
		}
		return false;
	}

	public void stopAgent(RegulationWarning warning) throws HandlingException {
		if(!this.hasExpired()) {
			Date currentDate = getCurrentDate();
			this.setEndDate(currentDate);
			/*
			if(this.getGlobalProduction() != null) {
				getGlobalProduction().setEndDate(getCurrentDate());
			}
			if(this.getGlobalNeed() != null) {
				getGlobalNeed().setEndDate(getCurrentDate());
			}*/
		}
		EnergyEvent stopEvent = getStopEvent();
		if(stopEvent == null) {
			stopEvent = generateStopEvent(warning, "");
		}
	}

	public String getLocation() {
		return this.getAuthentication().getNodeLocation().getMainServiceAddress();
	}

	public String getNodeName() {
		return lsa.getAgentAuthentication().getNodeLocation().getName();
	}


	@Override
	public EnergyStorageSetting getStorageSetting() {
		if(storage != null) {
			return storage.getSetting();
		}
		return null;
	}

	@Override
	public Double getStoredWH() {
		if(storage != null) {
			return storage.computeBalanceSavedWH();
		}
		return 0.0;
	}

	public ProtectedConfirmationTable getProtectedConfirmationTable(EnergyAgent producerAgent) {
		Property pProdConfirm = producerAgent.getLsa().getOnePropertyByName("CONTRACT_CONFIRM");
		if (pProdConfirm != null && pProdConfirm.getValue() instanceof ProtectedConfirmationTable) {
			ProtectedConfirmationTable pConfirmationTable = (ProtectedConfirmationTable) pProdConfirm.getValue();
			return pConfirmationTable;
		}
		ConfirmationTable confirmationTable = new ConfirmationTable(producerAgent.getAgentName(), getTimeShiftMS());
		return new ProtectedConfirmationTable(confirmationTable);
	}

}
