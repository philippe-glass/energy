package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public abstract class AbstractEnergyFlow implements IEnergyFlow {
	private static final long serialVersionUID = 1L;
	protected ProsumerProperties issuerProperties;
	protected Date beginDate;
	protected Date endDate;
	protected Boolean isComplementary = Boolean.FALSE;
	protected Boolean disabled = Boolean.FALSE;
	/*
	 * abstract public Double getPower();
	 *
	 * abstract public void setPower(Double _power);
	 *
	 * abstract public Double getPowerMin();
	 *
	 * abstract public void setPowerMin(Double powerMin);
	 *
	 * abstract public Double getPowerMax();
	 *
	 * abstract public void setPowerMax(Double powerMax);
	 *
	 * public abstract EnergyEvent generateEvent(EventType type, String comment);
	 *
	 * public abstract EnergySupply generateSupply();
	 *
	 * public abstract EnergyRequest generateRequest();
	 *
	 * public abstract Double getPowerMargin();
	 *
	 * public abstract Double getKWH();
	 *
	 * public abstract Double getWH();
	 *
	 * public abstract PowerSlot getPowerSlot();
	 *
	 * public abstract PowerSlot getForcastPowerSlot(Date aDate);
	 *
	 * public abstract void checkPowers() throws HandlingException;
	 *
	 * public abstract int comparePower(AbstractEnergyFlow other);
	 *
	 * public abstract AbstractEnergyFlow copy(boolean copyIds);
	 *
	 */

	abstract public AbstractEnergyFlow clone();

	abstract public AbstractEnergyFlow copyForLSA(AbstractLogger logger);

	public ProsumerProperties getIssuerProperties() {
		return issuerProperties;
	}

	public void setIssuerProperties(ProsumerProperties issuerProperties) {
		this.issuerProperties = issuerProperties;
	}

	public boolean isIssuerLocal() {
		if (issuerProperties != null) {
			return issuerProperties.isLocal();
		}
		return false;
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isComplementary() {
		return isComplementary;
	}

	public boolean getIsComplementary() {
		return isComplementary;
	}

	public boolean isMain() {
		return !isComplementary;
	}

	public boolean getIsMain() {
		return !isComplementary;
	}

	public void setIsComplementary(Boolean complementary) {
		this.isComplementary = complementary;
	}

	public boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public Double getDuration() {
		double duration = UtilDates.computeDurationMinutes(beginDate, endDate);
		return duration;
	}

	public void checkBeginNotPassed() {
		Date current = getCurrentDate();
		if (current.after(beginDate)) {
			beginDate = current;
		}
		if (beginDate.after(endDate)) {
			if (issuerProperties != null) {
				SapereLogger.getInstance().error("checkBeginNotPassed : begin date is after end date for device "
						+ issuerProperties.getDeviceProperties() + " : begin = "
						+ UtilDates.format_time.format(beginDate) + ", end = " + UtilDates.format_time.format(endDate));
			}
		}
	}

	public boolean isStartInFutur() {
		Date current = getCurrentDate();
		return current.before(beginDate);
	}

	public TimeSlot getTimeSlot() {
		return new TimeSlot(this.beginDate, this.endDate);
	}

	public long getTimeShiftMS() {
		if (issuerProperties != null) {
			return issuerProperties.getTimeShiftMS();
		}
		return 0;
	}

	@Override
	public Date getCurrentDate() {
		return UtilDates.removeMilliSeconds(UtilDates.getNewDate(getTimeShiftMS()));
	}

	public void checkDates() throws HandlingException {
		if (this.beginDate.after(endDate)) {
			String issuer = this.getIssuer();
			throw new HandlingException(this.getClass().getName() + ".checkDates " + issuer + " : beginDate "
					+ UtilDates.formatTimeOrDate(beginDate, getTimeShiftMS()) + " is after "
					+ UtilDates.formatTimeOrDate(endDate, getTimeShiftMS()));
		}
	}

	public boolean hasExpired() {
		Date current = getCurrentDate();
		return !current.before(this.endDate);
	}

	public boolean isActive() {
		Date current = getCurrentDate();
		return (!current.before(beginDate)) && current.before(this.endDate);
	}

	public boolean isInActiveSlot(Date aDate) {
		return aDate != null && (!aDate.before(beginDate)) && aDate.before(this.endDate);
	}

	public boolean isBeginDateOK(Date maxBeginDate) {
		return !this.beginDate.after(maxBeginDate);
	}

	public long getTotalDurationSec() {
		return (long) UtilDates.computeDurationSeconds(beginDate, endDate);
	}

	public long getTotalDurationMS() {
		return endDate.getTime() - beginDate.getTime();
	}

	public long getTimeLeftMS(boolean addWaitingBeforeStart) {
		long currentMS = (getCurrentDate()).getTime();
		long beginMS = beginDate.getTime();
		if (addWaitingBeforeStart || (currentMS > beginMS)) {
			return Math.max(0, endDate.getTime() - currentMS);
		} else {
			return Math.max(0, endDate.getTime() - beginMS);
		}
	}

	public int getTimeLeftSec(boolean addWaitingBeforeStart) {
		long timeLeftMS = getTimeLeftMS(addWaitingBeforeStart);
		if (timeLeftMS > 0) {
			return (int) timeLeftMS / 1000;
		}
		return 0;
	}

	public int comparTimeLeft(AbstractEnergyFlow other) {
		return (this.getTimeLeftSec(false) - other.getTimeLeftSec(false));
	}

	public int compareDistance(AbstractEnergyFlow other) {
		if (issuerProperties != null && other.getIssuerProperties() != null) {
			issuerProperties.compareDistance(other.getIssuerProperties());
		}
		return 0;
	}

	public int compareEnvironmentImpact(SingleOffer other) {
		if(issuerProperties != null && other.getIssuerProperties() != null) {
			issuerProperties.compareEnvironmentImpact(other.getIssuerProperties());
		}
		return 0;
	}

	public String getIssuer() {
		if (issuerProperties != null) {
			return this.issuerProperties.getAgentName();
		}
		return "";
	}

	public boolean checkLocation() {
		return issuerProperties != null && issuerProperties.getLocation() != null;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		if (issuerProperties != null) {
			issuerProperties.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
		}
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		if (issuerProperties != null) {
			return issuerProperties.retrieveInvolvedLocations();
		}
		return new ArrayList<NodeLocation>();
	}

	public boolean hasChanged(AbstractEnergyFlow newContent) {
		if (newContent == null) {
			return true;
		}
		String issuer = this.getIssuer();
		String newContentIssuer = newContent.getIssuer();
		if (!issuer.equals(newContentIssuer)) {
			return true;
		}
		Date longDate = UtilDates.shiftDateDays(getCurrentDate(), 1000);
		if (!this.endDate.equals(newContent.getEndDate()) && endDate.before(longDate)) {
			return true;
		}
		return false;
	}
}