package com.sapereapi.model.markov;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.util.UtilDates;

public class NodeMarkovTransitions extends NodeMarkovStates {
	private static final long serialVersionUID = 1L;
	protected Map<String, Double> mapLastValues = new HashMap<String, Double>();
	protected Map<String, MarkovState> mapLastStates = new HashMap<String, MarkovState>();

	public NodeMarkovTransitions(PredictionContext predictionContext, String[] _variables, Date aComputeDate, Double _maxTotalPower) {
		super(predictionContext, _variables, aComputeDate, _maxTotalPower);
	}

	private void copyToLast() {
		mapLastValues.clear();
		for (String key : mapValues.keySet()) {
			mapLastValues.put(key, mapValues.get(key));
		}
		mapLastStates.clear();
		for (String key : mapStates.keySet()) {
			MarkovState clone = mapStates.get(key) == null ? null : mapStates.get(key).clone();
			mapLastStates.put(key, clone);
		}
	}

	public void initializeLast() {
		mapLastValues.clear();
		mapLastStates.clear();
		for (String variable : super.variables) {
			double value = 0;
			mapLastValues.put(variable, value);
			try {
				mapLastStates.put(variable, NodeMarkovStates.getMarkovState(value));
			} catch (Exception e) {
				SapereLogger.getInstance().error(e);
			}
		}
	}

	public void refreshTransitions(NodeTotal nodeTotal, boolean enableObsUpdate) {
		copyToLast();
		reset();
		super.refreshStates(nodeTotal);
		// For debug
		for(String variable : mapStates.keySet()) {
			if(mapStates.containsKey(variable) && mapLastStates != null && mapLastStates.containsKey(variable)) {
				MarkovState currentState = mapStates.get(variable);
				MarkovState lastState = mapLastStates.get(variable);
				// For debug
				if(enableObsUpdate && currentState != null && lastState != null) {
					if(lastState.getId().equals(currentState.getId()) && currentState.getId() >=4) {
						SapereLogger.getInstance().warning("refreshTransitions : observation on stationary transition " + variable + " " +  currentState.getLabel());
					}
				}
			}
		}
	}

	public MarkovState getCurrentState(String variable) {
		return mapStates.get(variable);
	}

	public boolean hasTransition(String variable) {
		MarkovState lastState = getLastState(variable);
		MarkovState currentState = getCurrentState(variable);
		return (lastState != null) && (currentState != null);
	}

	public Integer getCurrentStateId(String variable) {
		MarkovState state = getCurrentState(variable);
		if (state != null) {
			return state.getId();
		}
		return null;
	}

	public void setValue(String variable, double currentValue, double lastValue) {
		mapValues.put(variable, currentValue);
		mapLastValues.put(variable, lastValue);
	}

	public void setState(String variable, MarkovState currentState, MarkovState lastState) {
		mapStates.put(variable, currentState);
		mapLastStates.put(variable, lastState);
	}

	public Map<String, Double> getMapLastValues() {
		return mapLastValues;
	}

	public Map<String, MarkovState> getMapLastStates() {
		return mapLastStates;
	}

	public MarkovState getLastState(String variable) {
		return mapLastStates.get(variable);
	}

	public Integer getLastStateId(String variable) {
		MarkovState state = getLastState(variable);
		if (state != null) {
			return state.getId();
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (stateDate != null) {
			result.append("time ").append(UtilDates.format_time.format(this.stateDate)).append(CR);
			for (String variableName : mapStates.keySet()) {
				MarkovState current = mapStates.get(variableName);
				MarkovState last = mapLastStates.get(variableName);
				result.append(CR).append(variableName).append(" : ").append(last).append(" -> ").append(current);
			}
		}
		return result.toString();
	}
}
