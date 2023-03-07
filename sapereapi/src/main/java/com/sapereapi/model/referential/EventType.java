package com.sapereapi.model.referential;

public enum EventType {
	// Production events
	PRODUCTION_START(EventObjectType.PRODUCTION, EventMainCategory.START),
	PRODUCTION_STOP(EventObjectType.PRODUCTION, EventMainCategory.STOP),
	PRODUCTION_EXPIRY(EventObjectType.PRODUCTION, EventMainCategory.EXPIRY),
	PRODUCTION_UPDATE(EventObjectType.PRODUCTION, EventMainCategory.UPDATE),
	// Request events
	REQUEST_START(EventObjectType.REQUEST, EventMainCategory.START),
	REQUEST_STOP(EventObjectType.REQUEST, EventMainCategory.STOP),
	REQUEST_EXPIRY(EventObjectType.REQUEST, EventMainCategory.EXPIRY),
	REQUEST_UPDATE(EventObjectType.REQUEST, EventMainCategory.UPDATE),
	// Contract events
	CONTRACT_START(EventObjectType.CONTRACT, EventMainCategory.START),
	CONTRACT_STOP(EventObjectType.CONTRACT, EventMainCategory.STOP),
	CONTRACT_EXPIRY(EventObjectType.CONTRACT, EventMainCategory.EXPIRY),
	CONTRACT_UPDATE(EventObjectType.CONTRACT, EventMainCategory.UPDATE),
	;
	private String label;
	private Integer id;
	private EventObjectType objectType;
	private EventMainCategory mainCategory;
	private Boolean isCancel;	// True if cancels the previous event
	private Boolean isEnding;

	private EventType(EventObjectType _objectType, EventMainCategory _mainCategory) {
		this.mainCategory = _mainCategory;
		this.objectType = _objectType;
		this.label =  objectType.getLabel() + "_" + mainCategory.getLabel();
				//+ (EventMainCategory.START.equals(mainCategory)? "" : ("_" + mainCategory.getLabel()));
		this.id = 100*objectType.getId() + mainCategory.getId();
		this.isCancel = mainCategory.getIsCancel();
		this.isEnding = mainCategory.getIsEnding();
	}

	public EventObjectType getObjectType() {
		return objectType;
	}

	public EventMainCategory getMainCategory() {
		return mainCategory;
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

	public static EventType getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (EventType pLevel : EventType.values()) {
			if (pLevel.getLabel().equals(label2)) {
				return pLevel;
			}
		}
		return null;
	}

	public boolean isStart() {
		return EventMainCategory.START.equals(mainCategory);
	}

	public boolean isUpdate() {
		return EventMainCategory.UPDATE.equals(mainCategory);
	}

	public boolean isStop() {
		return EventMainCategory.STOP.equals(mainCategory);
	}

	public boolean isExpiry() {
		return EventMainCategory.EXPIRY.equals(mainCategory);
	}
}
