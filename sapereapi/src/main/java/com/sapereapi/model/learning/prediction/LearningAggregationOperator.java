package com.sapereapi.model.learning.prediction;

import java.io.Serializable;

public class LearningAggregationOperator implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private LearningAggregationType aggregationType;
	private int waitingMinutesBetweenAggragations;

	public String getName() {
		return name;
	}

	public void setName(String operator) {
		this.name = operator;
	}

	public int getWaitingMinutesBetweenAggragations() {
		return waitingMinutesBetweenAggragations;
	}

	public void setWaitingMinutesBetweenAggragations(int waitingMinutesBetweenAggragations) {
		this.waitingMinutesBetweenAggragations = waitingMinutesBetweenAggragations;
	}

	public LearningAggregationType getAggregationType() {
		return aggregationType;
	}

	public void setAggregationType(LearningAggregationType aggregationType) {
		this.aggregationType = aggregationType;
	}

	public LearningAggregationOperator() {
	}

	public LearningAggregationOperator(LearningAggregationType aggregationType, String name, int waitingMinutesBetweenAggragations) {
		super();
		this.name = name;
		this.aggregationType = aggregationType;
		this.waitingMinutesBetweenAggragations = waitingMinutesBetweenAggragations;
	}

	public static LearningAggregationOperator createModelAggregationOperator(String name, int waitingMinutesBetweenAggragations) {
		return new LearningAggregationOperator(LearningAggregationType.MODEL, name, waitingMinutesBetweenAggragations);
	}

	public static LearningAggregationOperator createPredictionAggregationOperator(String name, int waitingMinutesBetweenAggragations) {
		return new LearningAggregationOperator(LearningAggregationType.PREDICTION, name, waitingMinutesBetweenAggragations);
	}

	public boolean isModelAggregation() {
		return LearningAggregationType.MODEL.equals(aggregationType);
	}

	public boolean isPredictionAggregation() {
		return LearningAggregationType.PREDICTION.equals(aggregationType);
	}

	@Override
	public String toString() {
		return "LearningAggregationOperator [name=" + name + ", aggregationType=" + aggregationType + "]";
	}

}
