package com.sapereapi.model.learning.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.AbstractAggregatable;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class PredictionData extends AbstractAggregatable implements IAggregateable {
	private static final long serialVersionUID = 1L;
	private List<String> variables;
	private Date initialDate;
	private SortedSet<Date> targetDates = new TreeSet<Date>();
	private List<PredictionStep> listSteps;
	private Map<String, VariableState> initialStates;
	private Map<String, Double> initialValues;
	private Map<String, Map<Date, PredictionResult>> mapResults;
	//private Map<String, PredictionResult> mapLastResults;
	private Map<FeaturesKey, StatesStatistic> mapStatesStatistics = new HashMap<FeaturesKey, StatesStatistic>();
	private List<String> errors;
	private PredictionContext context = new PredictionContext();
	private Boolean useCorrections = false;
	private List<SingleNodeStateItem> stateHistory = new ArrayList<SingleNodeStateItem>();  // Optionnal : can be used for aggregation.
	protected Map<String, Map<String, Double>> aggregationWeights = new HashMap<String, Map<String, Double>>(); // Optionnal : can be used for aggregation.

	public static final String OP_DISTANCE_POWER_PROFILE = "dist_power_hist";
	public static final String OP_DISTANCE_CURRENT_POWER = "dist_power_last";

	public PredictionData() {
		super();
		reset();
	}

	public PredictionData(PredictionContext _context, boolean _useCorrections) {
		super();
		context = _context;
		useCorrections = _useCorrections;
		reset();
		lastUpdate = new Date();
	}

	private void reset() {
		listSteps = new ArrayList<PredictionStep>();
		targetDates = new TreeSet<Date>();
		mapResults = new HashMap<String, Map<Date, PredictionResult>>();
		//mapLastResults = new HashMap<String, PredictionResult>();
		errors = new ArrayList<String>();
		mapStatesStatistics = new HashMap<FeaturesKey, StatesStatistic>();
		lastUpdate = new Date();
	}

	public List<String> getVariables() {
		return variables;
	}

	public void setVariables(String[] _variables) {
		this.variables = Arrays.asList(_variables);
	}

	public Date getInitialDate() {
		return initialDate;
	}

	public void setInitialDate(Date initialDate) {
		this.initialDate = initialDate;
	}

	public Map<String, VariableState> getInitialStates() {
		return initialStates;
	}

	public Map<FeaturesKey, StatesStatistic> getMapStatesStatistics() {
		return mapStatesStatistics;
	}

	public void setMapStatesStatistics(Map<FeaturesKey, StatesStatistic> mapStatesStatistics) {
		this.mapStatesStatistics = mapStatesStatistics;
	}

	public void setInitialStates(Map<String, VariableState> initialStates) {
		this.initialStates = initialStates;
	}

	public void setInitialContent(NodeStatesTransitions transitions) {
		this.initialStates = transitions.getMapStates();
		this.initialValues = transitions.getMapValues();
	}

	public boolean hasInitialContent() {
		return initialStates != null && initialValues != null && initialStates.size() > 0 && initialValues.size() > 0;
	}

	public void addTargetDate(Date aDate) {
		targetDates.add(aDate);
	}

	public Date getLastTargetDate() {
		if (targetDates.size() > 0) {
			return targetDates.last();
		}
		return null;// targetDate;
	}

	public Map<String, PredictionResult> getMapLastResults() {
		Map<String, PredictionResult> result = new HashMap<String, PredictionResult>();
		Date lastTargetDate = getLastTargetDate();
		for (String variable : mapResults.keySet()) {
			Map<Date, PredictionResult> mapResult2 = mapResults.get(variable);
			if (mapResult2.containsKey(lastTargetDate)) {
				PredictionResult predictionResult = mapResult2.get(lastTargetDate);
				result.put(variable, predictionResult);
			}
		}
		return result;
	}

	public boolean isUseCorrections() {
		return useCorrections;
	}

	public void setUseCorrections(boolean useCorrections) {
		this.useCorrections = useCorrections;
	}

	public List<SingleNodeStateItem> getStateHistory() {
		return stateHistory;
	}

	public void setStateHistory(List<SingleNodeStateItem> stateHistory) {
		this.stateHistory = stateHistory;
	}

	public Map<String, Map<String, Double>> getAggregationWeights() {
		return aggregationWeights;
	}

	public void setAggregationWeights(Map<String, Map<String, Double>> aggregationWeights) {
		this.aggregationWeights = aggregationWeights;
	}

	public void addError(String error) {
		if (!errors.contains(error)) {
			errors.add(error);
		}
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public PredictionStep getFirstStep() {
		if (listSteps.size() > 0) {
			return listSteps.get(0);
		}
		return null;
	}

	public PredictionStep getLastStep(Date targetDate) {
		for (PredictionStep nextStep : listSteps) {
			if (nextStep.isInSlot(targetDate)) {
				return nextStep;
			}
		}
		return null;
	}

	public PredictionStep getLastStep() {
		if (listSteps.size() > 0) {
			int idx = listSteps.size() - 1;
			return listSteps.get(idx);
		}
		return null;
	}

	public List<PredictionStep> getListSteps() {
		return listSteps;
	}

	public void setListSteps(List<PredictionStep> _listSteps) {
		this.listSteps = _listSteps;
	}

	public Map<String, Double> getInitialValues() {
		return initialValues;
	}

	public void setInitialValues(Map<String, Double> initialValues) {
		this.initialValues = initialValues;
	}

	public Map<String, Map<Date, PredictionResult>> getMapResults() {
		return mapResults;
	}

	public void setMapResults(Map<String, Map<Date, PredictionResult>> mapResults) {
		this.mapResults = mapResults;
	}

	public FeaturesKey getInitialFeaturesKey() {
		if (listSteps.size() > 0) {
			PredictionStep firstStep = listSteps.get(0);
			return firstStep.getFeaturesKey();
		}
		return null;
	}

	public List<Long> getListTransitionMatrixId(String variable) {
		List<Long> result = new ArrayList<Long>();
		for (PredictionStep nextStep : listSteps) {
			long nextId = nextStep.getUsedTransitionMatrixId(variable);
			if (!result.contains(nextId)) {
				result.add(nextId);
			}
		}
		return result;
	}

	public List<VariableFeaturesKey> getListTransitionMatrixKeys(String variable) {
		List<VariableFeaturesKey> result = new ArrayList<VariableFeaturesKey>();
		for (PredictionStep nextStep : listSteps) {
			VariableFeaturesKey nextKey = nextStep.getUsedTransitionMatrixKey(variable, context);
			if (nextKey != null && !result.contains(nextKey)) {
				result.add(nextKey);

			}
		}
		return result;
	}

	public List<Integer> getListTimeWindowId() {
		List<Integer> result = new ArrayList<Integer>();
		for (PredictionStep nextStep : listSteps) {
			int nextId = nextStep.getMarovTimeWindowId();
			if (!result.contains(nextId)) {
				result.add(nextId);
			}
		}
		return result;
	}

	public void setResult(String variable, Date targetDate, PredictionResult result) {
		if (targetDates.contains(targetDate) && variables.contains(variable)) {
			if (!mapResults.containsKey(variable)) {
				mapResults.put(variable, new HashMap<Date, PredictionResult>());
			}
			Map<Date, PredictionResult> mapResult2 = this.mapResults.get(variable);
			mapResult2.put(targetDate, result);
			lastUpdate = new Date();
		}
	}

	public boolean hasResult(String variable, Date targetDate) {
		return mapResults.containsKey(variable) && (mapResults.get(variable)).containsKey(targetDate);
	}

	public boolean hasLastResult(String variable) {
		if(mapResults.containsKey(variable)) {
			Map<Date, PredictionResult> mapResult2 = this.mapResults.get(variable);
			Date lastTargetDate = getLastTargetDate();
			return mapResult2.containsKey(lastTargetDate);
		}
		return false;
	}

	public PredictionResult getResult(String variable, Date targetDate) {
		if (hasResult(variable, targetDate)) {
			return (mapResults.get(variable)).get(targetDate);
		}
		return null;
	}

	public PredictionResult getClosestResult(String variable, Date targetDate, int maxGapMS) {
		Date closestDate = SapereUtil.getClosestDate(targetDate, targetDates);
		if (closestDate != null) {
			long gapMS = Math.abs(closestDate.getTime() - targetDate.getTime());
			long gapSeconds = gapMS / 1000;
			if (gapSeconds <= maxGapMS) {
				return getResult(variable, closestDate);
			}
		}
		return null;
	}

	public PredictionResult getLastResult(String variable) {
		if(mapResults.containsKey(variable)) {
			Map<Date, PredictionResult> mapResult2 = this.mapResults.get(variable);
			Date lastTargetDate = getLastTargetDate();
			if(mapResult2.containsKey(lastTargetDate)) {
				return mapResult2.get(lastTargetDate);
			}
		}
		return null;
	}

	public List<FeaturesKey> getListTargetFeatureKeys(String variable) {
		Map<Date, PredictionResult> mapVarResult = mapResults.get(variable);
		List<FeaturesKey> result = new ArrayList<FeaturesKey>();
		for (Date nextDate : mapVarResult.keySet()) {
			PredictionResult nextResult = mapVarResult.get(nextDate);
			if (!result.contains(nextResult.getTargetFeaturesKey())) {
				result.add(nextResult.getTargetFeaturesKey());
			}
		}
		return result;
	}

	public void computeMapStateStatistics(String variable, List<SingleNodeStateItem> localStateHistory) {
		mapStatesStatistics = new HashMap<FeaturesKey, StatesStatistic>();
		Map<Date, PredictionResult> mapVariableResults = mapResults.get(variable);
		for (Date nextTargetDate : mapVariableResults.keySet()) {
			PredictionResult nextPredictionResult = mapVariableResults.get(nextTargetDate);
			FeaturesKey nextFeaturesKey = nextPredictionResult.getTargetFeaturesKey();
			if (!mapStatesStatistics.containsKey(nextFeaturesKey)) {
				StatesStatistic statesStatistic = nextPredictionResult.computeStateStatistic(context,
						localStateHistory);
				mapStatesStatistics.put(nextFeaturesKey, statesStatistic);
			}
			StatesStatistic statesStatistic = mapStatesStatistics.get(nextFeaturesKey);
			nextPredictionResult.setActualStatesStatistic(statesStatistic);
		}
		lastUpdate = new Date();
	}

	public Map<FeaturesKey, List<Double>> getMeanStateProbabilities(String variable) {
		Map<Date, PredictionResult> mapVariableResults = mapResults.get(variable);
		Map<FeaturesKey, Map<Integer, List<Double>>> mapAllStatesProbabilities = new HashMap<FeaturesKey, Map<Integer, List<Double>>>();
		for (Date nextTargetDate : mapVariableResults.keySet()) {
			PredictionResult nextPredictionResult = mapVariableResults.get(nextTargetDate);
			FeaturesKey nextFeaturesKey = nextPredictionResult.getTargetFeaturesKey();
			if (!mapAllStatesProbabilities.containsKey(nextFeaturesKey)) {
				mapAllStatesProbabilities.put(nextFeaturesKey, new HashMap<Integer, List<Double>>());
			}
			Map<Integer, List<Double>> mapAllStatesProbabilities2 = mapAllStatesProbabilities.get(nextFeaturesKey);
			List<Double> stateProbabilities = nextPredictionResult.getStateProbabilities();
			int stateIdx = 0;
			for (Double nextStateProba : stateProbabilities) {
				if (!mapAllStatesProbabilities2.containsKey(stateIdx)) {
					mapAllStatesProbabilities2.put(stateIdx, new ArrayList<Double>());
				}
				List<Double> listValues = mapAllStatesProbabilities2.get(stateIdx);
				listValues.add(nextStateProba);
				stateIdx++;
			}
		}
		Map<FeaturesKey, List<Double>> result = new HashMap<FeaturesKey, List<Double>>();
		for (FeaturesKey nextFeaturesKey : mapAllStatesProbabilities.keySet()) {
			List<Double> listMeanProbabilities = new ArrayList<Double>();
			Map<Integer, List<Double>> mapAllStatesProbabilities2 = mapAllStatesProbabilities.get(nextFeaturesKey);
			for (Integer stateIdx : mapAllStatesProbabilities2.keySet()) {
				List<Double> listValues = mapAllStatesProbabilities2.get(stateIdx);
				Double meanValue = SapereUtil.auxComputeAvg(listValues);
				listMeanProbabilities.add(meanValue);
			}
			result.put(nextFeaturesKey, listMeanProbabilities);
		}
		return result;
	}

	public Double computeMeanCrossEntropy(String variable, AbstractLogger logger,
			List<SingleNodeStateItem> localStateHistory) {
		Map<FeaturesKey, List<Double>> meanStateProbabilities = getMeanStateProbabilities(variable);
		computeMapStateStatistics(variable, localStateHistory);
		List<Double> listCrossEntropy = new ArrayList<Double>();
		for (FeaturesKey featureKey : meanStateProbabilities.keySet()) {
			List<Double> stateProbabilities = meanStateProbabilities.get(featureKey);
			StatesStatistic actualStatesStatistic = mapStatesStatistics.get(featureKey);
			List<Double> statesDistribution = actualStatesStatistic.getArrayStateDistribution();
			Double nextCrossEntropy = SapereUtil.computeCrossEntroy(logger, statesDistribution, stateProbabilities);
			listCrossEntropy.add(nextCrossEntropy);
		}
		Double meanCrossEntropy = SapereUtil.auxComputeAvg(listCrossEntropy);
		lastUpdate = new Date();
		return meanCrossEntropy;
	}

	public VariableState getLastRandomTargetState(String variable) {
		PredictionResult result = getLastResult(variable);
		if (result != null) {
			return result.getRandomTargetState();
		}
		return null;
	}

	public void generateRandomState() {
		for (String variable : mapResults.keySet()) {
			Map<Date, PredictionResult> varResults = mapResults.get(variable);
			for (PredictionResult nextResult : varResults.values()) {
				nextResult.generateRandomState();
			}
		}
		lastUpdate = new Date();
	}

	public PredictionContext getContext() {
		return context;
	}

	public void setContext(PredictionContext context) {
		this.context = context;
	}

	public SortedSet<Date> getTargetDates() {
		return targetDates;
	}

	public List<Integer> computeHorizons() {
		List<Integer> listHorizons = new ArrayList<Integer>();
		for (Date nextTargetDate : targetDates) {
			double nextHorizonDouble = Math.round(UtilDates.computeDurationMinutes(initialDate, nextTargetDate));
			int nextHorizon = (int) nextHorizonDouble;
			listHorizons.add(nextHorizon);
		}
		Collections.sort(listHorizons);
		return listHorizons;
	}

	public SortedSet<Date> getTargetDatesWithoutResult(String variable) {
		SortedSet<Date> result = new TreeSet<Date>();
		for (Date targetDate : this.targetDates) {
			if (!hasResult(variable, targetDate)) {
				result.add(targetDate);
			}
		}
		return result;
	}

	public void setVariables(List<String> variables) {
		this.variables = variables;
	}

	public void setTargetDates(SortedSet<Date> targetDates) {
		this.targetDates = targetDates;
	}
/*
	public void setMapLastResults(Map<String, PredictionResult> mapLastResults) {
		this.mapLastResults = mapLastResults;
		lastUpdate = new Date();
	}
*/
	public void setActualTargetState(String variable, Date targetDate, VariableState actualTargetState,
			Double actualTargetValue) {
		if (hasResult(variable, targetDate)) {
			PredictionResult predictionResult = (mapResults.get(variable)).get(targetDate);
			predictionResult.setActualTargetState(actualTargetState);
			predictionResult.setActualTargetValue(actualTargetValue);
			lastUpdate = new Date();
		}
	}

	public void setActualStatesStatistics(String variable, Date targetDate, StatesStatistic statesStatistic) {
		if (hasResult(variable, targetDate)) {
			PredictionResult predictionResult = (mapResults.get(variable)).get(targetDate);
			predictionResult.setActualStatesStatistic(statesStatistic);
			lastUpdate = new Date();
		}
	}

	public void setActualTargetState(NodeStatesTransitions targetTransition) {
		Date targetDate = targetTransition.getStateDate();
		for (String variable : targetTransition.getMapStates().keySet()) {
			VariableState state = targetTransition.getMapStates().get(variable);
			Double value = targetTransition.getMapValues().get(variable);
			setActualTargetState(variable, targetDate, state, value);
			lastUpdate = new Date();
		}
	}

	public static int getDeviationIndex(String key, List<PredictionDeviation> deviations) {
		int idx = 0;
		for (PredictionDeviation nextDeviation : deviations) {
			if (key.equals(nextDeviation.getKey())) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	public static List<String> getOpposedDeviationKeys(List<PredictionDeviation> deviations) {
		List<String> result = new ArrayList<>();
		PredictionDeviation opposedDeviation = null;
		for (int idx = 0; idx < deviations.size() && opposedDeviation == null; idx++) {
			PredictionDeviation nextDeviation = deviations.get(idx);
			List<PredictionDeviation> followingDeviations = deviations.subList(idx + 1, deviations.size() - 0);
			opposedDeviation = nextDeviation.getOpposedTo(followingDeviations);
			if (opposedDeviation != null) {
				result.add(nextDeviation.getKey());
				result.add(opposedDeviation.getKey());
			}
		}
		return result;
	}

	@Override
	public String toString() {
		long timeShiftMS = context.getTimeShiftMS();
		StringBuffer result = new StringBuffer();
		if(this.isAggregated()) {
			result.append(super.toString());
			result.append(SapereUtil.CR);
		}
		result.append(super.toString());
		result.append("PredictionData [scenario = ").append(context.getScenario()).append(" : ")
				.append(UtilDates.formatTimeOrDate(initialDate, timeShiftMS)).append(", initialStates=")
				.append(initialStates);
		result.append(SapereUtil.CR);
		for (Map<Date, PredictionResult> nextTableResult : mapResults.values()) {
			for (PredictionResult predResut : nextTableResult.values()) {
				result.append(predResut);
				result.append(SapereUtil.CR).append("");
			}
		}
		if(this.stateHistory.size() > 0) {
			result.append(SapereUtil.CR);
			SingleNodeStateItem firstItem = stateHistory.get(0);
			String sFistDate = UtilDates.format_time.format(firstItem.getDate());
			int idxLast = stateHistory.size() - 1;
			SingleNodeStateItem lastItem = stateHistory.get(idxLast);
			String sLastDate = UtilDates.format_time.format(lastItem.getDate());
			String sCurrent = UtilDates.format_time.format(context.getCurrentDate());
			result.append("stateHistory : size = ").append(stateHistory.size() + ", fistDate = " + sFistDate + ", lastDate" + sLastDate + ", current = " + sCurrent);
			// Check dates
			List<String> errors = checkDatesInHistory();
			for(String nextError : errors) {
				result.append(SapereUtil.CR).append(nextError);
			}
		}
		return result.toString();
	}

	public List<String> checkDatesInHistory() {
		List<String> errors = new ArrayList<String>();
		Date lastDate = null;
		for (SingleNodeStateItem nextItem : this.stateHistory) {
			Date nextDate = nextItem.getDate();
			if (lastDate != null && nextDate.before(lastDate)) {
				String nextError = "### pb in stateHistory : lastDate = " + UtilDates.format_time.format(lastDate)
						+ ", nextDate = " + UtilDates.format_time.format(nextDate);
				errors.add(nextError);

			}
			lastDate = nextDate;
		}
		return errors;
	}

	@Override
	public IAggregateable aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return PredictionData.aggregate2(operator, mapObjects, agentAuthentication, logger);
	}

	public static PredictionData aggregate2(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		Map<String, PredictionData> mapPredictionData = new HashMap<String, PredictionData>();
		for(String agentName : mapObjects.keySet()) {
			IAggregateable nextObj = mapObjects.get(agentName);
			if(nextObj instanceof PredictionData) {
				mapPredictionData.put(agentName, (PredictionData) nextObj);
			}
		}
		int nbOfObjects = mapPredictionData.size();
		PredictionData result = null;
		String localAgent = agentAuthentication.getAgentName();
		Map<String, Map<Date,Map<String,PredictionResult>>> mapAllPredictionResults = new HashMap<String, Map<Date, Map<String, PredictionResult>>>();
		if(mapPredictionData.containsKey(localAgent)) {
			PredictionData localPrediction = mapPredictionData.get(localAgent);
			result = new PredictionData();
			result.setListSteps(localPrediction.getListSteps());
			result.setInitialDate(localPrediction.getInitialDate());
			result.setContext(localPrediction.getContext());
			result.setVariables(localPrediction.getVariables());
			result.setInitialStates(localPrediction.getInitialStates());
			result.setInitialValues(localPrediction.getInitialValues());
			SortedSet<Date> targetDates = new TreeSet<Date>();
			for(Date nextDate : localPrediction.getTargetDates()) {
				targetDates.add(nextDate);
			}
			result.setTargetDates(targetDates);
		} else {
			logger.error("PredictionData.aggregate2 " + localAgent + " not in mapPredictionData; KeySet = " + mapPredictionData.keySet());
			return null;
		}
		int maxGapSeconds = 3*60;
		for (String agentName : mapPredictionData.keySet()) {
			PredictionData nextPredictionData = mapPredictionData.get(agentName);
			Date nextInitalDate = nextPredictionData.getInitialDate();
			long gapInSeconds = Math.abs(nextInitalDate.getTime() - result.getInitialDate().getTime())/1000;
			if(gapInSeconds > maxGapSeconds) {
				logger.error("PredictionData.aggregate2 : eccessive gap for initDate : " + gapInSeconds);
				return null;
			}
			for (String variable : nextPredictionData.getVariables()) {
				if (!mapAllPredictionResults.containsKey(variable)) {
					mapAllPredictionResults.put(variable, new HashMap<Date, Map<String,PredictionResult>>());
				}
				Map<Date, Map<String,PredictionResult>> mapVarPredictionResults = mapAllPredictionResults.get(variable);
				//PredictionResult nextPredictionResult = nextPredictionData.getLastResult(variable);
				for(Date targetDate : result.getTargetDates()) {
					// Retrieve the result that is closed to the given target date
					PredictionResult predictionResult = nextPredictionData.getClosestResult(variable,targetDate, maxGapSeconds);
					if(predictionResult != null) {
						if(!mapVarPredictionResults.containsKey(targetDate)) {
							mapVarPredictionResults.put(targetDate, new HashMap<String, PredictionResult>());
						}
						Map<String,PredictionResult> mapTargetdatePredictionResults = mapVarPredictionResults.get(targetDate);
						mapTargetdatePredictionResults.put(agentName, predictionResult);
					}
				}
				//mapVarPredictionResults.put(node, nextPredictionResult);
				//PredictionResult predResult = nextPred.getLastResult(variable);
				//mapPredictionResults.add(predResult);
				//mapAllPredictionResults.put(variable, mapVarPredictionResults);
			}
		}
		if (result != null) {
			Map<String, Map<String, Double>> weightTable = evaluatePredictionsWeigts(mapPredictionData, agentAuthentication, result.getContext(), operator, logger);
			result.setAggregationWeights(weightTable);
			for (String variable : mapAllPredictionResults.keySet()) {
				Map<String, Double> varWeightTable = weightTable.get(variable);
				if(varWeightTable.size() < nbOfObjects) {
					logger.error("PredictionData.aggregate2 : computed weightTable is incomplete for variable " + variable);
					return null;
				}
				Map<Date, Map<String, PredictionResult>> mapVarPredictionResults = mapAllPredictionResults.get(variable);
				for(Date targetDate : mapVarPredictionResults.keySet()) {
					Map<String, PredictionResult> mapPredictionResult = mapVarPredictionResults.get(targetDate);
					if(nbOfObjects == mapPredictionResult.size()) {
						try {
							PredictionResult aggregatedPredictionResult = PredictionResult.auxAggregate(
									mapPredictionResult, varWeightTable, agentAuthentication, logger);
							result.setResult(variable, targetDate, aggregatedPredictionResult);
						} catch (HandlingException e) {
							logger.error(e);
						}
					}
				}
			}
			result.generateRandomState();
		}
		return result;
	}


	protected static Map<String, Map<String, Double>> evaluatePredictionsWeigts(
			Map<String, PredictionData> mapPredictionData, AgentAuthentication agentAuthentication,
			PredictionContext predictionContext, String operator, AbstractLogger logger) {
		String localAgent = agentAuthentication.getAgentName();
		PredictionData localPredictionData = mapPredictionData.get(localAgent);
		double maxTotalPower = predictionContext.getNodeContext().getMaxTotalPower();
		int statesNb = NodeStates.getNbOfStates();
		double distanceDivisor = maxTotalPower / statesNb;
		Map<String, Double> mapDistance = new HashMap<String, Double>();
		Map<String, Double> mapLocalInitValues = localPredictionData.getInitialValues();
		Map<String, Map<String, Double>> weightsByVarAndNode = new HashMap<String, Map<String, Double>>();
		for (String variable : predictionContext.getNodeContext().getVariables()) {
			weightsByVarAndNode.put(variable, new HashMap<String, Double>());
		}
		for (String agentName : mapPredictionData.keySet()) {
			PredictionData agentPredictionData = mapPredictionData.get(agentName);
			if (OP_DISTANCE_CURRENT_POWER.equals(operator)) {
				Map<String, Double> nodeInitialValues = agentPredictionData.getInitialValues();
				double nodeInitialValue = 0;
				for (String variable : predictionContext.getNodeContext().getVariables()) {
					if (nodeInitialValues != null && nodeInitialValues.containsKey(variable)) {
						nodeInitialValue = nodeInitialValues.get(variable);
					}
					// Evaluate the difference of initial values between the current node and the local node
					double localInitalvalue = mapLocalInitValues.get(variable);
					double distance = Math.abs(nodeInitialValue - localInitalvalue);
					double nodeWeight = Math.pow(10, -1 * distance / distanceDivisor);
					Map<String, Double> varWeightTable = weightsByVarAndNode.get(variable);
					varWeightTable.put(agentName, nodeWeight);
				}
			} else if(OP_DISTANCE_POWER_PROFILE.equals(operator)) {
				mapDistance = SapereUtil.computeProfileDistance(localPredictionData.getStateHistory(),
						agentPredictionData.getStateHistory(), logger);
				for (String variable : predictionContext.getNodeContext().getVariables()) {
					if (mapDistance.containsKey(variable)) {
						double distance = mapDistance.get(variable);
						double nodeWeight = Math.pow(10, -1 * distance / distanceDivisor);
						Map<String, Double> varWeightTable = weightsByVarAndNode.get(variable);
						varWeightTable.put(agentName, nodeWeight);
					}
				}
			} else {
				// Weight by default : set the same for all nodes
				for (String variable : predictionContext.getNodeContext().getVariables()) {
					Map<String, Double> varWeightTable = weightsByVarAndNode.get(variable);
					varWeightTable.put(agentName, 1.0);
				}
			}
		}
		// Normalize all weights (e.g. divdie each item by the sum)
		Map<String, Map<String, Double>> weightTable = new HashMap<String, Map<String, Double>>();
		for (String variable : predictionContext.getNodeContext().getVariables()) {
			Map<String, Double> varWeightTable = weightsByVarAndNode.get(variable);
			double weightSum = 0;
			for(double nextWeight : varWeightTable.values()) {
				weightSum+=nextWeight;
			}
			for(String node : varWeightTable.keySet()) {
				varWeightTable.put(node, varWeightTable.get(node)/weightSum);
			}
			weightTable.put(variable, varWeightTable);
		}
		return weightTable;
	}

	public Map<String, PredictionData> retrieveSourcePrecictions(AbstractLogger logger) {
		Map<String, PredictionData> result = new HashMap<String, PredictionData>();
		for(String agentName : mapSourceObjects.keySet()) {
			IAggregateable nextObj = mapSourceObjects.get(agentName);
			if(nextObj instanceof PredictionData) {
				PredictionData nextPredictionData = (PredictionData) nextObj;
				result.put(agentName, nextPredictionData);
			}
		}
		return result;
	}

	public boolean isComplete() {
		Map<String, PredictionResult> mapLastResults = getMapLastResults();
		if (mapLastResults.size() == 0) {
			return false;
		}
		for (String variable : mapLastResults.keySet()) {
			PredictionResult nextResult = mapLastResults.get(variable);
			if (!nextResult.isComplete()) {
				return false;
			}
		}
		return true;
	}

	public boolean hasPredictionAggregator() {
		LearningAggregationOperator aggregator = context.getAggregationOperator();
		return (aggregator != null && aggregator.isPredictionAggregation());
	}

	public boolean hasModelAggregator() {
		LearningAggregationOperator aggregator = context.getAggregationOperator();
		return (aggregator != null && aggregator.isModelAggregation());
	}

	public boolean isReadyForAggregation() {
		if(this.hasPredictionAggregator() && this.isComplete() && lastUpdate != null) {
			Date lastAggregation = this.getAggregationDate();
			//Date current = predictionContext.getCurrentDate();
			int waitingMinutesBetweenAggragations = this.context.getAggregationOperator().getWaitingMinutesBetweenAggragations();
			Date minAggregationDate = lastAggregation == null ? lastUpdate : UtilDates.shiftDateMinutes(lastAggregation, waitingMinutesBetweenAggragations);
			if(lastAggregation == null || lastUpdate.after(minAggregationDate)) {
				// send input for a new aggregation
				return true;
			}
		}
		return false;
	}

	public AbstractAggregationResult checkupAggregation(AggregationCheckupRequest fedAvgCheckupRequest
			,Map<String, PredictionData> receivedPredictions
			,AbstractLogger logger) {
		String variableName = fedAvgCheckupRequest.getVariableName();
		try {
			boolean isAggregated = (aggregationDate != null);
			PredictionAggregationResult result = new PredictionAggregationResult();
			logger.info("AbstractLearningModel.checkuAggregation aggregationWeights = " + aggregationWeights);
			result.setVariableName(variableName);
			result.setAggregationDate(aggregationDate);
			result.setAggragationOperator(context.getAggregationOperator());
			Map<String, String> mapNodes = new HashMap<String, String>();
			for (String nextAgent : receivedPredictions.keySet()) {
				PredictionData nextPredictionData = receivedPredictions.get(nextAgent);
				String modelNode = nextPredictionData.getContext().getNodeLocation().getName();
				mapNodes.put(nextAgent, modelNode);
			}
			result.setMapNodeByAgent(mapNodes);
			if (isAggregated && aggregationWeights.containsKey(variableName)) {
				result.setAggregationWeights(SapereUtil.cloneMapStringDouble(aggregationWeights.get(variableName)));
				Map<Date, PredictionResult> mapAggregatedResults = mapResults.get(variableName);
				for (Date nextDate : mapAggregatedResults.keySet()) {
					PredictionResult nextPredictionResult = mapAggregatedResults.get(nextDate);
					result.setAggredatedStateProbabilities(nextDate, nextPredictionResult.getStateProbabilities());
					for (String nextnode : receivedPredictions.keySet()) {
						PredictionData nodePredictionData = receivedPredictions.get(nextnode);
						Map<Date, PredictionResult> nodeMapResult = nodePredictionData.getMapResults()
								.get(variableName);
						Date closestDate = SapereUtil.getClosestDate(nextDate, nodeMapResult.keySet());
						if (closestDate != null) {
							long dateGapMinutes = Math.abs(closestDate.getTime() - nextDate.getTime()) / (1000 * 60);
							if (dateGapMinutes <= 3) {
								PredictionResult nodePredictionResult = nodeMapResult.get(closestDate);
								result.setNodeStateProbabilities(nextDate, nextnode,
										nodePredictionResult.getStateProbabilities());
							}
						}
					}
				}
			}
			return result;
		} catch (Throwable e) {
			logger.error(e);
		}
		logger.info("CompleteLSTMModelheckupFedAVG : end");
		return null;
	}

	@Override
	public PredictionData copyForLSA(AbstractLogger logger) {
		PredictionData result = new PredictionData();
		result.setLastUpdate(lastUpdate);
		result.setInitialDate(initialDate);
		result.setVariables(SapereUtil.cloneListString(variables));
		result.setTargetDates(SapereUtil.cloneSetDate(targetDates));
		result.setListSteps(new ArrayList<PredictionStep>());
		Map<String, VariableState> copyInitialStates = new HashMap<String, VariableState>();
		for (String variable : initialStates.keySet()) {
			VariableState vState = initialStates.get(variable);
			copyInitialStates.put(variable, vState.clone());
		}
		result.setInitialStates(copyInitialStates);
		Map<String, Double> copyInitialValues = SapereUtil.cloneMapStringDouble(initialValues);
		result.setInitialValues(copyInitialValues);
		Map<String, Map<Date, PredictionResult>> copyMapResult = new HashMap<String, Map<Date, PredictionResult>>();
		for (String variable : mapResults.keySet()) {
			Map<Date, PredictionResult> copyMapResult2 = new HashMap<Date, PredictionResult>();
			Map<Date, PredictionResult> mapResult2 = mapResults.get(variable);
			for (Date nextDate : mapResult2.keySet()) {
				PredictionResult nextResult = mapResult2.get(nextDate);
				copyMapResult2.put(nextDate, nextResult.copy(false));

			}
			copyMapResult.put(variable, copyMapResult2);
		}
		result.setMapResults(copyMapResult);
		result.setMapStatesStatistics(new HashMap<FeaturesKey, StatesStatistic>());
		result.setErrors(SapereUtil.cloneListString(errors));
		if (context != null) {
			result.setContext(context.copyContent(false));
		}
		result.setUseCorrections(useCorrections);
		List<SingleNodeStateItem> copyStateHistory = new ArrayList<SingleNodeStateItem>();
		for (SingleNodeStateItem stateItem : stateHistory) {
			copyStateHistory.add(stateItem.clone());
		}
		result.setStateHistory(copyStateHistory);
		if (aggregationWeights != null && aggregationWeights.size() > 0) {
			result.setAggregationWeights(SapereUtil.cloneMap2StringDouble(aggregationWeights));
		}
		return result;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<NodeLocation>();
		if (this.context != null) {
			result.add(context.getNodeLocation());
		}
		return result;
	}

}
