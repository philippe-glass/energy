package com.sapereapi.model.learning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class NodeStates implements Serializable {
	private static final long serialVersionUID = 9118011L;
	public final static double DEFAULT_NODE_MAX_TOTAL_POWER = 3000;
	public final static String CR = System.getProperty("line.separator"); // Carriage return
	protected String[]  variables= {};
	protected String location;
	//protected TransitionMatrixScope scope;
	protected String scenario;
	protected Map<String, Double> mapValues = new HashMap<String, Double>();
	protected Map<String, VariableState> mapStates = new HashMap<String, VariableState>();
	protected Date stateDate;
	protected static Double maxTotalPower;
	protected static List<VariableState> statesList =new ArrayList<VariableState>();

	public static VariableState getVariablState(Double value) throws HandlingException {
		VariableState result = null;
		for (VariableState variableState : statesList) {
			if (variableState.containsValue(value)) {
				if (result != null) {
					throw new HandlingException("Variable state doublon for value " + value + " " + result + " " + variableState);
				}
				result = variableState;
			}
		}
		if (result == null) {
			throw new HandlingException("Variable state not found for value " + value);
		}
		return result;
	}

	public static VariableState getById(Integer id) {
		if(id!=null) {
			for (VariableState variableState : statesList) {
				if(id.equals(variableState.getId())) {
					return variableState;
				}
			}
		}
		return null;
	}

	public static List<VariableState> getStatesList() {
		return statesList;
	}

	public static int getStateIndex(VariableState state) {
		int idx = 0;
		for(VariableState nextSate : statesList) {
			if(nextSate.getId().equals(state.getId())) {
				return idx;
			}
			idx++;
		}
		return statesList.indexOf(state);
	}

	public static int getNbOfStates() {
		return statesList.size();
	}

	public static void initialize(Double _maxTotalPower) {
		maxTotalPower = _maxTotalPower;
		statesList = Arrays.asList(new VariableState[] {
				new VariableState(1, BooleanOperator.EQUALS, Double.valueOf(0), null, null),
				new VariableState(2, BooleanOperator.GREATER_THAN			 , 0.0 * maxTotalPower, BooleanOperator.LESS_THAN, 0.2 * maxTotalPower),
				new VariableState(3, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.2 * maxTotalPower, BooleanOperator.LESS_THAN, 0.4 * maxTotalPower),
				new VariableState(4, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.4 * maxTotalPower, BooleanOperator.LESS_THAN, 0.6 * maxTotalPower),
				new VariableState(5, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.6 * maxTotalPower, BooleanOperator.LESS_THAN, 0.8 * maxTotalPower),
				new VariableState(6, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.8 * maxTotalPower, BooleanOperator.LESS_THAN, 1.0 * maxTotalPower),
				new VariableState(7, BooleanOperator.GREATER_THAN_OR_EQUALS, 1.0 * maxTotalPower, null, null) });
	}

	public NodeStates(PredictionContext predictionContext, String[] _variables, Date aDate, Double _maxTotalPower) {
		super();
		initialize(_maxTotalPower);
		this.variables = _variables;
		this.location = predictionContext.getMainServiceAddress();
		this.scenario = predictionContext.getScenario();
		this.stateDate = aDate;
		reset();
	}

	public void reset() {
		mapValues.clear();
		mapStates.clear();
		for (String variable : this.variables) {
			mapStates.put(variable, null);
			mapValues.put(variable, null);
		}
	}

	public void refreshStates(NodeTotal nodeTotal, AbstractLogger logger) {
		try {
			for (String variableName : mapStates.keySet()) {
				Double power = nodeTotal.getVariablePower(variableName);
				if (power != null) {
					mapStates.put(variableName, getVariablState(power));
					mapValues.put(variableName, power);
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public Date getStateDate() {
		return stateDate;
	}

	public void setStateDate(Date aDate) {
		this.stateDate = aDate;
	}

	public VariableState getState(String variable) {
		return mapStates.get(variable);
	}

	public Map<String, VariableState> getMapStates() {
		return mapStates;
	}

	public Map<String, Double> getMapValues() {
		return mapValues;
	}

	public String[] getVariables() {
		return variables;
	}

	public void setVariables(String[] variables) {
		this.variables = variables;
	}

	public String getScenario() {
		return scenario;
	}
	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public static VariableState getMostLikelyState(List<Double> probList) {
		if(probList!=null && probList.size()==statesList.size()) {
			double maxProba = 0;
			int stateIndex = 0;
			int idx = 0;
			for(double nextStateProba : probList) {
				if(nextStateProba > maxProba) {
					maxProba = nextStateProba;
					stateIndex = idx;
				}
				idx++;
			}
			return statesList.get(stateIndex);
		}
		return null;
	}

	public static VariableState getRandomState(List<Double> probList) {
		if(probList.size()==statesList.size()) {
			double random = Math.random();
			double probSum = 0;
			int stateIndex = 0;
			for(double nextProb : probList) {
				probSum+=nextProb;
				if(random < probSum) {
					return statesList.get(stateIndex);
				}
				stateIndex++;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (stateDate != null) {
			result.append("time ").append(UtilDates.format_time.format(this.stateDate)).append(CR);
			for (String variableName : mapStates.keySet()) {
				result.append(CR).append(variableName).append(":").append((mapStates.get(variableName)));
			}
		}
		return result.toString();
	}

	public static double getFirstNonNullBondary() {
		for( VariableState nextState : statesList) {
			if(nextState.getMaxValue() != null && nextState.getMaxValue() > 0) {
				return nextState.getMaxValue();
			}
		}
		return 0.0;
	}
}
