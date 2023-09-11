package com.sapereapi.model.energy.input;

import com.sapereapi.agent.energy.ConsumerAgent;
import com.sapereapi.agent.energy.IEnergyAgent;
import com.sapereapi.agent.energy.ProducerAgent;
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
		if (agent instanceof IEnergyAgent) {
			boolean isOk = true;
			IEnergyAgent energyAgent = (IEnergyAgent) agent;
			DeviceCategory deviceCategory = energyAgent.getEnergySupply().getDeviceProperties().getCategory();
			String deviceCategoryName = deviceCategory.name();
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
