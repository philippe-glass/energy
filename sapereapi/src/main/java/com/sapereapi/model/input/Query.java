package com.sapereapi.model.input;


public class Query {
	private String[] prop;
	private String[] waiting;
	private String[] values;
	private String name;

	public Query(String agentName, String[] subdescription, String[] propertiesName, String[] values) {
		super();
		this.name = agentName;
		this.prop = propertiesName;
		this.waiting = subdescription;
		this.values = values;
	}

	public Query() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getProp() {
		return prop;
	}

	public void setProp(String[] prop) {
		this.prop = prop;
	}

	public String[] getWaiting() {
		return waiting;
	}

	public void setWaiting(String[] waiting) {
		this.waiting = waiting;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		this.values = values;
	}

}
