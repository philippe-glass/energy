package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.SapereAgent;

public class AgentForm {
	private String agentType;
	private int id;
	private String url;
	private String agentName;
	private String location;
	private String deviceName;
	private OptionItem deviceCategory;
	private Integer environmentalImpact;
	private String electricalPanel;
	private String sensorNumber;
	private String deviceLocation;
	private Double power;
	private Double powerMin;
	private Double powerMax;
	private Double disabledPower;
	private Date beginDate;
	private Date endDate;
	private Double duration;
	private String[] linkedAgents;
	private Map<String, PowerSlot> ongoingContractsRepartition;
	private Double offersTotal;
	private Map<String, Double> offersRepartition;
	private PowerSlot ongoingContractsTotal;	// only ONGOING contracts
	private PowerSlot ongoingContractsTotalLocal;
	private Boolean hasExpired;
	private Double delayToleranceMinutes;
	private Double delayToleranceRatio;
	private Double availablePower;
	private Double missingPower;
	private String priorityLevel;
	private PowerSlot waitingContractsPower;
	private List<String> waitingContractsConsumers;
	private Boolean isSatisfied;
	private Boolean isInSpace;
	private Boolean isDisabled;
	private int warningDurationSec;
	//private Map<Long, Double> mapPrices;
	private Double price;
	private long timeShiftMS;

