package com.sapereapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeConfig;
import eu.sapere.middleware.node.NodeManager;

public class NodeContext implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 879794;
	protected Long id = null;
	protected NodeConfig nodeConfig = null;
	protected List<NodeConfig> neighbourNodeConfigs = new ArrayList<>();
	protected String scenario;
	protected long timeShiftMS = 0;
	protected String sessionId = null;
	protected Map<Integer, Integer> datetimeShifts = null;
	protected Double maxTotalPower = null;
	protected String variables[] = { "requested", "produced", "consumed", "provided", "available", "missing" };
	protected boolean supervisionDisabled;
	protected boolean complementaryRequestsActivated = true;
	protected boolean aggregationsActivated = true;
	protected boolean predictionsActivated = false;
	protected String urlForcasting = null;
	protected TimeZone timeZone = TimeZone.getTimeZone("Europe/Zurich");

	public NodeContext(Long _id, NodeConfig _nodeConfig, String scenario, Map<Integer, Integer> _datetimeShifts,
			Double _maxTotalPower, String aSessionId, String[] _variables
			, boolean _supervisionDisabled
			, boolean _activateComplementaryRequests
			, boolean _activateAggregation
			, boolean _activatePrediction
			, TimeZone _timeZone
			, String _urlForcasting) {
		super();
		this.id = _id;
		this.nodeConfig = _nodeConfig;
		this.scenario = scenario;
		this.datetimeShifts = _datetimeShifts;
		this.timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		this.maxTotalPower = _maxTotalPower;
		this.sessionId = aSessionId;
		this.variables = _variables;
		this.supervisionDisabled = _supervisionDisabled;
		this.complementaryRequestsActivated = _activateComplementaryRequests;
		this.aggregationsActivated = _activateAggregation;
		this.predictionsActivated = _activatePrediction;
		this.neighbourNodeConfigs = new ArrayList<>();
		this.timeZone = _timeZone;
		this.urlForcasting = _urlForcasting;
	}

	public NodeContext() {
		super();
	}

	public boolean isLocal() {
		return (nodeConfig != null) && NodeManager.isLocal(nodeConfig);
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public Double getMaxTotalPower() {
		return maxTotalPower;
	}

	public void setMaxTotalPower(Double maxTotalPower) {
		this.maxTotalPower = maxTotalPower;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<Integer, Integer> getDatetimeShifts() {
		return datetimeShifts;
	}

	public void setDatetimeShifts(Map<Integer, Integer> datetimeShifts) {
		this.datetimeShifts = datetimeShifts;
	}

	public String[] getVariables() {
		return variables;
	}

	public void setVariables(String[] variables) {
		this.variables = variables;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public NodeConfig getNodeConfig() {
		return nodeConfig;
	}

	public void setNodeConfig(NodeConfig nodeConfig) {
		this.nodeConfig = nodeConfig;
	}

	public boolean isSupervisionDisabled() {
		return supervisionDisabled;
	}

	public void setSupervisionDisabled(boolean supervisionDisabled) {
		this.supervisionDisabled = supervisionDisabled;
	}

	public boolean isComplementaryRequestsActivated() {
		return complementaryRequestsActivated;
	}

	public void setcomplementaryRequestsActivated(boolean activateComplementaryRequests) {
		this.complementaryRequestsActivated = activateComplementaryRequests;
	}

	public boolean isAggregationsActivated() {
		return aggregationsActivated;
	}

	public void setAggregationsActivated(boolean activateAggregation) {
		this.aggregationsActivated = activateAggregation;
	}

	public boolean isPredictionsActivated() {
		return predictionsActivated;
	}

	public void setPredictionsActivated(boolean activatePrediction) {
		this.predictionsActivated = activatePrediction;
	}

	public List<NodeConfig> getNeighbourNodeConfigs() {
		return neighbourNodeConfigs;
	}

	public void setNeighbourNodeConfigs(List<NodeConfig> neighbourNodeConfigs) {
		this.neighbourNodeConfigs = neighbourNodeConfigs;
	}

	public void resetNeighbourNodeConfig() {
		this.neighbourNodeConfigs.clear();
	}

	public void addNeighbourNodeConfig(NodeConfig aNodeConfig) {
		this.neighbourNodeConfigs.add(aNodeConfig);
	}

	public String getUrlForcasting() {
		return urlForcasting;
	}

	public void setUrlForcasting(String urlForcasting) {
		this.urlForcasting = urlForcasting;
	}

	public List<Long> getNeighbourNodeConfigIds() {
		List<Long> result = new ArrayList<Long>();
		for(NodeConfig nextNodeConfig : this.neighbourNodeConfigs) {
			result.add(nextNodeConfig.getId());
		}
		return result;
	}

	public List<String> getNeighbourNMainServiceAddresses() {
		List<String> result = new ArrayList<String>();
		for(NodeConfig nextNodeConfig : this.neighbourNodeConfigs) {
			result.add(nextNodeConfig.getMainServiceAddress());
		}
		return result;
	}

	public String getNeighbourNames() {
		StringBuffer result = new StringBuffer();
		String sep = "";
		for(NodeConfig nextNodeConfig : this.neighbourNodeConfigs) {
			result.append(sep).append(nextNodeConfig.getName());
			sep = ",";
		}
		return result.toString();
	}

	public NodeContext clone() {
		NodeContext result = new NodeContext(id, nodeConfig.clone(), scenario, datetimeShifts, maxTotalPower, sessionId,
				variables, supervisionDisabled, complementaryRequestsActivated
				, aggregationsActivated, predictionsActivated, timeZone, urlForcasting);
		for(NodeConfig nextNodeConfig : neighbourNodeConfigs) {
			result.addNeighbourNodeConfig(nextNodeConfig.clone());
		}
		return result;
	}

	public boolean hasDatetimeShift() {
		return (datetimeShifts.size() > 0);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}

	public String getMainServiceAddress() {
		if (nodeConfig == null) {
			return null;
		}
		return nodeConfig.getMainServiceAddress();
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

}
