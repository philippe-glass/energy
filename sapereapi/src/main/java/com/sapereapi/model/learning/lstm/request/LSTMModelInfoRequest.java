package com.sapereapi.model.learning.lstm.request;

import com.sapereapi.model.learning.PredictionScope;

public class LSTMModelInfoRequest {
	private String nodeName;
	private PredictionScope scope;
	private String[] listVariables;
	private boolean loadExistingWeights = false;

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

	public String[] getListVariables() {
		return listVariables;
	}

	public void setListVariables(String[] listVariables) {
		this.listVariables = listVariables;
	}

	public boolean isLoadExistingWeights() {
		return loadExistingWeights;
	}

	public void setLoadExistingWeights(boolean loadExistingWeights) {
		this.loadExistingWeights = loadExistingWeights;
	}

}
