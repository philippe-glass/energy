package com.sapereapi.agent.energy;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.award.AwardsTable;
import com.sapereapi.model.energy.policy.IEnergyAgentPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.reschedule.RescheduleTable;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.model.referential.WarningType;

import eu.sapere.middleware.agent.AgentAuthentication;

public interface IEnergyAgent {
	void initFields(EnergySupply globalSupply, EnergyRequest need, EnergyStorageSetting energyStorageSetting, IEnergyAgentPolicy producerPolicy, IEnergyAgentPolicy consumerPolicy, NodeContext nodeContext);
	boolean isProducer();
	boolean isConsumer();
	void reinitialize(int id, AgentAuthentication authentication, EnergySupply globalSupply, EnergyRequest need, EnergyStorageSetting energyStorageSetting
			, IEnergyAgentPolicy consumerPolicy, IEnergyAgentPolicy producerPolicy, NodeContext nodeContext);
	boolean hasExpired();
	boolean isDisabled();
	boolean isActive();
	boolean isInActiveSlot(Date aDate);
	boolean isSatisfied();
	int getTimeLeftSec(boolean addWaitingBeforeStart);
	void disableAgent(RegulationWarning warning) throws HandlingException;
	void postEvent(EnergyEvent eventToPost);
	EnergyEvent generateStartEvent() throws HandlingException ;
	EnergyEvent generateUpdateEvent(WarningType warningType, String comment) throws HandlingException;
	EnergyEvent generateExpiryEvent() throws HandlingException;
	EnergyEvent generateStopEvent(RegulationWarning warning, String comment) throws HandlingException;
	EventType getStartEventType();
	EventType getUpdateEventType();
	EventType getSwitchEventType();
	EventType getExpiryEventType();
	EventType getStopEventType();
	void initGlobalProduction(EnergySupply supply);
	EnergySupply getGlobalProduction();
	EnergyRequest getGlobalNeed();
	EnergyEvent getStopEvent();
	boolean tryReactivation() throws HandlingException;
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
	Double getStoredWH();
	Double getStorageUsedForNeed();
	Double getStorageUsedForProd();
	void handleWarning(RegulationWarning warning) throws HandlingException;
	void handleWarningOverConsumption(RegulationWarning warning) throws HandlingException;
	void handleWarningOverProduction(RegulationWarning warning) throws HandlingException;
	void handleWarningUserInteruption(RegulationWarning warning) throws HandlingException;
	void handleWarningGeneralInteruption(RegulationWarning warning) throws HandlingException;
	void handleWarningChangeRequest(RegulationWarning warning) throws HandlingException;
	void handleAwards(AwardsTable awardsTable) throws HandlingException;
	void handleReschedule(RescheduleTable rescheduleTable) throws HandlingException;
	void cleanExpiredData() throws HandlingException;
	String getLocation();
	String getNodeName();
	IProducerPolicy getProducerPolicy();
	void setEventId(long eventId);
	void setBeginDate(Date aDate);
	void setEndDate(Date aDate);
	void setDisabled(boolean bDisabled);
	EnergyEvent generateEvent(EventType eventType, String log);
	EnergyStorageSetting getStorageSetting();
	ProsumerRole computeRole();
}