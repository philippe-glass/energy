package com.sapereapi.model;

import java.io.Serializable;

import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;

public class PredictionSetting implements Serializable {
	private static final long serialVersionUID = 1L;
	private Boolean activated = Boolean.FALSE;
	private LearningAggregationOperator aggregator = null;
	private LearningModelType usedModel = null;

	public PredictionSetting() {
		super();
	}

	public PredictionSetting(Boolean activated, LearningAggregationOperator modelAggregator, LearningModelType _usedModel) {
		super();
		this.activated = activated;
		this.aggregator = modelAggregator;
		this.usedModel = _usedModel;// _usedModel.toOptionItem();
	}

	public Boolean isActivated() {
		return activated;
	}

	public void setActivated(Boolean enabled) {
		this.activated = enabled;
	}

	public LearningAggregationOperator getAggregator() {
		return aggregator;
	}

	public void setAggregator(LearningAggregationOperator modelAggregator) {
		this.aggregator = modelAggregator;
	}

	public boolean isAggregationActivated() {
		return aggregator != null;
	}

	public boolean isModelAggregationActivated() {
		return aggregator != null && aggregator.isModelAggregation();
	}

	public boolean isPredictionAggregationActivated() {
		return aggregator != null && aggregator.isPredictionAggregation();
	}

	public LearningModelType getUsedModel() {
		return usedModel;
	}

	public void setUsedModel(LearningModelType usedModel) {
		this.usedModel = usedModel;
	}

	public PredictionSetting clone() {
		PredictionSetting result = new PredictionSetting();
		if (activated != null) {
			result.setActivated(activated.booleanValue());
		}
		result.setAggregator(aggregator);
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(activated).append(",").append(usedModel).append(",").append(aggregator);
		return result.toString();
	}

}
