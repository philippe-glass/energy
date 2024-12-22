package com.sapereapi.model.referential;

public enum EnvironmentalImpact {
	 VERY_LOW("Very low", 1)
	,LOW("Low", 2)
	,MEDIUM("Medium", 3)
	,HIGH("High", 4)
	,VERY_HIGH("Very high", 5);

	private String label;
	private Integer level;

	EnvironmentalImpact(String _label, Integer _level) {
		this.label = _label;
		this.level = _level;
	}

	public String getLabel() {
		return label;
	}

	public Integer getLevel() {
		return level;
	}

	public static EnvironmentalImpact getByLevel(Integer level) {
		Integer level2 = (level == null) ? -1 : level;
		for (EnvironmentalImpact envImpact : EnvironmentalImpact.values()) {
			if (envImpact.getLevel().equals(level2)) {
				return envImpact;
			}
		}
		return null;
	}
}
