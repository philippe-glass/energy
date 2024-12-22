package com.sapereapi.model.learning.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.matrix.DoubleMatrix;

public class PredictionStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private FeaturesKey featuresKey;
	private Date startDate;
	private Date endDate;
	private Map<String, Long> usedTransitionMatrixId = null;
	private List<Double> stateProbabilities = null;

	public PredictionStep(FeaturesKey _featuresKey, Date startDate, Date endDate) {
		super();
		this.featuresKey = _featuresKey;
		this.usedTransitionMatrixId = new HashMap<String, Long>();
		this.startDate = startDate;
		this.endDate = endDate;
		this.stateProbabilities = new ArrayList<Double>();
	}

	public FeaturesKey getFeaturesKey() {
		return featuresKey;
	}

	public void setFeaturesKey(FeaturesKey featuresKey) {
		this.featuresKey = featuresKey;
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
		if (featuresKey != null) {
			return featuresKey.getTimeWindowId();
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

	public VariableFeaturesKey getUsedTransitionMatrixKey(String variable, PredictionContext context) {
		if (this.usedTransitionMatrixId.containsKey(variable)) {
			VariableFeaturesKey trMatrixKey = context.getTransitionMatrixKey(featuresKey, variable);
			trMatrixKey.setId(usedTransitionMatrixId.get(variable));
			return trMatrixKey;
		}
		return null;
	}

	public boolean isInSlot(Date aDate) {
		return (!aDate.before(startDate)) && aDate.before(this.endDate);
	}

	public void setStateProbabilities(DoubleMatrix predictionRowMatrix) {
		stateProbabilities = new ArrayList<>();
		for (double value : predictionRowMatrix.getRowPackedCopy()) {
			stateProbabilities.add(value);
		}
	}

	public List<Double> getStateProbabilities() {
		return stateProbabilities;
	}

	public VariableState getMostLikelyState() {
		return NodeStates.getMostLikelyState(stateProbabilities);
	}

	public double getDurationMinutes() {
		return UtilDates.computeDurationMinutes(startDate, endDate);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(UtilDates.format_time.format(startDate)).append("-")
				.append(UtilDates.format_time.format(endDate));
		if (this.featuresKey != null) {
			result.append(featuresKey);
		}
		return result.toString();
	}
}
