package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

public class SingleOffer extends EnergySupply implements IEnergyObject, Cloneable, Serializable {
	private static final long serialVersionUID = 17L;
	private String producerAgent;
	private EnergyRequest request;
	private Date creationTime;
	private Date usedTime;
	private Date acceptanceTime;
	private Date deadline;
	private Date contractTime;
	private Long id;
	private Boolean acquitted;
	private Boolean used;
	private Boolean accepted;
	private Long contractEventId;
	private String log;
	private String log2;
	private String logCancel;
	private Long histoId;

	public SingleOffer(String producerAgent, EnergySupply energySupply,
			int _validityDurationSeconds, EnergyRequest _request) {
		super(producerAgent, energySupply.getIssuerLocation(), _request.isComplementary(), energySupply.getPower(), energySupply.getPowerMin(), energySupply.getPowerMax()
				, energySupply.getBeginDate(), energySupply.getEndDate(), energySupply.getDeviceProperties()
				, energySupply.getPricingTable()
				, energySupply.getTimeShiftMS());
		Date current = getCurrentDate();
		if (beginDate.before(current)) {
			beginDate = current;
		}
		this.producerAgent = producerAgent;
		this.request = _request;
		//this.isComplementary = _request.isComplementary();
		this.creationTime = getCurrentDate();
		this.deadline = UtilDates.shiftDateSec(creationTime, _validityDurationSeconds);
		this.acquitted = Boolean.FALSE;;
		this.used = Boolean.FALSE;
		this.log = "";
	}

	public String getProducerAgent() {
		return producerAgent;
	}

	public void setProducerAgent(String producerAgent) {
		this.producerAgent = producerAgent;
	}

	public String getConsumerAgent() {
		if(request==null) {
			return null;
		}
		return this.request.getIssuer();
	}

	public Date getDeadline() {
		return deadline;
	}

	public void setDeadline(Date deadline) {
		this.deadline = deadline;
	}

	public EnergyRequest getRequest() {
		return request;
	}

	public void setRequest(EnergyRequest request) {
		this.request = request;
	}

