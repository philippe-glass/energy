package com.sapereapi.model.learning.markov;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.exception.IncompleteMatrixException;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;

public class VariableMarkovModel implements Serializable {
	private static final long serialVersionUID = 1L;
	private String variable;
	private Map<FeaturesKey, TransitionMatrix> mapMatrices = new HashMap<FeaturesKey, TransitionMatrix>();

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public Map<FeaturesKey, TransitionMatrix> getMapMatrices() {
		return mapMatrices;
	}

	public void setMapMatrices(Map<FeaturesKey, TransitionMatrix> mapMatrices) {
		this.mapMatrices = mapMatrices;
	}

	public List<FeaturesKey> getKeys(AbstractLogger logger) {
		List<FeaturesKey> result = new ArrayList<FeaturesKey>();
		for (FeaturesKey nextKey : mapMatrices.keySet()) {
			if (nextKey == null) {
				logger.error("CompleteMarkovModel.getKeys : key is null");
			}
			result.add(nextKey);
		}
		// sort the items
		Collections.sort(result, new Comparator<FeaturesKey>() {
			public int compare(FeaturesKey key1, FeaturesKey key2) {
				return key1.compareTo(key2);
			}
		});
		return result;
	}

	public boolean isComplete() {
		if (mapMatrices.size() == 0) {
			return false;
		}
		for (FeaturesKey nextKey : mapMatrices.keySet()) {
			TransitionMatrix nodeTransitionMatrices = mapMatrices.get(nextKey);
			if (!nodeTransitionMatrices.isComplete()) {
				return false;
			}
		}
		return true;
	}

	public void initTransitionMatrix(PredictionContext context, FeaturesKey aFeaturesKey) {
		VariableFeaturesKey trMatricKey = context.getTransitionMatrixKey(aFeaturesKey, variable);
		TransitionMatrix trMatrix =  new TransitionMatrix(trMatricKey);
		mapMatrices.put(aFeaturesKey, trMatrix);
	}

	public void addTransitionMatrix(TransitionMatrix trMatrix, AbstractLogger logger) {
		if (trMatrix.getFeaturesKey() == null) {
			logger.error("addNodeTransitionMatrices : For debug : feateure key is null");
		}
		mapMatrices.put(trMatrix.getFeaturesKey(), trMatrix);
	}

	public void refreshAllMatrices() {
		for (FeaturesKey nextFeaturesKey : mapMatrices.keySet()) {
			TransitionMatrix transitionMatrix = mapMatrices.get(nextFeaturesKey);
			transitionMatrix.refreshAllMatrices();
		}
	}

	public void setValueAtIteration(FeaturesKey featuresKey, int iterationNumber
			, int rowIdx, int columnIndx, double iterationObservationNb, double iterationCorrectionNb) {
		if (mapMatrices.containsKey(featuresKey)) {
			TransitionMatrix transitionMatrix = mapMatrices.get(featuresKey);
			transitionMatrix.setValueAtIteration(iterationNumber, rowIdx, columnIndx, iterationObservationNb,
					iterationCorrectionNb);
			// lastUpdate = new Date();
		}
	}

	public int size() {
		return mapMatrices.size();
	}

	public List<TransitionMatrix> getListNodeTransitionMatrices(AbstractLogger logger) {
		List<TransitionMatrix> result = new ArrayList<TransitionMatrix>();
		for (FeaturesKey nextFeaturesKey : getKeys(logger)) {
			TransitionMatrix transitionMatrix = mapMatrices.get(nextFeaturesKey);
			result.add(transitionMatrix);
		}
		return result;
	}

	public List<Long> getListMatrixIds(FeaturesKey currentFeaturesKey, boolean onlyCurrentMatrix, AbstractLogger logger) {
		List<Long> result = new ArrayList<Long>();
		for (FeaturesKey nextFeaturesKey : getKeys(logger)) {
			boolean isCurrentFeatureKey = currentFeaturesKey != null && currentFeaturesKey.equals(nextFeaturesKey);
			if (!onlyCurrentMatrix || isCurrentFeatureKey) {
				TransitionMatrix trMatrix = mapMatrices.get(nextFeaturesKey);
				VariableFeaturesKey trMatrixKey = trMatrix.getKey();
				Long trMatrixId = trMatrixKey.getId();
				if (trMatrixId != null) {
					result.add(trMatrixId);
				}
			}
		}
		return result;
	}

