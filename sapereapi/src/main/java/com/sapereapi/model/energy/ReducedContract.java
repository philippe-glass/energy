package com.sapereapi.model.energy;

import java.util.Date;
import java.util.Set;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeLocation;

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
		String issuer = getIssuer();
		return agreements.contains(ALL_OTHERS) && agreements.contains(issuer);
	}

	public boolean hasDisagreement() {
		return disagreements.size() > 0;
	}

	public boolean hasProducerAgreement() {
		String issuer = getIssuer();
		return agreements.contains(issuer);
	}

	public boolean hasProducerDisagreement() {
		String issuer = getIssuer();
		return disagreements.contains(issuer);
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
		if(request.getIssuerProperties() == null) {
			return 0;
		}
		return request.getIssuerProperties().getDistance();
	}

	public String getConsumerAgent() {
		return request.getIssuer();
	}

	public NodeLocation getConsumerLocation() {
		if (request == null || request.getIssuerProperties() == null) {
			return null;
		}
		return request.getIssuerProperties().getLocation();
	}

	public ReducedContract(EnergyRequest request
			, EnergySupply supply
			, Date validationDeadline) {
		super(supply.getIssuerProperties(), request.isComplementary(), supply.getPowerSlot(), supply.getBeginDate(), supply.getEndDate(), supply.getPricingTable());
		this.request = request;
		this.validationDeadline = validationDeadline;
	}

	public PowerSlot getForcastProducerPowerSlot(Date aDate) {
		if(this.isInActiveSlot(aDate)) {
			return powerSlot.clone();
		} else {
			return new PowerSlot();
		}
	}

	public PowerSlot getProducerPowerSlot() {
		return powerSlot.clone();
	}

	public ReducedContract copy(boolean copyIds) {
		PricingTable clonePricingTable = pricingTable == null ? null : pricingTable.clone();
		ProsumerProperties cloneIssuerProperties = this.issuerProperties.copy(copyIds);
		EnergySupply cloneSupply = new EnergySupply(cloneIssuerProperties, isComplementary, getPowerSlot(),
				beginDate, endDate, clonePricingTable);
		ReducedContract result = new ReducedContract(request.copy(copyIds), cloneSupply, validationDeadline);
		result.setAgreements(SapereUtil.cloneSetStr(agreements));
		result.setDisagreements(SapereUtil.cloneSetStr(disagreements));
		result.setMerged(isMerged);
		return result;
	}

	@Override
	public ReducedContract copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	@Override
	public ReducedContract clone() {
		return copy(true);
	}

	public void addProducerAgreement(boolean isOk) {
		String issuer = getIssuer();
		if (isOk) {
			this.agreements.add(issuer);
		} else {
			this.disagreements.add(issuer);
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