	public AgentForm() {
		super();
		this.power = 0.0;
		this.powerMin = 0.0;
		this.powerMax = 0.0;
		this.disabledPower = 0.0;
		this.duration = 0.0;
		this.offersTotal =  0.0;
		this.ongoingContractsTotal =  new PowerSlot();
		this.ongoingContractsTotalLocal =  new PowerSlot();
		this.delayToleranceMinutes = 0.0;
		this.availablePower =  0.0;
		this.missingPower =  0.0;
		this.availablePower =  0.0;
		this.missingPower =  0.0;
		this.waitingContractsPower =  new PowerSlot();
		this.hasExpired = false;
		this.isDisabled = false;
		this.isInSpace = false;
		//this.mapPrices = new HashMap<>();
		this.price = 0.0;
		this.environmentalImpact = 0;
		ongoingContractsRepartition = new HashMap<>();
		waitingContractsConsumers = new ArrayList<>();
		linkedAgents = new String[] {};
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public PowerSlot getOngoingContractsTotal() {
		return ongoingContractsTotal;
	}

	public void setOngoingContractsTotal(PowerSlot contractsTotal) {
		this.ongoingContractsTotal = contractsTotal;
	}

	public PowerSlot getOngoingContractsTotalLocal() {
		return ongoingContractsTotalLocal;
	}

	public void setOngoingContractsTotalLocal(PowerSlot contractsTotalLocal) {
		this.ongoingContractsTotalLocal = contractsTotalLocal;
	}

	public String getAgentType() {
		return agentType;
	}

	public void setAgentType(String agentType) {
		this.agentType = agentType;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double _power) {
		this.power = _power;
	}

	public Double getPowerMin() {
		return powerMin;
	}

	public void setPowerMin(Double powerMin) {
		this.powerMin = powerMin;
	}

	public Double getPowerMax() {
		return powerMax;
	}

	public void setPowerMax(Double powerMax) {
		this.powerMax = powerMax;
	}

	public void setPowers(Double _power, Double _powerMin, Double _powerMax) {
		this.power = _power;
		this.powerMin = _powerMin;
		this.powerMax = _powerMax;
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

	public Double getDuration() {
		return duration;
	}

	public void setDuration(Double duration) {
		this.duration = duration;
	}

	public String[] getLinkedAgents() {
		return linkedAgents;
	}

	public void setLinkedAgents(String[] linkedAgents) {
		this.linkedAgents = linkedAgents;
	}

	public Boolean getHasExpired() {
		return hasExpired;
	}

	public Map<String, PowerSlot> getOngoingContractsRepartition() {
		return ongoingContractsRepartition;
	}

	public void setOngoingContractsRepartition(Map<String, PowerSlot> ongoingContractsRepartition) {
		this.ongoingContractsRepartition = ongoingContractsRepartition;
	}

	public Double getDelayToleranceMinutes() {
		return delayToleranceMinutes;
	}

	public void setDelayToleranceMinutes(Double delayToleranceMinutes) {
		this.delayToleranceMinutes = delayToleranceMinutes;
	}

	public Double getAvailablePower() {
		return availablePower;
	}

	public void setAvailablePower(Double _availablePower) {
		this.availablePower = _availablePower;
	}

	public Double getMissingPower() {
		return missingPower;
	}

	public void setMissingPower(Double _missingPower) {
		this.missingPower = _missingPower;
	}

	public String getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(String priorityLevel) {
		this.priorityLevel = priorityLevel;
	}

	public Boolean getIsInSpace() {
		return isInSpace;
	}

	public void setIsInSpace(Boolean isInSpace) {
		this.isInSpace = isInSpace;
	}

	public Integer getEnvironmentalImpact() {
		return environmentalImpact;
	}

	public void setEnvironmentalImpact(Integer environmentalImpact) {
		this.environmentalImpact = environmentalImpact;
	}

	public String getElectricalPanel() {
		return electricalPanel;
	}

	public void setElectricalPanel(String electricalPanel) {
		this.electricalPanel = electricalPanel;
	}

	public String getSensorNumber() {
		return sensorNumber;
	}

	public void setSensorNumber(String sensorNumber) {
		this.sensorNumber = sensorNumber;
	}

	public String getDeviceLocation() {
		return deviceLocation;
	}

	public void setDeviceLocation(String deviceLocation) {
		this.deviceLocation = deviceLocation;
	}

	public PricingTable generatePricingTable() {
		PricingTable result = new PricingTable();
		/*
		Double price = null;
		Double lastPrice = null;
		Date date = null;
		Date lastDate = null;
		*/
		/*
		for(Long longDate : mapPrices.keySet()) {
			lastDate = date;
			lastPrice = price;
			price = mapPrices.get(longDate);
			date = new Date(longDate);
			if(lastDate!=null && lastPrice !=null ) {
				result.addPrice(lastDate, date, lastPrice);
			}
		}
		if(date!=null && price !=null ) {
			result.addPrice(date, this.endDate, price);
		}
		*/
		return result;
	}

	public void updateDeviceProperties(DeviceProperties deviceProperties) {
		this.deviceName = deviceProperties.getName();
		this.deviceCategory = deviceProperties.getCategory().getOptionItem();
		this.environmentalImpact = deviceProperties.getEnvironmentalImpact().getLevel();
		this.electricalPanel = deviceProperties.getElectricalPanel();
		this.sensorNumber = deviceProperties.getSensorNumber();
		this.deviceLocation = deviceProperties.getLocation();
	}

	public DeviceProperties retrieveDeviceProperties() {
		if(deviceCategory==null)  {
			SapereLogger.getInstance().error("retrieveDeviceProperties " + this.deviceName + " : deviceCategory is null");
		}
		DeviceProperties result = new DeviceProperties(this.deviceName
				, deviceCategory == null ? null : DeviceCategory.getByName(deviceCategory.getValue())
				, EnvironmentalImpact.getByLevel(environmentalImpact)
				, isProducer());
		result.setLocation(deviceLocation);
		result.setElectricalPanel(electricalPanel);
		result.setSensorNumber(sensorNumber);
		return result;
	}

	public EnergySupply getEnergySupply() {
		return new EnergySupply(this.agentName, this.location, false, this.power, this.powerMin, this.powerMax, this.beginDate, this.endDate
				, retrieveDeviceProperties()
				, generatePricingTable()
				, this.timeShiftMS
				);
	}

	public EnergyRequest getEnergyRequest() {
		if(delayToleranceMinutes==null) {
			//logger.warning("AgentForm.getEnergyRequest " + this.agentName + " : delayToleranceMinutes is null");
			delayToleranceMinutes = Double.valueOf(0);
		}
		if(delayToleranceRatio==null) {
			delayToleranceRatio = Double.valueOf(0);
		}
		if(this.delayToleranceMinutes==0 && delayToleranceRatio > 0) {
			delayToleranceMinutes = delayToleranceRatio * UtilDates.computeDurationMinutes(beginDate, endDate);
		}
		return new EnergyRequest(this.agentName, this.location, false, this.power, this.powerMin, this.powerMax, this.beginDate, this.endDate, this.delayToleranceMinutes,
				PriorityLevel.getByLabel(this.priorityLevel), retrieveDeviceProperties(),
				generatePricingTable(),
				this.timeShiftMS
				);
	}

	public PowerSlot getWaitingContractsPower() {
		return waitingContractsPower;
	}

	public void setWaitingContractsPower(PowerSlot _waitingContractsPower) {
		this.waitingContractsPower = _waitingContractsPower;
	}

	public List<String> getWaitingContractsConsumers() {
		return waitingContractsConsumers;
	}

	public void setWaitingContractsConsumers(List<String> waitingContractsConsumers) {
		this.waitingContractsConsumers = waitingContractsConsumers;
	}

	public Boolean getIsSatisfied() {
		return isSatisfied;
	}

	public void setIsSatisfied(Boolean isSatisfied) {
		this.isSatisfied = isSatisfied;
	}

	public void setHasExpired(Boolean hasExpired) {
		this.hasExpired = hasExpired;
	}

	public Double getOffersTotal() {
		return offersTotal;
	}

	public void setOffersTotal(Double offersTotal) {
		this.offersTotal = offersTotal;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	/*
	public Map<Long, Double> getMapPrices() {
		return mapPrices;
	}

	public void setMapPrices(Map<Long, Double> mapPrices) {
		this.mapPrices = mapPrices;
	}
*/
	public Map<String, Double> getOffersRepartition() {
		return offersRepartition;
	}

	public void setOffersRepartition(Map<String, Double> offersRepartition) {
		this.offersRepartition = offersRepartition;
	}

	public Boolean getIsDisabled() {
		return isDisabled;
	}

	public void setIsDisabled(Boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public Double getDisabledPower() {
		return disabledPower;
	}

	public void setDisabledPower(Double _disabledPower) {
		this.disabledPower = _disabledPower;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public OptionItem getDeviceCategory() {
		return deviceCategory;
	}

	public void setDeviceCategory(OptionItem deviceCategory) {
		this.deviceCategory = deviceCategory;
	}

	public Double getDelayToleranceRatio() {
		return delayToleranceRatio;
	}

	public void setDelayToleranceRatio(Double delayToleranceRatio) {
		this.delayToleranceRatio = delayToleranceRatio;
	}

	public int getWarningDurationSec() {
		return warningDurationSec;
	}

	public void setWarningDurationSec(int warningDurationSec) {
		this.warningDurationSec = warningDurationSec;
	}

	public boolean isProducer() {
		return  AgentType.PRODUCER.getLabel().equals(agentType);
	}

	public boolean isConsumer() {
		return  AgentType.CONSUMER.getLabel().equals(agentType);
	}

	public boolean checkInSpace() {
		if(!isInSpace) {
			return false;
		}
		return true;
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public boolean isRunning() {
		if(isDisabled==null || hasExpired==null) {
			return false;
		}
		return !hasExpired && !isDisabled;
	}

	public boolean hasDeviceCategory(String deviceCategoryValue) {
		if(deviceCategory != null) {
			return deviceCategory.equals(deviceCategory.getValue().equals(deviceCategoryValue));
		}
		return false;
	}

	public void generateSimplePricingTable(double rateKWH) {
		this.price = rateKWH;
		/*
		this.mapPrices = new HashMap<>();
		this.mapPrices.put(beginDate.getTime(), rateKWH);
		*/
		//this.mapPrices =  new PricingTable(beginDate, endDate, rateKWH);
	}

	public boolean isStartInFutur() {
		Date current = this.getCurrentDate();
		return current.before(beginDate);
	}

	public AgentForm(AgentType _agentType, String _agentName, String _deviceName, DeviceCategory _deviceCategory, EnvironmentalImpact _envImpact,
			Map<Long, Double> _mapPrices,
			Double _power, Date _beginDate, Date _enDate, long _timeShiftMS) {
		super();
		this.agentType = _agentType.getLabel();
		this.agentName = _agentName;
		this.deviceName = _deviceName;
		this.deviceCategory = _deviceCategory.getOptionItem();
		this.environmentalImpact = _envImpact.getLevel();
		this.power = _power;
		this.powerMin = _power;
		this.powerMax = _power;
		this.offersTotal = Double.valueOf(0);
		this.beginDate = _beginDate;
		this.endDate = _enDate;
		for(Double nextPrice : _mapPrices.values()) {
			this.price = nextPrice;
		}
		//this.mapPrices = _mapPrices;
		this.timeShiftMS = _timeShiftMS;
		Date current = getCurrentDate();
		this.hasExpired = !current.before(endDate);
	}

	public AgentForm(AgentType _agentType, String _agentName, String _deviceName, DeviceCategory _deviceCategory, EnvironmentalImpact _envImpact,
			Map<Long, Double> _mapPrices, Double _power, Date _beginDate, Date _enDate, PriorityLevel _priorityLevel, Double _delayToleranceMinutes, long _timeShiftMS) {
		this(_agentType, _agentName, _deviceName, _deviceCategory, _envImpact, _mapPrices, _power, _beginDate, _enDate, _timeShiftMS);
		if (AgentType.CONSUMER.getLabel().equals(this.agentType)) {
			this.priorityLevel = _priorityLevel.getLabel();
			this.delayToleranceMinutes = _delayToleranceMinutes;
			this.duration = UtilDates.computeDurationHours(_beginDate, _enDate);
			this.delayToleranceRatio = Double.valueOf(0);
			if(duration>0) {
				this.delayToleranceRatio =  Math.min(1.0, delayToleranceMinutes / duration);
			}
		}
	}

	public void init(SapereAgent agent, boolean _isInSpace) {
		this.agentName = agent.getAgentName();
		this.url = agent.getAgentName();
		this.isInSpace = _isInSpace;
		if(agent.getAuthentication()!=null) {
			this.agentType = agent.getAuthentication().getAgentType();
			this.location = agent.getAuthentication().getAgentLocation();
		}
	}

	public AgentForm(EnergyAgent agent, boolean isInSpace) {
		super();
		init(agent, isInSpace);
		this.id = agent.getId();
		EnergySupply supply = agent.getEnergySupply();
		if (supply != null) {
			DeviceProperties deviceProperties = supply.getDeviceProperties();
			this.updateDeviceProperties(deviceProperties);
		}
		this.power = (agent.hasExpired() || agent.isDisabled() || agent.isStartInFutur()) ? 0.0
				: supply.getPower();
		this.disabledPower = (agent.hasExpired() || agent.isDisabled()) ? supply.getPower()
				: Double.valueOf(0);
		this.beginDate = supply.getBeginDate();
		this.endDate = supply.getEndDate();
		this.duration = supply.getDuration();
		this.delayToleranceRatio = Double.valueOf(0);
		this.delayToleranceMinutes = Double.valueOf(0);
		this.priorityLevel = "";
		if(supply instanceof EnergyRequest) {
			EnergyRequest request = (EnergyRequest) supply;
			this.delayToleranceMinutes = request.getDelayToleranceMinutes();
			this.priorityLevel = request.getPriorityLevel().getLabel();
			this.warningDurationSec = request.getWarningDurationSec();
			//SapereLogger.getInstance().info("AgentForm constructor : warningDurationSec = " + warningDurationSec);
		}
		if(duration>0) {
			this.delayToleranceRatio =   Math.min(1.0, delayToleranceMinutes / duration);
		}
		Object[] objs = agent.getLinkedAgents().toArray();
		// String sobjs = objs.toString();
		this.linkedAgents = new String[objs.length];
		for (int idx = 0; idx < objs.length; idx++) {
			linkedAgents[idx] = "" + objs[idx];
		}
		this.ongoingContractsTotal = agent.getOngoingContractsPowerSlot(null);
		this.ongoingContractsTotalLocal = agent.getOngoingContractsPowerSlot(location);
		this.hasExpired = agent.hasExpired();
		this.ongoingContractsRepartition = agent.getOngoingContractsRepartition();
		this.offersTotal = agent.getOffersTotal();
		this.offersRepartition = agent.getOffersRepartition();
		this.availablePower = agent.computeAvailablePower();
		this.missingPower = agent.computeMissingPower();
		this.isDisabled = agent.isDisabled();
		this.isSatisfied = false;
		// Waiting contracts
		this.waitingContractsPower = agent.getWaitingContratsPowerSlot();
		this.waitingContractsConsumers = agent.getConsumersOfWaitingContrats();
		this.isSatisfied = agent.isSatisfied();
	}


	/**
	 * AgentForm comparator
	 *
	 * @param other
	 * @return
	 */
	public int compareTo(AgentForm other) {
		int compareType = this.agentType.compareTo(other.getAgentType());
		if (compareType == 0) {
			return this.id - other.getId();
		} else {
			return compareType;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof AgentForm) {
			boolean result;
			try {
				AgentForm agentForm = (AgentForm) obj;
				result = ((this.power==null)? (agentForm.getPower()==null) : this.power.floatValue()==agentForm.getPower().floatValue());
				result = result && ((this.powerMin==null)? (agentForm.getPowerMin()==null) : this.powerMin.floatValue()==agentForm.getPowerMin().floatValue());
				result = result && ((this.powerMax==null)? (agentForm.getPowerMax()==null) : this.powerMax.floatValue()==agentForm.getPowerMax().floatValue());
				result = result && ((this.agentName==null)? (agentForm.getAgentName()==null) : this.agentName.equals(agentForm.getAgentName()));
				result = result && (this.id == agentForm.getId());
				result = result && ((this.beginDate==null)? (agentForm.getBeginDate()==null) : this.beginDate.equals(agentForm.getBeginDate()));
				result = result && ((this.endDate==null)? (agentForm.getEndDate()==null) : this.endDate.equals(agentForm.getEndDate()));
				result = result && ((this.duration==null)? (agentForm.getDuration()==null) : this.duration.floatValue() == agentForm.getDuration().floatValue());
				result = result && ((this.url==null)? (agentForm.getUrl()==null) : this.url.equals(agentForm.getUrl()));
				result = result && (this.hasExpired == agentForm.getHasExpired());
				result = result && (this.isDisabled == agentForm.getIsDisabled());
				result = result && (this.isSatisfied == agentForm.getIsSatisfied());
				result = result && (this.isInSpace == agentForm.getIsInSpace());
				result = result && ((this.agentType==null)? (agentForm.getAgentType()==null) : this.agentType.equals(agentForm.getAgentType()));
				result = result && ((this.deviceName==null)? (agentForm.getDeviceName()==null) :this.deviceName.equals(agentForm.getDeviceName()));
				result = result && ((this.deviceCategory==null)? (agentForm.getDeviceCategory()==null) :this.deviceCategory.equals(agentForm.getDeviceCategory()));
				result = result	&& ((this.availablePower==null)? (agentForm.getAvailablePower()==null) : this.availablePower.floatValue() == agentForm.getAvailablePower().floatValue());
				result = result	&& (this.disabledPower.floatValue() == agentForm.getDisabledPower().floatValue());
				result = result	&& (this.delayToleranceMinutes.floatValue() == agentForm.getDelayToleranceMinutes().floatValue());
				result = result	&& ((this.priorityLevel==null)? (agentForm.getPriorityLevel()==null) : this.priorityLevel.equals(agentForm.getPriorityLevel()));
				result = result	&& (this.delayToleranceMinutes.floatValue() == agentForm.getDelayToleranceMinutes().floatValue());
				result = result	&& (this.ongoingContractsTotal==null) ? (agentForm.getOngoingContractsTotal()==null) : this.ongoingContractsTotal.equals(agentForm.getOngoingContractsTotal());
				result = result && ((this.offersRepartition==null)? (agentForm.getOffersRepartition()==null) : this.offersRepartition.equals(agentForm.getOffersRepartition()));
				result = result	&& ((this.ongoingContractsRepartition==null)? (agentForm.getOngoingContractsRepartition()==null) : this.ongoingContractsRepartition.equals(agentForm.getOngoingContractsRepartition()));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return result;
		}
		return false;
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}
}
