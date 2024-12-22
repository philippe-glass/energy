package com.sapereapi.model.energy.node;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.input.AgentFilter;

import eu.sapere.middleware.node.NodeLocation;

public class MultiNodesContent extends NodeContent {
	List<NodeLocation> neighborNodeLocations = new ArrayList<NodeLocation>();

	public MultiNodesContent(NodeContext aNodeContext, AgentFilter filter, long _timeShiftMS) {
		super(aNodeContext, filter, _timeShiftMS);
	}

	public void merge(NodeContent otherContent, int nodeDistanceOtherContent) {
		NodeLocation otherNodeLocation = otherContent.getNodeContext().getNodeLocation();
		//boolean isNeighbor = false;
		if (!nodeContext.getNodeLocation().equals(otherNodeLocation)) {
			//isNeighbor = true;
			if (!neighborNodeLocations.contains(otherNodeLocation)) {
				neighborNodeLocations.add(otherNodeLocation);
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
