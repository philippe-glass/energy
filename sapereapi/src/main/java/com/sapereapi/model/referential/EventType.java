package com.sapereapi.model.referential;

public enum EventType {
	// Production events
	PRODUCTION_START(EventObjectType.PRODUCTION, EventMainCategory.START),
	PRODUCTION_STOP(EventObjectType.PRODUCTION, EventMainCategory.STOP),
	PRODUCTION_EXPIRY(EventObjectType.PRODUCTION, EventMainCategory.EXPIRY),
	PRODUCTION_UPDATE(EventObjectType.PRODUCTION, EventMainCategory.UPDATE),
	PRODUCTION_SWITCH(EventObjectType.PRODUCTION, EventMainCategory.SWITCH),
	// Request events
	REQUEST_START(EventObjectType.REQUEST, EventMainCategory.START),
	REQUEST_STOP(EventObjectType.REQUEST, EventMainCategory.STOP),
	REQUEST_EXPIRY(EventObjectType.REQUEST, EventMainCategory.EXPIRY),
	REQUEST_UPDATE(EventObjectType.REQUEST, EventMainCategory.UPDATE),
	REQUEST_SWITCH(EventObjectType.REQUEST, EventMainCategory.SWITCH),
	// Contract events
	CONTRACT_START(EventObjectType.CONTRACT, EventMainCategory.START),
	CONTRACT_STOP(EventObjectType.CONTRACT, EventMainCategory.STOP),
	CONTRACT_EXPIRY(EventObjectType.CONTRACT, EventMainCategory.EXPIRY),
	CONTRACT_UPDATE(EventObjectType.CONTRACT, EventMainCategory.UPDATE),
	;
	private Integer id;
	private EventObjectType objectType;
	private EventMainCategory mainCategory;
	private Boolean isCancel;	// True if cancels the previous event
	private Boolean isEnding;

	private EventType(EventObjectType _objectType, EventMainCategory _mainCategory) {
		this.mainCategory = _mainCategory;
		this.objectType = _objectType;
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

	public Integer getId() {
		return id;
	}

	public Boolean getIsCancel() {
		return isCancel;
	}

	public Boolean getIsEnding() {
		return isEnding;
	}

	public boolean isStart() {
		return EventMainCategory.START.equals(mainCategory);
	}

	public boolean isUpdate() {
		return EventMainCategory.UPDATE.equals(mainCategory);
	}

	public boolean isSwitch() {
		return EventMainCategory.SWITCH.equals(mainCategory);
	}

	public boolean isStop() {
		return EventMainCategory.STOP.equals(mainCategory);
	}

	public boolean isExpiry() {
		return EventMainCategory.EXPIRY.equals(mainCategory);
	}

	public static EventType retrieve(EventObjectType _objectType, EventMainCategory _mainCategory) {
		for (EventType nextEventType : values()) {
			if (nextEventType.getObjectType().equals(_objectType)
					&& nextEventType.getMainCategory().equals(_mainCategory)) {
				return nextEventType;
			}
		}
		return null;
	}
}
