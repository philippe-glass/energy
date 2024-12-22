package com.sapereapi.model.learning.aggregation;

import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.learning.lstm.INNLayer;

public class LSTMAggregationResult extends AbstractAggregationResult {
	private Map<String, INNLayer> nodeLayers = new HashMap<>();
	private INNLayer aggregatedLayer = null;

	public LSTMAggregationResult() {
		super();
	}

	public Map<String, INNLayer> getNodeLayers() {
		return nodeLayers;
	}

	public void setNodeLayers(Map<String, INNLayer> nodeLayers) {
		this.nodeLayers = nodeLayers;
	}

	public INNLayer getAggregatedLayer() {
		return aggregatedLayer;
	}

	public void setAggregatedLayer(INNLayer aggregatedLayer) {
		this.aggregatedLayer = aggregatedLayer;
	}

	public int getNodesCount() {
		return this.nodeLayers.size();
	}

	public void addNodeLayer(String nodeName, INNLayer layer) {
		nodeLayers.put(nodeName, layer);
	}
}
