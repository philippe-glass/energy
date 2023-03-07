package com.sapereapi.model.energy;

import java.util.Date;
import java.util.Set;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

/**
 * Contract informaitons with the reduced visibility of a producer agent
 * 
 * @author phili
 *
 */
public class ReducedContract extends EnergySupply implements IEnergyObject {
	/**
	 * 
	 */
	public static final String ALL_OTHERS = "all_others";
	private static final long serialVersionUID = 1L;

	private EnergyRequest request;
	private Set<String> agreements = null;
	private Set<String> disagreements = null;
	private Date validationDeadline;
	private boolean isMerged = false;

	public EnergyRequest getRequest() {
		return request;
	}

	public void setRequest(EnergyRequest request) {
		this.request = request;
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

	public void setDisagreements(Set<String> disagreements) {
		this.disagreements = disagreements;
	}

	public boolean isMerged() {
		return isMerged;
	}

	public void setMerged(boolean isMerged) {
		this.isMerged = isMerged;
	}

	public boolean hasAllAgreements() {
		if (hasDisagreement()) {
			return false;
		}
		return agreements.contains(ALL_OTHERS) && agreements.contains(this.issuer);
	}

	public boolean hasDisagreement() {
		return disagreements.size() > 0;
	}

	public boolean hasProducerAgreement() {
		return agreements.contains(this.issuer);
	}

	public boolean hasProducerDisagreement() {
		return disagreements.contains(this.issuer);
	}

	public boolean isOnGoing() {
		return hasAllAgreements() && !hasExpired();
	}

	public boolean waitingValidation() {
		return !hasDisagreement() && !hasAllAgreements();
	}

	public Date getValidationDeadline() {
		return validationDeadline;
	}

	public void setValidationDeadline(Date validationDeadline) {
		this.validationDeadline = validationDeadline;
	}

	public boolean validationHasExpired() {
		if (this.waitingValidation() && this.validationDeadline != null) {
			Date current = getCurrentDate();
			return current.after(this.validationDeadline);
		}
		return false;
	}

	public String getStatus() {
		if (this.hasExpired()) {
			return "Expired";
		} else if (this.hasDisagreement()) {
			return "Canceled";
		} else if (this.hasAllAgreements()) {
			return "Confirmed";
		} else if (this.waitingValidation()) {
			return "Waiting ";
		}
		return "";
	}

	public int getIssuerDistance() {
		return request.getIssuerDistance();
	}

	public String getConsumerAgent() {
		return request.getIssuer();
	}

	public String getConsumerLocation() {
		if (request == null) {
			return "";
		}
		return request.getIssuerLocation();
	}

	public ReducedContract(EnergyRequest _request, EnergySupply supply, Date _validationDeadline) {
		super(supply.getIssuer(), supply.getIssuerLocation(), _request.isComplementary(), supply.getPower(), supply.getPowerMin(),
				supply.getPowerMax(), supply.getBeginDate(), supply.getEndDate(), supply.getDeviceProperties(),
				supply.getPricingTable(), supply.getTimeShiftMS());
		this.request = _request;
		this.validationDeadline = _validationDeadline;
		//this.isComplementary = request.isComplementary;
	}

	public PowerSlot getForcastProducerPowerSlot(Date aDate) {
		if(this.isInActiveSlot(aDate)) {
			return new PowerSlot(power, powerMin, powerMax);
		} else {
			return new PowerSlot();
		}
	}

	public PowerSlot getProducerPowerSlot() {
		return new PowerSlot(power, powerMin, powerMax);
	}

	public ReducedContract clone() {
		PricingTable clonePricingTable = pricingTable == null ? null : pricingTable.clone();
		EnergySupply cloneSupply = new EnergySupply(issuer, issuerLocation, isComplementary, power, powerMin, powerMax, beginDate,
				endDate, deviceProperties, clonePricingTable, timeShiftMS);
		ReducedContract result = new ReducedContract(request.clone(), cloneSupply, validationDeadline);
		result.setAgreements(SapereUtil.cloneSetStr(agreements));
		result.setDisagreements(SapereUtil.cloneSetStr(disagreements));
		result.setMerged(isMerged);
		return result;
	}

	public void addProducerAgreement(boolean isOk) {
		if (isOk) {
			this.agreements.add(this.issuer);
		} else {
			this.disagreements.add(this.issuer);
		}
	}

	public String formatContent() {
		StringBuffer result = new StringBuffer();
		result.append(this.getConsumerAgent());
		String supplyStr = super.toString();
		result.append(" : ").append(supplyStr);
		result.append(" status:").append(getStatus());
		if(isMerged) {
			result.append(" (merged)");
		}
		if(waitingValidation() && validationDeadline != null) {
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
		return formatContent();
	}
}
