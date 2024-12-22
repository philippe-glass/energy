package com.sapereapi.model.learning.lstm.request;

import java.util.Map;

import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.util.matrix.DoubleMatrix;

public class LSTMUpdateWeightsRequest {
	private String nodeName;
	private PredictionScope scope;
	private Map<String, Map<String, DoubleMatrix >> mapModeleWeights;
	//private String[] listVariables;

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String node) {
		this.nodeName = node;
	}

	public PredictionScope getScope() {
		return scope;
	}

	public void setScope(PredictionScope scope) {
		this.scope = scope;
	}

	public Map<String, Map<String, DoubleMatrix>> getMapModeleWeights() {
		return mapModeleWeights;
	}

	public void setMapModeleWeights(Map<String, Map<String, DoubleMatrix>> mapModeleWeights) {
		this.mapModeleWeights = mapModeleWeights;
	}

}
