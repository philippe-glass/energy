package com.sapereapi.model.energy.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.ProsumerAgent;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;

public class NodeContent {
	protected NodeContext nodeContext;
	protected AgentFilter filter;
	protected List<AgentForm> consumers;
	protected List<AgentForm> producers;
	protected List<OptionItem> listProsumerRole;
	protected List<OptionItem> listPriorityLevel;
	protected List<OptionItem> listDeviceCategoryConsumer;
	protected List<OptionItem> listDeviceCategoryProducer;
	protected List<OptionItem> listYesNo;
	protected Map<String, NodeLocation> mapNeighborNodes;
	protected Map<String, Integer> mapNodeDistance;
	protected NodeTotal total;
	protected List<String> warnings;
	protected List<String> errors;
	protected boolean noFilter = true;
	protected Long timeShiftMS = null;
	protected Date currentDate = null;


	public NodeContext getNodeContext() {
		return nodeContext;
	}

	public void setNodeContext(NodeContext nodeContext) {
		this.nodeContext = nodeContext;
	}

	public List<AgentForm> getConsumers() {
		return consumers;
	}

	public void setConsumers(List<AgentForm> consumers) {
		this.consumers = consumers;
	}

	public List<AgentForm> getProducers() {
		return producers;
	}

	public void setProducers(List<AgentForm> producers) {
		this.producers = producers;
	}

	public List<OptionItem> getListProsumerRole() {
		return listProsumerRole;
	}

	public void setListProsumerRole(List<OptionItem> listProsumerRole) {
		this.listProsumerRole = listProsumerRole;
	}

	public List<OptionItem> getListPriorityLevel() {
		return listPriorityLevel;
	}

	public void setListPriorityLevel(List<OptionItem> listPriorityLevel) {
		this.listPriorityLevel = listPriorityLevel;
	}

	public List<OptionItem> getListDeviceCategoryConsumer() {
		return listDeviceCategoryConsumer;
	}

	public void setListDeviceCategoryConsumer(List<OptionItem> listDeviceCategoryConsumer) {
		this.listDeviceCategoryConsumer = listDeviceCategoryConsumer;
	}

	public List<OptionItem> getListDeviceCategoryProducer() {
		return listDeviceCategoryProducer;
	}

	public void setListDeviceCategoryProducer(List<OptionItem> listDeviceCategoryProducer) {
		this.listDeviceCategoryProducer = listDeviceCategoryProducer;
	}

	public NodeTotal getTotal() {
		return total;
	}

	public void setCurrentDate(Date currentDate) {
		this.currentDate = currentDate;
	}

	public Map<String, NodeLocation> getMapNeighborNodes() {
		return mapNeighborNodes;
	}

	public void setMapNeighborNodes(Map<String, NodeLocation> mapNeighborNodes) {
		this.mapNeighborNodes = mapNeighborNodes;
	}

