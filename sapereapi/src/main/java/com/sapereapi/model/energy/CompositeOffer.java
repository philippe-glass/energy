package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.exception.UnauthorizedModificationException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class CompositeOffer implements IEnergyObject, Cloneable {
	private static final long serialVersionUID = 10L;
	protected EnergyRequest request;
	protected Date beginDate;
	protected Date endDate;
	protected Map<String, SingleContribution> mapContributions; // TODO put an accessor a map by node. ids can only be used in the same node
	protected boolean isMerged = false;
	protected Long timeShiftMS;

	public CompositeOffer() {
		super();
		this.mapContributions = new HashMap<String, SingleContribution>();
	}

	public CompositeOffer(EnergyRequest agentRequest) {
		super();
		this.request = agentRequest.clone();
		this.beginDate = request.getBeginDate();
		this.endDate = request.getEndDate();
		this.timeShiftMS = request.getTimeShiftMS();
		this.mapContributions = new HashMap<String, SingleContribution>();
	}

	public EnergyRequest getRequest() {
		return request;
	}

	public void setRequest(EnergyRequest request) {
		this.request = request;
	}

	public Set<String> getProducerAgents() {
		return mapContributions.keySet();
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Map<String, Double> getMapPower() {
		Map<String, Double> result = new HashMap<String, Double>();
		for(String supplier : mapContributions.keySet()) {
			SingleContribution nextContribution = mapContributions.get(supplier);
					result.put(supplier, nextContribution.getPower());
		}
		return result;
	}

	public Map<String, Double> getMapPowerMin() {
		Map<String, Double> result = new HashMap<String, Double>();
		for(String supplier : mapContributions.keySet()) {
			SingleContribution nextContribution = mapContributions.get(supplier);
					result.put(supplier, nextContribution.getPowerMin());
		}
		return result;
	}

	public Map<String, Double> getMapPowerMax() {
		Map<String, Double> result = new HashMap<String, Double>();
		for(String supplier : mapContributions.keySet()) {
			SingleContribution nextContribution = mapContributions.get(supplier);
					result.put(supplier, nextContribution.getPowerMax());
		}
		return result;
	}

	public Map<String, PowerSlot> getMapPowerSlot() {
		Map<String, PowerSlot> result = new HashMap<String, PowerSlot>();
		for(String supplier : mapContributions.keySet()) {
			SingleContribution nextContribution = mapContributions.get(supplier);
					result.put(supplier, nextContribution.getPowerSlot());
		}
		return result;
	}

	public Map<String, NodeLocation> getMapLocation() {
		Map<String, NodeLocation> result = new HashMap<String, NodeLocation>();
		result.put(getConsumerAgent(), getConsumerLocation());
		for (String supplier : mapContributions.keySet()) {
			SingleContribution nextContribution = mapContributions.get(supplier);
			result.put(supplier, nextContribution.getLocation());
		}
		return result;
	}

	public Map<String, PricingTable> getMapPricingTable() {
		Map<String, PricingTable> result = new HashMap<String, PricingTable>();
		for (String supplier : mapContributions.keySet()) {
			SingleContribution nextContribution = mapContributions.get(supplier);
			if (nextContribution.getPricingTable() != null) {
				result.put(supplier, nextContribution.getPricingTable());
			}
		}
		return result;
	}

	public Map<String, SingleContribution> getMapContributions() {
		return mapContributions;
	}

	public void setMapContributions(Map<String, SingleContribution> mapContributions) {
		this.mapContributions = mapContributions;
	}

	public String getIssuer() {
		return request.getIssuer();
	}

	public int getIssuerDistance() {
		if(request.getIssuerProperties() == null) {
			return 0;
		}
		return request.getIssuerProperties().getDistance();
	}

	public boolean isIssuerLocal() {
		return request.isIssuerLocal();
	}

	public NodeLocation getIssuerLocation() {
		if(request.getIssuerProperties() == null) {
			return null;
		}
		return request.getIssuerProperties().getLocation();
	}

	@Override
	public ProsumerProperties getIssuerProperties() {
		return request.getIssuerProperties();
	}

	public TimeSlot getTimeSlot() {
		return new TimeSlot(beginDate, endDate);
	}

	@Override
	public Double getPowerMargin() {
		return this.getPowerMax() - this.getPower();
	}

	public boolean isMerged() {
		return isMerged;
	}

	public void setMerged(boolean isMerged) {
		this.isMerged = isMerged;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	@Override
	public boolean isInActiveSlot(Date aDate) {
		return aDate != null && (!aDate.before(beginDate)) && aDate.before(this.endDate);
	}



	public void addSingleOffer(SingleOffer singleOffer) {
		String consumerAgent = getConsumerAgent();
		if (consumerAgent.equals(singleOffer.getConsumerAgent()) && !singleOffer.hasExpired(0)) {
			Double missing = request.getPower() - getPower();
			if (missing > 0) {
				double addedPower = Math.min(missing, singleOffer.getPower());
				double addedPowerMax = addedPower + singleOffer.getPowerMargin();
				double addedPowerMin = Math.max(0.0, addedPower - singleOffer.getPowerMargin());
				if (addedPower > 0) {
					Date nextBeginDate = singleOffer.getBeginDate();
					if (nextBeginDate.after(beginDate)) {
						beginDate = nextBeginDate;
					}
					Date nextEndDate = singleOffer.getEndDate();
					if (nextEndDate.before(endDate)) {
						endDate = nextEndDate;
					}
					String producer = singleOffer.getProducerAgent();
					PowerSlot providedPowerSlot = new PowerSlot(addedPower, addedPowerMin, addedPowerMax);
					NodeLocation providerLocation = singleOffer.getIssuerProperties().getLocation();
					List<Long> listOfferIds = new ArrayList<Long>();
					listOfferIds.add(singleOffer.getId());
					SingleContribution contribution = new SingleContribution(providedPowerSlot, providerLocation, singleOffer.getPricingTable(), listOfferIds);
					mapContributions.put(producer, contribution);
				}
			}
		}
	}

	public String getConsumerAgent() {
		String consumerAgent = request == null ? "" : request.getIssuer();
		return consumerAgent;
	}

	public NodeLocation getConsumerLocation() {
		if(request == null || request.getIssuerProperties() == null) {
			return null;
		}
		return request.getIssuerProperties().getLocation();
	}

	public Date getDate() {
		return beginDate;
	}

	public void setDate(Date date) {
		this.beginDate = date;
	}

	public boolean hasExpired() {
		if(this.endDate==null) {
			return false;
		}
		Date current = getCurrentDate();
		return current.after(this.endDate);
	}

	public boolean isAboutToExpire(int nbSecond) {
		if(this.endDate==null) {
			return false;
		}
		Date current = getCurrentDate();
		Date enDate2 = UtilDates.shiftDateSec(endDate, -1*nbSecond);
		return current.before(this.endDate) && !current.before(enDate2);
	}

	public boolean hasSingleOffersIds() {
		for(SingleContribution nextContribution : mapContributions.values()) {
			if(nextContribution.hasOffersId()) {
				return true;
			}
		}
		return false;
	}

	public List<Long> getSingleOffersIds() {
		List<Long> result = new ArrayList<Long>();
		for(SingleContribution nextContribution : mapContributions.values()) {
			if(nextContribution.hasOffersId()) {
				result.addAll(nextContribution.getListOfferId());
			}
		}
		return result;
	}

	public String getSingleOffersIdsStr() {
		StringBuffer result = new StringBuffer();
		String sep="";
		List<Long> listOfferIds = getSingleOffersIds();
		for(Long id : listOfferIds) {
			result.append(sep).append(id);
			sep=",";
		}
		return result.toString();
	}

	public boolean isActive() {
		Date current = getCurrentDate();
		return (!current.before(beginDate)) && current.before(this.endDate);
	}

	public Double getDuration() {
		double duration = UtilDates.computeDurationMinutes(beginDate, endDate);
		return duration;
	}

	protected Double auxComputeTotalPower() {
		return auxComputeTotalPower(null);
	}

	protected Double auxComputeTotalPowerMin() {
		return auxComputeTotalPowerMin(null);
	}

	protected Double auxComputeTotalPowerMax() {
		return auxComputeTotalPowerMax(null);
	}

	protected PricingTable auxComputeTotalPricingTable() {
		return SapereUtil.auxComputeMapPricingTable(getMapPower(), getMapPricingTable(), timeShiftMS);
	}

	protected List<PowerSlot> auxRetrievePowerSlots(String locationFilter) {
		List<PowerSlot> result = new ArrayList<PowerSlot>();
		for (SingleContribution nextContribution : this.mapContributions.values()) {
			if (locationFilter == null || nextContribution.hasMainServerAddress(locationFilter)) {
				result.add(nextContribution.getPowerSlot());
			}
		}
		return result;
	}

	protected Double auxComputeTotalPower(String locationFilter) {
		double result = 0;
		for (PowerSlot nextSlot : auxRetrievePowerSlots(locationFilter)) {
			result += nextSlot.getCurrent();
		}
		return result;
	}

	protected Double auxComputeTotalPowerMin(String locationFilter) {
		double result = 0;
		for (PowerSlot nextSlot : auxRetrievePowerSlots(locationFilter)) {
			result += nextSlot.getMin();
		}
		return result;
	}

	protected Double auxComputeTotalPowerMax(String locationFilter) {
		double result = 0;
		for (PowerSlot nextSlot : auxRetrievePowerSlots(locationFilter)) {
			result += nextSlot.getMax();
		}
		return result;
	}

	protected PowerSlot auxComputeTotalPowerSlot(String locationFilter) {
		PowerSlot result = PowerSlot.create(0);
		for (PowerSlot nextSlot : auxRetrievePowerSlots(locationFilter)) {
			result.add(nextSlot);
		}
		return result;
	}

	public Double computeInitalPower() {
		return auxComputeTotalPower();
	}

	public Double computeInitalPowerMin() {
		return auxComputeTotalPowerMin();
	}

	public Double computeInitalPowerMax() {
		return auxComputeTotalPowerMax();
	}

	public Double getPower() {
		if (!hasExpired()) {
			return auxComputeTotalPower();
		}
		return 0.0;
	}

	public PricingTable getPricingTable() {
		if (!hasExpired()) {
			return auxComputeTotalPricingTable();
		}
		return new PricingTable(timeShiftMS);
	}

	public Double getPowerMin() {
		if (!hasExpired()) {
			return auxComputeTotalPowerMin();
		}
		return 0.0;
	}

	public Double getPowerMax() {
		if (!hasExpired()) {
			return auxComputeTotalPowerMax();
		}
		return 0.0;
	}

	public PowerSlot getPowerSlot() {
		if (!hasExpired()) {
			return auxComputeTotalPowerSlot(null);
		}
		return new PowerSlot();
	}

	public Double computePower(String locationFilter) {
		if (!hasExpired()) {
			return auxComputeTotalPower(locationFilter);
		}
		return 0.0;
	}

	public PowerSlot getForcastPowerSlot(Date aDate) {
		return getForcastPowerSlot(null, aDate);
	}

	public PowerSlot getForcastPowerSlot(String locationFilter, Date aDate) {
		if (!hasExpired() && isInActiveSlot(aDate)) {
			return auxComputeTotalPowerSlot(locationFilter);
		}
		return new PowerSlot();
	}

	public PowerSlot computePowerSlot(String locationFilter) {
		if (!hasExpired()) {
			return auxComputeTotalPowerSlot(locationFilter);
		}
		return new PowerSlot();
	}

	public Double getPowerMarginProducer(String producer) {
		if(this.mapContributions.containsKey(producer)) {
			SingleContribution contribution = mapContributions.get(producer);
			return contribution.getPowerMargin();
		}
		return 0.0;
	}

	public double computeGap() {
		if(this.request!=null && !hasExpired()) {
			double totalPower = this.getPower();
			return Math.abs(request.getPower() - totalPower);
		}
		return 0;
	}

	public boolean hasGap() {
		double gap=computeGap();
		return gap>=0.0001;
	}

	public int comparePower(Contract other) {
		double auxPowerDifference = this.getPower() - other.getPower();
		return (int) (100*auxPowerDifference);
	}

	public boolean hasProducer(String agentName) {
		return mapContributions.containsKey(agentName);
	}

	public PowerSlot getPowerSlotFromAgent(String agentName) {
		if(mapContributions.containsKey(agentName)) {
			SingleContribution contribution = mapContributions.get(agentName);
			return contribution.getPowerSlot();
		}
		return null;
	}

	@Override
	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}

	public NodeLocation getLocationFromAgent(String agentName) {
		if(mapContributions.containsKey(agentName)) {
			SingleContribution contribution = mapContributions.get(agentName);
			return contribution.getLocation();
		}
		return null;
	}

	public void checkBeginNotPassed() {
		Date current = getCurrentDate();
		if (current.after(beginDate)) {
			beginDate = current;
			 SapereLogger.getInstance().info("date correction : "  + UtilDates.format_time.format(beginDate)) ;
		}
	}

	public void checkDates(AbstractLogger logger, String tag) {
		Date current = getCurrentDate();
		Date dateMin = UtilDates.shiftDateSec(current, -10);
		String consumerAgent = getConsumerAgent();
		if (beginDate.before(dateMin)) {
			logger.error("checkDate ctr " + consumerAgent + " : " + tag + " : beginDate "
					+ UtilDates.format_time.format(beginDate) + " has expired since 10 sec ");
		}
		if (endDate.before(current)) {
			logger.error("checkDate ctr " + consumerAgent + " : " + tag + " : endDate "
					+ UtilDates.format_time.format(endDate) + " has expired");
		}
	}
/*
	public EnergySupply __toEnergySupply() {
		return new EnergySupply(this.consumerAgent, NodeManager.getLocation(), false, this.getPower(), this.getPowerMin(),  this.getPowerMax(), beginDate, endDate 
				, getConsumerDeviceProperties(), getPricingTable(), timeShiftMS);
	}
*/
	public String toString() {
		StringBuffer result = new StringBuffer();
		String consumerAgent = getConsumerAgent();
		result.append("GlobalOffer ").append(consumerAgent).append(" ").append(this.getProducerAgents())
				.append(" ").append(this.getPower()).append(" From ")
				.append(UtilDates.format_time.format(this.beginDate)).append(" To ")
				.append(UtilDates.format_time.format(this.endDate));
		/*
		if(consumerDeviceProperties==null) {
			SapereLogger.getInstance().warning("CompositeOffer toString consumerDeviceProperties is null");
		}*/
		return result.toString();
	}


	public CompositeOffer copy(boolean copyIds) {
		CompositeOffer result = new CompositeOffer();
		//result.setConsumerAgent(consumerAgent);
		result.setRequest(request.copy(copyIds));
		result.setBeginDate(beginDate);
		result.setEndDate(endDate);
		result.setMerged(isMerged);
		result.setTimeShiftMS(timeShiftMS);
		// Clone of mapContribution
		Map<String, SingleContribution> cloneMapContribution = SingleContribution.cloneMapContribution(mapContributions, copyIds);
		result.setMapContributions(cloneMapContribution);
		return result;
	}

	@Override
	public CompositeOffer clone() {
		return copy(true);
	}

	@Override
	public CompositeOffer copyForLSA(AbstractLogger logger) {
		return copy(false);
	}

	@Override
	public boolean isComplementary() {
		return request.isComplementary();
	}

	public boolean hasChanged(CompositeOffer newContent) {
		if (newContent == null) {
			return true;
		}
		String consumerAgent = getConsumerAgent();
		if (!consumerAgent.equals(newContent.getConsumerAgent())) {
			return true;
		}
		if (this.isMerged != newContent.isMerged) {
			return true;
		}
		if(SapereUtil.areMapPowerSlotDifferent(this.getMapPowerSlot(), newContent.getMapPowerSlot())) {
			return true;
		}
		/*
		if(SapereUtil.areMapDifferent(mapPower, newContent.getMapPower())) {
			return true;
		}
		if(SapereUtil.areMapDifferent(mapPowerMin, newContent.getMapPowerMin())) {
			return true;
		}
		if(SapereUtil.areMapDifferent(mapPowerMax, newContent.getMapPowerMax())) {
			return true;
		}*/
		if (!this.endDate.equals(newContent.getEndDate())) {
			return true;
		}
		return false;
	}

	public boolean canModify(EnergyRequest newRequest, EnergyRequest complementaryRequest) throws HandlingException {
		double maxPower = this.getPowerMax();
		if(request.isComplementary()) {
			throw new UnauthorizedModificationException("cannot modify a complementary contract");
		}
		double neededPower = newRequest.getPower();
		if(complementaryRequest != null) {
			neededPower-=  complementaryRequest.getPower();
		}
		if(neededPower <= maxPower + 0.0001) {
			return true;
		} else {
			String newReqIssuer = newRequest.getIssuer();
			throw new UnauthorizedModificationException("requested power " + UtilDates.df5.format(newRequest.getPower())
				+ " [agent " + newReqIssuer + "]"
				+ " higher than the max limit " +  UtilDates.df5.format(maxPower));
		}
	}

	private void setCurrentPower(String producer, double powerToSet) {
		if (this.mapContributions.containsKey(producer)) {
			SingleContribution contribution = mapContributions.get(producer);
			if(contribution.getPowerSlot() == null) {
				contribution.setPowerSlot(PowerSlot.create(powerToSet));
			} else {
				contribution.getPowerSlot().setCurrent(powerToSet);
			}
		}
	}

	// TODO : return true if the minPower/maxPower has been changed
	public void modifyPower(double totalPowerToSet, AbstractLogger logger) {
		double initialGap = this.computeGap();
		double currentTotalPower = this.getPower();
		double currentTotalPowerMax = this.getPowerMax();
		double currentTotalPowerMin = this.getPowerMin();
		double currentTotalPowerMargin = this.getPowerMargin();
		if(Math.abs(totalPowerToSet - currentTotalPower ) <= 0.0001) {
			// Nothing to do
		} else if (totalPowerToSet <= currentTotalPowerMax) {
			Map<String, Double> cloneMapPowerMax = SapereUtil.cloneMapStringDouble(getMapPowerMax());
			double discountFactor = Math.min(1.0, totalPowerToSet / currentTotalPower);
			double marginFactor = 0;
			boolean useMinCondition = (totalPowerToSet >= currentTotalPowerMin);
			if(currentTotalPowerMargin!=0 && totalPowerToSet > currentTotalPower) {
				marginFactor =  (totalPowerToSet - currentTotalPower) / currentTotalPowerMargin;
			}
			// Update power for each producer
			for (String producer : mapContributions.keySet()) {
				SingleContribution contribution = mapContributions.get(producer);
				PowerSlot powerSlot = contribution.getPowerSlot();
				double newPower1 = 0;
				if(totalPowerToSet > currentTotalPower) { // We have to increase the power
					double currentMarginProducer = powerSlot.getMargin();// this.getPowerMarginProducer(producer);
					newPower1 = powerSlot.getCurrent()  + marginFactor*currentMarginProducer;
				} else { // powerToSet < currentPower : wer have to decrease the power
					newPower1 = discountFactor * powerSlot.getCurrent();
				}
				double newPower = SapereUtil.roundPower(newPower1);
				if(useMinCondition) {
					newPower = Math.max(powerSlot.getMin(), newPower);	// the power to set cannot be lower than the min value
				}
				newPower = Math.min(powerSlot.getMax(), newPower);	// the power to set cannot be higher than the max value
				contribution.getPowerSlot().setCurrent(newPower);
			}
			if(Math.abs(getPower() - totalPowerToSet) >= 0.0001) {
				logger.warning("modifyPower STEP1 : did not manage to set the target value " + totalPowerToSet + " gap=" + this.computeGap() + ", useMinCondition = " + useMinCondition
						 + SapereUtil.CR + ", mapPower = " + getMapPower()
						 + SapereUtil.CR + ", mapPowerMin = " + getMapPowerMin()
						 + SapereUtil.CR + ", mapPowerMax = " + getMapPowerMax());
				Map<String, Double> mapPowerMin = getMapPowerMin();
				Map<String, Double> usedMpaPowerMin = useMinCondition? SapereUtil.cloneMapStringDouble(mapPowerMin) : new HashMap<>();
				Map<String, Double> mapPower = getMapPower();
				SapereUtil.adjustMapValues(mapPower, totalPowerToSet, usedMpaPowerMin, cloneMapPowerMax, logger);
				for(String producer : mapPower.keySet()) {
					double powerToSet = mapPower.get(producer);
					setCurrentPower(producer, powerToSet);
				}
			}
			if(Math.abs(getPower() - totalPowerToSet) >= 0.001) {
				logger.error("modifyPower STEP2 : did not manage to set the target value " + totalPowerToSet + ", gap = " + this.computeGap()
						 + SapereUtil.CR + ", mapPower = " + getMapPower()
						 + SapereUtil.CR + ", mapPowerMin = " + getMapPowerMin()
						 + SapereUtil.CR + ", mapPowerMax = " + getMapPowerMax());
			}
			if(!useMinCondition || getPower() < currentTotalPowerMin) {
				// Update power min and max for each producer
				for (String producer : mapContributions.keySet()) {
					SingleContribution contribution = mapContributions.get(producer);
					//PowerSlot powerSlot = contribution.getPowerSlot();
					double newPower = contribution.getPower();
					// update min power value
					double newPowerMin = SapereUtil.roundPower((1 - EnergySupply.DEFAULT_POWER_MARGIN_RATIO) * newPower);
					// update max power value
					double newPowerMax = SapereUtil.roundPower((1 + EnergySupply.DEFAULT_POWER_MARGIN_RATIO) * newPower);
					contribution.setPowerSlot(new PowerSlot(newPower, newPowerMin, newPowerMax));
				}
			}
		}
		try {
			this.checkPowers();
		} catch (Exception e) {
			logger.error(e);
		}
		if (this.hasGap()) {
			logger.error("modifyPower end : gap found : " + this.computeGap() + " , gap before = " + initialGap);
		}
	}

	public void checkPowers() throws HandlingException {
		// Checkup for all producer
		Map<String, PowerSlot> mapPowerSlots = getMapPowerSlot();
		for (String producer : mapPowerSlots.keySet()) {
			PowerSlot powerSlot = mapPowerSlots.get(producer);
			Double power = SapereUtil.roundPower(powerSlot.getCurrent());
			Double powerMin = SapereUtil.roundPower(powerSlot.getMin());
			Double powerMax = SapereUtil.roundPower(powerSlot.getMax());
			String startMsg = "CompositeOffer power checkup " + producer + " : ";
			if(powerMin > powerMax) {
				throw new HandlingException(startMsg + UtilDates.df3.format(powerMin) + " cannot be higher  than powerMax " + UtilDates.df3.format(powerMax));
			}
			if(power > powerMax) {
				throw new HandlingException(startMsg +  UtilDates.df3.format(power) + " cannot be higher than powerMax " +  UtilDates.df3.format(powerMax));
			}
			if(power < powerMin) {
				throw new HandlingException(startMsg +  UtilDates.df3.format(power) + " cannot be lower than powerMin " +  UtilDates.df3.format(powerMin));
			}
		}
	}

	public boolean checkCanMerge(CompositeOffer otherContract) throws HandlingException {
		if(this.isComplementary()) {
			throw new HandlingException("CompositeOffer.merge : the contract to merge must not be complementary");
		}
		if(otherContract == null) {
			throw new HandlingException("CompositeOffer.merge : the contract to add is null");
		}
		if(!otherContract.isComplementary()) {
			throw new HandlingException("CompositeOffer.merge : the contract to add must be complementary");
		}
		if(!this.getIssuer().equals(otherContract.getIssuer())) {
			throw new HandlingException("CompositeOffer.merge : the contract to add must have the same isser");
		}
		if(this.hasGap()) {
			throw new HandlingException("CompositeOffer.merge : the contract to merge has a gap of " + this.computeGap());
		}
		if(otherContract.hasGap()) {
			throw new HandlingException("CompositeOffer.merge : the contract to add has a gap of " + this.computeGap());
		}
		return true;
	}

	public boolean merge(CompositeOffer otherContract) throws HandlingException {
		this.isMerged = false;
		if(checkCanMerge(otherContract)) {
			beginDate = UtilDates.getCurrentSeconde(timeShiftMS);
			if (otherContract.getBeginDate().after(beginDate)) {
				beginDate = otherContract.getBeginDate();
			}
			if (otherContract.getEndDate().before(endDate)) {
				endDate = otherContract.getEndDate();
			}
			for(String producer : otherContract.getMapContributions().keySet()) {
				SingleContribution otherContribution = otherContract.getMapContributions().get(producer);
				NodeLocation otherLocation = otherContribution.getLocation();
				if(mapContributions.containsKey(producer)) {
					// check if the locations are equal
					SingleContribution contribution = mapContributions.get(producer);
					NodeLocation location = contribution.getLocation();
					if(!location.equals(otherLocation)) {
						throw new HandlingException("CompositeOffer.merge : the contract to add must have the same location for the producer " + producer);
					}
					contribution.merge(otherContribution);
				} else {
					mapContributions.put(producer, otherContribution.clone());
				}
			}
			PowerSlot reqPower = new PowerSlot(this.request.getPower(), this.request.getPowerMin(), this.request.getPowerMax());
			PowerSlot otherReqPower = otherContract.getRequest().getPowerSlot();
			reqPower.add(otherReqPower);
			this.request.setPowerSlot(reqPower);
			this.isMerged = true;
		}
		return isMerged;
	}


	public boolean checkLocation() {
		if(!request.checkLocation()) {
			return false;
		}
		for(SingleContribution nextContribution : mapContributions.values()) {
			if(!nextContribution.checkLocation()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		request.completeInvolvedLocations(bondedLsa, mapNodeLocation, logger);
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = request.retrieveInvolvedLocations();
		result.add(getConsumerLocation());
		for(SingleContribution contribution : mapContributions.values()) {
			NodeLocation nextLocation = contribution.getLocation();
			if(!result.contains(nextLocation)) {
				result.add(nextLocation);
			}
		}
		return result;
	}


}