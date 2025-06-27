package com.sapereapi.model.protection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.referential.AgentType;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class ProtectedSingleOffer extends ProtectedObject {
	private static final long serialVersionUID = 7L;
	private SingleOffer singleOffer;

	public ProtectedSingleOffer(SingleOffer offer) {
		super();
		this.singleOffer = offer;
	}

	@Override
	boolean checkAccessAsIssuer(AgentAuthentication authentication) {
		boolean hasTypeProsuer = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (hasTypeProsuer && singleOffer.getProducerAgent().equals((authentication.getAgentName()))) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	@Override
	boolean checkAccessAsReceiver(AgentAuthentication authentication) {
		boolean hasTypeProsumer = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (hasTypeProsumer && singleOffer.getConsumerAgent().equals((authentication.getAgentName()))) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	public String getConsumerAgent(SapereAgent agent) throws PermissionException {
		boolean hasAccess = hasAccessAsIssuer(agent) || hasAccessAsReceiver(agent);
		if (!hasAccess) {
			throw new PermissionException("Access denied for agent " + agent.getAgentName());
		}
		return singleOffer.getConsumerAgent();
	}

	public SingleOffer getSingleOffer(SapereAgent agent) throws PermissionException {
		boolean hasAccess = hasAccessAsIssuer(agent) || hasAccessAsReceiver(agent);
		if (!hasAccess) {
			throw new PermissionException("Access denied for agent " + agent.getAgentName());
		}
		return singleOffer;
	}

	@Override
	public ProtectedSingleOffer clone() {
		SingleOffer offerClone = singleOffer.clone();
		return new ProtectedSingleOffer(offerClone);
	}

	@Override
	public ProtectedSingleOffer copyForLSA(AbstractLogger logger) {
		SingleOffer offerClone = singleOffer.copyForLSA();
		return new ProtectedSingleOffer(offerClone);
	}

	@Override
	public String toString() {
		return singleOffer.toString();
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		if(singleOffer != null) {
			singleOffer.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
		}
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		if(singleOffer != null) {
			return singleOffer.retrieveInvolvedLocations();
		}
		return new ArrayList<>();
	}



	public static IAggregateable aggregate2(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		// TODO Auto-generated method stub
		return null;
	}

}
