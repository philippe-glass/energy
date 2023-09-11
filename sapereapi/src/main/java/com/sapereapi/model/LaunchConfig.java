package com.sapereapi.model;

import java.util.HashMap;
import java.util.Map;

import eu.sapere.middleware.node.NodeConfig;

public class LaunchConfig {
	Map<String, NodeConfig> mapNodes = new HashMap<String, NodeConfig>();
	Map<String, String> mapLocationByNode = new HashMap<String, String>();

	public Map<String, NodeConfig> getMapNodes() {
		return mapNodes;
	}

	public void setMapNodes(Map<String, NodeConfig> mapNodes) {
		this.mapNodes = mapNodes;
	}

	public Map<String, String> getMapLocationByNode() {
		return mapLocationByNode;
	}

	public void setMapLocationByNode(Map<String, String> mapLocationByNode) {
		this.mapLocationByNode = mapLocationByNode;
	}

	public LaunchConfig() {
		super();
	}

	public void addNodeConfig(NodeConfig nodeConfig) {
		mapNodes.put(nodeConfig.getName(), nodeConfig);
	}

	public NodeConfig getFirstNode() {
		NodeConfig result = null;
		for (String nodeName : mapNodes.keySet()) {
			if (result == null) {
				result = mapNodes.get(nodeName);
			}
		}
		return result;
	}

	public String getBaseUrl() {
		NodeConfig firstNode = getFirstNode();
		if (firstNode != null) {
			return firstNode.getUrl();
		}
		return null;
	}

	public int getNodesNb() {
		return mapNodes.size();
	}
}
