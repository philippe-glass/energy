package com.sapereapi.model.learning;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class CompactNodeStatesTransitions implements Serializable {
	private static final long serialVersionUID = 1L;
	protected Date stateDate;
	protected Map<String, Double> mapLastValues = new HashMap<String, Double>();
	protected Map<String, Integer> mapLastStateIds = new HashMap<String, Integer>();
	protected Map<String, Double> mapValues = new HashMap<String, Double>();
	protected Map<String, Integer> mapStateIds = new HashMap<String, Integer>();

	public CompactNodeStatesTransitions(Date aComputeDate) {
		stateDate = aComputeDate;
	}

	public Date getStateDate() {
		return stateDate;
	}

	public void setStateDate(Date stateDate) {
		this.stateDate = stateDate;
	}

	public Integer getStateId(String variable) {
		if (!mapStateIds.containsKey(variable)) {
			return null;
		}
		return mapStateIds.get(variable);
	}

	public Integer getCurrentStateId(String variable) {
		if (!mapStateIds.containsKey(variable)) {
			return null;
		}
		return mapStateIds.get(variable);
	}

	public boolean hasTransition(String variable) {
		Integer lastState = getLastStateId(variable);
		Integer currentState = getCurrentStateId(variable);
		return (lastState != null) && (currentState != null);
	}

	public Double getCurrentValue(String variable) {
		if (mapValues.containsKey(variable)) {
			return mapValues.get(variable);
		}
		return null;
	}

	public void setValue(String variable, Double currentValue, Double lastValue) {
		mapValues.put(variable, currentValue);
		if (lastValue != null) {
			mapLastValues.put(variable, lastValue);
		}
	}

	public void setStateId(String variable, Integer currentState, Integer lastState) {
		mapStateIds.put(variable, currentState);
		if (lastState != null) {
			mapLastStateIds.put(variable, lastState);
		}
	}

	public Map<String, Double> getMapLastValues() {
		return mapLastValues;
	}

	public Map<String, Integer> getMapLastStateIds() {
		return mapLastStateIds;
	}

	public Integer getLastStateId(String variable) {
		if (!mapLastStateIds.containsKey(variable)) {
			return null;
		}
		return mapLastStateIds.get(variable);
	}

	public Map<String, Double> getMapValues() {
		return mapValues;
	}

	public Map<String, Integer> getMapStateIds() {
		return mapStateIds;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (stateDate != null) {
			result.append("time ").append(UtilDates.format_time.format(this.stateDate)).append(SapereUtil.CR);
			for (String variableName : mapStateIds.keySet()) {
				Integer current = mapStateIds.get(variableName);
				Integer last = mapLastStateIds.get(variableName);
				result.append(SapereUtil.CR).append(variableName).append(" : ").append(last).append(" -> ")
						.append(current);
			}
		}
		return result.toString();
	}

	public NodeStatesTransitions generateNodeStateTransitions() {
		NodeStatesTransitions result = new NodeStatesTransitions(stateDate);
		for (String variable : mapValues.keySet()) {
			if (mapLastValues.containsKey(variable)) {
				Double lastValue = mapLastValues.get(variable);
				result.getMapLastValues().put(variable, lastValue);
			}
			if (mapLastValues.containsKey(variable)) {
				Double value = mapValues.get(variable);
				result.getMapValues().put(variable, value);
			}
		}
		for (String variable : mapStateIds.keySet()) {
			if (mapLastStateIds.containsKey(variable)) {
				Integer lastStateId = mapLastStateIds.get(variable);
				VariableState lastState = NodeStates.getById(lastStateId);
				result.getMapLastStates().put(variable, lastState);
			}
			if (mapStateIds.containsKey(variable)) {
				Integer stateId = mapStateIds.get(variable);
				VariableState state = NodeStates.getById(stateId);
				if (state != null) {
					result.getMapStates().put(variable, state);
				}
			}
		}
		return result;
	}

	public CompactNodeStatesTransitions clone() {
		CompactNodeStatesTransitions result = new CompactNodeStatesTransitions(stateDate);
		for (String variable : mapValues.keySet()) {
			Double lastValue = null;
			Double value = mapValues.get(variable);
			if (mapLastValues.containsKey(variable)) {
				lastValue = mapLastValues.get(variable);
				result.setValue(variable, value, lastValue);
			} else {
				result.getMapLastValues().put(variable, value);
			}
		}
		for (String variable : mapStateIds.keySet()) {
			Integer lastState = null;
			Integer state = mapStateIds.get(variable);
			if (mapLastStateIds.containsKey(variable)) {
				lastState = mapLastStateIds.get(variable);
				result.setStateId(variable, state, lastState);
			} else {
				result.getMapStateIds().put(variable, state);
			}
		}
		return result;
	}
}
