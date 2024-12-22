package com.sapereapi.model.learning.lstm;

import java.util.Date;
import java.util.List;

public class LSTMPredictionResult {
	// private List<Date> listDates;
	private List<List<Date>> listDatesX;
	private List<List<Double>> listTrue;
	private List<List<Double>> listPredicted;
	private List<Date> predictionDates;
	private List<Integer> horizons;

	public List<List<Date>> getListDatesX() {
		return listDatesX;
	}

	public void setListDatesX(List<List<Date>> listDatesX) {
		this.listDatesX = listDatesX;
	}

	public List<List<Double>> getListPredicted() {
		return listPredicted;
	}

	public void setListPredicted(List<List<Double>> listPredicted) {
		this.listPredicted = listPredicted;
	}

	public List<List<Double>> getListTrue() {
		return listTrue;
	}

	public void setListTrue(List<List<Double>> listTrue) {
		this.listTrue = listTrue;
	}

	public List<Date> getPredictionDates() {
		return predictionDates;
	}

	public void setPredictionDates(List<Date> predictionDates) {
		this.predictionDates = predictionDates;
	}

	public List<Integer> getHorizons() {
		return horizons;
	}

	public void setHorizons(List<Integer> horizons) {
		this.horizons = horizons;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("listDatesX = ").append(listDatesX);
		result.append("listPredicted ").append(listPredicted);
		result.append("listTrue ").append(listTrue);
		return result.toString();
	}
}
