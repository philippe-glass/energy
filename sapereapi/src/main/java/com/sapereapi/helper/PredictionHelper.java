package com.sapereapi.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.TimeWindow;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionCorrection;
import com.sapereapi.model.learning.prediction.PredictionDeviation;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.model.learning.prediction.PredictionStatistic;
import com.sapereapi.model.learning.prediction.input.StatisticsRequest;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class PredictionHelper {
	public final static List<TimeWindow> ALL_TIME_WINDOWS = PredictionDbHelper.retrieveTimeWindows();
	public final static int LEARNING_TIME_STEP_MINUTES = 1;
	private static SapereLogger logger = SapereLogger.getInstance();
	private static Double maxTotalPower = NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	private static String variables[] = {};
	private static PredictionHelper instance = null;

	public static Double getMaxTotalPower() {
		return maxTotalPower;
	}

	public static PredictionHelper getInstance() {
		if (instance == null) {
			instance = new PredictionHelper();
		}
		return instance;
	}

	public static void setMaxTotalPower(Double maxTotalPower) {
		PredictionHelper.maxTotalPower = maxTotalPower;
	}

	public static String[] getVariables() {
		return variables;
	}

	public static void setVariables(String[] variables) {
		PredictionHelper.variables = variables;
	}

	public static int getVariableIndex(String variable) {
		int idx = 0;
		for(String nextVariable : variables) {
			if(nextVariable.equals(variable)) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	public static Map<VariableFeaturesKey, Double> computeStateEntropie(
				 PredictionContext predictionContext
				,Map<Date, NodeStatesTransitions> mapAllTransitions
				,String[] listVariables) {
		Map<VariableFeaturesKey,Map<String, Integer>> statesDistributionByTrMatrix = new HashMap<>();
		for (NodeStatesTransitions nextTransition : mapAllTransitions.values()) {
			for(String variable : listVariables) {
				String nextState = nextTransition.getState(variable).getName();
				Date stateDate = nextTransition.getStateDate();
				//FeaturesKey featuresKey = OLD_getFeaturesKey(stateDate);
				FeaturesKey featuresKey = predictionContext.getFeaturesKey2(stateDate);
				//FeaturesKey featuresKey = new FeaturesKey(timeWindow);
				VariableFeaturesKey trMatrixKey = predictionContext.getTransitionMatrixKey(featuresKey, variable);
				if(!statesDistributionByTrMatrix.containsKey(trMatrixKey)) {
					statesDistributionByTrMatrix.put(trMatrixKey, new HashMap<>());
				}
				Map<String, Integer> statesDistribution = statesDistributionByTrMatrix.get(trMatrixKey);
				if(!statesDistribution.containsKey(nextState)) {
					statesDistribution.put(nextState, 0);
				}
				int stateCardinality = statesDistribution.get(nextState);
				statesDistribution.put(nextState, 1+stateCardinality);
			}
		}
		Map<VariableFeaturesKey, Double> result = new HashMap<>();
		for(VariableFeaturesKey key : statesDistributionByTrMatrix.keySet()) {
			Map<String, Integer> statesDistribution = statesDistributionByTrMatrix.get(key);
			double entropie = SapereUtil.computeShannonEntropie(statesDistribution);
			result.put(key, entropie);
		}
		return result;
	}

	public static Map<String,PredictionStatistic> computePredictionStatistics(PredictionContext predictionContext
			,StatisticsRequest statisticsRequest) throws HandlingException {
		Map<String,PredictionStatistic> mapStatistics = PredictionDbHelper.computePredictionStatistics(
				predictionContext, statisticsRequest);
		return mapStatistics;
	}

	public static List<PredictionCorrection> applyPredictionSelfCorrection(
			PredictionContext predictionContext
			, TimeSlot targetTimeSLot) throws HandlingException {
		Date computeDay = UtilDates.removeTime(new Date());
		PredictionDbHelper.consolidatePredictions(predictionContext);
		List<PredictionResult> listPredictionResults = PredictionDbHelper.retrieveListPredictionReults(predictionContext, computeDay, targetTimeSLot);
		Map<VariableFeaturesKey, Map<VariableState, PredictionCorrection>> mapCorrections =
				auxGenerateCorrections(predictionContext, listPredictionResults, "periodicSelfCorrection");
		List<PredictionCorrection> result = new ArrayList<>();
		for( Map<VariableState, PredictionCorrection> mapCorrection1 : mapCorrections.values()) {
			for(PredictionCorrection correction : mapCorrection1.values()) {
				result.add(correction);
			}
		}
		return result;
	}

	private static List<Double> getAverageDifferential(List<PredictionResult> listPredictionResult) {
		Map<Integer, List<Double>> mapProba = new HashMap<Integer, List<Double>>();
		for(int stateIdx = 0; stateIdx < NodeStates.getNbOfStates(); stateIdx++) {
			mapProba.put(stateIdx, new ArrayList<Double>());
		}
		for(PredictionResult nextResult : listPredictionResult) {
			List<Double> vectorDifferential = nextResult.getVectorDifferential();
			if(vectorDifferential != null && vectorDifferential.size() >= mapProba.size()) {
				for(Integer stateIdx : mapProba.keySet()) {
					Double nextDiff = vectorDifferential.get(stateIdx);
					(mapProba.get(stateIdx)).add(nextDiff);
					//Double nextStateProba = nextResult.getStateProbability(stateIdx);
					//(mapProba.get(stateIdx)).add(nextStateProba);
				}
			}
		}
		List<Double> result = new ArrayList<Double>();
		for(int stateIdx = 0; stateIdx < NodeStates.getNbOfStates(); stateIdx++) {
			List<Double> listProba = mapProba.get(stateIdx);
		    Double average = listProba.stream().mapToDouble(val -> val).average().orElse(Double.NaN);
		    if(average.isNaN()) {
			    return null;
		    }
			result.add(average);
		}
		return result;
	}

	private static PredictionDeviation auxGenerateDeviation(
			VariableFeaturesKey trKey,
			VariableState initialState,
			List<PredictionResult> listResults) {
		List<Double> vectDifferential = getAverageDifferential(listResults);
		if(vectDifferential != null && vectDifferential.size() >= NodeStates.getNbOfStates()) {
			 VariableState stateOver = null;
			 VariableState stateUnder = null;
			 double maxDiffItem = 0;
			 double minDiffItem = 0;
			 double excess = 0;
			 for(int stateIdx = 0; stateIdx < NodeStates.getNbOfStates(); stateIdx++) {
				 double diffItem = vectDifferential.get(stateIdx);
				 if(diffItem > 0 && diffItem > maxDiffItem) {
					 maxDiffItem = diffItem;
					 stateOver = NodeStates.getStatesList().get(stateIdx);
				 } else if(diffItem < 0 && diffItem <  minDiffItem) {
					 minDiffItem = diffItem;
					 stateUnder = NodeStates.getStatesList().get(stateIdx);
				 }
			 }
			 excess = Math.min(Math.abs(minDiffItem), Math.abs(maxDiffItem));
			 if(stateOver != null && stateUnder != null) {
				 PredictionDeviation deviation2 = new PredictionDeviation(trKey, initialState, stateUnder, stateOver, excess);
				 for(PredictionResult nextResult : listResults) {
					 deviation2.addPrediction(nextResult);
				 }
				 return deviation2;
			 }
		}
		return null;
	}

	private static Map<VariableFeaturesKey,Map<VariableState, List<PredictionResult>>> auxGenerateMapResult(
			PredictionContext context,
			List<PredictionResult> listResult) {
		Map<VariableFeaturesKey,Map<VariableState, List<PredictionResult>>> mapResult = new HashMap<VariableFeaturesKey,Map<VariableState, List<PredictionResult>>>();
		for(PredictionResult predictionResult : listResult) {
			VariableFeaturesKey transitionMatrixKey = predictionResult.getTargetTransitionMatrixKey(context);
			if(!mapResult.containsKey(transitionMatrixKey)) {
				mapResult.put(transitionMatrixKey, new HashMap<VariableState, List<PredictionResult>>());
			}
			Map<VariableState, List<PredictionResult>> map2 = mapResult.get(transitionMatrixKey);
			VariableState initialState = predictionResult.getInitialState();
			if(!map2.containsKey(initialState)) {
				map2.put(initialState, new ArrayList<>());
			}
			List<PredictionResult> listResult2 = map2.get(initialState);
			listResult2.add(predictionResult);
		}
		return mapResult;
	}

	public static Map<VariableFeaturesKey, Map<VariableState, PredictionCorrection>> auxGenerateCorrections (
			PredictionContext predictionContext,
			List<PredictionResult> listResult,
			String tag) throws HandlingException {
		Map<VariableFeaturesKey,Map<VariableState, List<PredictionResult>>> mapResult2 = auxGenerateMapResult(predictionContext, listResult);
		Map<VariableFeaturesKey, Map<VariableState, PredictionCorrection>> result
			= new HashMap<VariableFeaturesKey, Map<VariableState, PredictionCorrection>>();
		for(VariableFeaturesKey trKey : mapResult2.keySet()) {
			Map<VariableState, List<PredictionResult>> map2 = mapResult2.get(trKey);
			for(VariableState initialState : map2.keySet()) {
				List<PredictionResult> listResults = map2.get(initialState);
				PredictionDeviation deviation2 = auxGenerateDeviation(trKey, initialState, listResults);
				if(deviation2 != null) {
					 Integer addedCorrection = PredictionDbHelper.addPredictionCorrection(predictionContext, deviation2, "self_correction");
					 if(addedCorrection != null) {
						PredictionCorrection correction = new PredictionCorrection(deviation2, addedCorrection);
						if(!result.containsKey(trKey)) {
							 result.put(trKey, new HashMap<VariableState, PredictionCorrection>());
						 }
						 Map<VariableState, PredictionCorrection> result1 = result.get(trKey);
						 result1.put(initialState, correction);
					 }
				 }
			}
		}
		return result;
	}


}
