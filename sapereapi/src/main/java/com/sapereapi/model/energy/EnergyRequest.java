package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.prosumerflow.ProsumerEnergyRequest;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class EnergyRequest extends EnergyFlow implements IEnergyRequest {
	private static final long serialVersionUID = 14567L;
	protected Long eventId;
	private PriorityLevel priorityLevel;
	private Double delayToleranceMinutes;
	//private Date aux_expiryDate = null;
	private Date warningDate = null;
	private Date refreshDate = null;
	private double awardsCredit = 0.0;

	public EnergyRequest(ProsumerProperties issuerProperties, Boolean isComplementary, PowerSlot powerSlot
			, Date beginDate, Date endDate, Double delayToleranceMinutes, PriorityLevel priority, Boolean disabled) {
		super(issuerProperties, isComplementary, powerSlot, beginDate, endDate, disabled);
		this.delayToleranceMinutes = delayToleranceMinutes;
		this.priorityLevel = priority;
		//this.aux_expiryDate = getCurrentDate();
		this.refreshDate = getCurrentDate();
		this.awardsCredit = 0.0;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public PriorityLevel getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(PriorityLevel priorityLevel) {
		this.priorityLevel = priorityLevel;
	}

	public Double getDelayToleranceMinutes() {
		return delayToleranceMinutes;
	}

	public void setDelayToleranceMinutes(Double delayToleranceMinutes) {
		this.delayToleranceMinutes = delayToleranceMinutes;
	}

	public Date getMaxBeginDate() {
		Date maxBeginDate = UtilDates.shiftDateMinutes(this.beginDate, delayToleranceMinutes);
		return maxBeginDate;
	}
/*
	public Date getAux_expiryDate() {
		return aux_expiryDate;
	}

	public void setAux_expiryDate(Date aux_expiryDate) {
		this.aux_expiryDate = aux_expiryDate;
	}
*/
	public double getAwardsCredit() {
		return awardsCredit;
	}

	public void setAwardsCredit(double awardsCredit) {
		this.awardsCredit = awardsCredit;
	}

	public boolean canBeSupplied() {
		Date current = getCurrentDate();
		if(current.before(beginDate)) {
			return false;
		}
		return !current.after(getMaxBeginDate()) && current.before(this.endDate);
	}

	public boolean isOK(SingleOffer singleOffer) {
		Date maxBeginDate = getMaxBeginDate();
		return singleOffer.isActive() && singleOffer.getPower() > 0 && singleOffer.isBeginDateOK(maxBeginDate);
	}

	public long getWarningDurationMS(Date current) {
		if(warningDate==null) {
			return 0;
		} else {
			return 1000+ Math.max(0, current.getTime() - warningDate.getTime());
		}
	}

	public int getWarningDurationSec() {
		long warningDuraitonMS = getWarningDurationMS(refreshDate);
		return (int) (warningDuraitonMS/1000);
	}

	public int compareWarningDesc(EnergyRequest other) {
		return -1 * (this.getWarningDurationSec() - other.getWarningDurationSec());
	}

	public int comparePriorityDesc(EnergyRequest other) {
		return -1 * this.priorityLevel.compare(other.getPriorityLevel());
	}

	public int comparePriorityDescAndPower(EnergyRequest other) {
		int comparePriority = comparePriorityDesc(other);
		if (comparePriority == 0) {
			return comparePower(other);
		} else {
			return comparePriority;
		}
	}

	public Date getRefreshDate() {
		return refreshDate;
	}

	public void setRefreshDate(Date _refreshDate) {
		this.refreshDate = _refreshDate;
	}

	public Date getWarningDate() {
		return warningDate;
	}

	public void setWarningDate(Date warningDate) {
		this.warningDate = warningDate;
	}

	public void resetWarningCounter(Date _current) {
		refreshDate = _current;
		warningDate = null;
	}

	public void incrementWarningCounter(Date _current) {
		refreshDate = _current;
		if (warningDate == null) {
			warningDate = _current;
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		if(priorityLevel==null) {
			System.out.println("EnergyRequest : priorityLevel is null");
		}
		if(priorityLevel!=null && this.priorityLevel.compare(PriorityLevel.LOW) > 0) {
			result.append(", urgency : ").append(this.priorityLevel.getLabel());
		}
		int warningDurationSec = this.getWarningDurationSec();
		if(warningDurationSec > 0) {
			result.append(", warningDurationSec : ").append(warningDurationSec);
		}
		if(this.warningDate != null) {
			result.append(" warning since ").append(UtilDates.format_time.format(warningDate));
		}
		if(this.disabled) {
			result.append("# DISABLED #");
		}
		if(this.eventId != null) {
			result.append("[eventId:").append(eventId).append("]");
		}
		if(Math.abs(this.awardsCredit) > 0) {
			result.append(" awardsCredit : ").append(SapereUtil.roundPower(awardsCredit));
		}
		return result.toString();
	}

	public EnergyRequest generateComplementaryRequest(double powerToSet) {
		EnergyRequest result = this.clone();
		result.setPowerSlot(PowerSlot.create(powerToSet));
		result.setIsComplementary(true);
		return result;
	}

	@Override
	public EnergyRequest copy(boolean addIds) {
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(addIds);
		//PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		EnergyRequest copy = new EnergyRequest(cloneIssuerProperties, isComplementary, getPowerSlot(),
				beginDate == null ? null : new Date(beginDate.getTime()),
				endDate == null ? null : new Date(endDate.getTime())
				,this.delayToleranceMinutes, this.priorityLevel, this.disabled);
		if(addIds) {
			copy.setEventId(eventId);
		}
		copy.setRefreshDate(new Date(refreshDate.getTime()));
		if(warningDate!=null) {
			copy.setWarningDate(new Date(warningDate.getTime()));
		}
		copy.setAwardsCredit(awardsCredit);
		return copy;
	}

	@Override
	public EnergyRequest copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	@Override
	public EnergyRequest clone() {
		return copy(true);
	}

	public EnergySupply generateSupply() {
		PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(true);
		EnergySupply result = new EnergySupply(cloneIssuerProperties, isComplementary, getPowerSlot(), beginDate,endDate, pricingTable, disabled);
		result.setEventId(eventId);
		return result;
	}

	public ProsumerEnergyRequest generateProsumerEnergyRequest() {
		ProsumerProperties cloneIssuerProperties = issuerProperties == null ? null : issuerProperties.clone();
		ProsumerEnergyRequest result = new ProsumerEnergyRequest(cloneIssuerProperties, isComplementary, getPowerSlot(),
				beginDate, endDate, null, delayToleranceMinutes, priorityLevel, disabled);
		result.setEventId(eventId);
		result.setRefreshDate(refreshDate);
		result.setWarningDate(warningDate);
		result.setAwardsCredit(awardsCredit);
		return result;
	}
}
