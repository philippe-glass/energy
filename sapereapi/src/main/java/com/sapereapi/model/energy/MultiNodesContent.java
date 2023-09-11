package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.energy.input.AgentFilter;

import eu.sapere.middleware.node.NodeConfig;

public class MultiNodesContent extends NodeContent {
	List<NodeConfig> neighborNodeConfigs = new ArrayList<NodeConfig>();

	public MultiNodesContent(NodeConfig aNodeConfig, AgentFilter filter, long _timeShiftMS) {
		super(aNodeConfig, filter, _timeShiftMS);
	}

	public void merge(NodeContent otherContent, int nodeDistanceOtherContent) {
		NodeConfig otherNodeConfig = otherContent.getNodeConfig();
		//boolean isNeighbor = false;
		if (!nodeConfig.equals(otherNodeConfig)) {
			//isNeighbor = true;
			if (!neighborNodeConfigs.contains(otherNodeConfig)) {
				neighborNodeConfigs.add(otherNodeConfig);
			}
		}
		for (AgentForm consumer : otherContent.getConsumers()) {
			// each agent name should be unique
			if (getAgent(consumer.getAgentName()) == null) {
				/*
				 * if (false && isNeighbor) {
				 * consumer.setAgentName(SapereUtil.addStar(consumer.getAgentName())); }
				 */
				consumer.setDistance(nodeDistanceOtherContent);
				this.consumers.add(consumer);
			} else {
				// TODO send an exception ?
			}
		}
		for (AgentForm producer : otherContent.getProducers()) {
			if (getAgent(producer.getAgentName()) == null) {
				/*
				if (false && isNeighbor) {
					producer.setAgentName(SapereUtil.addStar(producer.getAgentName()));
				}*/
				producer.setDistance(nodeDistanceOtherContent);
				this.producers.add(producer);
			}
		}
		// refresh all totals
		computeTotal();
	}
}
