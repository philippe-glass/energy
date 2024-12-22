package eu.sapere.middleware.lsa;

import java.io.Serializable;
import java.util.Map;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.node.NodeLocation;

public class Property implements Serializable {

	private static final long serialVersionUID = -5065528085751577453L;

	private String query;
	private String bond;
	private Object value;
	private String name;
	private String state;
	private String ip;
	private Boolean chosen;
	private Object aggregatedValue;

	public Property(String name, Object value, String query, String bond, String state, String ip, Boolean chosen) {
		this.name = name;
		this.value = value;
		this.query = query;
		this.bond = bond;
		this.state = state;
		this.ip = ip;
		this.chosen = chosen;
		this.aggregatedValue = null;
	}

	public Property(String name, Object value) {
		this.name = name;
		this.value = value;
		this.query = "";
		this.bond = "";
		this.state = "";
		this.ip = "";
		this.chosen = false;
		this.aggregatedValue = null;
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

	public Object getAggregatedValue() {
		return aggregatedValue;
	}

	public void setAggregatedValue(Object aggregatedValue) {
		this.aggregatedValue = aggregatedValue;
	}

	public Property copyForLSA() {
		Object valueCopy = value;
		if(value instanceof IPropertyObject) {
			IPropertyObject valueToCopy = (IPropertyObject) value;
			try {
				valueCopy = valueToCopy.copyForLSA(MiddlewareLogger.getInstance());
			} catch (Throwable e) {
				MiddlewareLogger.getInstance().error(e);
			}
		}
		Property result = new Property(name, valueCopy, query, bond, state, ip, chosen);
		return result;
	}

	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation) {
		if(value instanceof IPropertyObject) {
			IPropertyObject propValue = (IPropertyObject) value;
			propValue.completeInvolvedLocations(bondedLsa, mapNodeLocation, MiddlewareLogger.getInstance());
		}
	}

	@Override
	public String toString() {
		String sAggregated = "";
		if(aggregatedValue != null) {
			sAggregated = " #Aggregated:" + aggregatedValue;
		}
		return "<"+name + ":" + value+">" + " #B:" + bond + " #Q:" + query + " #S=" + state + " #" + chosen + sAggregated; // +"-"+ip;
	}
}
