package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class ProsumerProperties implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	protected String agentName;
	protected NodeLocation location;
	protected int distance;
	protected DeviceProperties deviceProperties;
	protected long timeShiftMS;

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public NodeLocation getLocation() {
		return location;
	}

	public void setLocation(NodeLocation location) {
		this.location = location;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public DeviceProperties getDeviceProperties() {
		return deviceProperties;
	}

	public void setDeviceProperties(DeviceProperties deviceProperties) {
		this.deviceProperties = deviceProperties;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public ProsumerProperties(String agentName, NodeLocation location, int distance, long timeShiftMS,
			DeviceProperties deviceProperties) {
		super();
		this.agentName = agentName;
		this.location = location;
		this.distance = distance;
		this.deviceProperties = deviceProperties;
		this.timeShiftMS = timeShiftMS;
	}

	public boolean isLocal() {
		return (0 == distance);
	}

	public ProsumerProperties copy(boolean addIds) {
		DeviceProperties copyDeviceProperties = null;
		if (deviceProperties != null) {
			copyDeviceProperties = deviceProperties.copy(addIds);
		}
		NodeLocation copyLocation = null;
		if (location != null) {
			copyLocation = location.copy();
		}
		ProsumerProperties copy = new ProsumerProperties(agentName, copyLocation, distance, timeShiftMS,
				copyDeviceProperties);
		return copy;
	}

	@Override
	public ProsumerProperties clone()  {
		return copy(true);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ProsumerProperties) {
			ProsumerProperties other = (ProsumerProperties) obj;
			if (agentName != null) {
				return agentName.equals(other.getAgentName());
			}
		}
		return false;
	}

	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		NodeLocation bondedLsaLocation = bondedLsa.getAgentAuthentication().getNodeLocation();
		if (location.getName().equals(bondedLsaLocation.getName())) {
			distance = bondedLsa.getSourceDistance();
		}
	}

	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<>();
		result.add(location);
		return result;
	}

	public int compareDistance(ProsumerProperties other) {
		return this.distance - other.getDistance();
	}

	public int compareEnvironmentImpact(ProsumerProperties other) {
		int impactLevel = this.deviceProperties.getEnvironmentalImpactLevel();
		int otherImpactLevel = other.getDeviceProperties().getEnvironmentalImpactLevel();
		return impactLevel - otherImpactLevel;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(agentName).append(" @node ").append(location).append(" : ");
		result.append(deviceProperties);
		return result.toString();
	}

	public boolean hasCategoryExternalSupply() {
		if (deviceProperties != null) {
			return deviceProperties.hasCategoryExternalSupply();
		}
		return false;
	}
}
