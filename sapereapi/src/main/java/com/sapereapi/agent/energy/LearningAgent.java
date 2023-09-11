package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.helper.PredictionHelper;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyEventTable;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.TimestampedValue;
import com.sapereapi.model.energy.forcasting.EndUserForcastingResult;
import com.sapereapi.model.energy.forcasting.ForcastingResult;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRequest;
import com.sapereapi.model.energy.forcasting.input.ForcastingRequest;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovTransitions;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.prediction.PredictionCorrection;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionStep;
import com.sapereapi.model.protection.ProtectedContract;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeConfig;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;

public class LearningAgent extends SupervisionAgent implements ISupervisorAgent {
	private static final long serialVersionUID = 1L;
	public final static int REFRESH_PERIOD_SEC = 60;
	public final static int LEARNING_TIME_STEP_MINUTES = 1;
	//public final static int PREDICTION_HORIZON_MINUTES = 60;
	public final static int[] LIST_PREDICTION_HORIZON_MINUTES = {5,10,30,60};
	public final static Set<Integer> DAYS_OF_WEEK = new  HashSet<>(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY, Calendar.FRIDAY,Calendar.SATURDAY,Calendar.SUNDAY));
	public final static List<MarkovTimeWindow> ALL_TIME_WINDOWS = PredictionDbHelper.retrieveTimeWindows();
	private List<String> querys = new ArrayList<String>();
	private Map<String, Integer> mapReceivedEventKeys = null;
	private int stepNumber = 1;
	// Node data
	private NodeTotal nodeTotal = null;
	private NodeMarkovTransitions nodeMarkovTransitions = null;
	private NodeTransitionMatrices nodeTransitionMatrices = null;
	private Map<String, Long> mapLastStateHistoryId = null;

	// Neighborhood data
	private Map<String, NodeConfig> mapNeighborNodeConfigs = new HashMap<String, NodeConfig>();
	private Map<String, NodeTotal> mapNeighborhoodTotal = null;
	private Map<String, NodeMarkovTransitions> mapNeighborhoodMarkovTransitions = null;
	private Map<String, NodeTransitionMatrices> mapNeighborhoodTransitionMatrices = null;
	//private String variables[] = null;
	private PredictionStep currentTimeSlot = null;
	private Date forcedCurrentTime = null;
	private Date registerDate = null;
	private int learningWindow = 100;
	public final static int EVENT_INIT_DECAY = 5;
	private PredictionContext predictionContext = null;
	private boolean enableNeighborHoodPredictions = false;


	public LearningAgent(String _agentName,  AgentAuthentication _authentication, NodeContext _nodeContext) {
		super(_agentName, _authentication
				, new String[] { "EVENT", "WARNING", "MODEL" }
				, new String[] { "PRED", "MODEL" }
				, _nodeContext);
		this.nodeTotal = new NodeTotal();
		this.nodeTotal.setTimeShiftMS(nodeContext.getTimeShiftMS());
		this.mapNeighborhoodTotal = new HashMap<String, NodeTotal>();
		this.mapReceivedEventKeys = new HashMap<String, Integer>();
		EnergyDbHelper.cleanHistoryDB();
		this.addDecay(REFRESH_PERIOD_SEC);
		debugLevel = 0;
		registerDate = getCurrentDate();
		this.predictionContext = new PredictionContext(nodeContext, agentName, learningWindow, ALL_TIME_WINDOWS);
		if(predictionContext.isPredictionsActivated()) {
			Map<Integer, Map<String, TransitionMatrixKey>> mapTransitionMatrixKeys = EnergyDbHelper.loadMapNodeTransitionMatrixKeys(predictionContext);
			this.predictionContext.setMapTransitionMatrixKeys(mapTransitionMatrixKeys);
		}
		mapLastStateHistoryId = new HashMap<>();
		nodeMarkovTransitions = new NodeMarkovTransitions(predictionContext, nodeContext.getVariables(), registerDate, nodeContext.getMaxTotalPower());
		mapNeighborhoodMarkovTransitions  = new HashMap<String, NodeMarkovTransitions>();
		//forcedCurrentTime = (ALL_TIME_WINDOWS.get(18)).getStartDate();
		if(nodeContext.hasDatetimeShift()) {
			forcedCurrentTime = nodeContext.getCurrentDate();
		}
		Date markovDate = getMarkovDate();
		try {
			currentTimeSlot = PredictionHelper.getTimeSlot(markovDate);
		} catch (Exception e) {
			logger.error(e);
		}
		if(currentTimeSlot!=null && predictionContext.isPredictionsActivated()) {
			nodeTransitionMatrices = loadNodeTransitionMatrices(nodeContext.getNodeConfig(), markovDate);
		}
		mapNeighborhoodTransitionMatrices = new HashMap<String,NodeTransitionMatrices>();
		stepNumber = 1;
		if(predictionContext.isPredictionsActivated() && predictionContext.isAggregationsActivated()) {
			// addCustomizedAggregation("max_power", "CONTRACT1", false);
			addCustomizedAggregation("avg", "MODEL", true);
		}
		this.periodicRefresh();
	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

	public String[] getVariables() {
		return nodeContext.getVariables();
	}

	public String getScenario() {
		return nodeContext.getScenario();
	}

/*
	public List<String> getNeighborLocations() {
		ArrayList<String> result = new ArrayList<>();
		//result.add(nodeContext.getMainServiceAddress());
		for(NodeConfig neighborNodeConfig : neighborNodeConfigs)
			result.add(neighborNodeConfig.getMainServiceAddress());
		Collections.sort(result);
		return result;
	}


	private static String[] getNeighbors() {
		return NodeManager.instance().getNetworkDeliveryManager().getNeighbours();
	}
*/

	public Map<String, NodeConfig> getMapAllNodeConfigs(boolean includeHomeNode) {
		Map<String, NodeConfig> result = new HashMap<>();
		if(includeHomeNode) {
			result.put(this.nodeContext.getNodeConfig().getName(), this.nodeContext.getNodeConfig());
		}
		for(NodeConfig nodeConfig : mapNeighborNodeConfigs.values()) {
			result.put(nodeConfig.getName(), nodeConfig);
		}
		return result;
	}


	public Map<String, NodeConfig> getMapNeighborNodeConfigs() {
		return mapNeighborNodeConfigs;
	}

	public int getLearningWindow() {
		return learningWindow;
	}

	public PredictionContext getPredictionContextCopy() {
		if(predictionContext == null) {
			return null;
		}
		return predictionContext.clone();
	}

	@Override
	public void onBondNotification(BondEvent event) {
		try {
			Lsa bondedLsa = event.getBondedLsa();
			String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
			// lastQuery = query;
			if(debugLevel>0) {
				logger.info("** LearningAgent bonding ** " + agentName + " Q: " + query);
			}
			lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
			this.addBondedLSA(bondedLsa);

			if (lsa.hasBondedBefore(bondedLsa.getAgentName(), query)) {
				logger.info("** " + bondedLsa.getAgentName() + " Already bound before query " + query);
			}

			if (lsa.getSubDescription().size() >= 1) { // output
				Lsa chosenLSA = getBondedLsaByQuery(query).get(rand.nextInt(getBondedLsaByQuery(query).size()));
				// Agent linked to the Bonding LSA
				NodeConfig lsaNodeConfig = chosenLSA.getAgentAuthentication().getNodeLocation();
				if(!NodeManager.isLocal(lsaNodeConfig) && !mapNeighborNodeConfigs.containsKey(lsaNodeConfig.getName())) {
					// TODO : register node config in DB
					lsaNodeConfig.setId(null);
					lsaNodeConfig = EnergyDbHelper.registerNodeConfig(lsaNodeConfig);
					mapNeighborNodeConfigs.put(lsaNodeConfig.getName(), lsaNodeConfig);
				}
				completeContent(bondedLsa);
				Property pEvent = chosenLSA.getOnePropertyByName("EVENT");
				Property pWarning = chosenLSA.getOnePropertyByName("WARNING");
				if (pEvent != null && pEvent.getValue() instanceof EnergyEventTable) {
					 EnergyEventTable nextEventTable = (EnergyEventTable) pEvent.getValue();
					 for(EnergyEvent nextEvent : nextEventTable.getEvents()) {
						 handleEvent( nextEvent, bondedLsa);
					 }
				}
				if (pWarning != null && pWarning.getValue() instanceof RegulationWarning) {
					handleWarning((RegulationWarning) pWarning.getValue(), bondedLsa);
				}
				this.removeBondedLsasOfQuery(query);
			}
		} catch (Throwable t) {
			logger.error(t);
		}
	}

	private void handleWarning(RegulationWarning warning, Lsa bondedLsa) {
		AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
		if (AgentType.REGULATOR.equals(bondAgentType)) {
			if(WarningType.GENERAL_INTERRUPTION.equals(warning.getType())) {
				stopAgent();
			}
		}
	}
	private void handleEvent(EnergyEvent nextEvent,Lsa bondedLsa) throws Throwable {
		//logger.info(this.agentName + " learningAgent receives event " + nextEvent);
		if(EventType.CONTRACT_STOP.equals(nextEvent.getType())) {
			//logger.info(this.agentName + " For debug : learningAgent receives event " + nextEvent.getType());
		}
		AgentType bondAgentType = AgentType.getFromLSA(bondedLsa);
		int issuerDistance = bondedLsa.getSourceDistance();
		nextEvent.setIssuerDistance(issuerDistance);
		String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
		if (AgentType.CONSUMER.equals(bondAgentType) || AgentType.PRODUCER.equals(bondAgentType)) {
			String nextEventKey = nextEvent.getKey();
			if(EventType.CONTRACT_START.equals(nextEvent.getType())) {
				if(nextEvent.isComplementary()) {
					logger.info("handleEvent for debug");
				}
			}
			if(!this.mapReceivedEventKeys.containsKey(nextEventKey)) {
				mapReceivedEventKeys.put(nextEventKey, EVENT_INIT_DECAY);
				// Check if the event is local
				NodeConfig issuerLocation = nextEvent.getIssuerLocation();
				if(!NodeManager.isLocal(issuerLocation) && true) {
					issuerLocation.setId(null); // Remove ids from another node
					String evtNode = issuerLocation.getName();
					if(mapNeighborNodeConfigs.containsKey(evtNode)) {
						issuerLocation = mapNeighborNodeConfigs.get(evtNode);
						nextEvent.setIssuerLocation(issuerLocation);
						Contract contract = null;
						if(AgentType.CONSUMER.equals(bondAgentType)) {
							// Get contract from contrat adgent
							Property pMainContract = bondedLsa.getOnePropertyByName("CONTRACT1");
							Property pSdContract = bondedLsa.getOnePropertyByName("CONTRACT2");
							logger.info("For debug : register distant event " + nextEvent);
							if(pMainContract!=null && pMainContract.getValue() instanceof ProtectedContract) {
								ProtectedContract protectedContrat = (ProtectedContract) pMainContract.getValue();
								contract = protectedContrat.getContract(this);
								//contract.completeLocationId(mapNeighborNodeConfigs);
								if(!contract.checkLocationId()) {
									bondedLsa = completeContent(bondedLsa);
									logger.error("For debug1 map location = "+ contract.getMapLocation());
								}
							} else if(pSdContract!=null && pSdContract.getValue() instanceof ProtectedContract) {
								ProtectedContract protectedContrat = (ProtectedContract) pSdContract.getValue();
								contract = protectedContrat.getContract(this);
								//contract.completeLocationId(mapNeighborNodeConfigs);
								if(!contract.checkLocationId()) {
									logger.error("For debug2 map location = "+ contract.getMapLocation());
								}
							}
						}
						EnergyDbHelper.registerEvent2(nextEvent, contract);
					} else {
						logger.error("handleEvent : event location " + evtNode + " not in mapNeighborNodeConfigs " + nextEvent);
					}
				}
				//lastEvent = nextEvent;
				refreshNodeTotal(nextEvent);
				if ((!"".equals(query)) && (!querys.contains(query))) {
					querys.add(query);
				}
				refreshLSA();
				//refreshMarkovChains(false);
			} else {
				if(debugLevel>0) {
					logger.info("Event already added " + nextEvent);
				}
			}
		}
	}

	private void refreshLSA() {
		//String sBonds = SapereUtil.implode(bond_agents, ",");
		try {
			/*
			String state = lsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString();
			state = SapereUtil.addOutputsToState(state, new String[] {});
			*/
			//String sQueries = "";
			lsa.removePropertiesByName("TOTAL");
			lsa.addProperty(new Property("TOTAL", nodeTotal));
			//logger.info("add properties on query " + sQueries + " and bond " + sBonds);
		} catch (Throwable e) {
			logger.error(e);
		}
	}

	/**
	 * Get next expired event key (for table cleaning)
	 * 
	 * @return
	 */
	private String getNextExpiredEventKey2() {
		for (String key : this.mapReceivedEventKeys.keySet()) {
			Integer decay  = mapReceivedEventKeys.get(key);
			if(decay <= 0) {
				return key;
			}
		}
		return null;
	}

	private void refreshNodeTotal(EnergyEvent event) {
		registerDate = (event==null)? getCurrentDate() : event.getBeginDate();
		String warningConsumer = "";
		if(nodeTotal !=null && nodeTotal.getMaxWarningConsumer() != null) {
			warningConsumer = nodeTotal.getMaxWarningConsumer();
		}
		if(event!=null && event.canStopMissingWarning() && nodeTotal!=null && (
				nodeTotal.hasMissingRequestWarning() || nodeTotal.getMaxWarningDuration() > 0 || (warningConsumer.length() > 0))) {
			// Check if the event can end a warning duration
			// Date t - 1 sec 
			Date minusOne = UtilDates.shiftDateSec(registerDate, -1);
			if(nodeTotal.getDate().before(minusOne)) {
				logger.info("register complementary nodeTotal at t minus 1 : " + UtilDates.format_time.format(minusOne) + " event : " + event);
				EnergyDbHelper.generateNodeTotal(minusOne, nodeContext.getTimeShiftMS(), null, getUrl(), agentName, nodeContext.getNodeConfig());
			}
		}

		// Clean expired events
		/*
		String keyToRemove = getNextExpiredEventKey();
		while (keyToRemove != null) {
			eventTable.remove(keyToRemove);
			keyToRemove = getNextExpiredEventKey();
		}*/
		List<String> lsaAgents = new ArrayList<String>();
		for (Lsa lsa : NodeManager.instance().getSpace().getAllLsa().values()) {
			lsaAgents.add(lsa.getAgentName());
		}
		// Refresh Node total
		//Long idLast = nodeTotal==null? null : nodeTotal.getId();
		nodeTotal = EnergyDbHelper.generateNodeTotal(registerDate, nodeContext.getTimeShiftMS(),event, getUrl(), agentName, nodeContext.getNodeConfig());
		mapNeighborhoodTotal.clear();
		/*
		if(enableNeighborHoodPredictions) {
			for(String neighborLocation : NodeManager.instance().getNetworkDeliveryManager().getNeighbours()) {
				NodeTotal neighborTotal = EnergyDbHelper.generateNodeTotal(registerDate, nodeContext.getTimeShiftMS(), event, getUrl(), agentName, neighborLocation);
				if(neighborTotal.hasActivity()) {
					mapNeighborhoodTotal.put(neighborLocation, neighborTotal);
				}
			}
		}*/

	}

	@Override
	public void onPropagationEvent(PropagationEvent event) {
	}

	@Override
	public void onDecayedNotification(DecayedEvent event) {
		try {
			// decayedLsa.toVisualString());
			if(stopped) {
				querys.clear();
				mapReceivedEventKeys.clear();;
				nodeTotal = null;
				nodeMarkovTransitions = null;
				nodeTransitionMatrices = null;
				// Neighborhood data
				mapNeighborhoodTotal.clear();;
				mapNeighborhoodMarkovTransitions.clear();
				mapNeighborhoodTransitionMatrices.clear();
				this.addDecay(0);
			} else {
				Lsa decayedLsa = event.getLsa();
				// logger.info("onDecayedNotification: decayedLsa = " +
				Integer decay = Integer.valueOf("" + decayedLsa.getSyntheticProperty(SyntheticPropertyName.DECAY));
				if (decay < 1) {
					periodicRefresh();
					this.addDecay(REFRESH_PERIOD_SEC);
				}
				// Remove expired event keys
				String eventKey = null;
				while((eventKey = getNextExpiredEventKey2()) !=null) {
					mapReceivedEventKeys.remove(eventKey);
				}
				// update decay on none expired event keys
				for(String key : mapReceivedEventKeys.keySet()) {
					int decay2 = mapReceivedEventKeys.get(key);
					mapReceivedEventKeys.put(key, decay2-1);
				}
			}
			// For propagation
			addGradient(3);
			//logger.info("mapReceivedEventKeys:" + mapReceivedEventKeys);
		} catch (Throwable e) {
			logger.error(e);
			logger.info("Exception thrown in onDecayedNotification :" + agentName + " " + event + " " + e.getLocalizedMessage());
		}
	}

	@Override
	public void onLsaUpdatedEvent(LsaUpdatedEvent event) {
		logger.info("onLsaUpdatedEvent:" + agentName);
	}

	public void refreshHistory(EnergyEvent event) {
		refreshNodeTotal(event);
		refreshLSA();
	}

	private Date getMarkovDate() {
		Date markovDate = registerDate;
		if(nodeContext.hasDatetimeShift()) {
			forcedCurrentTime = nodeContext.getCurrentDate();
			markovDate = forcedCurrentTime;
		}
		return markovDate;
	}

	public void periodicRefresh() {
		boolean hasActivity = nodeTotal!=null && nodeTotal.hasActivity();
		boolean enableHistoUpdate = (stepNumber>1);
		boolean generatePrediction = (stepNumber>1) && hasActivity && !predictionContext.isSupervisionDisabled();
		boolean saveTransitionMatrix = (stepNumber>1) && hasActivity && !predictionContext.isSupervisionDisabled();
		//EnergyDbHelper.correctHisto();
		refreshHistory(null);
		if(predictionContext.isPredictionsActivated()) {
			try {
				refreshMarkovChains(enableHistoUpdate, saveTransitionMatrix);
				if(generatePrediction) {
				    logger.info("Before prediction");
					PredictionData prediction1 = computePrediction(LIST_PREDICTION_HORIZON_MINUTES, NodeManager.getLocation(), false);
					PredictionDbHelper.savePredictionResult(prediction1);
					PredictionData prediction2 = computePrediction(LIST_PREDICTION_HORIZON_MINUTES, NodeManager.getLocation(), true);
					PredictionDbHelper.savePredictionResult(prediction2);
					logger.info("After prediction");
					lsa.removePropertiesByName("PRED");
					lsa.addProperty(new Property("PRED", prediction2));
					Date markovDate = getMarkovDate();
					Date dateMax = UtilDates.removeMinutes(markovDate);
					Date dateMin = UtilDates.shiftDateMinutes(dateMax,-60);
					// TO DELETE !!!!!!!!
					/*
					int testShift = -5;
					dateMin  = SapereUtil.shiftDateMinutes(dateMin,testShift*60);
					dateMax  = SapereUtil.shiftDateMinutes(dateMax,testShift*60);
					*/
					TimeSlot timeSlot = new TimeSlot(dateMin, dateMax);
					boolean generateSelfCorrections = true;
					if(generateSelfCorrections) {
						List<PredictionCorrection> corrections = PredictionHelper.applyPredictionSelfCorrection(predictionContext, timeSlot);
						if(corrections.size() > 0) {
							logger.info("periodicRefresh : " + corrections.size() + " corrections done on transition matrix following prediction fails");
							// Change has been done transition matdix : refresh transition matrix
							nodeTransitionMatrices = loadNodeTransitionMatrices(nodeContext.getNodeConfig(), markovDate);
						}
					}
				}
			} catch (Throwable e) {
				logger.error(e);
			}
		}
		stepNumber++;
		// Log memroy state
		int mb = 1024 * 1024; 
		double totalMemory = Runtime.getRuntime().totalMemory()/ mb;
		//double freeMemory = Runtime.getRuntime().freeMemory()/ mb;
		double usedMemory =  (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/mb;
		logger.info("Used memory " + usedMemory + " / " + totalMemory + " MB");
		//logger.info("Total memory " +  Runtime.getRuntime().totalMemory()/ mb);
		//logger.info("Free memory " +  Runtime.getRuntime().freeMemory()/ mb);
		logger.info("Free maxMemory " +  Runtime.getRuntime().maxMemory()/ mb + " MB");
		/*
		ObjectSizeFetcher.printObjectSize(this, logger);
		ObjectSizeFetcher.printObjectSize(NodeManager.instance(), logger);
		ObjectSizeFetcher.printObjectSize(NodeManager.instance().getNotifier(), logger);
		ObjectSizeFetcher.printObjectSize(NodeManager.instance().getSpace(), logger);
		*/
		logger.info("nbSubscriptions = " + NodeManager.instance().getNotifier().getNbSubscriptions());
		Map<String, Integer> nbSubscriptionsByAgent = NodeManager.instance().getNotifier().getNbSubscriptionsByAgent();
		for(String agent : nbSubscriptionsByAgent.keySet()) {
			logger.info("     " +  agent  + " : " + nbSubscriptionsByAgent.get(agent));
		}
	}

	private NodeTransitionMatrices loadNodeTransitionMatrices(NodeConfig nodeConfig, Date markovDate) {
		if(currentTimeSlot!=null) {
			PredictionContext predictionCtx = PredictionHelper.generateNeightborContext(predictionContext);
			PredictionDbHelper.refreshTransitionMatrixCell(predictionCtx, currentTimeSlot.getMarovTimeWindowId());
			return PredictionDbHelper.loadNodeTransitionMatrices(predictionCtx, nodeContext.getVariables(), currentTimeSlot.getMarkovTimeWindow(), markovDate);
		}
		return null;
	}

	public void refreshMarkovChains(boolean enableHistoUpdate, boolean enableObsUpdate) throws Exception {
		// Refresh Markov state : only when no specific event (ie at periodic refresh)
		Date markovDate = getMarkovDate();
		nodeMarkovTransitions.refreshTransitions(nodeTotal, enableObsUpdate);

		// Set states index in history
		if(enableHistoUpdate) {
			if(nodeContext.hasDatetimeShift()) {
				NodeTotal nodeTotal2 = nodeTotal.clone();
				nodeTotal2.setDate(forcedCurrentTime);
				mapLastStateHistoryId = PredictionDbHelper.saveHistoryStates(nodeTotal2, nodeMarkovTransitions, nodeContext);
			} else {
				mapLastStateHistoryId = PredictionDbHelper.saveHistoryStates(nodeTotal, nodeMarkovTransitions, nodeContext);
			}
		}
		if(enableNeighborHoodPredictions) {
			/*
			for(String neighborLocation : mapNeighborhoodTotal.keySet()) {
				NodeTotal neighborTotal = mapNeighborhoodTotal.get(neighborLocation);
				// Initialize a Markov transition it does not exists for this neighbor
				if(!mapNeighborhoodMarkovTransitions.containsKey(neighborLocation)) {
					PredictionContext neighborContext = PredictionHelper.generateNeightborContext(predictionContext);
					mapNeighborhoodMarkovTransitions.put(
						neighborLocation
						, new NodeMarkovTransitions(neighborContext, nodeContext.getVariables(), registerDate, nodeContext.getMaxTotalPower()));
				}
				NodeMarkovTransitions neighborTransition = mapNeighborhoodMarkovTransitions.get(neighborLocation);
				neighborTransition.refreshTransitions(neighborTotal, enableObsUpdate);
				if(neighborTotal.hasActivity() && neighborTransition.getLastState("requested")==null) {
					neighborTransition.initializeLast();
				}
				mapNeighborhoodMarkovTransitions.put(neighborLocation, neighborTransition);
				// Initialize new Markov transition matrix it does not exists for this neighbor
				if(!mapNeighborhoodTransitionMatrices.containsKey(neighborLocation)) {
					NodeTransitionMatrices nextNodeTransitionMatrices = loadNodeTransitionMatrices(nodeContext.getNodeConfig(), markovDate);
					mapNeighborhoodTransitionMatrices.put(neighborLocation, nextNodeTransitionMatrices);
				}
			}*/
		}
		if(currentTimeSlot !=null && markovDate.before(currentTimeSlot.getEndDate())) {
			// OK
		} else {
			// Time Slot change
			currentTimeSlot = PredictionHelper.getTimeSlot(markovDate);
		}
		// load of reload transition matrix if necessary
		if(currentTimeSlot ==null || nodeTransitionMatrices.getTimeWindowId() != currentTimeSlot.getMarovTimeWindowId()) {
			// load transition matrix or node
			currentTimeSlot = PredictionHelper.getTimeSlot(markovDate);
			nodeTransitionMatrices = loadNodeTransitionMatrices(nodeTotal.getNodeConfig(), markovDate);
			/*
			for(String neighborLocation : mapNeighborhoodTotal.keySet()) {
				NodeTransitionMatrices nextNodeTransitionMatrices = loadNodeTransitionMatrices(neighborLocation, markovDate);
				mapNeighborhoodTransitionMatrices.put(neighborLocation, nextNodeTransitionMatrices);
			}*/
		}
		// Update node transition matrix
		if(enableObsUpdate) {
			boolean updateted = nodeTransitionMatrices.updateMatrices(markovDate, this.nodeMarkovTransitions);
			if(updateted) {
				PredictionDbHelper.saveNodeTransitionMatrices(predictionContext, nodeTransitionMatrices, mapLastStateHistoryId);
			}
			// Update neighbors transition matrix
			if(enableNeighborHoodPredictions) {
				/*
				for(String neighborLocation : mapNeighborhoodMarkovTransitions.keySet()) {
					NodeMarkovTransitions neighborTransitions = mapNeighborhoodMarkovTransitions.get(neighborLocation);
					if(mapNeighborhoodTransitionMatrices.containsKey(neighborLocation)) {
						NodeTransitionMatrices neighborTransitionMatrices = mapNeighborhoodTransitionMatrices.get(neighborLocation);
						boolean updateted2 = neighborTransitionMatrices.updateMatrices(markovDate, neighborTransitions);
						if(updateted2) {
							PredictionDbHelper.saveNodeTransitionMatrices(predictionContext, neighborTransitionMatrices, new HashMap<>());
						}
					}
				}*/
			}
		}
		if(nodeTransitionMatrices != null) {
			this.lsa.replacePropertyWithName(new Property("MODEL", nodeTransitionMatrices));
		}
		if(debugLevel>0) {
			logger.info("nodeMarkovTransitions = " + nodeMarkovTransitions);
			logger.info("nodeTransitionMatrices = " + nodeTransitionMatrices);
		}
	}

	public NodeTransitionMatrices getNodeTransitionMatrices() {
		return nodeTransitionMatrices;
	}

	public PredictionData computePrediction(int[] listHorizonMinutes, String location, boolean useCorrections) {
		List<Date> targetDates = new ArrayList<Date>();
		Date current = getCurrentDate();
		for(Integer horizonMinute : listHorizonMinutes) {
			targetDates.add(UtilDates.shiftDateMinutes(current, horizonMinute));
		}
		PredictionData result2 = PredictionHelper.getInstance().computePrediction(predictionContext,
				current, targetDates, nodeContext.getVariables(), nodeMarkovTransitions, mapNeighborhoodMarkovTransitions, useCorrections);
		return result2;
	}

	public List<MarkovStateHistory> retrieveLastMarkovHistoryStates(Date minCreationDate, String variableName, boolean observationUpdated) {
		return PredictionDbHelper.retrieveLastMarkovHistoryStates(nodeMarkovTransitions, minCreationDate,  variableName, observationUpdated);
	}

	public MarkovState getCurrentMarkovState(String variable) {
		if(nodeMarkovTransitions != null) {
			return nodeMarkovTransitions.getState(variable);
		}
		return null;
	}

	public void stopAgent() {
		stopped = true;
	}


	public EndUserForcastingResult getForcasting(EndUserForcastingRequest endUserForcastingRequest) {
		int nbTValues = endUserForcastingRequest.getSamplingNb();
		Date date = endUserForcastingRequest.getTimestamp();
		EndUserForcastingResult result = new EndUserForcastingResult();
		List<TimestampedValue> sample_tvalues = EnergyDbHelper.getInstance().retrieveLastValues(date, nbTValues);
		try {
			ForcastingResult forcasting = callForcastingService(sample_tvalues);
			result.setPredicetedValues(forcasting.getValues());
		} catch (Exception e) {
			logger.error(e);
			List<Double> values = Arrays.asList(-1., -1., -1., -1.);
			result.setPredicetedValues(values);
			result.setErrorMessage(e.getMessage());
		}
		Date horizonDate = UtilDates.shiftDateMinutes(date, 60);
		List<TimestampedValue> real_tvalues = EnergyDbHelper.getInstance().retrieveLastValues(horizonDate, 4);
		List<Double> realValues = new ArrayList<>();
		List<Date> timestamps = new ArrayList<>();
		for (TimestampedValue t_value: real_tvalues) {
			realValues.add(t_value.getValue());
			timestamps.add(t_value.getTimestamp());
		}
		// set realValues = value at T + 1H
		result.setRealValues(realValues);
		result.setTimestamps(timestamps);
		return result;
	}

	public ForcastingResult callForcastingService(List<TimestampedValue> values) throws Exception {
		ForcastingResult result = new ForcastingResult();
		ForcastingRequest forcastingRequest = new ForcastingRequest();
	    forcastingRequest.setTimestampedValues(values);
		Map<String, Double> forcastingRequest2 = forcastingRequest.getMap();
	    String urlForcasting = nodeContext.getUrlForcasting();
	    StringBuffer sParams = UtilJsonParser.toJsonStr(forcastingRequest, logger, 0);
		StringBuffer sParams2 = UtilJsonParser.toJsonStr(forcastingRequest2, logger, 0);
		logger.info("callForcastingService sParams2 = " + sParams2);
		/*
		String baseForcastingUrl = nodeConfig.getUrl();//  "http://localhost:1234/";
		//StringBuffer jsonContent = UtilJsonParser.toJsonStr(forcastingRequest, logger, 0);
		String hostname = InetAddress.getLocalHost().getHostName();
		String nodeConfigHostname = nodeConfig.getHost();
		logger.info("callForcastingService : hostname = " + hostname + ", nodeConfigHostname = " + nodeConfigHostname);			*/
		//boolean sendRequest = false;
		if(urlForcasting == null) {
			logger.info("call generateMockForcasting : params = " + sParams.toString());
			result = generateMockForcasting(forcastingRequest2);
		} else {
			// TODO : send to http://127.0.0.1:5000/predict
			//baseForcastingUrl = baseForcastingUrl.replace("9191", "9292");
			//baseForcastingUrl =  baseForcastingUrl.replace(nodeConfigHostname, hostname);
			String postResponse = null;
			try {
				logger.info("send POST request to urlForcasting service " + urlForcasting + " params=" + sParams2.toString());
				postResponse =  UtilHttp.sendPostRequest(urlForcasting, forcastingRequest2, logger, debugLevel);
			}
			catch (Throwable e) {
				logger.error(e);
				Exception e2 = new Exception("Error returned during the call of web-service " + SapereUtil.addDoubleQuote(urlForcasting)  + ": " + e, e);
				throw e2;
			}
			if (postResponse == null) {
				logger.warning("callForcastingService :postResponse is null ");
				new Exception("no output returned by the web-service " + urlForcasting);
			}
			try {
				JSONObject jsonForcastingResult = new JSONObject(postResponse);
				result = UtilJsonParser.parseForcastingResult(jsonForcastingResult, logger);
			} catch (Throwable e) {
				new Exception("Error during the json parsing of the output returned by " + urlForcasting +  ":" + e, e);
			}
			logger.info("callForcastingService : result = " + result);
		}
		return result;
	}

	public ForcastingResult generateMockForcasting(Map<String, Double> /*ForcastingRequest*/ forcastingRequest) {
		ForcastingResult result = new ForcastingResult();
		List<Double> values = new ArrayList<>();
		values = Arrays.asList(0.123, 1.234, 2.345, 3.456);
		result.setValues(values);
		return result;
	}
}
