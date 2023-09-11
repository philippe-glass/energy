package com.sapereapi.model.energy.forcasting.input;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.energy.TimestampedValue;
import com.sapereapi.util.UtilDates;

public class ForcastingRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	// TimestampedValue[] timestampedValues;
	List<TimestampedValue> timestampedValues;

	public ForcastingRequest() {
		super();
	}

	public List<TimestampedValue> getTimestampedValues() {
		return timestampedValues;
	}

	public void setTimestampedValues(List<TimestampedValue> timestampedValues) {
		this.timestampedValues = timestampedValues;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (TimestampedValue nextTSvalue : timestampedValues) {
			result.append("" + nextTSvalue).append(", ");
		}
		return result.toString();
	}

	public Map<String, Double> getMap() {
		Map<String, Double> result = new HashMap<>();
		for (TimestampedValue nextTSvalue : timestampedValues) {
			Date date = nextTSvalue.getTimestamp();
			String jsonDate = UtilDates.format_json_datetime.format(date);
			result.put(jsonDate, nextTSvalue.getValue());
		}
		return result;
	}

}
