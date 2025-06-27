package com.sapereapi.model.protection;

import java.util.List;
import java.util.Map;

import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.ConfirmationTable;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.ProsumerRole;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class ProtectedConfirmationTable extends ProtectedObject {
	private static final long serialVersionUID = 2L;
	private ConfirmationTable confirmationTable = null;

	public ProtectedConfirmationTable(ConfirmationTable confirmationTable) {
		super();
		this.confirmationTable = confirmationTable;
	}

	@Override
	boolean checkAccessAsIssuer(AgentAuthentication authentication) {
		boolean agentTypeOK = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (agentTypeOK && confirmationTable.hasIssuer(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	@Override
	boolean checkAccessAsReceiver(AgentAuthentication authentication) {
		boolean agentTypeOK = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (agentTypeOK && confirmationTable.hasReceiver(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	public ConfirmationTable getConfirmationTable(SapereAgent producerAgent) throws PermissionException {
		if (!hasAccessAsIssuer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable;
	}

	public String getIssuer(SapereAgent producerAgent) throws PermissionException {
		if(!hasAccessAsStackholder(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable.getIssuer();
	}

	public boolean hasExpiredItem(SapereAgent producerAgent) throws PermissionException {
		String agentName = producerAgent.getAgentName();
		if (!hasAccessAsIssuer(producerAgent) && confirmationTable.getIssuer().equals(agentName)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable.hasExpiredItem();
	}

	public void cleanExpiredDate(SapereAgent producerAgent) throws PermissionException {
		if (!hasAccessAsIssuer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		confirmationTable.cleanExpiredDate();
	}

	public boolean hasConfirmationItem(SapereAgent producerAgent, String receiver, boolean isComplementary, ProsumerRole role) throws PermissionException {
		if (!hasAccessAsIssuer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable.hasConfirmationItem(receiver, isComplementary, role);
	}

	public void removeConfirmation(SapereAgent producerAgent, String receiver, boolean isComplementary, ProsumerRole role) throws PermissionException {
		if (!hasAccessAsIssuer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		confirmationTable.removeConfirmation(receiver, isComplementary, role);
	}

	public void confirmAsProducer(SapereAgent producerAgent, String receiver, boolean isComplementary, Boolean value,
			String comment, int nbOfRenewals) throws PermissionException {
		this.confirm(producerAgent, receiver, isComplementary, ProsumerRole.PRODUCER, value, comment, nbOfRenewals);
	}

	public void confirmAsConsumer(SapereAgent producerAgent, String receiver, boolean isComplementary, Boolean value,
			String comment, int nbOfRenewals) throws PermissionException {
		this.confirm(producerAgent, receiver, isComplementary, ProsumerRole.CONSUMER, value, comment, nbOfRenewals);
	}

	private void confirm(SapereAgent producerAgent, String receiver, boolean isComplementary, ProsumerRole role,  Boolean value, String comment, int nbOfRenewals)
			throws PermissionException {
		if (!hasAccessAsIssuer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		confirmationTable.confirm(receiver, isComplementary, role, value, comment, nbOfRenewals);
	}

	public boolean renewConfirmations(SapereAgent producerAgent) throws PermissionException {
		if (!hasAccessAsIssuer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable.renewConfirmations();
	}

/*
	public Boolean getConfirmation(SapereAgent consumerAgent) throws PermissionException {
		if (!this.hasAccesAssConsumer(consumerAgent)) {
			throw new PermissionException("Access denied for agent " + consumerAgent.getAgentName());
		}
		return confirmationTable.getConfirmation(consumerAgent.getAgentName());
	}
*/

	public ConfirmationItem getConfirmationItem(SapereAgent consumerAgent, boolean isComplementary, ProsumerRole role) throws PermissionException {
		if (!hasAccessAsStackholder(consumerAgent)) {
			throw new PermissionException("Access denied for agent " + consumerAgent.getAgentName());
		}
		return confirmationTable.getConfirmationItem(consumerAgent.getAgentName(), isComplementary, role);
	}

	public ConfirmationItem getConfirmationItem2(SapereAgent consumerAgent, String agentName, boolean isComplementary, ProsumerRole role) throws PermissionException {
		if (!hasAccessAsStackholder(consumerAgent)) {
			throw new PermissionException("Access denied for agent " + consumerAgent.getAgentName());
		}
		return confirmationTable.getConfirmationItem(agentName, isComplementary, role);
	}

	/* */
	public ProtectedConfirmationTable clone() {
		ConfirmationTable confirmationTableClone = confirmationTable.clone();
		return new ProtectedConfirmationTable(confirmationTableClone);
	}

	public ProtectedConfirmationTable copyForLSA(AbstractLogger logger) {
		ConfirmationTable confirmationTableClone = confirmationTable.copyForLSA(logger);
		return new ProtectedConfirmationTable(confirmationTableClone);
	}

	@Override
	public String toString() {
		return " " + confirmationTable ;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		confirmationTable.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		return confirmationTable.retrieveInvolvedLocations();
	}


}
