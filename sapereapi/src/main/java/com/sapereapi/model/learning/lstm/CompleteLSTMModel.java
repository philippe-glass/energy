package com.sapereapi.model.learning.lstm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.input.HistoryInitializationRequest;
import com.sapereapi.model.learning.AbstractLearningModel;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.aggregation.LSTMAggregationResult;
import com.sapereapi.model.learning.lstm.request.LSTMPredictionRequest;
import com.sapereapi.model.learning.lstm.request.LSTMUpdateWeightsRequest;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.IPropertyObject;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class CompleteLSTMModel extends AbstractLearningModel implements ILearningModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String, VariableLSTMModel> mapModels = new HashMap<String, VariableLSTMModel>();
	//private Date lastSubmitedHistoDate = null;
	private int nbHistoToSubmit = 0;

	public CompleteLSTMModel(PredictionContext aPredictionContext) {
		predictionContext = aPredictionContext;
		mapModels.clear();
	}

	public Map<String, VariableLSTMModel> getMapModels() {
		return mapModels;
	}

	public CompleteLSTMModel(PredictionContext aPredictionContext, List<LSTMModelInfo> listModelInfo) throws HandlingException {
		predictionContext = aPredictionContext;
		for (LSTMModelInfo modelInfo : listModelInfo) {
			mapModels.put(modelInfo.getVariable(), new VariableLSTMModel(modelInfo));
		}
	}

	public VariableLSTMModel getModel(String variable) {
		if (mapModels.containsKey(variable)) {
			return mapModels.get(variable);
		}
		return null;
	}

	public void putModel(String variable, VariableLSTMModel model) {
		mapModels.put(variable, model);
	}

	public Map<String, DoubleMatrix> forward_propagate_full(Map<String, DoubleMatrix> map_X) throws HandlingException {
		Map<String, DoubleMatrix> result = new HashMap<String, DoubleMatrix>();
		for (String variable : map_X.keySet()) {
			DoubleMatrix X = map_X.get(variable);
			if (mapModels.containsKey(variable)) {
				VariableLSTMModel model = mapModels.get(variable);
				result.put(variable, model.forward_propagate_full(X));
			}
		}
		return result;
	}

	@Override
	public IAggregateable aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		Map<String, CompleteLSTMModel> mapModels = new HashMap<String, CompleteLSTMModel>();
		for(String node : mapObjects.keySet()) {
			IAggregateable nextObj= mapObjects.get(node);
			if(nextObj instanceof CompleteLSTMModel) {
				mapModels.put(node, (CompleteLSTMModel) nextObj);
			}
		}
		return aggregate2(operator, mapModels, agentAuthentication, logger);
	}

	private static  Map<String, ILearningModel> toMapLearningModel(Map<String, CompleteLSTMModel> mapLSTMModels) {
		Map<String, ILearningModel> result = new HashMap<String, ILearningModel>();
		for(String key : mapLSTMModels.keySet()) {
			CompleteLSTMModel nextModel = mapLSTMModels.get(key);
			result.put(key, nextModel);
		}
		return result;
	}

	public static CompleteLSTMModel aggregate2(String operator, Map<String, CompleteLSTMModel> mapLstmModels,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		String localAgent = agentAuthentication.getAgentName();
		CompleteLSTMModel firstLSTMModel = null;
		CompleteLSTMModel result = null;
		if (mapLstmModels.containsKey(localAgent)) {
			firstLSTMModel = mapLstmModels.get(localAgent);
			try {
				result = new CompleteLSTMModel(firstLSTMModel.getPredictionContext(), new ArrayList<LSTMModelInfo>());
			} catch (HandlingException e) {
				logger.error(e);
			}
			result.setCurrentFeaturesKey(firstLSTMModel.getCurrentFeaturesKey());
			result.setPredictionContext(firstLSTMModel.getPredictionContext().copyContent(false));
		} else {
			logger.warning("CompleteLSTMModel.aggregate2 : local node " + localAgent + " not in aggregation source");
			return null;
		}
		Map<String, Map<String, Double>> weightsTable = evaluateModelsWeigts(toMapLearningModel(mapLstmModels),
				agentAuthentication, result.getPredictionContext(), operator, logger);
		if(weightsTable.isEmpty()) {
			logger.error("CompleteLSTMModel.aggregate2 : the computed weightsTable is empty");
		}
		result.setAggregationWeights(weightsTable);
		for (String variable : result.getPredictionContext().getNodeContext().getVariables()) {
			Map<String, VariableLSTMModel> mapModels = new HashMap<String, VariableLSTMModel>();
			if(weightsTable.containsKey(variable)) {
				Map<String, Double> varWeightTable = weightsTable.get(variable);
				for (String agentName : mapLstmModels.keySet()) {
					CompleteLSTMModel nextModel = mapLstmModels.get(agentName);
					VariableLSTMModel varModel = nextModel.getModel(variable);
					mapModels.put(agentName, varModel);
				}
				VariableLSTMModel aggregatedModel = null;
				try {
					aggregatedModel = VariableLSTMModel.auxAggregate(mapModels, varWeightTable, agentAuthentication,
							logger);
				} catch (HandlingException e) {
					logger.error(e);
				}
				result.putModel(variable, aggregatedModel);
			} else {
				logger.error("CompleteLSTMModel.aggregate2 : variable " + variable + " not found in weightsTable " + weightsTable);
			}
		}
		return result;
	}

	@Override
	public IPropertyObject copyForLSA(AbstractLogger logger) {
		PredictionContext predictionContextCopy = this.predictionContext.copyContent(false);
		CompleteLSTMModel result = new CompleteLSTMModel(predictionContextCopy);
		for(String variable : mapModels.keySet()) {
			VariableLSTMModel model = mapModels.get(variable);
			result.putModel(variable, model.copy(logger));
		}
		return result;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getNbOfObservations(String variable) {
		if(mapModels.containsKey(variable)) {
			return mapModels.get(variable).getSamplingNb();
		}
		return 0;
	}

	@Override
	public PredictionData computeVariablePrediction(NodeStatesTransitions aNodeStateTransitions,
			PredictionData predictionData, String variable, AbstractLogger logger) throws HandlingException {
		if (mapModels.containsKey(variable)) {
			int depthX = 10;
			int depthY = 10;
			//int len = stateHistory.size();
			int nbOfItems = 3;
			//Integer[] horizons = { 5, 10, 30, 60 };
			// List<SingleNodeStateItem> nodeStateItems2 =
			// stateHistory.subList(len-nbOfItems-depth, len);
			VariableLSTMModel model = mapModels.get(variable);
			PredictionScope scope = predictionContext.getScope();
			String nodeName = predictionContext.getNodeLocation().getName();
			List<Integer> listHorizons = predictionData.computeHorizons();
			Integer[] arrayHorizons = listHorizons.toArray(new Integer[0]);
			LSTMPredictionRequest lstmRequest = SingleNodeStateItem.preparePredictParameters1(stateHistory,
					predictionData.getInitialDate(), depthX, depthY, nbOfItems, nodeName, scope, variable, arrayHorizons,
					logger);
			predictionData = model.computeVariablePrediction(lstmRequest, predictionContext, predictionData, variable,
					logger);
		}
		return predictionData;
	}

	@Override
	public void initialize(Date aDate, PredictionContext aPredictionContext, AbstractLogger logger) {
		// TODO Auto-generated method stub

	}

	@Override
	public ILearningModel cloneWithFilter(MatrixFilter matrixFilter, AbstractLogger logger) {
		PredictionContext predictionContextCopy = this.predictionContext.clone();
		CompleteLSTMModel result = new CompleteLSTMModel(predictionContextCopy);
		result.setLastUpdate(lastUpdate);
		String variableFilter = matrixFilter.getVariableName();
		boolean noVarFilter = variableFilter == null || "".equals(variableFilter);
		for(String variable : mapModels.keySet()) {
			if(noVarFilter || variableFilter.equals(variable)) {
				VariableLSTMModel model = mapModels.get(variable);
				result.putModel(variable, model.copy(logger));
			}
		}
		return result;
	}

	@Override
	public Map<Object, Boolean> copyFromOther(ILearningModel other, AbstractLogger logger) {
		//this.computeDate = other.getComputeDate();
		super.copyAggregationWeigtsFomOther(other);
		Map<Object, Boolean> mapHasChanged = new HashMap<Object, Boolean>();
		//boolean hasChanged = false;
		if(other instanceof CompleteLSTMModel) {
			CompleteLSTMModel otherLstmModel = (CompleteLSTMModel) other;
			for (String variable : mapModels.keySet()) {
				VariableLSTMModel model = mapModels.get(variable);
				VariableLSTMModel varModelOther = otherLstmModel.getModel(variable);
				if(varModelOther != null) {
					try {
						boolean nextHasChanged = model.copyFromOther(varModelOther);
						mapHasChanged.put(variable, nextHasChanged);
					} catch (HandlingException e) {
						logger.error(e);
					}
				} else {
					logger.warning("copyFromOther : other LSTM model does not exist for scope " + predictionContext.getScope() + " and variable " + variable);
				}
			}
		}
		return mapHasChanged;
	}

	@Override
	public void initNodeHistory(List<NodeTotal> nodeHistory, boolean completeMatrices, AbstractLogger logger) {
		// Date lastDay = null;
		NodeTotal firstTotal = nodeHistory.get(0);
		Date firstDate = firstTotal.getDate();
		nodeStateTransitions = new NodeStatesTransitions(firstDate);
		this.stateHistory.clear();
		currentFeaturesKey = null;
		for (NodeTotal nextNodeTotal : nodeHistory) {
			// Date nextDate = nextNodeTotal.getDate();
			// Date nextDay = UtilDates.removeTime(nextDate);
			// logger.info("CompleteLSTMModel.initNodeHistory nextDate
			// = " + UtilDates.format_sql.format(nextDate));
			super.auxRefreshTransitions(nextNodeTotal, completeMatrices, logger);
			// lastDay = nextDay;
		}
		// Try to call web service of LSTM
		// call remote initNodehistory
		int debugLevel = 0;
		// PredictionScope scope = historyInitForm.getScopeEnum();
		NodeContext nodeContext = predictionContext.getNodeContext();
		String urlInitNodeHistory = nodeContext.getUrlForcasting() + "init_node_history";
		HistoryInitializationRequest historyInitRequest = generateHistoryInitReuqest();
		try {
			String postResponse1 = UtilHttp.sendPostRequest(urlInitNodeHistory, historyInitRequest, logger, debugLevel);
			// logger.info("postResponse1 = " + postResponse1);
			JSONArray jsonListModelInfo = new JSONArray(postResponse1);
			List<LSTMModelInfo> listModelInfo = UtilJsonParser.parseListLSTMModelInfo(jsonListModelInfo, logger);
			for (LSTMModelInfo modelInfo : listModelInfo) {
				mapModels.put(modelInfo.getVariable(), new VariableLSTMModel(modelInfo));
			}
			/*
			if(stateHistory.size() > 0) {
				int idxLast = stateHistory.size()-1;
				SingleNodeStateItem lastStateItem = stateHistory.get(idxLast);
				lastSubmitedHistoDate = lastStateItem.getDate();
				nbHistoToSubmit++;
				Date current = predictionContext.getCurrentDate();
				if(lastSubmitedHistoDate.after(current)) {
					lastSubmitedHistoDate = current;
				}
			}
			*/
		} catch (Throwable e) {
			logger.error(e);
		}
		logger.info("CompleteLSTMModel.initNodeHistory : end ");
	}

	@Override
	public boolean refreshModel(NodeTotal nodeTotal, boolean enableObsUpdate, AbstractLogger logger) {
		boolean keyHasChanged = auxRefreshTransitions(nodeTotal, enableObsUpdate, logger);
		if (keyHasChanged) {
			logger.info("refreshModel : keyHasChanged : " + this.getCurrentFeaturesKey());
		}
		// To submit to the server
		int minLengthHeadToSumbmit = 100;// 20;// 300;
		/*
		// history to add in the list to submit, according to the deepness parameter
		// (10)
		List<SingleNodeStateItem> historyToSubmitPrevious = new ArrayList<SingleNodeStateItem>();
		List<SingleNodeStateItem> historyToSubmitHead = new ArrayList<SingleNodeStateItem>();
		for (int histoIdx = stateHistory.size() - 1; histoIdx >= 0; histoIdx--) {
			SingleNodeStateItem nextItem = stateHistory.get(histoIdx);
			if (lastSubmitedHistoDate == null || nextItem.getDate().after(lastSubmitedHistoDate)) {
				// Put the new values in the head size
				historyToSubmitHead.add(nextItem.clone());
			} else if (historyToSubmitPrevious.size() < 10) {
				// put the 10 following values in the tail part
				historyToSubmitPrevious.add(nextItem.clone());
			} else {
				// do nothing
			}
		}
		logger.info("CompactLSTMModel.refreshModel.refreshModel : head to sumbit size = " + historyToSubmitHead.size() + ", tail to submit size = " + historyToSubmitPrevious.size());
		*/
		boolean modelUpdated = false;
		nbHistoToSubmit++;
		if(nbHistoToSubmit >= minLengthHeadToSumbmit) {
			HistoryInitializationRequest historySubmitForm = generateHistoryInitReuqest();
			NodeContext nodeContext = predictionContext.getNodeContext();
			int debugLevel = 0;
			String urlAddNodeHistory = nodeContext.getUrlForcasting() + "add_node_history";
			try {
				logger.info("CompleteLSTMModel.refreshModel : before call to " + urlAddNodeHistory);
				String postResponse1 = UtilHttp.sendPostRequest(urlAddNodeHistory, historySubmitForm, logger, debugLevel);
				logger.info("CompleteLSTMModel.refreshModel : after call to " + urlAddNodeHistory);
				JSONArray jsonListModelInfo = new JSONArray(postResponse1);
				List<LSTMModelInfo> listModelInfo = UtilJsonParser.parseListLSTMModelInfo(jsonListModelInfo, logger);
				for (LSTMModelInfo modelInfo : listModelInfo) {
					mapModels.put(modelInfo.getVariable(), new VariableLSTMModel(modelInfo));
				}
				nbHistoToSubmit = 0;
				modelUpdated = true;
			} catch (Throwable e) {
				logger.error(e);
			}
		}
		/*
		if(historyToSubmitHead.size() >= minLengthHeadToSumbmit && historyToSubmitPrevious.size() >= 10) {
			Collections.reverse(historyToSubmitPrevious);
			Collections.reverse(historyToSubmitHead);
			List<SingleNodeStateItem> historyToSubmit = historyToSubmitPrevious;
			historyToSubmit.addAll(historyToSubmitHead);
			// Ensure that all items are in the correct order
			Collections.sort(historyToSubmit, new Comparator<SingleNodeStateItem>() {
				@Override
				public int compare(SingleNodeStateItem o1, SingleNodeStateItem o2) {
					return o1.getDate().compareTo(o2.getDate());
				}
			});
			HistoryInitializationForm historySubmitForm = SingleNodeStateItem.initHistoryForm(
					historyToSubmit, predictionContext);
			NodeContext nodeContext = predictionContext.getNodeContext();
			int debugLevel = 0;
			String urlAddNodeHistory = nodeContext.getUrlForcasting() + "add_node_history";
			try {
				logger.info("CompleteLSTMModel.refreshModel : before call to " + urlAddNodeHistory);
				String postResponse1 = UtilHttp.sendPostRequest(urlAddNodeHistory, historySubmitForm, logger, debugLevel);
				logger.info("CompleteLSTMModel.refreshModel : after call to " + urlAddNodeHistory);
				JSONArray jsonListModelInfo = new JSONArray(postResponse1);
				List<LSTMModelInfo> listModelInfo = UtilJsonParser.parseListLSTMModelInfo(jsonListModelInfo, logger);
				for (LSTMModelInfo modelInfo : listModelInfo) {
					mapModels.put(modelInfo.getVariable(), new VariableLSTMModel(modelInfo));
				}
				int idxLast = historyToSubmitHead.size()-1;
				SingleNodeStateItem lastStateItem = historyToSubmitHead.get(idxLast);
				lastSubmitedHistoDate = lastStateItem.getDate();
				modelUpdated = true;
			} catch (Throwable e) {
				logger.error(e);
			}
		}*/
		try {
			// set observation_update on state_history
			PredictionDbHelper.setObservationUpdates(predictionContext);
		} catch (HandlingException e) {
			logger.error(e);
		}
		return modelUpdated;
	}


	@Override
	public boolean isComplete() {
		if(mapModels.size()==0) {
			return false;
		}
		for(String variable : predictionContext.getNodeContext().getVariables()) {
			if(mapModels.containsKey(variable)) {
				VariableLSTMModel model = mapModels.get("requested");
				if (model.getLayers().size()==0) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for(String variable : mapModels.keySet()) {
			result.append(variable).append(":").append(mapModels.get(variable));
		}
		return result.toString();
	}

	@Override
	public List<String> getUsedVariables() {
		List<String> result = new ArrayList<String>();
		for (String variable : mapModels.keySet()) {
			result.add(variable);
		}
		return result;
	}

	public INNLayer getLayer(String variable, String layerId) {
		if (mapModels.containsKey(variable)) {
			VariableLSTMModel model = mapModels.get(variable);
			return model.getLayer(layerId);
		}
		return null;
	}

	@Override
	public CompactLSTMModel generateCompactedModel(AbstractLogger logger) {
		CompactLSTMModel result = new CompactLSTMModel();
		super.fillCompactModel(result, logger);
		if (mapModels.size() > 0) {
			VariableLSTMModel firstModel = mapModels.values().iterator().next();
			result.setSamplingNb(firstModel.getSamplingNb());
		}
		for (String variable : mapModels.keySet()) {
			VariableLSTMModel varModel = mapModels.get(variable);
			Map<String, String> zippedContent = varModel.getZippedContent();
			result.addZippedContent(variable, zippedContent);
			List<String> zippedLayerDefinitions = new ArrayList<String>();
			List<LayerDefinition> layerDefinitions = varModel.getCopyOfLayerDefinition();
			for (LayerDefinition nextLayerDefinition : layerDefinitions) {
				zippedLayerDefinitions.add(nextLayerDefinition.zip());
				result.putListLayerDefinitions(variable, zippedLayerDefinitions);
			}
		}
		result.checkUp(logger);
		return result;
	}

	public void updateModelWeightsOnServer(AbstractLogger logger) {
		LSTMUpdateWeightsRequest updateWeightsRequest = generateUpdateWeightsRequest();
		NodeContext nodeContext = predictionContext.getNodeContext();
		String urlUpdateWeight = nodeContext.getUrlForcasting() + "update_model_weights";
		int debugLevel = 0;
		try {
			String postResponse1 = UtilHttp.sendPostRequest(urlUpdateWeight, updateWeightsRequest, logger, debugLevel);
			// logger.info("postResponse1 = " + postResponse1);
			JSONObject jsonResponse = new JSONObject(postResponse1);
			Map<String, Boolean> mapResult = UtilJsonParser.parseJsonMapBoolean(jsonResponse, logger);
			for (String variable : mapResult.keySet()) {
				logger.info("next result " + variable + " : " + mapResult.get(variable));
			}
		} catch (HandlingException e) {
			logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public HistoryInitializationRequest generateHistoryInitReuqest() {
		HistoryInitializationRequest historyInitRequest = HistoryInitializationRequest.generateHistoryInitReuqest(predictionContext);
		return historyInitRequest;
	}

	public LSTMUpdateWeightsRequest generateUpdateWeightsRequest() {
		LSTMUpdateWeightsRequest request = new LSTMUpdateWeightsRequest();
		request.setNodeName(predictionContext.getNodeLocation().getName());
		request.setScope(predictionContext.getScope());
		Map<String, Map<String, DoubleMatrix >> mapModeleWeights = new HashMap<String, Map<String,DoubleMatrix>>();
		for(String variable : mapModels.keySet()) {
			VariableLSTMModel varModel = mapModels.get(variable);
			mapModeleWeights.put(variable, varModel.getMapMatrices());
		}
		request.setMapModeleWeights(mapModeleWeights);
		return request;
	}


	public AbstractAggregationResult checkupModelAggregation(AggregationCheckupRequest fedAvgCheckupRequest,
			AbstractLogger logger, Map<String, ILearningModel> receivedModels) {
		String variableName = fedAvgCheckupRequest.getVariableName();
		boolean aggregationSet = false;
		try {
			boolean isAggregated = (aggregationDate != null);
			LSTMAggregationResult result = new LSTMAggregationResult();
			super.fillAggregationResult(fedAvgCheckupRequest, result, receivedModels, logger);
			String layerId = fedAvgCheckupRequest.getLayerId();
			INNLayer nodeLayer = this.getLayer(variableName, layerId);
			result.setAggregatedLayer(nodeLayer);
			if (isAggregated) {
				for (String nextAgentName : receivedModels.keySet()) {
					ILearningModel nextCompleteModel = receivedModels.get(nextAgentName);
					if (nextCompleteModel instanceof CompleteLSTMModel) {
						CompleteLSTMModel nextLSTMCompleteModel = (CompleteLSTMModel) nextCompleteModel;
						INNLayer nextLayer = nextLSTMCompleteModel.getLayer(variableName, layerId);
						result.addNodeLayer(nextAgentName, nextLayer);
					}
				}
			}
			if (!aggregationSet && result.getNodeLayers().size() == 1) {
				INNLayer uniqueLayer = result.getNodeLayers().values().iterator().next();
				result.setAggregatedLayer(uniqueLayer);
			}
			return result;
		} catch (Throwable e) {
			logger.error(e);
		}
		logger.info("CompleteLSTMModelheckupFedAVG : end");
		return null;
	}

	@Override
	public void load(Date currentDate) throws HandlingException {
	}

	@Override
	public void loadPartially(String[] _variables, List<FeaturesKey> listFeaturesKey, Date currentDate)
			throws HandlingException {
	}

	@Override
	public long save(boolean onlyCurrentMatrix, boolean saveAllIterations, AbstractLogger logger) throws HandlingException {
		this.updateModelWeightsOnServer(logger);
		return 0;
	}

}
