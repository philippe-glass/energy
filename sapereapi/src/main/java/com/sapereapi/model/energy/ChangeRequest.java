package com.sapereapi.model.energy;

import java.io.Serializable;

import com.sapereapi.model.referential.ProsumerRole;

public class ChangeRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private EnergyFlow supplyOrRequest;
	private ProsumerRole prosumerRole;
	private String agentName;

	public EnergySupply getSupply() {
		if(supplyOrRequest instanceof EnergySupply) {
			return (EnergySupply) supplyOrRequest;
		}
		return supplyOrRequest.generateSupply();
	}

	public EnergyRequest getRequest() {
		if(supplyOrRequest instanceof EnergyRequest) {
			return (EnergyRequest) supplyOrRequest;
		}
		return supplyOrRequest.generateRequest();
	}

	public void setSupplyOrRequest(EnergySupply supply) {
		this.supplyOrRequest = supply;
	}

	public ProsumerRole getProsumerRole() {
		return prosumerRole;
	}

	public void setProsumerRole(ProsumerRole prosumerRole) {
		this.prosumerRole = prosumerRole;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public ChangeRequest(String agentName, EnergyFlow supply, ProsumerRole prosumerRole) {
		super();
		this.agentName = agentName;
		this.supplyOrRequest = supply;
		this.prosumerRole = prosumerRole;
	}

	@Override
	public ChangeRequest clone() {
		ChangeRequest clone = new ChangeRequest(agentName, supplyOrRequest.clone(), prosumerRole);
		return clone;
	}
}
