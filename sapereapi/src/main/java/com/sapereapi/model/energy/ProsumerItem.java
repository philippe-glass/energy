package com.sapereapi.model.energy;

import java.io.Serializable;

import com.sapereapi.model.referential.ProsumerRole;

import eu.sapere.middleware.node.NodeLocation;

public class ProsumerItem implements Serializable {
	private static final long serialVersionUID = 1L;
	String agentName;
	ProsumerRole role;
	PowerSlot powerSlot;
	NodeLocation location;

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public PowerSlot getPowerSlot() {
		return powerSlot;
	}

	public void setPowerSlot(PowerSlot powerSlot) {
		this.powerSlot = powerSlot;
	}

	public NodeLocation getLocation() {
		return location;
	}

	public void setLocation(NodeLocation location) {
		this.location = location;
	}

	public ProsumerRole getRole() {
		return role;
	}

	public void setRole(ProsumerRole role) {
		this.role = role;
	}

	public ProsumerItem(String agentName, ProsumerRole role, PowerSlot powerSlot, NodeLocation location) {
		super();
		this.agentName = agentName;
		this.role = role;
		this.powerSlot = powerSlot;
		this.location = location;
	}

	public ProsumerItem clone() {
		ProsumerItem result = new ProsumerItem(agentName, role, powerSlot, location);
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(agentName).append(" (").append(role).append(")");
		if (powerSlot != null) {
			result.append(", power : ").append(powerSlot);
		}
		if (location != null) {
			result.append(", node : ").append(location.getName());
		}
		return result.toString();
	}
}
