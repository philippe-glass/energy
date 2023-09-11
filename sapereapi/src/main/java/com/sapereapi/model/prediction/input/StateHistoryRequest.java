package com.sapereapi.model.prediction.input;

import java.util.Date;

public class StateHistoryRequest {
	private Long minDateLong;
	private String variableName;
	private Boolean observationUpdated;

	public Long getMinDateLong() {
		return minDateLong;
	}

	public Date getMinDate() {
		return new Date(minDateLong);
	}

	public void setMinDateLong(Long minDateLong) {
		this.minDateLong = minDateLong;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public Boolean getObservationUpdated() {
		return observationUpdated;
	}

	public void setObservationUpdated(Boolean observationUpdated) {
		this.observationUpdated = observationUpdated;
	}

	public StateHistoryRequest() {
		super();
	}

	public StateHistoryRequest(Long _minDateLong, String variableName, Boolean _observationUpdated) {
		super();
		this.minDateLong = _minDateLong;
		this.variableName = variableName;
		this.observationUpdated = _observationUpdated;
	}

}
