package com.sapereapi.model.learning.prediction.input;

import java.util.Date;

import com.sapereapi.model.OptionItem;
import com.sapereapi.model.learning.PredictionScope;

public class StateHistoryRequest {
	private Long minDateLong;
	private String variableName;
	private Boolean observationUpdated;
	OptionItem scope = null;

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

	public OptionItem getScope() {
		return scope;
	}

	public void setScope(OptionItem scope) {
		this.scope = scope;
	}

	public PredictionScope getScopeEnum() {
		return PredictionScope.valueOf(scope.getLabel());
	}

	public StateHistoryRequest() {
		super();
	}

	public StateHistoryRequest(PredictionScope _predictionScope, Long _minDateLong, String variableName, Boolean _observationUpdated) {
		super();
		if(_predictionScope != null) {
			this.scope = _predictionScope.toOptionItem();
		}
		this.minDateLong = _minDateLong;
		this.variableName = variableName;
		this.observationUpdated = _observationUpdated;
	}

}
