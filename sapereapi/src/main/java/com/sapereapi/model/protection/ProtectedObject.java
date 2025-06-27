package com.sapereapi.model.protection;

import java.util.HashSet;
import java.util.Set;

import com.sapereapi.model.Sapere;
import com.sapereapi.model.referential.AgentType;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.IPropertyObject;

public abstract class ProtectedObject implements IPropertyObject {

	private static final long serialVersionUID = 1L;
	protected Set<AgentAuthentication> validAuthentications = new HashSet<AgentAuthentication>();

	protected boolean checkAuthentication(AgentAuthentication authentication) {
		if (validAuthentications.contains(authentication)) {
			return true;
		}
		boolean authenticated = Sapere.getInstance().isAuthenticated(authentication);
		if (authenticated) {
			validAuthentications.add(authentication);
		}
		return authenticated;
	}

	public boolean hasAccessAsIssuer(SapereAgent agent) {
		return checkAccessAsIssuer(agent.getAuthentication());
	}

	public boolean hasAccessAsReceiver(SapereAgent agent) {
		return checkAccessAsReceiver(agent.getAuthentication());
	}

	public boolean hasAccessAsStackholder(SapereAgent agent) {
		return checkAccessAsStackholder(agent.getAuthentication());
	}

	public boolean checkAccessAsStackholder(AgentAuthentication agentAuthentication) {
		return checkAccessAsIssuer(agentAuthentication) || checkAccessAsReceiver(agentAuthentication);
	}

	protected boolean checkAccessAsLearningAgent(AgentAuthentication authentication) {
		if (AgentType.LEARNING_AGENT.name().equals(authentication.getAgentType())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	abstract boolean checkAccessAsIssuer(AgentAuthentication agent);

	abstract boolean checkAccessAsReceiver(AgentAuthentication agent);

}
