package com.sapereapi.model.learning.aggregation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.learning.prediction.LearningAggregationOperator;

public abstract class AbstractAggregationResult {
	protected String variableName = "";
	protected LearningAggregationOperator aggragationOperator = null;
	protected Date aggregationDate = null;
	protected Map<String, Double> aggregationWeights = new HashMap<String, Double>();
	private Map<String, String> mapNodeByAgent = new HashMap<String, String>();

	public abstract int getNodesCount();

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public Map<String, Double> getAggregationWeights() {
		return aggregationWeights;
	}

	public void setAggregationWeights(Map<String, Double> aggregationWeights) {
		this.aggregationWeights = aggregationWeights;
	}

	public LearningAggregationOperator getAggragationOperator() {
		return aggragationOperator;
	}

	public void setAggragationOperator(LearningAggregationOperator aggragationOperator) {
		this.aggragationOperator = aggragationOperator;
	}

	public Map<String, String> getMapNodeByAgent() {
		return mapNodeByAgent;
	}

	public void setMapNodeByAgent(Map<String, String> mapNodeByAgent) {
		this.mapNodeByAgent = mapNodeByAgent;
	}

	public Date getAggregationDate() {
		return aggregationDate;
	}

	public void setAggregationDate(Date aggregationDate) {
		this.aggregationDate = aggregationDate;
	}

}
