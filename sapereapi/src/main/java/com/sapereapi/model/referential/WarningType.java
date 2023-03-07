package com.sapereapi.model.referential;

public enum WarningType {
		OVER_PRODUCTION("Over production", 30)
	, OVER_PRODUCTION_FORCAST("Over production forcast", 30)
	, OVER_CONSUMPTION("Over consumption", 30)
	, OVER_CONSUMPTION_FORCAST("Over consumption", 30)
	, NOT_IN_SPACE("Not in space", 5)
	, USER_INTERRUPTION("User interruption", 2)
	, GENERAL_INTERRUPTION("General interruption", 30)
	, CHANGE_REQUEST("Change request", 3)
	, CONTRACT_MERGE("Contract merge", 5)
	;

	private String label;
	private int validitySeconds;

	WarningType(String _label, int _validitySeconds) {
		this.label = _label;
		this.validitySeconds = _validitySeconds;
	}

	public String getLabel() {
		return label;
	}

	public int getValiditySeconds() {
		return validitySeconds;
	}

	public static WarningType getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (WarningType warningType : WarningType.values()) {
			if (warningType.getLabel().equals(label2)) {
				return warningType;
			}
		}
		return null;
	}
}
