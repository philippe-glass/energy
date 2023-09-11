package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.model.referential.EventMainCategory;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.WarningType;

import eu.sapere.middleware.node.NodeConfig;

public class EnergyEvent extends EnergySupply implements Cloneable, Serializable {
	private static final long serialVersionUID = 14408L;
	protected Long id;
	protected Long histoId;
	protected EventType type;
	protected EnergyEvent originEvent = null;
	protected WarningType warningType = null;
	protected PowerSlot powerUpateSlot = null;
	protected PricingTable pricingTable = null;
	protected String comment = null;

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

	public PricingTable getPricingTable() {
		return pricingTable;
	}

	public void setPricingTable(PricingTable pricingTable) {
		this.pricingTable = pricingTable;
	}

	public EnergyEvent(EventType type, String _agent, NodeConfig _location, Integer _issuerDistance, Boolean _isComplementary, Double _power, Double _powerMin, Double _powerMax, Date beginDate, Date endDate, DeviceProperties deviceProperties, PricingTable pricingTable, String _comment, long timeShiftMS) {
		super(_agent, _location, _issuerDistance, _isComplementary, _power, _powerMin, _powerMax, beginDate, endDate, deviceProperties, pricingTable, timeShiftMS);
		this.type = type;
		this.comment = _comment;
	}

	public EnergyEvent(EventType type, EnergySupply energySupply, String _comment) {
		super(energySupply.getIssuer(), energySupply.getIssuerLocation(), energySupply.getIssuerDistance()
				, energySupply.getIsComplementary(), energySupply.getPower(), energySupply.getPowerMin(), energySupply.getPowerMax() ,energySupply.getBeginDate(), energySupply.getEndDate(), energySupply.getDeviceProperties(), energySupply.getPricingTable(), energySupply.getTimeShiftMS());
		this.type = type;
		this.comment = _comment;
	}

	public String getKey() {
		StringBuffer result = new StringBuffer();
		result.append(this.issuer).append("#");
		if(type!=null) {
			result.append(type.getLabel());
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
		this.power = energySupply.getPower();
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
		result.append(this.type.getLabel()).append(" ")
		.append(this.issuer).append(" ")
		.append(this.isComplementary() ? " [COMPLEMENTARY] " : "")
		.append((super.toString()));
		if(warningType!=null) {
			result.append(" ").append(warningType.getLabel());
		}
		return result.toString();
	}

	public EnergyEvent clone() {
		EnergySupply supply = super.clone();
		EventType type2 = EventType.getByLabel(this.type.getLabel());
		EnergyEvent copy = new EnergyEvent(type2, supply, comment);
		copy.setId(id);
		if(powerUpateSlot!=null) {
			copy.setPowerUpateSlot(powerUpateSlot);
		}
		return copy;
	}

	@Override
	public EnergyEvent copyForLSA() {
		return copy(false);
	}

	public EnergyEvent copy(boolean copyIds) {
		EnergySupply supply = super.copy(copyIds);
		EventType type2 = EventType.getByLabel(this.type.getLabel());
		EnergyEvent copy = new EnergyEvent(type2, supply, comment);
		if(copyIds) {
			copy.setId(id);
		}
		if(powerUpateSlot!=null) {
			copy.setPowerUpateSlot(powerUpateSlot);
		}
		return copy;
	}

	public PowerSlot getPowerUpateSlot() {
		return powerUpateSlot;
	}

	public void setPowerUpateSlot(PowerSlot _powerUpateSlot) {
		this.powerUpateSlot = _powerUpateSlot;
	}

	public void setPowerUpates(double powerUpdate, double powerMinUpdate, double powerMaxUpdate) {
		this.powerUpateSlot = new PowerSlot(powerUpdate, powerMinUpdate, powerMaxUpdate);
	}

	public double getPowerUpdate() {
		if(powerUpateSlot != null) {
			return powerUpateSlot.getCurrent();
		}
		return 0.0;
	}

	public double getPowerMinUpdate() {
		if(powerUpateSlot != null) {
			return powerUpateSlot.getMin();
		}
		return 0.0;
	}

	public double getPowerMaxUpdate() {
		if(powerUpateSlot != null) {
			return powerUpateSlot.getMax();
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
		}
		return result;
	}
}
