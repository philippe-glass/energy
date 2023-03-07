package com.sapereapi.model.energy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class NodeTotal implements Cloneable, Serializable{
	private static final long serialVersionUID = 15078L;
	protected Long id;
	protected Long idLast;
	protected Long idNext;
	protected String sessionId;
	protected String location;
	protected int distance;
	protected Double requested;
	protected Double produced;
	protected Double consumed;
	protected Double consumedLocally;
	protected Double provided;
	protected Double providedLocally;
	protected Double available;
	protected Double missing;
	protected Double providedMargin;
	protected Double sentOffersTotal;
	protected Double receivedOffersTotal;
	protected Date date;
	protected Long timeShiftMS;
	protected String agentName;
	protected List<EnergyEvent> linkedEvents;
	protected Map<String, Double> receivedOffersRepartition;
	protected Map<String, Double> sentOffersRepartition;
	private double minRequestMissingRequest;
	protected Long maxWarningDuration;
	protected String maxWarningConsumer;

	//protected Map<String, Field> variableFields = new HashMap<String, Field>();

	public NodeTotal() {
		super();
		reset();
		linkedEvents = new ArrayList<EnergyEvent>();
		/*
		variableFields.clear();
		for(Field field : this.getClass().getDeclaredFields()) {
			if(Float.class.equals(field.getType()) || Double.class.equals(field.getType())) {
				variableFields.put(field.getName(), field);
			}
		}*/
	}

	public void reset() {
		requested = Double.valueOf(0);
		produced = Double.valueOf(0);
		consumed = Double.valueOf(0);
		consumedLocally = Double.valueOf(0);
		available = Double.valueOf(0);
		missing = Double.valueOf(0);
		provided = Double.valueOf(0);
		providedLocally = Double.valueOf(0);
		sentOffersTotal = Double.valueOf(0);
		receivedOffersTotal = Double.valueOf(0);
		maxWarningDuration = Long.valueOf(0);
	}

	public boolean hasActivity() {
		return requested>0 || produced>0;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getRequested() {
		return requested;
	}

	public Double getProduced() {
		return produced;
	}

	public Double getConsumed() {
		return consumed;
	}

	public Double getConsumedLocally() {
		return consumedLocally;
	}

	public Double getProvidedLocally() {
		return providedLocally;
	}

	public void setConsumedLocally(Double consumedLocally) {
		this.consumedLocally = consumedLocally;
	}

	public void setProvidedLocally(Double providedLocally) {
		this.providedLocally = providedLocally;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<EnergyEvent> getLinkedEvents() {
		return linkedEvents;
	}

	public void setLinkedEvents(List<EnergyEvent> linkedEvents) {
		this.linkedEvents = linkedEvents;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public void setRequested(Double requested) {
		this.requested = requested;
	}

	public void setProduced(Double produced) {
		this.produced = produced;
	}

	public void setConsumed(Double consumed) {
		this.consumed = consumed;
	}

	public Double getAvailable() {
		return available;
	}

	public void setAvailable(Double available) {
		this.available = available;
	}

	public Double getMissing() {
		return missing;
	}

	public void setMissing(Double missing) {
		this.missing = missing;
	}

	public Double getProvided() {
		return provided;
	}

	public void setProvided(Double provided) {
		this.provided = provided;
	}

	public Double getProvidedMargin() {
		return providedMargin;
	}

	public void setProvidedMargin(Double _providedMargin) {
		this.providedMargin = _providedMargin;
	}

	public Double getSentOffersTotal() {
		return sentOffersTotal;
	}

	public void setSentOffersTotal(Double sentOffersTotal) {
		this.sentOffersTotal = sentOffersTotal;
	}

	public Double getReceivedOffersTotal() {
		return receivedOffersTotal;
	}

	public void setReceivedOffersTotal(Double receivedOffersTotal) {
		this.receivedOffersTotal = receivedOffersTotal;
	}

	public Map<String, Double> getReceivedOffersRepartition() {
		return receivedOffersRepartition;
	}

	public void setReceivedOffersRepartition(Map<String, Double> receivedOffersRepartition) {
		this.receivedOffersRepartition = receivedOffersRepartition;
	}

	public Map<String, Double> getSentOffersRepartition() {
		return sentOffersRepartition;
	}

	public void setSentOffersRepartition(Map<String, Double> sentOffersRepartition) {
		this.sentOffersRepartition = sentOffersRepartition;
	}

	public boolean hasMissingRequestWarning() {
		return minRequestMissingRequest > 0 && minRequestMissingRequest < available;
	}

	public double getMinRequestMissing() {
		return minRequestMissingRequest;
	}

	public void setMinRequestMissing(double _minRequestMissingRequest) {
		this.minRequestMissingRequest = _minRequestMissingRequest;
	}

	public Long getIdLast() {
		return idLast;
	}

	public void setIdLast(Long idLast) {
		this.idLast = idLast;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Long getIdNext() {
		return idNext;
	}

	public void setIdNext(Long idNext) {
		this.idNext = idNext;
	}

	public String getLocation() {
		return location;
	}

	public int getDistance() {
		return distance;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public Long getMaxWarningDuration() {
		return maxWarningDuration;
	}

	public void setMaxWarningDuration(Long maxWarningDuration) {
		this.maxWarningDuration = maxWarningDuration;
	}

	public String getMaxWarningConsumer() {
		return maxWarningConsumer;
	}

	public void setMaxWarningConsumer(String maxWarningConsumer) {
		this.maxWarningConsumer = maxWarningConsumer;
	}

	public Long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public Double getVariablePower(String fieldName) {
		try {
			Field field = this.getClass().getDeclaredField(fieldName);
			//Field field = variableFields.get(fieldName);
			Object value = field.get(this);
			double dvalue = SapereUtil.getDoubleValue(value);
			return Math.max(0,dvalue);
			/*
			if(value instanceof Float) {
				Float vPower = (Float) value;
				if(vPower<0) {
					vPower = Float.valueOf(0);
				}
				return vPower.doubleValue();
			} else if(value instanceof Double) {
				Double vPower = (Double) value;
				if(vPower<0) {
					vPower = Double.valueOf(0);
				}
				return vPower;
			}*/
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		} 
		return null;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("time ").append(UtilDates.format_time.format(this.date)).append(" requested:")
				.append(UtilDates.df.format(this.requested))
				.append(" produced:").append(UtilDates.df.format(this.produced))
				.append(" provided:").append(UtilDates.df.format(this.provided))
				.append(" consumed:").append(UtilDates.df.format(this.consumed))
				.append(" available:").append(UtilDates.df.format(this.available))
				.append(" missing:").append(UtilDates.df.format(this.missing));
		return result.toString();
	}

	@Override
	public NodeTotal clone() {
		NodeTotal result = new NodeTotal();
		result.setTimeShiftMS(timeShiftMS);
		result.setId(id);
		result.setDate(date);
		result.setAgentName(agentName);
		result.setIdLast(idLast);
		result.setIdNext(idNext);
		result.setSessionId(sessionId);
		result.setLocation(location);
		result.setDistance(distance);
		result.setRequested(requested);
		result.setProduced(produced);
		result.setConsumed(consumed);
		result.setConsumedLocally(consumedLocally);
		result.setProvided(provided);
		result.setProvidedLocally(providedLocally);
		result.setProvidedMargin(providedMargin);
		result.setAvailable(available);
		result.setMissing(missing);
		result.setSentOffersTotal(sentOffersTotal);
		result.setReceivedOffersTotal(receivedOffersTotal);
		result.setMaxWarningDuration(maxWarningDuration);
		result.setMaxWarningConsumer(maxWarningConsumer);
		result.setTimeShiftMS(timeShiftMS);
		if(receivedOffersRepartition!=null) {
			Map<String, Double> _receivedOffersRepartition = new HashMap<>();
			_receivedOffersRepartition.putAll(receivedOffersRepartition);
			result.setReceivedOffersRepartition(_receivedOffersRepartition);
		}
		if(sentOffersRepartition!=null) {
			Map<String, Double> _sentOffersRepartition = new HashMap<>(); ;
			_sentOffersRepartition.putAll(sentOffersRepartition);
			result.setSentOffersRepartition(_sentOffersRepartition);
		}
		if(linkedEvents!=null) {
			List<EnergyEvent> _linkedEvent = new ArrayList<>();
			_linkedEvent.addAll(linkedEvents);
			result.setLinkedEvents(_linkedEvent);
		}
		return result;
	}
}