	public boolean hasFeaturesKey(FeaturesKey featuresKey) {
		return mapMatrices.containsKey(featuresKey);
	}

	private VariableFeaturesKey generateTransitionMatrixKey(PredictionContext context, FeaturesKey featuresKey) {
		return context.getTransitionMatrixKey(featuresKey, variable);
	}

	public void checkNodeTransitionMatrices(PredictionContext predictionContext, FeaturesKey featuresKey) {
		if (!mapMatrices.containsKey(featuresKey)) {
			TransitionMatrix trMatrix = new TransitionMatrix();
			trMatrix.setKey(null);
			VariableFeaturesKey trMatrixKey = generateTransitionMatrixKey(predictionContext, featuresKey);
			TransitionMatrix nodeTransitionMatrices = new TransitionMatrix(trMatrixKey);
			mapMatrices.put(featuresKey, nodeTransitionMatrices);
		}
	}

	public boolean updateMatrices2(PredictionContext predictionContext
			, FeaturesKey featuresKey
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
			TransitionMatrix trMatrix = mapMatrices.get(featuresKey);
			// nodeTransitionMatrices.reset(predictionContext);
			Date stateDate = transition.getStateDate();
			// aux_updateIteration(stateDate, iterationNumber);
			result = trMatrix.updateMatrices2(stateDate, iterationNumber, transition, refreshAll, logger);
			// lastUpdate = new Date();
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public Map<FeaturesKey, Integer> completeMatrices(Integer interationNumber) {
		Map<FeaturesKey, Integer> result = new HashMap<FeaturesKey, Integer>();
		for (TransitionMatrix nodeTransitionMatrices : mapMatrices.values()) {
			Integer nextResult = nodeTransitionMatrices.completeMatrix(interationNumber);
			result.put(nodeTransitionMatrices.getFeaturesKey(), nextResult);
		}
		// lastUpdate = new Date();
		return result;
	}

	public TransitionMatrix getTransitionMatrix(FeaturesKey featuresKey) {
		if (mapMatrices.containsKey(featuresKey)) {
			return mapMatrices.get(featuresKey);
		}
		return null;
	}

	public void setTransitionMatrixId(FeaturesKey featuresKey, Long id) {
		if (mapMatrices.containsKey(featuresKey)) {
			TransitionMatrix trMatrix = mapMatrices.get(featuresKey);
			VariableFeaturesKey featureKey = trMatrix.getKey();
			featureKey.setId(id);
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("VariableMarkovModel:").append(variable).append(" ").append(mapMatrices.size())
				.append(" matrices");
		int idx = 0;
		for (FeaturesKey nextKey : mapMatrices.keySet()) {
			if (false || idx < 1) {
				result.append(SapereUtil.CR);
				result.append("{key:").append(nextKey).append("}");
				TransitionMatrix nodeTransitionMatrices = mapMatrices.get(nextKey);
				result.append(nodeTransitionMatrices);
			}
			idx++;
		}
		return result.toString();
	}

	public Map<Object, Boolean> copyFromOther(VariableMarkovModel otherMarkovModel, AbstractLogger logger) {
		Map<Object, Boolean> mapHasChanged = new HashMap<Object, Boolean>();
		for (FeaturesKey nextKey : otherMarkovModel.getKeys(logger)) {
			if (!mapMatrices.containsKey(nextKey)) {
				mapMatrices.put(nextKey, new TransitionMatrix());
			}
			TransitionMatrix nextTransitionMatrices = mapMatrices.get(nextKey);
			TransitionMatrix otherTransitionMatrices = otherMarkovModel.getTransitionMatrix(nextKey);
			boolean hasChanged = nextTransitionMatrices.copyFromOther(otherTransitionMatrices);
			mapHasChanged.put(nextKey, hasChanged);
		}
		// lastUpdate = other.getLastUpdate();
		return mapHasChanged;
	}

	public VariableMarkovModel copyForLSA(AbstractLogger logger) {
		VariableMarkovModel result = new VariableMarkovModel();
		result.setVariable(variable);
		for (FeaturesKey nextKey : getKeys(logger)) {
			TransitionMatrix nextNodeTrMatrices = mapMatrices.get(nextKey);
			result.addTransitionMatrix(nextNodeTrMatrices.copyForLSA(logger), logger);
		}
		return result;
	}

	public VariableMarkovModel cloneWithFilter(MatrixFilter matrixFilter) {
		VariableMarkovModel result = new VariableMarkovModel();
		result.setVariable(variable);
		result.setMapMatrices(getFilteredContentMatrixFilter(matrixFilter));
		return result;
	}

	public Map<FeaturesKey, TransitionMatrix> getFilteredContentMatrixFilter(MatrixFilter matrixFilter) {
		Map<FeaturesKey, TransitionMatrix> result = new HashMap<FeaturesKey, TransitionMatrix>();
		boolean hasNoVariableFilter = matrixFilter.getVariableName() == null
				|| "".equals(matrixFilter.getVariableName());
		if (hasNoVariableFilter || variable.equals(matrixFilter.getVariableName())) {
			List<FeaturesKey> listFeaturesKeys = new ArrayList<FeaturesKey>();
			for (FeaturesKey nextFeaturesKey : mapMatrices.keySet()) {
				if (matrixFilter.applyFilter(nextFeaturesKey)) {
					listFeaturesKeys.add(nextFeaturesKey);
					TransitionMatrix toAdd = mapMatrices.get(nextFeaturesKey);
					result.put(nextFeaturesKey, toAdd.copy());
				}
			}
		}
		return result;
	}

	public Map<String, String> getZip(AbstractLogger logger) {
		Map<String, String> result = new HashMap<String, String>();
		for (FeaturesKey nextKey : getKeys(logger)) {
			TransitionMatrix nextTrMatrix = mapMatrices.get(nextKey);
			String sTrMatrix = nextTrMatrix.getCompleteObsMatrix().zip();
			String codeFeaturesKey = nextKey.getCode();
			result.put(codeFeaturesKey, sTrMatrix);
		}
		return result;
	}

	public PredictionData computeVariablePrediction(NodeStatesTransitions aNodeStateTransitions,
			PredictionData prediction, AbstractLogger logger) throws HandlingException {
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
				TransitionMatrix nextTransitionMatrix = this.getTransitionMatrix(nextStep.getFeaturesKey());
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

	public int getNbOfObservations() {
		int result = 0;
		for (FeaturesKey nextKey : mapMatrices.keySet()) {
			TransitionMatrix trMatrix = mapMatrices.get(nextKey);
			result += trMatrix.getNbOfObservations();
		}
		return result;
	}

	public boolean isEmpty() {
		for (FeaturesKey nextKey : mapMatrices.keySet()) {
			TransitionMatrix trMatrix = mapMatrices.get(nextKey);
			if (trMatrix.getNbOfObservations() > 0) {
				return false;
			}
		}
		return true;
	}

	public static VariableMarkovModel auxAggregate(Map<String, VariableMarkovModel> mapVarMakovModels,
			Map<String, Double> weightsTable, AgentAuthentication agentAuthentication, AbstractLogger logger) {
		VariableMarkovModel firstNodeTransitionMatrices = null;
		for (String nextNode : mapVarMakovModels.keySet()) {
			VariableMarkovModel nextObj = mapVarMakovModels.get(nextNode);
			if (firstNodeTransitionMatrices == null) {
				firstNodeTransitionMatrices = nextObj;
			}
		}
		VariableMarkovModel result = new VariableMarkovModel();
		// result.setComputeDate(new Date());
		result.setVariable(firstNodeTransitionMatrices.getVariable());
		Map<FeaturesKey, TransitionMatrix> aggregatedMap = new HashMap<FeaturesKey, TransitionMatrix>();
		for (FeaturesKey nextFeaturesKey : firstNodeTransitionMatrices.getMapMatrices().keySet()) {
			Map<String, TransitionMatrix> mapTransitionMatrix = new HashMap<String, TransitionMatrix>();
			for (String nextNode : mapVarMakovModels.keySet()) {
				VariableMarkovModel nextVariableMarkovModel = mapVarMakovModels.get(nextNode);
				mapTransitionMatrix.put(nextNode, nextVariableMarkovModel.getTransitionMatrix(nextFeaturesKey));
			}
			TransitionMatrix aggregatedTrMatrix = TransitionMatrix.auxAggregate(mapTransitionMatrix, weightsTable,
					agentAuthentication, logger, false);
			if (aggregatedTrMatrix != null) {
				aggregatedMap.put(nextFeaturesKey, aggregatedTrMatrix);
			}
		}
		result.setMapMatrices(aggregatedMap);
		return result;
	}

}
