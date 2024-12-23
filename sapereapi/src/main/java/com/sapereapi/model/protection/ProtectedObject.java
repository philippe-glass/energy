package com.sapereapi.model.protection;

import java.util.HashSet;
import java.util.Set;

import com.sapereapi.model.Sapere;

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



	abstract boolean hasAccesAsConsumer(SapereAgent consumerAgent);

	abstract boolean hasAccesAsProducer(SapereAgent prodAgent);

	abstract boolean checkAccessAsProducer(AgentAuthentication authentication);

	abstract boolean checkAccessAsConsumer(AgentAuthentication authentication);

}
