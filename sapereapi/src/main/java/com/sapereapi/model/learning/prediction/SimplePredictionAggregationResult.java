package com.sapereapi.model.learning.prediction;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimplePredictionAggregationResult implements Serializable {
	private static final long serialVersionUID = 1L;
	Date targetDate = null;
	String variableName = null;

	List<Double> aggregatedStateProbabilities;
	Map<String, List<Double>> nodesStateProbabilities = new HashMap<String, List<Double>>();

	public SimplePredictionAggregationResult() {
		super();
	}

	public Date getTargetDate() {
		return targetDate;
	}

	public void setTargetDate(Date targetDate) {
		this.targetDate = targetDate;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public List<Double> getAggregatedStateProbabilities() {
		return aggregatedStateProbabilities;
	}

	public void setAggregatedStateProbabilities(List<Double> aggregatedStateProbabilities) {
		this.aggregatedStateProbabilities = aggregatedStateProbabilities;
	}

	public Map<String, List<Double>> getNodesStateProbabilities() {
		return nodesStateProbabilities;
	}

	public void setNodesStateProbabilities(Map<String, List<Double>> nodesStateProbabilities) {
		this.nodesStateProbabilities = nodesStateProbabilities;
	}

	public int getNodesCount() {
		return this.nodesStateProbabilities.size();
	}

	public void setStateProbabilities(String node, List<Double> listProba) {
		nodesStateProbabilities.put(node, listProba);
	}
}
