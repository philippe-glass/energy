package com.sapereapi.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeManager;

public class NodeContext implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 879794;
	protected Long id = null;
	protected String location;
	protected String scenario;
	protected long timeShiftMS = 0;
	protected String sessionId = null;
	protected Map<Integer, Integer> datetimeShifts = null;
	protected Double maxTotalPower = null;
	protected String variables[] = { "requested", "produced", "consumed", "provided", "available", "missing" };

	public NodeContext(Long _id, String location, String scenario, Map<Integer, Integer> _datetimeShifts,
			Double _maxTotalPower, String aSessionId, String[] _variables) {
		super();
		this.id = _id;
		this.location = location;
		this.scenario = scenario;
		this.datetimeShifts = _datetimeShifts;
		this.timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		this.maxTotalPower = _maxTotalPower;
		this.sessionId = aSessionId;
		this.variables = _variables;
	}

	public NodeContext() {
		super();
	}

	public boolean isLocal() {
		return NodeManager.getLocation().equals(location);
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public Double getMaxTotalPower() {
		return maxTotalPower;
	}

	public void setMaxTotalPower(Double maxTotalPower) {
		this.maxTotalPower = maxTotalPower;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<Integer, Integer> getDatetimeShifts() {
		return datetimeShifts;
	}

	public void setDatetimeShifts(Map<Integer, Integer> datetimeShifts) {
		this.datetimeShifts = datetimeShifts;
	}

	public String[] getVariables() {
		return variables;
	}

	public void setVariables(String[] variables) {
		this.variables = variables;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public NodeContext clone() {
		NodeContext result = new NodeContext(id, location, scenario, datetimeShifts, maxTotalPower, sessionId,
				variables);
		return result;
	}

	public boolean hasDatetimeShift() {
		return (datetimeShifts.size() > 0);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}
}
