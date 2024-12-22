package com.sapereapi.model.learning;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.OptionItem;

public enum PredictionScope {
	NODE(1), CLUSTER(2);

	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private PredictionScope(int id) {
		this.id = id;
	}

	public OptionItem toOptionItem() {
		OptionItem result = new OptionItem("" + id, "" + toString());
		return result;
	}

	public static List<OptionItem> getListScopeItems() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (PredictionScope nextScope : PredictionScope.values()) {
			result.add(nextScope.toOptionItem());
		}
		return result;
	}
}
