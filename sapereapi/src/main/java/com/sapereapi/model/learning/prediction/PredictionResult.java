package com.sapereapi.model.learning.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;

public class PredictionResult implements Serializable {
	private static final long serialVersionUID = 1755L;
	private Date initialDate;
	private Date targetDate;
	private String variable;
	private List<Double> stateProbabilities;
	private VariableState initialState;
	private VariableState randomTargetState;
	private VariableState actualTargetState;
	private Double likelyTargetValue = null;
	private Double actualTargetValue;
	private StatesStatistic actualStatesStatistic;
	private FeaturesKey targetFeaturesKey;
	private Long predictionId;
	private Long timeShiftMS;

	public PredictionResult(Date _initialDate, VariableState _initialState, Date _targetDate, String _variable, FeaturesKey _targetFeaturesKey, long _timeShiftMS) {
		super();
		this.initialDate = _initialDate;
		this.targetDate = _targetDate;
		this.variable = _variable;
		this.initialState = _initialState;
		this.targetFeaturesKey = _targetFeaturesKey;
		this.stateProbabilities = new ArrayList<Double>();
		this.timeShiftMS = _timeShiftMS;
		this.predictionId = null;
	}

	public PredictionResult copy(boolean copyIds) {
		PredictionResult result = new PredictionResult(initialDate
				, initialState == null ? null : initialState.clone()
				, targetDate
				, variable
				, targetFeaturesKey == null ? null : targetFeaturesKey.clone()
				, timeShiftMS);
		result.setStateProbabilities(SapereUtil.cloneListDouble(stateProbabilities));
		if(actualTargetState != null) {
			result.setActualTargetState(actualTargetState.clone());
		}
		if(randomTargetState != null) {
			result.setRandomTargetState(randomTargetState.clone());
		}
		result.setLikelyTargetValue(likelyTargetValue);
		result.setActualTargetValue(actualTargetValue);
		if(copyIds) {
			result.setPredictionId(predictionId);
		}
		return result;
	}

	public Long getPredictionId() {
		return predictionId;
	}

	public void setPredictionId(Long predictionId) {
		this.predictionId = predictionId;
	}

	public Date getInitialDate() {
		return initialDate;
	}

	public void setInitialDate(Date initialDate) {
		this.initialDate = initialDate;
	}

	public VariableState getInitialState() {
		return initialState;
	}

