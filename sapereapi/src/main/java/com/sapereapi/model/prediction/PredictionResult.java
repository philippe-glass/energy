package com.sapereapi.model.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;

public class PredictionResult implements Serializable {
	private static final long serialVersionUID = 1755L;
	private Date initialDate;
	private Date targetDate;
	private String variable;
	private List<Double> stateProbabilities;
	private MarkovState initialState;
	private MarkovState radomTargetState;
	private MarkovState actualTargetState;
	private Double actualTargetValue;
	private StatesStatistic actualStatesStatistic;
	private MarkovTimeWindow targetTimeWindow;
	private Long predictionId;

	public PredictionResult(Date _initialDate, MarkovState _initialState, Date _targetDate, String _variable, MarkovTimeWindow _targetTimeWindow) {
		super();
		this.initialDate = _initialDate;
		this.targetDate = _targetDate;
		this.variable = _variable;
		this.initialState = _initialState;
		this.targetTimeWindow = _targetTimeWindow;
		this.stateProbabilities = new ArrayList<Double>();
		this.predictionId = null;
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

	public MarkovState getInitialState() {
		return initialState;
	}

	public void setInitialState(MarkovState initialState) {
		this.initialState = initialState;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public MarkovTimeWindow getTargetTimeWindow() {
		return targetTimeWindow;
	}

	public void setTargetTimeWindow(MarkovTimeWindow targetTimeWindow) {
		this.targetTimeWindow = targetTimeWindow;
	}

	public List<Double> getStateProbabilities() {
		return stateProbabilities;
	}

	public void setStateProbabilities(List<Double> stateProbabilities) {
		this.stateProbabilities = stateProbabilities;
	}

	public void setStateProbabilities(int stateIdx, Double proba) {
		if(stateProbabilities.size() < NodeMarkovStates.getNbOfStates()) {
			stateProbabilities = new ArrayList<>();
			for(int idx = 0; idx < NodeMarkovStates.getNbOfStates(); idx++) {
				stateProbabilities.add(0.0);
			}
		}
		this.stateProbabilities.set(stateIdx, proba);
	}

	public void setStateProbabilities(Matrix predictionRowMatrix) {
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

	public MarkovState getRadomTargetState() {
		return radomTargetState;
	}

	public void setRadomTargetState(MarkovState radomTargetState) {
		this.radomTargetState = radomTargetState;
	}

	public Double getActualTargetValue() {
		return actualTargetValue;
	}

	public void setActualTargetValue(Double actualTargetValue) {
		this.actualTargetValue = actualTargetValue;
	}

	public MarkovState getActualTargetState() {
		return actualTargetState;
	}

	public boolean hasActualTargetState() {
		return (this.actualTargetState != null);
	}

	public void setActualTargetState(MarkovState actualTargetState) {
		this.actualTargetState = actualTargetState;
	}

	public void generateRandomState() {
		radomTargetState = NodeMarkovStates.getRandomState(stateProbabilities);
	}

	public MarkovState getMostLikelyState() {
		return NodeMarkovStates.getMostLikelyState(stateProbabilities);
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
			return actualTargetState.getId().equals(this.radomTargetState.getId());
		}
		return false;
	}

	public boolean hasFailed() {
		if (actualTargetState != null) {
			MarkovState mostLikelySate = getMostLikelyState();
			return mostLikelySate != null && !actualTargetState.getId().equals(mostLikelySate.getId());
		}
		return false;
	}

	public boolean isMostLikelyStateOK() {
		if (actualTargetState != null) {
			MarkovState mostLikelySate = getMostLikelyState();
			return actualTargetState.getId().equals(mostLikelySate.getId());
		}
		return false;
	}

	public TransitionMatrixKey getTargetTransitionMatrixKey(PredictionContext context) {
		return context.getTransitionMatrixKey(targetTimeWindow, variable);
	}

	public List<Double> getVectorDifferential() {
		List<Double> result = new ArrayList<Double>();
		if(actualStatesStatistic != null) {
			for(int stateIndex = 0; stateIndex < stateProbabilities.size(); stateIndex++) {
				MarkovState state = NodeMarkovStates.getStatesList().get(stateIndex);
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

	public Double getDifferentialComplementary() {
		Double differential = getDifferential();
		if(differential==null) {
			return null;
		}
		return 1 - differential;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		StringBuffer sStateProbabilities = new StringBuffer();
		for (double nextProba : stateProbabilities) {
			sStateProbabilities.append(UtilDates.df2.format(nextProba));
			sStateProbabilities.append(",");
		}
		result.append("PredictionResult [initialDate=").append(UtilDates.format_date_time.format(initialDate))
				.append(",targetDate=").append(UtilDates.format_date_time.format(targetDate)).append(", variable=")
				.append(variable).append(", stateProbabilities=").append(sStateProbabilities)
				.append(", radomTargetState=").append(radomTargetState).append("]");
		return result.toString();
	}
}
