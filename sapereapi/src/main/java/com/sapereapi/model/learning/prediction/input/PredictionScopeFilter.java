package com.sapereapi.model.learning.prediction.input;

import com.sapereapi.model.OptionItem;
import com.sapereapi.model.learning.PredictionScope;

public class PredictionScopeFilter {
	private OptionItem scope = null;

	public OptionItem getScope() {
		return scope;
	}

	public void setScope(OptionItem scope) {
		this.scope = scope;
	}

	public PredictionScope getScopeEnum() {
		return PredictionScope.valueOf(scope.getLabel());
	}

}
