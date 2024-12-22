package com.sapereapi.model.learning.aggregation;

import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.learning.markov.TransitionMatrix;

public class MarkovChainAggregationResult extends AbstractAggregationResult {
	private Map<String, TransitionMatrix> nodeTransitionMatrices = new HashMap<>();
	private TransitionMatrix aggregateTransitionMatrix = new TransitionMatrix();

	public MarkovChainAggregationResult() {
		super();
		aggregateTransitionMatrix = new TransitionMatrix();
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
