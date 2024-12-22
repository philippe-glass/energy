package com.sapereapi.model.energy.pricing;

import java.io.Serializable;

import com.sapereapi.util.SapereUtil;

public class DiscountItem implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double value;
	private double creditGrantedWH;
	private double expectedDurationSeconds;

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getCreditGrantedWH() {
		return creditGrantedWH;
	}

	public void setCreditGrantedWH(double creditGrantedWH) {
		this.creditGrantedWH = creditGrantedWH;
	}

	public double getExpectedDurationSeconds() {
		return expectedDurationSeconds;
	}

	public void setExpectedDurationSeconds(double expectedDurationSeconds) {
		this.expectedDurationSeconds = expectedDurationSeconds;
	}

	public DiscountItem(double value, double creditGrantedWH, double expectedDurationSeconds) {
		super();
		this.value = value;
		this.creditGrantedWH = creditGrantedWH;
		this.expectedDurationSeconds = expectedDurationSeconds;
	}

	public DiscountItem clone() {
		DiscountItem result = new DiscountItem(value, creditGrantedWH, expectedDurationSeconds);
		return result;
	}

	public DiscountItem applyRiseFactor(double factor) {
		DiscountItem result = new DiscountItem(factor * value, factor * creditGrantedWH, expectedDurationSeconds);
		return result;
	}

	public void add(DiscountItem other) {
		this.value += other.getValue();
		this.creditGrantedWH += other.getCreditGrantedWH();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("( value:").append(SapereUtil.round(value, 3));
		result.append(", creditGrantedWH:").append(SapereUtil.round(creditGrantedWH, 3));
		result.append(", expectedDurationSeconds:").append(SapereUtil.round(expectedDurationSeconds, 3)).append(")");
		return result.toString();
	}

}
