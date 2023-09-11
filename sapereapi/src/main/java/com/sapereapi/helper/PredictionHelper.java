package com.sapereapi.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.exception.IncompleteMatrixException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.NodeMarkovTransitions;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.markov.TransitionMatrix;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.prediction.MultiPredictionsData;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.prediction.PredictionCorrection;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionDeviation;
import com.sapereapi.model.prediction.PredictionResult;
import com.sapereapi.model.prediction.PredictionStatistic;
import com.sapereapi.model.prediction.PredictionStep;
import com.sapereapi.model.prediction.StatesStatistic;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;
import eu.sapere.middleware.node.NodeManager;

public class PredictionHelper {
	public final static List<MarkovTimeWindow> ALL_TIME_WINDOWS = PredictionDbHelper.retrieveTimeWindows();
	public final static int LEARNING_TIME_STEP_MINUTES = 1;
	private static SapereLogger logger = SapereLogger.getInstance();
	private static Double maxTotalPower = NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER;
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


	public static PredictionStep getTimeSlot(Date aDate) throws Exception {
		MarkovTimeWindow markovTimeWindow = getMarkovTimeWindow(aDate);
		if(markovTimeWindow!=null) {
			Date startDate = aDate;
			Date markovWindowEndDate = markovTimeWindow.getEndDate(aDate);
			if(startDate.after(markovWindowEndDate)) {
				throw new Exception("PredictionTimeSlot : startDate " + UtilDates.format_date_time.format(startDate) + " is after end date " +  UtilDates.format_date_time.format(markovWindowEndDate));
			}
			Date endDate = UtilDates.shiftDateMinutes(aDate, LEARNING_TIME_STEP_MINUTES);
			if(endDate.after(markovWindowEndDate)) {
				endDate = markovWindowEndDate;
			}
			return new PredictionStep(markovTimeWindow, startDate, endDate);
		}
		return null;
	}

	public static MarkovTimeWindow getMarkovTimeWindow(Date aDate) {
		for (MarkovTimeWindow timeWindow : ALL_TIME_WINDOWS) {
			if (timeWindow.containsDate(aDate)) {
				return timeWindow;
			}
		}
		return null;
	}

	public static PredictionStep generateStep(Date currentDate, Date targetDate) throws Exception {
		MarkovTimeWindow markovTimeWindow = getMarkovTimeWindow(currentDate);
		if (markovTimeWindow != null) {
			Date startDate = currentDate;
			Date markovWindowEndDate = markovTimeWindow.getEndDate(currentDate);
			if (startDate.after(markovWindowEndDate)) {
				throw new Exception("PredictionTimeSlot : startDate " + UtilDates.format_date_time.format(startDate)
						+ " is after end date " + UtilDates.format_date_time.format(markovWindowEndDate));
			}
			Date endDate = UtilDates.shiftDateMinutes(currentDate, LEARNING_TIME_STEP_MINUTES);
			if (endDate.after(markovWindowEndDate)) {
				endDate = markovWindowEndDate;
			}
			if(endDate.after(targetDate)) {
				endDate = targetDate;
			}
			return new PredictionStep(markovTimeWindow, startDate, endDate);
		}
		return null;
	}

