package com.sapereapi.model.learning;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.OptionItem;

public enum LearningModelType {
	MARKOV_CHAINS(1, true), LSTM(2, false);

	private int id;
	private boolean learnsLocally;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isLearnsLocally() {
		return learnsLocally;
	}

	public void setLearnsLocally(boolean learnsLocally) {
		this.learnsLocally = learnsLocally;
	}

	private LearningModelType(int id, boolean learnsLocally) {
		this.id = id;
		this.learnsLocally = learnsLocally;
	}

	public OptionItem toOptionItem() {
		OptionItem result = new OptionItem("" + id, "" + toString());
		return result;
	}

	public static List<OptionItem> getListLearningModels() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (LearningModelType nextScope : LearningModelType.values()) {
			result.add(nextScope.toOptionItem());
		}
		return result;
	}

}
