package com.sapereapi.agent.energy.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.exception.PermissionException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.ConfirmationTable;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.protection.ProtectedConfirmationTable;
import com.sapereapi.model.protection.ProtectedContract;
import com.sapereapi.model.protection.ProtectedSingleOffer;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;

public class ConsumersProcessingMangager {
	private ConsumersProcessingTable mainProcessingTable = null;
	private ConsumersProcessingTable secondProcessingTable = null;
	private static SapereLogger logger = SapereLogger.getInstance();
	private boolean activateOfferRemoveLog = true;
	private boolean showWaitingOffers = false; 	// used for debug to display waiting offers on LSA in WAITING_OFFERS  property
	//private boolean showAgreements = false; 	// used for debug to display confirmations to consumer agents in AGREEMENTS   property
	private Map<String, EnergyRequest> tableBondedRequests = null;
	private Map<String, List<String>> logBondedRequests = null;
	private boolean activateLogOnBondedRequests = false;
	private long timeShiftMS = 0;
	public final static int OFFER_VALIDITY_SECONDS = 8;
	public final static int OFFER_EXPIRATION_MARGIN_SEC = 1;


	public final static int WARNING_DURATION_THRESHOLD = 12;


	public ConsumersProcessingMangager(EnergyAgent producerAgent) {
		timeShiftMS = producerAgent.getTimeShiftMS();
		this.mainProcessingTable = new ConsumersProcessingTable(false, timeShiftMS);
		this.secondProcessingTable = new ConsumersProcessingTable(true, timeShiftMS);
		//this.receivedConfirmations = new HashMap<String, ConfirmationItem>();
		this.tableBondedRequests = new HashMap<String, EnergyRequest>();
		this.logBondedRequests = new HashMap<String,List<String>>();
	}

	public PowerSlot getWaitingContratsPowerSlot(EnergyAgent producerAgent) {
		return SapereUtil.add(
			mainProcessingTable.computePowerOfWaitingContrats(producerAgent),
			secondProcessingTable.computePowerOfWaitingContrats(producerAgent)
			);
	}

	public List<String> getConsumersOfWaitingContrats() {
		return SapereUtil.mergeListStr(
			mainProcessingTable.getConsumersOfWaitingContrats(),
			secondProcessingTable.getConsumersOfWaitingContrats());
	}

	public Set<String> getLinkedAgents() {
		return SapereUtil.mergeSetStr(
			mainProcessingTable.getLinkedAgents(),
			secondProcessingTable.getLinkedAgents());
	}

	public PowerSlot getForcastOngoingContractsPowerSlot(String location, EnergyAgent producerAgent, Date aDate) {
		return SapereUtil.add(
			mainProcessingTable.getForcastOngoingContractsPowerSlot(location, producerAgent, aDate),
			secondProcessingTable.getForcastOngoingContractsPowerSlot(location, producerAgent, aDate));
	}

	public PowerSlot getOngoingContractsPowerSlot(String location, EnergyAgent producerAgent) {
		return SapereUtil.add(
			mainProcessingTable.getOngoingContractsPowerSlot(location, producerAgent),
			secondProcessingTable.getOngoingContractsPowerSlot(location, producerAgent));
	}

	public Map<String, PowerSlot> getOngoingContractsRepartition(EnergyAgent producerAgent) {
		return SapereUtil.mergeMapStrPowerSlot(
			mainProcessingTable.getOngoingContractsRepartition(producerAgent),
			secondProcessingTable.getOngoingContractsRepartition(producerAgent));
	}

	public Double getOffersTotal() {
		return mainProcessingTable.getOffersTotal()
			+ secondProcessingTable.getOffersTotal();
	}

	public Map<String, Double> getOffersRepartition() {
		return SapereUtil.mergeMapStrDouble(
			mainProcessingTable.getOffersRepartition(),
			secondProcessingTable.getOffersRepartition());
	}

	public void removeConsumer(String consumer, boolean isComplementary, String logTag) {
		if(isComplementary) {
			secondProcessingTable.removeConsumer(consumer, logTag);
		} else {
			mainProcessingTable.removeConsumer(consumer, logTag);
		}
	}

	public void removeConsumer(String consumer, String logTag) {
		// remove consumer in all processing tables
		mainProcessingTable.removeConsumer(consumer, logTag);
		secondProcessingTable.removeConsumer(consumer, logTag);
	}

	public Map<String, List<SingleOffer>> getTableWaitingOffers() {
		return SapereUtil.mergeMapStrOffer(
			mainProcessingTable.getTableWaitingOffers(),
			secondProcessingTable.getTableWaitingOffers(), logger);
	}

	public Collection<SingleOffer> getWaitingOffers() {
		return SapereUtil.mergeCollectionOffers(
			mainProcessingTable.getTableWaitingOffers().values(),
			secondProcessingTable.getTableWaitingOffers().values());
	}

	public Map<String, List<ReducedContract>> getTableValidContracts() {
		return SapereUtil.mergeMapStrRContract(
			mainProcessingTable.getTableValidContracts(),
			secondProcessingTable.getTableValidContracts(), logger);
	}

	public Map<String, List<EnergyRequest>> getTableWaitingRequest() {
		return SapereUtil.mergeMapStrRequest(
				mainProcessingTable.getTableWaitingRequest(),
				secondProcessingTable.getTableWaitingRequest(), logger);
	}

	public void checkWaitingRequests() {
		Map<String, EnergyRequest> mainRequests = mainProcessingTable.getTableWaitingRequest();
		Map<String, EnergyRequest> secondaryRequests = secondProcessingTable.getTableWaitingRequest();
		for(String issuer: mainRequests.keySet()) {
			if(secondaryRequests.containsKey(issuer)) {
				logger.error("checkRequests " + issuer + " has both main and secondary requests " + mainRequests.get(issuer) + " and " + secondaryRequests.get(issuer));
				secondaryRequests.remove(issuer);
			}
		}
	}
	public Collection<EnergyRequest> getWaitingRequest() {
		return SapereUtil.mergeCollectionRequests(
				mainProcessingTable.getTableWaitingRequest().values(),
				secondProcessingTable.getTableWaitingRequest().values());
	}

	public Collection<ReducedContract> getValidContracts() {
		return SapereUtil.mergeCollectionContracts(
			mainProcessingTable.getValidContracts(),
			secondProcessingTable.getValidContracts());
	}

