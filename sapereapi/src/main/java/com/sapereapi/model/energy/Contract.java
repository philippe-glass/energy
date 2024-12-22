package com.sapereapi.model.energy;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.pricing.ComposedRate;
import com.sapereapi.model.energy.pricing.DiscountItem;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.log.AbstractLogger;

public class Contract extends CompositeOffer implements IEnergyObject, Cloneable {
	private static final long serialVersionUID = 4L;
	private Date validationDeadline;
	private Set<String> agreements;
	private Set<String> disagreements;
	private PricingTable globalPricingTable;
	private Long eventId;


	public Contract() {
		super();
	}

	public Contract(CompositeOffer globalOffer, Date _validationDeadline) {
		super();
		this.request = globalOffer.getRequest();
		this.timeShiftMS = globalOffer.getTimeShiftMS();
		Date current = getCurrentDate();
		this.beginDate = globalOffer.getBeginDate();
		if (beginDate.before(current)) {
			beginDate = current;
		}
		this.endDate = globalOffer.getEndDate();
		this.mapContributions = globalOffer.getMapContributions();
		this.agreements = new HashSet<>();
		this.disagreements = new HashSet<>();
		this.validationDeadline = _validationDeadline;
		this.isMerged = globalOffer.isMerged();
		this.globalPricingTable = globalOffer.auxComputeTotalPricingTable();
	}

	public void stop(String agentName) {
		addAgreement(agentName, false);
	}

