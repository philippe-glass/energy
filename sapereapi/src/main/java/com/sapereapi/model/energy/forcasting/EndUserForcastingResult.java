package com.sapereapi.model.energy.forcasting;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class EndUserForcastingResult implements Serializable {
	private static final long serialVersionUID = 1L;
	List<Date> timestamps;
	List<Double> predicetedValues;
	List<Double> realValues;
	String errorMessage;

	public List<Double> getPredicetedValues() {
		return predicetedValues;
	}

	public void setPredicetedValues(List<Double> predicetedValues) {
		this.predicetedValues = predicetedValues;
	}

	public List<Double> getRealValues() {
		return realValues;
	}

	public void setRealValues(List<Double> realValues) {
		this.realValues = realValues;
	}

	public List<Date> getTimestamps() {
		return timestamps;
	}

	public void setTimestamps(List<Date> timestamps) {
		this.timestamps = timestamps;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public boolean isErrorReturned() {
		return (this.errorMessage != null) && (this.errorMessage.length() > 0);
	}
}
