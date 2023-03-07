package com.sapereapi.model.protection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.referential.AgentType;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class ProtectedContract extends ProtectedObject implements Serializable, IAggregateable {
	private static final long serialVersionUID = 5L;
	private Contract contract;
	public ProtectedContract(Contract contract) {
		super();
		this.contract = contract;
	}

	public boolean checkAccessAsProducer(AgentAuthentication authentication) {
		if (AgentType.PRODUCER.getLabel().equals(authentication.getAgentType())
				&& contract.hasProducer(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	protected boolean checkAccessAsConsumer(AgentAuthentication authentication) {
		if (AgentType.CONSUMER.getLabel().equals(authentication.getAgentType())
				&& contract.getConsumerAgent().equals(authentication.getAgentName())) {
			return checkAuthentication(authentication);
		}
		return false;
	}

	protected boolean checkAccessAsStakeholder(AgentAuthentication authentication) {
		if (AgentType.CONSUMER.getLabel().equals(authentication.getAgentType())) {
			return checkAccessAsConsumer(authentication);
		} else if (AgentType.PRODUCER.getLabel().equals(authentication.getAgentType())) {
			return checkAccessAsProducer(authentication);
		}
		return false;
	}

	protected boolean checkAccessAsLearningAgent(AgentAuthentication authentication) {
		if (AgentType.LEARNING_AGENT.getLabel().equals(authentication.getAgentType())) {
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
		if (!(checkAccessAsConsumer(authentication)
				|| checkAccessAsLearningAgent(authentication)
				)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract;
	}

	public boolean hasAccesAsConsumer(SapereAgent consumerAgent) {
		return checkAccessAsConsumer(consumerAgent.getAuthentication());
	}

	public boolean hasAccesAsProducer(SapereAgent prodAgent) {
		return checkAccessAsProducer(prodAgent.getAuthentication());
	}

	public boolean hasProducer(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsProducer(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.hasProducer(authentication.getAgentName());
	}

	public String getConsumerAgent() {
		return contract.getConsumerAgent();
	}

	public String getConsumerLocation() {
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
		if (!checkAccessAsProducer(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.hasAgreement(authentication.getAgentName());
	}

	public boolean hasProducerDisagreement(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsProducer(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.hasDisagreement(authentication.getAgentName());
	}

	public void addProducerAgreement(SapereAgent prodAgent, boolean isOk) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsProducer(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		contract.addAgreement(prodAgent, isOk);
	}

	public void producerStop(SapereAgent prodAgent) throws PermissionException {
		addProducerAgreement(prodAgent, false);
	}

	public PowerSlot getProducerPowerSlot(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsProducer(authentication)) {
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
		if (!checkAccessAsProducer(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.getValidationDeadline();
	}

	public boolean validationHasExpired(SapereAgent prodAgent) throws PermissionException {
		AgentAuthentication authentication = prodAgent.getAuthentication();
		if (!checkAccessAsProducer(authentication)) {
			throw new PermissionException("Access denied for agent " + authentication.getAgentName());
		}
		return contract.validationHasExpired();
	}

	public ProtectedContract clone() {
		Contract contractClone = contract.clone();
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
		return contract.getRequest().getIssuerDistance();
	}

	public boolean isMerged() {
		return contract.isMerged();
	}

	public ReducedContract getProducerContent(SapereAgent agent) throws PermissionException {
		AgentAuthentication authentication = agent.getAuthentication();
		String agentName = authentication.getAgentName();
		if (!checkAccessAsProducer(authentication) || !(agent instanceof EnergyAgent)) {
			throw new PermissionException("Access denied for agent " + agentName);
		}
		EnergyAgent producerAgent = (EnergyAgent) agent;
		EnergySupply supply = producerAgent.getEnergySupply().clone();
		PowerSlot producerPowerSlot = contract.getPowerSlotFromAgent(agentName);
		supply.setPower(producerPowerSlot.getCurrent());
		supply.setPowerMin(producerPowerSlot.getMin());
		supply.setPowerMax(producerPowerSlot.getMax());
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

	public List<IAggregateable> auxRetriveListContracts(List<IAggregateable> listObjects, AgentAuthentication authentication)  {
		List<IAggregateable> listContracts = new ArrayList<IAggregateable>();
		try {
			for(IAggregateable nextObj : listObjects) {
				if(nextObj instanceof ProtectedContract) {
					ProtectedContract nextProtectedContract = (ProtectedContract) nextObj;
					Contract nextContract = nextProtectedContract.getContract(authentication);
					if(!nextContract.isComplementary() && nextContract.isOnGoing()) {
						listContracts.add(nextContract);
					}
				}
			}
		} catch (PermissionException e) {
			SapereLogger.getInstance().error(e);
			return new ArrayList<>();
		}
		return listContracts;
	}

	public ProtectedContract aggregate(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication) {
		List<IAggregateable> listContracts = auxRetriveListContracts(listObjects, agentAuthentication);
		if(listContracts.size() == 0) {
			return null;
		}
		Contract firstContract = (Contract) listContracts.get(0);
		Contract resultContract = firstContract.aggregate(operator, listContracts, agentAuthentication);
		if(resultContract != null) {
			return new ProtectedContract(resultContract);
		}
		return null;
	}
}
