package com.sapereapi.model;

import java.util.ArrayList;
import java.util.List;

public class NodesAddresses {
	private String[] listNodeBaseUrl;
	private List<NodeConfig> listNodeConfig;

	public String[] getListNodeBaseUrl() {
		return listNodeBaseUrl;
	}

	public void setListNodeBaseUrl(String[] listNodeBaseUrl) {
		this.listNodeBaseUrl = listNodeBaseUrl;
	}

	public List<NodeConfig> getListNodeConfig() {
		return listNodeConfig;
	}

	public void setListNodeConfig(List<NodeConfig> listNodeConfig) {
		this.listNodeConfig = listNodeConfig;
	}

	public NodesAddresses() {
		super();
		listNodeBaseUrl = new String[] {};
		listNodeConfig = new ArrayList<>();
	}

	public void resetList() {
		listNodeBaseUrl = new String[] {};
		listNodeConfig = new ArrayList<>();
	}

	public void addNodeConfig(NodeConfig nodeConfig) {
		listNodeConfig.add(nodeConfig);
		listNodeBaseUrl = new String[listNodeConfig.size()];
		int idx = 0;
		for (NodeConfig nextNodeConfig : listNodeConfig) {
			listNodeBaseUrl[idx] = nextNodeConfig.getUrl();
			idx++;
		}
	}

}
