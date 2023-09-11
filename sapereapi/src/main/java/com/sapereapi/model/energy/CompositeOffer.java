package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.exception.UnauthorizedModificationException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeConfig;

public class CompositeOffer implements IEnergyObject, Cloneable, Serializable {
	private static final long serialVersionUID = 10L;
	protected String consumerAgent;
	protected EnergyRequest request;
	protected Date beginDate;
	protected Date endDate;
	protected DeviceProperties consumerDeviceProperties;
	//protected EnvironmentalImpact consumerEnvironmentalImpact;
	protected Map<String, Double> mapPower;
	protected Map<String, Double> mapPowerMax;
	protected Map<String, Double> mapPowerMin;
	protected Map<String, NodeConfig> mapLocation;
	protected Map<String, PricingTable> mapPricingTable;
	protected List<Long> singleOffersIds = new ArrayList<Long>();	// TODO put in a map by node. ids can only be used in the same node
	protected boolean isMerged = false;
	protected Long timeShiftMS;

	public CompositeOffer() {
		super();
		this.mapPower = new HashMap<String, Double>();
		this.mapPowerMax = new HashMap<String, Double>();
		this.mapPowerMin = new HashMap<String, Double>();
		this.mapLocation = new HashMap<String, NodeConfig>();
		this.mapPricingTable = new HashMap<String, PricingTable>();
		this.singleOffersIds =  new ArrayList<Long>();
	}

	public CompositeOffer(
			//EnergyAgent aConsumerAgent
			EnergyRequest agentRequest) {
		super();
		//EnergyRequest agentRequest = request; //aConsumerAgent.getEnergyRequest();
		this.consumerAgent = agentRequest.getIssuer();// aConsumerAgent.getAgentName();
		this.consumerDeviceProperties = agentRequest.getDeviceProperties().clone();
		this.request = agentRequest.clone();
		this.beginDate = request.getBeginDate();
		this.endDate = request.getEndDate();
		this.timeShiftMS = request.getTimeShiftMS();
		this.mapPower = new HashMap<String, Double>();
		this.mapPowerMax = new HashMap<String, Double>();
		this.mapPowerMin = new HashMap<String, Double>();
		this.mapLocation = new HashMap<String, NodeConfig>();
		//String consumerLoc = aConsumerAgent.getAuthentication().getAgentLocation();
		this.mapLocation.put(consumerAgent, request.getIssuerLocation());
		this.singleOffersIds =  new ArrayList<Long>();
	}

	public EnergyRequest getRequest() {
		return request;
	}

	public void setRequest(EnergyRequest request) {
		this.request = request;
	}

	public Set<String> getProducerAgents() {
		return mapPower.keySet();
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
		return mapPower;
	}

	public void setMapPower(Map<String, Double> _mapPowers) {
		this.mapPower = _mapPowers;
	}

	public Map<String, Double> getMapPowerMax() {
		return mapPowerMax;
	}

	public void setMapPowerMax(Map<String, Double> mapPowerMax) {
		this.mapPowerMax = mapPowerMax;
	}

	public Map<String, Double> getMapPowerMin() {
		return mapPowerMin;
	}

	public void setMapPowerMin(Map<String, Double> mapPowerMin) {
		this.mapPowerMin = mapPowerMin;
	}

	public Map<String, NodeConfig> getMapLocation() {
		return mapLocation;
	}

	public void setMapLocation(Map<String, NodeConfig> mapLocation) {
		this.mapLocation = mapLocation;
	}

	public void setSingleOffersIds(List<Long> singleOffersIds) {
		this.singleOffersIds = singleOffersIds;
	}


	/*
	public void setConsumerEnvironmentalImpact(EnvironmentalImpact consumerEnvironmentalImpact) {
		this.consumerEnvironmentalImpact = consumerEnvironmentalImpact;
	}*/

	public DeviceProperties getConsumerDeviceProperties() {
		return consumerDeviceProperties;
	}

