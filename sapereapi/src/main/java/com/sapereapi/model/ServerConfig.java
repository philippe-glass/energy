package com.sapereapi.model;

import java.util.List;

import eu.sapere.middleware.node.NodeConfig;

public class ServerConfig {
	private String initSqlScript = null;
	private boolean modeAuto = false;
	private String csvFile = null;
	private String urlForcasting = null;
	private List<NodeConfig> defaultNeighbours = null;
	private NodeConfig nodeConfig = null;
	private boolean useStressTest = false;
	private boolean useFooHandler = false;
	private boolean sendFooJsonResponse = false;

	public String getInitSqlScript() {
		return initSqlScript;
	}

	public void setInitSqlScript(String initSqlScript) {
		this.initSqlScript = initSqlScript;
	}

	public boolean isModeAuto() {
		return modeAuto;
	}

	public void setModeAuto(boolean modeAuto) {
		this.modeAuto = modeAuto;
	}

	public String getCsvFile() {
		return csvFile;
	}

	public void setCsvFile(String csvFile) {
		this.csvFile = csvFile;
	}

	public String getUrlForcasting() {
		return urlForcasting;
	}

	public void setUrlForcasting(String urlForcasting) {
		this.urlForcasting = urlForcasting;
	}

	public List<NodeConfig> getDefaultNeighbours() {
		return defaultNeighbours;
	}

	public void setDefaultNeighbours(List<NodeConfig> defaultNeighbours) {
		this.defaultNeighbours = defaultNeighbours;
	}

	public NodeConfig getNodeConfig() {
		return nodeConfig;
	}

	public void setNodeConfig(NodeConfig nodeConfig) {
		this.nodeConfig = nodeConfig;
	}

	public boolean isUseStressTest() {
		return useStressTest;
	}

	public void setUseStressTest(boolean useStressTest) {
		this.useStressTest = useStressTest;
	}

	public boolean isUseFooHandler() {
		return useFooHandler;
	}

	public void setUseFooHandler(boolean useFooHandler) {
		this.useFooHandler = useFooHandler;
	}

	public boolean isSendFooJsonResponse() {
		return sendFooJsonResponse;
	}

	public void setSendFooJsonResponse(boolean sendFooJsonResponse) {
		this.sendFooJsonResponse = sendFooJsonResponse;
	}

}
