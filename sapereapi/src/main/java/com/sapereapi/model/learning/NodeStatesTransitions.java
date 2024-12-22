package com.sapereapi.model.learning;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class NodeStatesTransitions implements Serializable {
	private static final long serialVersionUID = 1L;
	protected Date stateDate;
	protected Map<String, Double> mapLastValues = new HashMap<String, Double>();
	protected Map<String, VariableState> mapLastStates = new HashMap<String, VariableState>();
	protected Map<String, Double> mapValues = new HashMap<String, Double>();
	protected Map<String, VariableState> mapStates = new HashMap<String, VariableState>();
	protected Map<String, Long> mapStateHistoryId = new HashMap<String, Long>(); // history id in database for each variable

	public NodeStatesTransitions( Date aComputeDate) {
		//super(predictionContext, _variables, aComputeDate, _maxTotalPower);
		stateDate = aComputeDate;
	}

	public Date getStateDate() {
		return stateDate;
	}

	public void setStateDate(Date stateDate) {
		this.stateDate = stateDate;
	}

	public VariableState getState(String variable) {
		if(!mapStates.containsKey(variable)) {
			return null;
		}
		return mapStates.get(variable);
	}

	public void clearMapStateHistoryId() {
		mapStateHistoryId.clear();
	}

	public void setStateHistoryId(String variable, long historyId) {
		mapStateHistoryId.put(variable, historyId);
	}

	public Long getStateHistoryId(String variable) {
		if(!mapStateHistoryId.containsKey(variable)) {
			return null;
		}
		return mapStateHistoryId.get(variable);
	}

	private void copyCurrentToLast() {
		mapLastValues.clear();
		for (String key : mapValues.keySet()) {
			mapLastValues.put(key, mapValues.get(key));
		}
		mapLastStates.clear();
		for (String key : mapStates.keySet()) {
			VariableState clone = mapStates.get(key) == null ? null : mapStates.get(key).clone();
			mapLastStates.put(key, clone);
		}
	}

	public void initializeLast(PredictionContext predictionContext, AbstractLogger logger) {
		mapLastValues.clear();
		mapLastStates.clear();
		for (String variable : predictionContext.getNodeContext().getVariables()) {
			double value = 0;
			mapLastValues.put(variable, value);
			try {
				mapLastStates.put(variable, NodeStates.getVariablState(value));
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	public void setCurrentState(PredictionContext predicitonContext, SingleNodeStateItem singleNodeStateItem, AbstractLogger logger) {
		mapValues.clear();
		mapStates.clear();
		// Set current state date
		stateDate = singleNodeStateItem.getDate();
		try {
			for (String variableName : predicitonContext.getNodeContext().getVariables()) {
				Double power = singleNodeStateItem.getValue(variableName);
				if (power != null) {
					mapStates.put(variableName, NodeStates.getVariablState(power));
					mapValues.put(variableName, power);
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public void setCurrentState(PredictionContext predicitonContext, NodeTotal nodeTotal, AbstractLogger logger) {
		mapValues.clear();
		mapStates.clear();
		// Set current state date
		stateDate = nodeTotal.getDate();
		try {
			for (String variableName : predicitonContext.getNodeContext().getVariables()) {
				Double power = nodeTotal.getVariablePower(variableName);
				if (power != null) {
					mapStates.put(variableName, NodeStates.getVariablState(power));
					mapValues.put(variableName, power);
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public void refreshTransitions(PredictionContext predicitonContext, NodeTotal nodeTotal, boolean enableObsUpdate, AbstractLogger logger) {
		copyCurrentToLast();
		setCurrentState(predicitonContext, nodeTotal, logger);

		//super.refreshStates(nodeTotal);
		// For debug
		for(String variable : mapStates.keySet()) {
			if(mapStates.containsKey(variable) && mapLastStates != null && mapLastStates.containsKey(variable)) {
				VariableState currentState = mapStates.get(variable);
				VariableState lastState = mapLastStates.get(variable);
				// For debug
				if(enableObsUpdate && currentState != null && lastState != null) {
					if(lastState.getId().equals(currentState.getId()) && currentState.getId() >=4) {
						logger.warning("refreshTransitions : observation on stationary transition " + variable + " " +  currentState.getLabel());
					}
				}
			}
		}
	}

	public VariableState getCurrentState(String variable) {
		return mapStates.get(variable);
	}

	public boolean hasTransition(String variable) {
		VariableState lastState = getLastState(variable);
		VariableState currentState = getCurrentState(variable);
		return (lastState != null) && (currentState != null);
	}

	public Integer getCurrentStateId(String variable) {
		VariableState state = getCurrentState(variable);
		if (state != null) {
			return state.getId();
		}
		return null;
	}

	public Double getCurrentValue(String variable) {
		if(mapValues.containsKey(variable)) {
			return  mapValues.get(variable);
		}
		return null;
	}

	public void setValue(String variable, double currentValue, double lastValue) {
		mapValues.put(variable, currentValue);
		mapLastValues.put(variable, lastValue);
	}

	public void setState(String variable, VariableState currentState, VariableState lastState) {
		mapStates.put(variable, currentState);
		mapLastStates.put(variable, lastState);
	}

	public Map<String, Double> getMapLastValues() {
		return mapLastValues;
	}

	public Map<String, VariableState> getMapLastStates() {
		return mapLastStates;
	}

	public VariableState getLastState(String variable) {
		return mapLastStates.get(variable);
	}

	public Map<String, Double> getMapValues() {
		return mapValues;
	}

	public Map<String, VariableState> getMapStates() {
		return mapStates;
	}

	public Integer getLastStateId(String variable) {
		VariableState state = getLastState(variable);
		if (state != null) {
			return state.getId();
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (stateDate != null) {
			result.append("time ").append(UtilDates.format_time.format(this.stateDate)).append(SapereUtil.CR);
			for (String variableName : mapStates.keySet()) {
				VariableState current = mapStates.get(variableName);
				VariableState last = mapLastStates.get(variableName);
				result.append(SapereUtil.CR).append(variableName).append(" : ").append(last).append(" -> ").append(current);
			}
		}
		return result.toString();
	}

	public CompactNodeStatesTransitions generateCompactNodeStatesTransitions() {
		CompactNodeStatesTransitions result = new CompactNodeStatesTransitions(stateDate);
		for(String variable : mapValues.keySet()) {
			if(mapLastValues.containsKey(variable)) {
				Double lastValue = mapLastValues.get(variable);
				result.getMapLastValues().put(variable, lastValue);
			}
			if(mapValues.containsKey(variable)) {
				Double value = mapValues.get(variable);
				result.getMapValues().put(variable, value);
			}
		}
		for(String variable : mapStates.keySet()) {
			if(mapLastStates.containsKey(variable)) {
				VariableState lastState = null;
				lastState = mapLastStates.get(variable);
				result.getMapLastStateIds().put(variable, lastState.getId());
			}
			if(mapStates.containsKey(variable)) {
				VariableState state = mapStates.get(variable);
				result.getMapStateIds().put(variable, state.getId());
			}
		}
		return result;
	}

	public NodeStatesTransitions clone() {
		NodeStatesTransitions result = new NodeStatesTransitions(stateDate);
		for(String variable : mapValues.keySet()) {
			if(mapLastValues.containsKey(variable)) {
				Double lastValue = mapLastValues.get(variable);
				result.getMapLastValues().put(variable, lastValue);
			}
			if(mapValues.containsKey(variable)){
				Double value = mapValues.get(variable);
				result.getMapValues().put(variable, value);
			}
		}
		for(String variable : mapStates.keySet()) {
			if(mapLastStates.containsKey(variable)) {
				VariableState lastState = mapLastStates.get(variable);
				result.getMapLastStates().put(variable, lastState);
			}
			if(mapStates.containsKey(variable)) {
				VariableState state = mapStates.get(variable);
				result.getMapStates().put(variable, state);
			}
		}
		return result;
	}

	public SingleNodeStateItem generateSingleNodeStateItem(PredictionContext predictionContext) {
		SingleNodeStateItem nextNodeState = new SingleNodeStateItem();
		nextNodeState.setDate(this.stateDate);
		for(String variable : predictionContext.getNodeContext().getVariables()) {
			Integer stateId = getCurrentStateId(variable);
			Double value = getCurrentValue(variable);
			nextNodeState.setStateId(variable, stateId);
			nextNodeState.setValue(variable, value);
		}
		return nextNodeState;
	}

	public boolean isComplete(PredictionContext predictionContext) {
		boolean result = mapLastStates != null && mapStates != null && mapLastValues != null && mapValues != null;
		if(result) {
			for(String variable : predictionContext.getNodeContext().getVariables()) {
				result = result && mapLastStates.containsKey(variable) && mapStates.containsKey(variable);
				if(result) {
					result = result && mapLastValues.containsKey(variable) && mapValues.containsKey(variable);
				}
			}
		}
		return result;
	}
}
