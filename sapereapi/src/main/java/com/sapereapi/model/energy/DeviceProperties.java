package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PhaseNumber;
import com.sapereapi.model.referential.PriorityLevel;

public class DeviceProperties implements Cloneable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String name;
	protected PriorityLevel priorityLevel;
	protected DeviceCategory category;
	protected EnvironmentalImpact environmentalImpact;
	protected String location;
	protected String electricalPanel;
	protected String sensorNumber;
	protected Set<PhaseNumber> phases;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PriorityLevel getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(PriorityLevel priorityLvel) {
		this.priorityLevel = priorityLvel;
	}

	public boolean isProducer() {
		if(category != null) {
			return category.isProducer();
		}
		return false;
	}

	public boolean isConsumer() {
		if (category != null) {
			return category.isConsumer();
		}
		return false;
	}

	public DeviceCategory getCategory() {
		return category;
	}

	public void setCategory(DeviceCategory category) {
		this.category = category;
	}

	public String getSensorNumber() {
		return sensorNumber;
	}

	public void setSensorNumber(String sensorNumber) {
		this.sensorNumber = sensorNumber;
	}

	public Set<PhaseNumber> getPhases() {
		return phases;
	}

	public void setPhases(Set<PhaseNumber> phases) {
		this.phases = phases;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getElectricalPanel() {
		return electricalPanel;
	}

	public void setElectricalPanel(String electricalPanel) {
		this.electricalPanel = electricalPanel;
	}

	public EnvironmentalImpact getEnvironmentalImpact() {
		return environmentalImpact;
	}

	public void setEnvironmentalImpact(EnvironmentalImpact environmentalImpact) {
		this.environmentalImpact = environmentalImpact;
	}

	public int getEnvironmentalImpactLevel() {
		if (environmentalImpact == null) {
			return 9999;
		}
		return environmentalImpact.getLevel();
	}

	public void addPhase(PhaseNumber phaseNumber) {
		if (phaseNumber != null) {
			this.phases.add(phaseNumber);
		}
	}

	public void setThreePhases() {
		for (PhaseNumber phaseNumber : PhaseNumber.values()) {
			addPhase(phaseNumber);
		}
	}

	public boolean hasPhase(PhaseNumber phaseNumber) {
		return this.phases.contains(phaseNumber);
	}

	public boolean isThreePhases() {
		return this.phases.size() == PhaseNumber.values().length;
	}

	public boolean isSinglePhases() {
		return this.phases.size() == 1;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(name);
		result.append("(").append(category).append(")");
		return result.toString();
	}

	public String toString2() {
		StringBuffer result = new StringBuffer();
		result.append("  ").append(name);
		// result.append(" (").append(category).append(")");
		return result.toString();
	}

	public DeviceProperties() {
		super();
	}

	public DeviceProperties(String name, DeviceCategory category, EnvironmentalImpact _environmentalImpact
			//,boolean _isProducer
			) {
		super();
		this.name = name;
		this.category = category;
		this.environmentalImpact = _environmentalImpact;
		//this.isProducer = _isProducer;
		this.priorityLevel = PriorityLevel.UNKNOWN;
		this.phases = new HashSet<PhaseNumber>();
		if(category == null) {
			SapereLogger.getInstance()
			.error("DeviceProperties Constructor : category is null for device name " + name);
		}
	}

	@Override
	public DeviceProperties clone() {
		return copy(true);
	}

	//@Override
	public DeviceProperties copyForLSA() {
		return copy(false);
	}

	public DeviceProperties copy(boolean addIds) {
		DeviceProperties result = new DeviceProperties(name, category, environmentalImpact);
		result.setPriorityLevel(priorityLevel);
		//result.setProducer(isProducer);
		result.setLocation(location);
		result.setPhases(phases);
		result.setElectricalPanel(electricalPanel);
		// result.setRunningAgentName(runningAgentName.clone);
		return result;
	}

	public boolean hasCategoryExternalSupply() {
		return DeviceCategory.EXTERNAL_ENG.equals(category);
	}
}
