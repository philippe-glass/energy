package com.sapereapi.model.markov;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.util.UtilDates;

public class NodeMarkovStates implements Serializable {
	private static final long serialVersionUID = 9118011L;
	public final static double DEFAULT_NODE_MAX_TOTAL_POWER = 3000;
	public final static String CR = System.getProperty("line.separator"); // Carriage return
	protected String[]  variables= {};
	protected String location;
	//protected TransitionMatrixScope scope;
	protected String scenario;
	protected Map<String, Double> mapValues = new HashMap<String, Double>();
	protected Map<String, MarkovState> mapStates = new HashMap<String, MarkovState>();
	protected Date stateDate;
	protected static Double maxTotalPower;
	protected static List<MarkovState> statesList =new ArrayList<MarkovState>();

	public static MarkovState getMarkovState(Double value) throws Exception {
		MarkovState result = null;
		for (MarkovState markovState : statesList) {
			if (markovState.containsValue(value)) {
				if (result != null) {
					throw new Exception("Markov state doublon for value " + value + " " + result + " " + markovState);
				}
				result = markovState;
			}
		}
		if (result == null) {
			throw new Exception("Markov state not found for value " + value);
		}
		return result;
	}

	public static MarkovState getById(Integer id) {
		if(id!=null) {
			for (MarkovState markovState : statesList) {
				if(id.equals(markovState.getId())) {
					return markovState;
				}
			}
		}
		return null;
	}

	public static List<MarkovState> getStatesList() {
		return statesList;
	}

	public static int getStateIndex(MarkovState state) {
		int idx = 0;
		for(MarkovState nextSate : statesList) {
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

	public NodeMarkovStates(PredictionContext predictionContext, String[] _variables, Date aDate, Double _maxTotalPower) {
		super();
		this.variables = _variables;
		this.location = predictionContext.getLocation();
		this.scenario = predictionContext.getScenario();
		this.stateDate = aDate;
		maxTotalPower = _maxTotalPower;
		statesList = Arrays.asList(new MarkovState[] {
				new MarkovState(1, BooleanOperator.EQUALS, Double.valueOf(0), null, null),
				new MarkovState(2, BooleanOperator.GREATER_THAN			 , 0.0 * maxTotalPower, BooleanOperator.LESS_THAN, 0.2 * maxTotalPower),
				new MarkovState(3, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.2 * maxTotalPower, BooleanOperator.LESS_THAN, 0.4 * maxTotalPower),
				new MarkovState(4, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.4 * maxTotalPower, BooleanOperator.LESS_THAN, 0.6 * maxTotalPower),
				new MarkovState(5, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.6 * maxTotalPower, BooleanOperator.LESS_THAN, 0.8 * maxTotalPower),
				new MarkovState(6, BooleanOperator.GREATER_THAN_OR_EQUALS, 0.8 * maxTotalPower, BooleanOperator.LESS_THAN, 1.0 * maxTotalPower),
				new MarkovState(7, BooleanOperator.GREATER_THAN_OR_EQUALS, 1.0 * maxTotalPower, null, null) });
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

	public void refreshStates(NodeTotal nodeTotal) {
		try {
			for (String variableName : mapStates.keySet()) {
				Double power = nodeTotal.getVariablePower(variableName);
				if (power != null) {
					mapStates.put(variableName, getMarkovState(power));
					mapValues.put(variableName, power);
				}
			}
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
	}

	public Date getStateDate() {
		return stateDate;
	}

	public void setStateDate(Date aDate) {
		this.stateDate = aDate;
	}

	public MarkovState getState(String variable) {
		return mapStates.get(variable);
	}

	public Map<String, MarkovState> getMapStates() {
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

	public static MarkovState getMostLikelyState(List<Double> probList) {
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

	public static MarkovState getRandomState(List<Double> probList) {
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
}
