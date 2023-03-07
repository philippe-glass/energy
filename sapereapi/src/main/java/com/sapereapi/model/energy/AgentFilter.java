package com.sapereapi.model.energy;

import com.sapereapi.agent.energy.ConsumerAgent;
import com.sapereapi.agent.energy.ProducerAgent;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.agent.SapereAgent;

public class AgentFilter {
	private String[] consumerDeviceCategories;
	private String[] producerDeviceCategories;
	private boolean hideExpiredAgents = false;

	public String[] getConsumerDeviceCategories() {
		return consumerDeviceCategories;
	}

	public void setConsumerDeviceCategories(String[] consumerDeviceCategories) {
		this.consumerDeviceCategories = consumerDeviceCategories;
	}

	public String[] getProducerDeviceCategories() {
		return producerDeviceCategories;
	}

	public void setProducerDeviceCategories(String[] producerDeviceCategories) {
		this.producerDeviceCategories = producerDeviceCategories;
	}

	public boolean isHideExpiredAgents() {
		return hideExpiredAgents;
	}

	public void setHideExpiredAgents(boolean hideExpiredAgents) {
		this.hideExpiredAgents = hideExpiredAgents;
	}

	public boolean applyFilter(SapereAgent agent) {
		boolean isOk = true;
		if (agent instanceof ConsumerAgent) {
			ConsumerAgent consumer = (ConsumerAgent) agent;
			if (consumerDeviceCategories != null && consumerDeviceCategories.length > 0) {
				String deviceCategory = consumer.getNeed().getDeviceProperties().getCategory().name();
				isOk = isOk && SapereUtil.isInStrArray(consumerDeviceCategories, deviceCategory);
			}
			if (hideExpiredAgents) {
				isOk = isOk && !consumer.hasExpired();
			}
		}
		if (agent instanceof ProducerAgent) {
			ProducerAgent producer = (ProducerAgent) agent;
			if (producerDeviceCategories != null && producerDeviceCategories.length > 0) {
				String deviceCategory = producer.getEnergySupply().getDeviceProperties().getCategory().name();
				isOk = isOk && SapereUtil.isInStrArray(producerDeviceCategories, deviceCategory);
			}
			if (hideExpiredAgents) {
				isOk = isOk && !producer.hasExpired();
			}
		}
		return isOk;
	}
}
