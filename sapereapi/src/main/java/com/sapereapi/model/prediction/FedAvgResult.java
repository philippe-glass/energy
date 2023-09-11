package com.sapereapi.model.prediction;

import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.markov.TransitionMatrix;

public class FedAvgResult {
	private String variableName = "";
	Map<String, TransitionMatrix> nodeTransitionMatrices = new HashMap<>();
	TransitionMatrix aggregateTransitionMatrix = new TransitionMatrix();

	public FedAvgResult() {
		super();
		aggregateTransitionMatrix = new TransitionMatrix();
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public Map<String, TransitionMatrix> getNodeTransitionMatrices() {
		return nodeTransitionMatrices;
	}

	public void setNodeTransitionMatrices(Map<String, TransitionMatrix> nodeTransitionMatrices) {
		this.nodeTransitionMatrices = nodeTransitionMatrices;
	}

	public TransitionMatrix getAggregateTransitionMatrix() {
		return aggregateTransitionMatrix;
	}

	public void setAggregateTransitionMatrix(TransitionMatrix aggregateTransitionMatrix) {
		this.aggregateTransitionMatrix = aggregateTransitionMatrix;
	}

	public void addNodeTransitionMatrix(String nodeName, TransitionMatrix trMatrix) {
		nodeTransitionMatrices.put(nodeName, trMatrix);
	}

	public int getNodesCount() {
		return this.nodeTransitionMatrices.size();
	}
}
