package com.sapereapi.model.referential;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.OptionItem;

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

	public Integer getLevel() {
		return level;
	}

	public static PriorityLevel getByLevel(Integer level) {
		Integer level2 = (level == null) ? -1 : level;
		for (PriorityLevel priorityLevel : PriorityLevel.values()) {
			if (priorityLevel.getLevel().equals(level2)) {
				return priorityLevel;
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

	public static List<OptionItem> getOptionList() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (PriorityLevel pLevel : PriorityLevel.values()) {
			result.add(new OptionItem(pLevel.name(), pLevel.getLabel()));
		}
		return result;
	}
}
