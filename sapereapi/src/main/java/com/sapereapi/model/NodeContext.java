package com.sapereapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapereapi.lightserver.DisableJson;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;

public class NodeContext implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 879794;
	protected Long id = null;
	protected NodeLocation nodeLocation = null;
	protected List<NodeLocation> neighbourNodeLocations = new ArrayList<>();
	protected String scenario;
	protected long timeShiftMS = 0;
	protected Session session = null;
	protected Map<Integer, Integer> datetimeShifts = null;
	protected Double maxTotalPower = null;
	protected String variables[] = { "requested", "produced", "consumed", "provided", "available", "missing" };
	protected boolean supervisionDisabled;
	protected boolean awardsActivated = true;
	protected boolean energyStorageActivated = false;
	protected boolean complementaryRequestsActivated = true;
	protected boolean qualityOfServiceActivated = false;
	protected PredictionSetting nodePredicitonSetting = new PredictionSetting();
	protected PredictionSetting clusterPredictionSetting = new PredictionSetting();
	protected String urlForcasting = null;
	protected String timeZoneId = "Europe/Zurich";
	protected int debugLevel;
	protected String learningAgentName;
	protected String regulatorAgentName;

	public NodeContext(Long _id
			, NodeLocation _nodeLocation
			, String scenario, Map<Integer, Integer> _datetimeShifts
			, Double _maxTotalPower
			, Session aSession
			, String[] _variables
			, String _learningAgentName
			, String _regulatorAgentName
			, boolean _supervisionDisabled
			, boolean _activateComplementaryRequests
			, boolean _awardsActivated
			, boolean _energyStorageActivated
			, PredictionSetting _nodePredicitonSetting
			, PredictionSetting _clusterPredictionSetting
			, String _timeZoneId
			, String _urlForcasting
			, int _debugLevel) {
		super();
		this.id = _id;
		this.nodeLocation = _nodeLocation;
		this.scenario = scenario;
		this.datetimeShifts = _datetimeShifts;
		this.timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		this.maxTotalPower = _maxTotalPower;
		this.session = aSession;
		this.variables = _variables;
		this.supervisionDisabled = _supervisionDisabled;
		this.complementaryRequestsActivated = _activateComplementaryRequests;
		this.awardsActivated = _awardsActivated;
		this.energyStorageActivated = _energyStorageActivated;
		this.nodePredicitonSetting = _nodePredicitonSetting;
		this.clusterPredictionSetting = _clusterPredictionSetting;
		this.neighbourNodeLocations = new ArrayList<>();
		this.timeZoneId = _timeZoneId;
		this.urlForcasting = _urlForcasting;
		this.learningAgentName = _learningAgentName;
		this.regulatorAgentName = _regulatorAgentName;
		this.debugLevel = _debugLevel;
	}

	public NodeContext() {
		super();
	}

	public boolean isLocal() {
		return (nodeLocation != null) && NodeManager.isLocal(nodeLocation);
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

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	@DisableJson
	public String getLearningAgentName() {
		return learningAgentName;
	}

	public void setLearningAgentName(String learningAgentName) {
		this.learningAgentName = learningAgentName;
	}

	public String getRegulatorAgentName() {
		return regulatorAgentName;
	}

	public void setRegulatorAgentName(String regulatorAgentName) {
		this.regulatorAgentName = regulatorAgentName;
	}

	public Map<Integer, Integer> getDatetimeShifts() {
		return datetimeShifts;
	}

	public void setDatetimeShifts(Map<Integer, Integer> datetimeShifts) {
		this.datetimeShifts = datetimeShifts;
		this.timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
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

	public NodeLocation getNodeLocation() {
		return nodeLocation;
	}

	public void setNodeLocation(NodeLocation nodeLocation) {
		this.nodeLocation = nodeLocation;
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

	public boolean isAwardsActivated() {
		return awardsActivated;
	}

	public void setAwardsActivated(boolean awardsActivated) {
		this.awardsActivated = awardsActivated;
	}

	public boolean isEnergyStorageActivated() {
		return energyStorageActivated;
	}

	public void setEnergyStorageActivated(boolean energyStorageActivated) {
		this.energyStorageActivated = energyStorageActivated;
	}

	public PredictionSetting getPredictionSetting(PredictionScope scope) {
		if(PredictionScope.NODE.equals(scope)) {
			return nodePredicitonSetting;
		} else if(PredictionScope.CLUSTER.equals(scope)) {
			return clusterPredictionSetting;
		}
		return null;
	}

	public boolean isPredictionsActivated(PredictionScope scope) {
		PredictionSetting predictionSetting = getPredictionSetting(scope);
		return predictionSetting != null && predictionSetting.isActivated();
	}

	public boolean _isPredictionsActivated() {
		return nodePredicitonSetting.isActivated() || clusterPredictionSetting.isActivated();
	}

	public boolean isAggregationsActivated(PredictionScope scope) {
		PredictionSetting predictionSetting = getPredictionSetting(scope);
		return predictionSetting.isModelAggregationActivated();
	}

	public boolean isQualityOfServiceActivated() {
		return qualityOfServiceActivated;
	}

	public void setQualityOfServiceActivated(boolean qualityOfServiceActivated) {
		this.qualityOfServiceActivated = qualityOfServiceActivated;
	}

	public void setComplementaryRequestsActivated(boolean complementaryRequestsActivated) {
		this.complementaryRequestsActivated = complementaryRequestsActivated;
	}

	public List<NodeLocation> getNeighbourNodeLocations() {
		return neighbourNodeLocations;
	}

	public void setNeighbourNodeLocations(List<NodeLocation> neighbourNodeLocations) {
		this.neighbourNodeLocations = neighbourNodeLocations;
	}

	public void resetNeighbourNodeLocations() {
		this.neighbourNodeLocations.clear();
	}

	public void addNeighbourNodeLocation(NodeLocation aNodeLocation) {
		this.neighbourNodeLocations.add(aNodeLocation);
	}

	public String getUrlForcasting() {
		return urlForcasting;
	}

	public void setUrlForcasting(String urlForcasting) {
		this.urlForcasting = urlForcasting;
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

	public List<String> getNeighbourNMainServiceAddresses() {
		List<String> result = new ArrayList<String>();
		for(NodeLocation nextNodeLocation : this.neighbourNodeLocations) {
			result.add(nextNodeLocation.getMainServiceAddress());
		}
		return result;
	}

	public List<String> getNeighbourNodes() {
		List<String> result = new ArrayList<String>();
		for(NodeLocation nextNodeLocation : this.neighbourNodeLocations) {
			result.add(nextNodeLocation.getName());
		}
		return result;
	}


	public NodeContext clone() {
		return copyContent(true);
	}

	public NodeContext copyContent(boolean copyId) {
		Long idToSet = copyId ? id : null;
		NodeContext result = new NodeContext(idToSet, nodeLocation.clone(), scenario, datetimeShifts, maxTotalPower
				, session == null ? null : session.clone()
				, variables, learningAgentName, regulatorAgentName
				, supervisionDisabled, complementaryRequestsActivated, awardsActivated, energyStorageActivated
				, nodePredicitonSetting.clone(), clusterPredictionSetting.clone()
				, timeZoneId, urlForcasting, debugLevel);
		for(NodeLocation nextNodeLocation : neighbourNodeLocations) {
			result.addNeighbourNodeLocation(nextNodeLocation.clone());
		}
		return result;
	}

	public boolean hasDatetimeShift() {
		return (datetimeShifts.size() > 0);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}

	public String getMainServiceAddress() {
		if (nodeLocation == null) {
			return null;
		}
		return nodeLocation.getMainServiceAddress();
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}

}
