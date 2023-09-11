package com.sapereapi.agent.energy;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.RescheduleTable;
import com.sapereapi.model.energy.policy.IEnergyAgentPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;

import eu.sapere.middleware.agent.AgentAuthentication;

public interface IEnergyAgent {
	void initFields(EnergySupply _globalSupply, IEnergyAgentPolicy agentPolicy);
	boolean isProducer();
	boolean isConsumer();
	void reinitialize(int _id, AgentAuthentication _authentication, EnergySupply _globalSupply, IEnergyAgentPolicy agentPolicy);
	boolean hasExpired();
	boolean isDisabled();
	boolean isActive();
	boolean isInActiveSlot(Date aDate);
	boolean isSatisfied();
	int getTimeLeftSec(boolean addWaitingBeforeStart);
	void disableAgent(RegulationWarning warning);
	void postEvent(EnergyEvent eventToPost);
	EnergyEvent generateStartEvent();
	EnergyEvent generateUpdateEvent(WarningType warningType);
	EnergyEvent generateExpiryEvent();
	EnergyEvent generateStopEvent(RegulationWarning warning, String comment);
	EventType getStartEventType();
	EventType getUpdateEventType();
	EventType getExpiryEventType();
	EventType getStopEventType();
	void initEnergySupply(EnergySupply supply);
	EnergySupply getEnergySupply();
	EnergyRequest getEnergyRequest();
	EnergyEvent getStopEvent();
	boolean tryReactivation();
	Set<String> getLinkedAgents();
	Map<String, PowerSlot> getOngoingContractsRepartition();
	PowerSlot getWaitingContratsPowerSlot();
	PowerSlot getForcastOngoingContractsPowerSlot(String location, Date aDate);
	PowerSlot getOngoingContractsPowerSlot(String locationFilter);
	Map<String, Double> getOffersRepartition();
	Double getOffersTotal();
	List<String> getConsumersOfWaitingContrats();
	Double computeAvailablePower();
	Double computeMissingPower();
	void handleWarning(RegulationWarning warning);
	void handleWarningOverConsumption(RegulationWarning warning);
	void handleWarningOverProduction(RegulationWarning warning);
	void handleWarningUserInteruption(RegulationWarning warning);
	void handleWarningGeneralInteruption(RegulationWarning warning);
	void handleWarningChangeRequest(RegulationWarning warning);
	void handleReschedule(RescheduleTable rescheduleTable);
	void cleanExpiredData();
	String getLocation();
	String getNodeName();
	IProducerPolicy getProducerPolicy();
}