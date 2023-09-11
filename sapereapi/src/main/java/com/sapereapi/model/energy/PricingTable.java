package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class PricingTable implements Cloneable, Serializable {

	private static final long serialVersionUID = 11175L;
	// price in kWh by time slot
	private Map<TimeSlot, Double> ratesTable = new HashMap<TimeSlot, Double>();
	private long timeShiftMS = 0;

	public PricingTable(long _timeShiftMS) {
		super();
		this.ratesTable = new HashMap<>();
		this.timeShiftMS = _timeShiftMS;
	}

	public Map<TimeSlot, Double> getRatesTable() {
		return ratesTable;
	}

	public void setRatesTable(Map<TimeSlot, Double> ratesTable) {
		this.ratesTable = ratesTable;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public PricingTable(Date dateBegin, Date dateEnd, double rateKWH, long _timeShiftMS) {
		super();
		this.ratesTable = new HashMap<>();
		addPrice(dateBegin, dateEnd, rateKWH);
	}

	public Date getBeginDate() {
		Date result = null;
		for(TimeSlot timeSlot : ratesTable.keySet()) {
			if(result == null || result.after(timeSlot.getBeginDate())) {
				result = timeSlot.getBeginDate();
			}
		}
		return result;
	}

	public Date getEndDate() {
		Date result = null;
		for(TimeSlot timeSlot : ratesTable.keySet()) {
			if(result == null || result.before(timeSlot.getEndDate())) {
				result = timeSlot.getEndDate();
			}
		}
		return result;
	}

	public void addPrice(Date dateBegin, Date dateEnd, double newRate) {
		TimeSlot newTimeSlot = new TimeSlot(dateBegin, dateEnd);
		if(this.ratesTable.isEmpty() || this.ratesTable.containsKey(newTimeSlot)) {
			this.ratesTable.put(newTimeSlot, newRate);
		} else {
			Date tableEndDate = getEndDate();
			Date tableBeginDate = getBeginDate();
			if (dateBegin.equals(tableEndDate)) {
				// new time slot after the last time position
				this.ratesTable.put(newTimeSlot, newRate);
			} else if (dateEnd.equals(tableBeginDate)) {
				// new time slot before the first time position
				this.ratesTable.put(newTimeSlot, newRate);
			} else {
				List<TimeSlot> toAdd = new ArrayList<>();
				toAdd.add(newTimeSlot);
				List<TimeSlot> newListTimeSlots = SapereUtil.mergeTimeSlots(getTimeSlots(), toAdd);
				//SapereLogger.getInstance().info("addPrice : newListTimeSlots = " + newListTimeSlots);
				Map<TimeSlot, Double> newRatesTable = new HashMap<>();
				for(TimeSlot nexTimeSlot : newListTimeSlots) {
					Date nextBeginDate = nexTimeSlot.getBeginDate();
					if(!nextBeginDate.before(dateBegin) && nextBeginDate.before(dateEnd)) {
						// use the given rate
						newRatesTable.put(nexTimeSlot,  newRate);
					} else {
						Double nextRate = getRate(nextBeginDate);
						if(nextRate == null) {
							SapereLogger.getInstance().error("addPrice : nextRate is null at "
									+ UtilDates.formatTimeOrDate(nextBeginDate, timeShiftMS));
						} else {
							newRatesTable.put(nexTimeSlot, nextRate);
						}
					}
				}
				this.ratesTable.clear();
				this.ratesTable = newRatesTable;
			}
		}
	}

	public Double getRate(Date aDate) {
		for (TimeSlot nextSlot : ratesTable.keySet()) {
			if (nextSlot.isInSlot(aDate)) {
				return ratesTable.get(nextSlot);
			}
		}
		return null;
	}

	public List<TimeSlot> getTimeSlots() {
		List<TimeSlot> result = new ArrayList<TimeSlot>();
		for (TimeSlot timeSlot : ratesTable.keySet()) {
			result.add(timeSlot);
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
		for (TimeSlot nextSlot : getTimeSlots()) {
			//result.append(nextSlot);
			result.append(sep);
			result.append(UtilDates.formatTimeOrDate(nextSlot.getBeginDate(), timeShiftMS));
			result.append(" : ");
			Object obj = ratesTable.get(nextSlot);
			result.append(obj);
			sep = ", ";
		}
		return result.toString();
	}

	public Map<Long, Double> getMapPrices() {
		Map<Long, Double> result = new HashMap<>();
		for (TimeSlot nextSlot : ratesTable.keySet()) {
			Date date = nextSlot.getBeginDate();
			Double price = ratesTable.get(nextSlot);
			result.put(date.getTime(), price);
		}
		return result;
	}

	public TimeSlot getLowestPriceTimeSlot(Date current, Date minEndDate) {
		TimeSlot result = null;
		Double lowestPrice = null;
		for (TimeSlot nextSlot : ratesTable.keySet()) {
			if(nextSlot.hasExpired(current)) {
				// do nothing
			} else if(nextSlot.getEndDate().before(minEndDate)) {
				// do nothing
				SapereLogger.getInstance().info("getLowestPriceTimeSlot endDate of " + nextSlot + " is before minEndDate " + UtilDates.formatTimeOrDate(minEndDate, timeShiftMS));
			} else {
				Double nextPrice = ratesTable.get(nextSlot);
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
		for (TimeSlot nextSlot : ratesTable.keySet()) {
			double rate = riseFactor*ratesTable.get(nextSlot);
			result.addPrice(nextSlot.getBeginDate(), nextSlot.getEndDate(), rate);
		}
		return result;
	}

	public boolean hasNotNullValue() {
		for (TimeSlot nextSlot : ratesTable.keySet()) {
			Double rate = ratesTable.get(nextSlot);
			if(rate != null && rate > 0) {
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
		for (TimeSlot nextSlot : ratesTable.keySet()) {
			Double rate = ratesTable.get(nextSlot);
			result.addPrice(nextSlot.getBeginDate(), nextSlot.getEndDate(), rate);
		}
		return result;
	}
}
