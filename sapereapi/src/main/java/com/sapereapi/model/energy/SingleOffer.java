package com.sapereapi.model.energy;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class SingleOffer extends EnergySupply implements IEnergyObject, Cloneable {
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

	public SingleOffer(String producerAgent
			, EnergySupply energySupply
			,int validityDurationSeconds
			, EnergyRequest request) {
		super(energySupply.getIssuerProperties(), request.isComplementary(), energySupply.getPowerSlot(),
				energySupply.getBeginDate(), energySupply.getEndDate(), energySupply.getPricingTable(), energySupply.getDisabled());
		Date current = getCurrentDate();
		if (beginDate.before(current)) {
			beginDate = current;
		}
		this.producerAgent = producerAgent;
		this.request = request;
		//this.isComplementary = _request.isComplementary();
		this.creationTime = getCurrentDate();
		this.deadline = UtilDates.shiftDateSec(creationTime, validityDurationSeconds);
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
		if(request==null ) {
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

	public long getRemainValidationTimeSec() {
		Date current = getCurrentDate();
		Date expiration = deadline;
		if (endDate.before(expiration)) {
			expiration = endDate;
		}
		if (current.before(expiration)) {
			long duration_ms = expiration.getTime() - current.getTime();
			return duration_ms / 1000;
		}
		return 0;
	}

	public long getExpirationDurationSec() {
		Date current = getCurrentDate();
		Date expiration = deadline;
		if(endDate.before(expiration)) {
			expiration = endDate;
		}
		if (current.after(expiration)) {
			long duration_ms = current.getTime() - expiration.getTime();
			return duration_ms/1000;
		}
		return 0;
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
				+ " W = " + UtilDates.df3.format(this.getPower()) + " from "
				+ UtilDates.format_time.format(beginDate) + " to "	+ UtilDates.format_time.format(endDate)
				+ " (" + UtilDates.df.format(this.getTimeCoverRate()) + ")"
				+ (acquitted!=null && this.acquitted? " acquitted " : "### NOT ACQUITTED ###")
				+ (used!=null && this.used? " used " : "")
				+ (accepted!=null && this.accepted? " accepted " : "")
				+ (contractEventId!=null ? " has contract " : "")
				;
	}

	public SingleOffer copy(boolean copyIds) {
		EnergySupply supplyClone = super.copy(copyIds);
		EnergyRequest requestClone = this.request.copy(copyIds);
		SingleOffer copy = new SingleOffer(new String(producerAgent), supplyClone, 0, requestClone);
		if(deadline!=null) {
			copy.setDeadline(new Date(this.deadline.getTime()));
		}
		if(creationTime!=null) {
			copy.setCreationTime(new Date(this.creationTime.getTime()));
		}
		if(contractTime!=null) {
			copy.setContractTime(new Date(this.contractTime.getTime()));
		}
		copy.setIsComplementary(this.isComplementary);
		if(copyIds) {
			copy.setId(this.id);
		}
		copy.setAcquitted(this.acquitted);
		copy.setUsed(this.used);
		copy.setContractEventId(this.contractEventId);
		copy.setLog(this.log);
		copy.setLog2(this.log2);
		copy.setLogCancel(this.logCancel);
		return copy;
	}

	@Override
	public SingleOffer clone() {
		return copy(true);
	}

	public SingleOffer copyForLSA() {
		return copy(false);
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		super.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
		if(request != null) {
			request.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
		}
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result= super.retrieveInvolvedLocations();
		if(request != null) {
			for(NodeLocation nodeLocation : request.retrieveInvolvedLocations()) {
				if(!result.contains(nodeLocation)) {
					result.add(nodeLocation);
				}
			}
		}
		return result;
	}
}
