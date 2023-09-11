package com.sapereapi.agent.energy.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.IEnergyObject;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.node.NodeConfig;

public class ConsumersProcessingTable {
	private Map<String, IEnergyObject> tableConsumersProcessing = null;
	private boolean activateLogOnOperations = true;
	private Map<String, List<String>> logOperations = null;
	private boolean showAgreements = false;
	protected Boolean isComplementary = null;
	private Map<String, ConfirmationItem> receivedConfirmations;
	private long timeShiftMS = 0;

	private static SapereLogger logger = SapereLogger.getInstance();

	public ConsumersProcessingTable(boolean _complementary, long _timeShiftMS) {
		super();
		this.isComplementary = _complementary;
		this.timeShiftMS = _timeShiftMS;
		tableConsumersProcessing = new HashMap<String, IEnergyObject>();
		receivedConfirmations = new HashMap<String, ConfirmationItem>();
		logOperations = new HashMap<String,List<String>>();
	}

	public boolean isMain() {
		return !isComplementary;
	}

	public boolean isComplementary() {
		return isComplementary;
	}

	public void addLogOperation(String consumer, String operation) {
		if(activateLogOnOperations) {
			int logMaxSize = 10;
			if(!logOperations.containsKey(consumer)) {
				logOperations.put(consumer, new ArrayList<String>());
			 }
			 String time = UtilDates.format_time.format(getCurrentDate());
			 String newEntry = time + " : " + operation;
			 List<String> listOperations = logOperations.get(consumer);
			 listOperations.add(0,newEntry);
			 while(listOperations.size()>logMaxSize) {
				 listOperations.remove(logMaxSize);
			 }
		}
	}

	public boolean logOperationHasConsumer(String consumer) {
		return logOperations.containsKey(consumer);
	}

	public String getStrLogOperations(String concumer) {
		StringBuffer logLastOperations = new StringBuffer();
		if(activateLogOnOperations && logOperations.containsKey(concumer)) {
			List<String> lastOperations = logOperations.get(concumer);
			for(String operation : lastOperations ) {
				logLastOperations.append(SapereUtil.CR).append("   ").append(operation);
			}
		}
		return logLastOperations.toString();
	}

	public boolean hasConsumer(String consumer) {
		return tableConsumersProcessing.containsKey(consumer);
	}

	public void removeConsumer(String consumer, String logTag) {
		String objClass = "";
		if(this.activateLogOnOperations && tableConsumersProcessing.containsKey(consumer)) {
			objClass = "" + tableConsumersProcessing.get(consumer).getClass().getSimpleName();
		}
		addLogOperation(consumer, "remove object " + objClass + " " + logTag);
		tableConsumersProcessing.remove(consumer);
	}

	public boolean hasWaitingRequest(String consumer) {
		if(tableConsumersProcessing.containsKey(consumer)) {
			Object value = tableConsumersProcessing.get(consumer) ;
			return (value instanceof EnergyRequest);
		}
		return false;
	}

	public boolean hasWaitingOffer(String consumer) {
		if(tableConsumersProcessing.containsKey(consumer)) {
			IEnergyObject value = tableConsumersProcessing.get(consumer) ;
			return (value instanceof SingleOffer);
		}
		return false;
	}

	public boolean hasContract(String consumer) {
		if(tableConsumersProcessing.containsKey(consumer)) {
			IEnergyObject value = tableConsumersProcessing.get(consumer) ;
			return (value instanceof ReducedContract);
		}
		return false;
	}

	public boolean hasValidContract(String consumer) {
		if(tableConsumersProcessing.containsKey(consumer)) {
			IEnergyObject value = tableConsumersProcessing.get(consumer) ;
			if (value instanceof ReducedContract) {
				ReducedContract reducedContract = (ReducedContract) value;
				return reducedContract.hasAllAgreements();
			}
		}
		return false;
	}

	public boolean hasCanceledContract(String consumer) {
		if(tableConsumersProcessing.containsKey(consumer)) {
			IEnergyObject value = tableConsumersProcessing.get(consumer) ;
			if (value instanceof ReducedContract) {
				ReducedContract reducedContract = (ReducedContract) value;
				return reducedContract.hasDisagreement();			}
		}
		return false;
	}

	public boolean hasWaitingValidationContract(String consumer) {
		if(tableConsumersProcessing.containsKey(consumer)) {
			IEnergyObject value = tableConsumersProcessing.get(consumer) ;
			if (value instanceof ReducedContract) {
				ReducedContract reducedContract = (ReducedContract) value;
				return reducedContract.waitingValidation();			}
		}
		return false;
	}

