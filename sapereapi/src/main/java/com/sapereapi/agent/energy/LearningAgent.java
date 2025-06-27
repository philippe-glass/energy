package com.sapereapi.agent.energy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sapereapi.db.ClemapDbHelper;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.helper.PredictionHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.TimestampedValue;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyEventTable;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.forcasting.EndUserForcastingResult;
import com.sapereapi.model.energy.forcasting.ForcastingResult1;
import com.sapereapi.model.energy.forcasting.ForcastingResult2;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRequest;
import com.sapereapi.model.energy.forcasting.input.ForcastingRequest;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.input.HistoryInitializationForm;
import com.sapereapi.model.input.HistoryInitializationRequest;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.ICompactedModel;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.AggregationsTracking;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.TimeWindow;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.lstm.CompleteLSTMModel;
import com.sapereapi.model.learning.lstm.LSTMModelInfo;
import com.sapereapi.model.learning.lstm.request.LSTMModelInfoRequest;
import com.sapereapi.model.learning.markov.CompleteMarkovModel;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.MultiPredictionsData;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionCorrection;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionStatistic;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.model.learning.prediction.input.PredictionRequest;
import com.sapereapi.model.learning.prediction.input.StatisticsRequest;
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
import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.AggregationEvent;

public class LearningAgent extends SupervisionAgent implements ISupervisorAgent {
	private static final long serialVersionUID = 1L;
	public final static int REFRESH_PERIOD_SEC = 60;
	public final static int DECAY_SUPERVISION_DISABLED = 60*60;
	public final static int LEARNING_TIME_STEP_MINUTES = 1;
	//public final static int PREDICTION_HORIZON_MINUTES = 60;
	public final static int[] LIST_PREDICTION_HORIZON_MINUTES = {5,10,30,60};
	public final static Set<Integer> DAYS_OF_WEEK = new  HashSet<>(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY, Calendar.FRIDAY,Calendar.SATURDAY,Calendar.SUNDAY));
	public final static List<TimeWindow> ALL_TIME_WINDOWS = PredictionDbHelper.retrieveTimeWindows();
	private Map<String, Integer> mapReceivedEventKeys = null;
	private int stepNumber = 1;
	// Node data
	private NodeTotal nodeTotal = null;
	private NodeTotal clusterTotal = null;

	private ILearningModel learningModel = null;
	private ILearningModel learningModelCluster = null;
	private long maxHistoryDurationMS = 1000*3600*2; // 2 hours

	// Neighborhood data
	//private Map<String, NodeTotal> mapNeighborhoodTotal = null;
	private Map<PredictionScope, Map<String, ILearningModel>> receivedLearningModels = new HashMap<PredictionScope, Map<String,ILearningModel>>();
	private Map<PredictionScope, Map<String, PredictionData>> receivedPredictions = new HashMap<PredictionScope, Map<String,PredictionData>>();
	private Map<PredictionScope, PredictionData> aggregatedPredictions = new HashMap<PredictionScope, PredictionData>();

	//private Map<String, NodeMarkovTransitions> mapNeighborhoodMarkovTransitions = null;
	//private Map<String, NodeTransitionMatrices> mapNeighborhoodTransitionMatrices = null;
	private Date forcedCurrentTime = null;
	private Date registerDate = null;
	private int learningWindow = 100;
	public final static int EVENT_INIT_DECAY = 20; // 5


	public LearningAgent(String _agentName,  AgentAuthentication _authentication, NodeContext _nodeContext) throws HandlingException {
		super(_agentName, _authentication
				, new String[] { "EVENT", "WARNING" }
				, new String[] { "PRED", "PRED_CLUSTER"}
				, _nodeContext);
		this.nodeTotal = new NodeTotal();
		this.clusterTotal = new NodeTotal();
		this.nodeTotal.setTimeShiftMS(nodeContext.getTimeShiftMS());
		//this.mapNeighborhoodTotal = new HashMap<String, NodeTotal>();
		this.mapReceivedEventKeys = new HashMap<String, Integer>();
		String dumpHistoryDirectory = "../lstm/history_data/";
		EnergyDbHelper.cleanHistoryDB();
		debugLevel = _nodeContext.getDebugLevel();
		registerDate = getCurrentDate();
		this.addDecay(nodeContext.isSupervisionDisabled()? DECAY_SUPERVISION_DISABLED : REFRESH_PERIOD_SEC);
		if(nodeContext.hasDatetimeShift()) {
			forcedCurrentTime = nodeContext.getCurrentDate();
		}
		NodeStates.initialize(nodeContext.getMaxTotalPower());
		// MODEL OF LOCAL NODE
		// Initialize learning model that predict state of local node
		Map<PredictionScope, LearningAggregationOperator> mapAggregationOp =new HashMap<PredictionScope, LearningAggregationOperator>();
		PredictionSetting nodePredictionSetting = nodeContext.getNodePredicitonSetting();
		LearningModelType modelType = nodePredictionSetting.getUsedModel();	// LearningModelType.valueOf(nodePredictionSetting.getUsedModel().getLabel())  ;
		if(nodePredictionSetting.isActivated() && nodePredictionSetting.isAggregationActivated()) {
			mapAggregationOp.put(PredictionScope.NODE,  nodePredictionSetting.getAggregator());
		}
		PredictionSetting clusterPredictionSetting = nodeContext.getClusterPredictionSetting();
		if(clusterPredictionSetting.isActivated() && clusterPredictionSetting.isAggregationActivated()) {
			if(!mapAggregationOp.isEmpty()) {
				logger.info("add aggregators both in node and cluster prediction models");
			}
			mapAggregationOp.put(PredictionScope.CLUSTER,  clusterPredictionSetting.getAggregator());
		}
		LearningAggregationOperator modelAggregationOpNode = null;
		if(mapAggregationOp.containsKey(PredictionScope.NODE)) {
			modelAggregationOpNode = mapAggregationOp.get(PredictionScope.NODE);
		}
		//String predictionAggregationOpNode = null;
		PredictionContext predictionContext = new PredictionContext(nodeContext, PredictionScope.NODE, modelType, learningWindow, ALL_TIME_WINDOWS, modelAggregationOpNode, dumpHistoryDirectory);
		// register preidctionContext and set its new Id
		if(nodeContext.isPredictionsActivated(PredictionScope.NODE)) {
			predictionContext = PredictionDbHelper.registerPredictionContext(predictionContext);
			Map<FeaturesKey, Map<String, VariableFeaturesKey>> mapTransitionMatrixKeys;
			try {
				mapTransitionMatrixKeys = PredictionDbHelper.loadMapNodeTransitionMatrixKeys2(predictionContext);
				predictionContext.setMapTransitionMatrixKeys(mapTransitionMatrixKeys);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		if(LearningModelType.MARKOV_CHAINS.equals(modelType)) {
			learningModel = new CompleteMarkovModel();
		} else if(LearningModelType.LSTM.equals(modelType)) {
			learningModel = initLSTMmodel(predictionContext);
		} else if(modelType == null) {
			throw new HandlingException("model type not set int node context");
		} else {
			throw new HandlingException("model type " + modelType + " is not implemented");
		}
		learningModel.initialize(registerDate, predictionContext, logger);
		//forcedCurrentTime = (ALL_TIME_WINDOWS.get(18)).getStartDate();
		try {
			if(nodeContext.getNodePredicitonSetting().isActivated()) {
				learningModel = loadCompleteModel(PredictionScope.NODE);
			}
		} catch (Exception e) {
			logger.error(e);
		}

		// MODEL  OF CLUSTER PREDICTION
		// Initialize learning model that predict state of all nodes the cluster
		LearningAggregationOperator aggregationOpCluster = null;
		if(mapAggregationOp.containsKey(PredictionScope.CLUSTER)) {
			aggregationOpCluster = mapAggregationOp.get(PredictionScope.CLUSTER);
		}
		PredictionContext predictionContextCluster = new PredictionContext(nodeContext, PredictionScope.CLUSTER, modelType, learningWindow, ALL_TIME_WINDOWS, aggregationOpCluster, dumpHistoryDirectory);
		if(nodeContext.isPredictionsActivated(PredictionScope.CLUSTER)) {
			predictionContextCluster = PredictionDbHelper.registerPredictionContext(predictionContextCluster);
			Map<FeaturesKey, Map<String, VariableFeaturesKey>> mapTransitionMatrixKeys;
			try {
				mapTransitionMatrixKeys = PredictionDbHelper.loadMapNodeTransitionMatrixKeys2(predictionContextCluster);
				predictionContextCluster.setMapTransitionMatrixKeys(mapTransitionMatrixKeys);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		if(LearningModelType.MARKOV_CHAINS.equals(modelType)) {
			learningModelCluster = new CompleteMarkovModel();
		} else if(LearningModelType.LSTM.equals(modelType)) {
			learningModelCluster = initLSTMmodel(predictionContextCluster);
		}
		learningModelCluster.initialize(registerDate, predictionContextCluster, logger);
		if(nodeContext.getClusterPredictionSetting().isActivated()) {
			// register preidctionContext and set its new Id
			try {
				learningModelCluster = loadCompleteModel(PredictionScope.CLUSTER);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		// Create aggregator to be executer by the aggregation low
		//addCustomizedAggregation(ILearningModel.OP_SAMPLING_NB, "MODEL", true);
		if (nodeContext.getClusterPredictionSetting().isActivated()) {
			addCustomizedAggregation(NodeTotal.OP_SUM, "TOTAL", true);
		}
		if (learningModel.hasModelAggregator()) {
			LearningAggregationOperator aggregationOp = learningModel.getPredictionContext().getAggregationOperator();
			if (aggregationOp.isModelAggregation()) {
				// Add an aggregation applied on Learning Models
				addCustomizedAggregation(aggregationOp.getName(), "MODEL", true);
			}
		}
		if (learningModelCluster.hasModelAggregator()) {
			LearningAggregationOperator aggregationOp = learningModelCluster.getPredictionContext()
					.getAggregationOperator();
			if (aggregationOp.isModelAggregation()) {
				// Add an aggregation applied on Learning Models
				addCustomizedAggregation(aggregationOp.getName(), "CLUSTER_MODEL", true);
			}
		}
		if(learningModel.hasModelOrPredictionAggregator()|| learningModelCluster.hasModelOrPredictionAggregator()) {
			addCustomizedAggregation("sum", "AGGR_STATUS", true);
		}
		if (learningModel.hasPredictionAggregator()) {
			LearningAggregationOperator aggregationOp = learningModel.getPredictionContext().getAggregationOperator();
			if (aggregationOp.isPredictionAggregation()) {
				// Add an aggregation applied on predictions(ensemble learning)
				addCustomizedAggregation(aggregationOp.getName(), "PRED", true);
			}
		}
		if (learningModelCluster.hasPredictionAggregator()) {
			LearningAggregationOperator aggregationOp = learningModelCluster.getPredictionContext()
					.getAggregationOperator();
			if (aggregationOp.isPredictionAggregation()) {
				// Add an aggregation applied on predictions(ensemble learning)
				addCustomizedAggregation(aggregationOp.getName(), "PRED_CLUSTER", true);
			}
		}
		stepNumber = 1;
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


	public PredictionContext getPredictionContext(PredictionScope scope) {
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		PredictionContext result = selectedLearningModel.getPredictionContext();
		return result;
	}

	public int getLearningWindow() {
		return learningWindow;

	}

	private NodeTotal selectNodeTotal(PredictionScope scope) {
		NodeTotal usedNodeTotal = PredictionScope.NODE.equals(scope) ? nodeTotal  : clusterTotal;
		return usedNodeTotal;
	}

	private ILearningModel selectLearningModel(PredictionScope scope) {
		ILearningModel selectedLearningModel = PredictionScope.NODE.equals(scope) ? learningModel : learningModelCluster;
		return selectedLearningModel;
	}

	private  Map<String, ILearningModel> selectReceivedLearningModels(PredictionScope scope) {
		if(receivedLearningModels.containsKey(scope)) {
			return receivedLearningModels.get(scope);
		}
		return new HashMap<String, ILearningModel>();
		//return PredictionScope.NODE.equals(scope) ? receivedLearningModelsNode : receivedLearningModelsCluster;
	}

	private ILearningModel selectModelWithAggregator(PredictionScope scope) {
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		if(selectedLearningModel.hasModelAggregator()) {
			return selectedLearningModel;
		}
		return null;
	}

	public AggregationsTracking retrieveAggregationTrackingFromLSA() {
		AggregationsTracking aggregationTracking = new AggregationsTracking();
		Object obj = lsa.getOnePropertyValueByName("AGGR_STATUS");
		if (obj != null && obj instanceof AggregationsTracking) {
			aggregationTracking = (AggregationsTracking) obj;
		}
		return aggregationTracking;
	}

	public void refreshAggregationTrackingProperty(ILearningModel selectedModel) {
		if (selectedModel.getPredictionContext().isAggregationsActivated() && stepNumber > 1) {
			AggregationsTracking aggregationsTracking = retrieveAggregationTrackingFromLSA();
			boolean isReady = selectedModel.isReadyForAggregation();
			PredictionScope scope = selectedModel.getPredictionContext().getScope();
			int newObjAvailability = isReady ? 1 : 0;
			// check if isReady has changed
			if(newObjAvailability != aggregationsTracking.getObjectAvailability(scope)) {
				aggregationsTracking.setModelAvailability(scope, newObjAvailability);
				replacePropertyWithName(new Property("AGGR_STATUS", aggregationsTracking));
			}
		}
	}

	public void refreshAggregationTrackingProperty(PredictionData predictionData) {
		if (predictionData.getPredictionContext().isAggregationsActivated() && stepNumber > 1) {
			AggregationsTracking aggregationsTracking = retrieveAggregationTrackingFromLSA();
			boolean isReady = predictionData.isReadyForAggregation();
			PredictionScope scope = predictionData.getPredictionContext().getScope();
			int newObjAvailability = isReady ? 1 : 0;
			// check if isReady has changed
			if(newObjAvailability != aggregationsTracking.getObjectAvailability(scope)) {
				aggregationsTracking.setModelAvailability(scope, newObjAvailability);
				replacePropertyWithName(new Property("AGGR_STATUS", aggregationsTracking));
			}
		}
	}

	private boolean sendModelForAggregation(PredictionScope scope) {
		if (stepNumber > 1) {
			Object obj = lsa.getOneAggregatedValue("AGGR_STATUS");
			if (obj != null && obj instanceof AggregationsTracking) {
				AggregationsTracking aggregatedModelAvailability = (AggregationsTracking) obj;
				int nbAvailable = aggregatedModelAvailability.getObjectAvailability(scope);
				int allNodesCount = NodeManager.getAllNodesCount();
				return (nbAvailable >= allNodesCount);
			}
		}
		return false;
	}

	private void setLearningModel(ILearningModel aCompleteLearningModel) {
		PredictionScope scope = aCompleteLearningModel.getPredictionContext().getScope();
		if(PredictionScope.NODE.equals(scope)) {
			learningModel = aCompleteLearningModel;
		} else {
			learningModelCluster = aCompleteLearningModel;
		}
	}

	private ILearningModel loadCompleteModel(PredictionScope scope) throws HandlingException {
		ILearningModel modelToLoad = selectLearningModel(scope);
		PredictionContext predictionContext = modelToLoad.getPredictionContext();
		Date stateDate = getCurrentDate();
		modelToLoad.load(stateDate);
		modelToLoad.setCurrentFeaturesKey(predictionContext.getFeaturesKey2(stateDate));
		return modelToLoad;
	}

	public PredictionContext getPredictionContextCopy(PredictionScope scope) {
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		if(selectedLearningModel == null) {
			return null;
		}
		return selectedLearningModel.getPredictionContext().clone();
	}

	public ILearningModel getLearningModelCopy(PredictionScope scope) {
		// Use an empty matrix filter
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		return selectedLearningModel.cloneWithFilter(new MatrixFilter(), logger);
	}

	public ILearningModel getLearningModelCopy(MatrixFilter matrixFilter) {
		PredictionScope scope = matrixFilter.getScopeEnum();
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		return selectedLearningModel.cloneWithFilter(matrixFilter, logger);
	}

	@Override
	public void onBondNotification(BondEvent event) {
		try {
			Lsa bondedLsa = event.getBondedLsa();
			if(debugLevel>0) {
				logger.info("** LearningAgent.onBondNotification " + agentName + " bonding ** " + bondedLsa.getAgentName());
			}
			lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check

			NodeLocation lsaNodeLocation = bondedLsa.getAgentAuthentication().getNodeLocation();
			EnergyDbHelper.checkNodeLocation(lsaNodeLocation);
			completeInvolvedLocations(bondedLsa, logger);
			Property pEvent = bondedLsa.getOnePropertyByName("EVENT");
			if(debugLevel>0) {
				logger.info("** ** LearningAgent.onBondNotification pEvent = " + pEvent);
			}
			if (pEvent != null && pEvent.getValue() instanceof EnergyEventTable) {
				 EnergyEventTable nextEventTable = (EnergyEventTable) pEvent.getValue();
				 for(EnergyEvent nextEvent : nextEventTable.getEvents()) {
					 handleEvent( nextEvent, bondedLsa);
				 }
			}
			Property pWarning = bondedLsa.getOnePropertyByName("WARNING");
			if (pWarning != null && pWarning.getValue() instanceof RegulationWarning) {
				handleWarning((RegulationWarning) pWarning.getValue(), bondedLsa);
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
		ProsumerProperties evtIssuer = nextEvent.getIssuerProperties();
		evtIssuer.setDistance(issuerDistance);
		if (AgentType.PROSUMER.equals(bondAgentType)) {
			String nextEventKey = nextEvent.getKey();
			if(EventType.CONTRACT_START.equals(nextEvent.getType())) {
				if(nextEvent.isComplementary()) {
					logger.info("handleEvent for debug: receive CONTRACT_START event for complementary need");
				}
			}
			if(!this.mapReceivedEventKeys.containsKey(nextEventKey)) {
				mapReceivedEventKeys.put(nextEventKey, EVENT_INIT_DECAY);
				// Check if the event is local
				NodeLocation issuerLocation1 = evtIssuer.getLocation();
				if(!NodeManager.isLocal(issuerLocation1) && true) {
					String evtNode = issuerLocation1.getName();
					NodeLocation issuerLocation2 = NodeManager.getLocationByName(evtNode);
					if(issuerLocation2 != null) {
						evtIssuer.setLocation(issuerLocation2.clone());
						Contract contract = null;
						if(AgentType.PROSUMER.equals(bondAgentType)) {
							// Get contract from contrat adgent
							Property pMainContract = bondedLsa.getOnePropertyByName("CONTRACT1");
							Property pSdContract = bondedLsa.getOnePropertyByName("CONTRACT2");
							logger.info("For debug : register distant event " + nextEvent);
							if(pMainContract!=null && pMainContract.getValue() instanceof ProtectedContract) {
								ProtectedContract protectedContrat = (ProtectedContract) pMainContract.getValue();
								contract = protectedContrat.getContract(this);
								if(!contract.checkLocation()) {
									bondedLsa = completeInvolvedLocations(bondedLsa, logger);
									logger.error("For debug1 location is not set : mapLocation = "+ contract.getMapLocation());
								}
							} else if(pSdContract!=null && pSdContract.getValue() instanceof ProtectedContract) {
								ProtectedContract protectedContrat = (ProtectedContract) pSdContract.getValue();
								contract = protectedContrat.getContract(this);
								if(!contract.checkLocation()) {
									logger.error("For debug2 map location = "+ contract.getMapLocation());
								}
							}
						}
						EnergyDbHelper.registerEvent2(nextEvent, contract, "handleEvent by " + agentName);
					} else {
						logger.error("handleEvent : event location " + evtNode + " not in mapNeighborNodeLocations " + nextEvent);
					}
				}
				//lastEvent = nextEvent;
				refreshNodeTotal(nextEvent);
				updateTotalProperty();
				//refreshLearningModel(false);
			} else {
				if(debugLevel>0) {
					logger.info("Event already added " + nextEvent);
				}
			}
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

	private void refreshNodeTotal(EnergyEvent event) throws HandlingException {
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
				EnergyDbHelper.generateNodeTotal(minusOne, nodeContext, null, true);
			}
		}
		// Refresh Node total
		//Long idLast = nodeTotal==null? null : nodeTotal.getId();
		nodeTotal = EnergyDbHelper.generateNodeTotal(registerDate, nodeContext, event, false);
		//mapNeighborhoodTotal.clear();
	}



	@Override
	public void onDecayedNotification(DecayedEvent event) {
		try {
			// decayedLsa.toVisualString());
			if(stopped) {
				mapReceivedEventKeys.clear();
				waitingProperties.clear();
				nodeTotal = null;
				learningModel = null;
				learningModelCluster = null;
				this.addDecay(0);
			} else {
				Lsa decayedLsa = event.getLsa();
				// logger.info("onDecayedNotification: decayedLsa = " +
				//retrieveAggregatedProperties();	// already done in onAggregationEvent
				Integer decay = Integer.valueOf("" + decayedLsa.getSyntheticProperty(SyntheticPropertyName.DECAY));
				if (decay < 1) {
					periodicRefresh();
					this.addDecay(nodeContext.isSupervisionDisabled() ? DECAY_SUPERVISION_DISABLED : REFRESH_PERIOD_SEC);
				} else if (decay > REFRESH_PERIOD_SEC && !nodeContext.isSupervisionDisabled()) {
					this.addDecay(REFRESH_PERIOD_SEC);
				}
				updateModelProperty();

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
				// Sumbmit waiting properties
				checkWaitingProperties();
			}
			// For spreading
			activateSpreading();
			//logger.info("mapReceivedEventKeys:" + mapReceivedEventKeys);
		} catch (Throwable e) {
			logger.error(e);
			logger.info("Exception thrown in onDecayedNotification :" + agentName + " " + event + " " + e.getLocalizedMessage());
		}
	}

	@Override
	public void onAggregationEvent(AggregationEvent event) {
		// For debug
		Lsa lsa = event.getLsa();
		List<String> nameOfAggregatedProperties = new ArrayList<String>();
		for(Property property : lsa.getProperties()) {
			if(property.getAggregatedValue() != null) {
				nameOfAggregatedProperties.add(property.getName());
			}
		}
		logger.info("onAggregationEvent:" + agentName + " aggregated value in " + nameOfAggregatedProperties + " property");
		try {
			retrieveAggregatedProperties();
		} catch (HandlingException e) {
			logger.error("onAggregationEvent : " + e);
		}
	}

	public void refreshHistory(EnergyEvent event) throws HandlingException {
		refreshNodeTotal(event);
		updateTotalProperty();
	}

	private Date getLearningModelDate() {
		Date refreshDate = registerDate;
		if(nodeContext.hasDatetimeShift()) {
			forcedCurrentTime = nodeContext.getCurrentDate();
			refreshDate = forcedCurrentTime;
		}
		return refreshDate;
	}
/*
	private Map<String, ILearningModel> retrieveSourceModels(ICompactedModel compactModel, boolean refreshAllMatrices) {
		Map<String, ILearningModel> result = new HashMap<String, ILearningModel>();
		Map<String, IAggregateable> mapSourceObjects = compactModel.getMapSourceObjects();
		for(String agentName : mapSourceObjects.keySet()) {
			IAggregateable nextObj = mapSourceObjects.get(agentName);
			if(nextObj instanceof ICompactedModel) {
				ICompactedModel nextCompactedModel = (ICompactedModel) nextObj;
				ILearningModel nextCompleteModel = nextCompactedModel.generateCompleteModel(logger);
				result.put(agentName, nextCompleteModel);
			}
		}
		return result;
	}
*/

	public AbstractAggregationResult checkupModelAggregation(AggregationCheckupRequest fedAvgCheckupRequest) {
		PredictionScope scope = fedAvgCheckupRequest.getScopeEnum();
		PredictionSetting predictionSetting = nodeContext.getPredictionSetting(scope);
		if (predictionSetting.isActivated()) {
			if (predictionSetting.isModelAggregationActivated()) {
				try {
					ILearningModel selectedModel = selectLearningModel(scope);
					Map<String, ILearningModel> selectedReceivedModels = selectReceivedLearningModels(scope);
					return selectedModel.checkupModelAggregation(fedAvgCheckupRequest, logger, selectedReceivedModels);
				} catch (Throwable e) {
					logger.error(e);
				}
			}
			if (predictionSetting.isPredictionAggregationActivated()) {
				if (aggregatedPredictions.containsKey(scope)) {
					PredictionData aggregatedPrediction = aggregatedPredictions.get(scope);
					if (receivedPredictions.containsKey(scope)) {
						return aggregatedPrediction.checkupAggregation(fedAvgCheckupRequest,
								receivedPredictions.get(scope), logger);
					}
				}
			}
		}
		logger.info("checkupModelAggregation : after loop1");
		return null;
	}

	public void retrieveAggregatedProperties() throws HandlingException {
		if(nodeContext.getClusterPredictionSetting().isActivated()) {
			if(lsa.hasAggregatedValue("TOTAL")) {
				Object value = lsa.getOneAggregatedValue("TOTAL");
				if(value instanceof NodeTotal) {
					NodeTotal aggregatedNodeTotal = (NodeTotal) value;
					Date aggregationDate = aggregatedNodeTotal.getAggregationDate();
					if(aggregatedNodeTotal.isAggregationCompleted() && aggregationDate != null) {
						boolean newAggregation = (clusterTotal.getAggregationDate() == null) || (aggregationDate.after(clusterTotal.getAggregationDate()));
						logger.info("retrieveAggregatedProperties aggregationDate = " + UtilDates.format_time.format(aggregationDate) + ", newAggregation = " + newAggregation);
						if(aggregatedNodeTotal.isAggregationCompleted()) {
							clusterTotal = aggregatedNodeTotal.clone();
							clusterTotal.refreshLastUpdate();
						}
					}
				}
			}
		}
		// Reception of learning model aggregations
		if(nodeContext.getNodePredicitonSetting().isModelAggregationActivated()) {
			if (lsa.hasAggregatedValue("MODEL")) {
				Object value = lsa.getOneAggregatedValue("MODEL");
				if(value instanceof ICompactedModel) {
					ICompactedModel aggregateCompactedModel = (ICompactedModel) value;
					auxUpdateAggregatedModel(aggregateCompactedModel);
				}
			}
		}
		if(nodeContext.getClusterPredictionSetting().isModelAggregationActivated()) {
			if (lsa.hasAggregatedValue("CLUSTER_MODEL")) {
				Object value = lsa.getOneAggregatedValue("CLUSTER_MODEL");
				if(value instanceof ICompactedModel) {
					ICompactedModel aggregateCompactedModel = (ICompactedModel) value;
					auxUpdateAggregatedModel(aggregateCompactedModel);
				}
			}
		}
		// Reception of prediction aggregations
		if (nodeContext.getNodePredicitonSetting().isPredictionAggregationActivated()) {
			if (lsa.hasAggregatedValue("PRED")) {
				Object value = lsa.getOneAggregatedValue("PRED");
				if (value instanceof PredictionData) {
					PredictionData aggregatedPrediction = (PredictionData) value;
					auxUpdateAggregatedPrediction(aggregatedPrediction);
				}
			}
		}
		if (nodeContext.getClusterPredictionSetting().isPredictionAggregationActivated()) {
			if (lsa.hasAggregatedValue("PRED_CLUSTER")) {
				Object value = lsa.getOneAggregatedValue("PRED_CLUSTER");
				if (value instanceof PredictionData) {
					PredictionData aggregatedPrediction = (PredictionData) value;
					auxUpdateAggregatedPrediction(aggregatedPrediction);
				}
			}
		}
	}


	private void auxUpdateAggregatedModel(ICompactedModel aggregateCompactModel) throws HandlingException {
		Date aggregationDate = aggregateCompactModel.getAggregationDate();
		PredictionScope scope = aggregateCompactModel.getPredictionContext().getScope();
		if(aggregateCompactModel.isAggregationCompleted() && aggregationDate != null) {
			ILearningModel learningModelToUpdate = selectModelWithAggregator(scope);
			if(learningModelToUpdate != null) {
				boolean newAggregation = (learningModelToUpdate.getAggregationDate() == null) || (aggregationDate.after(learningModelToUpdate.getAggregationDate()));
				logger.info("retrieveAggregatedProperties aggregationDate = " + UtilDates.format_time.format(aggregationDate) + ", newAggregation = " + newAggregation);
				if(newAggregation) {
					// replace the current model with the aggregated model
					ILearningModel aggregateModel = aggregateCompactModel.generateCompleteModel(logger);
					Map<String, ILearningModel> sourceModels = aggregateCompactModel.retrieveSourceModels(true, logger);
					receivedLearningModels.put(scope, sourceModels);
					/*
					if(PredictionScope.NODE.equals(scope)) {
						receivedLearningModelsNode = retrieveSourceModels(aggregateCompactModel, true);
					} else if(PredictionScope.CLUSTER.equals(scope)) {
						receivedLearningModelsCluster  = retrieveSourceModels(aggregateCompactModel, true);
					}*/
					Map<Object, Boolean> mapChanges = learningModelToUpdate.copyFromOther(aggregateModel, logger);
					logger.info("retrieveAggregatedProperties : model changes = " + mapChanges);
					learningModelToUpdate.setAggregationDate(aggregationDate);
					//PredictionHelper.saveLearningModel(learningModelToUpdate, false,  true);
					learningModelToUpdate.save(false, true, logger);
					AggregationsTracking modelAvailability = retrieveAggregationTrackingFromLSA();
					modelAvailability.addAggregation(scope, aggregationDate);
				}
			}
			//lsa.removePropertiesByName("MODEL_AGGR");
			//lsa.removePropertiesByName("MODEL");
		}
		if(PredictionScope.NODE.equals(scope)) {
			lsa.clearAggredatedValue("MODEL");
		}
		if(PredictionScope.CLUSTER.equals(scope)) {
			lsa.clearAggredatedValue("CLUSTER_MODEL");
		}
	}

	private void auxUpdateAggregatedPrediction(PredictionData aggregatedPrediction) throws HandlingException {
		Date lastAggregationDate = null;
		PredictionScope scope = aggregatedPrediction.getPredictionContext().getScope();
		if (aggregatedPredictions.containsKey(scope)) {
			lastAggregationDate = aggregatedPredictions.get(scope).getAggregationDate();
		}
		if (lastAggregationDate == null || lastAggregationDate.before(aggregatedPrediction.getAggregationDate())) {
			// New aggregation of prediction : save the aggregated prediction
			PredictionDbHelper.savePredictionResult(aggregatedPrediction);
			aggregatedPredictions.put(scope, aggregatedPrediction);

			// Update received predictions
			Map<String, PredictionData> sourcePredictions = aggregatedPrediction.retrieveSourcePrecictions(logger);
			receivedPredictions.put(scope, sourcePredictions);
		}
	}

	public void periodicRefresh() {
		boolean hasActivity = nodeTotal!=null && nodeTotal.hasActivity();
		boolean enableHistoUpdate = (stepNumber>1 );
		boolean generatePrediction = (stepNumber>1) && hasActivity && !nodeContext.isSupervisionDisabled();
		boolean saveTransitionMatrix = (stepNumber>1) && hasActivity && !nodeContext.isSupervisionDisabled();
		//EnergyDbHelper.correctHisto();
		try {
			refreshHistory(null);
		} catch (Exception e) {
			logger.error(e);
		}
		if(nodeContext._isPredictionsActivated()) {
			try {
				retrieveAggregatedProperties();
				// Save next total in node clemap history table
				ClemapDbHelper.addNodeTotal(getPredictionContext(PredictionScope.NODE), nodeTotal);
				if(nodeContext.getNodePredicitonSetting().isActivated()) {
					refreshLearningModel(PredictionScope.NODE, enableHistoUpdate, saveTransitionMatrix);
				}
				if(nodeContext.getClusterPredictionSetting().isActivated()) {
					refreshLearningModel(PredictionScope.CLUSTER, enableHistoUpdate, saveTransitionMatrix);
				}
				if(generatePrediction) {
				    logger.info("Before prediction");
				    boolean useCorrection = false;
				    if(nodeContext.getNodePredicitonSetting().isActivated()) {
						boolean setStateHistory = nodeContext.getNodePredicitonSetting().isPredictionAggregationActivated();
						PredictionData predictionNode = computePrediction(PredictionScope.NODE, LIST_PREDICTION_HORIZON_MINUTES, useCorrection, setStateHistory);
						this.replacePropertyWithName(new Property("PRED", predictionNode));
						logger.info("After prediction");
						// save node prediction result if the aggregation is not activated on predictions
						if(!nodeContext.getNodePredicitonSetting().isPredictionAggregationActivated()) {
							PredictionDbHelper.savePredictionResult(predictionNode);
						}
				    }
				    if (nodeContext.getClusterPredictionSetting().isActivated()) {
						if (clusterTotal.isEmpty()) {
							// No data received to set the total cluster : cannot generate cluster predictions
							int nbNeighbourNodes = 0;
							Map<String, Integer> mapDistances = NodeManager.getMapDistanceByNode();
							for(String node : mapDistances.keySet()) {
								int distance = mapDistances.get(node);
								if(distance > 0) {
									nbNeighbourNodes++;
								}
							}
							logger.warning("ClusterTotal is still empty : total nbNeighbourNodes = " + nbNeighbourNodes);
						} else {
							boolean setStateHistory = nodeContext.getClusterPredictionSetting()
									.isPredictionAggregationActivated();
							PredictionData predictionCluster = computePrediction(PredictionScope.CLUSTER,
									LIST_PREDICTION_HORIZON_MINUTES, useCorrection, setStateHistory);
							this.replacePropertyWithName(new Property("PRED_CLUSTER", predictionCluster));
							if (!nodeContext.getClusterPredictionSetting().isPredictionAggregationActivated()) {
								PredictionDbHelper.savePredictionResult(predictionCluster);
							}
						}
					}
					Date learningModelDate = getLearningModelDate();
					Date dateMax = UtilDates.removeMinutes(learningModelDate);
					Date dateMin = UtilDates.shiftDateMinutes(dateMax,-60);
					// TO DELETE !!!!!!!!
					/*
					int testShift = -5;
					dateMin  = SapereUtil.shiftDateMinutes(dateMin,testShift*60);
					dateMax  = SapereUtil.shiftDateMinutes(dateMax,testShift*60);
					*/
					TimeSlot timeSlot = new TimeSlot(dateMin, dateMax);
					boolean generateSelfCorrections = false;
					if(generateSelfCorrections) {
						List<PredictionCorrection> corrections1 = PredictionHelper.applyPredictionSelfCorrection(learningModel.getPredictionContext(), timeSlot);
						if(corrections1.size() > 0) {
							logger.info("periodicRefresh : " + corrections1.size() + " corrections done on transition matrix following prediction fails");
							// Change has been done transition matdix : refresh transition matrix
							learningModel = loadCompleteModel(learningModel.getPredictionContext().getScope());
						}
						if(nodeContext.getClusterPredictionSetting().isActivated()) {
							List<PredictionCorrection> corrections2 = PredictionHelper.applyPredictionSelfCorrection(learningModelCluster.getPredictionContext(), timeSlot);
							if(corrections2.size() > 0) {
								logger.info("periodicRefresh : " + corrections2.size() + " corrections done on transition matrix following prediction fails");
								// Change has been done transition matdix : refresh transition matrix
								learningModelCluster = loadCompleteModel(learningModelCluster.getPredictionContext().getScope());
							}
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
		if(debugLevel > 0) {
			Map<String, Integer> nbSubscriptionsByAgent = NodeManager.instance().getNotifier().getNbSubscriptionsByAgent();
			for(String agent : nbSubscriptionsByAgent.keySet()) {
				logger.info("     " +  agent  + " : " + nbSubscriptionsByAgent.get(agent));
			}
		}
	}

	public ILearningModel initNodeHistory(HistoryInitializationForm historyInitForm) throws HandlingException{
		List<NodeTotal> listNodeTotal = NodeTotal.getListNodeTotal(historyInitForm);
		PredictionScope scope = historyInitForm.getScopeEnum();
		boolean completeMatrices = historyInitForm.getCompleteMatrices();
		ILearningModel result1 = initNodeHistory(scope, listNodeTotal, completeMatrices);
		return result1;
	}

	public ILearningModel initNodeHistory(HistoryInitializationRequest historyInitRequest) throws HandlingException{
		PredictionContext predictionContext = getPredictionContext(historyInitRequest.getScopeEnum());
		// For makov model : we load all history data has we train it localy (contrary to LSTM model)
		// TODO : add method in learningModel : isTrainedLocally : returns boolean
		//boolean isMC = LearningModelType.MARKOV_CHAINS.equals( predictionContext.getModelType());
		//List<NodeTotal> listNodeTotal =  ClemapDbHelper.loadNodeHistory(predictionContext, (isMC ? null : this.maxHistoryDurationMS));
		boolean loadAllHistory = predictionContext.getModelType().isLearnsLocally();
		List<NodeTotal> listNodeTotal =  ClemapDbHelper.loadNodeHistory(predictionContext, (loadAllHistory ? null : this.maxHistoryDurationMS));
		PredictionScope scope = historyInitRequest.getScopeEnum();
		boolean completeMatrices = historyInitRequest.getCompleteMatrices();
		ILearningModel result1 = initNodeHistory(scope, listNodeTotal, completeMatrices);
		return result1;
	}


	public ILearningModel initNodeHistory(
			PredictionScope scope,
			List<NodeTotal> nodeHistory,
			boolean completeMatrices) throws HandlingException {
		stepNumber = 1;
		ILearningModel selectedModel = selectLearningModel(scope);
		//Integer iterationNumber = 1;
		//Date lastDay = null;
		NodeStates.initialize(nodeContext.getMaxTotalPower());
		// load initial content of learning model
		selectedModel.load(getCurrentDate());
		//selectedModel = PredictionDbHelper.loadCompleteLearningModel(selectedModel, getCurrentDate());
		// initialize learning model with the state history
		selectedModel.initNodeHistory(nodeHistory, completeMatrices, logger);
		// save history state Model history in database
		PredictionDbHelper.saveAllHistoryStates(selectedModel.getPredictionContext(), selectedModel.getStateHistory());
		// save history state in a csv file
		String dumpFolder = selectedModel.getPredictionContext().getDumpHistoryFolder();
		if(dumpFolder != null ) {
			String node = nodeContext.getNodeLocation().getName();
			SapereUtil.dumpAllHistoryStates(logger, dumpFolder, scope, node, getVariables(), selectedModel.getStateHistory());
		}

		// Save node transition matrices in database
		//PredictionHelper.saveLearningModel(selectedModel, false,  true);
		selectedModel.save(false, true, logger);
		selectedModel.load(getCurrentDate());
		//selectedModel = PredictionDbHelper.loadCompleteLearningModel(selectedModel, getCurrentDate());
		setLearningModel(selectedModel);
		stepNumber = 1;
		//updateModelProperty();
		return selectedModel;
	}


	private void updateModelProperty() {
		if(stepNumber >= 1) {
			if(learningModel.getPredictionContext().isAggregationsActivated()) {
				if(sendModelForAggregation(PredictionScope.NODE)) {
					ICompactedModel compactedModel = learningModel.generateCompactedModel(logger);
					this.replacePropertyWithName(new Property("MODEL", compactedModel));
				} else {
					//remove property
					lsa.removePropertiesByName("MODEL");
				}
			}
			if(learningModelCluster.getPredictionContext().isAggregationsActivated()) {
				if(sendModelForAggregation(PredictionScope.CLUSTER)) {
					ICompactedModel compactedModel = learningModelCluster.generateCompactedModel(logger);
					this.replacePropertyWithName(new Property("CLUSTER_MODEL", compactedModel));
				} else {
					//remove property
					lsa.removePropertiesByName("CLUSTER_MODEL");
				}
			}
		}
	}

	private void updateTotalProperty() {
		if(nodeTotal == null) {
			return;
		}
		try {
			Date dateLastNodeTotal = null;
			Object objTotal = lsa.getOnePropertyValueByName("TOTAL");
			if(objTotal instanceof NodeTotal) {
				NodeTotal lastNodeTotal = (NodeTotal) objTotal;
				dateLastNodeTotal = lastNodeTotal.getDate();
			}
			// check if nodeTotal is more recent than lastNodeTotal;
			if(dateLastNodeTotal == null || nodeTotal.getDate().after(dateLastNodeTotal)) {
				replacePropertyWithName(new Property("TOTAL", nodeTotal));
			}
		} catch (Throwable e) {
			logger.error(e);
		}
	}

	private CompleteLSTMModel initLSTMmodel(PredictionContext predictionContext) {
		String rootUrlLSTM = nodeContext.getUrlForcasting();// "http://127.0.0.1:5000/";
		LSTMModelInfoRequest infoRequest = new LSTMModelInfoRequest();
		String nodeName = predictionContext.getNodeLocation().getName();
		infoRequest.setNodeName(nodeName);
		infoRequest.setScope(predictionContext.getScope());
		infoRequest.setListVariables(nodeContext.getVariables());
		String urlModelInfo = rootUrlLSTM + "model_info";
		try {
			String postResponse1 =  UtilHttp.sendPostRequest(urlModelInfo, infoRequest, logger, debugLevel);
			JSONArray jsonListModelInfo = new JSONArray(postResponse1);
			//logger.info("jsonModelInfo = " + jsonModelInfo);
			List<LSTMModelInfo> listModelInfo = UtilJsonParser.parseListLSTMModelInfo(jsonListModelInfo, logger);
			//logger.info("modelInfo = " + modelInfo);
			CompleteLSTMModel completeLSTMModel = new CompleteLSTMModel(predictionContext, listModelInfo);
			completeLSTMModel.setPredictionContext(predictionContext);
			logger.info("completeLSTMModel = " + completeLSTMModel);
			return completeLSTMModel;
		} catch (HandlingException e) {
			logger.error(e);
		}
		return null;
	}



	public void refreshLearningModel(PredictionScope scope, boolean enableHistoUpdate, boolean enableObsUpdate) throws HandlingException {
		// Refresh learning model : only when no specific event (ie at periodic refresh)
		// Use eight the node total of all nodes or the node total of the current node.
		NodeTotal usedNodeTotal = selectNodeTotal(scope);
		if(usedNodeTotal.isEmpty()) {
			// do nothing
			return;
		}
		ILearningModel selectedModel = selectLearningModel(scope);
		boolean updateted = false;
		updateted = selectedModel.refreshModel(usedNodeTotal, enableObsUpdate, logger);
		NodeStatesTransitions nodeStateTransitions = selectedModel.getNodeStatesTransitions();
		int idLastx0 = selectedModel.getStateHistory().size() -1;
		logger.info("refreshLearningModel lastDate in model(1)= " + UtilDates.format_time.format(selectedModel.getStateHistory().get(idLastx0).getDate()));

		// TODO : add newSingleNodeStateItem in slectedModel
		selectedModel.cleanHistory(maxHistoryDurationMS, logger);

		// For debug : check history dates
		int idLastx1 = selectedModel.getStateHistory().size() -1;
		logger.info("refreshLearningModel lastDate in model(2)= " + UtilDates.format_time.format(selectedModel.getStateHistory().get(idLastx1).getDate()));
		logger.info("refreshLearningModel current date        = " + UtilDates.format_time.format(nodeContext.getCurrentDate()));
		logger.info("refreshLearningModel nodeTotal date      = " + UtilDates.format_time.format(usedNodeTotal.getDate()));

		// Set states index in history
		if(enableHistoUpdate) {
			PredictionDbHelper.saveHistoryStates(nodeStateTransitions, selectedModel.getPredictionContext());
		}

		if(enableObsUpdate) {
			// Save node transition matrices in databse
			if(updateted) {
				//PredictionHelper.saveLearningModel(selectedModel, true,  true);
				selectedModel.save(true, true, logger);
				selectedModel.refreshLastUpdate();
			}
		}
		if(selectedModel.hasModelAggregator() && selectedModel.isComplete()) {
			if(selectedModel.getLastUpdate() == null) {
				selectedModel.refreshLastUpdate();
			}
			if(selectedModel.getStateHistory().size() > 1000 ) {
				selectedModel.cleanHistory(maxHistoryDurationMS, logger);
			}
		}
		setLearningModel(selectedModel);
		refreshAggregationTrackingProperty(selectedModel);

		if(debugLevel>0) {
			logger.info("nodeStateTransitions = " + nodeStateTransitions);
		}
	}

	public PredictionData computePrediction(PredictionScope scope, int[] listHorizonMinutes, boolean useCorrections, boolean setStateHistory) {
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		List<Date> targetDates = new ArrayList<Date>();
		Date current = getCurrentDate();
		Date initialDate = selectedLearningModel.getNodeStatesTransitions().getStateDate();
		if(Math.abs(initialDate.getTime() - current.getTime()) <= 60*1000) {
			current = initialDate;
		} else {
			SingleNodeStateItem closestItem = selectedLearningModel.getClosestStateItem(current, logger);
			if(Math.abs(closestItem.getDate().getTime() - current.getTime()) <= 60*1000) {
				current = closestItem.getDate();
			}
		}
		long msRemain = current.getTime() % 1000;
		Date current2 = new Date(current.getTime() - msRemain);
		for(Integer horizonMinute : listHorizonMinutes) {
			targetDates.add(UtilDates.shiftDateMinutes(current2, horizonMinute));
		}
		PredictionData result2 = null;
		try {
			result2 = selectedLearningModel.computePrediction(
					current2, targetDates, getVariables(), useCorrections, logger);
			if(setStateHistory) {
				result2.setStateHistory(selectedLearningModel.getStateHistory());
			}
		} catch (HandlingException e) {
			logger.error(e);
		}
		return result2;
	}

	public List<VariableStateHistory> retrieveLastHistoryStates(
			 PredictionScope scope
			,Date minCreationDate
			,String variableName
			,boolean observationUpdated) throws HandlingException {
		ILearningModel selectedModel = selectLearningModel(scope);
		PredictionContext predictionContext = selectedModel.getPredictionContext();
		return PredictionDbHelper.retrieveLastHistoryStates(predictionContext, selectedModel.getNodeStatesTransitions(), minCreationDate,  variableName, observationUpdated);
	}

	public VariableState getCurrentVariableState(PredictionScope scope, String variable) {
		ILearningModel selectedLearningModel = selectLearningModel(scope);
		return selectedLearningModel.getCurrentVariableState(variable);
	}

	public List<PredictionStatistic> computePredictionStatistics(StatisticsRequest statisticsRequest) throws HandlingException {
		List<PredictionStatistic> result = new ArrayList<PredictionStatistic>();
		int nodeCount = NodeManager.getAllNodesCount();
		String clusterConfig = "" + nodeCount + "N";
		if (nodeContext.isPredictionsActivated(statisticsRequest.getScopeEnum())) {
			try {
				PredictionContext predictionContext = getPredictionContext(statisticsRequest.getScopeEnum());
				Map<String, PredictionStatistic> mapStatistics = PredictionHelper.computePredictionStatistics(predictionContext, statisticsRequest);
				for (PredictionStatistic nextStatistic : mapStatistics.values()) {
					nextStatistic.setClusterConfig(clusterConfig);
					result.add(nextStatistic);
				}
				Collections.sort(result, new Comparator<PredictionStatistic>() {
					public int compare(PredictionStatistic predictionStatistic1,
							PredictionStatistic predictionStatistic2) {
						return predictionStatistic1.compareTo(predictionStatistic2);
					}
				});
			} catch (Exception e) {
				logger.error(e);
				throw e;
			}
		}
		return result;
	}


	public void stopAgent() {
		stopped = true;
	}

	public PredictionData computePrediction2(PredictionRequest predictionRequest) throws HandlingException {
		PredictionScope scope = predictionRequest.getScopeEnum();
		ILearningModel learningModel = selectLearningModel(scope);
		PredictionData result = learningModel.computePrediction2(predictionRequest, logger);
		return result;
	}

	public MultiPredictionsData generateMassivePredictions(MassivePredictionRequest massivePredictionRequest) throws HandlingException {
			PredictionScope scope = massivePredictionRequest.getScopeEnum();
			ILearningModel learningModel = selectLearningModel(scope);
			return learningModel.generateMassivePredictions(massivePredictionRequest, logger);
	}

	public EndUserForcastingResult getForcasting(EndUserForcastingRequest endUserForcastingRequest) throws HandlingException {
		int nbTValues = endUserForcastingRequest.getSamplingNb();
		Date date = endUserForcastingRequest.getTimestamp();
		EndUserForcastingResult result = new EndUserForcastingResult();
		Date horizonDate = UtilDates.shiftDateMinutes(date, 60);
		List<Date> listDates = new ArrayList<>();
		for (int i = 0; i <4; i++) {
			listDates.add(UtilDates.shiftDateMinutes(horizonDate, -i*15));
		}
		List<Double> realValues = new ArrayList<>();
		Collections.sort(listDates);
		List<String> datesWithNoValue = new ArrayList<>();
		Map<Date, TimestampedValue> map_realvalues = EnergyDbHelper.getInstance().retrieveValuesByDates(listDates);
		for(Date nextDate : listDates) {
			if(map_realvalues.containsKey(nextDate)) {
				TimestampedValue toAdd = map_realvalues.get(nextDate);
				realValues.add(toAdd.getValue());
			} else {
				datesWithNoValue.add(UtilDates.format_sql.format(nextDate));
				realValues.add(-99.);
			}
		}
		List<Double> errorPredictedValues = Arrays.asList(-99., -99., -99., -99.);
		if(datesWithNoValue.size() > 0) {
			String sep="";
			String errors =  "no value found at ";
			for(String nextDate : datesWithNoValue) {
				errors+= sep + nextDate ;
				sep = " , ";
			}
			errors+= " . Check the csv file.";
			result.setPredicetedValues(errorPredictedValues);
			result.setErrorMessage(errors);
		}
		List<TimestampedValue> real_tvalues2 = EnergyDbHelper.getInstance().retrieveLastValues(horizonDate, 4);
		List<Double> realValues2 = new ArrayList<>();
		List<Date> timestamps = new ArrayList<>();
		int idxValue = 0;
		if(datesWithNoValue.size() == 0) {
			for (TimestampedValue tValue2: real_tvalues2) {
				realValues2.add(tValue2.getValue());
				timestamps.add(tValue2.getTimestamp());
				double value2 = realValues.get(idxValue);
				if(Math.abs(tValue2.getValue() - value2) > 0.0001) {
					logger.error("Detla between value and value2 at " + UtilDates.format_sql.format(tValue2.getTimestamp()));
				}
				idxValue++;
			}
		}
		// set realValues = value at T + 1H
		result.setTimestamps(listDates);
		result.setRealValues(realValues);
		//result.setTimestamps(timestamps);
		if(datesWithNoValue.size() == 0) {
			List<TimestampedValue> sample_tvalues = EnergyDbHelper.getInstance().retrieveLastValues(date, nbTValues);
			try {
				ForcastingResult1 forcasting = callForcastingService(sample_tvalues);
				result.setPredicetedValues(forcasting.getPredictions());
			} catch (Exception e) {
				logger.error(e);
				result.setPredicetedValues(errorPredictedValues);
				result.setErrorMessage(e.getMessage());
			}
		}
		return result;
	}

	public ForcastingResult1 callForcastingService(List<TimestampedValue> values) throws HandlingException {
		ForcastingResult1 result = new ForcastingResult1();
		ForcastingRequest forcastingRequest = new ForcastingRequest();
	    forcastingRequest.setTimestampedValues(values);
		Map<String, Double> mapForcastingRequest = forcastingRequest.getMap();
	    String urlForcasting = nodeContext.getUrlForcasting();
	    StringBuffer sParams = UtilJsonParser.toJsonStr(forcastingRequest, logger, 0);
		StringBuffer sParams2 = UtilJsonParser.toJsonStr(mapForcastingRequest, logger, 0);
		logger.info("callForcastingService sParams2 = " + sParams2);
		/*
		String baseForcastingUrl = nodLocation.getUrl();//  "http://localhost:1234/";
		//StringBuffer jsonContent = UtilJsonParser.toJsonStr(forcastingRequest, logger, 0);
		String hostname = InetAddress.getLocalHost().getHostName();
		String nodeLocationHostname = nodeLocation.getHost();
		logger.info("callForcastingService : hostname = " + hostname + ", nodeLocationHostname = " + nodeLocationHostname);			*/
		//boolean sendRequest = false;
		/*
		String quote= "\"";
		String testResonse="{" + quote +  "predictions" + quote + ":[[-0.02749158814549446,-0.0414426252245903,0.06858451664447784,0.07378074526786804]]}";
		JSONObject testJsonObjet = new JSONObject(testResonse);
		result = UtilJsonParser.parseForcastingResult(testJsonObjet, logger);
		*/
		if(urlForcasting == null) {
			logger.info("call generateMockForcasting : params = " + sParams.toString());
			result = generateMockForcasting1(mapForcastingRequest);
		} else {
			String postResponse = null;
			try {
				logger.info("send POST request to urlForcasting service " + urlForcasting + " params=" + sParams2.toString());
				postResponse =  UtilHttp.sendPostRequest(urlForcasting, mapForcastingRequest, logger, debugLevel);
			}
			catch (Throwable e) {
				logger.error(e);
				HandlingException e2 = new HandlingException("Error returned during the call of web-service " + SapereUtil.addDoubleQuote(urlForcasting)  + ": " + e);
				throw e2;
			}
			if (postResponse == null) {
				logger.warning("callForcastingService :postResponse is null ");
				new Exception("no output returned by the web-service " + urlForcasting);
			}
			try {
				logger.info("callForcastingService : postResponse = " + postResponse);
				JSONObject jsonForcastingResult = new JSONObject(postResponse);
				logger.info("callForcastingService : jsonForcastingResult = " + jsonForcastingResult);
				result = UtilJsonParser.parseForcastingResult1(jsonForcastingResult, logger);
				if(result.getPredictions() != null && result.getPredictions().size() < 4) {
					ForcastingResult2 result2 = UtilJsonParser.parseForcastingResult2(jsonForcastingResult, logger);
					List<Double> values2 = result2.getFlatList();
					if(values2.size() == 4) {
						logger.info("callForcastingService : the forcasting service has returned 2-dimensions array : " + result2.getPredictions());
						result.setPredictions(values2);
					}
				}
			} catch (Throwable e) {
				new Exception("Error during the json parsing of the output returned by " + urlForcasting +  ":" + e, e);
			}
			logger.info("callForcastingService : result = " + result);
		}
		return result;
	}

	public ForcastingResult1 generateMockForcasting1(Map<String, Double> forcastingRequest) {
		ForcastingResult1 result = new ForcastingResult1();
		List<Double> mainList = Arrays.asList(0.123, 1.234, 2.345, 3.456);
		result.setPredictions(mainList);
		return result;
	}

	public ForcastingResult2 generateMockForcasting2(Map<String, Double> forcastingRequest) {
		ForcastingResult2 result = new ForcastingResult2();
		List<Double> subList = new ArrayList<>();
		subList = Arrays.asList(0.123, 1.234, 2.345, 3.456);
		List<List<Double>> mainList = new ArrayList<>();
		mainList.add(subList);
		result.setPredictions(mainList);
		return result;
	}
}
