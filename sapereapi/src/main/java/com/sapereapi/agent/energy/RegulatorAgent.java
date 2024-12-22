package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.ChangeRequest;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.ProsumerItem;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.award.AwardsComputingData;
import com.sapereapi.model.energy.award.AwardsTable;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.reschedule.RescheduleItem;
import com.sapereapi.model.energy.reschedule.RescheduleTable;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.referential.AgentType;
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
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;

public class RegulatorAgent extends SupervisionAgent implements ISupervisorAgent {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public final static int REFRESH_PERIOD_SEC = 30;
	private static Double maxPowerConsumption = null;
	private static Double maxPowerProduction = null;
	private Map<String,RegulationWarning> warningsNotInSpace = new HashMap<String,RegulationWarning>();
	private int logSubScriptionDecay = 0;
	private PredictionData lastPrediction = null;
	private boolean activateReschedule = true;
	private AwardsComputingData awardsComputingData = null;	// monitoring for awards
	private Date lastHistoDate = null;

	private static final Comparator<EnergySupply> supplyComparator = new Comparator<EnergySupply>() {
		public int compare(EnergySupply supply1, EnergySupply supply2) {
			int comaprePower = supply1.comparePower(supply2);
			if (comaprePower == 0) {
				return supply1.comparTimeLeft(supply2);
			} else {
				return comaprePower;
			}
		}
	};

	public static Double getMaxPowerConsumption() {
		return maxPowerConsumption;
	}

	public static Double getMaxPowerProduction() {
		return maxPowerProduction;
	}

	public RegulatorAgent(String agentName,  AgentAuthentication authentication, NodeContext nodeContext) {
		super(agentName, authentication
				, new String[] {"PRED"}
				, getOutputTags(nodeContext)
				, nodeContext);
		maxPowerConsumption = nodeContext.getMaxTotalPower();
		maxPowerProduction = nodeContext.getMaxTotalPower();
		this.addDecay(REFRESH_PERIOD_SEC);
		debugLevel = 0;
		warningsNotInSpace = new HashMap<String,RegulationWarning>();
		// forcedCurrentTime = (ALL_TIME_WINDOWS.get(18)).getStartDate();

	}

