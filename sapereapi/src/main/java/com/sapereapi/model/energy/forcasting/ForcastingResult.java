package com.sapereapi.model.energy.forcasting;

import java.io.Serializable;
import java.util.List;

public class ForcastingResult implements Serializable {
	private static final long serialVersionUID = 1L;
	List<Double> values;

	public List<Double> getValues() {
		return values;
	}

	public void setValues(List<Double> values) {
		this.values = values;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for(Double value : values) {
			result.append(""+value).append(", ");
		}
		return result.toString();
	}
}
