package com.sapereapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;

public class LsaForm {

	private String agentName;
	protected List<String> subDescription;
	private List<PropertyForm> propertyList;
	private Map<String, String> syntheticProperties;

	public LsaForm(Lsa lsa) {
		super();
		this.agentName = lsa.getAgentName();
		this.subDescription = lsa.getSubDescription();
		this.propertyList = new ArrayList<PropertyForm>();
		for (Property prop : lsa.getProperties()) {
			propertyList.add(new PropertyForm(prop));
		}
		this.syntheticProperties = new HashMap<String, String>();
		for (SyntheticPropertyName pname : SyntheticPropertyName.values()) {
			Object prop = lsa.getSyntheticProperty(pname);
			if (prop != null) {
				this.syntheticProperties.put(pname.toString(), prop.toString());
			}
		}
	}

	public LsaForm() {
		super();
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public List<PropertyForm> getPropertyList() {
		return propertyList;
	}

	public void setPropertyList(List<PropertyForm> propertyList) {
		this.propertyList = propertyList;
	}

	public List<String> getSubDescription() {
		return subDescription;
	}

	public void setSubDescription(List<String> subDescription) {
		this.subDescription = subDescription;
	}

	public Map<String, String> getSyntheticProperties() {
		return syntheticProperties;
	}

	public void setSyntheticProperties(Map<String, String> syntheticProperties) {
		this.syntheticProperties = syntheticProperties;
	}

}
