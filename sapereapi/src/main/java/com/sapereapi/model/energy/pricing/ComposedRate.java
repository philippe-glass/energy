package com.sapereapi.model.energy.pricing;

import java.io.Serializable;

import com.sapereapi.util.SapereUtil;

public class ComposedRate implements Serializable, Cloneable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static double maxRelativeDiscount = 1.0; // 0.5;

	private double defaultValue;
	private DiscountItem discount;

	public double getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(double defaultValue) {
		this.defaultValue = defaultValue;
	}

	public DiscountItem getDiscount() {
		return discount;
	}

	public double getDiscountValue() {
		if (discount != null) {
			return discount.getValue();
		}
		return 0.0;
	}

	public double getDiscountExpectedDurationSeconds() {
		if (discount != null) {
			return discount.getExpectedDurationSeconds();
		}
		return 0.0;
	}

	public void setDiscount(DiscountItem discount) {
		this.discount = discount;
	}

	public double getCreditGrantedWH() {
		if (discount != null) {
			return discount.getCreditGrantedWH();
		}
		return 0.0;
	}

	public double getRelativeDiscount() {
		return defaultValue > 0 ? getDiscountValue() / defaultValue : 0.0;
	}

	public static double getMaxRelativeDiscount() {
		return maxRelativeDiscount;
	}

	public static void setMaxRelativeDiscount(double maxRelativeDiscount) {
		ComposedRate.maxRelativeDiscount = maxRelativeDiscount;
	}

	public ComposedRate(double defaultValue, DiscountItem discount) {
		super();
		this.defaultValue = defaultValue;
		this.discount = discount;
	}

	public ComposedRate(double defaultValue) {
		super();
		this.defaultValue = defaultValue;
		this.discount = null;
	}

	public ComposedRate clone() {
		ComposedRate result = new ComposedRate(defaultValue);
		if(discount != null) {
			result.setDiscount(discount.clone());
		}
		return result;
	}

	public double getValue() {
		if (discount == null) {
			return defaultValue;
		}
		return Math.max(0.0, defaultValue - getDiscountValue());
	}

	public void add(ComposedRate other) {
		this.defaultValue += other.getDefaultValue();
		if (other.getDiscount() != null) {
			if (discount == null) {
				discount = new DiscountItem(0., 0., 0.);
			}
			this.discount.add(other.getDiscount());
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(SapereUtil.round(getValue(), 3));
		if (discount != null) {
			if (Math.abs(discount.getValue()) >= 0.001) {
				result.append("( discount:").append(discount).append(")");
			}
		}
		return result.toString();
	}

	public ComposedRate applyRiseFactor(double factor) {
		DiscountItem transofmedDiscount = discount == null ? null : discount.applyRiseFactor(factor);
		return new ComposedRate(factor * defaultValue, transofmedDiscount);
	}

	public boolean isAdjusted() {
		return discount != null && Math.abs(discount.getValue()) > 0;
	}
}
