package com.sapereapi.model.learning.prediction;

public class PredictionCorrection extends PredictionDeviation {
	/**
	 * Ajustment of a prediction
	 */
	private static final long serialVersionUID = 1789875L;
	private Integer correctionsNumber = null;

	public PredictionCorrection() {
		super();
	}

	public PredictionCorrection(PredictionDeviation aDeviation,Integer _correctionsNumber) {
		super(aDeviation.getTransitionMatrixKey(),aDeviation.getInitialState()
				,aDeviation.getStateUnder()
				,aDeviation.getStateOver()
				,aDeviation.getExcess());
		this.listTargetDates = aDeviation.getListTargetDates();
		this.listIdPredictions = aDeviation.getListIdPredictions();
		this.listVectorDifferential = aDeviation.getListVectorDifferential();
		this.correctionsNumber = _correctionsNumber;
	}

	public Integer getCorrectionsNumber() {
		return correctionsNumber;
	}

	public void setCorrectionsNumber(Integer correctionsNumber) {
		this.correctionsNumber = correctionsNumber;
	}

}