	public Set<String> getStakeholderAgents() {
		Set<String> result = new HashSet<>();
		try {
			result.addAll(getProducerAgents());
			result.add(getConsumerAgent());
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		return result;
	}

	public boolean isStackholder(String agentName) {
		return getStakeholderAgents().contains(agentName);
	}

	private void addAgreement(String agentName, boolean isOK) {
		if (isStackholder(agentName)) {
			if (isOK) {
				this.agreements.add(agentName);
				this.disagreements.remove(agentName);
			} else {
				this.disagreements.add(agentName);
				this.agreements.remove(agentName);
			}
		}
	}

	public Set<String> getAgreements() {
		return agreements;
	}

	public void setAgreements(Set<String> agreements) {
		this.agreements = agreements;
	}

	public Set<String> getDisagreements() {
		return disagreements;
	}

	public void setDisagreements(Set<String> disagreement) {
		this.disagreements = disagreement;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public void addProducerAgreement(SapereAgent consumerAgent, String producerName, boolean isOK) {
		if (consumerAgent != null) {
			addAgreement(producerName, isOK);
		}
	}

	public void addAgreement(SapereAgent agent, boolean isOK) {
		addAgreement(agent.getAgentName(), isOK);
	}

	public boolean hasAgreement(String agentName) {
		return agreements.contains(agentName);
	}

	public boolean hasDisagreement(String agentName) {
		return disagreements.contains(agentName);
	}

	public Date getValidationDeadline() {
		return validationDeadline;
	}

	public void setValidationDeadline(Date validationDeadline) {
		this.validationDeadline = validationDeadline;
	}

	public boolean hasAllAgreements() {
		/*
		 * if(this.hasExpired()) { return false; }
		 */
		if (disagreements.size() > 0) {
			return false;
		}
		for (String agent : this.getStakeholderAgents()) {
			if (!agreements.contains(agent)) {
				return false;
			}
		}
		return true;
	}

	public boolean isOnGoing() {
		return hasAllAgreements() && !hasExpired();
	}

	public boolean hasDisagreement() {
		return disagreements.size() > 0;
	}

	public boolean isWaitingValidation() {
		return disagreements.size() == 0 && !hasAllAgreements();
	}

	public boolean hasSameAgrrements(Contract other) {
		return this.agreements.equals(other.getAgreements()) && this.disagreements.equals(other.getDisagreements());
	}

	public Set<String> getContractors() {
		Set<String> result = new HashSet<String>();
		result.add(getConsumerAgent());
		for (String producer : this.mapContributions.keySet()) {
			result.add(producer);
		}
		return result;
	}

	public Set<String> getContractorNoConfirmed() {
		Set<String> result = new HashSet<String>();
		for (String agentName : getContractors()) {
			if (!this.agreements.contains(agentName)) {
				result.add(agentName);
			}
		}
		return result;
	}

	public String getStatus() {
		if (this.hasExpired()) {
			return "Expired";
		} else if (this.hasDisagreement()) {
			return "Canceled";
		} else if (this.hasAllAgreements()) {
			return "Confirmed";
		} else if (this.isWaitingValidation()) {
			return "Waiting " + getContractorNoConfirmed();
		}
		return "";
	}

	public String formatContent(boolean restricted) {
		StringBuffer result = new StringBuffer();
		result.append(restricted ? "(Restricted contract) " : "(Contract) ").append(getConsumerAgent());
		if(isComplementary()) {
			result.append(" [COMPLEMENTARY] ");
		}
		if(isMerged) {
			result.append(" [merged] ");
		}
		if (!restricted) {
			result.append(" : ").append(SapereUtil.formaMapValues(this.getMapPower(), UtilDates.df3));
		}
		result.append(" Sum W= " + UtilDates.df3.format(this.getPower())).append(" From ")
				.append(beginDate==null?"" : UtilDates.format_time.format(this.beginDate))
				.append(" To ")
				.append(endDate==null?"" : UtilDates.format_time.format(this.endDate))
				.append(" status:").append(getStatus());
		if (restricted && isWaitingValidation() && validationDeadline != null) {
			try {
				result.append(" validationDeadline : ").append(UtilDates.format_time.format(this.validationDeadline));
				result.append(" validationHasExpired :").append(this.validationHasExpired());
			} catch (Throwable e) {
				SapereLogger.getInstance().error(e);
			}
		}
		return result.toString();
	}

	public String toString() {
		return formatContent(false);
	}

	public boolean validationHasExpired() {
		if (this.isWaitingValidation() && this.validationDeadline != null) {
			Date current = getCurrentDate();
			return current.after(this.validationDeadline);
		}
		return false;
	}

	public PricingTable getGlobalPricingTable() {
		return globalPricingTable;
	}

	public void setGlobalPricingTable(PricingTable globalPricingTable) {
		this.globalPricingTable = globalPricingTable;
	}

	public Contract copy(boolean copyIds) {
		Contract result = new Contract();
		if (request != null) {
			result.setRequest(request.clone());
		}
		//result.setConsumerAgent(consumerAgent);
		result.setTimeShiftMS(timeShiftMS);
		result.setBeginDate(beginDate);
		result.setEndDate(endDate);
		result.setValidationDeadline(validationDeadline);
		result.setMerged(isMerged);
		// clone of mapPowertable
		Map<String, SingleContribution> cloneMapContribution = SingleContribution.cloneMapContribution(mapContributions, copyIds);		
		result.setMapContributions(cloneMapContribution);
		Set<String> copyAgreements = new HashSet<>();
		for (String next : agreements) {
			copyAgreements.add(next);
		}
		result.setAgreements(copyAgreements);
		Set<String> copyDisagreements = new HashSet<>();
		for (String next : disagreements) {
			copyDisagreements.add(next);
		}
		result.setDisagreements(copyDisagreements);
		if (globalPricingTable != null) {
			result.setGlobalPricingTable(this.globalPricingTable.clone());
		}
		if (copyIds && this.eventId != null) {
			result.setEventId(eventId);
		}
		// do not copy awardCreaditUsage
		return result;
	}

	@Override
	public Contract clone() {
		return copy(true);
	}

	@Override
	public Contract copyForLSA(AbstractLogger logger) {
		return copy(false);
	}


	public boolean hasChangeOnRequest(EnergyRequest newRequest) {
		//double contractPower = getPower();
		double currentRequestPower = request.getPower();
		Date toSet = newRequest.getEndDate();
		boolean dateChange = (request.getEndDate() == null
				|| request.getEndDate().getTime() != toSet.getTime());
		boolean powerChange = Math.abs(currentRequestPower - newRequest.getPower()) >= 0.001;
		return powerChange || dateChange;
	}

	public boolean hasChanged(Contract newContent) {
		// Call hasChanged of superclass
		if(super.hasChanged(newContent)) {
			return true;
		}
		if (!this.agreements.equals(newContent.getAgreements())) {
			return true;
		}
		if (!this.disagreements.equals(newContent.getDisagreements())) {
			return true;
		}
		return false;
	}

	public int getIssuerDistance() {
		if(request.getIssuerProperties() == null) {
			return 0;
		}
		return request.getIssuerProperties().getDistance();
	}

	public boolean modifyRequest(EnergyRequest newRequest, EnergyRequest complementaryRequest, AbstractLogger logger) throws HandlingException{
		if(canModify(newRequest, complementaryRequest)) {
			logger.info("modifyRequest begin : initial gap = " + this.computeGap());
			EnergyRequest newRequest2 = newRequest.clone();
			logger.info("CompositeOffer.modifyRequest newRequest = " + newRequest2 + ", complementaryRequest = " + complementaryRequest);
			Contract oldContract = this.clone();
			Date newBeginDate = getCurrentDate();
			newRequest2.setBeginDate(new Date(newBeginDate.getTime()));
			this.setRequest(newRequest2);
			this.setBeginDate(new Date(newBeginDate.getTime()));
			this.modifyPower(newRequest2.getPower(), logger);
			this.modifyPricingTable(oldContract, logger);
			return this.hasChanged(oldContract);
		}
		return false;
	}

	public void modifyPricingTable(Contract oldContract, AbstractLogger logger) {
		if(globalPricingTable != null && globalPricingTable.hasDiscount()) {
			double lastUsageWH = oldContract.computeCreditUsedWH("modifyRequest > modifyPricingTable", logger);
			logger.info("Contract.modifyPricingTable : globalPricingTable before = " + globalPricingTable + ", lastUsageWH = " + SapereUtil.roundPower(lastUsageWH));
			globalPricingTable = globalPricingTable.applyContractUpdateOnTable(lastUsageWH, this, logger);
			logger.info("Contract.modifyPricingTable : globalPricingTable after = " + globalPricingTable);
		}
	}

	public double computeCreditUsedWH(String step, AbstractLogger logger) {
		// NB : contract cannot be expired
		if (this.isOnGoing() && Math.abs(request.getAwardsCredit()) > 0) {
			Date current = getCurrentDate();
			Double currentPower = getPower();
			if(Math.abs(UtilDates.computeDurationSeconds(current, endDate)) <= 5) {
				current = endDate;
			}
			// ONLY FOR LOG
			double creditUsedWH = globalPricingTable.computeCreditUsedWH(beginDate, current, currentPower, step, logger);
			double creditGrantedWH = globalPricingTable.couputeCreditGrantedWH();
			DiscountItem globalDiscount = globalPricingTable.computeGlobalDiscount();
			try {
				double firstRateDiscount = getFirstRateDiscount();
				double timeSpentSec = UtilDates.computeDurationSeconds(beginDate, current);
				double expectedTimeSpentSec = UtilDates.computeDurationSeconds(beginDate, endDate);
				if(expectedTimeSpentSec != 0) {
					logger.info("Contract.computeCreditUsedWH : spent time ratio = " + SapereUtil.roundPower(timeSpentSec / expectedTimeSpentSec) );
				}
				if(creditGrantedWH != 0) {
					logger.info("Contract.computeCreditUsedWH : used credit ratio = " + SapereUtil.roundPower(creditUsedWH / creditGrantedWH) );
				}
				long requestEventId = request.getEventId();
				TimeSlot usageSlot = new TimeSlot(beginDate, current);
				EnergyDbHelper.logCreditUsedWH(eventId, requestEventId, creditUsedWH, globalDiscount, usageSlot, endDate, firstRateDiscount, timeSpentSec, step);
			} catch (HandlingException e) {
				logger.error(e);
			}
			return creditUsedWH;
		}
		return 0.0;
	}

	public double getFirstRateDiscount() {
		ComposedRate firstRate = globalPricingTable.getRate(beginDate);
		if(firstRate != null) {
			return firstRate.getDiscountValue();
		}
		return 0.0;
	}

	public double getFirstRateValue() {
		ComposedRate firstRate = globalPricingTable.getRate(beginDate);
		if(firstRate != null) {
			return firstRate.getValue();
		}
		return 0.0;
	}

	public boolean checkCanMerge(Contract otherContract) throws HandlingException {
		if(!this.isOnGoing() || !otherContract.isOnGoing()) {
			throw new HandlingException("Contract.merge : both contracts must be ongoing");
		}
		if(this.hasDisagreement() || otherContract.hasDisagreement()) {
			throw new HandlingException("Contract.merge : both contracts must have no disagreement");
		}
		return super.checkCanMerge(otherContract);
	}

	public boolean merge(Contract otherContract) throws HandlingException {
		this.isMerged = false;
		if(checkCanMerge(otherContract)) {
			super.merge(otherContract);
			agreements.addAll(otherContract.getAgreements());
		}
		return isMerged;
	}

	/*
	public Contract aggregate1(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, listObjects, agentAuthentication, logger);
	}


	public static Contract aggregate2(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication, AbstractLogger logger) {
		List<Contract> listContracts = new ArrayList<>();
		for(IAggregateable nextObj : listObjects) {
			if(nextObj instanceof Contract) {
				listContracts.add((Contract) nextObj);
			}
		}
		Contract result = null;
		for(Contract nextContract : listContracts) {
			if("max_power".equals(operator)) {
				if(result == null || nextContract.getPower() > result.getPower()) {
					result = nextContract.clone();
				}
			}
			if("min_power".equals(operator)) {
				if(result == null || nextContract.getPower() < result.getPower()) {
					result = nextContract.clone();
				}
			}
			if("sum_power".equals(operator)) {
				if(result == null) {
					result = nextContract.clone();
					result.getRequest().setIsComplementary(false);
				} else {
					try {
						nextContract.setConsumerAgent(result.getConsumerAgent());
						nextContract.getRequest().setIssuer(result.getRequest().getIssuer());
						//nextContract.addAgreement(resultContract.getRequest().getIssuer(), true);
						nextContract.getRequest().setIsComplementary(true);
						result.merge(nextContract);
					} catch (Exception e) {
						logger.error(e);
					}
				}
			}
		}
		return result;
	}*/
}
