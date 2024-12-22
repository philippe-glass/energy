package com.sapereapi.model.energy.policy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.pricing.ComposedRate;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class LowestPricePolicy implements IConsumerPolicy{
	protected static SapereLogger logger = SapereLogger.getInstance();

	//PricingTable producerPricingTale = null;
	Map<String, Double> mapProducerPower = null;
	Map<String, PricingTable> mapProducerPricingTale = null;

	public LowestPricePolicy(/*PricingTable producerPricingTale*/) {
		super();
		//this.producerPricingTale = producerPricingTale;
		this.mapProducerPower = new HashMap<>();
		this.mapProducerPricingTale = new HashMap<>();
	}

	public void setProducerPricingTable(EnergySupply energySupply) {
		String producer = energySupply.getIssuer();
		Double power = energySupply.getPower();
		PricingTable pricingTable = energySupply.getPricingTable();
		if(pricingTable != null && power != null && power.doubleValue() >0) {
			this.mapProducerPower.put(producer, power);
			this.mapProducerPricingTale.put(producer, pricingTable);
		}
	}

	private PricingTable computeAvgPricingTable(long timeShiftMS) {
		return SapereUtil.auxComputeMapPricingTable(mapProducerPower, mapProducerPricingTale, timeShiftMS);

	}

	@Override
	public EnergyRequest updateRequest(EnergyRequest currentRequest) {
		//if(currentRequest.hasExpired() && mapProducerPricingTale.size() > 0) {
		if(true && mapProducerPricingTale.size() > 0) {
			String reqIssuer = currentRequest.getIssuer();
			String tag = this.getClass().getSimpleName() + ".updateRequest " + reqIssuer + " : ";
			Date current = currentRequest.getCurrentDate();
			PricingTable avgPricingTable = computeAvgPricingTable(currentRequest.getTimeShiftMS());
			logger.info(tag + " : avgPricingTable = " + avgPricingTable);
			Date minEndTime = new Date(current.getTime() +  (long) (0.5 * currentRequest.getTotalDurationMS()));
			TimeSlot targetTimeSlot = avgPricingTable.getLowestPriceTimeSlot(current, minEndTime);
			if(targetTimeSlot != null) {
				Date targetDate = targetTimeSlot.getBeginDate();
				ComposedRate correspondingRate = avgPricingTable.getRate(targetDate);
				logger.info(tag + " , current = " + UtilDates.formatTimeOrDate(current, currentRequest.getTimeShiftMS())
					+ ", targetTimeSlot = " + targetTimeSlot + " correspondingRate = "+ correspondingRate);
				Date newBeginDate = targetTimeSlot.getBeginDate();
				if (newBeginDate.before(current)) {
					newBeginDate = current;
				}
				long timeShiftMS = newBeginDate.getTime() - currentRequest.getBeginDate().getTime();
				if(timeShiftMS != 0) {
					if(timeShiftMS < 30) {
						logger.warning(tag + "timeShiftMS = " + timeShiftMS);
					}
					Long reqDurationMS = currentRequest.getTotalDurationMS();
					EnergyRequest result = currentRequest.clone();
					result.setBeginDate(targetTimeSlot.getBeginDate());
					Long end2 = targetTimeSlot.getBeginDate().getTime() + reqDurationMS;
					result.setEndDate(new Date(end2));
					try {
						result.checkDates();
					} catch (Exception e) {
						logger.error(e);
					}
					logger.info(tag + reqIssuer  + ": change of request date from "
							+ UtilDates.formatTimeOrDate(currentRequest.getBeginDate(), currentRequest.getTimeShiftMS())
							+ " to " + UtilDates.formatTimeOrDate(targetTimeSlot.getBeginDate(), currentRequest.getTimeShiftMS())
							+ " corresponding rate = " + correspondingRate+ " ");
					logger.info(tag + "result = " + result);
					return result;
				}
			}
		}
		return currentRequest;
	}

}
