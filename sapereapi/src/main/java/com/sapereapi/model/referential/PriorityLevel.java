package com.sapereapi.model.referential;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.energy.OptionItem;

public enum PriorityLevel {
	  UNKNOWN("", 0)
	, LOW("Low", 1)
	, MEDIUM("Medium", 2)
	, HIGH("High", 3);

	private String label;
	private Integer level;

	PriorityLevel(String _label, Integer _level) {
		this.label = _label;
		this.level = _level;
	}

	public String getLabel() {
		return label;
	}

	public Integer getId() {
		return level;
	}

	public static PriorityLevel getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (PriorityLevel pLevel : PriorityLevel.values()) {
			if (pLevel.getLabel().equals(label2)) {
				return pLevel;
			}
		}
		return null;
	}

	public boolean isLowerThan(PriorityLevel other) {
		return this.compare(other) < 0;
	}

	public boolean isHigherThan(PriorityLevel other) {
		return this.compare(other) > 0;
	}

	public int compare(PriorityLevel other) {
		return this.level - other.level;
	}

	public static List<String> getLabels() {
		List<String> result = new ArrayList<>();
		for (PriorityLevel pLevel : PriorityLevel.values()) {
			result.add(pLevel.getLabel());
		}
		return result;
	}

	public static List<PriorityLevel> getList() {
		List<PriorityLevel> result = new ArrayList<PriorityLevel>();
		for (PriorityLevel pLevel : PriorityLevel.values()) {
			result.add(pLevel);
		}
		return result;
	}

	public static List<OptionItem> getOptionList() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (PriorityLevel pLevel : PriorityLevel.values()) {
			result.add(new OptionItem(pLevel.name(), pLevel.getLabel()));
		}
		return result;
	}
}
