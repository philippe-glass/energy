package eu.sapere.middleware.lsa;

import java.io.Serializable;

public class AggregatorProperty implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String operator;
	private AggregatorType type;
	private String propertyName;
	private Boolean activateGossip;
	//private Integer index;

	public String getOperator() {
		return operator;
	}

	public void setOperator(String name) {
		this.operator = name;
	}

	public AggregatorType getType() {
		return type;
	}

	public void setType(AggregatorType type) {
		this.type = type;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String field) {
		this.propertyName = field;
	}

	public Boolean getActivateGossip() {
		return activateGossip;
	}

	public void setActivateGossip(Boolean applyOnNodes) {
		this.activateGossip = applyOnNodes;
	}

	public AggregatorProperty(String name, AggregatorType type, String field, Boolean applyOnNodes) {
		super();
		this.operator = name;
		this.type = type;
		this.propertyName = field;
		this.activateGossip = applyOnNodes;
	}

	public boolean isStandard() {
		return AggregatorType.STANDARD.equals(type);
	}

	public boolean isCustomized() {
		return AggregatorType.CUSTOMIZED.equals(type);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if(isCustomized()) {
			result.append("custom_");
		} else if (isStandard()) {
			result.append("std_");
		}
		result.append(operator);
		result.append(":").append(propertyName);
		if(activateGossip) {
			result.append("$activateGossip");
		}
		//result.append(name).append(":").append(isCustomized()? "(customized)" : "").append(field).append("-").append(applyOnNodes ? " on all nodes" : "");
		return result.toString();
	}

	@Override
	public int hashCode() {
		int bPArt = propertyName == null ? 0 : propertyName.hashCode();
		bPArt = 2*bPArt + type.getIndex();
		bPArt = 2*bPArt + (activateGossip ? 1 :0);
		int nameCode = operator == null ? 0 : operator.hashCode();
		return bPArt*100 + nameCode;
	}


	@Override
	public boolean equals(Object obj) {
		if(obj instanceof AggregatorProperty) {
			AggregatorProperty other = (AggregatorProperty) obj;
			boolean typesEqual = (type ==null) ? (other.getType() == null) : type.equals(other.getType());
			if(typesEqual) {
				boolean namesEqual = (type ==null) ? (other.getOperator() == null) : operator.equals(other.getOperator());
				if(namesEqual) {
					boolean fieldsEqual = (propertyName ==null) ? (other.getPropertyName() == null) : propertyName.equals(other.getPropertyName());
					if(fieldsEqual) {
						boolean applyOnNodesEqual = (activateGossip ==null) ? (other.getActivateGossip() == null) : activateGossip.equals(other.getActivateGossip());
						return applyOnNodesEqual;
					}
				}
			}
		}
		return false;
	}

}
