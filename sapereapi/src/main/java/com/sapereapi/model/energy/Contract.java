package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class Contract extends CompositeOffer implements IEnergyObject, Cloneable, Serializable {
	private static final long serialVersionUID = 4L;
	private Date validationDeadline;
	private Set<String> agreements;
	private Set<String> disagreements;

	public Contract(CompositeOffer globalOffer, Date _validationDeadline) {
		super();
		this.request = globalOffer.getRequest();
		this.consumerAgent = globalOffer.getConsumerAgent();
		this.timeShiftMS = globalOffer.getTimeShiftMS();
		Date current = getCurrentDate();
		this.beginDate = globalOffer.getBeginDate();
		if (beginDate.before(current)) {
			beginDate = current;
		}
		this.endDate = globalOffer.getEndDate();
		this.mapPower = globalOffer.getMapPower();
		this.mapPowerMin = globalOffer.getMapPowerMin();
		this.mapPowerMax = globalOffer.getMapPowerMax();
		this.mapLocation = globalOffer.getMapLocation();
		this.singleOffersIds = globalOffer.getSingleOffersIds();
		this.agreements = new HashSet<>();
		this.disagreements = new HashSet<>();
		this.validationDeadline = _validationDeadline;
		this.isMerged = globalOffer.isMerged();
		this.consumerDeviceProperties = globalOffer.getConsumerDeviceProperties();
	}

	public void stop(String agentName) {
		addAgreement(agentName, false);
	}

	public Set<String> getStakeholderAgents() {
		Set<String> result = new HashSet<>();
		try {
			result.addAll(getProducerAgents());
			result.add(consumerAgent);
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
		result.add(this.consumerAgent);
		for (String producer : this.mapPower.keySet()) {
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
		result.append(restricted ? "(Restricted contract) " : "(Contract) ").append(this.consumerAgent);
		if(isComplementary()) {
			result.append(" [COMPLEMENTARY] ");
		}
		if(isMerged) {
			result.append(" [merged] ");
		}
		if (!restricted) {
			result.append(" : ").append(SapereUtil.formaMapValues(this.mapPower));
		}
		result.append(" Sum W= " + UtilDates.df.format(this.getPower())).append(" From ")
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

	@Override
	public Contract clone() {
		CompositeOffer globalOffer = (CompositeOffer) super.clone();
		Date cloneValidationDealine = (this.validationDeadline == null) ? null : new Date(validationDeadline.getTime());
		Contract result = new Contract(globalOffer, cloneValidationDealine);
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
		return result;
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
		return request.getIssuerDistance();
	}

	public boolean checkCanMerge(Contract otherContract) throws Exception {
		if(!this.isOnGoing() || !otherContract.isOnGoing()) {
			throw new Exception("Contract.merge : both contracts must be ongoing");
		}
		if(this.hasDisagreement() || otherContract.hasDisagreement()) {
			throw new Exception("Contract.merge : both contracts must have no disagreement");
		}
		return super.checkCanMerge(otherContract);
	}

	public boolean merge(Contract otherContract) throws Exception {
		this.isMerged = false;
		if(checkCanMerge(otherContract)) {
			super.merge(otherContract);
			agreements.addAll(otherContract.getAgreements());
		}
		return isMerged;
	}

	@Override
	public Contract aggregate(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication) {
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
}
