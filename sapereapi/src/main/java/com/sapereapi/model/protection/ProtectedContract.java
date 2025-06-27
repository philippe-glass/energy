package com.sapereapi.model.protection;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.referential.AgentType;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class ProtectedContract extends ProtectedObject {
	private static final long serialVersionUID = 5L;
	private Contract contract;
	public ProtectedContract(Contract contract) {
		super();
		this.contract = contract;
	}

	@Override
	protected boolean checkAccessAsIssuer(AgentAuthentication authentication) {
		boolean hasTypeConsumer = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (hasTypeConsumer	&& contract.getConsumerAgent().equals(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	@Override
	public boolean checkAccessAsReceiver(AgentAuthentication authentication) {
		boolean hasTypeProducer = AgentType.PROSUMER.name().equals(authentication.getAgentType());
		if (hasTypeProducer && contract.hasProducer(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	public Contract getContract(LearningAgent learningAgent) throws PermissionException {
		return getContract(learningAgent.getAuthentication());
	}

	public Contract getContract(SapereAgent consumerAgent) throws PermissionException {
		return getContract(consumerAgent.getAuthentication());
	}

	private Contract getContract(AgentAuthentication authentication) throws PermissionException {
		if (!(checkAccessAsIssuer(authentication)
				|| checkAccessAsLearningAgent(authentication)
				)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract;
	}

	public boolean hasProducer(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsStackholder(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.hasProducer(authentication.getAgentName());
	}

	public String getConsumerAgent() {
		return contract.getConsumerAgent();
	}

	public NodeLocation getConsumerLocation() {
		return contract.getConsumerLocation();
	}

	public boolean hasDisagreement() {
		return contract.hasDisagreement();
	}

	public boolean isComplementary() {
		return contract.isComplementary();
	}

	public boolean hasProducerAgreement(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsStackholder(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.hasAgreement(authentication.getAgentName());
	}

	public boolean hasProducerDisagreement(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsStackholder(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.hasDisagreement(authentication.getAgentName());
	}

	public void addProducerAgreement(SapereAgent prodAgent, boolean isOk) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsReceiver(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		contract.addAgreement(prodAgent, isOk);
	}

	public void producerStop(SapereAgent prodAgent) throws PermissionException {
		addProducerAgreement(prodAgent, false);
	}

	public PowerSlot getProducerPowerSlot(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsReceiver(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.getPowerSlotFromAgent(authentication.getAgentName());
	}

	public boolean hasAllAgreements() {
		return contract.hasAllAgreements();
	}

	public boolean hasExpired() {
		return contract.hasExpired();
	}

	public boolean waitingValidation() {
		return contract.isWaitingValidation();
	}

	public boolean isActive() {
		return contract.isActive();
	}

	public Date getValidationDeadline(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsStackholder(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.getValidationDeadline();
	}

	public boolean validationHasExpired(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsStackholder(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.validationHasExpired();
	}

	public ProtectedContract clone() {
		Contract contractClone = contract.clone();
		return new ProtectedContract(contractClone);
	}

	public ProtectedContract copyForLSA(AbstractLogger logger) {
		Contract contractClone = contract.copyForLSA(logger);
		return new ProtectedContract(contractClone);
	}

	public EnergyRequest getRequest() {
		return contract.getRequest();
	}

	public String getStatus() {
		return contract.getStatus();
	}

	@Override
	public String toString() {
		return contract.formatContent(false);
	}

	public boolean hasGap() {
		return contract.hasGap();
	}

	public int getIssuerDistance() {
		if(contract.getRequest().getIssuerProperties() == null) {
			return 0;
		}
		return contract.getRequest().getIssuerProperties() .getDistance();
	}

	public boolean isMerged() {
		return contract.isMerged();
	}

	public ReducedContract getProducerContent(SapereAgent agent) throws PermissionException {
		AgentAuthentication authentication = agent.getAuthentication();
		String agentName = authentication.getAgentName();
		if (!checkAccessAsReceiver(authentication) || !(agent instanceof EnergyAgent)) {
			throw new PermissionException("Access denied for agent " + agentName);
		}
		EnergyAgent producerAgent = (EnergyAgent) agent;
		if(producerAgent.getGlobalProduction() == null) {
			return null;
		}
		EnergySupply supply = producerAgent.getGlobalProduction().clone();
		PowerSlot producerPowerSlot = contract.getPowerSlotFromAgent(agentName);
		supply.setPowerSlot(producerPowerSlot.clone());
		supply.setBeginDate(contract.getBeginDate());
		supply.setEndDate(contract.getEndDate());
		ReducedContract result = new ReducedContract(contract.getRequest(), supply, contract.getValidationDeadline());
		Set<String> agreements = new HashSet<String>();
		Set<String> disagreements = new HashSet<String>();
		// Agreement/Disagreement of the agent
		if(contract.hasAgreement(agentName)) {
			agreements.add(agentName);
		}
		if(contract.hasDisagreement(agentName)) {
			disagreements.add(agentName);
		}
		// Agreement/disagreement of other agents
		boolean othersHasDisagreement = false;
		boolean othersHasAllAgreements = true;
		for(String nextAgentName : contract.getStakeholderAgents()) {
			if(!agentName.equals(nextAgentName)) {
				if(contract.hasDisagreement(nextAgentName)) {
					othersHasDisagreement = true;
					othersHasAllAgreements = false;
				}
				if(!contract.hasAgreement(nextAgentName)) {
					othersHasAllAgreements = false;
				}
			}
		}
		if(othersHasAllAgreements) {
			agreements.add(ReducedContract.ALL_OTHERS);
		}
		if(othersHasDisagreement) {
			disagreements.add(ReducedContract.ALL_OTHERS);
		}
		result.setAgreements(agreements);
		result.setDisagreements(disagreements);
		result.setMerged(contract.isMerged());
		return result;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		if(contract != null) {
			contract.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
		}
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		if(contract != null) {
			return contract.retrieveInvolvedLocations();
		}
		return new ArrayList<NodeLocation>();
	}

}