	public void setConsumerDeviceProperties(DeviceProperties consumerDeviceProperties) {
		this.consumerDeviceProperties = consumerDeviceProperties;
	}

	public String getIssuer() {
		return request.getIssuer();
	}

	public int getIssuerDistance() {
		return request.getIssuerDistance();
	}

	public boolean isIssuerLocal() {
		return request.isIssuerLocal();
	}

	public NodeConfig getIssuerLocation() {
		return request.getIssuerLocation();
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

	public Long getTimeShiftMS() {
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
		if (this.consumerAgent.equals(singleOffer.getConsumerAgent()) && !singleOffer.hasExpired(0)) {
			Double missing = request.getPower() - getPower();
			if (missing > 0) {
				double addedPower = Math.min(missing, singleOffer.getPower());
				double addedPowerMax = addedPower + singleOffer.getPowerMargin();
				double addPowerMin = Math.max(0.0, addedPower - singleOffer.getPowerMargin());
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
					mapPower.put(producer, addedPower);
					mapPowerMin.put(producer, addPowerMin);
					mapPowerMax.put(producer, addedPowerMax);
					mapLocation.put(producer, singleOffer.getIssuerLocation());
					if(singleOffer.getId() != null && singleOffer.getId()>0) {
						singleOffersIds.add(singleOffer.getId());
					}
				}
			}
		}
	}

	public String getConsumerAgent() {
		return consumerAgent;
	}

	public NodeConfig getConsumerLocation() {
		if(request==null) {
			return null;
		}
		return request.getIssuerLocation();
	}

