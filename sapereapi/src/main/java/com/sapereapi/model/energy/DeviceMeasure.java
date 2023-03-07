package com.sapereapi.model.energy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.referential.PhaseNumber;
import com.sapereapi.util.UtilDates;

public class DeviceMeasure {
	private Date datetime;
	private String deviceName;
	private Map<String, Double> map_power_p;
	private Map<String, Double> map_power_q;
	private Map<String, Double> map_power_s;

	public Date getDatetime() {
		return datetime;
	}

	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public Map<String, Double> getMap_power_p() {
		return map_power_p;
	}

	public void setMap_power_p(Map<String, Double> map_power_p) {
		this.map_power_p = map_power_p;
	}

	public Map<String, Double> getMap_power_q() {
		return map_power_q;
	}

	public void setMap_power_q(Map<String, Double> map_power_q) {
		this.map_power_q = map_power_q;
	}

	public Map<String, Double> getMap_power_s() {
		return map_power_s;
	}

	public void setMap_power_s(Map<String, Double> map_power_s) {
		this.map_power_s = map_power_s;
	}

	public void addPhaseMeasures(PhaseNumber _phaseNumber, Double power_p, Double power_q, Double power_s) {
		if (_phaseNumber != null) {
			map_power_p.put(_phaseNumber.getLabel(), power_p);
			map_power_q.put(_phaseNumber.getLabel(), power_q);
			map_power_s.put(_phaseNumber.getLabel(), power_s);
		}
	}

	public DeviceMeasure() {
		super();
		map_power_p = new HashMap<String, Double>();
		map_power_q = new HashMap<String, Double>();
		map_power_s = new HashMap<String, Double>();
	}

	public DeviceMeasure(Date datetime, String deviceName, PhaseNumber _phaseNumber, Double power_p, Double power_q,
			Double power_s) {
		super();
		map_power_p = new HashMap<String, Double>();
		map_power_q = new HashMap<String, Double>();
		map_power_s = new HashMap<String, Double>();
		this.datetime = datetime;
		this.deviceName = deviceName;
		if (_phaseNumber != null) {
			map_power_p.put(_phaseNumber.getLabel(), power_p);
			map_power_q.put(_phaseNumber.getLabel(), power_q);
			map_power_s.put(_phaseNumber.getLabel(), power_s);
		}
	}

/**
 * The principle of energy conservation is applied: the total active power is equal to the sum
 *  of the active powers of the three elementary receivers P = P1 + P2 + P3.
 * @return Active power of the three-phase receiver
 */
	public Double computeTotalPower_p() {
		double result = 0;
		for (Double power : map_power_p.values()) {
			result += power;
		}
		return result;
	}

/**
 * The total reactive power is equal to the sum of the reactive powers of the three elementary receivers.
    Q = Q1 + Q2 + Q3
    With: Q : reactive power of the three-phase receiver (in VARS)
    Q1, Q2 and Q3: reactive power of the elementary receivers (in VARS)
 * @return Reactive power of the three-phase receiver (in VARS)
 */
	public Double computeTotalPower_q() {
		double result = 0;
		for (Double power : map_power_q.values()) {
			result += power;
		}
		return result;
	}

/**
 *  The power is never obtained by adding the apparent powers, but by using the formula: S=sqrt(P^2 + Q^2)
 * @return Apparent power of the three-phase receiver (in VA).
 */
	public Double computeTotalPower_s() {
		double total_power_p =  computeTotalPower_p();
		double total_power_q = computeTotalPower_q();
		double result = Math.sqrt(Math.pow(total_power_p, 2)  + Math.pow(total_power_q, 2));
		return result;
	}

	@Override
	public DeviceMeasure clone() {
		DeviceMeasure result = new DeviceMeasure(this.datetime, this.deviceName,  null, null, null, null);
		for(String phase : map_power_p.keySet()) {
			PhaseNumber phaseNumber = PhaseNumber.getByLabel(phase);
			result.addPhaseMeasures(phaseNumber, map_power_p.get(phase), map_power_q.get(phase), map_power_s.get(phase));
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.deviceName).append(" at ").append(UtilDates.format_time.format(this.datetime));
		result.append(" p : ");
		for(String key : map_power_p.keySet()) {
			result.append(key).append(":").append(map_power_p.get(key));
		}
		return result.toString();
	}
}
