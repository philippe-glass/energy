package com.sapereapi.model.referential;

public enum EventMainCategory {
	START("START", 1, false, false),
	STOP("STOP", 2, true, true),
	EXPIRY("EXPIRY", 3, false, true),
	UPDATE("UPDATE", 4, true, false),
	SWITCH("SWITCH", 5, true, false),
	;

	private String label;
	private Integer id;
	private Boolean isCancel;	// True if cancels the previous event
	private Boolean isEnding;

	private EventMainCategory(String _label, Integer _id, Boolean _isCancel, Boolean _isEnding) {
		this.label = _label;
		this.id = _id;
		this.isCancel = _isCancel;
		this.isEnding = _isEnding;
	}

	public String getLabel() {
		return label;
	}

	public Integer getId() {
		return id;
	}

	public Boolean getIsCancel() {
		return isCancel;
	}

	public Boolean getIsEnding() {
		return isEnding;
	}
}
