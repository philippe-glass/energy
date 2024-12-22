package com.sapereapi.model.learning;

import java.io.Serializable;

import com.sapereapi.helper.PredictionHelper;

public class VariableFeaturesKey implements Serializable {
	private static final long serialVersionUID = 442311;
	private Long predictionContextId;
	private String variable = null;
	private FeaturesKey featuresKey;
	private Long id = null;

	public VariableFeaturesKey() {
		super();
	}

	public VariableFeaturesKey(Long _id, Long _predictionContextId,  String variable, FeaturesKey _featuresKey) {
		super();
		this.id = _id;
		this.predictionContextId = _predictionContextId;
		this.variable = variable;
		this.featuresKey = _featuresKey;
	}

	public Long getPredictionContextId() {
		return predictionContextId;
	}

	public void setPredictionContextId(Long _predictionContextId) {
		this.predictionContextId = _predictionContextId;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public Long getId() {
		return id;
	}

	public FeaturesKey getFeaturesKey() {
		return featuresKey;
	}

	public void setFeaturesKey(FeaturesKey featuresKey) {
		this.featuresKey = featuresKey;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getTimeWindowId() {
		return this.featuresKey.getTimeWindowId();//.timeWindow.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VariableFeaturesKey) {
			VariableFeaturesKey other = (VariableFeaturesKey) obj;
			return this.getTimeWindowId() == other.getTimeWindowId()
					&& this.predictionContextId.equals(other.getPredictionContextId())
					&& this.variable.equals(other.getVariable());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int variableCode = PredictionHelper.getVariableIndex(variable);
		int ctxId = predictionContextId.intValue();
		int result = 10000 * ctxId + 100 * variableCode + featuresKey.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if(id != null) {
			result.append("(id:").append(id).append(") ");
		}
		result.append("contextId: ").append(predictionContextId);
		result.append(", variable: ").append(variable);
		if (featuresKey != null) {
			result.append(featuresKey);
		}
		return result.toString();
	}

	public VariableFeaturesKey clone() {
		VariableFeaturesKey result = new VariableFeaturesKey(id, predictionContextId, variable, (featuresKey == null) ? null : featuresKey.clone());
		return result;
	}
}