	public void setTotal(NodeTotal total) {
		this.total = total;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	public AgentFilter getFilter() {
		return filter;
	}

	public void setFilter(AgentFilter filter) {
		this.filter = filter;
	}

	public List<OptionItem> getListYesNo() {
		return listYesNo;
	}

	public void setListYesNo(List<OptionItem> listYesNo) {
		this.listYesNo = listYesNo;
	}

	public Long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public Map<String, Integer> getMapNodeDistance() {
		return mapNodeDistance;
	}

	public void setMapNodeDistance(Map<String, Integer> mapNodeDistance) {
		this.mapNodeDistance = mapNodeDistance;
	}

	public List<String> getProducerNames() {
		List<String> producerNames = new ArrayList<String>();
		for(AgentForm producer : producers) {
			producerNames.add(producer.getAgentName());
		}
		return producerNames;
	}

	public List<String> getConsumerNames() {
		List<String> consumerNames = new ArrayList<String>();
		for(AgentForm consumer : consumers) {
			consumerNames.add(consumer.getAgentName());
		}
		return consumerNames;
	}

	public NodeContent(NodeContext _nodeContext, AgentFilter filter, long _timeShiftMS) {
		super();
		nodeContext = _nodeContext;
		consumers = new ArrayList<AgentForm>();
		producers = new ArrayList<AgentForm>();
		errors = new ArrayList<String>();
		warnings = new ArrayList<String>();
		total = new NodeTotal();
		total.setTimeShiftMS(timeShiftMS);
		listPriorityLevel = PriorityLevel.getOptionList();
		listDeviceCategoryConsumer = DeviceCategory.getOptionList(false, true);
		listDeviceCategoryProducer = DeviceCategory.getOptionList(true, false);
		listProsumerRole = ProsumerRole.getOptionList();
		mapNeighborNodes = new HashMap<String, NodeLocation>();
		listYesNo = new ArrayList<OptionItem>();
		listYesNo.add(new OptionItem("", " "));
		listYesNo.add(new OptionItem("YES", "Yes"));
		listYesNo.add(new OptionItem("NO", "No"));
		timeShiftMS = _timeShiftMS;
	}

	// AgentForm comparator : by id
	protected final Comparator<AgentForm> agentComparator = new Comparator<AgentForm>() {
		public int compare(AgentForm o1, AgentForm o2) {
			return o1.compareTo(o2);
		}
	};

	/*
	public void addConsumer(ConsumerAgent consumer, boolean isInSapce, int distance) {
		AgentForm newConsumer = new AgentForm(consumer, isInSapce, distance);
		this.consumers.add(newConsumer);
	}*/

	public void addConsumer(AgentForm newConsumer) {
		this.consumers.add(newConsumer);
	}

	public void addProsumer(ProsumerAgent prosumerAgent, boolean isInSapce,int distance) {
		AgentForm newProsumer = new AgentForm(prosumerAgent, isInSapce, distance);
		if(prosumerAgent.isProducer()) {
			this.producers.add(newProsumer);
		} else {
			this.consumers.add(newProsumer);
		}
	}

	/*
	public void addProducer(ProducerAgent producer, boolean isInSapce,int distance) {
		AgentForm newProducer = new AgentForm(producer, isInSapce, distance);
		this.producers.add(newProducer);
	}*/

	public void addProducer(AgentForm newProducer) {
		this.producers.add(newProducer);
	}

	public void sortAgents() {
		Collections.sort(consumers, agentComparator);
		Collections.sort(producers, agentComparator);
	}

	public AgentForm getProducer(String agentName) {
		if (agentName == null) {
			return null;
		}
		for (AgentForm producer : this.producers) {
			if (agentName.equals(producer.getAgentName())) {
				return producer;
			}
		}
		return null;
	}

	public AgentForm getConsumer(String agentName) {
		if (agentName == null) {
			return null;
		}
		for (AgentForm consumer : this.consumers) {
			if (agentName.equals(consumer.getAgentName())) {
				return consumer;
			}
		}
		return null;
	}

	public AgentForm getAgent(String agentName) {
		AgentForm agent = getConsumer(agentName);
		if (agent != null) {
			return agent;
		}
		agent = getProducer(agentName);
		return agent;
	}

	private int getNbActiveAgents(List<AgentForm> listAgents, boolean expired) {
		int result = 0;
		for (AgentForm producer : listAgents) {
			if (expired == producer.getHasExpired()) {
				result++;
			}
		}
		return result;
	}

	public int getNbActiveAgents(ProsumerRole prosumerRole) {
		if (ProsumerRole.CONSUMER.equals(prosumerRole)) {
			return getNbActiveAgents(consumers, false);
		} else if (ProsumerRole.PRODUCER.equals(prosumerRole)) {
			return getNbActiveAgents(producers, false);
		}
		return 0;
	}

	public int getNbExpiredAgents(ProsumerRole prosumerRole) {
		if (ProsumerRole.CONSUMER.equals(prosumerRole)) {
			return getNbActiveAgents(consumers, true);
		} else if (ProsumerRole.PRODUCER.equals(prosumerRole)) {
			return getNbActiveAgents(producers, true);
		}
		return 0;
	}

	public AgentForm getFirstInactiveConsumer() {
		for (AgentForm consumer : consumers) {
			if (consumer.getHasExpired()) {
				return consumer;
			}
		}
		return null;
	}

	public AgentForm getFirstInactiveProducer() {
		for (AgentForm producer : producers) {
			if (producer.getHasExpired()) {
				return producer;
			}
		}
		return null;
	}

	public AgentForm getRandomInactiveProducer(List<AgentForm> listAgents) {
		List<AgentForm> agents = new ArrayList<AgentForm>();
		for (AgentForm producer : listAgents) {
			if (producer.getHasExpired()) {
				agents.add(producer);
			}
		}
		if (agents.size() > 0) {
			Collections.shuffle(agents);
			return agents.get(0);
		}
		return null;
	}

	public AgentForm getRandomInactiveProducer() {
		return getRandomInactiveProducer(producers);
	}

	public AgentForm getRandomInactiveConsumer() {
		return getRandomInactiveProducer(consumers);
	}

	public boolean isNoFilter() {
		return noFilter;
	}

	public void setNoFilter(boolean noFilter) {
		this.noFilter = noFilter;
	}

	public NodeTotal getPartialTotal(DeviceCategory deviceCategory) {
		NodeTotal result = new NodeTotal();
		result.setTimeShiftMS(timeShiftMS);
		Double requested = Double.valueOf(0);
		Double produced = Double.valueOf(0);
		Double consumed = Double.valueOf(0);
		Double provided = Double.valueOf(0);
		Double sentOffersTotal = Double.valueOf(0);
		Double receivedOffersTotal = Double.valueOf(0);
		// Map<String, Float> receivedOffersRepartition = new HashMap<String, Float>();
		// // By Producers
		for (AgentForm consumer : consumers) {
			if (!consumer.getHasExpired()) {
				if (deviceCategory == null || consumer.hasDeviceCategory(deviceCategory)) {
					requested += consumer.getPower();
					consumed += consumer.getOngoingContractsTotal().getCurrent();
					receivedOffersTotal += consumer.getOffersTotal();
				}
			}
		}
		for (AgentForm producer : producers) {
			if (!producer.getHasExpired()) {
				if (deviceCategory == null || producer.hasDeviceCategory(deviceCategory) ) {
					produced += producer.getPower();
					provided += producer.getOngoingContractsTotal().getCurrent();
					sentOffersTotal += producer.getOffersTotal();
				}
			}
		}
		result.setReceivedOffersTotal(receivedOffersTotal);
		result.setSentOffersTotal(sentOffersTotal);
		result.setRequested(requested);
		result.setProduced(produced);
		result.setConsumed(consumed);
		result.setAvailable(Math.max(0, produced - consumed));
		result.setProvided(provided);
		result.setMissing(Math.max(0,requested - consumed));
		return result;
	}

	public List<AgentForm> getAgents() {
		List<AgentForm> result = new ArrayList<AgentForm>();
		result.addAll(consumers);
		result.addAll(producers);
		return result;
	}

	public Map<String, AgentForm> getMapRunningAgents() {
		Map<String, AgentForm> result = new HashMap<String, AgentForm>();
		for (AgentForm agentForm : getAgents()) {
			if (agentForm.isRunning()) {
				result.put(agentForm.getDeviceName(), agentForm);
			}
		}
		return result;
	}

	public AgentForm getAgentByDeviceName(String deviceName) {
		if(deviceName==null) {
			return null;
		}
		for(AgentForm agentForm : getAgents()) {
			if(deviceName.equals(agentForm.getDeviceName())) {
				return agentForm;
			}
		}
		return null;
	}

	public boolean hasDevice(String deviceName) {
		if(deviceName==null) {
			return false;
		}
		for(AgentForm agentForm : getAgents()) {
			if(deviceName.equals(agentForm.getDeviceName())) {
				return true;
			}
		}
		return false;
	}

	public void computeTotal() {
		total = new NodeTotal();
		total.setTimeShiftMS(timeShiftMS);
		double requested = 0.0;
		double produced = 0.0;
		double consumed = 0.0;
		double consumedLocally = 0.0;
		double provided = 0.0;
		double providedMargin = 0.0;
		double providedLocallyMargin = 0.0;
		double providedLocally = 0.0;
		double sentOffersTotal = 0.0;
		double receivedOffersTotal = 0.0;
		try {
			Map<String, Double> receivedOffersRepartition = new HashMap<String, Double>(); // By Producers
			for (AgentForm consumer : consumers) {
				if (!consumer.getHasExpired()) {
					requested += consumer.getPower();
					consumed += consumer.getOngoingContractsTotal().getCurrent();
					consumedLocally += consumer.getOngoingContractsTotalLocal().getCurrent();
					double testDelta = Math.abs(consumer.getOngoingContractsTotal().getCurrent() - consumer.getOngoingContractsTotalLocal().getCurrent());
					if(testDelta > 0.01) {
						SapereLogger.getInstance().info("NodeContent.computeTotal For debug : " + consumer.getAgentName() + " provided <> providedLocally : delta = " + testDelta);
					}
					receivedOffersTotal += consumer.getOffersTotal();
					// update received offers repartition
					for (String producer : consumer.getOffersRepartition().keySet()) {
						Double toAdd = consumer.getOffersRepartition().get(producer);
						if (!receivedOffersRepartition.containsKey(producer)) {
							receivedOffersRepartition.put(producer, Double.valueOf(0));
						}
						Double power = receivedOffersRepartition.get(producer);
						receivedOffersRepartition.put(producer, power + toAdd);
					}
				}
			}
			// total.setReceivedOffersRepartition(SapereUtil.formaMapValues(receivedOffersRepartition));
			List<String> filteredConsumers = this.getConsumerNames();
			List<String> filteredProducers = this.getProducerNames();
			//noFilter = false;
			total.setReceivedOffersRepartition(receivedOffersRepartition);
			Map<String, Double> sentOffersRepartition = new HashMap<String, Double>(); // By consumers
			for (AgentForm producer : producers) {
				if (!producer.getHasExpired()) {
					produced += producer.getPower();
					provided += producer.getOngoingContractsTotal().getCurrent();
					providedMargin += producer.getOngoingContractsTotal().getMargin();
					providedLocallyMargin +=  producer.getOngoingContractsTotalLocal().getMargin();
					providedLocally += producer.getOngoingContractsTotalLocal().getCurrent();
					sentOffersTotal += producer.getOffersTotal();
					// update sent offers repartition
					for (String consumer : producer.getOffersRepartition().keySet()) {
						if(noFilter || filteredConsumers.contains(consumer)) {
							Double toAdd = producer.getOffersRepartition().get(consumer);
							if (!sentOffersRepartition.containsKey(consumer)) {
								sentOffersRepartition.put(consumer, Double.valueOf(0));
							}
							Double power = sentOffersRepartition.get(consumer);
							sentOffersRepartition.put(consumer, power + toAdd);
						}
					}
					for (String consumerName : producer.getWaitingContractsConsumers()) {
						AgentForm consumer = this.getConsumer(consumerName);
						if(consumer != null && consumer.isLocal()) {
							if(noFilter || filteredConsumers.contains(consumerName)) {
								if (consumer.getIsSatisfied()) {
									// Add warning
									Double totalWaiting = producer.getWaitingContractsPower().getCurrent();
									String warningMsg = "The valided contract of " + consumerName
											+ " is still in waiting status in " + producer.getAgentName() + " LSA.";
									warningMsg += "(Sum power:" + UtilDates.df3.format(totalWaiting) + ")";
									if (!warnings.contains(warningMsg)) {
										warnings.add(warningMsg);
									}
								}
							}
						}
					}
					for (String consumerName : producer.getOngoingContractsRepartition().keySet()) {
						if(noFilter || filteredConsumers.contains(consumerName)) {
							AgentForm consumer = this.getConsumer(consumerName);
							double consumerContribution = producer.getOngoingContractsRepartition().get(consumerName).getCurrent();
							if (consumer!=null && !consumer.getIsSatisfied() && consumer.isLocal()) {
								double consumerContractTotal = consumer.getOngoingContractsTotal().getCurrent();
								// Add warning
								if(consumerContractTotal==0) {
									String warningMsg = "The contract of " + consumerName + " is already valid in "
											+ producer.getAgentName() + " lsa but not valid in " + consumerName + " lsa.";
									warningMsg += " (Contribution : " + UtilDates.df3.format(consumerContribution) + ")";
									if (!warnings.contains(warningMsg)) {
										warnings.add(warningMsg);
									}
								}
							}
						}
					}
				}
			}
			for (AgentForm consumer : consumers) {
				// Check request = contract
				if(consumer.getIsSatisfied() && consumer.getEnergyRequest()!=null) {
					String consumerName = consumer.getAgentName();
					double totalContract = consumer.getOngoingContractsTotal().getCurrent();
					double requestPower = consumer.getEnergyRequest().getPower();
					if( totalContract>0 && Math.abs( totalContract - requestPower) >= 0.001) {
						String warningMsg = "The total supplied to " + consumerName + " (" + UtilDates.df3.format(totalContract) + " W)"
								+ " does not correspond to the requested power : " + UtilDates.df3.format(requestPower) + " W.";
						if (!warnings.contains(warningMsg)) {
							warnings.add(warningMsg);
						}
					}
					for(String producerName : consumer.getOngoingContractsRepartition().keySet()) {
						if(filteredProducers.contains(producerName)) {
							Double providedLsaConsumer = consumer.getOngoingContractsRepartition().get(producerName).getCurrent();
							AgentForm producer = this.getProducer(producerName);
							if(producer!=null) {
								double providedLsaProducer = 0;
								if(producer.getOngoingContractsRepartition()!=null && producer.getOngoingContractsRepartition().containsKey(consumerName)) {
									providedLsaProducer = producer.getOngoingContractsRepartition().get(consumerName).getCurrent();
								}
								double gap = Math.abs( providedLsaProducer - providedLsaConsumer);
								if(gap >= 0.001) {
									String warningMsg = "The power supplied by " + producerName  + " to " + consumerName
											+ " has differents value in consumer lsa (" + UtilDates.df3.format(providedLsaConsumer) + " W) " 
											+ " and in producer lsa (" + UtilDates.df3.format(providedLsaProducer) + " W. )" 
											+ " gap = " + UtilDates.df3.format(gap);
									if (!warnings.contains(warningMsg)) {
										warnings.add(warningMsg);
									}
								}
							}
						}
					}
				}
			}
			/*
			 * for (AgentForm consumer : consumers) { Map<String, Float>
			 * contractsRepartition = consumer.getContractsRepartition(); }
			 */
			total.setSentOffersRepartition(sentOffersRepartition);
			// total.setSentOffersRepartition(SapereUtil.filterRepartition(sentOffersRepartition, filteredProducers));
			total.setDate(getCurrentDate());
			total.setRequested(requested);
			total.setProduced(produced);
			total.setConsumedLocally(consumedLocally);	// What is both consumed and produced locally
			total.setConsumed(consumed);
			total.setAvailable(produced - provided - providedMargin);
			total.setProvided(provided);
			total.setProvidedLocally(providedLocally);	// What is both locally produced and supplied
			total.setProvidedLocallyMargin(providedLocallyMargin);
			total.setProvidedMargin(providedMargin);
			total.setMissing(requested - consumed);
			total.setSentOffersTotal(sentOffersTotal);
			total.setReceivedOffersTotal(receivedOffersTotal);
			if(noFilter) {
				double delta = Math.abs(consumedLocally - providedLocally);
				if (Math.abs(consumedLocally) >0 && Math.abs(providedLocally) > 0 && delta >= 0.01) {
					errors.add("Total locally consumed power " + UtilDates.df3.format(consumedLocally)
							+ " is not equals to the total power locally provided by producers : " + UtilDates.df3.format(providedLocally)
							+ " gap=" + UtilDates.df3.format(delta));
				} else {
					// errors.add("OK");
				}
			}
		} catch (Throwable e) {
			SapereLogger.getInstance().error(e);
		}
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}
}
