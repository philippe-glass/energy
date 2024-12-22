package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.model.referential.EventMainCategory;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class EnergyEvent extends EnergyFlow implements Cloneable, Serializable {
	private static final long serialVersionUID = 14408L;
	protected Long id;
	protected Long histoId;
	protected EventType type;
	protected EnergyEvent originEvent = null;
	protected WarningType warningType = null;
	protected PowerSlot powerUpdateSlot = null;
	protected String comment = null;
	protected Double firstRate = null;
	protected Double additionalPower = 0.0;

	public final static String CLASS_OK = "warning_ok";
	public final static String CLASS_OK_LIGHT = "warning_ok_light";
	public final static String CLASS_WARNING_LIGHT = "warning_light";
	public final static String CLASS_WARNING_MEDIUM = "warning_medium";
	public final static String CLASS_WARNING_HIGH = "warning_high";


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EventType getType() {
		return type;
	}

	public EventObjectType getEventObjectType() {
		if(type != null) {
			return type.getObjectType();
		}
		return null;
	}

	public EventMainCategory getEventMainCategory() {
		if(type != null) {
			return type.getMainCategory();
		}
		return null;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public EnergyEvent getOriginEvent() {
		return originEvent;
	}

	public void setOriginEvent(EnergyEvent originEvent) {
		this.originEvent = originEvent;
	}


	public WarningType getWarningType() {
		return warningType;
	}

	public void setWarningType(WarningType warningType) {
		this.warningType = warningType;
	}

	public Long getHistoId() {
		return histoId;
	}

	public void setHistoId(Long histoId) {
		this.histoId = histoId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Double getFirstRate() {
		return firstRate;
	}

	public void setFirstRate(Double firstRate) {
		this.firstRate = firstRate;
	}

	public Double getAdditionalPower() {
		return additionalPower;
	}

	public void setAdditionalPower(Double additionalPower) {
		this.additionalPower = additionalPower;
	}

	public EnergyEvent(EventType type
			, ProsumerProperties prosumerProperties
			, Boolean isComplementary
			, PowerSlot powerSlot
			, Date beginDate, Date endDate
			, String comment, Double firstRate) {
		super(prosumerProperties, isComplementary, powerSlot, beginDate, endDate);
		this.type = type;
		this.comment = comment;
		this.firstRate = firstRate;
	}

	public String getKey() {
		StringBuffer result = new StringBuffer();
		String issuer = getIssuer();
		result.append(issuer).append("#");
		if(type!=null) {
			result.append(type.name());
		}
		result.append("#");
		if(beginDate!=null) {
			long timeSec = this.beginDate.getTime()/1000;
			result.append(timeSec);
		}
		result.append("#");
		result.append(isComplementary()?"sd":"");
		return result.toString();
	}

	public void setSupplyFields(EnergySupply energySupply) {
		//this.power = energySupply.getPower();
		this.powerSlot = energySupply.getPowerSlot();
		this.beginDate = energySupply.getBeginDate();
		this.endDate = energySupply.getEndDate();
	}

	public boolean hasExpired() {
		Date current = getCurrentDate();
		return current.after(this.endDate);
	}

	public boolean isActive() {
		Date current = getCurrentDate();
		return (!current.before(beginDate)) && current.before(this.endDate);
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("[id:").append(id).append("]");
		String issuer = getIssuer();
		result.append(this.type.name()).append(" ")
			.append(issuer).append(" ")
			.append(this.isComplementary() ? " [COMPLEMENTARY] " : "");
		result.append(":");
		result.append(UtilDates.df3.format(getPower())).append(" W from ");
		result.append(UtilDates.formatTimeOrDate(beginDate, getTimeShiftMS()));
		result.append(" to ");
		result.append(UtilDates.formatTimeOrDate(endDate, getTimeShiftMS()));
		if(this.disabled) {
			result.append("# DISABLED #");
		}
		if(warningType!=null) {
			result.append(" ").append(warningType.getLabel());
		}
		return result.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof EnergyEvent)) {
			return false;
		}
		EnergyEvent other = (EnergyEvent) obj;
		if (beginDate != null && !this.beginDate.equals(other.getBeginDate())) {
			return false;
		}
		if (endDate != null && !this.endDate.equals(other.getEndDate())) {
			return false;
		}
		if (type != null && !this.type.equals(other.getType())) {
			return false;
		}
		if (issuerProperties != null && !this.issuerProperties.equals(other.getIssuerProperties())) {
			return false;
		}
		if (id != null && !this.id.equals(other.getId())) {
			return false;
		}
		if (histoId != null && !this.histoId.equals(other.getHistoId())) {
			return false;
		}
		if (getPower() != null && !this.getPower().equals(other.getPower())) {
			return false;
		}
		return true;
	}

	public EnergyEvent clone() {
		return copy(true);
	}

	@Override
	public EnergyEvent copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	public EnergyEvent copy(boolean copyIds) {
		//PricingTable copyPricingTable = pricingTable==null ? null : pricingTable.copy(copyIds);
		ProsumerProperties cloneIssuerProperties = issuerProperties.copy(copyIds);
		EnergyEvent copy = new EnergyEvent(type, cloneIssuerProperties, isComplementary, getPowerSlot(), beginDate, endDate, comment, firstRate);
		if(copyIds) {
			copy.setId(id);
			copy.setHistoId(histoId);
		}
		return copy;
	}

	public PowerSlot getPowerUpdateSlot() {
		return powerUpdateSlot;
	}

	public void setPowerUpdateSlot(PowerSlot _powerUpdateSlot) {
		this.powerUpdateSlot = _powerUpdateSlot;
	}

	public void setPowerUpates(double powerUpdate, double powerMinUpdate, double powerMaxUpdate) {
		this.powerUpdateSlot = new PowerSlot(powerUpdate, powerMinUpdate, powerMaxUpdate);
	}

	public double getPowerUpdate() {
		if(powerUpdateSlot != null) {
			return powerUpdateSlot.getCurrent();
		}
		return 0.0;
	}

	public double getPowerMinUpdate() {
		if(powerUpdateSlot != null) {
			return powerUpdateSlot.getMin();
		}
		return 0.0;
	}

	public double getPowerMaxUpdate() {
		if(powerUpdateSlot != null) {
			return powerUpdateSlot.getMax();
		}
		return 0.0;
	}

	public boolean canStopMissingWarning() {
		// a new contract or contract raise can top a warning
		if(EventObjectType.CONTRACT.equals(this.getEventObjectType())) {
			if(type.isStart()) {
				return true;
			}
			if(type.isUpdate() && this.getPowerUpdate() > 0) {
				return true;
			}
		}
		// A decrease of request/production can stop a warning
		if(EventObjectType.REQUEST.equals(this.getEventObjectType())
			|| EventObjectType.PRODUCTION.equals(this.getEventObjectType())) {
			if(this.type.getIsEnding()) {
				return true;
			}
			if(type.isUpdate() && this.getPowerUpdate() < 0) {
				return true;
			}
		}
		return false;
	}

	public String getWarningClassification() {
		String result = "";
		double powerUpdate = this.getPowerUpdate();
		EventObjectType evtObjectType = getEventObjectType();
		// Contract events
		if(EventObjectType.CONTRACT.equals(evtObjectType)) {
			double powerMinUpdate = this.getPowerMinUpdate();
			boolean isContractMerge = WarningType.CONTRACT_MERGE.equals(warningType);
			if(type.isStart()) {
				result = CLASS_OK;
			}
		    if(type.isUpdate()) {
		      if(powerMinUpdate < 0 && isContractMerge) {
			        result = CLASS_WARNING_LIGHT;
		      }
		      result = CLASS_OK_LIGHT;
		    }
		    if(type.isStop()) {
		      if(isContractMerge) {
		        result = CLASS_OK_LIGHT;
		      } else {
			    result = CLASS_WARNING_HIGH;
		      }
		    }
		}
		// Request event
		if(EventObjectType.REQUEST.equals(evtObjectType)) {
			if(type.isStart()) {
		      result = CLASS_WARNING_MEDIUM;
		    }
			if(type.isUpdate()) {
		      if(powerUpdate < 0) {
		        result = CLASS_OK_LIGHT;
		      }
		      if(powerUpdate > 0) {
		        result = CLASS_WARNING_MEDIUM;
		      }
		    }
			if(type.isSwitch()) {
				result = CLASS_WARNING_MEDIUM;
			}
			if(type.getIsEnding()) {
				result = CLASS_OK_LIGHT;
			}
		}
		// Production event
		if(EventObjectType.PRODUCTION.equals(evtObjectType)) {
			if(type.isStart()) {
		      result = CLASS_OK_LIGHT;
		    }
			if(type.isUpdate()) {
		      if(powerUpdate > 0) {
		        result = CLASS_OK_LIGHT;
		      }
		      if(powerUpdate < 0) {
		        result = CLASS_WARNING_MEDIUM;
		      }
		    }
			if(type.getIsEnding()) {
				result = CLASS_WARNING_HIGH;
			}
			if(type.isSwitch()) {
				result = CLASS_OK_LIGHT;
			}
		}
		return result;
	}
}
