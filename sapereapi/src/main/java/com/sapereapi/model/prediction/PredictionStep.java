package com.sapereapi.model.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;

public class PredictionStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private MarkovTimeWindow markovTimeWindow;
	private Date startDate;
	private Date endDate;
	private Map<String, Long> usedTransitionMatrixId = null;
	private List<Double> stateProbabilities = null;

	public PredictionStep(MarkovTimeWindow _markovTimeWindow, Date startDate, Date endDate) {
		super();
		this.markovTimeWindow = _markovTimeWindow;
		this.usedTransitionMatrixId = new HashMap<String, Long>();
		this.startDate = startDate;
		this.endDate = endDate;
		this.stateProbabilities = new ArrayList<Double>();
	}

	public MarkovTimeWindow getMarkovTimeWindow() {
		return markovTimeWindow;
	}

	public void setMarkovTimeWindow(MarkovTimeWindow _markovTimeWindow) {
		this.markovTimeWindow = _markovTimeWindow;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public int getMarovTimeWindowId() {
		if (markovTimeWindow != null) {
			return markovTimeWindow.getId();
		}
		return -1;
	}

	public void setUsedTransitionMatrixId(String variable, Long id) {
		this.usedTransitionMatrixId.put(variable, id);
	}

	public Long getUsedTransitionMatrixId(String variable) {
		if (this.usedTransitionMatrixId.containsKey(variable)) {
			return this.usedTransitionMatrixId.get(variable);
		}
		return null;
	}

	public TransitionMatrixKey getUsedTransitionMatrixKey(String variable, PredictionContext context) {
		if (this.usedTransitionMatrixId.containsKey(variable)) {
			TransitionMatrixKey trMatrixKey = context.getTransitionMatrixKey(markovTimeWindow, variable);
			trMatrixKey.setId(usedTransitionMatrixId.get(variable));
			return trMatrixKey;
		}
		return null;
	}

	public boolean isInSlot(Date aDate) {
		return (!aDate.before(startDate)) && aDate.before(this.endDate);
	}

	public void setStateProbabilities(Matrix predictionRowMatrix) {
		stateProbabilities = new ArrayList<>();
		for (double value : predictionRowMatrix.getRowPackedCopy()) {
			stateProbabilities.add(value);
		}
	}

	public List<Double> getStateProbabilities() {
		return stateProbabilities;
	}

	public MarkovState getMostLikelyState() {
		return NodeMarkovStates.getMostLikelyState(stateProbabilities);
	}

	public double getDurationMinutes() {
		return UtilDates.computeDurationMinutes(startDate, endDate);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(UtilDates.format_time.format(startDate)).append("-")
				.append(UtilDates.format_time.format(endDate));
		if (this.markovTimeWindow != null) {
			result.append("(MTW:").append(this.markovTimeWindow.getId()).append(")");
		}
		return result.toString();
	}
}
