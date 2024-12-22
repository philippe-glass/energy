package com.sapereapi.model;

import java.io.Serializable;

public class OptionItem implements Serializable {
	private static final long serialVersionUID = 1L;
	private String value;
	private String label;

	public OptionItem() {
		super();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public OptionItem(String value, String label) {
		super();
		this.value = value;
		this.label = label;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OptionItem) {
			OptionItem oItem = (OptionItem) obj;
			return value.equals(oItem.getValue());
		}
		return false;
	}

	@Override
	public String toString() {
		return "OptionItem [" + label + "]";
	}

}
