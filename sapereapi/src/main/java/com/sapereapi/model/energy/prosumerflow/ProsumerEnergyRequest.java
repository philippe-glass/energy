package com.sapereapi.model.energy.prosumerflow;

import java.util.Date;

import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.ExtraSupply;
import com.sapereapi.model.energy.IEnergyRequest;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class ProsumerEnergyRequest extends ProsumerEnergyFlow implements IEnergyRequest {
	private static final long serialVersionUID = 14567L;
	protected Long eventId;
	private PriorityLevel priorityLevel;
	private Double delayToleranceMinutes;
	private Date aux_expiryDate = null;
	private Date warningDate = null;
	private Date refreshDate = null;
	private double awardsCredit = 0.0;

	public ProsumerEnergyRequest(ProsumerProperties issuerProperties, Boolean isComplementary,
			PowerSlot internalPowerSlot, Date beginDate, Date endDate, ExtraSupply extraSupply,
			Double delayToleranceMinutes, PriorityLevel priority) {
		super(issuerProperties, isComplementary, internalPowerSlot, beginDate, endDate, extraSupply);
		this.delayToleranceMinutes = delayToleranceMinutes;
		this.priorityLevel = priority;
		this.aux_expiryDate = getCurrentDate();
		this.refreshDate = getCurrentDate();
		this.awardsCredit = 0.0;
	}

	@Override
	public Double getPower() {
		return getInternalPower() - getAdditionalPower();
	}

	@Override
	public Double getPowerMin() {
		return getInternalPowerMin() -  getAdditionalPower();
	}

	@Override
	public Double getPowerMax() {
		return getInternalPowerMax() -  getAdditionalPower();
	}

	@Override
	public PowerSlot getPowerSlot() {
		PowerSlot result = this.internalPowerSlot.clone();
		result.substract(getAdditionalPowerSlot());
		return result;
	}

	@Override
	public Double getWH() {
		Double result = getInternalWH();
		if (extraSupply != null) {
			result -= this.extraSupply.getWH();
		}
		return result;
	}

	public Double getRemainWH() {
		Double result = getRemainInternalWH();
		if (extraSupply != null) {
			result -= this.extraSupply.getRemainWH();
		}
		return result;
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

	public Date getAux_expiryDate() {
		return aux_expiryDate;
	}

	public void setAux_expiryDate(Date aux_expiryDate) {
		this.aux_expiryDate = aux_expiryDate;
	}

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

	public int compareWarningDesc(ProsumerEnergyRequest other) {
		return -1 * (this.getWarningDurationSec() - other.getWarningDurationSec());
	}

	public int comparePriorityDesc(ProsumerEnergyRequest other) {
		return -1 * this.priorityLevel.compare(other.getPriorityLevel());
	}


	public int comparePriorityDescAndPower(ProsumerEnergyRequest other) {
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
			result.append(", awardsCredit : ").append(SapereUtil.roundPower(awardsCredit));
		}
		if(this.extraSupply != null) {
			result.append(", extraSupply : ").append(extraSupply);
		}
		return result.toString();
	}

	public ProsumerEnergyRequest generateComplementaryRequest(double powerToSet) {
		ProsumerEnergyRequest result = this.clone();
		result.setInternalPowerSlot(PowerSlot.create(powerToSet));
		result.setIsComplementary(true);
		return result;
	}

	public ProsumerEnergyRequest copy(boolean addIds) {
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(addIds);
		ExtraSupply cloneExtraSupply = extraSupply == null ? null : extraSupply.clone();
		//PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		ProsumerEnergyRequest copy = new ProsumerEnergyRequest(cloneIssuerProperties, isComplementary, getInternalPowerSlot()
				,beginDate == null ? null : new Date(beginDate.getTime())
				,endDate == null ? null : new Date(endDate.getTime())
				,cloneExtraSupply
				,this.delayToleranceMinutes, this.priorityLevel);
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
	public ProsumerEnergyRequest copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	@Override
	public ProsumerEnergyRequest clone() {
		return copy(true);
	}

	public EnergyRequest generateSimpleRequest() {
		ProsumerProperties cloneIssuerProperties = issuerProperties.clone();
		EnergyRequest result = new EnergyRequest(cloneIssuerProperties, isComplementary, getPowerSlot(),
				beginDate, endDate, this.delayToleranceMinutes, this.priorityLevel);
		result.setEventId(eventId);
		result.setRefreshDate(new Date(refreshDate.getTime()));
		if(warningDate!=null) {
			result.setWarningDate(new Date(warningDate.getTime()));
		}
		result.setAwardsCredit(awardsCredit);
		return result;
	}

	public ProsumerEnergySupply generateSupply() {
		PricingTable pricingTable = new PricingTable(issuerProperties.getTimeShiftMS());
		ExtraSupply cloneExtraSupply = (this.extraSupply == null) ? null : extraSupply.clone();
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(true);
		ProsumerEnergySupply result = new ProsumerEnergySupply(cloneIssuerProperties, isComplementary,
				getInternalPowerSlot(), beginDate, endDate, cloneExtraSupply, pricingTable);
		result.setEventId(eventId);
		return result;
	}
}
