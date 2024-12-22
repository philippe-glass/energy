package com.sapereapi.model.learning.lstm.request;

import java.util.Date;
import java.util.List;

import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.VariableState;

public class LSTMPredictionRequest {
	private Date initialDate;
	private Double initialValue;
	private VariableState initialState;
	private List<Integer> listHorizons;
	private List<Date> targetDates;
	private List<List<Double>> listX;
	private List<List<Double>> listTrue;
	private List<List<Date>> listDatesX;
	private String nodeName;
	private PredictionScope scope;
	private String variable;

	public Date getInitialDate() {
		return initialDate;
	}

	public void setInitialDate(Date initialDate) {
		this.initialDate = initialDate;
	}

	public Double getInitialValue() {
		return initialValue;
	}

	public void setInitialValue(Double initialValue) {
		this.initialValue = initialValue;
	}

	public VariableState getInitialState() {
		return initialState;
	}

	public void setInitialState(VariableState initialState) {
		this.initialState = initialState;
	}

	public List<Date> getTargetDates() {
		return targetDates;
	}

	public void setTargetDates(List<Date> targetDates) {
		this.targetDates = targetDates;
	}

	public List<List<Date>> getListDatesX() {
		return listDatesX;
	}

	public void setListDatesX(List<List<Date>> listDatesX) {
		this.listDatesX = listDatesX;
	}

	public List<List<Double>> getListX() {
		return listX;
	}

	public void setListX(List<List<Double>> listX) {
		this.listX = listX;
	}

	public List<Integer> getListHorizons() {
		return listHorizons;
	}

	public void setListHorizons(List<Integer> listHorizons) {
		this.listHorizons = listHorizons;
	}

	public List<List<Double>> getListTrue() {
		return listTrue;
	}

	public void setListTrue(List<List<Double>> listTrue) {
		this.listTrue = listTrue;
	}

	public PredictionScope getScope() {
		return scope;
	}

	public void setScope(PredictionScope scope) {
		this.scope = scope;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String node) {
		this.nodeName = node;
	}

}
