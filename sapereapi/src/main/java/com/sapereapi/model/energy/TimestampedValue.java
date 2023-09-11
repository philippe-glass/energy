package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class TimestampedValue implements Serializable {
	private static final long serialVersionUID = 1L;
	private Date timestamp;
	private Double value;

	public TimestampedValue() {
		super();
	}

	public TimestampedValue(Date timestamp, Double value) {
		super();
		this.timestamp = timestamp;
		this.value = value;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ts:" + UtilDates.format_time.format(timestamp) + ", value:"+SapereUtil.round(value, 3);
	}
}
