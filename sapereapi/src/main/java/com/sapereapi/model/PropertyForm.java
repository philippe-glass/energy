package com.sapereapi.model;

import eu.sapere.middleware.lsa.Property;

public class PropertyForm {
	private String query;
	private String bond;
	private String value;
	private String name;
	private String state;
	private String ip;
	private Boolean chosen;

	public PropertyForm() {
		super();
	}

	public PropertyForm(Property property) {
		super();
		this.query = property.getQuery();
		this.bond = property.getBond();
		this.value = "" + property.getValue();
		this.name = property.getName();
		this.state = property.getState();
		this.ip = property.getIp();
		this.chosen = property.getChosen();
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getBond() {
		return bond;
	}

	public void setBond(String bond) {
		this.bond = bond;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Boolean getChosen() {
		return chosen;
	}

	public void setChosen(Boolean chosen) {
		this.chosen = chosen;
	}

}
