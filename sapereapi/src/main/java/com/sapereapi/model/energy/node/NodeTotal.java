package com.sapereapi.model.energy.node;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.input.HistoryInitializationForm;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.IPropertyObject;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.AbstractAggregatable;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class NodeTotal extends AbstractAggregatable implements Cloneable, IAggregateable {
	private static final long serialVersionUID = 15078L;
	protected Long id;
	protected Long idLast;
	protected Long idNext;
	protected String sessionId;
	protected NodeLocation nodeLocation;
	protected int distance;
	protected Double requested;
	protected Double produced;
	protected Double consumed;
	protected Double consumedMargin;
	protected Double consumedLocally;
	protected Double provided;
	protected Double providedLocally;
	protected Double available;
	protected Double missing;
	protected Double providedLocallyMargin;
	protected Double providedMargin;
	protected Double sentOffersTotal;
	protected Double receivedOffersTotal;
	protected Double storageUsedForNeed;
	protected Double storageUsedForProd;
	protected Double storedProducersWH;
	protected Double storedConsumersWH;
	protected Date date;
	protected Long timeShiftMS;
	protected List<EnergyEvent> linkedEvents;
	protected Map<String, Double> receivedOffersRepartition;
	protected Map<String, Double> sentOffersRepartition;
	private double minRequestMissingRequest;
	protected Long maxWarningDuration;
	protected String maxWarningConsumer;
	protected boolean isAdditionalRefresh;

	public final static String OP_SUM = "sum";

	//protected Map<String, Field> variableFields = new HashMap<String, Field>();

	public NodeTotal() {
		super();
		reset();
		/*
		variableFields.clear();
		for(Field field : this.getClass().getDeclaredFields()) {
			if(Float.class.equals(field.getType()) || Double.class.equals(field.getType())) {
				variableFields.put(field.getName(), field);
			}
		}*/
	}

	public void reset() {
		requested = 0.0;
		produced = 0.0;
		consumed = 0.0;
		consumedLocally = 0.0;
		available = 0.0;
		missing = 0.0;
		provided = 0.0;
		providedLocally = 0.0;
		providedMargin = 0.0;
		consumedMargin = 0.0;
		sentOffersTotal = 0.0;
		receivedOffersTotal = 0.0;
		storageUsedForNeed = 0.0;
		storageUsedForProd = 0.0;
		storedProducersWH = 0.0;
		storedConsumersWH = 0.0;
		maxWarningDuration = (long) 0;
		linkedEvents = new ArrayList<EnergyEvent>();
		receivedOffersRepartition = new HashMap<String, Double>();
		sentOffersRepartition = new HashMap<String, Double>();
		maxWarningConsumer = null;
		refreshLastUpdate();
	}

	public boolean isEmpty() {
		return requested == 0 && produced == 0 && consumed == 0 && available == 0 && missing == 0 && provided == 0;
	}

	public boolean hasActivity() {
		return requested>0 || produced>0 || storageUsedForNeed > 0 || storageUsedForProd > 0;
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

	public Double getConsumedMargin() {
		return consumedMargin;
	}

	public void setConsumedMargin(Double consumedMargin) {
		this.consumedMargin = consumedMargin;
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

	public Double getStorageUsed() {
		return storageUsedForNeed + storageUsedForProd;
	}

	public Double getStorageUsedForNeed() {
		return storageUsedForNeed;
	}

	public void setStorageUsedForNeed(Double storageUsedForNeed) {
		this.storageUsedForNeed = storageUsedForNeed;
	}

	public Double getStorageUsedForProd() {
		return storageUsedForProd;
	}

	public void setStorageUsedForProd(Double storageUsedForProd) {
		this.storageUsedForProd = storageUsedForProd;
	}

	public Double getStoredProducersWH() {
		return storedProducersWH;
	}

	public void setStoredProducersWH(Double storedProducersWH) {
		this.storedProducersWH = storedProducersWH;
	}

	public Double getStoredConsumersWH() {
		return storedConsumersWH;
	}

	public void setStoredConsumersWH(Double storedConsumersWH) {
		this.storedConsumersWH = storedConsumersWH;
	}

	public Double getStoredWH() {
		return storedConsumersWH + storedProducersWH;
	}

	public double getMinRequestMissingRequest() {
		return minRequestMissingRequest;
	}

	public void setMinRequestMissingRequest(double minRequestMissingRequest) {
		this.minRequestMissingRequest = minRequestMissingRequest;
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

	public Double getProvidedLocallyMargin() {
		return providedLocallyMargin;
	}

	public void setProvidedLocallyMargin(Double providedLocallyMargin) {
		this.providedLocallyMargin = providedLocallyMargin;
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

	public NodeLocation getNodeLocation() {
		return nodeLocation;
	}

	public void setNodeLocation(NodeLocation nodeLocation) {
		this.nodeLocation = nodeLocation;
	}

	public int getDistance() {
		return distance;
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

	public boolean isAdditionalRefresh() {
		return isAdditionalRefresh;
	}

	public void setAdditionalRefresh(boolean isAdditionalRefresh) {
		this.isAdditionalRefresh = isAdditionalRefresh;
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
		if(this.isAggregated()) {
			result.append(super.toString());
			result.append(SapereUtil.CR);
		}
		if(date != null) {
			result.append("time ").append(UtilDates.format_time.format(this.date));
		}
		result.append(" requested:").append(UtilDates.df3.format(this.requested))
				.append(" produced:").append(UtilDates.df3.format(this.produced))
				.append(" provided:").append(UtilDates.df3.format(this.provided))
				.append(" consumed:").append(UtilDates.df3.format(this.consumed))
				.append(" available:").append(UtilDates.df3.format(this.available))
				.append(" missing:").append(UtilDates.df3.format(this.missing));
		if(storageUsedForNeed > 0) {
			result.append(" storageUsedForNeed:").append(UtilDates.df3.format(this.storageUsedForNeed));
		}
		if(storageUsedForProd > 0) {
			result.append(" storageUsedForProd:").append(UtilDates.df3.format(this.storageUsedForProd));
		}
		return result.toString();
	}

	@Override
	public NodeTotal clone() {
		NodeTotal result = new NodeTotal();
		result.setTimeShiftMS(timeShiftMS);
		result.setId(id);
		result.setDate(date);
		result.setIdLast(idLast);
		result.setIdNext(idNext);
		result.setSessionId(sessionId);
		if(nodeLocation != null) {
			result.setNodeLocation(nodeLocation.clone());
		}
		result.setDistance(distance);
		result.setRequested(requested);
		result.setProduced(produced);
		result.setConsumed(consumed);
		result.setConsumedLocally(consumedLocally);
		result.setProvided(provided);
		result.setProvidedLocally(providedLocally);
		result.setProvidedMargin(providedMargin);
		result.setConsumedMargin(consumedMargin);
		result.setAvailable(available);
		result.setMissing(missing);
		result.setSentOffersTotal(sentOffersTotal);
		result.setReceivedOffersTotal(receivedOffersTotal);
		result.setStorageUsedForNeed(storageUsedForNeed);
		result.setStorageUsedForProd(storageUsedForProd);
		result.setStoredConsumersWH(storedConsumersWH);
		result.setStoredProducersWH(storedProducersWH);
		result.setMaxWarningDuration(maxWarningDuration);
		result.setMaxWarningConsumer(maxWarningConsumer);
		result.setTimeShiftMS(timeShiftMS);
		result.setAdditionalRefresh(isAdditionalRefresh);
		if(aggregationDate != null)  {
			result.setAggregationDate(aggregationDate);
		}
		if(mapNodes != null) {
			result.setMapNodes(mapNodes);
		}
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
		result.setLastUpdate(lastUpdate);
		return result;
	}

	public boolean canAggregate(NodeTotal other) {
		if(timeShiftMS == null || other == null || other.getTimeShiftMS() == null) {
			return false;
		}
		long deltaTimShiftMS = timeShiftMS - other.getTimeShiftMS();
		boolean result = Math.abs(deltaTimShiftMS) < 1;
		return result;
	}

	public void add(NodeTotal other) {
		requested+=other.getRequested();
		produced+=other.getProduced();
		consumed+=other.getConsumed();
		consumedLocally+=other.getConsumedLocally();
		provided+=other.getProvided();
		providedLocally+=other.getProvidedLocally();
		available+=other.getAvailable();
		missing+=other.getMissing();
		if(other.getDate() != null) {
			if(date == null || other.getDate().after(date)) {
				date = other.getDate();
			}
		}
		if(other.getProvidedMargin() != null) {
			providedMargin+=other.getProvidedMargin();
		}
		if(other.getConsumedMargin() != null) {
			consumedMargin+=other.getConsumedMargin();
		}
		sentOffersTotal+=other.getSentOffersTotal();
		receivedOffersTotal+=other.getReceivedOffersTotal();
		storageUsedForNeed+=other.getStorageUsedForNeed();
		storageUsedForProd+=other.getStorageUsedForProd();
		storedProducersWH+=other.getStoredProducersWH();
		storedConsumersWH+=other.getStoredConsumersWH();
		//protected Date date;
		linkedEvents.addAll(other.getLinkedEvents());
		for(String agentName : other.getReceivedOffersRepartition().keySet()) {
			double toSet = other.getReceivedOffersRepartition().get(agentName);
			if(receivedOffersRepartition.containsKey(agentName)) {
				toSet+=receivedOffersRepartition.get(agentName);
			}
			receivedOffersRepartition.put(agentName, toSet);
		}
		for(String agentName : other.getSentOffersRepartition().keySet()) {
			double toSet = other.getSentOffersRepartition().get(agentName);
			if(sentOffersRepartition.containsKey(agentName)) {
				toSet+=sentOffersRepartition.get(agentName);
			}
			sentOffersRepartition.put(agentName, toSet);
		}
		if(other.getMinRequestMissing() < minRequestMissingRequest) {
			minRequestMissingRequest = other.getMinRequestMissing();
		}
		if(other.getMaxWarningDuration() > maxWarningDuration) {
			maxWarningDuration = other.getMaxWarningDuration();
		}
		 maxWarningConsumer = null;
		 refreshLastUpdate();
	}

	public NodeTotal aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, mapObjects, agentAuthentication, logger);
	}

	public static NodeTotal aggregate2(String operator, Map<String, IAggregateable> mapObjects
			,AgentAuthentication agentAuthentication, AbstractLogger logger) {
		try {
			if(OP_SUM.equals(operator)) {
				NodeTotal result = new NodeTotal();
				NodeTotal firstNodeTotal = null;
				List<NodeTotal> listNodeTotal = new ArrayList<>();
				for(String nextNode : mapObjects.keySet()) {
					IAggregateable nextObj = mapObjects.get(nextNode);
					if(nextObj instanceof NodeTotal) {
						if(firstNodeTotal == null) {
							firstNodeTotal = (NodeTotal) nextObj;
							listNodeTotal.add(firstNodeTotal);
							result.add(firstNodeTotal);
						} else {
							NodeTotal nextNodeTotal = (NodeTotal) nextObj;
							if(firstNodeTotal.canAggregate(nextNodeTotal)) {
								listNodeTotal.add(nextNodeTotal);
								result.add(nextNodeTotal);
							} else {
								logger.info("NodeTotal.aggregate2");
							}
						}
					}
				}
				if(result != null) {
					String test = result.toString();
					logger.info("NodeTotal.aggregate2 : result = " + test);
				}
				return result;
			}
		} catch (Throwable e) {
			logger.error(e);
		}
		return null;
	}

	@Override
	public IPropertyObject copyForLSA(AbstractLogger logger) {
		return clone();
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<NodeLocation>();
		if(nodeLocation != null) {
			result.add(nodeLocation);
		}
		return result;
	}

	public static List<NodeTotal> getListNodeTotal(HistoryInitializationForm historyInitForm) {
		List<NodeTotal> listNodeTotal = new ArrayList<NodeTotal>();
		Date[] dates = historyInitForm.getListDates();
		for(int dateIdx =0; dateIdx < dates.length; dateIdx++) {
			Date nextDate = dates[dateIdx];
			NodeTotal nextNodeTotal = new NodeTotal();
			nextNodeTotal.setDate(nextDate);
			nextNodeTotal.setRequested(historyInitForm.getRequested()[dateIdx]);
			nextNodeTotal.setProduced(historyInitForm.getProduced()[dateIdx]);
			nextNodeTotal.setAvailable(historyInitForm.getAvailable()[dateIdx]);
			nextNodeTotal.setConsumed(historyInitForm.getConsumed()[dateIdx]);
			nextNodeTotal.setProvided(historyInitForm.getProvided()[dateIdx]);
			nextNodeTotal.setMissing(historyInitForm.getMissing()[dateIdx]);
			if(historyInitForm.getConsumedLocally() != null) {
				nextNodeTotal.setConsumedLocally(historyInitForm.getConsumedLocally()[dateIdx]);
			}
			if(historyInitForm.getProvidedLocally() != null) {
				nextNodeTotal.setProvidedLocally(historyInitForm.getProvidedLocally()[dateIdx]);
			}
			//logger.info("nextNodeTotal = " + nextNodeTotal);
			listNodeTotal.add(nextNodeTotal);
		}
		return listNodeTotal;
	}

	public static HistoryInitializationForm initHistoryForm(List<NodeTotal> listNodeTotal
			,PredictionScope scope, LearningModelType usedModel) {
		HistoryInitializationForm historyInitForm = new HistoryInitializationForm();
		int historyLen = listNodeTotal.size();
		Date[] dates = new Date[historyLen];
		Double[] produced = new Double[historyLen];
		Double[] requested = new Double[historyLen];
		Double[] available = new Double[historyLen];
		Double[] missing = new Double[historyLen];
		Double[] consumed = new Double[historyLen];
		Double[] provided = new Double[historyLen];
		int idx = 0;
		for (NodeTotal nextNodeTotal : listNodeTotal) {
			dates[idx] = nextNodeTotal.getDate();
			produced[idx] = nextNodeTotal.getProduced();
			requested[idx] = nextNodeTotal.getRequested();
			available[idx] = nextNodeTotal.getAvailable();
			missing[idx] = nextNodeTotal.getMissing();
			consumed[idx] = nextNodeTotal.getConsumed();
			provided[idx] = nextNodeTotal.getProvided();
			idx++;
		}
		historyInitForm.setScope(scope.toOptionItem());
		historyInitForm.setUsedModel(usedModel);
		historyInitForm.setListDates(dates);
		historyInitForm.setProduced(produced);
		historyInitForm.setRequested(requested);
		historyInitForm.setAvailable(available);
		historyInitForm.setMissing(missing);
		historyInitForm.setProvided(provided);
		historyInitForm.setConsumed(consumed);
		historyInitForm.setCompleteMatrices(Boolean.TRUE);
		return historyInitForm;
	}

	public boolean hasChanges(NodeTotal lastTotal) {
		if (lastTotal == null) {
			return true;
		} else if (linkedEvents.size() > 0) {
			return true;
		} else if (Math.abs(this.requested - lastTotal.getRequested()) > 0.0001) {
			return true;
		} else if (Math.abs(this.produced - lastTotal.getProduced()) > 0.0001) {
			return true;
		} else if (Math.abs(this.provided - lastTotal.getProvided()) > 0.0001) {
			return true;
		} else if (Math.abs(this.consumed - lastTotal.getConsumed()) > 0.0001) {
			return true;
		} else if (Math.abs(this.missing - lastTotal.getMissing()) > 0.0001) {
			return true;
		} else if (Math.abs(this.available - lastTotal.getAvailable()) > 0.0001) {
			return true;
		} else if (Math.abs(this.providedMargin - lastTotal.getProvidedMargin()) > 0.0001) {
			return true;
		} else if (Math.abs(this.consumedMargin - lastTotal.getConsumedMargin()) > 0.0001) {
			return true;
		} else if (Math.abs(this.providedLocally - lastTotal.getProvidedLocally()) > 0.0001) {
			return true;
		} else if (Math.abs(this.maxWarningDuration - lastTotal.getMaxWarningDuration()) >= 10) {
			return true;
		} else if (isAdditionalRefresh != lastTotal.isAdditionalRefresh()) {
			return true;
		}
		return false;
	}

	public boolean isReadyForAggregation() {
		if(date != null) {
			Date lastAggregation = this.getAggregationDate();
			//Date current = predictionContext.getCurrentDate();
			int waitingMinutesBetweenAggragations = 0;
			Date minAggregationDate = lastAggregation == null ? lastUpdate : UtilDates.shiftDateMinutes(lastAggregation, waitingMinutesBetweenAggragations);
			if(lastAggregation == null || lastUpdate.after(minAggregationDate)) {
				// send input for a new aggregation
				return true;
			}
		}
		return false;
	}
}
