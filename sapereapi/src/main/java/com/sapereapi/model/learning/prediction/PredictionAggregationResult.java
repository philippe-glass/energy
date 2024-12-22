package com.sapereapi.model.learning.prediction;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;

public class PredictionAggregationResult extends AbstractAggregationResult {
	Map<Date, SimplePredictionAggregationResult> mapReults = new HashMap<Date, SimplePredictionAggregationResult>();

	public PredictionAggregationResult() {
		super();
	}

	public Map<Date, SimplePredictionAggregationResult> getMapReults() {
		return mapReults;
	}

	public void setMapReults(Map<Date, SimplePredictionAggregationResult> mapReults) {
		this.mapReults = mapReults;
	}

	public void setAggredatedStateProbabilities(Date aDate, List<Double> listProba) {
		if (!mapReults.containsKey(aDate)) {
			SimplePredictionAggregationResult simplePredictionAggregationResult = new SimplePredictionAggregationResult();
			simplePredictionAggregationResult.setTargetDate(aDate);
			mapReults.put(aDate, simplePredictionAggregationResult);
		}
		SimplePredictionAggregationResult simplePredictionAggregationResult = mapReults.get(aDate);
		simplePredictionAggregationResult.setAggregatedStateProbabilities(listProba);
	}

	public void setNodeStateProbabilities(Date aDate, String node, List<Double> listProba) {
		if (!mapReults.containsKey(aDate)) {
			mapReults.put(aDate, new SimplePredictionAggregationResult());
		}
		SimplePredictionAggregationResult simplePredictionAggregationResult = mapReults.get(aDate);
		simplePredictionAggregationResult.setStateProbabilities(node, listProba);
	}

	public int getNodesCount() {
		int result = 0;
		for (SimplePredictionAggregationResult nextResult : mapReults.values()) {
			int nextNodeCount = nextResult.getNodesCount();
			if (nextNodeCount > result) {
				result = nextNodeCount;
			}
		}
		return result;
	}
}
