package com.sapereapi.model.markov;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.util.UtilDates;

public class MarkovStateHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	Long id;
	Integer stateId;
	String stateLabel;
	Integer stateIdLast;
	String stateLabelLast;
	Double value;
	Date date;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getStateId() {
		return stateId;
	}

	public void setStateId(Integer stateId) {
		this.stateId = stateId;
	}

	public String getStateLabel() {
		return stateLabel;
	}

	public void setStateLabel(String label) {
		this.stateLabel = label;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Integer getStateIdLast() {
		return stateIdLast;
	}

	public void setStateIdLast(Integer stateIdLast) {
		this.stateIdLast = stateIdLast;
	}

	public String getStateLabelLast() {
		return stateLabelLast;
	}

	public void setStateLabelLast(String stateLabelLast) {
		this.stateLabelLast = stateLabelLast;
	}

	public boolean isStationary() {
		return stateId != null && stateIdLast != null && (stateId.intValue() == stateIdLast.intValue());
	}

	public MarkovStateHistory() {
		super();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(UtilDates.format_time.format(date));
		result.append(" ");
		if (stateLabelLast != null) {
			result.append(stateLabelLast).append(" -> ");
		}
		result.append(stateLabel);
		return result.toString();
	}
}
