package com.sapereapi.model.learning.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.util.UtilDates;

public class PredictionDeviation implements Serializable {
	private static final long serialVersionUID = 81275L;

	private VariableFeaturesKey transitionMatrixKey;
	private VariableState initialState;
	private VariableState stateUnder;
	private VariableState stateOver;
	private Double excess;
	protected List<Long> listIdPredictions;
	protected List<Date> listTargetDates;
	protected List<List<Double>> listVectorDifferential;

	public PredictionDeviation() {
		super();
	}

	public PredictionDeviation(VariableFeaturesKey _transitionMatrixKey
			,VariableState initialState
			,VariableState _stateUnder
			,VariableState _stateOver
			,Double _excess
	// ,Date targetDate, Double predictedStateProba
	) {
		super();
		this.transitionMatrixKey = _transitionMatrixKey;
		this.listIdPredictions = new ArrayList<>();
		this.listTargetDates = new ArrayList<>();
		// this.listTargetDates.add(targetDate);
		this.listVectorDifferential = new ArrayList<>();
		// this.listPredictedStateProba.add(predictedStateProba);
		this.initialState = initialState;
		this.stateUnder = _stateUnder;
		this.stateOver = _stateOver;
		this.excess = _excess;
	}

	public boolean deviate() {
		return (stateOver != null) && (stateUnder != null)
				&& (!stateOver.getId().equals(stateUnder.getId()));
	}

	public void addPrediction(PredictionResult predictionResult) {
		this.listIdPredictions.add(predictionResult.getPredictionId());
		this.listTargetDates.add(predictionResult.getTargetDate());
		this.listVectorDifferential.add(predictionResult.getVectorDifferential());
	}

	public void addPrediction(Long idPrediction, Date targetDate, List<Double> vectorDifferential) {
		this.listIdPredictions.add(idPrediction);
		this.listTargetDates.add(targetDate);
		this.listVectorDifferential.add(vectorDifferential);
	}

	public List<Long> getListIdPredictions() {
		return listIdPredictions;
	}

	public void setListIdPredictions(List<Long> listIdPredictions) {
		this.listIdPredictions = listIdPredictions;
	}

	public List<Date> getListTargetDates() {
		return listTargetDates;
	}

	public Date getTargetDate(int index) {
		if (listTargetDates != null && index < listTargetDates.size()) {
			return listTargetDates.get(index);
		}
		return null;
	}

	public void setListTargetDates(List<Date> listTargetDates) {
		this.listTargetDates = listTargetDates;
	}

	public Double getExcess() {
		return excess;
	}

	public void setExcess(Double excess) {
		this.excess = excess;
	}


	public List<List<Double>> getListVectorDifferential() {
		return listVectorDifferential;
	}

	public void setListVectorDifferential(List<List<Double>> listVectorDifferential) {
		this.listVectorDifferential = listVectorDifferential;
	}

	public VariableState getInitialState() {
		return initialState;
	}

	public void setInitialState(VariableState initialState) {
		this.initialState = initialState;
	}

	public VariableState getStateUnder() {
		return stateUnder;
	}

	public void setStateUnder(VariableState actualState) {
		this.stateUnder = actualState;
	}

	public VariableState getStateOver() {
		return stateOver;
	}

	public void setStateOver(VariableState _predictedState) {
		this.stateOver = _predictedState;
	}

	public VariableFeaturesKey getTransitionMatrixKey() {
		return transitionMatrixKey;
	}

	public void setTransitionMatrixKey(VariableFeaturesKey transitionMatrixKey) {
		this.transitionMatrixKey = transitionMatrixKey;
	}

