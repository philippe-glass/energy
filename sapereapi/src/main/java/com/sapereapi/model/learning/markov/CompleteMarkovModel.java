package com.sapereapi.model.learning.markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.exception.IncompleteMatrixException;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.AbstractLearningModel;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.TimeWindow;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.aggregation.MarkovChainAggregationResult;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class CompleteMarkovModel extends AbstractLearningModel implements ILearningModel {
	private static final long serialVersionUID = 1L;
	protected Map<Integer, Date> mapIterationDates = new HashMap<Integer, Date>();
	protected List<Integer> iterations = new ArrayList<Integer>();
	private Map<String, VariableMarkovModel> mapModels = new HashMap<String, VariableMarkovModel>();

	public Map<String, VariableMarkovModel> getMapModels() {
		return mapModels;
	}

	public void setMapModels(Map<String, VariableMarkovModel> mapModels) {
		this.mapModels = mapModels;
	}

	public List<Integer> getIterations() {
		return iterations;
	}

	public void setIterations(List<Integer> iterations) {
		this.iterations = iterations;
	}

	public Map<Integer, Date> getMapIterationDates() {
		return mapIterationDates;
	}

	public void setMapIterationDates(Map<Integer, Date> mapIterationDates) {
		this.mapIterationDates = mapIterationDates;
	}

	public VariableMarkovModel getVariableMarkovModel(String variable) {
		if (mapModels.containsKey(variable)) {
			return mapModels.get(variable);
		}
		return null;
	}

	public void putVariableMarkovModel(String variable, VariableMarkovModel varModel) {
		mapModels.put(variable, varModel);
	}

	public Integer getLastIterationNumber() {
		if (iterations.size() > 0) {
			int idx = iterations.size() - 1;
			return iterations.get(idx);
		}
		return null;
	}

	private void checkModel(String variable) {
		if (!mapModels.containsKey(variable)) {
			VariableMarkovModel newModel = new VariableMarkovModel();
			newModel.setVariable(variable);
			mapModels.put(variable, newModel);
		}
	}

	public void initTransitionMatrices(FeaturesKey aFeaturesKey) {
		String[] variables = predictionContext.getNodeContext().getVariables();
		for(String variable : variables) {
			checkModel(variable);
			VariableMarkovModel varModel = mapModels.get(variable);
			varModel.initTransitionMatrix(predictionContext, aFeaturesKey);
		}
	}

	public Date getLastIterationDate() {
		Integer iterationNumber = getLastIterationNumber();
		if (iterationNumber != null && mapIterationDates.containsKey(iterationNumber)) {
			return mapIterationDates.get(iterationNumber);
		}
		return null;
	}

	public Date getIterationDate(Integer iterationNumber) {
		return mapIterationDates.get(iterationNumber);
	}

	private void aux_updateIteration(Date registerDate, Integer iterationNumber) {
		if (0 == iterationNumber) {
			System.out.println("For debug : iterationNumber = " + iterationNumber);
		}
		if (!this.mapIterationDates.containsKey(iterationNumber)) {
			Date iterationDate = UtilDates.getCeilDayStart(registerDate);
			this.mapIterationDates.put(iterationNumber, iterationDate);
		}
		if (!this.iterations.contains(iterationNumber)) {
			iterations.add(iterationNumber);
			Collections.sort(iterations);
		}
	}

	public List<FeaturesKey> getKeys(AbstractLogger logger) {
		if (mapModels.size() > 0) {
			VariableMarkovModel firstModel = mapModels.values().iterator().next();
			return firstModel.getKeys(logger);
		}
		return new ArrayList<FeaturesKey>();
	}

	public boolean isComplete() {
		if (mapModels.size() == 0) {
			return false;
		}
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel nextModel = mapModels.get(variable);
			if (!nextModel.isComplete()) {
				return false;
			}
		}
		return true;
	}

	public boolean canAggregate(CompleteMarkovModel otherModel) {
		PredictionContext otherPredictionContext = otherModel.getPredictionContext();
		if (predictionContext != null && otherPredictionContext != null) {
			return predictionContext.getScenario().equals(otherPredictionContext.getScenario());
		}
		return false;
	}

	private static Map<Integer, Date> aggregateMapIterations(String localNode,
			Map<String, CompleteMarkovModel> mapMarkovModels) {
		Map<Integer, Date> localMapIterationDates = new HashMap<Integer, Date>();
		if (mapMarkovModels.containsKey(localNode)) {
			CompleteMarkovModel localMarkovModel = mapMarkovModels.get(localNode);
			localMapIterationDates = SapereUtil.cloneMapIntegerDate(localMarkovModel.getMapIterationDates());
		}
		for (String nextNode : mapMarkovModels.keySet()) {
			CompleteMarkovModel nextMarkovModel = mapMarkovModels.get(nextNode);
			boolean isLocal = localNode.equals(nextNode);
			if (isLocal) {
			} else {
				// Upddate the iteration map if necessary
				Map<Integer, Date> nextMapIterationDates = SapereUtil.correctMapIterationDate(localMapIterationDates,
						nextMarkovModel.getMapIterationDates());
				for (Integer nextIt : nextMapIterationDates.keySet()) {
					if (!localMapIterationDates.containsKey(nextIt)) {
						localMapIterationDates.put(nextIt, nextMapIterationDates.get(nextIt));
					}
				}
			}
		}
		return localMapIterationDates;
	}

	private static Map<String, ILearningModel> toMapLearningModel(Map<String, CompleteMarkovModel> mapMarkovModels) {
		Map<String, ILearningModel> result = new HashMap<String, ILearningModel>();
		for (String key : mapMarkovModels.keySet()) {
			CompleteMarkovModel nextModel = mapMarkovModels.get(key);
			result.put(key, nextModel);
		}
		return result;
	}

	public static CompleteMarkovModel aggregate2(String operator,
			Map<String, CompleteMarkovModel> mapCompleteMarkovModels, AgentAuthentication agentAuthentication,
			AbstractLogger logger) {
		// Map<String, CompleteMarkovModel> mapMarkovModels = new HashMap<String,
		// CompleteMarkovModel>();
		String localAgent = agentAuthentication.getAgentName();
		// Map<String, Double> weightsTable = new HashMap<String, Double>();
		CompleteMarkovModel firstMarkovModel = null;

		// PredictionContext predictionContext = null;
		CompleteMarkovModel result = new CompleteMarkovModel();
		if (mapCompleteMarkovModels.containsKey(localAgent)) {
			firstMarkovModel = mapCompleteMarkovModels.get(localAgent);
			result.setCurrentFeaturesKey(firstMarkovModel.getCurrentFeaturesKey());
			result.setPredictionContext(firstMarkovModel.getPredictionContext().copyContent(false));
		} else {
			logger.warning("CompleteMarkovModel.aggregate2 : local agent " + localAgent + " not in aggregation source");
			return null;
		}
		// Aggregate iteration and mapIteration
		Map<Integer, Date> localMapIterationDates = aggregateMapIterations(localAgent, mapCompleteMarkovModels);
		result.setMapIterationDates(localMapIterationDates);
		// aggregate iterations
		List<Integer> listIterations = new ArrayList<Integer>();
		for (Integer nextId : localMapIterationDates.keySet()) {
			listIterations.add(nextId);
		}
		Collections.sort(listIterations);
		result.setIterations(listIterations);
		Map<String, Map<String, Double>> weightsTable = evaluateModelsWeigts(
				toMapLearningModel(mapCompleteMarkovModels), agentAuthentication, result.getPredictionContext(),
				operator, logger);
		result.setAggregationWeights(weightsTable);
		for (String variable : firstMarkovModel.getUsedVariables()) {
			Map<String, VariableMarkovModel> mapNodeVarModels = new HashMap<String, VariableMarkovModel>();
			for (String nextAgentName : mapCompleteMarkovModels.keySet()) {
				CompleteMarkovModel nextMarkovModel = mapCompleteMarkovModels.get(nextAgentName);
				VariableMarkovModel toAdd = nextMarkovModel.getVariableMarkovModel(variable);
				mapNodeVarModels.put(nextAgentName, toAdd);
			}
			Map<String, Double> variableWeightsTable = weightsTable.get(variable);
			VariableMarkovModel aggregatedVariableModel = VariableMarkovModel.auxAggregate(mapNodeVarModels,
					variableWeightsTable, agentAuthentication, logger);
			if (aggregatedVariableModel != null) {
				result.putVariableMarkovModel(aggregatedVariableModel.getVariable(), aggregatedVariableModel);
			}
			// String keyCode = nextKey.getCode();
			// JUST FOR DEBUG
			if ("produced".equals(variable)) {
				// String keyCode = "0";
				TimeWindow tw0 = LearningAgent.ALL_TIME_WINDOWS.get(0);
				FeaturesKey featuresKey = firstMarkovModel.getPredictionContext().getFeaturesKey(tw0.getId());
				for (String node : mapNodeVarModels.keySet()) {
					VariableMarkovModel nextModel = mapNodeVarModels.get(node);
					TransitionMatrix nextTrMatrix = nextModel.getTransitionMatrix(featuresKey);
					if (nextTrMatrix != null) {
						logger.info("CompleteMarkovModel.aggregae2 : for debug source iteration matrix of " + node
								+ " = " + nextTrMatrix.getCompleteObsMatrix());
					}
				}
				TransitionMatrix testMatrix = aggregatedVariableModel.getTransitionMatrix(featuresKey);
				if (testMatrix != null) {
					logger.info("CompleteMarkovModel.aggregae2 : for debug aggregatedIterationMatrix = "
							+ testMatrix.getCompleteObsMatrix());
				}
			}
		}
		return result;
	}

	public void refreshAllMatrices() {
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel nextModel = mapModels.get(variable);
			nextModel.refreshAllMatrices();
		}
	}

	public void setValueAtIteration(FeaturesKey featuresKey, String variable, int iterationNumber, Date iterationDate,
			int rowIdx, int columnIndx, double iterationObservationNb, double iterationCorrectionNb) {
		aux_updateIteration(iterationDate, iterationNumber);
		if (mapModels.containsKey(variable)) {
			VariableMarkovModel varModel = mapModels.get(variable);
			varModel.setValueAtIteration(featuresKey, iterationNumber, rowIdx, columnIndx, iterationObservationNb,
					iterationCorrectionNb);
			// lastUpdate = new Date();
		}
	}

	public int size() {
		if (mapModels.size() > 0) {
			VariableMarkovModel firstModel = mapModels.values().iterator().next();
			return firstModel.size();
		}
		return 0;
	}

	public List<Long> getListMatrixIds(boolean onlyCurrentMatrix, AbstractLogger logger) {
		List<Long> result = new ArrayList<Long>();
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			List<Long> listIds = varModel.getListMatrixIds(currentFeaturesKey, onlyCurrentMatrix, logger);
			result.addAll(listIds);
		}
		return result;
	}

	public boolean hasFeaturesKey(FeaturesKey featuresKey) {
		if (mapModels.size() > 0) {
			VariableMarkovModel firstModel = mapModels.values().iterator().next();
			return firstModel.hasFeaturesKey(featuresKey);
		}
		return false;
	}

	/*
	 * public boolean updateMatricesLastTransition(PredictionContext
	 * predictionContext, Date registerDate, NodeMarkovTransitions transition,
	 * boolean refreshAll) { checkNodeTransitionMatrices(predictionContext,
	 * currentFeaturesKey); NodeTransitionMatrices nodeTransitionMatrices =
	 * content.get(currentFeaturesKey); //
	 * nodeTransitionMatrices.reset(predictionContext); Integer transitionNumber =
	 * getLastIterationNumber(); boolean result =
	 * nodeTransitionMatrices.updateMatrices2(registerDate, transitionNumber,
	 * transition, refreshAll); //lastUpdate = new Date(); return result; }
	 */

	private void checkNodeTransitionMatrices(PredictionContext predictionContext, FeaturesKey featuresKey) {
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			varModel.checkNodeTransitionMatrices(predictionContext, featuresKey);
		}
	}

	public boolean updateMatrices2(FeaturesKey featuresKey
			, Integer iterationNumber
			, NodeStatesTransitions transition
			, boolean refreshAll
			, AbstractLogger logger) {
		boolean result = false;
		try {
			if (featuresKey == null) {
				logger.error("updateMatrices2 : featuresKey is null");
			}
			checkNodeTransitionMatrices(predictionContext, featuresKey);
			for (String variable : mapModels.keySet()) {
				VariableMarkovModel varModel = mapModels.get(variable);
				boolean nextResult = varModel.updateMatrices2(predictionContext, featuresKey, iterationNumber,
						transition, refreshAll, logger);
				result = result || nextResult;
			}
			// NodeTransitionMatrices nodeTransitionMatrices = content.get(featuresKey);
			// nodeTransitionMatrices.reset(predictionContext);
			Date stateDate = transition.getStateDate();
			aux_updateIteration(stateDate, iterationNumber);
			// result = nodeTransitionMatrices.updateMatrices2(stateDate, iterationNumber,
			// transition, refreshAll);
			// lastUpdate = new Date();
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public Map<String, Map<FeaturesKey, Integer>> completeMatrices(Integer interationNumber) {
		Map<String, Map<FeaturesKey, Integer>> result = new HashMap<String, Map<FeaturesKey, Integer>>();
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			Map<FeaturesKey, Integer> partialResult = varModel.completeMatrices(interationNumber);
			result.put(variable, partialResult);

		}
		return result;
	}

	public TransitionMatrix getTransitionMatrix(FeaturesKey featuresKey, String variable) {
		if (mapModels.containsKey(variable)) {
			VariableMarkovModel varModel = mapModels.get(variable);
			return varModel.getTransitionMatrix(featuresKey);
		}
		return null;
	}

	public void setTransitionMatrixId(FeaturesKey featuresKey, String variable, Long id) {
		if (mapModels.containsKey(variable)) {
			VariableMarkovModel varModel = mapModels.get(variable);
			varModel.setTransitionMatrixId(featuresKey, id);
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("CompleteMarkovModel:");// .append(this.size()).append(" matrices");
		result.append(", iterations:").append(this.getIterations());
		// int idx = 0;
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			result.append(SapereUtil.CR).append(varModel);
		}
		if (stateHistory.size() > 0) {
			result.append(", stateHistory:");
			int idx2 = 0;
			String sep = "";
			for (SingleNodeStateItem nextStateItem : stateHistory) {
				if (idx2 < 100) {
					result.append(sep).append(nextStateItem);
					sep = ", ";
				}
				idx2++;
			}
		}
		return result.toString();
	}

	@Override
	public Map<Object, Boolean> copyFromOther(ILearningModel other, AbstractLogger logger) {
		super.copyAggregationWeigtsFomOther(other);
		Map<Object, Boolean> mapHasChanged = new HashMap<Object, Boolean>();
		if (other instanceof CompleteMarkovModel) {
			CompleteMarkovModel otherMarkovModel = (CompleteMarkovModel) other;
			for (String variable : otherMarkovModel.getMapModels().keySet()) {
				checkModel(variable);
				VariableMarkovModel varModel = mapModels.get(variable);
				VariableMarkovModel otherVarModel = otherMarkovModel.getVariableMarkovModel(variable);
				Map<Object, Boolean> mapHasChanged2 = varModel.copyFromOther(otherVarModel, logger);
				for (Object nextKey : mapHasChanged2.keySet()) {
					mapHasChanged.put(nextKey, mapHasChanged2.get(nextKey));
				}
			}
			// lastUpdate = other.getLastUpdate();
		}
		return mapHasChanged;
	}

	public CompleteMarkovModel copyForLSA(AbstractLogger logger) {
		CompleteMarkovModel result = new CompleteMarkovModel();
		result.setPredictionContext(predictionContext.copyContent(false));
		result.setLastUpdate(lastUpdate);
		result.setIterations(SapereUtil.cloneListInteger(iterations));
		result.setMapIterationDates(SapereUtil.cloneMapIntegerDate(mapIterationDates));
		if (aggregationWeights != null && aggregationWeights.size() > 0) {
			result.setAggregationWeights(SapereUtil.cloneMap2StringDouble(aggregationWeights));
		}
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			result.putVariableMarkovModel(variable, varModel.copyForLSA(logger));
		}
		return result;
	}

	public CompleteMarkovModel cloneWithFilter(MatrixFilter matrixFilter, AbstractLogger logger) {
		CompleteMarkovModel result = new CompleteMarkovModel();
		result.setPredictionContext(predictionContext.clone());
		result.setLastUpdate(lastUpdate);
		result.setIterations(SapereUtil.cloneListInteger(iterations));
		result.setMapIterationDates(SapereUtil.cloneMapIntegerDate(mapIterationDates));
		if (aggregationWeights != null && aggregationWeights.size() > 0) {
			result.setAggregationWeights(SapereUtil.cloneMap2StringDouble(aggregationWeights));
		}
		for (String variable : mapModels.keySet()) {
			if (matrixFilter.applyVariableFilter(variable)) {
				VariableMarkovModel modelToFilter = mapModels.get(variable);
				VariableMarkovModel filteredModel = modelToFilter.cloneWithFilter(matrixFilter);
				if (!filteredModel.isEmpty()) {
					filteredModel.refreshAllMatrices();
					result.putVariableMarkovModel(variable, filteredModel);
				}
			}
		}
		return result;
	}


	@Override
	public CompactMarkovModel generateCompactedModel(AbstractLogger logger) {
		CompactMarkovModel result = new CompactMarkovModel();
		super.fillCompactModel(result, logger);
		result.setIterations(SapereUtil.cloneListInteger(iterations));
		result.setMapIterationDates(SapereUtil.cloneMapIntegerDate(mapIterationDates));
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			Map<String, String> zippedContent = varModel.getZip(logger);
			result.addZippedContent(variable, zippedContent);
		}
		return result;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		// TODO Auto-generated method stub
	}

	@Override
	public PredictionData computePrediction(Date initDate, List<Date> targetDates, String[] variables,
			boolean useCorrections, AbstractLogger logger) {
		this.refreshAllMatrices();
		return super.computePrediction(initDate, targetDates, variables, useCorrections, logger);
	}

	public PredictionData computeVariablePrediction(NodeStatesTransitions aNodeStateTransitions,
			PredictionData prediction, String variable, AbstractLogger logger) throws HandlingException {
		if (aNodeStateTransitions == null) {
			throw new HandlingException("No current state");
		}
		VariableState initalState = aNodeStateTransitions.getState(variable);
		if (initalState == null) {
			throw new HandlingException("No current state for " + variable + " variable");
		}
		int stateIdx = NodeStates.getStateIndex(initalState);
		int nbStates = NodeStates.getNbOfStates();
		DoubleMatrix predictionRow = new DoubleMatrix(1, nbStates);
		predictionRow.set(0, stateIdx, 1);
		long timeShiftMS = prediction.getContext().getTimeShiftMS();
		double checkSum = predictionRow.getSum();
		// logger.info("prediction testSum=" + checkSum);
		for (PredictionStep nextStep : prediction.getListSteps()) {
			try {
				TransitionMatrix nextTransitionMatrix = this.getTransitionMatrix(nextStep.getFeaturesKey(), variable);
				if (nextTransitionMatrix == null) {
					throw new Exception("No transition matrix found for  " + variable + " at " + nextStep);
				} else {
					nextStep.setUsedTransitionMatrixId(variable, nextTransitionMatrix.getKey().getId());
					nextTransitionMatrix.checkCompletion(predictionRow);
					if (nextStep.getDurationMinutes() >= 0.5) {
						boolean useCorrection = prediction.isUseCorrections();
						predictionRow = predictionRow
								.multiplyByMatrix(nextTransitionMatrix.getNormalizedMatrix(useCorrection));
						checkSum = predictionRow.getSum();
						if (Math.abs(checkSum - 1.0) > 0.0001) {
							throw new Exception("Prediction sum should be equals to 1 for variable " + variable
									+ " , timewindow " + nextStep + "[sum:" + checkSum + "]");
						}
					} else {
						// logger.info("ignored prediction step : " + nextStep);
					}
					nextStep.setStateProbabilities(predictionRow);
					// logger.error(e);("prediction testSum=" + checkSum);
				}
			} catch (Exception e) {
				if (e instanceof IncompleteMatrixException) {
					logger.warning(e.getMessage());
				} else {
					logger.error(e);
				}
				prediction.addError(e.getMessage());
				return prediction;
			}
			for (Date nextTargetDate : prediction.getTargetDatesWithoutResult(variable)) {
				if (nextStep.isInSlot(nextTargetDate)) {
					PredictionResult result = new PredictionResult(prediction.getInitialDate(), initalState,
							nextTargetDate, variable, nextStep.getFeaturesKey(), timeShiftMS);
					result.setStateProbabilities(predictionRow);
					prediction.setResult(variable, nextTargetDate, result);
				}
			}
		} // end loop on prediction steps
		Date lastTargetDate = prediction.getLastTargetDate();
		if (!prediction.hasResult(variable, lastTargetDate)) {
			PredictionResult result = new PredictionResult(prediction.getInitialDate(), initalState, lastTargetDate,
					variable, prediction.getLastStep().getFeaturesKey(), timeShiftMS);
			result.setStateProbabilities(predictionRow);
			prediction.setResult(variable, lastTargetDate, result);
		}
		// For log
		boolean toLog = false;
		if (toLog) {
			for (Date nextTargetDate : prediction.getTargetDates()) {
				if (prediction.hasResult(variable, nextTargetDate)) {
					logger.info("computeVariablePrediction : " + variable + " : "
							+ prediction.getResult(variable, nextTargetDate));
				}
			}
		}
		return prediction;
	}

	public boolean refreshTransitions(NodeTotal nodeTotal, Integer iterationNumber, boolean enableObsUpdate,
			AbstractLogger logger) {
		// boolean hasLastState = false;
		super.auxRefreshTransitions(nodeTotal, enableObsUpdate, logger);
		boolean trMatrixUpdated = false;
		if (enableObsUpdate && nodeStateTransitions.isComplete(predictionContext)) {
			// Update the observation number on transition matrix
			trMatrixUpdated = updateMatrices2(currentFeaturesKey, iterationNumber, nodeStateTransitions, false, logger);
		}
		return trMatrixUpdated;
	}


	@Override
	public boolean refreshModel(NodeTotal nodeTotal, boolean enableObsUpdate, AbstractLogger logger) {
		Date transitionTime = nodeTotal.getDate();
		Date transitionDate = UtilDates.getCeilDayStart(transitionTime);
		Integer iteration = getLastIterationNumber();
		if(iteration == null) {
			iteration = 1;
		} else if (mapIterationDates.containsKey(iteration)){
			Date lastIterationDate = mapIterationDates.get(iteration);
			if(transitionDate.after(lastIterationDate)) {
				// The iteration date changes : increment the iteration
				iteration+=1;
				logger.info("refreshModel : increment iteration : iteration = " + iteration
						+ ", transitionTime = " + UtilDates.format_date_time.format(lastIterationDate)
						+ ", transitionTime = " + UtilDates.format_date_time.format(lastIterationDate));
			}
		}
		boolean updateted = refreshTransitions(nodeTotal, iteration, enableObsUpdate, logger);
		return updateted;
	}

	public void initNodeHistory(List<NodeTotal> nodeHistory,
			boolean completeMatrices, AbstractLogger logger) {
		// int stepNumber = 1;
		Integer iterationNumber = 1;
		Date lastDay = null;
		NodeTotal firstTotal = nodeHistory.get(0);
		Date firstDate = firstTotal.getDate();
		nodeStateTransitions = new NodeStatesTransitions(firstDate);
		this.stateHistory.clear();
		currentFeaturesKey = null;
		for (NodeTotal nextNodeTotal : nodeHistory) {
			Date nextDate = nextNodeTotal.getDate();
			Date nextDay = UtilDates.removeTime(nextDate);
			// logger.info("CompleteMarkovModel.initNodeHistory nextDate
			// = " + UtilDates.format_sql.format(nextDate));
			if (lastDay != null && nextDay.getTime() > lastDay.getTime()) {
				iterationNumber++;
			}
			boolean upddated = refreshTransitions(nextNodeTotal, iterationNumber, true, logger);
			logger.info("initNodeHistory done : upddated = " + upddated);
			// FeaturesKey featuresKey = predictionContext.getFeaturesKey2(nextDate);
			// SingleNodeStateItem nextNodeState =
			// nodeMarkovTransitions.generateSingleNodeStateItem(predictionContext);
			// mapStateHistory.put(nextDate, nextNodeState);
			// stateHistory.add(nextNodeState);
			// nodeTransitionMatrices.reset(predictionContext);
			// boolean upddated = updateMatrices2(featuresKey, nextDate, iterationNumber,
			// nodeMarkovTransitions, false);
			lastDay = nextDay;
		}
		if (completeMatrices) {
			Integer iterationNumer = getLastIterationNumber();
			completeMatrices(iterationNumer);
			boolean testIsComplete = isComplete();
			logger.info("testIsComplete = " + testIsComplete + " ,iterationNumer = " + iterationNumer);
			completeMatrices(iterationNumber);
		}
		this.refreshAllMatrices();
	}

	public int getNbOfObservations(String variable) {
		if (mapModels.containsKey(variable)) {
			VariableMarkovModel varModel = mapModels.get(variable);
			return varModel.getNbOfObservations();
		}
		return 0;
	}

	@Override
	public IAggregateable aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		Map<String, CompleteMarkovModel> mapModels = new HashMap<String, CompleteMarkovModel>();
		for (String agentName : mapObjects.keySet()) {
			IAggregateable nextObj = mapObjects.get(agentName);
			if (nextObj instanceof CompleteMarkovModel) {
				mapModels.put(agentName, (CompleteMarkovModel) nextObj);
			}
		}
		return aggregate2(operator, mapModels, agentAuthentication, logger);
	}

	@Override
	public List<String> getUsedVariables() {
		List<String> result = new ArrayList<String>();
		for (String variable : mapModels.keySet()) {
			VariableMarkovModel varModel = mapModels.get(variable);
			if (!varModel.isEmpty()) {
				result.add(variable);
			}
		}
		return result;
	}

	public AbstractAggregationResult checkupModelAggregation(
			 AggregationCheckupRequest fedAvgCheckupRequest
		    ,AbstractLogger logger
			,Map<String, ILearningModel> receivedModels) {
		String variableName = fedAvgCheckupRequest.getVariableName();
		Integer timeWindowId = fedAvgCheckupRequest.getTimeWindowId();
		boolean aggregationSet = false;
		try {
			boolean isAggregated = (aggregationDate != null);
			FeaturesKey correspondingFeatudeKey = predictionContext.getFeaturesKey(timeWindowId);
			/*
			 * if(false && testFeatudeKey != null) { currentFeaturesKey = testFeatudeKey; }
			 */
			logger.info("CompleteMarkovModel.checkupFedAVG aggregationWeights = " + aggregationWeights);
			MarkovChainAggregationResult result = new MarkovChainAggregationResult();
			super.fillAggregationResult(fedAvgCheckupRequest, result, receivedModels, logger);
			TransitionMatrix trMatrix = this.getTransitionMatrix(correspondingFeatudeKey, variableName);
			trMatrix.refreshAllMatrices();
			result.setAggregateTransitionMatrix(trMatrix);
			if (isAggregated) {
				for (String nextAgent : receivedModels.keySet()) {
					ILearningModel nextCompleteModel = receivedModels.get(nextAgent);
					if (nextCompleteModel instanceof CompleteMarkovModel) {
						CompleteMarkovModel nextMarkovCompleteModel = (CompleteMarkovModel) nextCompleteModel;
						TransitionMatrix nextTrMatrix = nextMarkovCompleteModel.getTransitionMatrix(correspondingFeatudeKey, variableName);
						nextTrMatrix.refreshAllMatrices();
						result.addNodeTransitionMatrix(nextAgent, nextTrMatrix);
					}
				}
			}
			if (!aggregationSet && result.getNodeTransitionMatrices().size() == 1) {
				TransitionMatrix uniqueTrMatrix = result.getNodeTransitionMatrices().values().iterator().next();
				result.setAggregateTransitionMatrix(uniqueTrMatrix);
			}
			return result;
		} catch (Throwable e) {
			logger.error(e);
		}
		logger.info("checkupFedAVG : after loop1");
		return null;
	}

	@Override
	public void load(Date currentDate) throws HandlingException {
		List<FeaturesKey> listFeaturesKey = predictionContext.getAllFeaturesKeys();
		String[] variables = predictionContext.getNodeContext().getVariables();
		PredictionDbHelper.loadPartialMarkovModel(this, variables, listFeaturesKey, currentDate);
	}

	@Override
	public void loadPartially(String[] _variables, List<FeaturesKey> listFeaturesKey, Date currentDate)
			throws HandlingException {
		PredictionDbHelper.loadPartialMarkovModel(this, _variables, listFeaturesKey, currentDate);
	}

	@Override
	public long save(boolean onlyCurrentMatrix, boolean saveAllIterations, AbstractLogger logger) throws HandlingException {
		return PredictionDbHelper.saveMarkovModel(this, onlyCurrentMatrix, saveAllIterations);
	}

}
