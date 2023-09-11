package com.sapereapi.model.input;

public class TimeSlotFilter {
	//private Date dateBegin;
	//private Date dateEnd;
	private Long longDateBegin;
	private Long longDateEnd;
	private String featureType;

	public TimeSlotFilter() {
		super();
	}

	public Long getLongDateBegin() {
		return longDateBegin;
	}

	public void setLongDateBegin(Long longDateBegin) {
		this.longDateBegin = longDateBegin;
	}

	public Long getLongDateEnd() {
		return longDateEnd;
	}

	public void setLongDateEnd(Long longDateEnd) {
		this.longDateEnd = longDateEnd;
	}

	public String getFeatureType() {
		return featureType;
	}

	public void setFeatureType(String featureType) {
		this.featureType = featureType;
	}

}