	public int getStartHour() {
		return transitionMatrixKey.getFeaturesKey().getTimeWindow().getStartHour();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PredictionDeviation)) {
			return false;
		}
		PredictionDeviation other = (PredictionDeviation) obj;
		return transitionMatrixKey.getId().equals(other.getTransitionMatrixKey().getId())
				&& stateUnder.getId().equals(other.getStateUnder().getId())
				&& stateOver.getId().equals(other.getStateUnder().getId());
	}

	@Override
	public int hashCode() {
		Long transitionMatrixId = transitionMatrixKey.getId();
		return (int) (100 * transitionMatrixId + 10 * stateUnder.getId() + stateOver.getId());
	}

	public static String formatKey(VariableFeaturesKey _usedTransitionMatrixKey, VariableState _actualState,
			VariableState _predictedState) {
		return _usedTransitionMatrixKey.getId() + "." + _actualState.getId() + "." + _predictedState.getId();
	}

	public int getCardinality() {
		return listTargetDates.size();
	}

	public String getKey() {
		return formatKey(transitionMatrixKey, stateUnder, stateOver);
	}

	public int getStepNb() {
		return this.listTargetDates.size();
	}

	public Date getFirstTargetDate() {
		if (listTargetDates.size() > 0) {
			return listTargetDates.get(0);
		}
		return null;
	}

	public int compareStartHour(PredictionDeviation other) {
		if (other == null) {
			return 1;
		}
		return this.getStartHour() - other.getStartHour();
	}

	public int compareStepNb(PredictionDeviation other) {
		if (other == null) {
			return 1;
		}
		return this.getStepNb() - other.getStepNb();
	}

	public int compareFirstTargetDate(PredictionDeviation other) {
		if (other == null) {
			return 1;
		}
		long delta = this.getFirstTargetDate().getTime() - other.getFirstTargetDate().getTime();
		return (int) delta;
	}

	public boolean isOpposedTo(PredictionDeviation other) {
		if (other == null) {
			return false;
		}
		return this.transitionMatrixKey.getId().equals(other.getTransitionMatrixKey().getId())
				&& this.stateUnder.getId().equals(other.getStateOver().getId())
				&& this.stateOver.getId().equals(other.getStateUnder().getId());
	}

	public PredictionDeviation getOpposedTo(List<PredictionDeviation> others) {
		PredictionDeviation result = null;
		for (PredictionDeviation deviation : others) {
			if (isOpposedTo(deviation)) {
				return deviation;
			}
		}
		return result;
	}

	public String getStrListIdPredictions(String separator) {
		String sep2 = "";
		StringBuffer result = new StringBuffer();
		for (Long idPrediction : listIdPredictions) {
			if (idPrediction != null) {
				result.append(sep2);
				result.append(idPrediction);
				sep2 = separator;
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("")
				.append(transitionMatrixKey.getVariable() + " at " + transitionMatrixKey.getFeaturesKey().getTimeWindow().getStartHour())
				.append(" : ").append(stateUnder.getName()).append(" -> ").append(stateOver.getName())
				.append(" ");
		int idx = 0;
		result.append("step deviations :");
		for (Date nextTargetDate : listTargetDates) {
			if (idx > 0) {
				result.append(", ");
			}
			result.append(UtilDates.format_time.format(nextTargetDate));
			if (idx < listVectorDifferential.size()) {
				List<Double> vectorDifferential = listVectorDifferential.get(idx);
				double differential = 0;
				//if(vectorDifferential.size() > 0) {
					for(double nextDiff : vectorDifferential) {
						differential+=Math.abs(nextDiff);
					}
				//}
				result.append("[").append(UtilDates.df5.format(differential)).append("]");
			}
			idx++;
		}
		return result.toString();
	}

	public static List<PredictionDeviation> aggregateDeviations(List<PredictionDeviation> listSingleDeviations,
			int minDeviationSize) {
		Map<String, PredictionDeviation> mapDeviations = new HashMap<String, PredictionDeviation>();
		for (PredictionDeviation singleDeviaiton : listSingleDeviations) {
			VariableFeaturesKey trMatrixKey = singleDeviaiton.getTransitionMatrixKey();
			// PredictionDeviation oldDeviation = new PredictionDeviation(trMatrixKey,
			// singleDeviaiton.getInitialState(), singleDeviaiton.getActualState(),
			// singleDeviaiton.getPredictedState());
			VariableState actualState = singleDeviaiton.getStateUnder();
			VariableState predictedState = singleDeviaiton.getStateOver();
			//VariableState initialState = singleDeviaiton.getInitialState();
			for (int idx = 0; idx < singleDeviaiton.getListIdPredictions().size(); idx++) {
				Long predictionId = singleDeviaiton.getListIdPredictions().get(idx);
				Date targetDate = singleDeviaiton.getTargetDate(idx);
				List<List<Double>> listVectorDifferential = singleDeviaiton.getListVectorDifferential();
				List<Double> firstVectorDifferential = (listVectorDifferential.size()==0)? null :  listVectorDifferential.get(0);
				// oldDeviation.addPrediction(predictionId, targetDate, predictedStateProba);
				String deviationKey = PredictionDeviation.formatKey(trMatrixKey, actualState, predictedState);
				PredictionDeviation nextDeviation = mapDeviations.get(deviationKey);
				nextDeviation.addPrediction(predictionId, targetDate, firstVectorDifferential);
			}
		}
		// List<PredictionDeviation> result = new ArrayList<>();
		return filterDeviations(mapDeviations, minDeviationSize);
	}

	public static List<PredictionDeviation> filterDeviations(Map<String, PredictionDeviation> mapDeviations,
			int minDeviationSize) {
		List<PredictionDeviation> result = new ArrayList<PredictionDeviation>();
		for (PredictionDeviation deviation : mapDeviations.values()) {
			if (deviation.deviate() && deviation.getListIdPredictions().size() >= minDeviationSize) {
				result.add(deviation);
			}
		}
		Collections.sort(result, new Comparator<PredictionDeviation>() {
			@Override
			public int compare(PredictionDeviation o1, PredictionDeviation o2) {
				int result = o1.compareStartHour(o2);
				if (result != 0) {
					return result;
				}
				result = -o1.compareStepNb(o2);
				if (result != 0) {
					return result;
				}
				result = o1.compareFirstTargetDate(o2);
				return result;
			}
		});
		// remove opposite deviations
		List<String> keysToRemove = PredictionData.getOpposedDeviationKeys(result);
		while (keysToRemove.size() > 0) {
			for (String nextKey : keysToRemove) {
				int idx = PredictionData.getDeviationIndex(nextKey, result);
				result.remove(idx);
			}
			keysToRemove = PredictionData.getOpposedDeviationKeys(result);
		}
		return result;
	}

	public boolean isMeaningful() {
		if(stateOver != null && stateUnder != null && excess != null) {
			return excess >= 0.025 && getCardinality()>=5;
		}
		return false;
	}
}
