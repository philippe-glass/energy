package com.sapereapi.model.learning.prediction.input;

import com.sapereapi.model.OptionItem;
import com.sapereapi.model.learning.PredictionScope;

public class AggregationCheckupRequest {
	private String variableName = "";
	private Integer timeWindowId = 0;
	private OptionItem scope = null;
	private String layerId = "";

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public Integer getTimeWindowId() {
		return timeWindowId;
	}

	public void setTimeWindowId(Integer timeWindowId) {
		this.timeWindowId = timeWindowId;
	}

	public OptionItem getScope() {
		return scope;
	}

	public PredictionScope getScopeEnum() {
		return PredictionScope.valueOf(scope.getLabel());
	}

	public void setScope(OptionItem scope) {
		this.scope = scope;
	}


	public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public AggregationCheckupRequest() {
		super();
	}

}
