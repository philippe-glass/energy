package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.manager.ConsumersProcessingMangager;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.DoublonException;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyEventTable;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.RescheduleItem;
import com.sapereapi.model.energy.RescheduleTable;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeConfig;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;

public abstract class EnergyAgent extends MicroGridAgent implements IEnergyAgent {
	protected int id;
	protected EnergyEvent waitingStartEvent = null;
	protected EnergyEvent startEvent = null;
	protected EnergyEvent expiryEvent = null;
	protected EnergyEvent stopEvent = null;
	private static final long serialVersionUID = 1L;
	protected List<Property> propertiesToPost = new ArrayList<Property>();
	protected boolean firstDecay = true;
	protected int eventDecay = 0;
	protected List<RegulationWarning> receivedWarnings = new ArrayList<RegulationWarning>();
	protected Map<String, Lsa> tableChosenLsa = null;
	protected NodeConfig nodeConfig = NodeManager.getNodeConfig();
	protected String[] lsaInputTags;
	protected String[] lsaOutputTags;
	public final static int DISABLED_DURATION_MINUTES = 5;
	public final static int DISABLED_SHORT_DURATION_SEC = 5;
	protected NodeTotal lastNodeTotal = null;
	protected Date timeLastDecay = null;
	protected long timeShiftMS = 0;

	public EnergyAgent(int _id, String name, AgentAuthentication authentication, String[] _lsaInputTags,
			String[] _lsaOutputTags, EnergySupply energySupply) {
		super(name, authentication, _lsaInputTags, _lsaOutputTags);
		initCommonFields(_id, authentication, energySupply, _lsaInputTags, _lsaOutputTags);
	}

	public void initCommonFields(int _id, AgentAuthentication _authentication, EnergySupply _energySupply,
			String[] _lsaInputTags, String[] _lsaOutputTags) {
		setEpsilon(0); // No greedy policy
		super.initSGAgent(this.agentName, _authentication, _lsaInputTags, _lsaOutputTags);
		id = _id;
		this.lsaInputTags = _lsaInputTags;
		this.lsaOutputTags = _lsaOutputTags;
		this.tableChosenLsa = new HashMap<String, Lsa>();
		this.debugLevel = 0;
		this.timeShiftMS = _energySupply.getTimeShiftMS();
		// logger.info("ProducerAgent : lsa = " + lsa.toVisualString());
	}