	public EnergyRequest getWaitingRequest(String consumer) {
		if(hasWaitingRequest(consumer)) {
			return (EnergyRequest) tableConsumersProcessing.get(consumer);
		}
		return null;
	}

	public SingleOffer getWaitingOffer(String consumer) {
		if(hasWaitingOffer(consumer)) {
			return (SingleOffer) tableConsumersProcessing.get(consumer);
		}
		return null;
	}

	public ReducedContract getReducedContract(String consumer) {
		if(hasContract(consumer)) {
			return (ReducedContract) tableConsumersProcessing.get(consumer);
		}
		return null;
	}

	public Map<String, ReducedContract> getTableContracts(boolean addValid, boolean addWaiting, boolean addInvalid) {
		Map<String, ReducedContract> result = new HashMap<String, ReducedContract>();
		for(IEnergyObject nextObject : tableConsumersProcessing.values()) {
			if(nextObject instanceof ReducedContract) {
				ReducedContract nextContract = (ReducedContract) nextObject;
				String consumer = nextContract.getConsumerAgent();
				if (addValid && nextContract.hasAllAgreements()) {
					result.put(consumer, nextContract);
				} else if (addWaiting && nextContract.waitingValidation()) {
					result.put(consumer, nextContract);
				} else if (addInvalid && nextContract.hasDisagreement()) {
					result.put(consumer, nextContract);
				}
			}
		}
		return result;
	}

	public Map<String, ReducedContract> getTableAllContracts() {
		return getTableContracts(true, true, true);
	}

	public Map<String, ReducedContract> getTableValidContracts() {
		return getTableContracts(true, false, false);
	}

	public Collection<ReducedContract> getValidContracts() {
		return getTableValidContracts().values();
	}

	public Map<String, ReducedContract> getTableCanceledContracts() {
		return getTableContracts(false, false, true);
	}

	public Map<String, ReducedContract> getTableWaitingContracts() {
		return getTableContracts(false, true, false);
	}

	public PowerSlot computePowerOfWaitingContrats(EnergyAgent produceragent) {
		PowerSlot waitingContractsPower = new PowerSlot();
		for (ReducedContract contract : getTableWaitingContracts().values()) {
			waitingContractsPower.add(contract.getProducerPowerSlot());
		}
		return waitingContractsPower;
	}

	public List<String> getConsumersOfWaitingContrats() {
		List<String> waitingContractsConsumers = new ArrayList<String>();
		for (ReducedContract contract : getTableWaitingContracts().values()) {
			waitingContractsConsumers.add(contract.getConsumerAgent());
		}
		return waitingContractsConsumers;
	}

	public Set<String> getLinkedAgents() {
		Set<String> result = new HashSet<>();
		for (ReducedContract contract : getValidContracts()) {
			if (!contract.hasExpired()) {
				result.add(contract.getConsumerAgent());
			}
		}
		return result;
	}

	public PowerSlot getForcastOngoingContractsPowerSlot(String location, EnergyAgent produceragent, Date aDate) {
		PowerSlot power = new PowerSlot();
		for (ReducedContract contract : getValidContracts()) {
			if (!contract.hasExpired()) {
				NodeConfig consumerLocation = contract.getConsumerLocation();
				if(location==null || location.equals(consumerLocation.getMainServiceAddress())) {
					power.add(contract.getForcastProducerPowerSlot(aDate));
				} else {
					logger.info("For debug : location not found");
				}
			}
		}
		return power;
	}

	public PowerSlot getOngoingContractsPowerSlot(String location, EnergyAgent produceragent) {
		PowerSlot power = new PowerSlot();
		for (ReducedContract contract : getValidContracts()) {
			if (!contract.hasExpired()) {
				NodeConfig consumerLocation = contract.getConsumerLocation();
				if(location==null || location.equals(consumerLocation.getMainServiceAddress())) {
					power.add(contract.getProducerPowerSlot());
				} else {
					logger.info("For debug : location not found");
				}
			}
		}
		return power;
	}

