package com.sapereapi.model.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.util.UtilDates;

public class PredictionStatistic implements Serializable {
	private static final long serialVersionUID = 1L;
	private String location = null;
	private String scenario = null;
	private String variable = null;
	private Date computeDay = null;
	private TimeSlot timeSlot = null;
	private List<Integer> horizons = new ArrayList<>();
	private List<Boolean> useOfCorrections = new ArrayList<>();
	private Integer nbOfPredictions = null;
	private Integer nbOfSuccesses = null;
	private Integer nbOfCorrections = null;
	private Double differential = null;
	private Map<String, Double> meansOfProba = new HashMap<String, Double>();
	private StatesStatistic statesStatistic = null;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public Date getComputeDay() {
		return computeDay;
	}

	public void setComputeDay(Date computeDay) {
		this.computeDay = computeDay;
	}

	public TimeSlot getTimeSlot() {
		return timeSlot;
	}

	public void setTimeSlot(TimeSlot timeSlot) {
		this.timeSlot = timeSlot;
	}

	public List<Integer> getHorizons() {
		return horizons;
	}

	public void resetHorizons() {
		this.horizons = new ArrayList<>();
	}

	public void resetUseOfCorrections() {
		this.useOfCorrections = new ArrayList<>();
	}

	public void setHorizons(List<Integer> horizons) {
		this.horizons = horizons;
	}

	public void addHorizon(Integer nextHorizon) {
		this.horizons.add(nextHorizon);
	}

	public void addUseOfCorrections(Boolean useOfCorrections) {
		this.useOfCorrections.add(useOfCorrections);
	}

	public List<Boolean> getUseOfCorrections() {
		return useOfCorrections;
	}

	public void setUseOfCorrections(List<Boolean> useOfCorrections) {
		this.useOfCorrections = useOfCorrections;
	}

	public Integer getNbOfPredictions() {
		return nbOfPredictions;
	}

	public void setNbOfPredictions(Integer nbOfPredictions) {
		this.nbOfPredictions = nbOfPredictions;
	}

	public Integer getNbOfSuccesses() {
		return nbOfSuccesses;
	}

	public void setNbOfSuccesses(Integer nbOfSuccesses) {
		this.nbOfSuccesses = nbOfSuccesses;
	}

	public Integer getNbOfCorrections() {
		return nbOfCorrections;
	}

	public void setNbOfCorrections(Integer nbOfCorrections) {
		this.nbOfCorrections = nbOfCorrections;
	}

	public StatesStatistic getStatesStatistic() {
		return statesStatistic;
	}

	public void setStatesStatistic(StatesStatistic statesStatistic) {
		this.statesStatistic = statesStatistic;
	}

	public Double getDifferential() {
		return differential;
	}

	public Double getDifferentialComplementary() {
		if(differential==null) {
			return null;
		}
		return 1 - differential;
	}

	public void setDifferential(Double vectorDifferential) {
		this.differential = vectorDifferential;
	}

	public Double getSucessRate() {
		if (nbOfSuccesses != null && nbOfPredictions != null && nbOfPredictions > 0) {
			return ((double) nbOfSuccesses) / nbOfPredictions;
		}
		return 0.0;
	}

	public Map<String, Double> getMeansOfProba() {
		return meansOfProba;
	}

	public void setMeansOfProba(Map<String, Double> _meansOfProba) {
		this.meansOfProba = _meansOfProba;
	}

	public void addMeanOfProba(String stateName, double proba) {
		this.meansOfProba.put(stateName, proba);
	}

	public List<Double> getArrayMeansOfProba() {
		List<Double> result = new ArrayList<>();
		for (MarkovState state : NodeMarkovStates.getStatesList()) {
			String stateName = state.getName();
			double nextProba = 0;
			if (meansOfProba.containsKey(stateName)) {
				nextProba = meansOfProba.get(stateName);
			}
			result.add(nextProba);
		}
		return result;
	}

	public String getKeyOfStateDistribution() {
		Date stateDate = timeSlot.getBeginDate();
		//return StatesStatistic.generateKey(computeDay, stateDate, UtilDates.getHourOfDay(stateDate), variable);
		return StatesStatistic.generateKey(stateDate, UtilDates.getHourOfDay(stateDate), variable);
		// return SapereUtil.format_day.format(stateDate)+ "." + hour + "." + variable;
	}

	public int compareTo(PredictionStatistic other) {
		int result = 0;
		if (this.statesStatistic != null && other.getStatesStatistic() != null) {
			result = statesStatistic.compareTo(other.getStatesStatistic());
		}
		if (result == 0) {
			if (horizons.size() == 1 && other.getHorizons().size() == 1) {
				int horizon1 = horizons.get(0);
				int horizon2 = other.getHorizons().get(0);
				result = horizon1 - horizon2;
			} else {
				result = horizons.toString().compareTo(other.getHorizons().toString());
			}
		}
		if (result == 0) {
			result = this.useOfCorrections.toString().compareTo(other.getUseOfCorrections().toString());
		}
		return result;

	}
}
