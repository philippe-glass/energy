package com.sapereapi.model.markov;

import java.io.Serializable;

import com.sapereapi.helper.PredictionHelper;

public class TransitionMatrixKey implements Serializable {
	private static final long serialVersionUID = 442311;
	private Long contextId;
	private String variable = null;
	private MarkovTimeWindow timeWindow;
	private Long id = null;

	public TransitionMatrixKey() {
		super();
	}

	public TransitionMatrixKey(Long _id, Long _contextId, String variable, MarkovTimeWindow timeWindow) {
		super();
		this.id = _id;
		this.contextId = _contextId;
		this.variable = variable;
		this.timeWindow = timeWindow;
	}

	public Long getContextId() {
		return contextId;
	}

	public void setContextId(Long contextId) {
		this.contextId = contextId;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public MarkovTimeWindow getTimeWindow() {
		return timeWindow;
	}

	public void setTimeWindow(MarkovTimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getTimeWindowId() {
		return this.timeWindow.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TransitionMatrixKey) {
			TransitionMatrixKey other = (TransitionMatrixKey) obj;
			return this.getTimeWindowId() == other.getTimeWindowId()
					&& this.contextId.equals(other.getContextId())
					&& this.variable.equals(other.getVariable());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int variableCode = PredictionHelper.getVariableIndex(variable);
		int ctxId = contextId.intValue();
		int result = 10000 * ctxId + 100 * variableCode + timeWindow.getStartHour();
		// result = 100*variableCode + timeWindow.getStartHour();
		// result = timeWindow.getStartHour();
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("contextId: ").append(contextId);
		result.append(", variable: ").append(variable);
		if (timeWindow != null) {
			result.append(", start time: ").append(timeWindow.getStartHour());
		}
		return result.toString();
	}
}
