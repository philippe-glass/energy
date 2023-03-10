package com.sapereapi.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class InitializationForm {
	private String scenario;
	private Integer initialStateId;
	private String initialStateVariable;
	private Integer shiftHourOfDay;
	private Integer shiftDayOfMonth;
	private Integer shiftMonth;
	private Double maxTotalPower;
	private boolean disableSupervision = false;

	public InitializationForm() {
		super();
	}

	public InitializationForm(String scenario, Double maxTotalPower,
			Map<Integer, Integer> datetimeConstraints) {
		super();
		this.scenario = scenario;
		this.maxTotalPower = maxTotalPower;
		this.disableSupervision = false;
		setTimeConstraints(datetimeConstraints);
	}

	public void setTimeConstraints(Map<Integer, Integer> datetimeShifts) {
		this.shiftHourOfDay = null;
		if (datetimeShifts.containsKey(Calendar.HOUR_OF_DAY)) {
			this.shiftHourOfDay = datetimeShifts.get(Calendar.HOUR_OF_DAY);
		}
		this.shiftDayOfMonth = null;
		if (datetimeShifts.containsKey(Calendar.DAY_OF_MONTH)) {
			this.shiftDayOfMonth = datetimeShifts.get(Calendar.DAY_OF_MONTH);
		}
		this.shiftMonth = null;
		if (datetimeShifts.containsKey(Calendar.MONTH)) {
			this.shiftMonth = datetimeShifts.get(Calendar.MONTH);
		}
	}

	public Map<Integer, Integer> generateDatetimeShifts() {
		Map<Integer, Integer> datetimeShifts = new HashMap<>();
		if (shiftHourOfDay != null) {
			datetimeShifts.put(Calendar.HOUR_OF_DAY, shiftHourOfDay);
		}
		if (shiftDayOfMonth != null) {
			datetimeShifts.put(Calendar.DAY_OF_MONTH, shiftDayOfMonth);
		}
		if (shiftMonth != null) {
			datetimeShifts.put(Calendar.MONTH, shiftMonth);
		}
		return datetimeShifts;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public Integer getInitialStateId() {
		return initialStateId;
	}

	public void setInitialStateId(Integer initialStateId) {
		this.initialStateId = initialStateId;
	}

	public String getInitialStateVariable() {
		return initialStateVariable;
	}

	public void setInitialStateVariable(String initialStateVariable) {
		this.initialStateVariable = initialStateVariable;
	}

	public Boolean getDisableSupervision() {
		return disableSupervision;
	}

	public void setDisableSupervision(Boolean disableSupervision) {
		this.disableSupervision = disableSupervision;
	}

	public void setInitialState(String _initialStateVariable, Integer _initialStateId) {
		this.initialStateVariable = _initialStateVariable;
		this.initialStateId = _initialStateId;
		this.disableSupervision = true;
	}

	public Double getMaxTotalPower() {
		return maxTotalPower;
	}

	public void setMaxTotalPower(Double maxTotalPower) {
		this.maxTotalPower = maxTotalPower;
	}

	public Integer getShiftHourOfDay() {
		return shiftHourOfDay;
	}

	public void setShiftHourOfDay(Integer shiftHourOfDay) {
		this.shiftHourOfDay = shiftHourOfDay;
	}

	public Integer getShiftDayOfMonth() {
		return shiftDayOfMonth;
	}

	public void setShiftDayOfMonth(Integer shiftDayOfMonth) {
		this.shiftDayOfMonth = shiftDayOfMonth;
	}

	public Integer getShiftMonth() {
		return shiftMonth;
	}

	public void setShiftMonth(Integer shiftMonth) {
		this.shiftMonth = shiftMonth;
	}

}
