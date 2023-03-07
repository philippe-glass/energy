package eu.sapere.middleware.lsa;

import java.io.Serializable;

public class Property implements Serializable {

	private static final long serialVersionUID = -5065528085751577453L;

	private String query;
	private String bond;
	private Object value;
	private String name;
	private String state;
	private String ip;
	private Boolean chosen;

	public Property(String name, Object value, String query, String bond, String state, String ip, Boolean chosen) {
		this.name = name;
		this.value = value;
		this.query = query;
		this.bond = bond;
		this.state = state;
		this.ip = ip;
		this.chosen = chosen;
	}

	public Property(String name, Object value) {
		this.name = name;
		this.value = value;
		this.query = "";
		this.bond = "";
		this.state = "";
		this.ip = "";
		this.chosen = false;
	}

	public Boolean getChosen() {
		return chosen;
	}

	public void setChosen(Boolean chosen) {
		this.chosen = chosen;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "<"+name + ":" + value+">" + " #B:" + bond + " #Q:" + query + " #S=" + state + " #" + chosen; // +"-"+ip;

	}
}
