package com.sapereapi.model.energy.award;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.ProsumerItem;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class AwardsComputingData implements Serializable {

	private static final long serialVersionUID = 1L;
	private long timeShiftMS = 0;
	private Map<String, Map<Date, Double>> historyProvidedPower = new HashMap<String, Map<Date,Double>>();
	private Map<String, Map<Date, Double>> historyProposedPower = new HashMap<String, Map<Date,Double>>();
	private Map<String, Map<Date, Double>> historyIdealSupplyPower = new HashMap<String, Map<Date,Double>>();
	private Map<String, Map<Date, Double>> historyIdealConsumtionPower = new HashMap<String, Map<Date,Double>>();
	private Map<String, Map<Date, Double>> historyBonusProvidedPower = new HashMap<String, Map<Date,Double>>();
	private Map<String, Map<Date, Double>> historyMalusNotGivenPower = new HashMap<String, Map<Date,Double>>();

	private Map<String, Double> mapProvidedWH = new HashMap<String, Double>();
	private Map<String, Double> mapIdealSupplyWH  = new HashMap<String, Double>();
	private Map<String, Double> mapIdealConsumptionWH  = new HashMap<String, Double>();
	private Map<String, Double> mapBonusProvidedWH = new HashMap<String, Double>();
	private Map<String, Double> mapMalusNotGivenWH = new HashMap<String, Double>();
	private List<Date> historyDates = new ArrayList<Date>();
	private Date lastHistoryDate = null;
	private Date nextEmptyDeadline = null;
	private int cycleSeconds = 60;
	private int awardsPropertyExpirationDelaySec = 3;
	private Map<Date, AwardsTable> mapAwardsTables = new TreeMap<Date, AwardsTable>();
	private AwardsTable cumulativeArewardTable = new AwardsTable();


	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public Map<String, Map<Date, Double>> getHistoryProvidedPower() {
		return historyProvidedPower;
	}

	public void setHistoryProvidedPower(Map<String, Map<Date, Double>> historyProvidedPower) {
		this.historyProvidedPower = historyProvidedPower;
	}

	public Map<String, Map<Date, Double>> getHistoryProposedPower() {
		return historyProposedPower;
	}

	public void setHistoryProposedPower(Map<String, Map<Date, Double>> historyProposedPower) {
		this.historyProposedPower = historyProposedPower;
	}

	public Map<String, Map<Date, Double>> getHistoryBonusProvidedPower() {
		return historyBonusProvidedPower;
	}

	public void setHistoryBonusProvidedPower(Map<String, Map<Date, Double>> historyBonusProvidedPower) {
		this.historyBonusProvidedPower = historyBonusProvidedPower;
	}

	public Map<String, Map<Date, Double>> getHistoryIdealSupplyPower() {
		return historyIdealSupplyPower;
	}

	public void setHistoryIdealSupplyPower(Map<String, Map<Date, Double>> historyIdealSupplyPower) {
		this.historyIdealSupplyPower = historyIdealSupplyPower;
	}

	public Map<String, Map<Date, Double>> getHistoryMalusNotGivenPower() {
		return historyMalusNotGivenPower;
	}

	public void setHistoryMalusNotGivenPower(Map<String, Map<Date, Double>> historyNotMalusGivenPower) {
		this.historyMalusNotGivenPower = historyNotMalusGivenPower;
	}

	public Map<String, Double> getMapProvidedWH() {
		return mapProvidedWH;
	}

	public void setMapProvidedWH(Map<String, Double> mapProvidedWH) {
		this.mapProvidedWH = mapProvidedWH;
	}

	public Map<String, Double> getMapBonusProvidedWH() {
		return mapBonusProvidedWH;
	}

	public void setMapBonusProvidedWH(Map<String, Double> mapBonusProvidedWH) {
		this.mapBonusProvidedWH = mapBonusProvidedWH;
	}

	public Map<String, Double> getMapMalusNotGivenWH() {
		return mapMalusNotGivenWH;
	}

	public void setMapMalusNotGivenWH(Map<String, Double> mapMalusNotGivenWH) {
		this.mapMalusNotGivenWH = mapMalusNotGivenWH;
	}

	public Map<String, Double> getMapIdealSupplyWH() {
		return mapIdealSupplyWH;
	}

	public void setMapIdealSupplyWH(Map<String, Double> mapIdealSupplyWH) {
		this.mapIdealSupplyWH = mapIdealSupplyWH;
	}

	public Map<String, Double> getMapIdealConsumptionWH() {
		return mapIdealConsumptionWH;
	}

	public void setMapIdealConsumptionWH(Map<String, Double> mapIdealConsumptionWH) {
		this.mapIdealConsumptionWH = mapIdealConsumptionWH;
	}

	public Map<String, Map<Date, Double>> getHistoryIdealConsumtionPower() {
		return historyIdealConsumtionPower;
	}

	public void setHistoryIdealConsumtionPower(Map<String, Map<Date, Double>> historyIdealConsumtionPower) {
		this.historyIdealConsumtionPower = historyIdealConsumtionPower;
	}

	public List<Date> getHistoryDates() {
		return historyDates;
	}

	public void setHistoryDates(List<Date> historyDates) {
		this.historyDates = historyDates;
	}

	public Date getLastHistoryDate() {
		return lastHistoryDate;
	}

	public void setLastHistoryDate(Date lastHistoryDate) {
		this.lastHistoryDate = lastHistoryDate;
	}

	public int getAwardsPropertyExpirationDelaySec() {
		return awardsPropertyExpirationDelaySec;
	}

	public void setAwardsPropertyExpirationDelaySec(int awardsPropertyExpirationDelaySec) {
		this.awardsPropertyExpirationDelaySec = awardsPropertyExpirationDelaySec;
	}

	public AwardsComputingData(long timeShiftMS) {
		super();
		this.timeShiftMS = timeShiftMS;
		Date current = getCurrentDate();
		this.nextEmptyDeadline = UtilDates.shiftDateSec(current, cycleSeconds);
	}

	public boolean hasEmptyDeadlineExpired() {
		Date current = getCurrentDate();
		return nextEmptyDeadline.before(current);
	}

	public AwardsTable generateAwardsTable(boolean cleanData,  AbstractLogger logger) {
		Date currentDate = getCurrentDate();
		AwardsTable newAwardTable = new AwardsTable(timeShiftMS, awardsPropertyExpirationDelaySec);
		for (String prosumer : mapBonusProvidedWH.keySet()) {
			double scoreSupplies = mapBonusProvidedWH.get(prosumer) - mapMalusNotGivenWH.get(prosumer);
			double scoreConsumptions = 0;
			double scoreEquity = 0;
			AwardItem award = new AwardItem(prosumer, currentDate, scoreSupplies, scoreConsumptions, scoreEquity);
			newAwardTable.addAward(prosumer, award);
		}
		if (cleanData) {
			updateAwardsTables(currentDate, newAwardTable, logger);
			clearData();
		}
		return newAwardTable;
	}

	private void updateAwardsTables(Date currentDate, AwardsTable newAwardTable,  AbstractLogger logger) {
		mapAwardsTables.put(currentDate, newAwardTable);
		// clean obsolete award tables
		List<Date> toRemove = new ArrayList<Date>();
		Date minDate = UtilDates.shiftDateMinutes(currentDate, -60);
		for (Date aDate : mapAwardsTables.keySet()) {
			if (aDate.before(minDate)) {
				toRemove.add(minDate);
			}
		}
		for (Date nextDate : toRemove) {
			mapAwardsTables.remove(nextDate);
		}
		cumulativeArewardTable = new AwardsTable();
		for (AwardsTable nextTable : mapAwardsTables.values()) {
			cumulativeArewardTable.addTable(nextTable);
		}
		logger.info("updateAwardsTables : cumulativeArewardTable = " + cumulativeArewardTable);
	}

	private void clearData() {
		this.mapProvidedWH.clear();
		this.mapBonusProvidedWH.clear();
		this.mapMalusNotGivenWH.clear();
		this.mapIdealSupplyWH.clear();
		Date current = getCurrentDate();
		this.nextEmptyDeadline = UtilDates.shiftDateSec(current, cycleSeconds);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}

	public boolean updateRecentHistory(Date currentTime, List<ExtendedEnergyEvent> currentEvents,
			Map<String, Double> proposedPowerByProducer, AbstractLogger logger) {
		Map<String, Double> mapProducers = new HashMap<String, Double>();
		Map<String, Double> mapProviders = new HashMap<String, Double>();
		double totalRequested = 0.0;
		double totalRequestedPenality = 0.0;
		double totalRequestedWithNoPenality = 0.0;
		double totalProduced = 0.0;
		double totalProvided = 0.0;
		historyDates.add(currentTime);
		for (ExtendedEnergyEvent nextEvent : currentEvents) {
			EventType evtType = nextEvent.getType();
			String prosumer = nextEvent.getIssuer();
			if (EventObjectType.REQUEST.equals(evtType.getObjectType()) && !evtType.getIsEnding()) {
				totalRequested += nextEvent.getPower();
				// check if the prosumer has an negative award
				double prosumerAward = cumulativeArewardTable.getAwardTotal(prosumer);
				if(prosumerAward < -0.001) {
					logger.info("AwardComputingData.updateRecentHistory : the requester " + prosumer + " has a penality " + SapereUtil.roundPower(prosumerAward));
					totalRequestedPenality += nextEvent.getPower();
				} else {
					totalRequestedWithNoPenality += nextEvent.getPower();
				}
			}
			if (EventObjectType.PRODUCTION.equals(evtType.getObjectType()) && !evtType.getIsEnding()) {
				mapProducers.put(prosumer, nextEvent.getPower());
				totalProduced += nextEvent.getPower();
			}
			if (EventObjectType.CONTRACT.equals(evtType.getObjectType()) && !evtType.getIsEnding()) {
				totalProvided += nextEvent.getPower();
				for (String providerName : nextEvent.getMapProviders().keySet()) {
					ProsumerItem nextProvider = nextEvent.getMapProviders().get(providerName);
					if (nextProvider.getPowerSlot() != null) {
						if (!mapProviders.containsKey(providerName)) {
							mapProviders.put(providerName, 0.0);
						}
						double previousValue = mapProviders.get(providerName);
						mapProviders.put(providerName, previousValue + nextProvider.getPowerSlot().getCurrent());
					}
				}
			}
			if (!mapProvidedWH.containsKey(prosumer)) {
				mapProvidedWH.put(prosumer, 0.);
			}
			if (!mapIdealSupplyWH.containsKey(prosumer)) {
				mapIdealSupplyWH.put(prosumer, 0.);
			}
			if (!mapIdealConsumptionWH.containsKey(prosumer)) {
				mapIdealConsumptionWH.put(prosumer, 0.);
			}
			if (!mapBonusProvidedWH.containsKey(prosumer)) {
				mapBonusProvidedWH.put(prosumer, 0.);
			}
			if (!mapMalusNotGivenWH.containsKey(prosumer)) {
				mapMalusNotGivenWH.put(prosumer, 0.);
			}
			if (!historyProvidedPower.containsKey(prosumer)) {
				historyProvidedPower.put(prosumer, new TreeMap<Date, Double>());
			}
			if (!historyProposedPower.containsKey(prosumer)) {
				historyProposedPower.put(prosumer, new TreeMap<Date, Double>());
			}
			if (!historyIdealSupplyPower.containsKey(prosumer)) {
				historyIdealSupplyPower.put(prosumer, new TreeMap<Date, Double>());
			}
			if (!historyMalusNotGivenPower.containsKey(prosumer)) {
				historyMalusNotGivenPower.put(prosumer, new TreeMap<Date, Double>());
			}
			if (!historyBonusProvidedPower.containsKey(prosumer)) {
				historyBonusProvidedPower.put(prosumer, new TreeMap<Date, Double>());
			}
		}
		if (totalRequestedPenality > 0) {
			logger.info("For debug: totalRequestedPenality = " + totalRequestedPenality);
		}
		if (totalRequested > 0.0 && lastHistoryDate != null) {
			for (String providerName : mapProducers.keySet()) {
				Map<Date, Double> prosumerHistoryProvidedPower = historyProvidedPower.get(providerName);
				Map<Date, Double> prosumerHistoryProposedPower = historyProposedPower.get(providerName);
				Map<Date, Double> prosumerHistoryIdealSupplyPower = historyIdealSupplyPower.get(providerName);
				Map<Date, Double> prosumerHistoryNoGivenPower = historyMalusNotGivenPower.get(providerName);
				Map<Date, Double> prosumerHistoryBonusProvidedPower = historyBonusProvidedPower.get(providerName);
				double producedPower = mapProducers.get(providerName);
				double proposedPower = proposedPowerByProducer.containsKey(providerName) ? proposedPowerByProducer.get(providerName) : 0;
				if(proposedPower > 0) {
					prosumerHistoryProposedPower.put(currentTime, proposedPower);
				}
				double producedRatio = (totalProduced ==0) ? 1.0 : producedPower / totalProduced;
				double totalRequested2 = totalRequestedWithNoPenality + totalRequestedPenality * 0.5;
				double idealSupplyPower = Math.min(totalRequested2, totalProduced) * producedRatio;
				prosumerHistoryIdealSupplyPower.put(currentTime, idealSupplyPower);
				double providedPower = mapProviders.containsKey(providerName) ? mapProviders.get(providerName) : 0;
				if (providedPower > 0) {
					prosumerHistoryProvidedPower.put(currentTime, providedPower);
				}
				double bonusProvidedPower = Math.max(0, providedPower - 0.8*idealSupplyPower);
				if (bonusProvidedPower > 0) {
					prosumerHistoryBonusProvidedPower.put(currentTime, bonusProvidedPower);
				}
				double notGivenPower = 0;
				// Compute not given power to generate penalities : correspond to saved energy of prosumers which don't provide pending requests 
				double totalMissing = Math.max(0, totalRequestedWithNoPenality - totalProvided);
				if(totalMissing > 0) {
					double availablePower = Math.max(0., producedPower - providedPower - proposedPower);
					if (availablePower > 0) {
						notGivenPower = Math.max(0, totalMissing - proposedPower);
						if (notGivenPower > availablePower) {
							notGivenPower = availablePower;
						}
						if (notGivenPower > 0) {
							prosumerHistoryNoGivenPower.put(currentTime, notGivenPower);
						}
						if(notGivenPower > 0 && proposedPower > 0) {
							logger.info("AwardsComputingData.updateData(1) " + providerName + " at " + UtilDates.format_time.format(currentTime)
									+" for debug : proposedPower = " + proposedPower
									+ ",  notGivenPower = " + notGivenPower + ", providedPower = " + providedPower + ", totalMissing = " + totalMissing
									+ ", availablePower = " + availablePower);
						}
					}
				}
				if (proposedPower > 0 && prosumerHistoryNoGivenPower.size() > 0) {
					// Remove the penalties that preceded the offer by a few seconds
					Date dateMin = UtilDates.shiftDateSec(currentTime, -12);
					List<Date> toRemove = new ArrayList<Date>();
					for (Date previousTime : prosumerHistoryNoGivenPower.keySet()) {
						if (!previousTime.before(dateMin) && !previousTime.after(currentTime)) {
							Double previousNotGivenPower = prosumerHistoryNoGivenPower.get(previousTime);
							// Double noGivenPowerToSet = previousNotGivenPower;
							Double previousProposedPower = 0.0; // By default, no offer exists at the previous time
							if (prosumerHistoryProposedPower.containsKey(previousTime)) {
								// Offers issued by the same prosumer already exist at a near time
								previousProposedPower = prosumerHistoryProposedPower.get(previousTime);
								if(proposedPower > previousProposedPower) {
									logger.info("AwardsComputingData.updateData(2) :  " + providerName + " : to decrease  previousNotGivenPower " + previousNotGivenPower + " by " + (proposedPower - previousProposedPower));
								}
							}
							Double noGivenPowerToSet = SapereUtil.roundPower(Math.max(0, previousNotGivenPower  + previousProposedPower - proposedPower));
							if(noGivenPowerToSet == 0) {
								toRemove.add(previousTime);
							} else if (noGivenPowerToSet > 0 && noGivenPowerToSet < previousNotGivenPower) {
								prosumerHistoryNoGivenPower.put(previousTime, noGivenPowerToSet);
								String sPreviousTime = UtilDates.format_time.format(previousTime);
								logger.info("AwardsComputingData.updateData(3) : " + providerName + " : previousNotGivenPower = " + previousNotGivenPower + ", noGivenPowerToSet = "	+ noGivenPowerToSet + " at " + sPreviousTime);
							} else {
								logger.warning("AwardsComputingData.updateData(4) : " + providerName + " : no penalty reduction : previousProposedPower = " + previousProposedPower + ", proposedPower = " + proposedPower
										+ ", previousNotGiven = " + previousNotGivenPower);
							}
						}
					}
					for(Date previousTime : toRemove) {
						prosumerHistoryNoGivenPower.remove(previousTime);
					}
				}
			}
		}
		//logger.info("AwardsComputingData.updateData : mapProvidedWH = " + SapereUtil.auxLogMapPowers(mapProvidedWH));
		//logger.info("AwardsComputingData.updateData : mapMalusNotProvidedWH = " + SapereUtil.auxLogMapPowers(mapMalusNotGivenWH));
		lastHistoryDate = currentTime;
		boolean toAggregate = (historyDates.size() > 20);
		boolean result = false;
		if (toAggregate) {
			aggregateHistoryData(currentTime);
			result = true;
		}
		return result;
	}

	public void aggregateHistoryData(Date currentTime) {
		Date dateMax = UtilDates.shiftDateSec(currentTime, -10);
		boolean cleanHistoryData = true;
		// Aggregate history of provided power
		if (cleanHistoryData) {
			mapProvidedWH = aggregateHistory(dateMax, historyProvidedPower, mapProvidedWH, cleanHistoryData);
		} else {
			mapProvidedWH = auxAggregateHistory(dateMax, historyProvidedPower);
		}
		// Aggregate history of "Ideal" supply
		if (cleanHistoryData) {
			mapIdealSupplyWH = aggregateHistory(dateMax, historyIdealSupplyPower, mapIdealSupplyWH,
					cleanHistoryData);
		} else {
			mapIdealSupplyWH = auxAggregateHistory(dateMax, historyIdealSupplyPower);
		}
		// Aggregate history of bonus of provided power
		if (cleanHistoryData) {
			mapBonusProvidedWH = aggregateHistory(dateMax, historyBonusProvidedPower, mapBonusProvidedWH,
					cleanHistoryData);
		} else {
			mapBonusProvidedWH = auxAggregateHistory(dateMax, historyBonusProvidedPower);
		}
		// Aggregate history of peanalty du to not given power
		if (cleanHistoryData) {
			mapMalusNotGivenWH = aggregateHistory(dateMax, historyMalusNotGivenPower, mapMalusNotGivenWH, cleanHistoryData);
		} else {
			mapMalusNotGivenWH = auxAggregateHistory(dateMax, historyMalusNotGivenPower);
		}

		if (cleanHistoryData) {
			auxCleanHistoryData(dateMax, historyProposedPower);
			cleanHistoryDates(dateMax);
		}
	}

	public void cleanHistoryDates(Date dateMax) {
		List<Date> toRemove = new ArrayList<Date>();
		for (Date nextTime : historyDates) {
			if (nextTime.before(dateMax)) {
				toRemove.add(nextTime);
			}
		}
		for (Date date : toRemove) {
			historyDates.remove(date);
		}
	}

	public void auxCleanHistoryData(Date dateMax, Map<String, Map<Date, Double>> historyPowerData) {
		for (String providerName : historyPowerData.keySet()) {
			Map<Date, Double> prosumerHistoryProvidedPower = historyPowerData.get(providerName);
			for (Date nextTime : historyDates) {
				if (nextTime.before(dateMax) && prosumerHistoryProvidedPower.containsKey(nextTime)) {
					prosumerHistoryProvidedPower.remove(nextTime);
				}
			}
		}
	}

	public Map<String, Double> aggregateHistory(Date dateMax, Map<String, Map<Date, Double>> historyPowerData, Map<String, Double> mapWH, boolean appendOnCurrent) {
		Map<String, Double> aggregatedHistoryWH = auxAggregateHistory(dateMax, historyPowerData);
		if (appendOnCurrent) {
			mapWH = SapereUtil.mergeMapStrDouble(aggregatedHistoryWH,mapWH);
			auxCleanHistoryData(dateMax, historyPowerData);
			return mapWH;
		} else {
			return aggregatedHistoryWH;
		}
	}

	public Map<String, Double> auxAggregateHistory(Date dateMax, Map<String, Map<Date, Double>> historyPowerData) {
		Map<String, Double> mapWH = new HashMap<String, Double>();
		for (String providerName : historyPowerData.keySet()) {
			Map<Date, Double> prosumerHistoryPower = historyPowerData.get(providerName);
			Date previousTime = null;
			double previousPower = 0;
			double providedW_MS = 0.0;
			for (Date nextTime : historyDates) {
				if (!nextTime.after(dateMax)) {
					Double nextPower = prosumerHistoryPower.containsKey(nextTime)
							? prosumerHistoryPower.get(nextTime)
							: 0.0;
					if (previousTime != null && previousPower > 0 && nextPower > 0) {
						double deltaMS = nextTime.getTime() - previousTime.getTime();
						double consideredPower = Math.min(previousPower, nextPower);
						providedW_MS += (deltaMS * consideredPower);
					}
					previousTime = nextTime;
					previousPower = nextPower;
				}
			}
			double providedWH = providedW_MS / UtilDates.MS_IN_HOUR;
			mapWH.put(providerName, providedWH);
		}
		return mapWH;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("AwardsComputingData [");
		result.append(SapereUtil.CR).append("mapProvidedWH = {" + SapereUtil.auxLogMapPowers(mapProvidedWH) + "}");
		result.append(SapereUtil.CR).append("mapIdealSupplyWH = {" + SapereUtil.auxLogMapPowers(mapIdealSupplyWH) + "}");
		result.append(SapereUtil.CR).append("mapBonusProvidedWH = {" + SapereUtil.auxLogMapPowers(mapBonusProvidedWH) + "}");
		result.append(SapereUtil.CR).append("mapMalusNotGivenWH = {" + SapereUtil.auxLogMapPowers(mapMalusNotGivenWH) + "}");
		boolean displayHistory = false;
		result.append(SapereUtil.CR).append("historyNoGivenPower = {" + SapereUtil.auxLogComposedMapPowers(historyMalusNotGivenPower, true) + "}");
		if(displayHistory) {
			result.append(SapereUtil.CR).append("historyDates");
			for (Date nextDate : historyDates) {
				result.append(UtilDates.format_time.format(nextDate)).append(",");
			}
			result.append(SapereUtil.CR ).append("historyProvidedPower = {" + SapereUtil.auxLogComposedMapPowers(historyProvidedPower, true) + "}");
			result.append(SapereUtil.CR ).append("historyIdealSupplyPower = {" + SapereUtil.auxLogComposedMapPowers(historyIdealSupplyPower, true) + "}");
			result.append(SapereUtil.CR).append("historyBonusProvidedPower = {" + SapereUtil.auxLogComposedMapPowers(historyBonusProvidedPower, true) + "}");
			result.append(SapereUtil.CR).append("historyNoGivenPower = {" + SapereUtil.auxLogComposedMapPowers(historyMalusNotGivenPower, true) + "}");
			result.append(SapereUtil.CR + "lastUpdate=" + UtilDates.format_time.format(lastHistoryDate) + "]");
		}
		result.append(SapereUtil.CR).append("cumulativeArewardTable = ").append(SapereUtil.auxLogMapPowers(cumulativeArewardTable.getShortContent()));
		return result.toString();
	}

}