	public Map<String, PowerSlot> getOngoingContractsRepartition(EnergyAgent produceragent) {
		Map<String, PowerSlot> result = new HashMap<String, PowerSlot>();
		for (ReducedContract contract : getValidContracts()) {
			if (!contract.hasExpired()) {
				try {
					boolean isLocal = Sapere.getInstance().isLocalAgent(contract.getConsumerAgent());
					String agentName = contract.getConsumerAgent() + (isLocal? "" : "*");
					result.put(agentName, contract.getProducerPowerSlot());
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
		return result;
	}

	public Map<String, EnergyRequest> getTableWaitingRequest() {
		Map<String, EnergyRequest> tableWaitingRequest = new HashMap<String, EnergyRequest>();
		for(String consumer : tableConsumersProcessing.keySet()) {
			IEnergyObject nextValue = tableConsumersProcessing.get(consumer);
			if(nextValue instanceof EnergyRequest) {
				if(!nextValue.isComplementary() && this.isComplementary()) {
					logger.error("The request " + nextValue + " should be complementary" );
				}
				tableWaitingRequest.put(consumer, (EnergyRequest) nextValue);
			}
		}
		return tableWaitingRequest;
	}

	public Map<String, SingleOffer> getTableWaitingOffers() {
		Map<String, SingleOffer> tableWaitingOffers = new HashMap<String, SingleOffer>();
		for(String consumer : tableConsumersProcessing.keySet()) {
			Object nextValue = tableConsumersProcessing.get(consumer);
			if(nextValue instanceof SingleOffer) {
				tableWaitingOffers.put(consumer, (SingleOffer) nextValue);
			}
		}
		return tableWaitingOffers;
	}

	public Double computeUsedPower(EnergyAgent produceragent, boolean ignoreOffers, boolean ignoreWaitingContracts) {
		double result = 0;
		Map<String, ReducedContract> tablleContrats = getTableContracts(true, !ignoreWaitingContracts, false);
		for (ReducedContract contract : tablleContrats.values()) {
			if (!contract.hasExpired()) {
				double usedMax = 0;
				PowerSlot contractPowerSlot = contract.getProducerPowerSlot();
				usedMax =  contractPowerSlot.getMax();// contract.getProducerPowerSlot(produceragent);
				result += usedMax;
			}
		}
		if (!ignoreOffers) {
			for (SingleOffer offer : getTableWaitingOffers().values()) {
				if (!offer.hasExpired(ConsumersProcessingMangager.OFFER_EXPIRATION_MARGIN_SEC)) {
					Double used = offer.getPowerMax();
					result += used;
				}
			}
		}
		return result;
	}

	public Double getOffersTotal() {
		Double result = Double.valueOf(0);
		for (SingleOffer offer : this.getTableWaitingOffers().values()) {
			if (!offer.hasExpired(ConsumersProcessingMangager.OFFER_EXPIRATION_MARGIN_SEC)) {
				result+=offer.getPower();
			}
		}
		return result;
	}

	public Map<String, Double> getOffersRepartition() {
		Map<String, Double> result = new HashMap<String, Double>();
		for (SingleOffer offer : this.getTableWaitingOffers().values()) {
			if (!offer.hasExpired(ConsumersProcessingMangager.OFFER_EXPIRATION_MARGIN_SEC)) {
				result.put(offer.getConsumerAgent(), offer.getPower());
			}
		}
		return result;
	}

	public void addReceivedConfirmation(EnergyAgent producerAgent, String consumer) {
		if(hasValidContract(consumer)) {
			// Add Received confirmation from consumer agent
			receivedConfirmations.put(consumer, new ConfirmationItem(consumer, isComplementary(), true, "", timeShiftMS));
		}
	}

	public boolean hasReceivedConfirmationItem(String producer) {
		return this.receivedConfirmations.containsKey(producer);
	}

	// Check consumer confirmations
	public Set<String> getConsumersWithNoRecentConfirmations(EnergyAgent producerAgent) {
		Set<String> result = new HashSet<String>();
		if(producerAgent.isActive()) {
			result = SapereUtil.getAgentNamesWithNoRecentConfirmations(producerAgent,
					getTableValidContracts().keySet()
					, this.receivedConfirmations, 5, logger);
		}
		return result;
	}

	public void removeConfirmation(String consumer) {
		receivedConfirmations.remove(consumer);
	}

	public void addOrUpdateRequest(EnergyAgent producerAgent, String consumer, EnergyRequest request) {
		try {
			if(!this.hasConsumer(consumer)) {
				request.setAux_expiryDate(getCurrentDate());
				if(request.getIssuerDistance() > 0) {
					// id from an external database : DO NOT USE IT (constraint integrity)
					request.setEventId(null);
				}
				String logExistingObject = "";
				if(tableConsumersProcessing.containsKey(consumer)) {
					logExistingObject = "existing object : " + tableConsumersProcessing.get(consumer).getClass().getSimpleName();
				}
				if(!request.isComplementary() && this.isComplementary()) {
					logger.error("addOrUpdateRequest : the request " + request + " should be complementary" );
				}
				this.tableConsumersProcessing.put(consumer, request.clone());
				addLogOperation(consumer, "add request " + request + logExistingObject);
			} else if(hasWaitingRequest(consumer)) {
				// Request already in table
				EnergyRequest localRequest = getWaitingRequest(consumer);
				// Update warning date
				if(request.isActive()) {
					localRequest.setWarningDate(request.getWarningDate());
				} else {
					localRequest.setWarningDate(null);
				}
				localRequest.setRefreshDate(request.getRefreshDate());
				if(producerAgent.hasHighWarningDuration() && request.getWarningDurationSec() >= ConsumersProcessingMangager.WARNING_DURATION_THRESHOLD) {
					logger.info("addOrUpdateRequest high warning request " + request);
				}
				addLogOperation(consumer, "update request " + localRequest);
			} else if(hasWaitingOffer(consumer)) {
				// store the request in bonding table
				// tableBondedRequests.put(consumer, request);
			}
		} catch (Throwable e) {
			logger.error(e);
		}
	}

	public Map<String, Date> generateTableRequestWarnings() {
		Map<String, Date> warnings = new HashMap<String, Date>();
		Map<String, EnergyRequest> tableWaitingRequest = getTableWaitingRequest();
		for(EnergyRequest request : tableWaitingRequest.values()) {
			if(request.getWarningDate()!=null) {
				warnings.put(request.getIssuer(), request.getWarningDate());
			}
		}
		return warnings;
	}

	public void refreshAggrementsProperty(EnergyAgent produceragent) {
		produceragent.getLsa().removePropertiesByName("DEBUG_AGREEMENTS");
		if(showAgreements) {
			Map<String, ReducedContract> tableContracts = getTableAllContracts();
			if(tableContracts!=null) {
				StringBuffer content = new StringBuffer();
				String sep="";
				for(ReducedContract nextContract : tableContracts.values()) {
					content.append(sep);
					content.append(nextContract);
					sep = "    -----------------    ";
				}
				produceragent.addProperty(new Property("DEBUG_AGREEMENTS", content));
			}
		}
	}

	public void addOffer(EnergyAgent producerAgent, SingleOffer newOffer) {
		String consumer = newOffer.getConsumerAgent();
		// add offer in WAITING_OFFERS property
		if(hasConsumer(consumer) && !hasWaitingRequest(consumer)) {
			// For debug
			Object obj = tableConsumersProcessing.get(consumer);
			logger.error("addOffer " + producerAgent.getAgentName() + " tableConsumersProcessing as already an object for consumer " + consumer + " " + obj.getClass() + " " + obj);
		}
		this.tableConsumersProcessing.put(consumer, newOffer.clone());
		addLogOperation(consumer, "put offer " + newOffer);
	}

	public void removeOffer(EnergyAgent producerAgent, String consumer, String logTag, boolean activateOfferRemoveLog) {
		if(hasWaitingOffer(consumer)) {
			SingleOffer offer = getWaitingOffer(consumer);
			if(activateOfferRemoveLog) {
				EnergyDbHelper.addLogOnOffer(offer.getId(), " remove at " + UtilDates.getCurrentTimeStr() + " : " + logTag);
			}
			this.tableConsumersProcessing.remove(consumer);
			addLogOperation(consumer, "remove offer " + logTag);
			refreshAggrementsProperty(producerAgent);
		}
	}

	public void removeContract(EnergyAgent producerAgent, String consumer, String logTag) {
		if(hasContract(consumer)) {
			ReducedContract reducedContract = getReducedContract(consumer);
			if(
					// isConcerned(producerAgent, reducedContract)
					   !reducedContract.hasDisagreement()
					&& !reducedContract.hasExpired()) {
				logger.info(" removeContract " + producerAgent.getAgentName() + " remove contract " + logTag + " " + reducedContract);
			}
			tableConsumersProcessing.remove(consumer);
			addLogOperation(consumer, "remove contract " + logTag);
			refreshAggrementsProperty(producerAgent);
		}
	}

	public void updateContract(EnergyAgent producerAgent, ReducedContract aContract, String tag) {
		if(aContract==null) {
			return;
		}
		String consumer = aContract.getConsumerAgent();
		// update the contract
		tableConsumersProcessing.put(consumer, aContract.clone());
		addLogOperation(consumer, "update contract " + tag);
		if(aContract.getIssuerDistance()>1) {
			// TODO : set id null
			aContract.getRequest().setEventId(null);
		}
		// Refresh AGREEMENTS property
		refreshAggrementsProperty(producerAgent);
	}

	public String getComplementaryKeyNotInMainTable(ConsumersProcessingTable mainProcessingTable){
		if(isComplementary && mainProcessingTable!=null) {
			// Remove object if there is no contract on the main table
			for(String consumer : this.tableConsumersProcessing.keySet()) {
				if(!mainProcessingTable.hasContract(consumer) || mainProcessingTable.hasCanceledContract(consumer)) {
					// Remove the object
					if(this.hasValidContract(consumer)) {
						logger.error("cleanExpiredData complementary table : a valid contract is found whereas there is no contract on main table " + this.getReducedContract(consumer));
					}
					return consumer;
				}
			}
		}
		return null;
	}

	/**
	 * Each element is removed if there is no contract is the main table for the same consumer
	 * @param mainProcessingTable
	 */
	public void cleanComplementaryDataNotInMainTable(ConsumersProcessingTable mainProcessingTable) {
		String consumerKey = null;
		while ((consumerKey= getComplementaryKeyNotInMainTable(mainProcessingTable)) != null) {
			Object removedObject = tableConsumersProcessing.get(consumerKey);
			addLogOperation(consumerKey,  "remove object not in main table " + removedObject);
			this.tableConsumersProcessing.remove(consumerKey);
		}
	}

	public void cleanExpiredData(EnergyAgent producerAgent, int debugLevel, Map<String, EnergyRequest> tableBondedRequests) {
		// Clean waiting request
		String consumerKey = null;
		while ((consumerKey= getExpiredObjectKey(tableConsumersProcessing, ConsumersProcessingMangager.OFFER_EXPIRATION_MARGIN_SEC, producerAgent, logger)) != null) {
			if(debugLevel>0) {
				logger.info(producerAgent.getAgentName() + " : cleanExpiredData : remove waiting object of consumer " + consumerKey);
			}
			Object expiredObject = tableConsumersProcessing.get(consumerKey);
			addLogOperation(consumerKey, "remove expired object " + expiredObject);
			tableConsumersProcessing.remove(consumerKey);
			if(expiredObject instanceof SingleOffer && tableBondedRequests.containsKey(consumerKey)) {
				// The request is still no satisfied : replace the expired offer by the last bonded request
				EnergyRequest request = tableBondedRequests.get(consumerKey);
				// Check if the request complementary field matches
				if(request.isComplementary() == this.isComplementary()) {
					logger.warning("cleanExpiredData " + producerAgent.getAgentName() + " the folowwing request should be inserted in tableConsumersProcessing " + request);
					addLogOperation(consumerKey, "add not satisfied request to replace an expired offer " + request);
					tableConsumersProcessing.put(consumerKey, request);
				}
			}
		}
		// Remove canceled contracts
		Map<String, ReducedContract> tableCanceledContracts = getTableCanceledContracts();
		if (!tableCanceledContracts.isEmpty()) {
			for (String consumer : tableCanceledContracts.keySet()) {
				removeContract(producerAgent, consumer, "canceled contract");
			}
		}
	}


	public String getExpiredObjectKey(Map<String, IEnergyObject> tableConsumersProcessing, int marginSeconds,
			EnergyAgent prodAgent, AbstractLogger logger) {
		Date current = getCurrentDate();
		for (String consumerKey : tableConsumersProcessing.keySet()) {
			IEnergyObject consumerObj = tableConsumersProcessing.get(consumerKey);
			if (consumerObj instanceof EnergyRequest) {
				EnergyRequest energyRequest = (EnergyRequest) consumerObj;
				if (energyRequest.getAux_expiryDate().after(current) || !energyRequest.canBeSupplied()) {
					return consumerKey;
				}
			} else if (consumerObj instanceof SingleOffer) {
				SingleOffer offer = (SingleOffer) consumerObj;
				if (offer.hasExpired(marginSeconds)) {
					return consumerKey;
				}
			} else if (consumerObj instanceof ReducedContract) {
				ReducedContract reducedContract = (ReducedContract) consumerObj;
				if (reducedContract.validationHasExpired()) {
					logger.warning("### Contract validation has expired " + reducedContract);
					return consumerKey;
				}
			}
		}
		return null;
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}
}
