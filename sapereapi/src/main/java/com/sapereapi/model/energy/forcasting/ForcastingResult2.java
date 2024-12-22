package com.sapereapi.model.energy.forcasting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ForcastingResult2 implements Serializable {
	private static final long serialVersionUID = 1L;
	List<List<Double>> predictions;

	public List<List<Double>> getPredictions() {
		return predictions;
	}

	public void setPredictions(List<List<Double>> predictions) {
		this.predictions = predictions;
	}

	public List<Double> getFlatList() {
		List<Double> result = new ArrayList<>();
		for (List<Double> listValues : predictions) {
			result.addAll(listValues);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (List<Double> listValues : predictions) {
			for (Double value : listValues) {
				result.append("" + value).append(", ");
			}
		}
		return result.toString();
	}
}
