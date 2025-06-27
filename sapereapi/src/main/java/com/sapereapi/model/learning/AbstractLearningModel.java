package com.sapereapi.model.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.helper.PredictionHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.MultiPredictionsData;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionCorrection;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.model.learning.prediction.StatesStatistic;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.learning.prediction.input.PredictionRequest;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.AbstractAggregatable;
import eu.sapere.middleware.node.NodeLocation;

public abstract class AbstractLearningModel extends AbstractAggregatable implements ILearningModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected FeaturesKey currentFeaturesKey;
	protected PredictionContext predictionContext;
	protected NodeStatesTransitions nodeStateTransitions = null;
	protected List<SingleNodeStateItem> stateHistory = new ArrayList<SingleNodeStateItem>();
	protected Map<String, Map<String, Double>> aggregationWeights = new HashMap<String, Map<String, Double>>();

	public PredictionContext getPredictionContext() {
		return predictionContext;
	}

	public void setPredictionContext(PredictionContext predictionContext) {
		this.predictionContext = predictionContext;
	}

	public FeaturesKey getCurrentFeaturesKey() {
		return currentFeaturesKey;
	}

	public void setCurrentFeaturesKey(FeaturesKey currentFeaturesKey) {
		this.currentFeaturesKey = currentFeaturesKey;
	}

	public List<SingleNodeStateItem> getStateHistory() {
		return stateHistory;
	}

	public void setStateHistory(List<SingleNodeStateItem> stateHistory) {
		this.stateHistory = stateHistory;
	}

	public void addStateHistory(SingleNodeStateItem item) {
		this.stateHistory.add(item);
	}

	public Map<String, Map<String, Double>> getAggregationWeights() {
		return aggregationWeights;
	}

	public void setAggregationWeights(Map<String, Map<String, Double>> aggregationWeights) {
		this.aggregationWeights = aggregationWeights;
	}

	public boolean hasModelAggregator() {
		LearningAggregationOperator aggregator = predictionContext.getAggregationOperator();
		return (aggregator != null && aggregator.isModelAggregation());
	}

	public boolean hasPredictionAggregator() {
		LearningAggregationOperator aggregator = predictionContext.getAggregationOperator();
		return (aggregator != null && aggregator.isPredictionAggregation());
	}

	public boolean hasModelOrPredictionAggregator() {
		LearningAggregationOperator aggregator = predictionContext.getAggregationOperator();
		if(aggregator != null) {
			return aggregator.isModelAggregation() || aggregator.isPredictionAggregation();
		}
		return false;
	}

	public NodeStatesTransitions getNodeStatesTransitions() {
		return nodeStateTransitions;
	}

	public void setNodeStatesTransitions(NodeStatesTransitions nodeStateTransitions) {
		this.nodeStateTransitions = nodeStateTransitions;
	}

	protected void copyAggregationWeigtsFomOther(ILearningModel other) {
		Map<String, Map<String, Double>>  aggregationWeightsOther = other.getAggregationWeights();
		this.aggregationWeights = SapereUtil.cloneMap2StringDouble(aggregationWeightsOther);
	}

	protected SingleNodeStateItem findNodeState(Date initDate, AbstractLogger logger) throws HandlingException {
		// Check node history
		//boolean loadHistory = true;
		Date dateMin = UtilDates.shiftDateMinutes(initDate, -3);
		Date dateMax = UtilDates.shiftDateMinutes(initDate, 3);
		TreeMap<Date, SingleNodeStateItem> selectedHistory = SapereUtil.selectMapHistory(stateHistory, dateMin, dateMax);
		Date closeInitDate = SapereUtil.getClosestDate(initDate, selectedHistory.keySet());
		// Date closeLastTargetDate = SapereUtil.getClosestDate(lastTargetDate,
		// selectedHistory.keySet());
		boolean isOk = (closeInitDate != null) && closeInitDate.getTime() - initDate.getTime() <= 3 * 1000 * 60;
		if (isOk) {
			return selectedHistory.get(closeInitDate);
		}
		if (!isOk) {
			// Load history from databse
			List<SingleNodeStateItem> historyFromDb = PredictionDbHelper.loadStateHistory(predictionContext, dateMin,
					dateMax);
			selectedHistory = SapereUtil.selectMapHistory(historyFromDb, dateMin, dateMax);
			closeInitDate = SapereUtil.getClosestDate(initDate, selectedHistory.keySet());
			isOk = (closeInitDate != null) && closeInitDate.getTime() - initDate.getTime() <= 3 * 1000 * 60;
			if (isOk) {
				return selectedHistory.get(closeInitDate);
			}
		}
		return null;
	}

	protected void fitNodeStateTransitions(Date initDate, AbstractLogger logger)
			throws HandlingException {
		if (nodeStateTransitions != null && nodeStateTransitions.getStateDate().equals(initDate)) {
			// nodeStateTransitions is OK : nothing to do
		} else {
			//SingleNodeStateItem initialState = selectClosestStateItem(stateHistory, initDate, logger);
			SingleNodeStateItem initialState = findNodeState(initDate, logger);
			if(initialState==null) {
				SingleNodeStateItem test = findNodeState(initDate, logger);
				throw new HandlingException("fitNodeStateTransitions : no state found arrond date " + UtilDates.format_date_time.format(initDate));
			}
			nodeStateTransitions = new NodeStatesTransitions(initDate);
			nodeStateTransitions.setCurrentState(predictionContext, initialState, logger);
		}
	}

	public boolean auxRefreshTransitions(NodeTotal nodeTotal, boolean enableObsUpdate, AbstractLogger logger) {
		// boolean hasLastState = false;
		if (nodeStateTransitions == null) {
			nodeStateTransitions = new NodeStatesTransitions(nodeTotal.getDate());
			nodeStateTransitions.setCurrentState(predictionContext, nodeTotal, logger);
		} else {
			nodeStateTransitions.refreshTransitions(predictionContext, nodeTotal, enableObsUpdate, logger);
		}
		Date newStateDate = nodeStateTransitions.getStateDate();
		SingleNodeStateItem nextNodeState = nodeStateTransitions.generateSingleNodeStateItem(predictionContext);
		stateHistory.add(nextNodeState);

		// Update current feature key
		FeaturesKey lastFeaturesKey = (currentFeaturesKey == null) ? null : currentFeaturesKey.clone();
		currentFeaturesKey = predictionContext.getFeaturesKey2(newStateDate);
		boolean keyHasChanged = lastFeaturesKey == null || (currentFeaturesKey != null && !currentFeaturesKey.equals(lastFeaturesKey));
		if (keyHasChanged) {
			logger.info("AbstractLearningModel.refreshTransitions : keyHasChanged : " + currentFeaturesKey);
			lastUpdate = new Date();
		}
		return keyHasChanged;
		// mapStateHistory.put(nextDate, nextNodeState);
	}

	public VariableState getCurrentVariableState(String variable) {
		if (nodeStateTransitions != null) {
			return nodeStateTransitions.getState(variable);
		}
		return null;
	}

	public void initialize(Date aDate, PredictionContext aPredictionContext, AbstractLogger logger) {
		this.nodeStateTransitions = new NodeStatesTransitions(aDate);
		this.predictionContext = aPredictionContext;
		try {
			this.currentFeaturesKey = predictionContext.getFeaturesKey2(aDate);
			lastUpdate = new Date();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public static Date getLastTargetDate(List<Date> targetDates) {
		Date lastTargetDate = targetDates.get(0);
		for (Date nextTargetDate : targetDates) {
			if (nextTargetDate.after(lastTargetDate)) {
				lastTargetDate = nextTargetDate;
			}
		}
		return lastTargetDate;
	}

	protected PredictionData initPredictionData(Date initDate, List<Date> targetDates, String[] variables,
			boolean useCorrections) {
		PredictionData prediction = new PredictionData(predictionContext, useCorrections);
		prediction.setVariables(variables);
		prediction.setInitialDate(initDate);
		for (Date targetDate : targetDates) {
			prediction.addTargetDate(targetDate);
		}
		return prediction;
	}

	public SingleNodeStateItem getClosestStateItem(Date aDate, AbstractLogger logger) {
		return SapereUtil.selectClosestStateItem(stateHistory, aDate, logger);
	}

	protected static List<SingleNodeStateItem> retrieveLocalStateHistory(Map<String, ILearningModel> mapLearningModels,
			String localNode, AbstractLogger logger) {
		List<SingleNodeStateItem> localStateHistory = new ArrayList<SingleNodeStateItem>();
		if (mapLearningModels.containsKey(localNode)) {
			ILearningModel localLearningModel = mapLearningModels.get(localNode);
			localStateHistory = localLearningModel.getStateHistory();
			if (localStateHistory != null && localStateHistory.size() > 0) {
				logger.info("LearningModel.retrieveLocalStateHistory : local stateHistory : " + localStateHistory);
			}
			return localStateHistory;
		}
		return localStateHistory;
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<NodeLocation>();
		if (this.predictionContext != null) {
			result.add(predictionContext.getNodeLocation());
		}
		return result;
	}

	public static FeaturesKey getFeaturesKey(Date aDate) {
		for (TimeWindow timeWindow : LearningAgent.ALL_TIME_WINDOWS) {
			if (timeWindow.containsDate(aDate)) {
				return new FeaturesKey(timeWindow);
			}
		}
		return null;
	}

	public static PredictionStep generateStep(Date currentDate, Date targetDate) throws HandlingException {
		FeaturesKey featuresKey = getFeaturesKey(currentDate);
		if (featuresKey != null) {
			Date startDate = currentDate;
			Date timeWindowEndDate = featuresKey.getTimeWindow().getEndDate(currentDate);
			if (startDate.after(timeWindowEndDate)) {
				throw new HandlingException(
						"PredictionTimeSlot : startDate " + UtilDates.format_date_time.format(startDate)
								+ " is after end date " + UtilDates.format_date_time.format(timeWindowEndDate));
			}
			Date endDate = UtilDates.shiftDateMinutes(currentDate, LearningAgent.LEARNING_TIME_STEP_MINUTES);
			if (endDate.after(timeWindowEndDate)) {
				endDate = timeWindowEndDate;
			}
			if (endDate.after(targetDate)) {
				endDate = targetDate;
			}
			return new PredictionStep(featuresKey, startDate, endDate);
		}
		return null;
	}

	public List<PredictionStep> computePredictionSteps(Date initialDate, Date targetDate) throws HandlingException {
		List<PredictionStep> result = new ArrayList<PredictionStep>();
		// Date current = SapereUtil.getCurrentMinute();
		PredictionStep nextTimeSlot = generateStep(initialDate, targetDate);
		result.add(nextTimeSlot);
		Date endDate = nextTimeSlot.getEndDate();
		while (endDate.before(targetDate)) {
			Date nextBeginDate = nextTimeSlot.getEndDate();
			// logger.info("nextBeginDate = " +
			// SapereUtil.format_date_time.format(nextBeginDate));
			nextTimeSlot = generateStep(nextBeginDate, targetDate);
			if (nextTimeSlot == null) {
				throw new HandlingException("Time window not found for date " + nextBeginDate);
			}
			result.add(nextTimeSlot);
			endDate = nextTimeSlot.getEndDate();
		}
		return result;
	}

	@Override
	public PredictionData computePrediction(Date initDate, List<Date> targetDates, String[] variables,
			boolean useCorrections, AbstractLogger logger) {
		NodeStatesTransitions backupStateTransition = (nodeStateTransitions == null)? null : nodeStateTransitions.clone();
		PredictionData prediction = initPredictionData(initDate, targetDates, variables, useCorrections);
		try {
			fitNodeStateTransitions(initDate, logger);
			Date lastTargetDate = getLastTargetDate(targetDates);
			List<PredictionStep> listSteps = computePredictionSteps(prediction.getInitialDate(), lastTargetDate);
			prediction.setListSteps(listSteps);
			// String location = predictionContext.getMainServiceAddress();
			prediction.setInitialContent(nodeStateTransitions);
			if (prediction.hasInitialContent()) {
				List<FeaturesKey> listFeaturesKeys = new ArrayList<FeaturesKey>();
				for (PredictionStep timeSlot : listSteps) {
					FeaturesKey featuresKey = timeSlot.getFeaturesKey();
					if (!listFeaturesKeys.contains(featuresKey)) {
						listFeaturesKeys.add(featuresKey);
					}
				}
				// Retrieve needed transtition matrices
				for (String variable : variables) {
					prediction = computeVariablePrediction(nodeStateTransitions, prediction, variable, logger);
					prediction.setSamplingNb(variable, this.getSamplingNb(variable));
				}
				// Generate random prediction
				prediction.generateRandomState();
			}
		} catch (Exception e) {
			logger.error(e);
			prediction.addError(e.getMessage());
		}
		if(nodeStateTransitions != null) {
			if(!nodeStateTransitions.getStateDate().equals(backupStateTransition.getStateDate())) {
				nodeStateTransitions = backupStateTransition.clone();
			}
		}
		return prediction;
	}

	public void cleanHistory(long maxHistoryDurationMS, AbstractLogger logger) {
		Date maxDate = this.predictionContext.getCurrentDate();
		Date minDate = new Date(maxDate.getTime() - maxHistoryDurationMS);
		// int idxToKeep = 0;
		// Test the first date
		if (stateHistory.size() > 0) {
			SingleNodeStateItem firstItem = stateHistory.get(0);
			Date firstStateDate = firstItem.getDate();
			int lastIdx = stateHistory.size() - 1;
			SingleNodeStateItem lastItem = stateHistory.get(lastIdx);
			Date lastStateDate = lastItem.getDate();
			if (firstStateDate.after(minDate) && lastStateDate.before(maxDate)) {
				// all dates are after minDate : nothing to do
				logger.info("cleanHistory : nothing done");
			} else {
				//logger.info("cleanHistory minDate = " + UtilDates.format_sql.format(minDate)
				//	+ ", maxDate = " + UtilDates.format_sql.format(maxDate));
				List<SingleNodeStateItem> newContent = new ArrayList<SingleNodeStateItem>();
				for (SingleNodeStateItem nextItem : this.stateHistory) {
					Date nextDate = nextItem.getDate();
					boolean toKeep = nextDate.after(minDate) && !nextDate.after(maxDate);
					// boolean toRemove = stateDate.before(minDate) || stateDate.after(maxDate);
					if (toKeep) {
						newContent.add(nextItem);
					} else {
						//logger.info("cleanHistory : element removed : " + UtilDates.format_time.format(nextItem.getDate()));
					}
				}
				logger.info("cleanHistory minDate = " + UtilDates.format_time.format(minDate));
				logger.info("cleanHistory maxDate = " + UtilDates.format_time.format(maxDate));
				logger.info("cleanHistory size before = " + this.stateHistory.size() + ", size after = " + newContent.size());
				if(newContent.size() < 30) {
					logger.warning("cleanHistory for debug");
				}
				this.stateHistory = newContent;
			}
		}
	}

	private static TreeMap<Date, SingleNodeStateItem> selectTargetItems(
			Map<String, ILearningModel> mapLearningModels
			,List<SingleNodeStateItem> localStateHistory
			,PredictionContext predictionContext
			,AbstractLogger logger) throws HandlingException {
		TreeMap<Date, SingleNodeStateItem> mapTargetItems = new TreeMap<Date, SingleNodeStateItem>();
		if (localStateHistory.size() > 0) {
			int[] arrayHorizonsMinutes = { 10, 20, 30, 40, 50, 60 };
			int maxHorizon = 0;
			for(int nextHorizon : arrayHorizonsMinutes) {
				if(nextHorizon > maxHorizon) {
					maxHorizon = nextHorizon;
				}
			}
			Date currentDate = predictionContext.getCurrentDate();
			TreeMap<Date, SingleNodeStateItem>  mapSelectedHistory = SapereUtil.selectMapHistory
					(localStateHistory, UtilDates.shiftDateMinutes(currentDate, -(maxHorizon+5)), currentDate);
			if(mapSelectedHistory.size() > 0)  {
				List<Date> listDates = new ArrayList<Date>();
				for(Date nextDate : mapSelectedHistory.keySet()) {
					listDates.add(nextDate);
				}
				Date initialDate = listDates.get(0);
				mapTargetItems.put(initialDate, mapSelectedHistory.get(initialDate));
				List<Date> listTargetDates = new ArrayList<Date>();
				for (int nextHorizonMinutes : arrayHorizonsMinutes) {
					Date nextTargetDate = UtilDates.shiftDateMinutes(initialDate, nextHorizonMinutes);
					Date nextClosestDate = SapereUtil.getClosestDate(nextTargetDate, listDates);
					if(nextClosestDate != null && mapSelectedHistory.containsKey(nextClosestDate)) {
						listTargetDates.add(nextTargetDate);
						mapTargetItems.put(nextTargetDate, mapSelectedHistory.get(nextClosestDate));
					}
				}
			}
		}
		return mapTargetItems;
	}

	private static Map<String, Map<String, Double>> evaluateModelsCrossEntropy(
			Map<String, ILearningModel> mapLearningModels
			,List<SingleNodeStateItem> localStateHistory
			,PredictionContext predictionContext
			, AbstractLogger logger) throws HandlingException {
		// Map of cross entropy by node and by variable
		Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();
		Map<Date, SingleNodeStateItem> mapTargetItems = selectTargetItems(mapLearningModels, localStateHistory, predictionContext, logger);
		if (mapTargetItems.size() > 0) {
			Date initialDate =  mapTargetItems.keySet().iterator().next();
			String[] listVariables = predictionContext.getNodeContext().getVariables();
			List<Date> listTargetDates = new ArrayList<Date>();
			for(Date nextDate : mapTargetItems.keySet()) {
				if(nextDate.after(initialDate)) {
					listTargetDates.add(nextDate);
				}
			}
			for (String nextNode : mapLearningModels.keySet()) {
				ILearningModel nextModel = mapLearningModels.get(nextNode);
				PredictionData predictionData = nextModel.computePrediction(initialDate, listTargetDates,
						listVariables, false, logger);
				// Select state items to compute state distribution.
				for (String variable : listVariables) {
					List<Double> listCrossEntropy = new ArrayList<Double>();
					predictionData.computeMapStateStatistics(variable, localStateHistory);
					Map<FeaturesKey, StatesStatistic> mapStatesStatistics = predictionData.getMapStatesStatistics();
					for (Date nextTargetDate : listTargetDates) {
						PredictionResult predictionResult = predictionData.getResult(variable, nextTargetDate);
						if (predictionResult != null && mapTargetItems.containsKey(nextTargetDate)) {
							SingleNodeStateItem targetStateItem = mapTargetItems.get(nextTargetDate);
							FeaturesKey targetFeaturesKey = predictionResult.getTargetFeaturesKey();
							predictionResult.setActualTargetValue(targetStateItem.getValue(variable));
							predictionResult
									.setActualTargetState(NodeStates.getById(targetStateItem.getStateId(variable)));
							if("produced".equals(variable)) {
								logger.info("AbstractLearningModel.evaluateModelsCrossEntropy " + variable + " " + nextNode
										+ ": predictionResult = " + predictionResult);
							}
							// get state distribution
							StatesStatistic statesStatistic = mapStatesStatistics.get(targetFeaturesKey);
							predictionResult.setActualStatesStatistic(statesStatistic);
							Double crossEntroy = predictionResult.computeCrossEntroy(logger);
							if (crossEntroy != null) {
								listCrossEntropy.add(crossEntroy);
							} else {
								logger.info("AbstractLearningModel.evaluateModelsCrossEntropy : cross entropy is null");
							}
						}
					}
					if (listCrossEntropy.size() > 0) {
						// compute the mean of cross entropy
						Double meanCrossEntropy = SapereUtil.auxComputeAvg(listCrossEntropy);
						if (!result.containsKey(variable)) {
							result.put(variable, new HashMap<String, Double>());
						}
						Map<String, Double> variableResult = result.get(variable);
						variableResult.put(nextNode, meanCrossEntropy);
						logger.info("AbstractLearningModel.evaluateModelsCrossEntropy : cross entropy[" + variable + "][" + nextNode
								+ "] = " + UtilDates.df3.format(meanCrossEntropy));
					}
				}
			}
		}
		return result;
	}

	protected static Map<String, Map<String, Double>> evaluateModelsWeigts(
			Map<String, ILearningModel> mapLearningModels, AgentAuthentication agentAuthentication,
			PredictionContext predictionContext, String operator, AbstractLogger logger) {
		String localAgent = agentAuthentication.getAgentName();
		Map<String, Map<String, Double>> weightTable = new HashMap<String, Map<String, Double>>();
		String[] listVariables = predictionContext.getNodeContext().getVariables();
		int nbOfModels = mapLearningModels.size();
		double defaultNoramlizedValue = 1.0 / ((double) nbOfModels);
		logger.info("LearningModel.evaluateModelsWeigts : defaultNoramlizedValue = " + defaultNoramlizedValue);
		if (OP_MIN_LOSS.equals(operator) || OP_POWER_LOSS.equals(operator)) {
			List<SingleNodeStateItem> localStateHistory = AbstractLearningModel
					.retrieveLocalStateHistory(mapLearningModels, localAgent, logger);
			Map<String, Map<String, Double>> modelsCrossEntropy = new HashMap<String, Map<String, Double>>();
			try {
				modelsCrossEntropy = evaluateModelsCrossEntropy(mapLearningModels, localStateHistory, predictionContext,
						logger);
			} catch (HandlingException e) {
				logger.error(e);
			}
			if (OP_POWER_LOSS.equals(operator)) {
				for (String nextVariable : modelsCrossEntropy.keySet()) {
					// get cross entropy table of next node
					Map<String, Double> varCrossEntropy = modelsCrossEntropy.get(nextVariable);
					Map<String, Double> varResult = new HashMap<String, Double>();
					for (String nextNode : varCrossEntropy.keySet()) {
						double crossEntropy = varCrossEntropy.get(nextNode);
						double crossEntropyPower = Math.pow(10, -1 * crossEntropy);
						if (Double.isInfinite(crossEntropy)) {
							logger.info(" infinite crossEntropy for " + nextVariable + " " + nextNode
									+ " + : crossEntropyPower = " + crossEntropyPower);
						}
						varResult.put(nextNode, crossEntropyPower);
					}
					weightTable.put(nextVariable, varResult);
				}
			}
			if (OP_MIN_LOSS.equals(operator)) {
				for (String variable : listVariables) {
					// Retrieve the outputforming node
					String outperformingNode = getOutperformingNode(modelsCrossEntropy, variable);
					if (outperformingNode != null) {
						// mapOutperformingNodes.put(variable, outperformingNode);
						Map<String, Double> variableResult = new HashMap<String, Double>();
						for (String nextNode : mapLearningModels.keySet()) {
							double weight = 0;
							if (nextNode.equals(outperformingNode)) {
								weight = 1.0;
							}
							variableResult.put(nextNode, weight);
						}
						weightTable.put(variable, variableResult);
					}
				}
			}
		} else if (OP_SAMPLING_NB.equals(operator)) {
			for (String variable : listVariables) {
				Map<String, Double> variableWeightTable = new HashMap<String, Double>();
				for (String nextNode : mapLearningModels.keySet()) {
					ILearningModel nodeModel = mapLearningModels.get(nextNode);
					double samplingNb = nodeModel.getSamplingNb(variable);
					// boolean isLocal = localNode.equals(nextNode);
					// double weight = isLocal ? 10. : 1.0;
					variableWeightTable.put(nextNode, samplingNb);
				}
				weightTable.put(variable, variableWeightTable);
			}
		} else if(OP_DISTANCE_POWER_PROFILE.equals(operator)) {
			List<SingleNodeStateItem> localStateHistory = AbstractLearningModel
					.retrieveLocalStateHistory(mapLearningModels, localAgent, logger);
			double maxTotalPower = predictionContext.getNodeContext().getMaxTotalPower();
			int statesNb = NodeStates.getNbOfStates();
			double distanceDivisor = maxTotalPower / statesNb;
			for (String nextNode : mapLearningModels.keySet()) {
				ILearningModel nodeModel = mapLearningModels.get(nextNode);
				Map<String, Double> mapDistance = SapereUtil.computeProfileDistance(localStateHistory,
						nodeModel.getStateHistory(), logger);
				for (String variable : predictionContext.getNodeContext().getVariables()) {
					if (mapDistance.containsKey(variable)) {
						double distance = mapDistance.get(variable);
						double nodeWeight = Math.pow(10, -1 * distance / distanceDivisor);
						if(!weightTable.containsKey(variable)) {
							weightTable.put(variable, new HashMap<String, Double>());
						}
						Map<String, Double> variableWeightTable = weightTable.get(variable);
						variableWeightTable.put(nextNode, nodeWeight);
					}
				}
			}
		}
		logger.info("LearningModel.evaluateModelsWeigts : complete result = " + weightTable);
		// Normalize the weight table
		for (String variable : weightTable.keySet()) {
			Map<String, Double> variableWeightTable = weightTable.get(variable);
			double variableTotal = 0;
			for (String nextNode : variableWeightTable.keySet()) {
				variableTotal += variableWeightTable.get(nextNode);
			}
			if (variableTotal == 0) {
				logger.warning("LearningModel.evaluateModelsWeigts : " + variable + " : variableTotal is 0");
			}
			for (String nextNode : variableWeightTable.keySet()) {
				double noramlizedValue = defaultNoramlizedValue;
				// Check if the total of weight coefficients is not null.
				if (variableTotal > 0) {
					noramlizedValue = variableWeightTable.get(nextNode) / variableTotal;
				}
				variableWeightTable.put(nextNode, noramlizedValue);
			}
		}
		for (String variable : weightTable.keySet()) {
			Map<String, Double> variableWeightTable = weightTable.get(variable);
			for (String nextNode : variableWeightTable.keySet()) {
				Double nextWeight = variableWeightTable.get(nextNode);
				logger.info("LearningModel.evaluateModelsWeigts : " + variable + " weigth[" + nextNode + "] = "
						+ UtilDates.df3.format(nextWeight));
			}
		}
		return weightTable;
	}

	public static String getOutperformingNode(Map<String, Map<String, Double>> modelsCrossEntropy, String variable) {
		String result = null;
		Double minCrossEntropy = null;
		if (modelsCrossEntropy.containsKey(variable)) {
			Map<String, Double> variableCrossEntropy = modelsCrossEntropy.get(variable);
			for (String nextNode : variableCrossEntropy.keySet()) {
				// get cross entropy of next node
				Double nextCrossEntropy = variableCrossEntropy.get(nextNode);
				if (minCrossEntropy == null || nextCrossEntropy < minCrossEntropy) {
					minCrossEntropy = nextCrossEntropy;
					result = nextNode;
				}
			}
		}
		return result;
	}


	public void fillCompactModel(AbstractCompactedModel result, AbstractLogger logger) {
		result.setCurrentCodeFeaturesKey(currentFeaturesKey.getCode());
		result.setLastUpdate(lastUpdate);
		result.setPredictionContext(predictionContext.clone());
		if (aggregationWeights != null && aggregationWeights.size() > 0) {
			result.setAggregationWeights(SapereUtil.cloneMap2StringDouble(aggregationWeights));
		}
		if (nodeStateTransitions != null) {
			result.setNodeStatevTransitions(nodeStateTransitions.generateCompactNodeStatesTransitions());
		}
		result.setComplete(this.isComplete());
		List<Date> historyDates = new ArrayList<Date>();
		List<Double[]> historyValues = new ArrayList<Double[]>();
		List<Integer[]> historyStateIds = new ArrayList<Integer[]>();
		String[] listVariables = predictionContext.getNodeContext().getVariables();
		try {
			for (SingleNodeStateItem nextStateItem : this.stateHistory) {
				historyDates.add(nextStateItem.getDate());
				List<Double> values = new ArrayList<Double>();
				List<Integer> stateIds = new ArrayList<Integer>();
				for (String variable : listVariables) {
					Double nextValue = nextStateItem.getValue(variable);
					values.add(nextValue == null ? -1 : nextValue);
					Integer nextStateId = nextStateItem.getStateId(variable);
					stateIds.add(nextStateId == null ? -1 : nextStateId);
				}
				try {
					Double[] valuesArray = (Double[]) values.toArray(new Double[values.size()]);
					historyValues.add(valuesArray);
					Integer[] statteIdsArray = (Integer[]) stateIds.toArray(new Integer[stateIds.size()]);
					historyStateIds.add(statteIdsArray);
				} catch (Exception e) {
					logger.error(e);
				}
			}
			Date[] historyDatesArray = (Date[]) historyDates.toArray(new Date[historyDates.size()]);
			result.setHistoryDates(historyDatesArray);
			Double[][] historyValuesArray = (Double[][]) historyValues.toArray(new Double[historyValues.size()][]);
			result.setHistoryValues(historyValuesArray);
			Integer[][] historyStateIdsArray = (Integer[][]) historyStateIds
					.toArray(new Integer[historyStateIds.size()][]);
			result.setHistoryStateIds(historyStateIdsArray);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public void fillAggregationResult(
			AggregationCheckupRequest fedAvgCheckupRequest
			,AbstractAggregationResult aggregationResult
			,Map<String, ILearningModel> receivedModels
			,AbstractLogger logger
			) {
		String variableName = fedAvgCheckupRequest.getVariableName();
		try {
			boolean isAggregated = (aggregationDate != null);
			logger.info("AbstractLearningModel.checkupFedAVG aggregationWeights = " + aggregationWeights);
			//LSTMAggregationResult result = new LSTMAggregationResult();
			aggregationResult.setVariableName(variableName);
			aggregationResult.setAggragationOperator(predictionContext.getAggregationOperator());
			aggregationResult.setAggregationDate(this.aggregationDate);
			if (isAggregated && aggregationWeights.containsKey(variableName)) {
				aggregationResult.setAggregationWeights(
						SapereUtil.cloneMapStringDouble(aggregationWeights.get(variableName)));
			}
			Map<String, String> mapNodes = new HashMap<String, String>();
			for (String nextAgent : receivedModels.keySet()) {
				ILearningModel nextModel = receivedModels.get(nextAgent);
				String modelNode = nextModel.getPredictionContext().getNodeLocation().getName();
				mapNodes.put(nextAgent, modelNode);
			}
			aggregationResult.setMapNodeByAgent(mapNodes);
		} catch (Throwable e) {
			logger.error(e);
		}
		logger.info("AbstractLearningModel.fillAggregationResult : end");
	}


	public PredictionData computePrediction2(PredictionRequest predictionRequest, AbstractLogger logger)
			throws HandlingException {
		double maxTotalPower = predictionContext.getNodeContext().getMaxTotalPower();
		String[] variables = predictionContext.getNodeContext().getVariables();
		Date targetDate = new Date(predictionRequest.getLongTargetDate());
		List<Date> targetDates = new ArrayList<Date>();
		targetDates.add(targetDate);
		Date initDate = new Date(predictionRequest.getLongInitDate() - (predictionRequest.getLongInitDate() % 1000));
		boolean useCorrections = predictionRequest.isUseCorrections();
		logger.info("tcomputePrediction2 : argetDate = " + targetDate);
		NodeStatesTransitions initialTransitions = PredictionDbHelper.loadClosestNodeStateTransition(predictionContext,
				initDate, variables, maxTotalPower);
		if (initialTransitions == null) {
			PredictionData emptyPrediction = new PredictionData(predictionContext, useCorrections);
			emptyPrediction.addError("computePrediction2 : No state arround " + UtilDates.format_sql.format(initDate)
					+ " for the scenario " + predictionContext.getScenario());
			return emptyPrediction;
		}
		PredictionData result = this.computePrediction(initDate, targetDates, variables, useCorrections, logger);
		for (Date nextTargetDate : targetDates) {
			NodeStatesTransitions finalTransitions = PredictionDbHelper.loadClosestNodeStateTransition(predictionContext,
					nextTargetDate, variables, maxTotalPower);
			if (finalTransitions != null) {
				// result.setActualTargetState(finalTransitions);
				for (String variable : variables) {
					VariableState actualTargetState = finalTransitions.getCurrentState(variable);
					Double actualTargetValue = finalTransitions.getMapValues().get(variable);
					result.setActualTargetState(variable, nextTargetDate, actualTargetState, actualTargetValue);
				}
			}
		}
		return result;
	}

	public MultiPredictionsData generateMassivePredictions(
			 MassivePredictionRequest massivePredictionRequest
			,AbstractLogger logger
			) throws HandlingException {
		// Just for test
		boolean evaluateGeneralEntropie = false;
		if(evaluateGeneralEntropie) {
			PredictionDbHelper.evaluateAllStatesEntropie();
		}
		TimeSlot targetDateSlot = massivePredictionRequest.getTimeSlot();
		logger.info("generateMassivePredictions : targetDateMin = " + targetDateSlot.getBeginDate() + ", targetDateMax = "
				+ targetDateSlot.getEndDate());
		int horizonMinutes = massivePredictionRequest.getHorizonInMinutes();
		boolean useCorrections = massivePredictionRequest.isUseCorrections();
		boolean generateCorrections = massivePredictionRequest.isGenerateCorrections();
		boolean savePredictions = massivePredictionRequest.isSavePredictions();
		String[] listVariableNames = massivePredictionRequest.getListVariableNames();
		//Date computeDay = UtilDates.removeTime(new Date());
		List<PredictionData> listPredictions = new ArrayList<>();
		//boolean useCorrections = true;
		Date firstInitDate = UtilDates.shiftDateMinutes(targetDateSlot.getBeginDate(), -1 * horizonMinutes);
		PredictionScope scope = predictionContext.getScope();
		MultiPredictionsData result = new MultiPredictionsData(predictionContext, targetDateSlot, listVariableNames);
		// Load existing actual states for this period
		double maxTotalPower = predictionContext.getNodeContext().getMaxTotalPower();
		Map<Date, NodeStatesTransitions> mapAllTransition = PredictionDbHelper.loadMapNodeStateTransitions(predictionContext,
				new TimeSlot(UtilDates.removeMinutes(firstInitDate), UtilDates.getCeilHourStart(targetDateSlot.getEndDate()))
				, listVariableNames, maxTotalPower);
		// Load existing actual states statistics
		//Date minTargetDate = targetDateSlot.getBeginDate();
		//Date maxTargetDate = targetDateSlot.getEndDate();
		int minHour = UtilDates.getHourOfDay(firstInitDate);
		int maxHour = UtilDates.getHourOfDay(targetDateSlot.getEndDate());
		Map<String, StatesStatistic> mapStatesStatistics = PredictionDbHelper.retrieveStatesStatistics(predictionContext
				, UtilDates.removeTime(firstInitDate)
				, UtilDates.removeTime(targetDateSlot.getEndDate())
				, minHour, maxHour, listVariableNames);
		// idem for neighbor nodes
		if(listVariableNames.length == 0) {
			result.addError("The variable filter has not been entered");
		} else if(mapAllTransition.size()==0) {
			result.addError("No state found during the requested time slot for the scenario " + predictionContext.getScenario());
		}
		List<Date> listStateDates = new ArrayList<>();
		for (Date aDate : mapAllTransition.keySet()) {
			listStateDates.add(aDate);
		}
		List<PredictionResult> listResult = new ArrayList<>();
		Collections.sort(listStateDates);
		if(listStateDates.size() > 0) {
			//Date firstDate = listStateDates.get(0);
			//learningModel = PredictionDbHelper.loadCompleteLearningModel(learningModel, firstDate);
			for (Date nextInitDate : listStateDates) {
				Date nextTargetDate = UtilDates.shiftDateMinutes(nextInitDate, 1 * horizonMinutes);
				if (!nextTargetDate.before(targetDateSlot.getBeginDate()) && nextTargetDate.before(targetDateSlot.getEndDate())) {
					List<Date> targetDates = new ArrayList<Date>();
					targetDates.add(nextTargetDate);
					PredictionData predictionData = computePrediction(nextInitDate, targetDates, listVariableNames, useCorrections, logger);
					// search closest result to nextTargetDate
					Date closestDate = SapereUtil.getClosestDate(nextTargetDate, listStateDates);
					for(String variableName : listVariableNames ) {
						String statesStatisticsKey  = StatesStatistic.generateKey(scope, closestDate, UtilDates.getHourOfDay(closestDate), variableName);
						if(mapStatesStatistics.containsKey(statesStatisticsKey)) {
							StatesStatistic statesStatistic = mapStatesStatistics.get(statesStatisticsKey);
							predictionData.setActualStatesStatistics(variableName, nextTargetDate, statesStatistic);
						}
						NodeStatesTransitions targetTransition = mapAllTransition.get(closestDate);
						//predictionData.setActualTargetState(targetTransition);
						VariableState actualTargetState = targetTransition.getCurrentState(variableName);
						Double actualTargetValue = targetTransition.getMapValues().get(variableName);
						predictionData.setActualTargetState(variableName, nextTargetDate, actualTargetState, actualTargetValue);
						PredictionResult predictionResult = predictionData.getLastResult(variableName);
						listResult.add(predictionResult);
					}
					listPredictions.add(predictionData);
					if(savePredictions) {
						PredictionDbHelper.savePredictionResult(predictionData);
					}
				}
			}
		}
		// TODO : call self-correction
		if(useCorrections && generateCorrections) {
			Map<VariableFeaturesKey, Map<VariableState, PredictionCorrection>> mapCorrections =
					PredictionHelper.auxGenerateCorrections(predictionContext, listResult, "generateMassivePredictions");
			for(Map<VariableState, PredictionCorrection> mapCorrections1 : mapCorrections.values()) {
				for(PredictionCorrection correction : mapCorrections1.values()) {
					result.addCorrection(correction);
				}
			}
		}
		for(PredictionData nextPrediction : listPredictions) {
			result.addPrediction(nextPrediction, logger);
		}
		// compute state entropie
		Map<VariableFeaturesKey, Double> entropieByTrMatrix = PredictionHelper.computeStateEntropie(predictionContext, mapAllTransition, listVariableNames);
		for(VariableFeaturesKey nextKey : entropieByTrMatrix.keySet()) {
			result.addEntropieResult(nextKey, entropieByTrMatrix.get(nextKey));
		}
		if(savePredictions) {
			try {
				PredictionDbHelper.consolidatePredictions(predictionContext);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return result;
	}

	public boolean isReadyForAggregation() {
		if(this.hasModelAggregator() && this.isComplete() && lastUpdate != null) {
			Date lastAggregation = this.getAggregationDate();
			//Date current = predictionContext.getCurrentDate();
			int waitingMinutesBetweenAggragations = this.predictionContext.getAggregationOperator().getWaitingMinutesBetweenAggragations();
			Date minAggregationDate = lastAggregation == null ? lastUpdate : UtilDates.shiftDateMinutes(lastAggregation, waitingMinutesBetweenAggragations);
			if(lastAggregation == null || lastUpdate.after(minAggregationDate)) {
				// send input for a new aggregation
				return true;
			}
		}
		return false;
	}
}
