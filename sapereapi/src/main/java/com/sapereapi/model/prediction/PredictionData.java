package com.sapereapi.model.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovTransitions;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.util.UtilDates;

public class PredictionData implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<String> variables;
	private Date initialDate;
	private SortedSet<Date> targetDates = new TreeSet<Date>();
	private List<PredictionStep> listSteps;
	private Map<String, MarkovState> initialStates;
	private Map<String, Double> initialValues;
	private Map<String, Map<Date, PredictionResult>> mapResults;
	private Map<String, PredictionResult> mapLastResults;
	private List<String> errors;
	private PredictionContext context = new PredictionContext();
	private Boolean useCorrections = false;

	public PredictionData() {
		super();
		reset();
	}

	public PredictionData(PredictionContext _context, boolean _useCorrections) {
		super();
		context = _context;
		useCorrections = _useCorrections;
		reset();
	}

	private void reset() {
		listSteps = new ArrayList<PredictionStep>();
		targetDates = new TreeSet<Date>();
		mapResults = new HashMap<String, Map<Date, PredictionResult>>();
		mapLastResults = new HashMap<String, PredictionResult>();
		errors = new ArrayList<String>();
	}

	public List<String> getVariables() {
		return variables;
	}

	public void setVariables(String[] _variables) {
		this.variables = Arrays.asList(_variables);
	}

	public Date getInitialDate() {
		return initialDate;
	}

	public void setInitialDate(Date initialDate) {
		this.initialDate = initialDate;
	}

	public Map<String, MarkovState> getInitialStates() {
		return initialStates;
	}

	public void setInitialStates(Map<String, MarkovState> initialStates) {
		this.initialStates = initialStates;
	}

	public void setInitialContent(NodeMarkovTransitions transitions) {
		this.initialStates = transitions.getMapStates();
		this.initialValues = transitions.getMapValues();
	}

	public boolean hasInitialContent() {
		return initialStates != null && initialValues != null && initialStates.size() > 0 && initialValues.size() > 0;
	}

	public void addTargetDate(Date aDate) {
		targetDates.add(aDate);
	}

	public Date getLastTargetDate() {
		if (targetDates.size() > 0) {
			return targetDates.last();
		}
		return null;// targetDate;
	}

	public Map<String, PredictionResult> getMapLastResults() {
		return mapLastResults;
	}

	public boolean isUseCorrections() {
		return useCorrections;
	}

	public void setUseCorrections(boolean useCorrections) {
		this.useCorrections = useCorrections;
	}

	public void addError(String error) {
		if (!errors.contains(error)) {
			errors.add(error);
		}
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public PredictionStep getFirstStep() {
		if(listSteps.size()>0) {
			return listSteps.get(0);
		}
		return null;
	}

	public PredictionStep getLastStep(Date targetDate) {
		for(PredictionStep nextStep : listSteps) {
			if (nextStep.isInSlot(targetDate)) {
				return nextStep;
			}
		}
		return null;
	}

	public PredictionStep getLastStep() {
		if(listSteps.size()>0) {
			int idx = listSteps.size()-1;
			return listSteps.get(idx);
		}
		return null;
	}

	public List<PredictionStep> getListSteps() {
		return listSteps;
	}

	public void setListSteps(List<PredictionStep> _listSteps) {
		this.listSteps = _listSteps;
	}

	public Map<String, Double> getInitialValues() {
		return initialValues;
	}

	public void setInitialValues(Map<String, Double> initialValues) {
		this.initialValues = initialValues;
	}

	public Map<String, Map<Date, PredictionResult>> getMapResults() {
		return mapResults;
	}

	public void setMapResults(Map<String, Map<Date, PredictionResult>> mapResults) {
		this.mapResults = mapResults;
	}

	public MarkovTimeWindow getInitialTimeWindow() {
		if(listSteps.size() > 0) {
			PredictionStep firstStep = listSteps.get(0);
			return firstStep.getMarkovTimeWindow();
		}
		return null;
	}

	public List<Long> getListTransitionMatrixId(String variable) {
		List<Long> result = new ArrayList<Long>();
		for(PredictionStep nextStep : listSteps) {
			long nextId = nextStep.getUsedTransitionMatrixId(variable);
			if(!result.contains(nextId)) {
				result.add(nextId);
			}
		}
		return result;
	}

	public List<TransitionMatrixKey> getListTransitionMatrixKeys(String variable) {
		List<TransitionMatrixKey> result = new ArrayList<TransitionMatrixKey>();
		for(PredictionStep nextStep : listSteps) {
			TransitionMatrixKey nextKey = nextStep.getUsedTransitionMatrixKey(variable, context);
			if(!result.contains( nextKey)) {
				result.add(nextKey);

			}
		}
		return result;
	}

	public List<Integer> getListTimeWindowId() {
		List<Integer> result = new ArrayList<Integer>();
		for(PredictionStep nextStep : listSteps) {
			int nextId = nextStep.getMarovTimeWindowId();
			if(!result.contains(nextId)) {
				result.add(nextId);
			}
		}
		return result;
	}

	public void setResult(String variable, Date targetDate, PredictionResult result) {
		if (targetDates.contains(targetDate) && variables.contains(variable)) {
			if (!mapResults.containsKey(variable)) {
				mapResults.put(variable, new HashMap<Date, PredictionResult>());
			}
			(this.mapResults.get(variable)).put(targetDate, result);
			Date lastTargetDate = getLastTargetDate();
			if (lastTargetDate.equals(targetDate)) {
				mapLastResults.put(variable, result);
			}
		}
	}

	public boolean hasResult(String variable, Date targetDate) {
		return mapResults.containsKey(variable) && (mapResults.get(variable)).containsKey(targetDate);
	}

	public boolean hasLastResult(String variable) {
		return mapLastResults.containsKey(variable);
	}

	public PredictionResult getResult(String variable, Date targetDate) {
		if (hasResult(variable, targetDate)) {
			return (mapResults.get(variable)).get(targetDate);
		}
		return null;
	}

	public PredictionResult getLastResult(String variable) {
		return mapLastResults.get(variable);
	}

	public MarkovState getLastRandomTargetState(String variable) {
		PredictionResult result = getLastResult(variable);
		if (result != null) {
			return result.getRadomTargetState();
		}
		return null;
	}

	public void generateRandomState() {
		for (String variable : mapResults.keySet()) {
			Map<Date, PredictionResult> varResults = mapResults.get(variable);
			for (PredictionResult nextResult : varResults.values()) {
				nextResult.generateRandomState();
			}
		}
	}

	public PredictionContext getContext() {
		return context;
	}

	public void setContext(PredictionContext context) {
		this.context = context;
	}

	public SortedSet<Date> getTargetDates() {
		return targetDates;
	}

	public SortedSet<Date> getTargetDatesWithoutResult(String variable) {
		SortedSet<Date> result = new TreeSet<Date>();
		for (Date targetDate : this.targetDates) {
			if (!hasResult(variable, targetDate)) {
				result.add(targetDate);
			}
		}
		return result;
	}

	public void setVariables(List<String> variables) {
		this.variables = variables;
	}

	public void setTargetDates(SortedSet<Date> targetDates) {
		this.targetDates = targetDates;
	}

	public void setMapLastResults(Map<String, PredictionResult> mapLastResults) {
		this.mapLastResults = mapLastResults;
	}

	public void setActualTargetState(String variable, Date targetDate, MarkovState actualTargetState, Double actualTargetValue) {
		if (hasResult(variable, targetDate)) {
			PredictionResult predictionResult = (mapResults.get(variable)).get(targetDate);
			predictionResult.setActualTargetState(actualTargetState);
			predictionResult.setActualTargetValue(actualTargetValue);
		}
	}

	public void setActualStatesStatistics(String variable, Date targetDate, StatesStatistic statesStatistic) {
		if (hasResult(variable, targetDate)) {
			PredictionResult predictionResult = (mapResults.get(variable)).get(targetDate);
			predictionResult.setActualStatesStatistic(statesStatistic);
		}
	}

	public void setActualTargetState(NodeMarkovTransitions targetTransition) {
		Date targetDate = targetTransition.getStateDate();
		for(String variable : targetTransition.getMapStates().keySet()) {
			MarkovState state =  targetTransition.getMapStates().get(variable);
			Double value = targetTransition.getMapValues().get(variable);
			setActualTargetState(variable,targetDate,state, value);
		}
	}

	public static int getDeviationIndex(String key, List<PredictionDeviation> deviations) {
		int idx = 0;
		for(PredictionDeviation nextDeviation : deviations) {
			if(key.equals(nextDeviation.getKey())) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	public static List<String> getOpposedDeviationKeys(List<PredictionDeviation> deviations) {
		List<String> result = new ArrayList<>();
		PredictionDeviation opposedDeviation = null;
		for(int idx = 0; idx < deviations.size() && opposedDeviation==null; idx++) {
			PredictionDeviation nextDeviation = deviations.get(idx);
			List<PredictionDeviation> followingDeviations = deviations.subList(idx+1, deviations.size()-0);
			opposedDeviation = nextDeviation.getOpposedTo(followingDeviations);
			if(opposedDeviation != null) {
				result.add(nextDeviation.getKey());
				result.add(opposedDeviation.getKey());
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "PredictionData [scenario = " + context.getScenario() + ", initialDate=" + UtilDates.formatTimeOrDate(initialDate)
				+ ", targetDate=" + UtilDates.formatTimeOrDate(getLastTargetDate()) + ", initialStates="
				+ initialStates + ", mapResults=" + mapResults + "]";
	}
}
