package com.sapereapi.model.referential;

public enum EventObjectType {
	 PRODUCTION("PRODUCTION", 1)
	,REQUEST("REQUEST", 2)
	,CONTRACT("CONTRACT", 3)
	;

	private String label;
	private Integer id;

	private EventObjectType(String _label, Integer _id) {
		this.label = _label;
		this.id = _id;
	}

	public String getLabel() {
		return label;
	}

	public Integer getId() {
		return id;
	}

	public static EventObjectType getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (EventObjectType pLevel : EventObjectType.values()) {
			if (pLevel.getLabel().equals(label2)) {
				return pLevel;
			}
		}
		return null;
	}
}