	public void setConsumerAgent(String consumerAgent) {
		this.consumerAgent = consumerAgent;
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

	public boolean hasSingleOffersIds() {
		return this.singleOffersIds.size() > 0;
	}

	public List<Long> getSingleOffersIds() {
		return singleOffersIds;
	}

	public String getSingleOffersIdsStr() {
		StringBuffer result = new StringBuffer();
		String sep="";
		for(Long id : singleOffersIds) {
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
		return SapereUtil.auxComputeMapTotal(this.mapPower);
	}

	protected Double auxComputeTotalPowerMin() {
		return SapereUtil.auxComputeMapTotal(this.mapPowerMin);
	}

	protected Double auxComputeTotalPowerMax() {
		return SapereUtil.auxComputeMapTotal(this.mapPowerMax);
	}

	protected PricingTable auxComputeTotalPricingTable() {
		return SapereUtil.auxComputeMapPricingTable(mapPower, mapPricingTable, timeShiftMS);
	}

	protected Double auxComputeTotalPower(String locationFilter) {
		double result = 0;
		for (String nextAgent : this.mapPower.keySet()) {
			double nextPower = mapPower.get(nextAgent);
			if(locationFilter!=null) {
				NodeConfig nextLocation = mapLocation.get(nextAgent);
				if(locationFilter.equals(nextLocation.getMainServiceAddress()) && mapPower.containsKey(nextAgent)) {
					result += nextPower;
				}
			} else {
				result += nextPower;
			}
		}
		return result;
	}

	protected Double auxComputeTotalPowerMin(String locationFilter) {
		double result = 0;
		for (String nextAgent : this.mapPowerMin.keySet()) {
			double nextPowerMin = mapPowerMin.get(nextAgent);
			if(locationFilter!=null) {
				NodeConfig nextLocation = mapLocation.get(nextAgent);
				if(locationFilter.equals(nextLocation.getMainServiceAddress()) && mapPowerMin.containsKey(nextAgent)) {
					result += nextPowerMin;
				}
			} else {
				result += nextPowerMin;
			}
		}
		return result;
	}

	protected Double auxComputeTotalPowerMax(String locationFilter) {
		double result = 0;
		for (String nextAgent : this.mapPowerMax.keySet()) {
			double nextPowerMax = mapPowerMax.get(nextAgent);
			if(locationFilter!=null) {
				NodeConfig nextLocation = mapLocation.get(nextAgent);
				if(locationFilter.equals(nextLocation.getMainServiceAddress()) && mapPowerMax.containsKey(nextAgent)) {
					result += nextPowerMax;
				}
			} else {
				result += nextPowerMax;
			}
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
			return new PowerSlot(
					 auxComputeTotalPower()
					,auxComputeTotalPowerMin()
					,auxComputeTotalPowerMax()
					)
				;
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
			return new PowerSlot(
					 auxComputeTotalPower(locationFilter)
					,auxComputeTotalPowerMin(locationFilter)
					,auxComputeTotalPowerMax(locationFilter)
					)
				;
		}
		return new PowerSlot();
	}

	public PowerSlot computePowerSlot(String locationFilter) {
		if (!hasExpired()) {
			return new PowerSlot(
					 auxComputeTotalPower(locationFilter)
					,auxComputeTotalPowerMin(locationFilter)
					,auxComputeTotalPowerMax(locationFilter)
					)
				;
		}
		return new PowerSlot();
	}

	public Double getPowerMarginProducer(String producer) {
		if(mapPowerMax.containsKey(producer) && mapPower.containsKey(producer)) {
			return this.mapPowerMax.get(producer) - this.mapPower.get(producer);
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
		return (int) (this.getPower() - other.getPower());
	}

	public boolean hasProducer(String agentName) {
		return mapPower.containsKey(agentName);
	}

	public Double getPowerFromAgent(String agent) {
		return this.mapPower.get(agent);
	}

	public Double getPowerMinFromAgent(String agent) {
		return this.mapPowerMin.get(agent);
	}

	public Double getPowerMaxFromAgent(String agent) {
		return this.mapPowerMax.get(agent);
	}

	public PowerSlot getPowerSlotFromAgent(String agent) {
		return new PowerSlot(
			 getPowerFromAgent(agent)
			,getPowerMinFromAgent(agent)
			,getPowerMaxFromAgent(agent)
			);
	}

	@Override
	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}

	public NodeConfig getLocationFromAgent(String agent) {
		return this.mapLocation.get(agent);
	}

	public void checkBeginNotPassed() {
		Date current = getCurrentDate();
		if (current.after(beginDate)) {
			beginDate = current;
			 SapereLogger.getInstance().info("date correction : "  + UtilDates.format_time.format(beginDate)) ;
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
		result.append("GlobalOffer ").append(this.consumerAgent).append(" ").append(this.getProducerAgents())
				.append(" ").append(this.getPower()).append(" From ")
				.append(UtilDates.format_time.format(this.beginDate)).append(" To ")
				.append(UtilDates.format_time.format(this.endDate));
		if(consumerDeviceProperties==null) {
			SapereLogger.getInstance().warning("CompositeOffer toString consumerDeviceProperties is null");
		}
		return result.toString();
	}

	public CompositeOffer copy(boolean copyIds) {
		CompositeOffer result = new CompositeOffer();
		result.setConsumerAgent(consumerAgent);
		result.setRequest(request.copy(copyIds));
		result.setBeginDate(new Date(beginDate.getTime()));
		result.setEndDate(new Date(endDate.getTime()));
		if(consumerDeviceProperties!=null) {
			result.setConsumerDeviceProperties(consumerDeviceProperties.clone());
		} else {
			SapereLogger.getInstance().warning("CompositeOffer clone consumerDeviceProperties is null");
		}
		result.setMerged(isMerged);
		result.setTimeShiftMS(timeShiftMS);
		//result.setConsumerEnvironmentalImpact(consumerEnvironmentalImpact);
		// clone of mapPower table
		result.setMapPower(cloneMapPower());

		// clone of mapPowerMin table
		result.setMapPowerMin(cloneMapPowerMin());

		// clone of mapPowerMax table
		result.setMapPowerMax(cloneMapPowerMax());

		HashMap<String, NodeConfig> mapLocationCopy = new HashMap<>();
		for(String key : mapLocation.keySet()) {
			NodeConfig location = mapLocation.get(key);
			mapLocationCopy.put(key, location.copy(copyIds));
		}
		result.setMapLocation(mapLocationCopy);
		return result;
	}

	@Override
	public CompositeOffer clone() {
		return copy(true);
	}

	@Override
	public CompositeOffer copyForLSA() {
		return copy(false);
	}

	@Override
	public boolean isComplementary() {
		return request.isComplementary();
	}

	protected Map<String, Double> cloneMapPower() {
		Map<String, Double> result = new HashMap<>();
		result.putAll(this.mapPower);
		return result;
	}

	protected Map<String, Double> cloneMapPowerMin() {
		Map<String, Double> result = new HashMap<>();
		result.putAll(this.mapPowerMin);
		return result;
	}

	protected Map<String, Double> cloneMapPowerMax() {
		Map<String, Double> result = new HashMap<>();
		result.putAll(this.mapPowerMax);
		return result;
	}


	public boolean hasChanged(CompositeOffer newContent) {
		if (newContent == null) {
			return true;
		}
		if (!this.consumerAgent.equals(newContent.getConsumerAgent())) {
			return true;
		}
		if (this.isMerged != newContent.isMerged) {
			return true;
		}
		if(SapereUtil.areMapDifferent(mapPower, newContent.getMapPower())) {
			return true;
		}
		if(SapereUtil.areMapDifferent(mapPowerMin, newContent.getMapPowerMin())) {
			return true;
		}
		if(SapereUtil.areMapDifferent(mapPowerMax, newContent.getMapPowerMax())) {
			return true;
		}
		if (!this.endDate.equals(newContent.getEndDate())) {
			return true;
		}
		return false;
	}

	public boolean canModify(EnergyRequest newRequest, EnergyRequest complementaryRequest) throws Exception {
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
			throw new UnauthorizedModificationException("requested power " + UtilDates.df2.format(newRequest.getPower())
				+ " [agent " + newRequest.getIssuer() + "]"
				+ " higher than the max limit " +  UtilDates.df2.format(maxPower));
		}
	}

	public boolean modifyRequest(EnergyRequest newRequest, EnergyRequest complementaryRequest, AbstractLogger logger) throws Exception{
		if(canModify(newRequest, complementaryRequest)) {
			logger.info("modifyRequest begin : initial gap = " + this.computeGap());
			EnergyRequest newRequest2 = newRequest.clone();
			logger.info("CompositeOffer.modifyRequest newRequest = " + newRequest2 + ", complementaryRequest = " + complementaryRequest);
			CompositeOffer oldContract = this.clone();
			Date newBeginDate = getCurrentDate();
			newRequest2.setBeginDate(new Date(newBeginDate.getTime()));
			this.setRequest(newRequest2);
			this.setBeginDate(new Date(newBeginDate.getTime()));
			this.modifyPower(newRequest2.getPower(), logger);
			return this.hasChanged(oldContract);
		}
		return false;
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
			Map<String, Double> cloneMapPowerMax = cloneMapPowerMax();
			double discountFactor = Math.min(1.0, totalPowerToSet / currentTotalPower);
			double marginFactor = 0;
			boolean useMinCondition = (totalPowerToSet >= currentTotalPowerMin);
			if(currentTotalPowerMargin!=0 && totalPowerToSet > currentTotalPower) {
				marginFactor =  (totalPowerToSet - currentTotalPower) / currentTotalPowerMargin;
			}
			// Update power for each producer
			for (String producer : this.mapPower.keySet()) {
				double maxValue = this.mapPowerMax.get(producer);
				double minValue = this.mapPowerMin.get(producer);
				double newPower1 = 0;
				if(totalPowerToSet > currentTotalPower) {
					double currentMarginProducer = this.getPowerMarginProducer(producer);
					newPower1 = this.mapPower.get(producer)  + marginFactor*currentMarginProducer;
				} else { // powerToSet < currentPower
					newPower1 = discountFactor * this.mapPower.get(producer);
				}
				double newPower = SapereUtil.round(newPower1, Sapere.NB_DEC_POWER);
				if(useMinCondition) {
					newPower = Math.max(minValue, newPower);	// the power to set cannot be lower than the min value
				}
				newPower = Math.min(maxValue, newPower);	// the power to set cannot be higher than the max value
				mapPower.put(producer, newPower);
			}
			if(Math.abs(getPower() - totalPowerToSet) >= 0.0001) {
				logger.warning("modifyPower STEP1 : did not manage to set the target value " + totalPowerToSet + " gap=" + this.computeGap());
				Map<String, Double> usedMpaPowerMin = useMinCondition? cloneMapPowerMin() : new HashMap<>();
				SapereUtil.adjustMapValues(mapPower, totalPowerToSet, usedMpaPowerMin, cloneMapPowerMax, logger);
			}
			if(Math.abs(getPower() - totalPowerToSet) >= 0.001) {
				logger.error("modifyPower STEP2 : did not manage to set the target value " + totalPowerToSet + ", gap = " + this.computeGap());
			}
			if(!useMinCondition || getPower() < currentTotalPowerMin) {
				// Update power min and max for each producer
				for (String producer : this.mapPower.keySet()) {
					double newPower = mapPower.get(producer);
					// update min power value
					double newPowerMin = SapereUtil.round((1 - EnergySupply.DEFAULT_POWER_MARGIN_RATIO) * newPower,2);
					mapPowerMin.put(producer, newPowerMin);
					// update max power value
					double newPowerMax = SapereUtil.round((1 + EnergySupply.DEFAULT_POWER_MARGIN_RATIO) * newPower,2);
					mapPowerMax.put(producer, newPowerMax);
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


	public void checkPowers() throws Exception {
		// Checkup for all producer
		for (String producer : this.mapPower.keySet()) {
			Double power = this.mapPower.get(producer);
			Double powerMin = this.mapPowerMin.get(producer);
			Double powerMax = this.mapPowerMax.get(producer);
			String startMsg = "CompositeOffer power checkup " + producer + " : ";
			if(SapereUtil.round(powerMin,3) > SapereUtil.round(powerMax,3)) {
				throw new Exception(startMsg + UtilDates.df2.format(powerMin) + " cannot be higher  than powerMax " + UtilDates.df2.format(powerMax));
			}
			if(SapereUtil.round(power,3) > SapereUtil.round(powerMax,3)) {
				throw new Exception(startMsg +  UtilDates.df2.format(power) + " cannot be higher than powerMax " +  UtilDates.df2.format(powerMax));
			}
			if(SapereUtil.round(power,3) < SapereUtil.round(powerMin,3)) {
				throw new Exception(startMsg +  UtilDates.df2.format(power) + " cannot be lower than powerMin " +  UtilDates.df2.format(powerMin));
			}
		}
	}

	public boolean checkCanMerge(CompositeOffer otherContract) throws Exception {
		if(this.isComplementary()) {
			throw new Exception("CompositeOffer.merge : the contract to merge must not be complementary");
		}
		if(otherContract == null) {
			throw new Exception("CompositeOffer.merge : the contract to add is null");
		}
		if(!otherContract.isComplementary()) {
			throw new Exception("CompositeOffer.merge : the contract to add must be complementary");
		}
		if(!this.getIssuer().equals(otherContract.getIssuer())) {
			throw new Exception("CompositeOffer.merge : the contract to add must have the same isser");
		}
		if(this.hasGap()) {
			throw new Exception("CompositeOffer.merge : the contract to merge has a gap of " + this.computeGap());
		}
		if(otherContract.hasGap()) {
			throw new Exception("CompositeOffer.merge : the contract to add has a gap of " + this.computeGap());
		}
		return true;
	}

	public boolean merge(CompositeOffer otherContract) throws Exception {
		this.isMerged = false;
		if(checkCanMerge(otherContract)) {
			beginDate = UtilDates.getCurrentSeconde(timeShiftMS);
			if (otherContract.getBeginDate().after(beginDate)) {
				beginDate = otherContract.getBeginDate();
			}
			if (otherContract.getEndDate().before(endDate)) {
				endDate = otherContract.getEndDate();
			}
			this.mapPower = SapereUtil.mergeMapStrDouble(this.mapPower, otherContract.getMapPower());
			this.mapPowerMin =  SapereUtil.mergeMapStrDouble(this.mapPowerMin, otherContract.getMapPowerMin());
			this.mapPowerMax = SapereUtil.mergeMapStrDouble(this.mapPowerMax, otherContract.getMapPowerMax());
			for(String producer : otherContract.getMapLocation().keySet()) {
				NodeConfig otherLocation = otherContract.getMapLocation().get(producer);
				if(mapLocation.containsKey(producer)) {
					// check if the locations are equal
					NodeConfig location = mapLocation.get(producer);
					if(!location.equals(otherLocation)) {
						throw new Exception("CompositeOffer.merge : the contract to add must have the same location for the producer " + producer);
					}
				} else {
					mapLocation.put(producer, otherLocation);
				}
			}
			singleOffersIds.addAll(otherContract.getSingleOffersIds());
			PowerSlot reqPower = new PowerSlot(this.request.getPower(), this.request.getPowerMin(), this.request.getPowerMax());
			PowerSlot otherReqPower = otherContract.getRequest().getPowerSlot();
			reqPower.add(otherReqPower);
			this.request.setPower(reqPower.getCurrent() );
			this.request.setPowerMin(reqPower.getMin());
			this.request.setPowerMax(reqPower.getMax());
			this.isMerged = true;
		}
		return isMerged;
	}

	@Override
	public CompositeOffer aggregate(String operator, List<IAggregateable> listObjects, AgentAuthentication agentAuthentication) {
		CompositeOffer result = null;
		for(IAggregateable nextObj : listObjects) {
			if(nextObj instanceof CompositeOffer) {
				CompositeOffer nextCompOffer = (CompositeOffer) nextObj;
				if(result == null || result.getPower() < nextCompOffer.getPower()) {
					result = nextCompOffer;
				}
			}
		}
		return result;
	}
/*
	public void completeLocationId(Map<String, NodeConfig> mapNeighborNodeConfigs) {
		this.request.completeLocationId(mapNeighborNodeConfigs);
		for(NodeConfig nodeLocation : mapLocation.values()) {
			if(nodeLocation.getId() == null) {
				String nodeName = nodeLocation.getName();
				if(mapNeighborNodeConfigs.containsKey(nodeName)) {
					NodeConfig correctionNodeConfig = mapNeighborNodeConfigs.get(nodeName);
					nodeLocation.setId(correctionNodeConfig.getId());
				}
			}
		}
	}
*/

	public boolean checkLocationId() {
		if(!request.checkLocationId()) {
			return false;
		}
		for(NodeConfig nodeLocation : mapLocation.values()) {
			if(nodeLocation.getId() == null) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void completeContent(Lsa bondedLsa, Map<String, NodeConfig> mapNodeLocation) {
		request.completeContent(bondedLsa, mapNodeLocation);
		for(String key : mapLocation.keySet()) {
			NodeConfig nodeConfig = mapLocation.get(key);
			if(nodeConfig.getId() == null && mapNodeLocation.containsKey(nodeConfig.getName())) {
				nodeConfig.completeContent(bondedLsa, mapNodeLocation);
			}
		}
	}

	@Override
	public List<NodeConfig> retrieveInvolvedLocations() {
		List<NodeConfig> result = request.retrieveInvolvedLocations();
		for(String key : mapLocation.keySet()) {
			NodeConfig nodeConfig = mapLocation.get(key);
			if(result.contains(nodeConfig))  {
				result.add(nodeConfig);
			}
		}
		return result;
	}
}