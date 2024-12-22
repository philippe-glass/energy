package com.sapereapi.model;

import java.util.List;

import com.sapereapi.db.DBConfig;

import eu.sapere.middleware.node.NodeLocation;

public class ServerConfig {
	private String environment = null;
	private String propertiesFileName = null;
	private List<String> initSqlScripts = null;
	private String scenario = "default";
	private boolean modeAuto = false;
	private String csvFile = null;
	private String urlForcasting = null;
	private List<NodeLocation> defaultNeighbours = null;
	private NodeLocation nodeLocation = null;
	private DBConfig dbConfig = null;
	private DBConfig clemapDbConfig = null;
	private PredictionSetting nodePredicitonSetting = new PredictionSetting();
	private PredictionSetting clusterPredictionSetting = new PredictionSetting();
	private boolean useStressTest = false;
	private boolean useFooHandler = false;
	private boolean sendFooJsonResponse = false;
	private int debugLevel = 0;

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
	}

	public List<String> getInitSqlScripts() {
		return initSqlScripts;
	}

	public void setInitSqlScripts(List<String> initSqlScripts) {
		this.initSqlScripts = initSqlScripts;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
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

	public List<NodeLocation> getDefaultNeighbours() {
		return defaultNeighbours;
	}

	public void setDefaultNeighbours(List<NodeLocation> defaultNeighbours) {
		this.defaultNeighbours = defaultNeighbours;
	}

	public NodeLocation getNodeLocation() {
		return nodeLocation;
	}

	public void setNodeLocation(NodeLocation nodeLocation) {
		this.nodeLocation = nodeLocation;
	}

	public DBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DBConfig dbConfig) {
		this.dbConfig = dbConfig;
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

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}

	public PredictionSetting getNodePredicitonSetting() {
		return nodePredicitonSetting;
	}

	public void setNodePredicitonSetting(PredictionSetting nodePredicitonSetting) {
		this.nodePredicitonSetting = nodePredicitonSetting;
	}

	public PredictionSetting getClusterPredictionSetting() {
		return clusterPredictionSetting;
	}

	public void setClusterPredictionSetting(PredictionSetting clusterPredictionSetting) {
		this.clusterPredictionSetting = clusterPredictionSetting;
	}

	public DBConfig getClemapDbConfig() {
		return clemapDbConfig;
	}

	public void setClemapDbConfig(DBConfig clemapDbConfig) {
		this.clemapDbConfig = clemapDbConfig;
	}

}
