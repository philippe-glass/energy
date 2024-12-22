package com.sapereapi.model.protection;

import java.util.List;
import java.util.Map;

import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.ConfirmationTable;
import com.sapereapi.model.referential.AgentType;

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
	public boolean hasAccesAsConsumer(SapereAgent consumerAgent) {
		return checkAccessAsConsumer(consumerAgent.getAuthentication());
	}

	@Override
	public boolean hasAccesAsProducer(SapereAgent prodAgent) {
		return checkAccessAsProducer(prodAgent.getAuthentication());
	}

	@Override
	boolean checkAccessAsProducer(AgentAuthentication authentication) {
		boolean agentTypeOK = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (agentTypeOK && confirmationTable.hasIssuer(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	@Override
	boolean checkAccessAsConsumer(AgentAuthentication authentication) {
		boolean agentTypeOK = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (agentTypeOK && confirmationTable.hasReceiver(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	public ConfirmationTable getConfirmationTable(SapereAgent producerAgent) throws PermissionException {
		if (!hasAccesAsProducer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable;
	}

	public boolean hasExpiredItem(SapereAgent producerAgent) throws PermissionException {
		if (!hasAccesAsProducer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		return confirmationTable.hasExpiredItem();
	}

	public void cleanExpiredDate(SapereAgent producerAgent) throws PermissionException {
		if (!hasAccesAsProducer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		confirmationTable.cleanExpiredDate();
	}

	public void removeConfirmation(SapereAgent producerAgent, String receiver, boolean isComplementary) throws PermissionException {
		if (!hasAccesAsProducer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		confirmationTable.removeConfirmation(receiver, isComplementary);
	}

	public void confirm(SapereAgent producerAgent, String receiver, boolean isComplementary,  Boolean value, String comment, long timeShiftMS)
			throws PermissionException {
		if (!hasAccesAsProducer(producerAgent)) {
			throw new PermissionException("Access denied for agent " + producerAgent.getAgentName());
		}
		confirmationTable.confirm(receiver, isComplementary, value, comment, timeShiftMS);
	}
/*
	public Boolean getConfirmation(SapereAgent consumerAgent) throws PermissionException {
		if (!this.hasAccesAsConsumer(consumerAgent)) {
			throw new PermissionException("Access denied for agent " + consumerAgent.getAgentName());
		}
		return confirmationTable.getConfirmation(consumerAgent.getAgentName());
	}
*/

	public ConfirmationItem getConfirmationItem(SapereAgent consumerAgent, boolean isComplementary) throws PermissionException {
		if (!hasAccesAsConsumer(consumerAgent)) {
			throw new PermissionException("Access denied for agent " + consumerAgent.getAgentName());
		}
		return confirmationTable.getConfirmationItem(consumerAgent.getAgentName(), isComplementary);
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
		return "ProtectedConfirmationTable " + confirmationTable ;
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