	public EnergyEvent getStartEvent() {
		return startEvent;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public boolean isStartInFutur() {
		EnergySupply supply = getEnergySupply();
		return supply.isStartInFutur();
	}

	public EnergyEvent generateStartEvent() {
		EnergySupply supply = getEnergySupply();
		EventType eventType = getStartEventType();
		if(supply.isStartInFutur()) {
			startEvent = null;
		} else {
			try {
				EnergyEvent newEvent = new EnergyEvent(eventType, supply, "");
				startEvent = EnergyDbHelper.registerEvent(newEvent);
			} catch (DoublonException e) {
				startEvent = EnergyDbHelper.retrieveEvent(eventType, agentName, false, supply.getBeginDate());
			}
			waitingStartEvent = null;
			if (startEvent.getId() != null) {
				this.getEnergySupply().setEventId(startEvent.getId());
			}
		}
		// Remove other events
		stopEvent = null;
		expiryEvent = null;
		// Post event in LSA
		postEvent(startEvent);
		return startEvent;
	}

	public EnergyEvent generateUpdateEvent(WarningType warningType) {
		EnergySupply supply = getEnergySupply();
		EventType eventType = getUpdateEventType();
		if (startEvent != null) {
			EnergyEvent newEvent = new EnergyEvent(eventType, supply, "");
			newEvent.setWarningType(warningType);
			EnergyEvent originStartEvent = startEvent.clone();
			newEvent.setOriginEvent(originStartEvent);
			PowerSlot powerSlotBefore = originStartEvent.getPowerSlot();
			PowerSlot powerUpdate = newEvent.getPowerSlot().clone();
			powerUpdate.substract(powerSlotBefore);
			newEvent.setPowerUpateSlot(powerUpdate);
			startEvent = EnergyDbHelper.registerEvent2(newEvent);
			// Remove other events
			stopEvent = null;
			expiryEvent = null;
			// Post event in LSA
			postEvent(startEvent);
		}
		return startEvent;
	}

	public EnergyEvent generateExpiryEvent() {
		EnergySupply supply = getEnergySupply();
		EventType eventType = getExpiryEventType();
		Date expiryDate = supply.getEndDate();
		expiryEvent = new EnergyEvent(eventType, supply, "");
		expiryEvent.setBeginDate(expiryDate);
		expiryEvent.setEndDate(expiryDate);
		if (startEvent != null) {
			expiryEvent.setOriginEvent(this.startEvent.clone());
		}
		expiryEvent = EnergyDbHelper.registerEvent2(expiryEvent);
		// Remove other events
		startEvent = null;
		stopEvent = null;
		// Post event in LSA
		postEvent(expiryEvent);
		return expiryEvent;
	}

	public EnergyEvent generateStopEvent(RegulationWarning warning, String log) {
		Date timeStop = warning.getDate();
		// Caution : on ProducerAgent implementation : Date timeStop =
		// SapereUtil.getCurrentSeconde();
		if (this instanceof ProducerAgent) {
			// timeStop = SapereUtil.getCurrentSeconde();
		}
		EnergySupply supply = getEnergySupply();
		EventType eventType = getStopEventType();
		stopEvent = new EnergyEvent(eventType, supply, log);
		stopEvent.setBeginDate(timeStop);
		stopEvent.setEndDate(timeStop);
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
		postEvent(stopEvent);
		return stopEvent;
	}

	public EnergyEventTable getEventTable() {
		Property pEvent = this.lsa.getOnePropertyByName("EVENT");
		if (pEvent != null && pEvent.getValue() instanceof EnergyEventTable) {
			return (EnergyEventTable) pEvent.getValue();
		}
		return null;
	}

	public void postEvent(EnergyEvent eventToPost) {
		EnergyEventTable energyEventTable = getEventTable();
		if (energyEventTable == null) {
			energyEventTable = new EnergyEventTable();
		}
		try {
			energyEventTable.putEvent(eventToPost.clone());
			this.lsa.removePropertiesByName("EVENT");
			addProperty(new Property("EVENT", energyEventTable));
			eventDecay = SapereUtil.EVENT_INIT_DECAY;
			firstDecay = true;
		} catch (Exception e) {
			logger.error(e);
		}
	}


	public void addProperty(Property propertyToAdd) {
		if (lsa.getProperties().size() >= Lsa.PROPERTIESSIZE) {
			propertiesToPost.add(propertyToAdd);
			logger.info(this.agentName + " addProperty : cannot post propertiesToPost " + propertyToAdd.getValue()
					+ " in lsa. Put it waiting queue.");
		} else {
			lsa.addProperty(propertyToAdd);
		}
	}

	protected void sendProperties() {
		while (this.propertiesToPost.size() > 0 && lsa.getProperties().size() < Lsa.PROPERTIESSIZE - 1) {
			Property prop = propertiesToPost.remove(0);
			logger.info(this.agentName + " : post not sent property " + prop);
			lsa.addProperty(prop);
		}
	}

	public EnergyEvent getStopEvent() {
		return stopEvent;
	}

	public void cleanEventProperties() {
		if (eventDecay > 0) {
			eventDecay -= 1;
		} else {
			this.lsa.removePropertiesByName("EVENT");
			if (debugLevel > 0) {
				logger.info("after removing event property : startEvent = " + startEvent);
			}
		}
	}

	protected boolean hasWantedProperty(Lsa bondedLsa) {
		for (String nextWaiting : getInput()) {
			if (!bondedLsa.getPropertiesByQueryAndName(agentName, nextWaiting).isEmpty()) {
				return true;
			}
			if (!bondedLsa.getPropertiesByName(nextWaiting).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasExpired() {
		EnergySupply supply = getEnergySupply();
		if (supply == null) {
			return false;
		}
		return supply.hasExpired();
	}

	public boolean isDisabled() {
		EnergySupply supply = getEnergySupply();
		if (supply == null) {
			return false;
		}
		return supply.getDisabled();
	}

	public boolean isActive() {
		EnergySupply supply = getEnergySupply();
		if (supply == null) {
			return false;
		}
		return supply.isActive();
	}

	public boolean isInActiveSlot(Date aDate) {
		EnergySupply supply = getEnergySupply();
		if (supply == null) {
			return false;
		}
		return supply.isInActiveSlot(aDate);
	}

	public int getTimeLeftSec(boolean addWaitingBeforeStart) {
		EnergySupply supply = getEnergySupply();
		if (supply == null) {
			return 0;
		}
		return supply.getTimeLeftSec(addWaitingBeforeStart);
	}

	public int getId() {
		return id;
	}

	public void reinitialize(int _id, AgentAuthentication _authentication, EnergySupply _globalSupply) {
		initCommonFields(_id, _authentication, _globalSupply, lsaInputTags, lsaOutputTags);
		this.lsa.removeAllProperties();
		this.lsa.setAgentName("");
		// Added
		firstDecay = true;
		lsa.removeSyntheticProperty(SyntheticPropertyName.LOCATION);
		lsa.removeSyntheticProperty(SyntheticPropertyName.BOND);
		lsa.removeSyntheticProperty(SyntheticPropertyName.DIFFUSE);
		lsa.removeSyntheticProperty(SyntheticPropertyName.QUERY);
		lsa.removeSyntheticProperty(SyntheticPropertyName.SOURCE);
		lsa.removeSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP);
		this.authentication = _authentication;
		logger.info(this.agentName + " reinitialize : lsa = " + lsa.toVisualString() + " this=" + this
				+ " memory address =" + this.hashCode());
	}

	public boolean tryReactivation() {
		boolean result = false;
		Property pDisabled = lsa.getOnePropertyByName("DISABLED");
		if (pDisabled != null && pDisabled.getValue() instanceof RegulationWarning) {
			// Clean expired disabled property
			RegulationWarning warning = (RegulationWarning) pDisabled.getValue();
			EnergySupply energySupply = getEnergySupply();
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
						lsa.removePropertiesByName("DISABLED");
						lsa.addProperty(new Property("DISABLED", warning));
					} else {
						// Agent is no longer disabled
						lsa.removePropertiesByName("DISABLED");
						try {
							energySupply.setBeginDate(getCurrentDate());
							energySupply.setDisabled(false);
							startEvent = generateStartEvent();
						} catch (Exception e) {
							logger.error(e);
						}
						result = true;
					}
				} else if (WarningType.USER_INTERRUPTION.equals(warning.getType())) {
					// Stop the agent
					lsa.removePropertiesByName("DISABLED");
					energySupply.setEndDate(getCurrentDate());
				} /*
					 * else if (WarningType.CHANGE_REQUEST.equals(warning.getType())) { // TODO :
					 * check // Stop the agent ONLY FOR PRODUCER if(this.isProducer()) {
					 * energySupply.setEndDate(getCurrentDate()); } }
					 */
			}
		} else if (waitingStartEvent != null && !isStartInFutur()) {
			waitingStartEvent = null;
			startEvent = generateStartEvent();
		}
		return result;
	}

