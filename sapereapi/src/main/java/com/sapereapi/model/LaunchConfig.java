package com.sapereapi.model;

import java.util.HashMap;
import java.util.Map;

import eu.sapere.middleware.node.NodeLocation;

public class LaunchConfig {
	Map<String, NodeLocation> mapNodes = new HashMap<String, NodeLocation>();
	Map<String, String> mapNodeByLocation = new HashMap<String, String>();

	public Map<String, NodeLocation> getMapNodes() {
		return mapNodes;
	}

	public void setMapNodes(Map<String, NodeLocation> mapNodes) {
		this.mapNodes = mapNodes;
	}

	public Map<String, String> getMapNodeByLocation() {
		return mapNodeByLocation;
	}

	public void setMapNodeByLocation(Map<String, String> mapNodeByLocation) {
		this.mapNodeByLocation = mapNodeByLocation;
	}

	public LaunchConfig() {
		super();
	}

	public void addNodeLocation(NodeLocation nodeLocation) {
		mapNodes.put(nodeLocation.getName(), nodeLocation);
	}

	public NodeLocation getFirstNode() {
		NodeLocation result = null;
		for (String nodeName : mapNodes.keySet()) {
			if (result == null) {
				result = mapNodes.get(nodeName);
			}
		}
		return result;
	}

	public String getBaseUrl() {
		NodeLocation firstNode = getFirstNode();
		if (firstNode != null) {
			return firstNode.getUrl();
		}
		return null;
	}

	public int getNodesNb() {
		return mapNodes.size();
	}
}
