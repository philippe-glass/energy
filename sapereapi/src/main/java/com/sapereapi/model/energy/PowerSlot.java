package com.sapereapi.model.energy;

import java.io.Serializable;

import com.sapereapi.util.UtilDates;

public class PowerSlot implements Serializable {
	private static final long serialVersionUID = 14498762L;
	private Double current;
	private Double min;
	private Double max;

	public Double getCurrent() {
		return current;
	}

	public void setCurrent(Double current) {
		this.current = current;
	}

	public Double getMin() {
		return min;
	}

	public void setMin(Double min) {
		this.min = min;
	}

	public Double getMax() {
		return max;
	}

	public void setMax(Double max) {
		this.max = max;
	}

	public PowerSlot() {
		super();
		this.current = 0.0;
		this.min = 0.0;
		this.max = 0.0;
	}

	public PowerSlot(Double current, Double min, Double max) {
		super();
		this.current = current;
		this.min = min;
		this.max = max;
	}

	public void add(PowerSlot other) {
		if (other != null) {
			this.current += other.getCurrent();
			this.min += other.getMin();
			this.max += other.getMax();
		}
	}

	public void substract(PowerSlot other) {
		if (other != null) {
			this.current -= other.getCurrent();
			this.min -= other.getMin();
			this.max -= other.getMax();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PowerSlot) {
			PowerSlot other = (PowerSlot) obj;
			return current.floatValue() == other.getCurrent().floatValue()
					|| min.floatValue() == other.getMin().floatValue()
					|| max.floatValue() == other.getMax().floatValue();
		}
		return false;
	}

	public double getIntervalLength() {
		return Math.abs(max - min);
	}

	public double getMargin() {
		return Math.min(0, max - current);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.current);
		result.append(" (min:").append(UtilDates.df2.format(min));
		result.append(" max:").append(UtilDates.df2.format(max));
		result.append(")");
		return result.toString();
	}

	public PowerSlot clone() {
		return new PowerSlot(current, min, max);
	}
}