	@Override
	public void onPropagationEvent(PropagationEvent event) {
		// logger.info("onPropagationEvent " + location + " " + this.agentName + " " +
		// event);
		Lsa eventLsa = event.getLsa();
		String source = eventLsa.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString();
		if (!"".equals(source) && !nodeConfig.getMainServiceAddress().equals(source)) {
			logger.info("onPropagationEvent : source = " + source + " , localIpPort = " + nodeConfig.getMainServiceAddress());
		}
		if (eventLsa.getAgentName().contains("*")) {
			logger.info("onPropagationEvent : eventLsa.getAgentName() = " + eventLsa.getAgentName());
		}
	}

	@Override
	public void handleWarning(RegulationWarning warning) {
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
	public void handleWarningGeneralInteruption(RegulationWarning warning) {
		if(!this.isDisabled()) {
			// Disable agent
			disableAgent(warning);
		}
		logger.info("handleWarningGeneralInteruption  " + this.agentName);
		stopAgent(warning);
	}

	@Override
	public void handleReschedule(RescheduleTable rescheduleTable) {
		// handle reschedule
		if (rescheduleTable.hasItem(agentName)) {
			RescheduleItem rescheduleItem = rescheduleTable.getItem(agentName);
			EnergySupply supply = this.getEnergySupply();
			if (rescheduleItem.getStopBegin().before(supply.getEndDate())) {
				// Modify contract end date
				supply.setBeginDate(getCurrentDate());
				supply.setEndDate(rescheduleItem.getStopBegin());
				startEvent = generateUpdateEvent(rescheduleItem.getWarningType());
			}
		}
	}

	@Override
	public void setInitialLSA() {
		try {
			if(isStartInFutur()) {
				logger.warning("setInitialLSA " + this.agentName + " : start date is in the future " + UtilDates.format_date_time.format(getEnergySupply().getBeginDate()));
				this.waitingStartEvent =  new EnergyEvent( getStartEventType(), getEnergySupply(), "");
			} else {
				this.startEvent = generateStartEvent();
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

	protected void completeOutputPropertyIfNeeded() {
		String sOutputProperty = "" + lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT);
		for (String nextOutput : this.getOutput())
			if (!sOutputProperty.contains(nextOutput)) {
				sOutputProperty = sOutputProperty + "," + nextOutput;
			}
		lsa.addSyntheticProperty(SyntheticPropertyName.OUTPUT, sOutputProperty);
	}

	protected void disableAgent(RegulationWarning warning, String[] lsaPropToRemove) {
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
			lsa.addProperty(new Property("DISABLED", warningToSet));
		}
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
			// Post expiration event
			if (expiryEvent == null) {
				try {
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

	public boolean containsChosenLsa(String agentName) {
		if (tableChosenLsa == null) {
			return false;
		}
		return tableChosenLsa.containsKey(agentName);
	}

	public Lsa getChosenLsa(String agentName) {
		if (!containsChosenLsa(agentName)) {
			return null;
		}
		return tableChosenLsa.get(agentName);
	}

	public void setPropertyChosen(String agentName, String propName) {
		if (this.containsChosenLsa(agentName)) {
			Lsa chosenLSA = this.getChosenLsa(agentName);
			List<Property> listProp = chosenLSA.getPropertiesByName(propName);
			int indx = rand.nextInt(listProp.size());
			listProp.get(indx).setChosen(true); // one Lsa can contain many property for same query
		}
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

	public void stopAgent(RegulationWarning warning) {
		if(!this.hasExpired()) {
			getEnergySupply().setEndDate(getCurrentDate());
		}
		if(stopEvent == null) {
			stopEvent = generateStopEvent(warning, "");
		}
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}

	public String getLocation() {
		return this.authentication.getNodeLocation().getMainServiceAddress();
	}

	public String getNodeName() {
		return this.authentication.getNodeLocation().getName();
	}

}