	public PriorityLevel getPriorityLevel() {
		if(request!=null) {
			return request.getPriorityLevel();
		}
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getAcquitted() {
		return acquitted;
	}

	public void setAcquitted(Boolean acquitted) {
		this.acquitted = acquitted;
	}

	public Boolean getUsed() {
		return used;
	}

	public void setUsed(Boolean used) {
		this.used = used;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getUsedTime() {
		return usedTime;
	}

	public void setUsedTime(Date usedTime) {
		this.usedTime = usedTime;
	}

	public Date getAcceptanceTime() {
		return acceptanceTime;
	}

	public void setAcceptanceTime(Date acceptanceTime) {
		this.acceptanceTime = acceptanceTime;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public Long getHistoId() {
		return histoId;
	}

	public void setHistoId(Long histoId) {
		this.histoId = histoId;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

	public Date getContractTime() {
		return contractTime;
	}

	public void setContractTime(Date contractTime) {
		this.contractTime = contractTime;
	}

	public Long getContractEventId() {
		return contractEventId;
	}

	public void setContractEventId(Long contractEventId) {
		this.contractEventId = contractEventId;
	}

	public String getLogCancel() {
		return logCancel;
	}

	public void setLogCancel(String logCancel) {
		this.logCancel = logCancel;
	}

	public String getLog2() {
		return log2;
	}

	public void setLog2(String log2) {
		this.log2 = log2;
	}

	public Double getTimeCoverRate() {
		if(request==null) {
			SapereLogger.getInstance().warning("### SingleOffer.getTimeCoverRate request is null");
			return Double.valueOf(0);
			//throw new Exception("SingleOffer.getTimeCoverRate request is null");
		}
		double requestDuration = this.request.getTimeLeftMS(false);
		double supplyDuration = this.getTimeLeftMS(false);
		if(supplyDuration >= requestDuration) {
			return Double.valueOf(1.0);
		} else {
			double result = supplyDuration/requestDuration;
			//double test2 = 2/3;
			return result;
		}
	}

	public int compareDistance(SingleOffer other) {
		return this.issuerDistance - other.getIssuerDistance();
	}

	public int compareEnvironmentImpact(SingleOffer other) {
		int impact = this.deviceProperties.getEnvironmentalImpactLevel();
		int otherImpact = other.getDeviceProperties().getEnvironmentalImpactLevel();
		return impact - otherImpact;
	}

	/*
	public int compareDistanceAndEnvImpact(SingleOffer other) {
		int compareDistance = compareDistance(other);
		if(compareDistance!=0) {
			return compareDistance;
		} else {
			// distances are equal : compare environmental impacts
			int compareEnvImpact = this.compareEnvironmentImpact(other);
			if(compareEnvImpact!=0) {
				return compareEnvImpact;
			} else {
				// environmental impacts are equal : compare time cover
				int timeCover = -1*compareTimeCover(other);
				return timeCover;
			}
		}
	}*/

	public int compareTimeCover(SingleOffer other) {
		double cover1 = this.getTimeCoverRate();
		double cover2 = other.getTimeCoverRate();
		double delta = cover1 - cover2;
		return (int) (1000* delta);
	}

	@Override
	public boolean hasExpired() {
		Date current = getCurrentDate();
		return current.after(this.endDate) || current.after(this.deadline);
	}

	/**
	 * We apply a margin for the producing agent. The offer expires a few seconds later for the producer agent: it guarantees its availability at all times
	 * (the margin corresponds to the time limit for acceptance of the offer by the consumer after the selection of the offers)
	 * @param marginSeconds
	 * @return
	 */
	public boolean hasExpired(int marginSeconds) {
		Date current = getCurrentDate();
		Date dealine2 = UtilDates.shiftDateSec(deadline, marginSeconds);
		return current.after(this.endDate) || current.after(dealine2);
	}

	/*
	 * public boolean isActive() { Date current = getCurrentDate(); return
	 * (!current.before(beginDate)) && current.before(this.endDate); }
	 */

	@Override
	public String toString() {
		return "[" + this.id + "] "  + this.producerAgent + "->" + this.getConsumerAgent() + " at " +  UtilDates.format_time.format(creationTime) + " until " + UtilDates.format_time.format(deadline)
				+ " W = " + UtilDates.df.format(this.power) + " from "
				+ UtilDates.format_time.format(beginDate) + " to "	+ UtilDates.format_time.format(endDate)
				+ " (" + UtilDates.df.format(this.getTimeCoverRate()) + ")"
				+ (acquitted!=null && this.acquitted? " acquitted " : "### NOT ACQUITTED ###")
				+ (used!=null && this.used? " used " : "")
				+ (accepted!=null && this.accepted? " accepted " : "")
				+ (contractEventId!=null ? " has contract " : "")
				;
	}

	@Override
	public SingleOffer clone() {
		EnergySupply supplyClone = super.clone();
		EnergyRequest requestClone = this.request.clone();
		SingleOffer clone = new SingleOffer(new String(producerAgent), supplyClone, 0, requestClone);
		if(deadline!=null) {
			clone.setDeadline(new Date(this.deadline.getTime()));
		}
		if(creationTime!=null) {
			clone.setCreationTime(new Date(this.creationTime.getTime()));
		}
		if(contractTime!=null) {
			clone.setContractTime(new Date(this.contractTime.getTime()));
		}
		clone.setIsComplementary(this.isComplementary);
		clone.setId(this.id);
		clone.setAcquitted(this.acquitted);
		clone.setUsed(this.used);
		clone.setContractEventId(this.contractEventId);
		clone.setLog(this.log);
		clone.setLog2(this.log2);
		clone.setLogCancel(this.logCancel);
		clone.setTimeShiftMS(this.timeShiftMS);
		return clone;
	}
}
