package com.sapereapi.model.energy.forcasting.input;

import java.util.Date;

public class EndUserForcastingRequest {
	int samplingNb;
	Date timestamp;

	public int getSamplingNb() {
		return samplingNb;
	}

	public void setSamplingNb(int samplingNb) {
		this.samplingNb = samplingNb;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

}