	private static String[] getOutputTags(NodeContext nodeContext) {
		List<String> outputTags = new ArrayList<String>();
		// common tags :
		String[] commonTags =  { "EVENT", "DISABLED" };
		outputTags.addAll(Arrays.asList(commonTags));
		if(nodeContext.isAwardsActivated()) {
			outputTags.add("AWARDS");
		}
		return SapereUtil.toStrArray(outputTags);
	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

	@Override
	public void onBondNotification(BondEvent event) {
		try {
			Lsa bondedLsa = event.getBondedLsa();
			lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
			Property pPrediction = bondedLsa.getOnePropertyByName("PRED");
			if (pPrediction != null && pPrediction.getValue() instanceof PredictionData) {
				PredictionData prediction = (PredictionData) pPrediction.getValue();
				if(prediction.hasLastResult("produced")) {
					AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
					if (AgentType.LEARNING_AGENT.equals(bondAgentType)) {
						if(false || lastPrediction==null || UtilDates.shiftDateMinutes(lastPrediction.getInitialDate(),0).before(prediction.getInitialDate())) {
							lastPrediction = prediction;
							int stateNb = NodeStates.getNbOfStates();
							VariableState randomState = prediction.getLastRandomTargetState("produced");
							if(randomState!=null && randomState.getId() == stateNb) {
								// Over production
								logger.info("Prediction of overProduction");
								activateReschedule = false;
								RescheduleTable rescheduleTable = getRescheduleTable();
								double rescheduledPower = rescheduleTable.computeRescheduledPower(prediction.getLastTargetDate(), WarningType.OVER_PRODUCTION_FORCAST);
								if(activateReschedule && rescheduledPower < 0.2*nodeContext.getMaxTotalPower()) {
									//double horizonInMin = prediction.getTimeHorizonMinutes();
									Date stopBegin = UtilDates.shiftDateMinutes(prediction.getLastTargetDate(), -10);
									Date stopEnd = UtilDates.shiftDateMinutes(prediction.getLastTargetDate(), 50);
									List<EnergySupply> listSupplies = retrieveListSupplies(stopBegin, stopEnd);
									if (listSupplies.size() > 0) {
										Collections.sort(listSupplies, supplyComparator);
										int supplyIdx = 0;
										while (rescheduledPower < 0.2*nodeContext.getMaxTotalPower() && supplyIdx < listSupplies.size()) {
											EnergySupply nextSupply = listSupplies.get(supplyIdx);
											String producer = nextSupply.getIssuer();
											RescheduleItem recheduleItem = new RescheduleItem(producer, WarningType.OVER_PRODUCTION_FORCAST, stopBegin, stopEnd, nextSupply.getPower(), nodeContext.getTimeShiftMS());
											rescheduleTable.addItem(producer, recheduleItem);
											rescheduledPower = rescheduleTable.computeRescheduledPower(prediction.getLastTargetDate(), WarningType.OVER_PRODUCTION_FORCAST);
											supplyIdx++;
										}
										// Update rescheduleTable in LSA
										replacePropertyWithName(new Property("RESCHEDULE", rescheduleTable));
									}
								}
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			logger.error(t);
		}
	}

	private List<EnergySupply> retrieveListConsumption() throws HandlingException {
		List<EnergySupply> result = new ArrayList<EnergySupply>();
		Date currentDate = getCurrentDate();
		for (ExtendedEnergyEvent nextEvent : EnergyDbHelper.retrieveCurrentSessionEvents(currentDate)) {
			EventType eventType = nextEvent.getType();
			if(EventObjectType.CONTRACT.equals(eventType.getObjectType()) &&  !eventType.getIsEnding()) {
				ProsumerItem linkedConsumer = nextEvent.getLinkedConsumer();
				if(linkedConsumer != null) {
					PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
					EnergySupply supply = new EnergySupply(nextEvent.getIssuerProperties(), nextEvent.isComplementary(),
							nextEvent.getPowerSlot(), nextEvent.getBeginDate(), nextEvent.getEndDate(), pricingTable);
					result.add(supply);
				}
			}
		}
		return result;
	}

	private List<EnergySupply> retrieveListSupplies() throws HandlingException {
		List<EnergySupply> result = new ArrayList<EnergySupply>();
		Date currentDate = getCurrentDate();
		for (EnergyEvent nextEvent : EnergyDbHelper.retrieveCurrentSessionEvents(currentDate)) {
			EventType evtType = nextEvent.getType();
			if (EventObjectType.PRODUCTION.equals(evtType.getObjectType()) && !evtType.getIsEnding()) {
				ProsumerProperties supplierProperties = nextEvent.getIssuerProperties();
				PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
				EnergySupply supply = new EnergySupply(supplierProperties, nextEvent.getIsComplementary(),
						nextEvent.getPowerSlot(), nextEvent.getBeginDate(), nextEvent.getEndDate(), pricingTable);
				result.add(supply);
			}
		}
		return result;
	}
	private List<EnergySupply> retrieveListSupplies(Date stopBegin, Date stopEnd) throws HandlingException {
		List<EnergySupply> allSupplies = retrieveListSupplies();
		List<EnergySupply> listsupplies = new ArrayList<EnergySupply>();
		for(EnergySupply nextSupply : allSupplies) {
			if(		nextSupply.getEndDate().after(stopBegin)
				 && nextSupply.getBeginDate().before(stopBegin)) {
				listsupplies.add(nextSupply);
			}
		}
		return listsupplies;
	}

	private boolean computeAwards(NodeTotal nodeTotal) throws HandlingException {
		boolean result = false;
		Date current = getCurrentDate();
		Date currentDate = getCurrentDate();
		List<ExtendedEnergyEvent> currentEvents = EnergyDbHelper.retrieveCurrentSessionEvents(currentDate);
		Map<String, Double> offeredByProducer = EnergyDbHelper.retrieveProposedPower(currentDate);
		if(awardsComputingData == null) {
			awardsComputingData = new AwardsComputingData(nodeContext.getTimeShiftMS());
		}
		boolean retrieveCtrResponseTimes = false;
		if(retrieveCtrResponseTimes) {
			Map<String, Map<String, Map<Long, Double>>> responseTimeTable = EnergyDbHelper.retrieveContractResponseTimes();
			logger.info("computeAwards : responseTimeTable = " + responseTimeTable);
		}
		boolean hasAggregated = awardsComputingData.updateRecentHistory(current, currentEvents, offeredByProducer, logger);
		replacePropertyWithName(new Property("_AWARDS_COMPUTING_", awardsComputingData));
		if (hasAggregated && awardsComputingData.hasEmptyDeadlineExpired()) {
			AwardsTable awardsTable = awardsComputingData.generateAwardsTable(true, logger);
			logger.info("computeAwards : after generateAwardsTable : awardsTable = " + awardsTable);
			replacePropertyWithName(new Property("AWARDS", awardsTable));
			// awardsComputingData.clearData();
		}
		return result;
	}

	private boolean checkOverConsumption(NodeTotal nodeTotal) throws HandlingException {
		boolean overConsumption = false;
		boolean addWarning = false;
		if (nodeTotal != null) {
			if (debugLevel > 0) {
				logger.info("consumed = " + nodeTotal.getConsumed());
			}
			if (getMapWarnings(WarningType.OVER_CONSUMPTION).size() > 0) {
				return addWarning;
			}
			overConsumption = (nodeTotal.getConsumed() > maxPowerConsumption);
			if (overConsumption) {
				List<EnergySupply> consumptionList = retrieveListConsumption();
				if (consumptionList.size() > 0) {
					Collections.sort(consumptionList, Collections.reverseOrder(supplyComparator));
					RegulationWarning warning = new RegulationWarning(WarningType.OVER_CONSUMPTION,
							nodeTotal.getDate(), nodeContext.getTimeShiftMS());
					double consumptionToWarn = 0;
					int consumptionIdx = 0;
					while (nodeTotal.getConsumed() - consumptionToWarn > maxPowerConsumption) {
						EnergySupply nextConsumption = consumptionList.get(consumptionIdx);
						String consumer = nextConsumption.getIssuer();
						warning.addAgent(consumer);
						consumptionToWarn = consumptionToWarn + nextConsumption.getPower();
					}
					for (String consumer : warning.getDestinationAgents()) {
						addProperty(new Property("WARNING", warning, consumer, consumer, "", getIpSource(), false));
						addWarning = true;
					}
				}
			}
		}
		return addWarning;
	}

	private boolean checkOverProduction(NodeTotal nodeTotal) throws HandlingException {
		boolean addWarning = false;
		if (nodeTotal != null) {
			if (debugLevel > 0) {
				logger.info("produced = " + nodeTotal.getProduced());
			}
			if(getMapWarnings().size()>0) {
				logger.info("checkOverProduction For debug : getMapWarnings().size() = " + getMapWarnings().size());
			}
			if (getMapWarnings(WarningType.OVER_PRODUCTION).size() > 0) {
				return addWarning;
			}
			double coeff = 1.0;
			//coeff = 0.555;
			boolean overProduction = (nodeTotal.getProduced() > coeff*maxPowerProduction);
			if (overProduction) {
				List<EnergySupply> allSupplies = retrieveListSupplies();
				if (allSupplies.size() > 0) {
					Collections.sort(allSupplies, Collections.reverseOrder(supplyComparator));
					RegulationWarning warning = new RegulationWarning(WarningType.OVER_PRODUCTION, nodeTotal.getDate(), nodeContext.getTimeShiftMS());
					double productionToWarn = 0;
					int supplyIdx = 0;
					while (nodeTotal.getProduced() - productionToWarn > coeff*maxPowerProduction) {
						EnergySupply nextSupply = allSupplies.get(supplyIdx);
						String producer = nextSupply.getIssuer();
						warning.addAgent(producer);
						productionToWarn = productionToWarn + nextSupply.getPower();
					}
					for (String producer : warning.getDestinationAgents()) {
						addProperty(new Property("WARNING", warning, producer, producer, "", getIpSource(), false));
						addWarning = true;
					}
				}
			}
		}
		return addWarning;
	}


	@Override
	public void onDecayedNotification(DecayedEvent event) {
		try {
			if(stopped) {
				warningsNotInSpace.clear();
				waitingProperties.clear();
				this.addDecay(0);
			} else {
				Lsa decayedLsa = event.getLsa();
				// logger.info("onDecayedNotification: decayedLsa = " +
				// decayedLsa.toVisualString());
				Integer decay = Integer.valueOf("" + decayedLsa.getSyntheticProperty(SyntheticPropertyName.DECAY));
				if (decay < 1) {
					this.addDecay(REFRESH_PERIOD_SEC);
				}
				// lsa.removePropertiesByName("WARNING");
				cleanExpiredData();
				NodeTotal nodeTotal = EnergyDbHelper.retrieveLastNodeTotal();
				if(!nodeContext.isSupervisionDisabled()) {
					boolean newUpdates = lastHistoDate == null || nodeTotal.getDate().after(lastHistoDate) || true;				
					if(newUpdates) {
						checkOverConsumption(nodeTotal);
						checkOverProduction(nodeTotal);
						if(nodeContext.isAwardsActivated()) {
							computeAwards(nodeTotal);
						}
					} else {
						logger.info("onDecayedNotification : " + agentName + " no recent update");
					}
					if(nodeTotal != null) {
						lastHistoDate = nodeTotal.getDate();
					}
				}
				warningsNotInSpace = checkupNotInSpace(warningsNotInSpace);
				// Sumbmit waiting properties
				checkWaitingProperties();
				// log notifier subscriptions
				if(logSubScriptionDecay<=0) {
					int nbSubscriptions = NodeManager.instance().getNotifier().getNbSubscriptions();
					//Map<String, Integer> subsciptionsByAgent = NodeManager.instance().getNotifier().getNbSubscriptionsByAgent();
					logger.info(this.agentName + " total subscriptions : " + nbSubscriptions);
								//+ " \n, subsciptions by agent : " +  subsciptionsByAgent);
					logSubScriptionDecay = 30;
				} else {
					logSubScriptionDecay--;
				}
			}
		} catch (Throwable e) {
			logger.error(e);
			logger.error("Exception throw in onDecayedNotification :" + agentName + " " + event + " "
					+ e.getLocalizedMessage());

		}
	}

	Map<String, RegulationWarning> getMapWarnings() {
		return getMapWarnings(null);
	}

	Map<String, RegulationWarning> getMapWarnings(WarningType typeFilter) {
		Map<String, RegulationWarning> result = new HashMap<String, RegulationWarning>();
		for (Property property : lsa.getProperties()) {
			if (property.getValue() instanceof RegulationWarning) {
				RegulationWarning warning = (RegulationWarning) property.getValue();
				if(typeFilter==null || typeFilter.equals(warning.getType())) {
					result.put(property.getQuery(), (RegulationWarning) property.getValue());
				}
			}
		}
		return result;
	}

	private String getExpiredWarningKey(Map<String, RegulationWarning> mapWarnings) {
		for (String agentName : mapWarnings.keySet()) {
			RegulationWarning warning = mapWarnings.get(agentName);
			if (warning.hasReceptionExpired()) {
				return agentName;
			}
		}
		return null;
	}

	private boolean hasAlreadyWarningType (WarningType warningType) {
		List<Property> props = lsa.getPropertiesByName("WARNING");
		for(Property nextProp : props) {
			if(nextProp.getValue() instanceof RegulationWarning) {
				RegulationWarning warning = (RegulationWarning) nextProp.getValue();
				if(warningType.equals(warning.getType())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasAlreadyWarningType (String agentName, WarningType warningType) {
		List<Property> props = lsa.getPropertiesByQueryAndName(agentName, "WARNING");
		for(Property nextProp : props) {
			if(nextProp.getValue() instanceof RegulationWarning) {
				RegulationWarning warning = (RegulationWarning) nextProp.getValue();
				if(warningType.equals(warning.getType()) && warning.hasAgent(agentName)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isAlreadyInterruupted(String agentName) {
		return hasAlreadyWarningType(agentName, WarningType.USER_INTERRUPTION);
	}

	private boolean hasAlreadyChangeRequest(String agentName) {
		return hasAlreadyWarningType(agentName, WarningType.CHANGE_REQUEST);
	}

	public void interruptAllAgents(List<String> listRunningAgents) {
		listRunningAgents.remove(this.agentName);
		if(listRunningAgents.size() > 0) {
			if(!hasAlreadyWarningType(WarningType.GENERAL_INTERRUPTION)) {
				this.lsa.removeAllProperties();
				RegulationWarning warning = new RegulationWarning(WarningType.GENERAL_INTERRUPTION, getCurrentDate(), nodeContext.getTimeShiftMS());
				//warning.addAgent();
				//addProperty(new Property("WARNING", warning));
				addProperty(new Property("WARNING", warning, this.agentName, this.agentName, "", getIpSource(), false));
			}
		} else {
			this.stopAgent();
		}
	}

	public void interruptListAgents(List<String> listRunningAgents) {
		if(listRunningAgents.size() > 0) {
			String firstAgent = listRunningAgents.get(0);
			if(!isAlreadyInterruupted(firstAgent)) {
				RegulationWarning warning = new RegulationWarning(WarningType.USER_INTERRUPTION, getCurrentDate(), nodeContext.getTimeShiftMS());
				for(String agent : listRunningAgents) {
					warning.addAgent(agent);
				}
				addProperty(new Property("WARNING", warning, this.agentName, this.agentName, "", getIpSource(), false));
			}
		}
	}

	public void interruptAgent(String agentName) {
		if(!isAlreadyInterruupted(agentName)) {
			RegulationWarning warning = new RegulationWarning(WarningType.USER_INTERRUPTION, getCurrentDate(), nodeContext.getTimeShiftMS());
			warning.addAgent(agentName);
			addProperty(new Property("WARNING", warning, agentName, agentName, "", getIpSource(), false));
		}
	}

	public void modifyAgent(ChangeRequest changeRequest) {
		if(hasAlreadyChangeRequest(changeRequest.getAgentName())) {
			logger.info("modifyAgent : already change request on " + changeRequest.getAgentName());
		} else if(Sapere.getInstance().isAgentStopped(changeRequest.getAgentName())) {
			logger.info("modifyAgent : agent " + changeRequest.getAgentName() + " is stopped");
		} else {
			RegulationWarning warning = new RegulationWarning(WarningType.CHANGE_REQUEST, getCurrentDate(), nodeContext.getTimeShiftMS());
			changeRequest.getSupply().checkBeginNotPassed();
			warning.setChangeRequest(changeRequest);
			warning.addAgent(changeRequest.getAgentName());
			addProperty(new Property("WARNING", warning, changeRequest.getAgentName(), changeRequest.getAgentName(), "", getIpSource(), false));
		}
	}

	public Map<String,RegulationWarning> checkupNotInSpace(Map<String,RegulationWarning> lastWarnings) {
		Map<String,RegulationWarning>  result = new HashMap<String, RegulationWarning>();
		List<String> agentNotInSpace = Sapere.getInstance().checkupNotInSpace();
		for(String nextAgentName : agentNotInSpace) {
			if(lastWarnings.containsKey(nextAgentName)) {
				RegulationWarning notInSapce = lastWarnings.get(nextAgentName);
				logger.warning(this.agentName + " checkupNotInSpace agent not in space " + notInSapce );
				if(notInSapce.hasWaitingExpired()) {
					logger.warning(this.agentName + " checkupNotInSpace : stop agent " + nextAgentName);
					Sapere.getInstance().auxStopAgent(nextAgentName, notInSapce);
				} else {
					result.put(nextAgentName, notInSapce);
				}
			} else {
				RegulationWarning notInSapce = new RegulationWarning(WarningType.NOT_IN_SPACE, getCurrentDate(), nodeContext.getTimeShiftMS());
				notInSapce.addAgent(nextAgentName);
				result.put(nextAgentName, notInSapce);
			}
		}
		return result;
	}


	private void cleanExpiredData() {
		Map<String, RegulationWarning> mapWarnings = getMapWarnings();
		String warningKey = null;
		while ((warningKey = getExpiredWarningKey(mapWarnings)) != null) {
			lsa.removePropertiesByQueryAndName(warningKey, "WARNING");
			// Refersh warning map
			mapWarnings = getMapWarnings();
		}
		RescheduleTable rescheduleTable = getRescheduleTable();
		rescheduleTable.cleanExpiredDate();
		AwardsTable awardsTable = getAwardsTable();
		if(awardsTable != null && awardsTable.hasExpired()) {
			lsa.removePropertiesByName("AWARDS");
		}
	}


	private RescheduleTable getRescheduleTable() {
		Property pRescheduleTable = lsa.getOnePropertyByName("RESCHEDULE");
		if(pRescheduleTable!=null && pRescheduleTable.getValue() instanceof RescheduleTable) {
			return (RescheduleTable) pRescheduleTable.getValue();
		}
		return new RescheduleTable();
	}

	private AwardsTable getAwardsTable() {
		Object oAwardsTable = lsa.getOnePropertyValueByName("AWARDS");
		if(oAwardsTable instanceof AwardsTable) {
			return (AwardsTable) oAwardsTable;
		}
		return null;
	}
}
