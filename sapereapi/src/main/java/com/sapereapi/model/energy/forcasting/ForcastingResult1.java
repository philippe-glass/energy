package com.sapereapi.model.energy.forcasting;

import java.io.Serializable;
import java.util.List;

public class ForcastingResult1 implements Serializable {
	private static final long serialVersionUID = 1L;
	List<Double> predictions;

	public List<Double> getPredictions() {
		return predictions;
	}

	public void setPredictions(List<Double> predictions) {
		this.predictions = predictions;
	}


	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Double value : predictions) {
			result.append("" + value).append(", ");
		}
		return result.toString();
	}
}