	public boolean hasWaitingRequest(String consumer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingTable.hasWaitingRequest(consumer);
		} else {
			return mainProcessingTable.hasWaitingRequest(consumer);
		}
	}

	public boolean hasWaitingOffer(String consumer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingTable.hasWaitingOffer(consumer);
		} else {
			return mainProcessingTable.hasWaitingOffer(consumer);
		}
	}

	public EnergyRequest getWaitingRequest(String consumer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingTable.getWaitingRequest(consumer);
		} else {
			return mainProcessingTable.getWaitingRequest(consumer);
		}
	}

	public boolean hasContract(String consumer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingTable.hasContract(consumer);
		} else {
			return mainProcessingTable.hasContract(consumer);
		}
	}

	public boolean hasValidContract(String consumer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingTable.hasValidContract(consumer);
		} else {
			return mainProcessingTable.hasValidContract(consumer);
		}
	}

	public boolean hasCanceledContract(String consumer, boolean isComplementary) {
		if(isComplementary) {
			return secondProcessingTable.hasCanceledContract(consumer);
		} else {
			return mainProcessingTable.hasCanceledContract(consumer);
		}
	}

	public void removeContract(EnergyAgent producerAgent, String consumer, boolean isComplementary, String logTag) {
		if(isComplementary) {
			secondProcessingTable.removeContract(producerAgent, consumer, logTag);
		} else {
			mainProcessingTable.removeContract(producerAgent, consumer, logTag);
		}
	}

	public void addOrUpdateRequest(EnergyAgent producerAgent, String consumer, EnergyRequest request) {
		if(request.isComplementary()) {
			// For debug
			if(mainProcessingTable.hasWaitingRequest(consumer)) {
				EnergyRequest mainRequest = mainProcessingTable.getWaitingRequest(consumer);
				logger.error("addOrUpdateRequest " + producerAgent.getAgentName() + " add complementary request " +request + " but mainProcessingTable already contains the request " + mainRequest);				
			} // end for debug
			secondProcessingTable.addOrUpdateRequest(producerAgent, consumer, request);
		} else {
			if(secondProcessingTable.hasWaitingRequest(consumer)) {
				EnergyRequest secondRequest = secondProcessingTable.getWaitingRequest(consumer);
				logger.warning("addOrUpdateRequest " + producerAgent.getAgentName() + " add main request " +request + " but secondProcessingTable already contains the request " + secondRequest);
				if(!secondRequest.getBeginDate().after(request.getBeginDate())) {
					// a complementary is already present for the same issuer: remove the complementary request for this issuer
					logger.info("addOrUpdateRequest : remove second request " + secondRequest);
					secondProcessingTable.removeConsumer(consumer, "Presence of a main request and a second request: delete the oldest request");
				} else {
					logger.info("addOrUpdateRequest : cannot remove the second request " + secondRequest+ " : the secondary request is more recent thant the request to add");
				}
				//.cleanExpiredData(producerAgent, producerAgent.getDebugLevel());
			} // end for debug
			mainProcessingTable.addOrUpdateRequest(producerAgent, consumer, request);
		}
	}

	public void addBondedRequest(Lsa bondedLsa) {
		Object oRequested = bondedLsa.getOnePropertyValueByName("REQ");
		 if (oRequested != null && oRequested instanceof EnergyRequest) {
			 String agentName = bondedLsa.getAgentName();
			 tableBondedRequests.put(agentName, (EnergyRequest) oRequested);
			 if(activateLogOnBondedRequests) {
				 addLogReceivedRequest(agentName, oRequested);
			 }
		 }
	}

	public void addLogReceivedRequest(String agentName, Object oRequested) {
		 if (activateLogOnBondedRequests && oRequested != null && oRequested instanceof EnergyRequest) {
			 int logMaxSize = 10;
			 String time = UtilDates.format_time.format(getCurrentDate());
			 String newEntry = time + " : " + oRequested;
			 if(!logBondedRequests.containsKey(agentName)) {
				 logBondedRequests.put(agentName, new ArrayList<String>());
			 }
			 List<String> listRequests = logBondedRequests.get(agentName);
			 listRequests.add(0,newEntry);
			 while(listRequests.size()>logMaxSize) {
				 listRequests.remove(logMaxSize);
			 }
		 }
	}

	public String getStrLogBondedRequests(String concumer) {
		StringBuffer logRequestReception = new StringBuffer();
		if(activateLogOnBondedRequests && logBondedRequests.containsKey(concumer)) {
			List<String> bondedRequests = logBondedRequests.get(concumer);
			for(String req : bondedRequests ) {
				logRequestReception.append(SapereUtil.CR).append("   ").append(req);
			}
		}
		return logRequestReception.toString();
	}

	public void refreshWaitingOffersProperty(EnergyAgent producerAgent) {
		if(showWaitingOffers) {
			Collection<SingleOffer> tableWaitingOffers = getWaitingOffers();
			if(tableWaitingOffers!=null) {
				producerAgent.replacePropertyWithName(new Property("DEBUG_WAITING_OFFERS", tableWaitingOffers.toString()));
			}
		}
	}

	public void addOffer(EnergyAgent producerAgent, SingleOffer newOffer) throws HandlingException {
		String consumer = newOffer.getConsumerAgent();
		String ipSource = producerAgent.getIpSource();
		removeOffer(producerAgent, consumer, newOffer.isComplementary(), "addOffer");
		// add offer in OFFER property
		ProtectedSingleOffer protectedOffer = new ProtectedSingleOffer(newOffer.clone());
		Property pOffer = new Property("OFFER", protectedOffer.clone(), consumer, consumer, "",	ipSource, false);
		producerAgent.addProperty(pOffer);
		// add offer in WAITING_OFFERS property
		if(newOffer.isComplementary()) {
			secondProcessingTable.addOffer(producerAgent, newOffer);
		} else {
			mainProcessingTable.addOffer(producerAgent, newOffer);
		}
		refreshWaitingOffersProperty(producerAgent);

		checkup(producerAgent);
	}

	public void removeOffer(EnergyAgent producerAgent, String consumer, boolean isComplementary, String logTag) throws HandlingException {
		// For debug
		debug_checkOfferAcquitted(producerAgent, consumer);
		producerAgent.getLsa().removePropertiesByQueryAndName(consumer, "OFFER");
		// Remove the offer from WAITING_OFFERS property
		if(isComplementary) {
			secondProcessingTable.removeOffer(producerAgent, consumer, logTag, activateOfferRemoveLog );
		} else {
			mainProcessingTable.removeOffer(producerAgent, consumer, logTag, activateOfferRemoveLog );
		}
		refreshWaitingOffersProperty(producerAgent);
	}

	public void debug_checkOfferAcquitted(EnergyAgent producerAgent, String consumer) throws HandlingException {
		ProtectedSingleOffer protectedOffer = (ProtectedSingleOffer) producerAgent.getLsa().getOnePropertyValueByQueryAndName(consumer, "OFFER");
		if(protectedOffer!=null && protectedOffer.hasAccessAsIssuer(producerAgent)) {
			try {
				SingleOffer offer = protectedOffer.getSingleOffer(producerAgent);
				if(offer!=null) {
					boolean isAcquitted = EnergyDbHelper.isOfferAcuitted(offer.getId());
					if(!isAcquitted) {
						// The offer is not acquitted
						logger.warning(producerAgent.getAgentName() + " debug_checkOfferAcquitted : the offer " + offer + " is not acquitted");
					}
				}
			} catch (PermissionException e) {
				logger.error(e);
			}
		}
	}

	public void checkup(EnergyAgent produceragent) {
		double avl = computeAvailablePower(produceragent, false, false);
		if(avl<-0.001) {
			logger.warning("### " +  produceragent.getAgentName() + " availability = " + avl);
			logger.warning("For debug tableWaitingOffers =  " + getWaitingOffers());
		}
	}

	public void updateContract(EnergyAgent producerAgent, ReducedContract aContract, String tag) {
		if(aContract==null) {
			return;
		}
		if(aContract.isMain()) {
			mainProcessingTable.updateContract(producerAgent, aContract, tag);
		} else {
			secondProcessingTable.updateContract(producerAgent, aContract, tag);
		}
		checkup(producerAgent);
	}

	public void cleanExpiredData(EnergyAgent producerAgent, int debugLevel) {
		// Clean waiting request
		this.mainProcessingTable.cleanExpiredData(producerAgent,debugLevel, tableBondedRequests);
		// in the second table, each element should be removed if there is no contract is the main table
		//this.secondProcessingTable.cleanComplementaryDataNotInMainTable(mainProcessingTable);
		this.secondProcessingTable.cleanExpiredData(producerAgent,debugLevel, tableBondedRequests);

		// Clean // clean last bonded request
		tableBondedRequests.clear();
	}

	public void stopAllContracts(EnergyAgent producerAgent, String log) {
		// Stop validated and waiting contracts
		mainProcessingTable.stopAllContracts(producerAgent, log);
		secondProcessingTable.stopAllContracts(producerAgent, log);
		checkup(producerAgent);
	}

	/**
	 * Send FALSE confirmation to all consumers of waiting offer (in case when the consumer start a new contract)
	 * @param producerAgent
	 * @param log
	 */
	public void cancelAllWaitingOffers(EnergyAgent producerAgent, String log) throws HandlingException {
		mainProcessingTable.cancelAllWaitingOffers(producerAgent, log, activateOfferRemoveLog);
		secondProcessingTable.cancelAllWaitingOffers(producerAgent, log, activateOfferRemoveLog);
		checkup(producerAgent);
	}

	public ReducedContract getReducedContract(String consumer, boolean isComplementary) {
		if(isComplementary) {
			if(secondProcessingTable.hasContract(consumer)) {
				return secondProcessingTable.getReducedContract(consumer);
			}
		} else {
			if(mainProcessingTable.hasContract(consumer)) {
				return mainProcessingTable.getReducedContract(consumer);
			}
		}
		return null;
	}

	public void stopContracts(EnergyAgent producerAgent, String consumer, String log) {
		// Stop both main an complementary contracts
		stopContract(producerAgent, consumer, false, log);
		stopContract(producerAgent, consumer, true, log);
	}

	public void stopContract(EnergyAgent producerAgent, String consumer, boolean isComplementary, String log) {
		ReducedContract reducedContract = getReducedContract(consumer, isComplementary);
		if(reducedContract!=null) {
			try {
				if (!reducedContract.hasDisagreement()) {
					reducedContract.addProducerAgreement(false);
					logger.info(producerAgent.getAgentName()+ " stopContract : _reducedContract = " + reducedContract + ", log = " + log);
					// For debug
					if(!isComplementary) {
						logger.info(producerAgent.getAgentName()+ " stopContract : secondary contract " + secondProcessingTable.getReducedContract(consumer));
					}
				}
				if(isComplementary) {
					secondProcessingTable.removeContract(producerAgent, consumer, log);
				} else {
					mainProcessingTable.removeContract(producerAgent, consumer, log);
				}
				if (reducedContract != null) {
					sendConfirmation(producerAgent, reducedContract, false, producerAgent.getAgentName() + " : " + log);
					checkup(producerAgent);
				}
				// Stop the complementary contract if a main contract is stopped
				if(!isComplementary) {
					if(secondProcessingTable.hasValidContract(consumer) || secondProcessingTable.hasWaitingValidationContract(consumer)) {
						logger.info(producerAgent.getAgentName()+ " stopContract : stop also the secondary contract " + secondProcessingTable.getReducedContract(consumer));
						stopContract(producerAgent, consumer, true, producerAgent.getAgentName() + " : the main contract has been stopped");
					}
				}
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	public void cleanConfirmationTable(EnergyAgent producerAgent) {
		ProtectedConfirmationTable protectedConfirmationTable = producerAgent.getProtectedConfirmationTable(producerAgent);
		try {
			// Renew confirmations if necessary
			boolean updated = false;
			if(protectedConfirmationTable.hasExpiredItem(producerAgent)) {
				protectedConfirmationTable.cleanExpiredDate(producerAgent);
				updated = true;
			}
			updated = updated || protectedConfirmationTable.renewConfirmations(producerAgent);
			if(updated) {
				producerAgent.replacePropertyWithName(new Property("CONTRACT_CONFIRM", protectedConfirmationTable));
			}
		} catch (PermissionException e) {
			logger.error(e);
		}
	}
/*
	public ProtectedConfirmationTable getProtectedConfirmationTable(EnergyAgent producerAgent) {
		Property pProdConfirm = producerAgent.getLsa().getOnePropertyByName("CONTRACT_CONFIRM");
		if(pProdConfirm!=null && pProdConfirm.getValue() instanceof ProtectedConfirmationTable) {
			return  (ProtectedConfirmationTable) pProdConfirm.getValue();
		}
		ConfirmationTable confirmationTable = new ConfirmationTable(producerAgent.getAgentName(), producerAgent.computeRole());
		return new ProtectedConfirmationTable(confirmationTable);
	}
*/
	public void addReceivedConfirmation(EnergyAgent producerAgent, String consumer) {
		mainProcessingTable.addReceivedConfirmation(producerAgent, consumer);
		secondProcessingTable.addReceivedConfirmation(producerAgent, consumer);
	}

	public Set<String> getConsumersWithNoRecentConfirmations(EnergyAgent producerAgent) {
		return SapereUtil.mergeSetStr(
			mainProcessingTable.getConsumersWithNoRecentConfirmations(producerAgent),
			secondProcessingTable.getConsumersWithNoRecentConfirmations(producerAgent));
	}

	public void checkReceivedConfirmations(EnergyAgent producerAgent) {
		// Check received confirmation from consumers
		for(String consumer : getConsumersWithNoRecentConfirmations(producerAgent)) {
			checkConsumerInSpace(producerAgent,consumer);
		}
	}

	public void confirmAvailabilityToConsumers(EnergyAgent producerAgent) {
		// Check up
		double availability = computeAvailablePower(producerAgent,false, false);
		String comment = "";
		boolean isOK = availability >= -0.001;
		if(!isOK) {
			comment = producerAgent.getAgentName() + " : availability not sufficient in checkup : W=" + UtilDates.df3.format(availability);
			logger.warning("general checkup " + comment );
		}
		// Send confirmation to consumer agents
		ProtectedConfirmationTable protectedConfirmationTable = producerAgent.getProtectedConfirmationTable(producerAgent);
		try {
			ConfirmationTable confirmationTable = protectedConfirmationTable.getConfirmationTable(producerAgent);
			for(ReducedContract reducedContract : getValidContracts()) {
				String consumerAgent = reducedContract.getConsumerAgent();
				if(!confirmationTable.hasReceiver(consumerAgent)) {
					sendConfirmation(producerAgent,reducedContract, isOK, comment);
				}
			}
		} catch (PermissionException e) {
			logger.error(e);
		}
	}

	public ConfirmationItem getSentConfirmationItem(EnergyAgent producerAgent, String consumer, boolean isComplementary, ProsumerRole role) {
		ProtectedConfirmationTable protectedConfirmationTable = producerAgent.getProtectedConfirmationTable(producerAgent);
		try {
			ConfirmationTable confirmationTable = protectedConfirmationTable.getConfirmationTable(producerAgent);
			return confirmationTable.getConfirmationItem(consumer, isComplementary, role);
		} catch (PermissionException e) {
			logger.error(e);
		}
		return null;
	}

	public boolean hasAlreadyInvalidation(EnergyAgent producerAgent, String consumer, boolean isComplementary) {
		return hasAlreadyInvalidation(producerAgent, consumer, isComplementary, ProsumerRole.CONSUMER)
			|| hasAlreadyInvalidation(producerAgent, consumer, isComplementary, ProsumerRole.PRODUCER);
	}

	public boolean hasAlreadyInvalidation(EnergyAgent producerAgent, String consumer, boolean isComplementary, ProsumerRole role) {
		ConfirmationItem confirmationItem = getSentConfirmationItem(producerAgent, consumer, isComplementary, role);
		if(confirmationItem!=null) {
			Date currentDate = UtilDates.getCurrentSeconde(timeShiftMS);
			if(confirmationItem.getDate().getTime() >= currentDate.getTime()) {
				//logger.info("hasAlreadyInvalidation confirmationItem.getDate() = " + SapereUtil.format_time.format(confirmationItem.getDate().getTime())
				//		+ ", currentDate = " + SapereUtil.format_time.format(currentDate.getTime()));
				return Boolean.FALSE.equals(confirmationItem.getIsOK());
			}
		}
		return false;
	}

	public void sendConfirmation(EnergyAgent producerAgent, ReducedContract reducedContract, boolean ok, String comment) {
		try {
			ProtectedConfirmationTable protectedConfirmationTable = producerAgent.getProtectedConfirmationTable(producerAgent);
			protectedConfirmationTable.confirmAsProducer(producerAgent, reducedContract.getConsumerAgent(), reducedContract.isComplementary(), ok, comment, 0);
			producerAgent.replacePropertyWithName(new Property("CONTRACT_CONFIRM", protectedConfirmationTable));
		} catch (PermissionException e) {
			logger.error(e);
		}
		if(!ok) {
			// Remove the consumer agent from the received confirmation
			boolean isComplementary = reducedContract.isComplementary();
			String consumer = reducedContract.getConsumerAgent();
			// Remove confirmations for the complementary contract
			this.secondProcessingTable.removeConfirmation(consumer);
			if(!isComplementary) {
				// remove confirmations for the main contract
				this.mainProcessingTable.removeConfirmation(consumer);;
			}
		}
	}

	public void checkConsumerInSpace(EnergyAgent producerAgent, String consumerName) {
		if(!Sapere.getInstance().isInSpace(consumerName)) {
			// Stop the contracts
			stopContracts(producerAgent,consumerName, consumerName + " agent is not in space.");
		}
	}

	public boolean confirmAgreementToConsumer(EnergyAgent producerAgent, ReducedContract newContract) {
		if(newContract.isComplementary()) {
			logger.info("confirmAgreementToConsumer : for debug");
		}
		ConfirmationItem checkItem = checkAvailabilityAndExpiration(producerAgent, newContract);
		try {
			if (!checkItem.getIsOK()) {
				logger.info("checkAvailabilityAndExpiration "  + producerAgent.getAgentName() +  " isOK1 =" + checkItem);
				Double availablePower = computeAvailablePower(producerAgent, false, false);	// Do not ignore offers and waiting contracts
				logger.info("confirmAgreementToContractAgent"  + producerAgent.getAgentName() +  " Remain =" + availablePower + " contractPower = " + newContract.getProducerPowerSlot());
				//boolean forDebug = checkAvailabilityAndExpiration(newContract);
			}
			if(checkItem.getIsOK()) {
				// ALL IS OK
				if(!newContract.hasProducerAgreement()) {
					newContract.addProducerAgreement(true);
					sendConfirmation(producerAgent, newContract, true, "");
				} else if (newContract.hasAllAgreements()) {
					sendConfirmation(producerAgent, newContract, true, "");
				}
			} else {
				// KO
				if(!newContract.hasProducerDisagreement()) {
					newContract.addProducerAgreement(false);
					sendConfirmation(producerAgent, newContract, false, checkItem.getComment());
				}
			}
		} catch (Throwable e) {
			logger.error(e);
		}
		checkup(producerAgent);
		return checkItem.getIsOK();
	}

	public void logContent1(EnergyAgent producerAgent) {
		double available1 = computeAvailablePower(producerAgent,true, false);
		if(available1 > 0) {
			String agentName = producerAgent.getAgentName();
			logger.info("Agent " + agentName + " has " + UtilDates.df3.format(available1) + " W");
			logger.info("  waiting request " + getTableWaitingRequest().keySet()
					+ " main warning request " + mainProcessingTable.generateTableRequestWarnings().keySet()
					+ " complementary warning request " + secondProcessingTable.generateTableRequestWarnings().keySet());
		}
	}

	public Double computeAvailablePower(EnergyAgent produceragent, boolean ignoreOffers, boolean ignoreWaitingContracts) {
		EnergySupply supply = produceragent.getGlobalProduction();
		if(supply != null) {
			return supply.getPower()
					- mainProcessingTable.computeUsedPower(produceragent, ignoreOffers, ignoreWaitingContracts)
					- secondProcessingTable.computeUsedPower(produceragent, ignoreOffers, ignoreWaitingContracts)
					;
		}
		return 0.0;
	}

	public ConfirmationItem checkAvailabilityAndExpiration(EnergyAgent producerAgent, ReducedContract reducedContract) {
		String agentName = producerAgent.getAgentName();
		String consumer = reducedContract.getIssuer();
		String comment = "";
		Boolean isComplementary = reducedContract.isComplementary();
		// TO DELETE !!!
		int __foo = 0;
		if(__foo>0) {
			return new ConfirmationItem(agentName, ProsumerRole.PRODUCER, consumer, isComplementary, false, agentName + " Foo test", 0, timeShiftMS);
		}
		if(reducedContract.validationHasExpired()) {
			comment = agentName + ": contract validation has expired : " + reducedContract;
			logger.warning("checkAvailabilityAndExpiration " + agentName + ": contract validation has expired : " + reducedContract);
			return new ConfirmationItem(agentName, ProsumerRole.PRODUCER, consumer, isComplementary, false, comment, 0, timeShiftMS);
		}
		Double availablePower = computeAvailablePower(producerAgent,false, false);	// Do not ignore offers and waiting contracts
		boolean isContractAlreadyInTable = hasContract(reducedContract.getConsumerAgent(), isComplementary);
		boolean isOK = false;
		double powerLeft = 0;
		PowerSlot contractPowerSlot = new PowerSlot();
		powerLeft = availablePower;
		if(isContractAlreadyInTable) {
			// do nothing
		} else {
			contractPowerSlot = reducedContract.getProducerPowerSlot();
			// subtract the contract max power (the contract is not in table)
			powerLeft = powerLeft -  contractPowerSlot.getMax() ;
		}
		isOK = (powerLeft >=-0.001);
		if(!isOK) {
			double missing = -1*powerLeft;
			comment = agentName + " availablePower not sufficient (availablePower = " + UtilDates.df5.format(availablePower)
					+ " isContractAlreadyInTable=" +isContractAlreadyInTable
					+  " ,missing = " + UtilDates.df5.format(missing);
			if(!isContractAlreadyInTable) {
				double contractMargin = contractPowerSlot.getMax() - contractPowerSlot.getCurrent();
				comment = comment +  ", contractPower = " + contractPowerSlot + ", contractMargin = " + UtilDates.df5.format(contractMargin);
			}
			comment = comment + ")";
			logger.warning("checkAvailabilityAndExpiration " + comment);
		}
		// ONLY FOR DEBUG
		/*
		if("__Prod_N1_1".equals(agentName)) {
			logger.info("checkAvailabilityAndExpiration availablePower = " +  SapereUtil.df2.format(availablePower) + " isContractAlreadyInTable=" +isContractAlreadyInTable 
				+  " ,missing = " + SapereUtil.df2.format(Math.max(0, -1*powerLeft))
				+ (isContractAlreadyInTable? "" : " contractPower = " + contractPowerSlot)
				);
			logger.info("checkAvailabilityAndExpiration tableConsumersProcessing = " + tableConsumersProcessing);
		}*/
		return new ConfirmationItem(agentName, ProsumerRole.PRODUCER, consumer, isComplementary, isOK, comment, 0, timeShiftMS);
	}



	public String generateRequestLog(EnergyAgent producerAgent, List<EnergyRequest> requestList, NodeTotal nodeTotal) {
		List<String> consumerListWaitingRequest  = new ArrayList<>();
		StringBuffer result = new StringBuffer();
		//boolean nodeTotalLogged = false;
		int maxWarningDuration = 0;
		if(requestList.size() > 0) {
			for (EnergyRequest request : requestList) {
				//String warningLog = "";
				int warningDuraction = 0;
				if(request.getWarningDate()!=null) {
					//warningLog = "*";
					warningDuraction = request.getWarningDurationSec();
					if(warningDuraction > maxWarningDuration) {
						maxWarningDuration = warningDuraction;
					}
					/*
					if(warningDuraction>0) {
						warningLog = "*(" + warningDuraction + ")";
					}*/
				}
				String reIssuer = request.getIssuer();
				int idx = reIssuer.lastIndexOf("_");
				String consumerNumber = reIssuer.substring(1+idx);
				String sReq = consumerNumber;
				if(warningDuraction>=5) {
					sReq = sReq + "[" + warningDuraction + "s]";
				}
				consumerListWaitingRequest.add(sReq);
			}
			result.append(SapereUtil.implode(consumerListWaitingRequest, ","));
		}
		return result.toString();
	}

	public String generateNodeTotalLog(EnergyAgent producerAgent, NodeTotal nodeTotal) {
		StringBuffer result = new StringBuffer();
		if(nodeTotal == null) {
			return "";
		}
		long maxWarningDuration =  nodeTotal.getMaxWarningDuration();
		if(maxWarningDuration>=8) {
			result.append("     nodeTotal={id:").append(nodeTotal.getId())
				//.append(",time:").append( SapereUtil.format_time.format(nodeTotal.getDate()))
				.append(" ,avb:").append(nodeTotal.getAvailable())
				.append("})");
		}
		/*
		if(false  && maxWarningDuration>WARNING_DURATION_THRESHOLD) {
			logger.warning(producerAgent.getAgentName() + " generateRequestLog : For debug : maxWarningDuration = " + maxWarningDuration);
		}*/
		return result.toString();
	}

	private boolean canProvideRequest(EnergyRequest request) {
		String consumer = request.getIssuer();
		boolean isComplementary = request.isComplementary();
		return request.canBeSupplied() && !hasValidContract(consumer, isComplementary) && !hasWaitingOffer(consumer, isComplementary);
	}

	public void logContent(EnergyAgent producerAgent) {
		String agentName = producerAgent.getAgentName();
		double available1 = computeAvailablePower(producerAgent,false, false);
		String tab1 = "    ";
		String tab2 = tab1 + tab1;
		logger.info(" ----------- Agent " + agentName + " has " + UtilDates.df3.format(available1) + " W available ---------");
		logger.info(tab1 + "Waiting Requests : ");
		List<EnergyRequest> requestList = producerAgent.getProducerPolicy().sortRequests(getWaitingRequest());
		for(EnergyRequest request : requestList) {
			String reqIssuer = request.getIssuer();
			logger.info(tab2 +  reqIssuer + " " + request);
		}
		logger.info(tab1 + "Waiting offers : ");
		Collection<SingleOffer> waitingOffers = getWaitingOffers();
		for(SingleOffer offer : waitingOffers) {
			logger.info(tab2 + offer.getConsumerAgent() + " " + offer);
		}
		logger.info(tab1 + "Contracts : ");
		Map<String, List<ReducedContract>> tableContracts = SapereUtil.mergeMapStrRContract(
				 mainProcessingTable.getTableAllContracts()
				,secondProcessingTable.getTableAllContracts()
				,logger);
		for(List<ReducedContract> listReducedContract : tableContracts.values()) {
			for(ReducedContract reducedContract : listReducedContract) {
				logger.info(tab2 + reducedContract.getConsumerAgent() + " " + reducedContract);
			}
		}
		// producerAgent.logLsaProperties();
	}

	public void releaseEnergyForUrgentRequest(EnergyAgent producerAgent, NodeTotal nodeTotal, EnergyRequest request) throws HandlingException {
		double nodeTotalAvailable = nodeTotal==null? 0 : nodeTotal.getAvailable();
		double availablePower = computeAvailablePower(producerAgent,false,false);
		// Check if the request has a high priority level and cannot be provided with the local
		if (PriorityLevel.HIGH.equals(request.getPriorityLevel()) && availablePower < request.getPower()) {
			String agentName = producerAgent.getAgentName();
			logger.info(agentName +" High priority request found " + request);
			 // Check if we can meet this demand globaly (with other producers linked to this node)
			 if(nodeTotalAvailable < request.getPower()) {
				// Break current offers until there is enough energy left to meet this demand
				Map<String, List<SingleOffer>> tableOffers = getTableWaitingOffers();
				SingleOffer offerToCancel = SapereUtil.getOfferToCancel(tableOffers, OFFER_EXPIRATION_MARGIN_SEC);
				while (availablePower < request.getPower() && offerToCancel != null) {
					removeOffer(producerAgent, offerToCancel.getConsumerAgent(), offerToCancel.isComplementary(), "generateNewOffers:urent request");
					tableOffers = getTableWaitingOffers();
					offerToCancel = SapereUtil.getOfferToCancel(tableOffers, OFFER_EXPIRATION_MARGIN_SEC);
					availablePower = computeAvailablePower(producerAgent, false,false);
				}

				// Break current contracts until there is enough energy left to meet this demand
				Map<String, List<ReducedContract>> tableValidContracts = getTableValidContracts();
				ReducedContract contractToCancel = SapereUtil.getContractToCancel(tableValidContracts);
				while (availablePower < request.getPower() && contractToCancel != null) {
					String warningLog = "The agent must respond to a higher priority request.";
					logger.info("releaseEnergyForUrgentRequest " + producerAgent.getAgentName() + " : stops contract " + contractToCancel + " " + warningLog);
					stopContract(producerAgent, contractToCancel.getConsumerAgent(), contractToCancel.isComplementary(), warningLog);
					tableValidContracts = getTableValidContracts();
					contractToCancel = SapereUtil.getContractToCancel(tableValidContracts);
					availablePower = computeAvailablePower(producerAgent,false,false);
				}
			 } else {
				 // This demand can be meet globaly
				 nodeTotalAvailable-= request.getPower();
			 }
		}
	}

	private String logWarningDurations(EnergyAgent producerAgent, NodeTotal nodeTotal) {
		String agentName = producerAgent.getAgentName();
		String firstWarningConsumer = nodeTotal==null? "" : nodeTotal.getMaxWarningConsumer();
		if(producerAgent.hasHighWarningDuration()) {
			String beginTag = "#########  " + agentName + " ";
			logger.warning(beginTag + "High Warning level : " + nodeTotal.getMaxWarningDuration() + " at " + UtilDates.formatTimeOrDate(nodeTotal.getDate(), timeShiftMS)
				+ " First Warning request : " + firstWarningConsumer);
			if(!mainProcessingTable.hasConsumer(firstWarningConsumer) && !secondProcessingTable.hasConsumer(firstWarningConsumer)) {
				logger.warning(beginTag  + firstWarningConsumer + " not found in waiting requests or waiting offers or contracts table");
				//logContent(producerAgent, usedPolicy);
				if(logBondedRequests.containsKey(firstWarningConsumer)) {
					logger.warning(beginTag + "last requests bonding : " + getStrLogBondedRequests(firstWarningConsumer));
				}
				if(mainProcessingTable.logOperationHasConsumer(firstWarningConsumer)) {
					logger.warning(beginTag + "last operations of " + firstWarningConsumer + " on main table : " + mainProcessingTable.getStrLogOperations(firstWarningConsumer));
				}
				if(secondProcessingTable.logOperationHasConsumer(firstWarningConsumer)) {
					logger.warning(beginTag + "last operations of " + firstWarningConsumer + " on complementary table : " + secondProcessingTable.getStrLogOperations(firstWarningConsumer));
				}
				if(tableBondedRequests.containsKey(firstWarningConsumer)) {
					logger.warning(beginTag + "last request " + tableBondedRequests.get(firstWarningConsumer));
				} else {
					logger.warning(beginTag + firstWarningConsumer + "no found in bonded requests table");
				}
			}
		}
		return firstWarningConsumer;
	}

	public int generateNewOffers(EnergyAgent producerAgent, NodeTotal nodeTotal) throws HandlingException {
		int nbNewOFfers = 0;
		String agentName = producerAgent.getAgentName();
		Map<String, List<EnergyRequest>> tableWaitingRequest = getTableWaitingRequest();
		boolean toLog = "Prosumer_N1_2".equals(agentName) && (tableWaitingRequest.size() > 0);
		if(toLog) {
			logger.info("generateNewOffers  " + agentName + " : begin waitingoffers = " + getTableWaitingOffers().keySet() + " tableWaitingRequest keys = " + tableWaitingRequest.keySet());
		}
		if(producerAgent.hasExpired()) {
			logger.info("generateNewOffers " + agentName + " has expired");
			return nbNewOFfers;
		}
		// Log high warning durations
		String firstWarningConsumer = logWarningDurations(producerAgent, nodeTotal);

		//double nodeTotalAvailable = nodeTotal==null? 0 : nodeTotal.getAvailable();
		if (tableWaitingRequest.size() > 0 && !producerAgent.hasExpired() && !producerAgent.isDisabled()) {
			// Wait untill all offers has expired ?
			Map<String, List<SingleOffer>> waitingOffers = getTableWaitingOffers();
			if(waitingOffers.size()>0) {
				return nbNewOFfers;
			}
			if(toLog) {
				logger.info(" generateNewOffers "+ agentName + " : step1 "
						+ ", globalProduction = " + producerAgent.getGlobalProduction()
						+ ", producerAgent.isDisabled " + producerAgent.isDisabled());
			}
			// Debug requests under available
			IProducerPolicy producerPolicy = producerAgent.getProducerPolicy();
			if(producerPolicy == null) {
				//logger.error("producerPolicy is null for agent " + producerAgent.getAgentName());
				throw new HandlingException("producerPolicy of agent " + producerAgent.getAgentName() + " is not set");
				//return nbNewOFfers;
			}
			List<EnergyRequest> requestList = producerAgent.getProducerPolicy().sortRequests(getWaitingRequest());
			// For debug : check if the first request is the same as the highest warning request in nodeTotal
			if(producerAgent.hasHighWarningDuration() && requestList.size() > 0) {
				EnergyRequest firstRequest = requestList.get(0);
				String firstReqIssuer = firstRequest.getIssuer();
				if(!firstWarningConsumer.equals(firstReqIssuer)) {
					logger.warning("#########  " + agentName + " " + firstWarningConsumer + " not in first position in waiting request list ");
					for(EnergyRequest nextRequest : requestList) {
						logger.warning("       " + nextRequest);
					}
				}
			}
			// End for debug
			String logConsumerList = generateRequestLog(producerAgent, requestList, nodeTotal);
			if(toLog) {
				logger.info(" generateNewOffers "+ agentName + " : step2 " + logConsumerList);
			}
			for (EnergyRequest request : requestList) {
				String consumer = request.getIssuer();
				boolean isComplementary = request.isComplementary();
				//boolean addOffer = false;
				if(hasWaitingOffer(consumer, isComplementary) || hasCanceledContract(consumer, isComplementary)) {
					logger.info(agentName + " generateNewOffers For debug has waiting offer or canceled contract for consumer " + consumer);
				}
				if (canProvideRequest(request) && producerPolicy.confirmSupply(producerAgent, request)) {
					// Check if the request has a high priority level and cannot be provided with the local
					releaseEnergyForUrgentRequest(producerAgent, nodeTotal, request);
					double availablePower = computeAvailablePower(producerAgent,false,false);
					if (availablePower > 0) {
						// Generate a new single offer
						Date requestedBeginDate = request.getBeginDate();
						//Date requestedEndDate = request.getEndDate();
						Double requestedPower = request.getPower();
						Double offerMargin = request.getPower() * EnergySupply.DEFAULT_POWER_MARGIN_RATIO;
						Date current = getCurrentDate();
						Date providedBeginDate = requestedBeginDate.after(current) ? requestedBeginDate : current;
						Double providedPower = Math.min(availablePower, requestedPower);
						Double providedPowerMax = Math.min(availablePower, requestedPower + offerMargin);
						Double providedPowerMin =  Math.min(availablePower, requestedPower - offerMargin);
						PowerSlot providedPowerSlot = new PowerSlot(providedPower, providedPowerMin, providedPowerMax);
						// boolean respondToConsumer = false;
						if (producerAgent.isInActiveSlot(providedBeginDate) && providedPower >= 0.001) {
							// Prepare the offer
							ProsumerProperties isseurProperties = producerAgent.getGlobalProduction().getIssuerProperties().clone();
							EnergySupply supply0 = producerAgent.getGlobalProduction();
							Date providedEndDate = request.getEndDate().after(supply0.getEndDate()) ?
									supply0.getEndDate()
									: request.getEndDate();
							EnergySupply supply = new EnergySupply(isseurProperties, false,
									providedPowerSlot, providedBeginDate,
									providedEndDate, producerPolicy.getDefaultPricingTable(), false);
							try {
								// For debug : log contracts
								/*
								String filterPower = "single_offer.power BETWEEN " + Math.floor(supply.getPower()) + " AND " + (1+Math.floor(supply.getPower())); 
								List<SingleOffer> alreadyGeneatedOffers=EnergyDbHelper.retrieveOffers(SapereUtil.shiftDateSec(getCurrentDate(), -8), getCurrentDate(), consumer, null,  filterPower);
								if(alreadyGeneatedOffers.size()>0) {
									double testAvl = computeAvailablePower(false, false);
									logger.info("---  Befor creation a new offer : available = " + testAvl);
									for(SingleOffer nextOffer : alreadyGeneatedOffers) {
										logger.info("Already generated  " + nextOffer);
									}
									for(ReducedContract contract : getTableAllContracts2().values()) {
										logger.info(this.agentName + " : next contrat : " + contract);
									}
								}*/
								SingleOffer newOffer = new SingleOffer(agentName, supply, OFFER_VALIDITY_SECONDS, request.clone());
								newOffer =  producerPolicy.priceOffer(producerAgent, newOffer);
								newOffer.setLog(logConsumerList);
								String logTotal = generateNodeTotalLog(producerAgent, nodeTotal);
								newOffer.setLog2(logTotal + "  avb="+ UtilDates.df3.format(availablePower));
								Long offerId = EnergyDbHelper.registerSingleOffer(newOffer, producerAgent.getStartEvent(), nodeTotal.getId());
								newOffer.setId(offerId);
								addOffer(producerAgent,newOffer);
								nbNewOFfers++;
								//addOffer = true;
								// For debug : check availability
								if(toLog) {
									logger.info("newOffer : " + newOffer);
								}
							} catch (Exception e) {
								logger.error(e);
							}
						} else {
							//logger.info("### For Debug2 : providedBeginDate " + providedBeginDate + " , providedPower = " + providedPower); 
							// addProperty(new Property("Ignore", "0", query, pBond, state, ipSource,
							// false));
						}
					}
				} else {
					logger.info(producerAgent.getAgentName() +  " The following request cannot be provided : " + request);
					logger.info(producerAgent.getAgentName() + " : canBeSupplied = " + request.canBeSupplied()
						+ ", hasValidContract = " + hasValidContract(consumer, isComplementary)
						+ ", hasWaitingOffer = " + hasWaitingOffer(consumer, isComplementary));
				}
			}
		}
		return nbNewOFfers;
	}

	public void handleConsumerRequest(EnergyAgent producerAgent, EnergyRequest request, String consumer, Lsa bondedLsa) {
		try {
			if(request.getIssuerProperties() != null) {
				request.getIssuerProperties().setDistance(bondedLsa.getSourceDistance());
			}
			String agentName = producerAgent.getAgentName();
			if(producerAgent.hasHighWarningDuration() && request.getWarningDurationSec() >= ConsumersProcessingMangager.WARNING_DURATION_THRESHOLD) {
				logger.warning("handleConsumerRequest " + request);
			}
			// Check if a valid contract exists for this consumer
			String reqIssuer = request.getIssuer();
			ReducedContract contract = this.getReducedContract(reqIssuer, request.isComplementary());
			if(contract!=null) {
				// Request found on ongoing contract
				logger.info("handleConsumerRequest " + agentName + " contract found for consumer " + consumer + " : " + contract);
				if(contract.hasAllAgreements() || contract.waitingValidation()) {
					// Stop the contract
					logger.warning(agentName + " receives a request of " + reqIssuer + " but a contract is ongoing : stop the contract");
					/*
					Date dateMin = SapereUtil.shiftDateSec(getCurrentDate(), -30);
					Date dateMax = getCurrentDate();
					List<SingleOffer> dbOffers = EnergyDbHelper.retrieveOffers(dateMin, dateMax, consumer, agentName, "accepted AND contract_event_id IS NULL");
					if(dbOffers.size()>0) {
						logger.warning("handleConsumerRequest for debug1 : " +  "The agent receives a request of " + request.getIssuer());
					}*/
					this.stopContract(producerAgent,reqIssuer, contract.isComplementary(), "The agent receives a request of " + reqIssuer);
				}
			}
			if (request.canBeSupplied()) {
				// add waiting request
				addOrUpdateRequest(producerAgent, consumer, request);
			} else {
				logger.info("Agent " + producerAgent.getAgentName() + " cannot supply request " + request );
			}
		} catch (NumberFormatException e) {
			logger.error(e);
		}
	}

	public boolean isConcerned(EnergyAgent producerAgent, ProtectedContract protectedContract) {
		boolean result = false;
		try {
			if(protectedContract!=null && protectedContract.hasAccessAsReceiver(producerAgent) && producerAgent.isProducer()) {
				result = protectedContract.hasProducer(producerAgent);
			}
		} catch (PermissionException e) {
			logger.error(e);
		}
		return result;
	}

	public void handleConsumerContracts(EnergyAgent produerAgent, Object oMainContract, Object oSecondContract, String consumer, Lsa bondedLsa, int debugLevel) {
		if (oMainContract instanceof ProtectedContract) {
			// handle consumer main contract
			ProtectedContract mainContract = ((ProtectedContract) oMainContract).clone();
			this.handleConsumerContract(produerAgent, mainContract, consumer, debugLevel);
		} else if(mainProcessingTable.hasValidContract(consumer)){
			// No contract property for this consumer: should remove it from the processing manager
			// TODO: log CONTRACT_CONFIRM property contained in bondedLSA - to put in parameter
			logger.error(produerAgent.getAgentName()  +  " should remove main contract of " + consumer
					+ " CONTRACT_CONFIRM property = " + bondedLsa.getOnePropertyByName("CONTRACT_CONFIRM"));
			mainProcessingTable.removeConsumer(consumer, "no CONTRACT1 property in LSA of " + consumer);
		}
		if(oSecondContract instanceof ProtectedContract) {
			// handle consumer complementary contract
			ProtectedContract secondContract = ((ProtectedContract) oSecondContract).clone();
			this.handleConsumerContract(produerAgent, secondContract, consumer, debugLevel);
		} else if(secondProcessingTable.hasValidContract(consumer)){
			// No contract property for this consumer: should remove it from the processing manager
			logger.error(produerAgent.getAgentName() +  " should remove secondary contract of " + consumer
					+ " CONTRACT_CONFIRM property = " + bondedLsa.getOnePropertyByName("CONTRACT_CONFIRM"));
			secondProcessingTable.removeConsumer(consumer, "no CONTRACT2 property in LSA of " + consumer);
		}
	}

	/**
	 * handleConsumerContract : Handle consumer contract
	 * @param produerAgent
	 * @param protectedContract
	 * @param consumer
	 * @param debugLevel
	 */
	public void handleConsumerContract(EnergyAgent produerAgent, ProtectedContract protectedContract, String consumer, int debugLevel) {
		String agentName = produerAgent.getAgentName();
		boolean isConcerned = isConcerned(produerAgent,protectedContract);
		String contractConsumer = protectedContract.getConsumerAgent();
		boolean isComplementary = protectedContract.isComplementary();
		/*
		if(protectedContract.hasGap()) {
			logger.warning("### handleConsumerContract " + agentName + " the following contract has a gap " + protectedContract);
		}*/
		try {
			if (hasWaitingRequest(consumer, isComplementary) || hasWaitingOffer(consumer, isComplementary)) {
				// Remove waiting request of waiting offer
				if(debugLevel>0) {
					logger.info("handleConsumerContract(1) " + agentName + " remove " + consumer + " object " + getWaitingRequest(consumer, isComplementary));
				}
				removeConsumer(consumer, isComplementary ,"bonded contract " + protectedContract + " " + (isConcerned?"(concerned)":("not concerned")));
			}
			if(!isConcerned) {
				// This contract does not concern the producer agent
				if(hasContract(contractConsumer, isComplementary)) {
					logger.info(agentName + " : remove this contract from data");
					// The contract contained in LSA is obsolete : remove the contract
					removeContract(produerAgent, contractConsumer, isComplementary,"this contract does not conern " + agentName);
				}
			}
			if(isConcerned) {
				// This contract concerns the producer agent
				boolean isLocalyCanceled = produerAgent.isDisabled() || hasCanceledContract(contractConsumer, isComplementary);
				// Check if a confirmation has already be sent for this contract
				if(hasAlreadyInvalidation(produerAgent, contractConsumer, protectedContract.isComplementary())) {
					logger.warning("handleConsumerContract" + agentName + " contract has already be invalidated");
					isLocalyCanceled = true;
				}
				if (!protectedContract.hasExpired() && !protectedContract.hasDisagreement() && !isLocalyCanceled) {
					// Add a new contract
					ReducedContract reducedContract = protectedContract.getProducerContent(produerAgent);
					ReducedContract newContract = reducedContract.clone();
					if(newContract.waitingValidation()) {
						logger.info("handleConsumerContract " + agentName + " add new contract " + newContract);
					}
					// Remove complementary contract if the main contract is merged
					if(newContract.isMerged() && secondProcessingTable.hasContract(newContract.getConsumerAgent()))  {
						secondProcessingTable.removeConsumer(newContract.getConsumerAgent(), "merge of main contract (" + newContract.getConsumerAgent() + ")");
					}
					confirmAgreementToConsumer(produerAgent, newContract);
					updateContract(produerAgent, newContract, "Update from producerAgent " + agentName);
					// TODO : check if there is no bug after removing following instruction "cleanExpiredData();"
					//cleanExpiredData();
				} else if (protectedContract.hasDisagreement()) {
					logger.info(agentName + " : Contract has disagreement : cancel the contract " + protectedContract);
					// Remove the contract
					removeContract(produerAgent, protectedContract.getConsumerAgent(), isComplementary, " : stop from " + protectedContract.getConsumerAgent());
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public void handleConsumerConfirmation(EnergyAgent producerAgent,
			ProtectedConfirmationTable issuerConfirmTable) throws HandlingException {
		mainProcessingTable.handleConsumerConfirmation(producerAgent, issuerConfirmTable);
		secondProcessingTable.handleConsumerConfirmation(producerAgent, issuerConfirmTable);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}
}
