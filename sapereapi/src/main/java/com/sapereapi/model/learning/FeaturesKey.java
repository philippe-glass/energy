package com.sapereapi.model.learning;

import java.io.Serializable;

public class FeaturesKey implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TimeWindow timeWindow;

	public TimeWindow getTimeWindow() {
		return timeWindow;
	}

	public void setTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}

	public int getTimeWindowId() {
		return this.timeWindow.getId();
	}

	public FeaturesKey() {
		super();
	}

	public FeaturesKey(TimeWindow timeWindow) {
		super();
		this.timeWindow = timeWindow;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FeaturesKey) {
			FeaturesKey other = (FeaturesKey) obj;
			return this.getTimeWindowId() == other.getTimeWindowId();
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = timeWindow.getStartHour();
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (timeWindow != null) {
			result.append("TW: start at:").append(timeWindow.getStartHour());
		}
		return result.toString();
	}

	public int compareTo(FeaturesKey other) {
		if(other != null && other.getTimeWindow() != null) {
			return timeWindow.getStartHour() - other.getTimeWindow().getStartHour();
		}
		return 1;
	}

	public String getCode() {
		StringBuffer result = new StringBuffer();
		if (timeWindow != null) {
			result.append(timeWindow.getStartHour());
		}
		return result.toString();
	}

	public FeaturesKey clone() {
		FeaturesKey result = new FeaturesKey();
		TimeWindow tw = new TimeWindow(timeWindow.getId(), timeWindow.getDaysOfWeek()
				, timeWindow.getStartHour(), timeWindow.getStartMinute(), timeWindow.getEndHour(), timeWindow.getEndMinute());
		result.setTimeWindow(tw);
		return result;
	}
}