	public void setInitialState(VariableState initialState) {
		this.initialState = initialState;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public FeaturesKey getTargetFeaturesKey() {
		return targetFeaturesKey;
	}

	public void setTargetFeaturesKey(FeaturesKey targetFeaturesKey) {
		this.targetFeaturesKey = targetFeaturesKey;
	}

	public Long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public List<Double> getStateProbabilities() {
		return stateProbabilities;
	}

	public void setStateProbabilities(List<Double> stateProbabilities) {
		this.stateProbabilities = stateProbabilities;
	}

	public Double getLikelyTargetValue() {
		return likelyTargetValue;
	}

	public void setLikelyTargetValue(Double likelyTargetVAlue) {
		this.likelyTargetValue = likelyTargetVAlue;
	}

	public void setStateProbabilities(int stateIdx, Double proba) {
		if(stateProbabilities.size() < NodeStates.getNbOfStates()) {
			stateProbabilities = new ArrayList<>();
			for(int idx = 0; idx < NodeStates.getNbOfStates(); idx++) {
				stateProbabilities.add(0.0);
			}
		}
		this.stateProbabilities.set(stateIdx, proba);
	}

	public void setStateProbabilities(DoubleMatrix predictionRowMatrix) {
		stateProbabilities = new ArrayList<>();
		for (double value : predictionRowMatrix.getRowPackedCopy()) {
			stateProbabilities.add(value);
		}
	}

	public Date getTargetDate() {
		return targetDate;
	}

	public void setTargetDate(Date targetDate) {
		this.targetDate = targetDate;
	}

	public VariableState getRandomTargetState() {
		return randomTargetState;
	}

	public void setRandomTargetState(VariableState randomTargetState) {
		this.randomTargetState = randomTargetState;
	}

	public Double getActualTargetValue() {
		return actualTargetValue;
	}

	public void setActualTargetValue(Double actualTargetValue) {
		this.actualTargetValue = actualTargetValue;
	}

	public VariableState getActualTargetState() {
		return actualTargetState;
	}

	public boolean hasActualTargetState() {
		return (this.actualTargetState != null);
	}

	public void setActualTargetState(VariableState actualTargetState) {
		this.actualTargetState = actualTargetState;
	}

	public void generateRandomState() {
		randomTargetState = NodeStates.getRandomState(stateProbabilities);
	}

	public VariableState getMostLikelyState() {
		return NodeStates.getMostLikelyState(stateProbabilities);
	}

	public StatesStatistic getActualStatesStatistic() {
		return actualStatesStatistic;
	}

	public void setActualStatesStatistic(StatesStatistic _actualStatesStatistics) {
		this.actualStatesStatistic = _actualStatesStatistics;
	}

	public double getStateProbability(int stateIdx) {
		double stateProba = 0;
		if (stateIdx < stateProbabilities.size()) {
			stateProba = stateProbabilities.get(stateIdx);
		}
		return stateProba;
	}

	public double getTimeHorizonMinutes() {
		if (targetDate == null) {
			return 0;
		}
		return UtilDates.computeDurationMinutes(initialDate, targetDate);
	}

	public boolean isRandomOK() {
		if (actualTargetState != null) {
			return actualTargetState.getId().equals(this.randomTargetState.getId());
		}
		return false;
	}

	public boolean hasFailed() {
		if (actualTargetState != null) {
			VariableState mostLikelySate = getMostLikelyState();
			return mostLikelySate != null && !actualTargetState.getId().equals(mostLikelySate.getId());
		}
		return false;
	}

	public boolean isMostLikelyStateOK() {
		if (actualTargetState != null) {
			VariableState mostLikelySate = getMostLikelyState();
			return actualTargetState.getId().equals(mostLikelySate.getId());
		}
		return false;
	}

	public VariableFeaturesKey getTargetTransitionMatrixKey(PredictionContext context) {
		return context.getTransitionMatrixKey(targetFeaturesKey, variable);
	}

	public List<Double> getVectorDifferential() {
		List<Double> result = new ArrayList<Double>();
		if(actualStatesStatistic != null) {
			for(int stateIndex = 0; stateIndex < stateProbabilities.size(); stateIndex++) {
				VariableState state = NodeStates.getStatesList().get(stateIndex);
				double predictionProba = stateProbabilities.get(stateIndex);
				double actualStateRatio = this.actualStatesStatistic.getRatio(state.getName());
				double stateDifferential = (predictionProba - actualStateRatio);
				result.add(stateDifferential);
			}
		}
		return result;
	}

	public Double getDifferential() {
		List<Double> vectorDifferential = getVectorDifferential();
		if(vectorDifferential.size() == 0) {
			return null;
		}
		double result = 0;
		//if(vectorDifferential.size() > 0) {
			for(double nextDiff : vectorDifferential) {
				result+=Math.abs(nextDiff);
			}
		//}
		return result/2;
	}

	public Double getReliability() {
		Double differential = getDifferential();
		if(differential==null) {
			return null;
		}
		return 1 - differential;
	}

	public Map<VariableState, Double> getMapStateProba() {
		Map<VariableState, Double> simpleMap = new HashMap<VariableState, Double>();
		int stateIdx = 0;
		for (double nextProba : stateProbabilities) {
			VariableState nextState = NodeStates.getStatesList().get(stateIdx);
			simpleMap.put(nextState, nextProba);
			stateIdx++;
		}
		return simpleMap;
	}

	public Map<VariableState, Double> getSortedMapStateProba() {
		Map<VariableState, Double> simpleMap = getMapStateProba();
		ArrayList<Map.Entry<VariableState, Double>> arrayOfEntries = new ArrayList<Map.Entry<VariableState, Double>>();
		 for(Map.Entry<VariableState, Double> e: simpleMap.entrySet()) {
			arrayOfEntries.add(e);
		}
		Comparator<Map.Entry<VariableState, Double>> valueComparator = new Comparator<Map.Entry<VariableState, Double>>() {
            @Override
            public int compare(Map.Entry<VariableState, Double> e1, Map.Entry<VariableState, Double> e2) {
                Double v1 = e1.getValue();
                Double v2 = e2.getValue();
                return v2.compareTo(v1);
            }
		};
		Collections.sort(arrayOfEntries, valueComparator);
		Map<VariableState, Double> result = new LinkedHashMap<VariableState, Double>();
		for(Map.Entry<VariableState, Double> e: arrayOfEntries) {
			result.put(e.getKey(), e.getValue());
			//System.out.println(e.getValue());
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		StringBuffer sStateProbabilities = new StringBuffer();
		sStateProbabilities.append("[");
		for (double nextProba : stateProbabilities) {
			sStateProbabilities.append(UtilDates.df3.format(nextProba));
			sStateProbabilities.append(";");
		}
		sStateProbabilities.append("]");
		StringBuffer sStateProbabilities2 = new StringBuffer();
		sStateProbabilities2.append("[");
		Map<VariableState, Double> mapProba = getSortedMapStateProba();
		for(VariableState nextState : mapProba.keySet()) {
			Double nextProba = mapProba.get(nextState);
			double nextProba2 = SapereUtil.round(nextProba, 3);
			if(nextProba2>0) {
				sStateProbabilities2.append(nextState.getName()).append("=");
				sStateProbabilities2.append(UtilDates.df.format(100*nextProba2)).append("%");
				sStateProbabilities2.append("    ");
			}
		}
		sStateProbabilities2.append("]");
		double horizonMin = getTimeHorizonMinutes();
		long horizonMin2 = Math.round(horizonMin);
		result.append("PredictionResult [")
				.append("'").append(variable).append("' : ")
				.append("").append(UtilDates.formatTimeOrDate(initialDate, timeShiftMS)).append(" -> ")
				//.append("").append(UtilDates.formatTimeOrDate(targetDate, timeShiftMS))
				.append("[+").append(horizonMin2).append("min]")
				//.append(", stateProbabilities=").append(sStateProbabilities)
				.append(", stateProbabilities2=").append(sStateProbabilities2)
				//.append(", randomTargetState=").append(randomTargetState)
				.append("]");
		return result.toString();
	}


	public static List<Double> auxComputeAvgProbabilities(Map<String, List<Double>> mapProbaVectors,
			Map<String, Double> weightsTable) {
		if (mapProbaVectors.size() == 0) {
			return null;
		}
		List<Double> firstVector = mapProbaVectors.values().iterator().next();
		List<Double> sumVector = new ArrayList<Double>();
		for(Double nextValue : firstVector) {
			sumVector.add(0*nextValue);
		}
		double sumWeight = 0;
		for(double nextWeight : weightsTable.values()) {
			sumWeight+=nextWeight;
		}
		for (String node : mapProbaVectors.keySet()) {
			List<Double> nextProbaVector = mapProbaVectors.get(node);
			if(weightsTable.containsKey(node)) {
				double weight = weightsTable.get(node);
				int idx = 0;
				for (Double toAdd : nextProbaVector) {
					double toAdd2 = toAdd * weight;
					sumVector.set(idx, sumVector.get(idx) + toAdd2);
					idx++;
				}
			}
		}
		List<Double> avgVector = new ArrayList<Double>();
		// double len = listProbaVectors.size();
		for (Double nextItem : sumVector) {
			avgVector.add(nextItem / sumWeight);
		}
		return avgVector;
	}

	public static PredictionResult auxAggregate(Map<String, PredictionResult> mapPredictionResults,
			Map<String, Double> weightsTable, AgentAuthentication agentAuthentication, AbstractLogger logger) throws HandlingException {
		PredictionResult result = null;
		Map<String, List<Double>> mapStateProbabilities = new HashMap<String, List<Double>>();
		String localAgentName = agentAuthentication.getAgentName();
		if (mapPredictionResults.containsKey(localAgentName)) {
			PredictionResult localPredictionResult = mapPredictionResults.get(localAgentName);
			String key = localPredictionResult.getVariable() + "-" + UtilDates.format_date_time.format(localPredictionResult.getTargetDate());
			result = new PredictionResult(localPredictionResult.getInitialDate(),
					localPredictionResult.getInitialState(),
					localPredictionResult.getTargetDate(),
					localPredictionResult.getVariable(),
					localPredictionResult.getTargetFeaturesKey(),
					localPredictionResult.getTimeShiftMS());
			for (String agentName : mapPredictionResults.keySet()) {
				PredictionResult nextPred = mapPredictionResults.get(agentName);
				mapStateProbabilities.put(agentName, nextPred.getStateProbabilities());
			}
			List<Double> avgProbabilities = auxComputeAvgProbabilities(mapStateProbabilities, weightsTable);
			// Check the sum of all probabilities
			double matrixSum = 0.0;
			for(Double nextProb : avgProbabilities) {
				matrixSum+=nextProb;
			}
			if (Math.abs(matrixSum - 1.0) > 0.0001) {
				throw new HandlingException("auxAggregate : the resulting distribution is not 1 " + matrixSum
						+ " is not equals to 1 for key : " + key.toString());
			}
			result.setStateProbabilities(avgProbabilities);
		}
		return result;
	}

	public StatesStatistic computeStateStatistic(PredictionContext predictionContext, List<SingleNodeStateItem> stateHistory) {
		StatesStatistic statesStatistic = new StatesStatistic(predictionContext, variable, targetFeaturesKey, targetDate);
		for(SingleNodeStateItem nextStateItem : stateHistory) {
			FeaturesKey nextFeaturesKey = predictionContext.getFeaturesKey2(nextStateItem.getDate());
			if(targetFeaturesKey.equals(nextFeaturesKey)) {
				int stateId = nextStateItem.getStateId(variable);
				VariableState state = NodeStates.getById(stateId);
				String stateName = state.getName();
				statesStatistic.incrementStateNb(stateName);
			}
		}
		return statesStatistic;
	}

	public Double computeCrossEntroy(AbstractLogger logger) {
		if(actualStatesStatistic != null && stateProbabilities != null) {
			int nbOfStates = NodeStates.getNbOfStates();
			if(stateProbabilities.size() == nbOfStates) {
				List<Double> statesDistribution = actualStatesStatistic.getArrayStateDistribution();
				return SapereUtil.computeCrossEntroy(logger, statesDistribution, stateProbabilities);
			}
		}
		return null;
	}

	public boolean isComplete() {
		if (stateProbabilities.size() == 0 || targetDate == null) {
			return false;
		}
		double probabilitySum = 0;
		for (double nextProba : stateProbabilities) {
			probabilitySum += nextProba;
		}
		return (Math.abs(probabilitySum - 1.0) < 0.0001);
	}

}
