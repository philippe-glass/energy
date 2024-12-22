package com.sapereapi.model.learning;

import java.io.Serializable;
import java.text.DecimalFormat;

import com.sapereapi.lightserver.DisableJson;

public class VariableState implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	private static DecimalFormat df = new DecimalFormat("# W");
	private Integer id = null;
	private BooleanOperator minCondition = null;
	private Double minValue = null;
	private BooleanOperator maxCondition = null;
	private Double maxValue = null;
	private String label = null;

	public VariableState(Integer id, BooleanOperator minCondition, Double minValue, BooleanOperator maxCondition,
			Double maxValue) {
		super();
		this.id = id;
		this.minCondition = minCondition;
		this.minValue = minValue;
		this.maxCondition = maxCondition;
		this.maxValue = maxValue;
		this.label = "";
		boolean isEqualCond = BooleanOperator.EQUALS.equals(minCondition);
		if(this.minCondition !=null) {
			this.label = minCondition.getIntervalMarker(minValue, df);
		} else {
			this.label = "]-∞";
		}
		if(!isEqualCond) {
			this.label = this.label + "," ;
			if(this.maxCondition !=null) {
				if(this.label.length()>0) {
				}
				this.label = this.label + maxCondition.getIntervalMarker(maxValue, df);
			} else {
				this.label = this.label +  "+∞[";
			}
		}
	}

	public boolean containsValue(Double value) {
		boolean result = true;
		if (minCondition != null && minValue != null) {
			result = result && (minCondition.apply(value, minValue));
		}
		if (result && maxCondition != null && maxValue != null) {
			result = result && (maxCondition.apply(value, maxValue));
		}
		return result;
	}

	public Integer getIndex() {
		return id-1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return "S" + id;
	}

	@DisableJson
	public BooleanOperator getMinCondition() {
		return minCondition;
	}

	public void setMinCondition(BooleanOperator minCondition) {
		this.minCondition = minCondition;
	}

	public Double getMinValue() {
		return minValue;
	}

	public void setMinValue(Double minValue) {
		this.minValue = minValue;
	}

	@DisableJson
	public BooleanOperator getMaxCondition() {
		return maxCondition;
	}

	public void setMaxCondition(BooleanOperator maxCondition) {
		this.maxCondition = maxCondition;
	}

	public Double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(Double maxValue) {
		this.maxValue = maxValue;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public VariableState clone() {
		VariableState result = new VariableState(id, minCondition, minValue, maxCondition, maxValue);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof VariableState) {
			VariableState other = (VariableState) obj;
			return this.id.equals(other.getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		if(id==null) {
			return -1;
		}
		return this.id.intValue();
	}

	@Override
	public String toString() {
		String name = getName();
		if(name != null && name.length()>0) {
			return name;
		}
		if(label.length()>0) {
			return label;
		}
		boolean displayCondition = false;
		StringBuffer result = new StringBuffer();
		result.append(getName());
		if(displayCondition) {
			result.append(" : [");
			String separator= "";
			if (minCondition != null) {
				result.append("?").append(minCondition.getLabel()).append(" ").append(minValue);
				separator = " && ";
			}
			if (maxCondition != null) {
				result.append(separator);
				result.append("?").append(maxCondition.getLabel()).append(" ").append(maxValue);
			}
			result.append("]");
		}
		return result.toString();
	}
}
