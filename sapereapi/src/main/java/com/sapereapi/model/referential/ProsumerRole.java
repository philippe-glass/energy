package com.sapereapi.model.referential;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.OptionItem;

public enum ProsumerRole {
	 CONSUMER("Consumer")
	,PRODUCER("Producer");

	private String label;

	ProsumerRole(String _label) {
		this.label = _label;
	}

	public String getLabel() {
		return label;
	}

	public OptionItem getOptionItem() {
		return new OptionItem(this.name(), label);
	}

	public static List<OptionItem> getOptionList() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (ProsumerRole item : ProsumerRole.values()) {
			result.add(item.getOptionItem());
		}
		return result;
	}
}
