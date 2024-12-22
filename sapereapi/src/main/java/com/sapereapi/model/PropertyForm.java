package com.sapereapi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.lsa.Property;

public class PropertyForm {
	private String query;
	private String bond;
	private String value;
	private String aggregatedValue;
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
		if(property.getAggregatedValue() == null) {
			this.aggregatedValue = "";
		} else {
			this.aggregatedValue = ""+property.getAggregatedValue();
		}
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

	public String getAggregatedValue() {
		return aggregatedValue;
	}

	public void setAggregatedValue(String aggregatedValue) {
		this.aggregatedValue = aggregatedValue;
	}

	public List<String> getValueLines() {
		if(value==null) {
			return new ArrayList<String>();
		}
		List<String> result = new ArrayList<String>(Arrays.asList(value.split(SapereUtil.CR)));
		return result;
	}

	public List<String> getAggregatedValueLines() {
		if (aggregatedValue == null) {
			return new ArrayList<String>();
		}
		return new ArrayList<String>(Arrays.asList(aggregatedValue.split(SapereUtil.CR)));
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
