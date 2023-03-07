package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

public class EnergyRequest extends EnergySupply implements IEnergyObject, Serializable {
	private static final long serialVersionUID = 14567L;
	private PriorityLevel priorityLevel;
	private Double delayToleranceMinutes;
	private Date aux_expiryDate = null;
	private Date warningDate = null;
	private Date refreshDate = null;

	public EnergyRequest(String issuer, String _location, Boolean _isComplementary, Double _power, Double _powerMin, Double _powerMax, Date beginDate, Date endDate, Double _delayToleranceMinutes,
			PriorityLevel _priority, DeviceProperties deviceProperties, PricingTable pricingTable, long timeShiftMS) {
		super(issuer, _location, _isComplementary, _power, _powerMin, _powerMax, beginDate, endDate, deviceProperties, pricingTable, timeShiftMS);
		this.delayToleranceMinutes = _delayToleranceMinutes;
		this.priorityLevel = _priority;
		this.aux_expiryDate = getCurrentDate();
		this.refreshDate = getCurrentDate();
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

	public int compareDistance(EnergyRequest other) {
		return this.issuerDistance - other.getIssuerDistance();
	}

	public int comparePower(EnergyRequest other) {
		double auxPpowerComparison = this.power - other.getPower();
		return (int) (100*auxPpowerComparison);
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
		return result.toString();
	}

	public EnergyRequest generateComplementaryRequest(double powerToSet) {
		EnergyRequest result = this.clone();
		result.setPower(powerToSet);
		result.setPowerMin(powerToSet);
		result.setPowerMax(powerToSet);
		result.setIsComplementary(true);
		return result;
	}

	@Override
	public EnergyRequest clone() {
		EnergySupply supply = super.clone();
		EnergyRequest clone = new EnergyRequest(supply.getIssuer(), supply.getIssuerLocation()
				, supply.getIsComplementary()
				, supply.getPower(), supply.getPowerMin(), supply.getPowerMax()
				, supply.getBeginDate()==null? null : new Date(supply.getBeginDate().getTime())
				, supply.getBeginDate()==null? null : new Date(supply.getEndDate().getTime())
				, this.delayToleranceMinutes
				, this.priorityLevel
				, this.deviceProperties
				, this.pricingTable==null ? null : this.pricingTable.clone()
				, this.timeShiftMS
				);
		clone.setEventId(eventId);
		clone.setRefreshDate(new Date(refreshDate.getTime()));
		if(warningDate!=null) {
			clone.setWarningDate(new Date(warningDate.getTime()));
		}
		clone.setIsComplementary(Boolean.valueOf(isComplementary.booleanValue()));
		return clone;
	}
}
