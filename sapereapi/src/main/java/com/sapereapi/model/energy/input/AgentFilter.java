package com.sapereapi.model.energy.input;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.agent.energy.ProsumerAgent;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.node.NodeManager;

public class AgentFilter {
	private String[] consumerDeviceCategories;
	private String[] producerDeviceCategories;
	private String[] neighborNodeNames;
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

	public String[] getNeighborNodeNames() {
		return neighborNodeNames;
	}

	public void setNeighborNodeNames(String[] neighborNodeNames) {
		this.neighborNodeNames = neighborNodeNames;
	}

	public boolean isHideExpiredAgents() {
		return hideExpiredAgents;
	}

	public void setHideExpiredAgents(boolean hideExpiredAgents) {
		this.hideExpiredAgents = hideExpiredAgents;
	}

	public boolean applyFilter(SapereAgent agent) {
		if (agent instanceof EnergyAgent) {
			boolean isOk = true;
			EnergyAgent energyAgent = (EnergyAgent) agent;
			ProsumerProperties prosumerProperties = energyAgent.getProductionOrNeed().getIssuerProperties();
			DeviceCategory deviceCategory = prosumerProperties.getDeviceProperties().getCategory();
			//String deviceCategoryName = deviceCategory.name();
			/*
			if (agent instanceof ConsumerAgent) {
				// ConsumerAgent consumer = (ConsumerAgent) agent;
				if (consumerDeviceCategories != null && consumerDeviceCategories.length > 0) {
					// String deviceCategory2 =
					// consumer.getNeed().getDeviceProperties().getCategory().name();
					isOk = isOk && SapereUtil.isInStrArray(consumerDeviceCategories, deviceCategoryName);
				}
			}
			if (agent instanceof ProducerAgent) {
				// ProducerAgent producer = (ProducerAgent) agent;
				if (producerDeviceCategories != null && producerDeviceCategories.length > 0) {
					// String deviceCategory =
					// producer.getEnergySupply().getDeviceProperties().getCategory().name();
					isOk = isOk && SapereUtil.isInStrArray(producerDeviceCategories, deviceCategoryName);
				}
			}*/
			if (agent instanceof ProsumerAgent) {
				// ProducerAgent producer = (ProducerAgent) agent;
				if(deviceCategory.isProducer()) {
					if (producerDeviceCategories != null && producerDeviceCategories.length > 0) {
						// String deviceCategory =
						// producer.getEnergySupply().getDeviceProperties().getCategory().name();
						isOk = isOk && SapereUtil.isInStrArray(producerDeviceCategories, deviceCategory.name());
					}
				}
				if(deviceCategory.isConsumer()) {
					if (consumerDeviceCategories != null && consumerDeviceCategories.length > 0) {
						// String deviceCategory2 =
						// consumer.getNeed().getDeviceProperties().getCategory().name();
						isOk = isOk && SapereUtil.isInStrArray(consumerDeviceCategories, deviceCategory.name());
					}
				}
			}
			if (hideExpiredAgents) {
				isOk = isOk && !energyAgent.hasExpired();
			}
			if (!NodeManager.getNodeName().equals(energyAgent.getNodeName())) {
				isOk = isOk && SapereUtil.isInStrArray(neighborNodeNames, energyAgent.getNodeName());
			}
			return isOk;
		}
		return false;
	}
}
