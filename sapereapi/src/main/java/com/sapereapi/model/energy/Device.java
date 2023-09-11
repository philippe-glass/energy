package com.sapereapi.model.energy;

import java.util.Set;

import com.sapereapi.model.Sapere;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PhaseNumber;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class Device {
	private Long id;
	DeviceProperties properties;
	private double powerMin;
	private double powerMax;
	private double currentPower;
	private double averageDurationMinutes;
	private String status;
	private String runningAgentName;

	public final static String STATUS_SLEEPING = "Sleeping";
	public final static String STATUS_RUNNING = "Running";

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return properties.getName();
	}

	public double getPowerMin() {
		return powerMin;
	}

	public void setPowerMin(double powerMin) {
		this.powerMin = powerMin;
	}

	public double getPowerMax() {
		return powerMax;
	}

	public void setPowerMax(double powerMax) {
		this.powerMax = powerMax;
	}

	public double getAverageDurationMinutes() {
		return averageDurationMinutes;
	}

	public void setAverageDurationMinutes(double averageDurationMinutes) {
		this.averageDurationMinutes = averageDurationMinutes;
	}

	public DeviceProperties getProperties() {
		return properties;
	}

	public void setProperties(DeviceProperties properties) {
		this.properties = properties;
	}

	public int getPriorityLevel() {
		return properties.getPriorityLevel();
	}

	public boolean isProducer() {
		return properties.isProducer();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public DeviceCategory getCategory() {
		return properties.getCategory();
	}

	public String getRunningAgentName() {
		return runningAgentName;
	}

	public void setRunningAgentName(String runningAgentName) {
		this.runningAgentName = runningAgentName;
	}

	public double getCurrentPower() {
		return currentPower;
	}

	public void setCurrentPower(double currentPower) {
		this.currentPower = currentPower;
	}

	public String getSensorNumber() {
		return properties.getSensorNumber();
	}

	public Set<PhaseNumber> getPhases() {
		return properties.getPhases();
	}

	public String getLocation() {
		return properties.getLocation();
	}

	public String getElectricalPanel() {
		return properties.getElectricalPanel();
	}

	public EnvironmentalImpact getEnvironmentalImpact() {
		return properties.getEnvironmentalImpact();
	}

	public boolean hasPhase(PhaseNumber phaseNumber) {
		return properties.hasPhase(phaseNumber);
	}

	public boolean isThreePhases() {
		return this.properties.isThreePhases();
	}

	public boolean isSinglePhases() {
		return properties.isSinglePhases();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(properties.getName());
		result.append("(").append(properties.getCategory()).append(")");
		result.append(" W: ").append(UtilDates.df3.format(powerMin)).append("-").append(UtilDates.df3.format(powerMax));
		if (currentPower > 0) {
			result.append(" current : ").append(SapereUtil.round(currentPower, Sapere.NB_DEC_POWER));
		}
		return result.toString();
	}

	public String toString2() {
		StringBuffer result = new StringBuffer();
		result.append(" W: ").append(UtilDates.df3.format(powerMin)).append("-").append(UtilDates.df3.format(powerMax));
		if (currentPower > 0) {
			result.append(" (current = ").append(UtilDates.df3.format(currentPower)).append(")  ");
		}
		result.append("  ").append(properties.getName());
		// result.append(" (").append(category).append(")");
		return result.toString();
	}

	public Device() {
		super();
	}

	public Device(Long id, DeviceProperties _properties, double powerMin, double powerMax,
			double averageDurationMinutes) {
		super();
		this.id = id;
		this.properties = _properties;
		this.powerMin = powerMin;
		this.powerMax = powerMax;
		this.averageDurationMinutes = averageDurationMinutes;
		this.status = STATUS_SLEEPING;
		this.runningAgentName = "";
	}

	@Override
	public Device clone() {
		DeviceProperties cloneProperties = properties.clone();
		Device result = new Device(id, cloneProperties, powerMin, powerMax, averageDurationMinutes);
		result.setCurrentPower(currentPower);
		// result.setRunningAgentName(runningAgentName.clone);
		return result;
	}

}
