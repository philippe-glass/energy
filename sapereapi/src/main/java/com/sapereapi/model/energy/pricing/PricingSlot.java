package com.sapereapi.model.energy.pricing;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class PricingSlot extends TimeSlot implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	private Date rateDate;
	private ComposedRate rate;
	private double power;

	public Date getRateDate() {
		return rateDate;
	}

	public void setRateDate(Date rateDate) {
		this.rateDate = rateDate;
	}

	public ComposedRate getRate() {
		return rate;
	}

	public void setRate(ComposedRate rate) {
		this.rate = rate;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public PricingSlot(Date beginDate, Date endDate, double power, Date rateDate, ComposedRate rate) {
		super();
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.power = power;
		this.rateDate = rateDate;
		this.rate = rate;
	}

	public PricingSlot clone() {
		return new PricingSlot(beginDate, endDate, power, rateDate, rate.clone());
	}

	public double computeEnergyWH() {
		double slotDurationMS = (endDate.getTime() - beginDate.getTime());
		double slotDurationHours = slotDurationMS / UtilDates.MS_IN_HOUR;
		double slotEnergyWH = slotDurationHours * power;
		return slotEnergyWH;
	}

	public double getRelativeDiscount() {
		return rate.getRelativeDiscount();
	}

	public double computeCreditUsedWH(AbstractLogger logger) {
		double relativeDiscount = rate.getRelativeDiscount();
		double maxRelativeDiscount = ComposedRate.getMaxRelativeDiscount();
		if(Math.abs(relativeDiscount) > 0) {
			double slotConsumptionWH = computeEnergyWH();
			double slotUsedCredit = (relativeDiscount / maxRelativeDiscount) * slotConsumptionWH;
			// Credit usage cannot be greater than credit granted in absolute value
			if(Math.abs(slotUsedCredit) > Math.abs(rate.getCreditGrantedWH())) {
				slotUsedCredit = rate.getCreditGrantedWH();
			}
			double percentUsed = slotUsedCredit / rate.getCreditGrantedWH() * 100;
			logger.info("PricingSlot.computeCreditWHUsage : slotUsedCredit = " + SapereUtil.roundPower(slotUsedCredit)
			+ ", % used = " + SapereUtil.round(percentUsed, 2));
			return slotUsedCredit;
		}
		return 0.0;
	}

	public ComposedRate applyAwardsSlotRate(double remainAwardCreditWH, AbstractLogger logger) {
		ComposedRate rateWithDiscount = new ComposedRate(rate.getDefaultValue());
		if (Math.abs(remainAwardCreditWH) > 0) {
			double slotWH = computeEnergyWH();
			if(slotWH > 0) {
				double discountOrPanalityInWH = 0;
				if (remainAwardCreditWH >= 0) {
					// Reward: the reward used cannot exceed the total energy of the slot.
					discountOrPanalityInWH = Math.min(remainAwardCreditWH, slotWH);
				} else {
					// Penalty: the penalty absolue value can exceed the total energy of the slot (up to a limit
					// of 10*energy of the slot).
					discountOrPanalityInWH = -1 * Math.min(Math.abs(remainAwardCreditWH), 10*slotWH);
				}
				double discountOrPanalty = (discountOrPanalityInWH / slotWH) * rate.getDefaultValue()
						* ComposedRate.getMaxRelativeDiscount();
				DiscountItem discountItem = new DiscountItem(discountOrPanalty, discountOrPanalityInWH,  this.getDurationSeconds());
				logger.info("PricingSlot.applyAwardsSlotRate : beginDate = " + UtilDates.format_time.format(this.beginDate) + ", discountItem = " + discountItem);
				rateWithDiscount.setDiscount(discountItem);
			}
			/*
			double isAwardFactor = (remainAwardCreditWH > 0) ? 1 : -1;
			double absDiscountOrPanalityInWH = Math.min(Math.abs(remainAwardCreditWH), slotWH);
			absDiscountOrPanalityInWH = (remainAwardCreditWH > 0) ? Math.min(remainAwardCreditWH, slotWH) : Math.min(Math.abs(10*remainAwardCreditWH), slotWH);
			double absDiscountOrPanalty = (absDiscountOrPanalityInWH / slotWH) * rate.getDefaultValue() * maxRelativeDiscount;
			rateWithDiscount.setDiscount(isAwardFactor * absDiscountOrPanalty);
			rateWithDiscount.setCreditGrantedWH(isAwardFactor * absDiscountOrPanalityInWH);
			*/
		}
		return rateWithDiscount;
	}
/*
	public ComposedRate applyContractUpdateOnSlotRate(double remainAwardCreditWH, AbstractLogger logger) {
		ComposedRate rateWithDiscount = new ComposedRate(rate.getDefaultValue());
		if(rate.getDiscount() != null && Math.abs(remainAwardCreditWH) > 0) {
			double slotWH = computeEnergyWH();
			double discountOrPanalityInWH = slotWH * rate.getRelativeDiscount();
			if(Math.abs(discountOrPanalityInWH) > Math.abs(remainAwardCreditWH)) {
				discountOrPanalityInWH = remainAwardCreditWH;
			}
			DiscountItem discountItem = new DiscountItem(rate.getRelativeDiscount(), discountOrPanalityInWH,  this.getDurationSeconds());
			logger.info("PricingSlot.applyContractUpdateOnSlotRate : beginDate = " + UtilDates.format_time.format(this.beginDate) + ", discountItem = " + discountItem);
			rateWithDiscount.setDiscount(discountItem);
		}
		return rateWithDiscount;
	}
*/
	public double computeValorisation() {
		double slotEnergyWH = computeEnergyWH();
		return rate.getValue() * slotEnergyWH;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(super.toString());
		result.append(", W=").append(SapereUtil.roundPower(power));
		result.append(", rate = ").append(rate)
			.append(" since ").append(UtilDates.format_time.format(rateDate));
		return result.toString();
	}

}