	public List<PredictionStep> computePredictionSteps(Date initialDate, Date targetDate) throws Exception {
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
				throw new Exception("Time window not found for date " + nextBeginDate);
			}
			result.add(nextTimeSlot);
			endDate = nextTimeSlot.getEndDate();
		}
		return result;
	}

	private static TransitionMatrix getTransitionMatrix(Map<Integer, NodeTransitionMatrices> mapTransitionMatrices,
			int timeWindowId, String variable) {
		NodeTransitionMatrices nextTransitionMatrices = mapTransitionMatrices.get(timeWindowId);
		if (nextTransitionMatrices != null) {
			return nextTransitionMatrices.getTransitionMatrix(variable);
		}
		return null;
	}

	private static double getSum(Matrix aMatrix) {
		double result = 0;
		for (double d : aMatrix.getRowPackedCopy()) {
			result += d;
		}
		return result;
	}

	public PredictionData computeVariablePrediction(Map<Integer, NodeTransitionMatrices> mapTransitionMatrices
			,NodeMarkovTransitions aNodeMarkovTransitions
			,PredictionData prediction
			,String variable) throws Exception {
		if (aNodeMarkovTransitions == null) {
			throw new Exception("No current state");
		}
		MarkovState initalState = aNodeMarkovTransitions.getState(variable);
		if (initalState == null) {
			throw new Exception("No current state for " + variable + " variable");
		}
		int stateIdx = NodeMarkovStates.getStateIndex(initalState);
		int nbStates = NodeMarkovStates.getNbOfStates();
		Matrix predictionRow = new Matrix(1, nbStates);
		predictionRow.set(0, stateIdx, 1);
		long timeShiftMS = prediction.getContext().getTimeShiftMS();
		double checkSum = getSum(predictionRow);
		// logger.info("prediction testSum=" + checkSum);
		for (PredictionStep nextStep : prediction.getListSteps()) {
			try {
				TransitionMatrix nextTransitionMatrix = getTransitionMatrix(mapTransitionMatrices,
						nextStep.getMarovTimeWindowId(), variable);
				if (nextTransitionMatrix == null) {
					throw new Exception("No transition matrix found for  " + variable + " at " + nextStep);
				} else {
					nextStep.setUsedTransitionMatrixId(variable, nextTransitionMatrix.getKey().getId());
					nextTransitionMatrix.checkCompletion(predictionRow);
					if(nextStep.getDurationMinutes() >= 0.5) {
						boolean useCorrection = prediction.isUseCorrections();
						predictionRow = predictionRow.times(nextTransitionMatrix.getNormalizedMatrix(useCorrection));
						checkSum = getSum(predictionRow);
						if (Math.abs(checkSum - 1.0) > 0.0001) {
							throw new Exception("Prediction sum should be equals to 1 for variable " + variable
									+ " , timewindow " + nextStep + "[sum:" + checkSum + "]");
						}
					} else {
						//logger.info("ignored prediction step : " + nextStep);
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
							nextTargetDate, variable, nextStep.getMarkovTimeWindow(), timeShiftMS);
					result.setStateProbabilities(predictionRow);
					prediction.setResult(variable, nextTargetDate, result);
				}
			}
		} // end loop on prediction steps
		Date lastTargetDate = prediction.getLastTargetDate();
		if (!prediction.hasResult(variable, lastTargetDate)) {
			PredictionResult result = new PredictionResult(prediction.getInitialDate(), initalState, lastTargetDate,
					variable, prediction.getLastStep().getMarkovTimeWindow(), timeShiftMS);
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

	public PredictionData computePrediction(PredictionContext predictionContext, Date initDate, List<Date> targetDates,
			String[] variables, NodeMarkovTransitions initialTransitions,
			Map<String, NodeMarkovTransitions> mapNeighborhoodMarkovTransitions,
			boolean useCorrections
			) {
		// learningWindow = 15;
		boolean isLocal = (predictionContext != null) && predictionContext.isLocal();
		PredictionData prediction = new PredictionData(predictionContext, useCorrections);
		prediction.setVariables(variables);
		prediction.setInitialDate(initDate);
		for (Date targetDate : targetDates) {
			prediction.addTargetDate(targetDate);
		}
		try {
			Date lastTargetDate = prediction.getLastTargetDate();
			List<PredictionStep> listSteps = computePredictionSteps(prediction.getInitialDate(), lastTargetDate);
			prediction.setListSteps(listSteps);
			String location = predictionContext.getMainServiceAddress();
			if (isLocal) {
				prediction.setInitialContent(initialTransitions);
			} else {
				if (mapNeighborhoodMarkovTransitions.containsKey(location)) {
					NodeMarkovTransitions neighborMarkovTransitions = mapNeighborhoodMarkovTransitions.get(location);
					prediction.setInitialContent(neighborMarkovTransitions);
				} else {
					prediction.addError("No initial state found at location : " + location);
				}
			}
			if (prediction.hasInitialContent()) {
				List<MarkovTimeWindow> listTimeWindows = new ArrayList<MarkovTimeWindow>();
				for (PredictionStep timeSlot : listSteps) {
					MarkovTimeWindow timeWindow = timeSlot.getMarkovTimeWindow();
					if (!listTimeWindows.contains(timeWindow)) {
						listTimeWindows.add(timeWindow);
					}
				}
				// Retrieve needed transtition matrices
				Map<Integer, NodeTransitionMatrices> mapTransitionMatrices = PredictionDbHelper
						.loadListNodeTransitionMatrice(predictionContext, variables, listTimeWindows, initDate);
				for (String variable : variables) {
					prediction = this.computeVariablePrediction(mapTransitionMatrices, initialTransitions, prediction,
							variable);
				}
				// Generate random prediction
				prediction.generateRandomState();
			}
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
			prediction.addError(e.getMessage());
		}
		return prediction;
	}

	public static PredictionContext generateNeightborContext(PredictionContext localContext) {
		PredictionContext neighborContext = localContext.clone();
		Map<Integer, Map<String, TransitionMatrixKey>> mapTransitionMatrixKeys = EnergyDbHelper.loadMapNodeTransitionMatrixKeys(neighborContext);
		neighborContext.setMapTransitionMatrixKeys(mapTransitionMatrixKeys);
		return neighborContext;
	}

	public PredictionData computePrediction2(PredictionContext predictionContext, Date initDate, List<Date> targetDates,
			String[] variables, boolean useCorrections) {
		NodeMarkovTransitions initialTransitions = PredictionDbHelper.loadClosestMarkovTransition(predictionContext,
				initDate, variables, maxTotalPower);
		if(initialTransitions == null) {
			PredictionData emptyPrediction = new PredictionData(predictionContext, useCorrections);
			emptyPrediction.addError("No state arround " + UtilDates.format_sql.format(initDate)
			 + " for the scenario " + predictionContext.getScenario() );
			return emptyPrediction;
		}
		Map<String, NodeMarkovTransitions> mapNeighborhoodMarkovTransitions = new HashMap<String, NodeMarkovTransitions>();
		for (String neighborLocation : NodeManager.instance().getNetworkDeliveryManager().getNeighbours()) {
			PredictionContext neighborContext = generateNeightborContext(predictionContext);
			NodeMarkovTransitions neighborInitialTransitions = PredictionDbHelper
					.loadClosestMarkovTransition(neighborContext, initDate, variables, maxTotalPower);
			if (neighborInitialTransitions != null) {
				mapNeighborhoodMarkovTransitions.put(neighborLocation, neighborInitialTransitions);
			}
		}
		PredictionData result = computePrediction(predictionContext, initDate, targetDates, variables,
				initialTransitions, mapNeighborhoodMarkovTransitions, useCorrections);
		for (Date targetDate : targetDates) {
			NodeMarkovTransitions finalTransitions = PredictionDbHelper.loadClosestMarkovTransition(predictionContext,
					targetDate, variables, maxTotalPower);
			if (finalTransitions != null) {
				//result.setActualTargetState(finalTransitions);
				for (String variable : variables) {
					MarkovState actualTargetState = finalTransitions.getCurrentState(variable);
					Double actualTargetValue = finalTransitions.getMapValues().get(variable);
					result.setActualTargetState(variable, targetDate, actualTargetState, actualTargetValue);
				}
			}
		}
		return result;
	}

	public Map<TransitionMatrixKey, Double> computeStateEntropie(
				PredictionContext predictionContext
				, Map<Date, NodeMarkovTransitions> mapAllTransitions
				, String variable) {
		Map<TransitionMatrixKey,Map<String, Integer>> statesDistributionByTrMatrix = new HashMap<>();
		for (NodeMarkovTransitions nextTransition : mapAllTransitions.values()) {
			String nextState = nextTransition.getState(variable).getName();
			Date stateDate = nextTransition.getStateDate();
			MarkovTimeWindow timeWindow = getMarkovTimeWindow(stateDate);
			TransitionMatrixKey trMatrixKey = predictionContext.getTransitionMatrixKey(timeWindow, variable);
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
		Map<TransitionMatrixKey, Double> result = new HashMap<>();
		for(TransitionMatrixKey key : statesDistributionByTrMatrix.keySet()) {
			Map<String, Integer> statesDistribution = statesDistributionByTrMatrix.get(key);
			double entropie = SapereUtil.computeShannonEntropie(statesDistribution);
			result.put(key, entropie);
		}
		return result;
	}

	public Map<String,PredictionStatistic> computePredictionStatistics(PredictionContext predictionContext
			,Date minCreationDate
			,Date maxCreationDate
			,Date minTargetDate
			,Date maxTargetDate
			,Integer minHour
			,Integer maxHour
			,Boolean useCorrectionFilter
			,String variableName
			,String[] fieldsToMerge
			) {
		Map<String,PredictionStatistic> mapStatistics = PredictionDbHelper.computePredictionStatistics(
				predictionContext, minCreationDate, maxCreationDate, minTargetDate, maxTargetDate, minHour, maxHour, useCorrectionFilter, variableName, fieldsToMerge);
		return mapStatistics;
	}

	public static List<PredictionCorrection> applyPredictionSelfCorrection(
			PredictionContext predictionContext
			, TimeSlot targetTimeSLot) {
		Date computeDay = UtilDates.removeTime(new Date());
		PredictionDbHelper.consolidatePredictions(predictionContext);
		List<PredictionResult> listPredictionResults = PredictionDbHelper.retrieveListPredictionReults(predictionContext, computeDay, targetTimeSLot);
		Map<TransitionMatrixKey, Map<MarkovState, PredictionCorrection>> mapCorrections =
				auxGenerateCorrections(predictionContext, listPredictionResults, "periodicSelfCorrection");
		List<PredictionCorrection> result = new ArrayList<>();
		for( Map<MarkovState, PredictionCorrection> mapCorrection1 : mapCorrections.values()) {
			for(PredictionCorrection correction : mapCorrection1.values()) {
				result.add(correction);
			}
		}
		return result;
	}

	private static List<Double> getAverageDifferential(List<PredictionResult> listPredictionResult) {
		Map<Integer, List<Double>> mapProba = new HashMap<Integer, List<Double>>();
		for(int stateIdx = 0; stateIdx < NodeMarkovStates.getNbOfStates(); stateIdx++) {
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
		for(int stateIdx = 0; stateIdx < NodeMarkovStates.getNbOfStates(); stateIdx++) {
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
			TransitionMatrixKey trKey,
			MarkovState initialState,
			List<PredictionResult> listResults) {
		List<Double> vectDifferential = getAverageDifferential(listResults);
		if(vectDifferential != null && vectDifferential.size() >= NodeMarkovStates.getNbOfStates()) {
			 MarkovState stateOver = null;
			 MarkovState stateUnder = null;
			 double maxDiffItem = 0;
			 double minDiffItem = 0;
			 double excess = 0;
			 for(int stateIdx = 0; stateIdx < NodeMarkovStates.getNbOfStates(); stateIdx++) {
				 double diffItem = vectDifferential.get(stateIdx);
				 if(diffItem > 0 && diffItem > maxDiffItem) {
					 maxDiffItem = diffItem;
					 stateOver = NodeMarkovStates.getStatesList().get(stateIdx);
				 } else if(diffItem < 0 && diffItem <  minDiffItem) {
					 minDiffItem = diffItem;
					 stateUnder = NodeMarkovStates.getStatesList().get(stateIdx);
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

	private static Map<TransitionMatrixKey,Map<MarkovState, List<PredictionResult>>> auxGenerateMapResult(
			PredictionContext context,
			List<PredictionResult> listResult) {
		Map<TransitionMatrixKey,Map<MarkovState, List<PredictionResult>>> mapResult = new HashMap<TransitionMatrixKey,Map<MarkovState, List<PredictionResult>>>();
		for(PredictionResult predictionResult : listResult) {
			TransitionMatrixKey transitionMatrixKey = predictionResult.getTargetTransitionMatrixKey(context);
			if(!mapResult.containsKey(transitionMatrixKey)) {
				mapResult.put(transitionMatrixKey, new HashMap<MarkovState, List<PredictionResult>>());
			}
			Map<MarkovState, List<PredictionResult>> map2 = mapResult.get(transitionMatrixKey);
			MarkovState initialState = predictionResult.getInitialState();
			if(!map2.containsKey(initialState)) {
				map2.put(initialState, new ArrayList<>());
			}
			List<PredictionResult> listResult2 = map2.get(initialState);
			listResult2.add(predictionResult);
		}
		return mapResult;
	}

	private static Map<TransitionMatrixKey, Map<MarkovState, PredictionCorrection>> auxGenerateCorrections(
			PredictionContext predictionContext,
			List<PredictionResult> listResult,
			String tag) {
		Map<TransitionMatrixKey,Map<MarkovState, List<PredictionResult>>> mapResult2 = auxGenerateMapResult(predictionContext, listResult);
		Map<TransitionMatrixKey, Map<MarkovState, PredictionCorrection>> result
			= new HashMap<TransitionMatrixKey, Map<MarkovState, PredictionCorrection>>();
		for(TransitionMatrixKey trKey : mapResult2.keySet()) {
			Map<MarkovState, List<PredictionResult>> map2 = mapResult2.get(trKey);
			for(MarkovState initialState : map2.keySet()) {
				List<PredictionResult> listResults = map2.get(initialState);
				PredictionDeviation deviation2 = auxGenerateDeviation(trKey, initialState, listResults);
				if(deviation2 != null) {
					 Integer addedCorrection = PredictionDbHelper.addPredictionCorrection(predictionContext, deviation2, "self_correction");
					 if(addedCorrection != null) {
						PredictionCorrection correction = new PredictionCorrection(deviation2, addedCorrection);
						if(!result.containsKey(trKey)) {
							 result.put(trKey, new HashMap<MarkovState, PredictionCorrection>());
						 }
						 Map<MarkovState, PredictionCorrection> result1 = result.get(trKey);
						 result1.put(initialState, correction);
					 }
				 }
			}
		}
		return result;
	}

	public MultiPredictionsData generateMassivePredictions(
			 PredictionContext predictionContext
			,TimeSlot targetDateSlot
			,int horizonMinutes
			,String variableName
			,boolean useCorrections
			,boolean generateCorrections) {
		// Just for test
		boolean evaluateGeneralEntropie = false;
		if(evaluateGeneralEntropie) {
			PredictionDbHelper.evaluateAllStatesEntropie();
		}
		//Date computeDay = UtilDates.removeTime(new Date());
		List<PredictionData> listPredictions = new ArrayList<>();
		//boolean useCorrections = true;
		Date firstInitDate = UtilDates.shiftDateMinutes(targetDateSlot.getBeginDate(), -1 * horizonMinutes);
		//MarkovTimeWindow firstTW = getMarkovTimeWindow(firstInitDate);
		//MarkovTimeWindow lastTW = getMarkovTimeWindow(targetDateSlot.getEndDate());
		String[] variables = new String[] { variableName };
		MultiPredictionsData result = new MultiPredictionsData(predictionContext, targetDateSlot, variables);
		// Load existing actual states for this period
		Map<Date, NodeMarkovTransitions> mapAllTransition = PredictionDbHelper.loadMapMarkovTransition(predictionContext,
				new TimeSlot(UtilDates.removeMinutes(firstInitDate), UtilDates.getCeilHourStart(targetDateSlot.getEndDate()))
				, variables, maxTotalPower);
		// Load existing actual states statistics
		//Date minTargetDate = targetDateSlot.getBeginDate();
		//Date maxTargetDate = targetDateSlot.getEndDate();
		int minHour = UtilDates.getHourOfDay(firstInitDate);
		int maxHour = UtilDates.getHourOfDay(targetDateSlot.getEndDate());
		Map<String, StatesStatistic> mapStatesStatistics = PredictionDbHelper.retrieveStatesStatistics(predictionContext
				, null // computeDay //
				, null // computeDay //
				, UtilDates.removeTime(firstInitDate)
				, UtilDates.removeTime(targetDateSlot.getEndDate())
				, minHour, maxHour, variableName);
		// idem for neighbor nodes
		Map<String, Map<Date, NodeMarkovTransitions>> mapNeighborhoodAllTransitions = new HashMap<>();
		for (String neighborLocation : NodeManager.instance().getNetworkDeliveryManager().getNeighbours()) {
			PredictionContext neighborContext = generateNeightborContext(predictionContext);
			Map<Date, NodeMarkovTransitions> mapTransitionNeightbour = PredictionDbHelper.loadMapMarkovTransition(
					neighborContext, new TimeSlot(firstInitDate, targetDateSlot.getEndDate()), variables,
					maxTotalPower);

			if (mapTransitionNeightbour.size() > 0) {
				mapNeighborhoodAllTransitions.put(neighborLocation, mapTransitionNeightbour);
			}
		}
		if("".equals(variableName)) {
			result.addError("The variable name has not been entered");
		} else if(mapAllTransition.size()==0) {
			result.addError("No state found during the requested time slot for the scenario " + predictionContext.getScenario());
		}
		List<Date> listStateDates = new ArrayList<>();
		for (Date aDate : mapAllTransition.keySet()) {
			listStateDates.add(aDate);
		}
		List<PredictionResult> listResult = new ArrayList<>();
		Collections.sort(listStateDates);
		for (Date nextInitDate : listStateDates) {
			Date nextTargetDate = UtilDates.shiftDateMinutes(nextInitDate, 1 * horizonMinutes);
			if (!nextTargetDate.before(targetDateSlot.getBeginDate()) && nextTargetDate.before(targetDateSlot.getEndDate())) {
				List<Date> targetDates = new ArrayList<Date>();
				targetDates.add(nextTargetDate);
				NodeMarkovTransitions initialTransitions = mapAllTransition.get(nextInitDate);
				Map<String, NodeMarkovTransitions> mapNeighborhoodInitTransitions = new HashMap<>();
				for (String neighborLocation : mapNeighborhoodAllTransitions.keySet()) {
					Map<Date, NodeMarkovTransitions> nextMap = mapNeighborhoodAllTransitions.get(neighborLocation);
					if (nextMap.containsKey(nextInitDate)) {
						mapNeighborhoodInitTransitions.put(neighborLocation, nextMap.get(nextInitDate));
					}
				}
				PredictionData predictionData = computePrediction(predictionContext, nextInitDate, targetDates,
						variables, initialTransitions, mapNeighborhoodInitTransitions, useCorrections);
				// search closest result to nextTargetDate
				Date closestDate = SapereUtil.getClosestDate(nextTargetDate, listStateDates);
				//String statesStatisticsKey  = StatesStatistic.generateKey(computeDay, closestDate, UtilDates.getHourOfDay(closestDate), variableName);
				String statesStatisticsKey  = StatesStatistic.generateKey(closestDate, UtilDates.getHourOfDay(closestDate), variableName);
				if(mapStatesStatistics.containsKey(statesStatisticsKey)) {
					StatesStatistic statesStatistic = mapStatesStatistics.get(statesStatisticsKey);
					predictionData.setActualStatesStatistics(variableName, nextTargetDate, statesStatistic);
				}
				NodeMarkovTransitions targetTransition = mapAllTransition.get(closestDate);
				//predictionData.setActualTargetState(targetTransition);
				MarkovState actualTargetState = targetTransition.getCurrentState(variableName);
				Double actualTargetValue = targetTransition.getMapValues().get(variableName);
				predictionData.setActualTargetState(variableName, nextTargetDate, actualTargetState, actualTargetValue);
				PredictionResult predictionResult = predictionData.getLastResult(variableName);
				//TransitionMatrixKey transitionMatrixKey = predictionData.getLastStep().getUsedTransitionMatrixKey(variableName, predictionContext);
				listResult.add(predictionResult);
				//MarkovState initialState = predictionResult.getInitialState();
				listPredictions.add(predictionData);
			}
		}
		// TODO : call self-correction
		if(useCorrections && generateCorrections) {
			Map<TransitionMatrixKey, Map<MarkovState, PredictionCorrection>> mapCorrections =
					auxGenerateCorrections(predictionContext, listResult, "generateMassivePredictions");
			for(Map<MarkovState, PredictionCorrection> mapCorrections1 : mapCorrections.values()) {
				for(PredictionCorrection correction : mapCorrections1.values()) {
					result.addCorrection(correction);
				}
			}
		}
		for(PredictionData nextPrediction : listPredictions) {
			result.addPrediction(nextPrediction);
		}
		// compute state entropie
		Map<TransitionMatrixKey, Double> entropieByTrMatrix = computeStateEntropie(predictionContext, mapAllTransition, variableName);
		for(TransitionMatrixKey nextKey : entropieByTrMatrix.keySet()) {
			result.addEntropieResult(nextKey, entropieByTrMatrix.get(nextKey));
		}
		return result;
	}
}
