package com.sapereapi.model.prediction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.util.UtilDates;

public class StatesStatistic {
	private String location = null;
	private String scenario = null;
	private String variable = null;
	private Date computeDay = null;
	private Date date = null;
	private Integer hour = null;
	// private TimeSlot timeSlot = null;
	private Integer nbOfCorrections = null;
	private Double shannonEntropie = null;
	private Double giniIndex = null;
	private Map<String, Integer> stateDistribution = new HashMap<String, Integer>();

	public void addStateNb(String stateName, int nb) {
		stateDistribution.put(stateName, nb);
	}

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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(Integer hour) {
		this.hour = hour;
	}

	public Integer getNbOfCorrections() {
		return nbOfCorrections;
	}

	public void setNbOfCorrections(Integer nbOfCorrections) {
		this.nbOfCorrections = nbOfCorrections;
	}

	public Double getShannonEntropie() {
		return shannonEntropie;
	}

	public void setShannonEntropie(Double shannonEntropie) {
		this.shannonEntropie = shannonEntropie;
	}

	public Double getGiniIndex() {
		return giniIndex;
	}

	public void setGiniIndex(Double giniIndex) {
		this.giniIndex = giniIndex;
	}

	public Map<String, Integer> getStateDistribution() {
		return stateDistribution;
	}

	public Date getComputeDay() {
		return computeDay;
	}

	public void setComputeDay(Date computeDay) {
		this.computeDay = computeDay;
	}

	public List<Double> getArrayStateDistribution() {
		List<Double> result = new ArrayList<>();
		int total = 0;
		for (Integer nb : stateDistribution.values()) {
			total += nb;
		}
		for (MarkovState state : NodeMarkovStates.getStatesList()) {
			String stateName = state.getName();
			double nextStateRatio = 0;
			if (total > 0 && stateDistribution.containsKey(stateName)) {
				Integer stateCount = stateDistribution.get(stateName);
				nextStateRatio = ((double) stateCount) / total;
			}
			result.add(nextStateRatio);
		}
		return result;
	}

	public void setStateDistribution(Map<String, Integer> stateDistribution) {
		this.stateDistribution = stateDistribution;
	}

	public static String generateKey2(Date aComputeDate, Date aDate, int hourValue, String aVariable) {
		return UtilDates.format_day.format(aComputeDate)
				+ "." +
				UtilDates.format_day.format(aDate)
				+ "." + hourValue + "." + aVariable;
	}

	public static String generateKey(Date aDate, int hourValue, String aVariable) {
		return UtilDates.format_day.format(aDate)
				+ "." + hourValue + "." + aVariable;
	}

	public String getKey() {
		//return generateKey(computeDay, date, hour, variable);
		return generateKey(date, hour, variable);
	}

	public int getTotalNb() {
		int result = 0;
		for (int nextCard : stateDistribution.values()) {
			result += nextCard;
		}
		return result;
	}

	public double getRatio(String aState) {
		double result = 0.0;
		if (stateDistribution.containsKey(aState)) {
			int totalNb = getTotalNb();
			if (totalNb > 0) {
				result = ((double) stateDistribution.get(aState)) / totalNb;
			}
		}
		return result;
	}

	public int compareTo(StatesStatistic other) {
		 String date1 = UtilDates.format_day.format(this.date);
		 String date2 = UtilDates.format_day.format(other.getDate());
		 int result = date1.compareTo(date2);
		 if(result == 0) {
			 result = this.hour - other.getHour();
		 }
		 if(result == 0) {
			 result = this.variable.compareTo(other.getVariable());
		 }
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(getKey()).append(" : ").append(stateDistribution);
		result.append(" ,total:").append(getTotalNb());
		result.append(" ,shannonEntropie:").append(shannonEntropie);
		result.append(" ,giniIndex:").append(giniIndex);
		return result.toString();
	}

}
