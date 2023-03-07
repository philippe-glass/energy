package com.sapereapi.model;

public class NodeConfig {
	private String node;
	private String url;
	private String location;
	// private String serialNumber;
	private boolean isStarted;

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public boolean isStarted() {
		return isStarted;
	}

	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}

	public NodeConfig(String node, String url) {
		super();
		this.node = node;
		this.url = url;
	}

}
