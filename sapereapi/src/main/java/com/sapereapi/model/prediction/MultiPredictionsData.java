package com.sapereapi.model.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.markov.TransitionMatrixKey;

public class MultiPredictionsData implements Serializable {
	private static final long serialVersionUID = 17357L;

	private PredictionContext predictionContext;
	private TimeSlot targetDateSlot;
	private String[] variables;
	private List<PredictionData> listPredictions = new ArrayList<>();
	private List<PredictionCorrection> listCorrections = new ArrayList<>();
	private Map<Long, Double> entropieByTrMatrixId = new HashMap<>();
	private Map<Long, TransitionMatrixKey> mapTrMatrixKey = new HashMap<>();
	private Map<TransitionMatrixKey, Long> inverseMapTrMatrixKey = new HashMap<>();
	private List<String> errors;

	public MultiPredictionsData() {
		super();
		reset();
	}

	public MultiPredictionsData(PredictionContext _context, TimeSlot _targetDateSlot, String[] _variables) {
		super();
		this.predictionContext = _context;
		this.targetDateSlot = _targetDateSlot;
		this.variables = _variables;
		reset();
	}

	public void reset() {
		listPredictions = new ArrayList<PredictionData>();
		listCorrections = new ArrayList<PredictionCorrection>();
		entropieByTrMatrixId = new HashMap<>();
		mapTrMatrixKey = new HashMap<>();
		inverseMapTrMatrixKey = new HashMap<>();
		errors = new ArrayList<String>();
	}

	public void addPrediction(PredictionData aPrediction) {
		this.listPredictions.add(aPrediction);
		for (String variable : aPrediction.getVariables()) {
			List<TransitionMatrixKey> usedTrMatrixKeys = aPrediction.getListTransitionMatrixKeys(variable);
			for (TransitionMatrixKey nextKey : usedTrMatrixKeys) {
				if (nextKey.getId() != null) {
					if (!mapTrMatrixKey.containsKey(nextKey.getId())) {
						mapTrMatrixKey.put(nextKey.getId(), nextKey);
					}
					if (!inverseMapTrMatrixKey.containsKey(nextKey)) {
						inverseMapTrMatrixKey.put(nextKey, nextKey.getId());
					}
				}
			}
		}
	}

	public void addEntropieResult(TransitionMatrixKey aKey, Double entropie) {
		if (inverseMapTrMatrixKey.containsKey(aKey)) {
			Long id = inverseMapTrMatrixKey.get(aKey);
			entropieByTrMatrixId.put(id, entropie);
		}
	}

	public PredictionContext getPredictionContext() {
		return predictionContext;
	}

	public void setPredictionContext(PredictionContext predictionContext) {
		this.predictionContext = predictionContext;
	}

	public TimeSlot getTargetDateSlot() {
		return targetDateSlot;
	}

	public void setTargetDateSlot(TimeSlot targetDateSlot) {
		this.targetDateSlot = targetDateSlot;
	}

	public String[] getVariables() {
		return variables;
	}

	public void setVariables(String[] variables) {
		this.variables = variables;
	}

	public List<PredictionData> getListPredictions() {
		return listPredictions;
	}

	public void setListPredictions(List<PredictionData> listPredictions) {
		this.listPredictions = listPredictions;
	}

	public List<PredictionCorrection> getListCorrections() {
		return listCorrections;
	}

	public void setListCorrections(List<PredictionCorrection> listCorrections) {
		this.listCorrections = listCorrections;
	}

	public Map<Long, Double> getEntropieByTrMatrixId() {
		return entropieByTrMatrixId;
	}

	public void setEntropieByTrMatrixId(Map<Long, Double> entropieByTrMatrixId) {
		this.entropieByTrMatrixId = entropieByTrMatrixId;
	}

	public Map<Long, TransitionMatrixKey> getMapTrMatrixKey() {
		return mapTrMatrixKey;
	}

	public void setMapTrMatrixKey(Map<Long, TransitionMatrixKey> mapTrMatrixKey) {
		this.mapTrMatrixKey = mapTrMatrixKey;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public void addError(String anError) {
		errors.add(anError);
	}

	public void addCorrection(PredictionCorrection aCorrection) {
		listCorrections.add(aCorrection);
	}
}
