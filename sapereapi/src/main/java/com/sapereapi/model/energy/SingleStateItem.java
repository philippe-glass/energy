package com.sapereapi.model.energy;

import java.util.Date;

public class SingleStateItem {
	Date date;
	String variable;
	Double value;
	Integer stateId;

	public SingleStateItem(Date date, String variable, Double value, Integer stateId) {
		super();
		this.date = date;
		this.variable = variable;
		this.value = value;
		this.stateId = stateId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Integer getStateId() {
		return stateId;
	}

	public void setStateId(Integer stateId) {
		this.stateId = stateId;
	}

}
