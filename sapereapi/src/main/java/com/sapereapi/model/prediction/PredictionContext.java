package com.sapereapi.model.prediction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.NodeContext;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.TransitionMatrixKey;

public class PredictionContext extends NodeContext implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 17110984L;
	private String learningAgentName;
	private int learningWindow;
	private List<MarkovTimeWindow> allTimeWindows;
	private Map<Integer, Map<String, TransitionMatrixKey>> mapTransitionMatrixKeys = new HashMap<>();;
	//private boolean useCorrections;

	public PredictionContext(NodeContext nodeContext
			,String learningAgentName
			,int learningWindow
			,List<MarkovTimeWindow> _allTimeWindows
			//,boolean _useCorrections
			) {
		super(nodeContext.getId(), nodeContext.getNodeConfig(), nodeContext.getScenario(), nodeContext.getDatetimeShifts(), nodeContext.getMaxTotalPower(), nodeContext.getSessionId()
				, nodeContext.getVariables()
				, nodeContext.isSupervisionDisabled(), nodeContext.isComplementaryRequestsActivated()
				, nodeContext.isAggregationsActivated(), nodeContext.isPredictionsActivated()
				, nodeContext.getTimeZone(), nodeContext.getUrlForcasting());
		this.learningAgentName = learningAgentName;
		this.learningWindow = learningWindow;
		this.allTimeWindows = _allTimeWindows;
		//this.useCorrections = _useCorrections;
	}

	public PredictionContext() {
		super();
	}

	/*
	public boolean isLocal() {
		return super.isLocal();
	}*/


	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public String getLearningAgentName() {
		return learningAgentName;
	}

	public void setLearningAgentName(String learningAgentName) {
		this.learningAgentName = learningAgentName;
	}

	public int getLearningWindow() {
		return learningWindow;
	}

	public void setLearningWindow(int learningWindow) {
		this.learningWindow = learningWindow;
	}

	public Map<Integer, Map<String, TransitionMatrixKey>> getMapTransitionMatrixKeys() {
		return mapTransitionMatrixKeys;
	}

	public void setMapTransitionMatrixKeys(Map<Integer, Map<String, TransitionMatrixKey>> mapTransitionMatrixKeys) {
		this.mapTransitionMatrixKeys = mapTransitionMatrixKeys;
	}

	public List<MarkovTimeWindow> getAllTimeWindows() {
		return allTimeWindows;
	}

	public void setAllTimeWindows(List<MarkovTimeWindow> allTimeWindows) {
		this.allTimeWindows = allTimeWindows;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public TransitionMatrixKey getTransitionMatrixKey(MarkovTimeWindow timeWindow, String variable) {
		Integer timeWindowId = timeWindow.getId();
		return getTransitionMatrixKey(timeWindowId, variable);
	}

	public TransitionMatrixKey getTransitionMatrixKey(Integer timeWindowId, String variable) {
		if(timeWindowId != null) {
			if(mapTransitionMatrixKeys.containsKey(timeWindowId)) {
				Map<String, TransitionMatrixKey> map1 = mapTransitionMatrixKeys.get(timeWindowId);
				if(map1.containsKey(variable)) {
					return map1.get(variable);
				}
			}
		}
		MarkovTimeWindow timeWindow = getMarkovTimeWindow(timeWindowId);
		return new TransitionMatrixKey(null, this.id, variable, timeWindow );
	}

	public MarkovTimeWindow getMarkovTimeWindow(Integer timeWindowId) {
		MarkovTimeWindow timeWindow = null;
		for(MarkovTimeWindow tw : allTimeWindows) {
			if(timeWindow==null && (tw.getId() == timeWindowId)) {
				timeWindow = tw;
			}
		}
		return timeWindow;
	}

	public PredictionContext clone() {
		PredictionContext result = new PredictionContext();
		result.setLearningAgentName(learningAgentName);
		result.setTimeShiftMS(timeShiftMS);
		result.setLearningWindow(learningWindow);
		result.setAllTimeWindows(allTimeWindows);
		result.setScenario(scenario);
		result.setNodeConfig(nodeConfig.clone());
		result.setId(id);
		Map<Integer, Map<String, TransitionMatrixKey>> copy = new HashMap<Integer, Map<String,TransitionMatrixKey>>();
		for(Integer key1 : mapTransitionMatrixKeys.keySet()) {
			Map<String, TransitionMatrixKey> map1 = mapTransitionMatrixKeys.get(key1);
			copy.put(key1, new HashMap<String, TransitionMatrixKey>());
			Map<String, TransitionMatrixKey> copyMap1 = copy.get(key1);
			 for(String key2 : map1.keySet()) {
				 TransitionMatrixKey value2 = map1.get(key2);
				 copyMap1.put(key2, value2);
			 }
		}
		result.setMapTransitionMatrixKeys(copy);
		return result;
	}
}
