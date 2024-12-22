package com.sapereapi.model.energy.pricing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class PricingTable implements Cloneable, Serializable {

	private static final long serialVersionUID = 11175L;
	// price in kWh by time slot
	private TreeMap<Date, ComposedRate> ratesTable = new TreeMap<Date, ComposedRate>();

	private long timeShiftMS = 0;

	public PricingTable(long _timeShiftMS) {
		super();
		this.ratesTable = new TreeMap<Date, ComposedRate>();
		this.timeShiftMS = _timeShiftMS;
	}

	public TreeMap<Date, ComposedRate> getRatesTable() {
		return ratesTable;
	}

	public void setRatesTable(TreeMap<Date, ComposedRate> ratesTable) {
		this.ratesTable = ratesTable;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}


	public PricingTable(Date dateBegin, double rateKWH, long _timeShiftMS) {
		super();
		this.ratesTable = new TreeMap<Date, ComposedRate>();
		putRate(dateBegin, rateKWH, null);
	}

	public int getSize() {
		return ratesTable.size();
	}

	public Date getBeginDate() {
		Date result = null;
		if (ratesTable.size() > 0) {
			result = ratesTable.keySet().iterator().next();
		}
		return result;
	}

	public boolean hasRate(Date aDate) {
		Date rateDate = this.getRateDate(aDate);
		return (rateDate != null);
	}

	public Date getEndDate() {
		Date result = null;
		Iterator<Date> keysIterator = ratesTable.keySet().iterator();
		while (keysIterator.hasNext()) {
			result = keysIterator.next();
		}
		return result;
	}

	public void putRate(Date dateBegin, ComposedRate newRate) {
		if(newRate != null) {
			// Remove the Milli-seconds part of the date
			Date dateBegin2 = UtilDates.removeMilliSeconds(dateBegin);
			ratesTable.put(dateBegin2, newRate.clone());
		}
	}

	public void putRate(Date dateBegin, double rateValue, DiscountItem discount /*d, double addedDiscount, double creditGrantedWH*/) {
		// Remove the Milli-seconds part of the date
		Date dateBegin2 = UtilDates.removeMilliSeconds(dateBegin);
		ratesTable.put(dateBegin2, new ComposedRate(rateValue, discount));
	}

	public Date getRateDate(Date givenDate) {
		// Date resultDate = null;
		Iterator<Date> datesIterator = ratesTable.keySet().iterator();
		Date fistRateDate = datesIterator.next();
		if (givenDate.before(fistRateDate)) {
			// The given date in not in table range : cannot provide a corresponding rate
			return null;
		}
		if (!datesIterator.hasNext()) {
			// Only one date : check if the rate date is before the given date.
			return fistRateDate;
		} else {
			Date dateMin = fistRateDate;
			Date dateMax = datesIterator.next();
			while(givenDate.after(dateMax) && datesIterator.hasNext()) {
				dateMin = dateMax;
				dateMax = datesIterator.next();
			}
			return dateMin;
		}
	}

	public List<Date> getRateDates(Date givenDateMin, Date givenDateMax) {
		List<Date> result = new ArrayList<Date>();
		if(givenDateMin.after(givenDateMax)) {
			return result;
		}
		Date firstRateDate = getRateDate(givenDateMin);
		result.add(firstRateDate);
		Iterator<Date> datesIterator = ratesTable.keySet().iterator();
		while(datesIterator.hasNext()) {
			Date nextDate = datesIterator.next();
			if(nextDate.after(firstRateDate) && nextDate.before(givenDateMax)) {
				result.add(nextDate);
			}
		}
		return result;
	}

	public PricingTable applyAwardsOnPricingTable(SingleOffer offer , boolean useAwardCredits, AbstractLogger logger) {
		EnergyRequest request = offer.getRequest();
		double remainAwardCreditWH = useAwardCredits ? request.getAwardsCredit() : 0.0;
		PricingTable result = new PricingTable(timeShiftMS);
		List<PricingSlot> listPricingSlots = computeListPricingSlots(offer);
		Date lastEndDate = null;
		boolean isTableAdjusted = false;
		logger.info("PricingTable.applyAwardsOnPricingTable : step1 : reqId=" + request.getEventId());
		for (PricingSlot pricingSlot : listPricingSlots) {
			lastEndDate = pricingSlot.getEndDate();
			// Rate with discount
			ComposedRate adjustedRate = pricingSlot.applyAwardsSlotRate(remainAwardCreditWH, logger);
			if (adjustedRate.isAdjusted()) {
				logger.info("PricingTable.applyAwardsOnPricingTable for request " + request + " : discount = " + adjustedRate.getDiscount());
				isTableAdjusted = true;
			}
			remainAwardCreditWH -= adjustedRate.getCreditGrantedWH();
			result.putRate(pricingSlot.getBeginDate(), adjustedRate);
		}
		if (lastEndDate != null && isTableAdjusted) {
			ComposedRate lastRate = getRate(lastEndDate);
			if (lastRate != null) {
				// add a final rate without discount at the end of the request
				result.putRate(lastEndDate, lastRate);
			}
		}
		return result;
	}

	public PricingTable applyContractUpdateOnTable(double lastUsageWH, Contract newContract , AbstractLogger logger) {
		List<PricingSlot> listPricingSlots = computeListPricingSlots(newContract);
		double remainAwardCreditWH = this.couputeCreditGrantedWH() - lastUsageWH;
		PricingTable result = new PricingTable(timeShiftMS);
		Date lastEndDate = null;
		boolean isTableAdjusted = false;
		EnergyRequest request = newContract.getRequest();
		logger.info("PricingTable.applyContractUpdateOnTable : step1 : reqId=" + request.getEventId());
		for (PricingSlot pricingSlot : listPricingSlots) {
			lastEndDate = pricingSlot.getEndDate();
			// Rate with discount
			//ComposedRate adjustedRate = pricingSlot.applyContractUpdateOnSlotRate(remainAwardCreditWH, logger);
			ComposedRate adjustedRate = pricingSlot.applyAwardsSlotRate(remainAwardCreditWH, logger);
			if (adjustedRate.isAdjusted()) {
				logger.info("PricingTable.applyContractUpdateOnTable for request " + request + " : discount = "
						+ adjustedRate.getDiscount());
				isTableAdjusted = true;
			}
			remainAwardCreditWH -= adjustedRate.getCreditGrantedWH();
			result.putRate(pricingSlot.getBeginDate(), adjustedRate);
		}
		if (lastEndDate != null && isTableAdjusted) {
			ComposedRate lastRate = getRate(lastEndDate);
			if (lastRate != null) {
				// add a final rate without discount at the end of the request
				lastRate.setDiscount(null);
				result.putRate(lastEndDate, lastRate);
			}
		}
		return result;
	}

	public double couputeCreditGrantedWH() {
		double result = 0.0;
		for(ComposedRate rate : this.ratesTable.values()) {
			result+= rate.getCreditGrantedWH();
		}
		return result;
	}

	public DiscountItem computeGlobalDiscount() {
		double creditGrantedWH = 0.0;
		double durationSec = 0.0;
		double discountValue = 0.0;
		double sumOfWeights = 0.0;
		double sumOfWeightedDiscounts = 0.0;
		int nbOfValues = 0;
		double lastDiscountValue = 0.0;
		for (ComposedRate rate : this.ratesTable.values()) {
			creditGrantedWH += rate.getCreditGrantedWH();
			if (creditGrantedWH != 0 && rate.getDiscount() != null) {
				durationSec += rate.getDiscountExpectedDurationSeconds();
				double nextWeight = creditGrantedWH / durationSec;
				sumOfWeights += nextWeight;
				lastDiscountValue = rate.getDiscountValue();
				sumOfWeightedDiscounts += nextWeight * lastDiscountValue;
				nbOfValues++;
			}
		}
		if (nbOfValues == 1) {
			discountValue = lastDiscountValue;
		} else if (nbOfValues > 1) {
			discountValue = sumOfWeightedDiscounts / sumOfWeights;
		}
		return new DiscountItem(discountValue, creditGrantedWH, durationSec);
	}

	public double computeCreditUsedWH(Date startDate, Date current, double power, String step, AbstractLogger logger) {
		List<PricingSlot> listPricingSlots = auxComputeListPricingSlots(startDate, current, power);
		double creditGrantedWH = couputeCreditGrantedWH();
		double usedCreditWH = 0.0;
		for (PricingSlot pricingSlot : listPricingSlots) {
			double slotUsedCredit = pricingSlot.computeCreditUsedWH(logger);
			usedCreditWH += slotUsedCredit;
		}
		double percentUsed = creditGrantedWH == 0 ? 0.0 : usedCreditWH / creditGrantedWH * 100;
		double timeSpentSec = UtilDates.computeDurationSeconds(startDate, current);
		logger.info("PricingTablet.computeCreditUsedWH " + step + " : usedCredit = " + SapereUtil.roundPower(usedCreditWH)
				+ ", % used = " + SapereUtil.round(percentUsed, 2)
				+ ", creditGrantedWH = " + SapereUtil.roundPower(creditGrantedWH)
				+ ", timeSpentSec = " + timeSpentSec
			);
		return usedCreditWH;
	}

	public List<PricingSlot> computeListPricingSlots(SingleOffer offer) {
		return auxComputeListPricingSlots(offer.getBeginDate(), offer.getEndDate(), offer.getPower());
	}

	public List<PricingSlot> computeListPricingSlots(Contract contract) {
		return auxComputeListPricingSlots(contract.getBeginDate(), contract.getEndDate(), contract.getPower());
	}

	public List<PricingSlot> auxComputeListPricingSlots(Date beginDate, Date endDate, double power) {
		List<PricingSlot> listPricingSlots = new ArrayList<PricingSlot>();
		Iterator<Date> datesIterator = ratesTable.keySet().iterator();
		Date fistRateDate = datesIterator.next();
		//double power = request.getPower();
		if (beginDate.before(fistRateDate)) {
			// The given date in not in table range : cannot provide a corresponding rate
			return listPricingSlots;
		}
		List<Date> listRateDates = getRateDates(beginDate, endDate);
		Map<Date, Date> mapNext = new HashMap<Date, Date>();
		Date lastRateDate = null;
		for (Date nextRateDate : listRateDates) {
			if (lastRateDate != null) {
				mapNext.put(lastRateDate, nextRateDate);
			}
		}
		for (Date rateDate : listRateDates) {
			Date slotDateBegin = rateDate;
			if (slotDateBegin.before(beginDate)) {
				slotDateBegin = beginDate;
			}
			Date slotDateEnd = endDate;
			if (mapNext.containsKey(rateDate)) {
				slotDateEnd = mapNext.get(rateDate);
			}
			ComposedRate slotRate = ratesTable.containsKey(rateDate) ? ratesTable.get(rateDate) : new ComposedRate(0.0);
			PricingSlot nexPricingSlot = new PricingSlot(slotDateBegin, slotDateEnd, power, rateDate, slotRate);
			listPricingSlots.add(nexPricingSlot);
		}
		return listPricingSlots;
	}

	public double computeValorisation(SingleOffer offer) {
		List<PricingSlot> listPricingSlots = computeListPricingSlots(offer);
		double totalPrice = 0.0;
		for (PricingSlot nexPricingSlot  : listPricingSlots) {
			double slotValorisation = nexPricingSlot.computeValorisation();
			totalPrice += slotValorisation;
		}
		return totalPrice;
	}

	public ComposedRate getRate(Date aDate) {
		Date rateDate = getRateDate(aDate);
		if(rateDate != null && ratesTable.containsKey(rateDate)) {
			return ratesTable.get(rateDate);
		}
		return null;
	}


	public List<Date> getRefreshedDates(Date current) {
		List<Date> result = new ArrayList<Date>();
		// Date lastDate = getEndDate();
		Date firstDate = getBeginDate();
		if (current.before(firstDate)) {
			// Do nothing
		} else {
			result.add(current);
			for (Date nextDate : ratesTable.keySet()) {
				if (nextDate.after(current)) {
					result.add(nextDate);
				}
			}
		}
		return result;
	}

	public PricingTable getRefreshedTable(Date current) {
		PricingTable result = new PricingTable(timeShiftMS);
		ComposedRate currentPrice = this.getRate(current);
		if (currentPrice == null) {
			// Do nothing
		} else {
			result.putRate(current, currentPrice);
			for (Date nextDate : ratesTable.keySet()) {
				if (nextDate.after(current)) {
					ComposedRate nextRate = getRate(nextDate);
					result.putRate(nextDate, nextRate);
				}
			}
		}
		return result;
	}

	public List<Date> getDates() {
		List<Date> result = new ArrayList<Date>();
		for (Date nextDate : ratesTable.keySet()) {
			result.add(nextDate);
		}
		return result;
	}

	public List<TimeSlot> getTimeSlots() {
		List<TimeSlot> result = new ArrayList<TimeSlot>();
		Date lastDate = null;
		for (Date nextDate : ratesTable.keySet()) {
			if (lastDate != null) {
				TimeSlot nextTimeSlot = new TimeSlot(lastDate, nextDate);
				result.add(nextTimeSlot);
			}
			lastDate = nextDate;
		}
		if (lastDate != null) {
			Date nextDate = UtilDates.shiftDateDays(lastDate, 1);
			TimeSlot nextTimeSlot = new TimeSlot(lastDate, nextDate);
			result.add(nextTimeSlot);
		}
		Collections.sort(result, new Comparator<TimeSlot>() {
			@Override
			public int compare(TimeSlot o1, TimeSlot o2) {
				return o1.getBeginDate().compareTo(o2.getBeginDate());
			}
		});
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		String sep = "";
		for (Date nextDatet : ratesTable.keySet()) {
			result.append(sep);
			result.append(UtilDates.formatTimeOrDate(nextDatet, timeShiftMS));
			result.append(" : ");
			ComposedRate rate = ratesTable.get(nextDatet);
			result.append(rate);
			sep = ", ";
		}
		return result.toString();
	}

	public Map<Long, Double> getMapPrices() {
		Map<Long, Double> result = new HashMap<>();
		for (Date nextDate : ratesTable.keySet()) {
			Double price = ratesTable.get(nextDate).getValue();
			result.put(nextDate.getTime(), price);
		}
		return result;
	}

	public TimeSlot getLowestPriceTimeSlot(Date current, Date minEndDate) {
		TimeSlot result = null;
		Double lowestPrice = null;
		for (TimeSlot nextSlot : getTimeSlots()) {
			if (nextSlot.hasExpired(current)) {
				// do nothing
			} else if (nextSlot.getEndDate().before(minEndDate)) {
				// do nothing
				SapereLogger.getInstance().info("getLowestPriceTimeSlot endDate of " + nextSlot
						+ " is before minEndDate " + UtilDates.formatTimeOrDate(minEndDate, timeShiftMS));
			} else {
				Date beginTime = nextSlot.getBeginDate();
				Double nextPrice = ratesTable.get(beginTime).getValue();
				if (lowestPrice == null || nextPrice.doubleValue() < lowestPrice.doubleValue()) {
					lowestPrice = nextPrice;
					result = nextSlot;
				}
			}
		}
		return result;
	}

	public PricingTable computeRise(double riseFactor) {
		PricingTable result = new PricingTable(timeShiftMS);
		for (Date nextDate : ratesTable.keySet()) {
			ComposedRate rate = ratesTable.get(nextDate).applyRiseFactor(riseFactor);
			result.putRate(nextDate, rate);
		}
		return result;
	}

	public boolean hasNotNullValue() {
		for (Date nextDate : ratesTable.keySet()) {
			Double rate = ratesTable.get(nextDate).getDefaultValue();
			if (rate != null && rate > 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasDiscount() {
		for(ComposedRate rate : ratesTable.values()) {
			if (rate.isAdjusted()) {
				return true;
			}
		}
		return false;
	}

	public PricingTable copyForLSA() {
		return copy(false);
	}

	public PricingTable clone() {
		return copy(true);
	}

	public PricingTable copy(boolean addIds) {
		PricingTable result = new PricingTable(timeShiftMS);
		for (Date nextDate : ratesTable.keySet()) {
			ComposedRate rate = ratesTable.get(nextDate);
			result.putRate(nextDate, rate.clone());
		}
		return result;
	}
}
