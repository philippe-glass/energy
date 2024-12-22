package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;

public class AgentForm {
	private ProsumerRole prosumerRole;
	private int id;
	private String url;
	private String agentName;
	private NodeLocation location;
	private String deviceName;
	//private boolean isLocal;
	private int distance;
	private DeviceCategory deviceCategory;
	private EnvironmentalImpact environmentalImpact;
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
	private PriorityLevel priorityLevel;
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
		this.environmentalImpact = null;
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

	public ProsumerRole getProsumerRole() {
		return prosumerRole;
	}

	public void setProsumerRole(ProsumerRole prosumerRole) {
		this.prosumerRole = prosumerRole;
	}

	public NodeLocation getLocation() {
		return location;
	}

	public void setLocation(NodeLocation location) {
		this.location = location;
	}
/*
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
*/
	public boolean isLocal() {
		return distance == 0;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
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

	public PriorityLevel getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(PriorityLevel priorityLevel) {
		this.priorityLevel = priorityLevel;
	}

	public Boolean getIsInSpace() {
		return isInSpace;
	}

	public void setIsInSpace(Boolean isInSpace) {
		this.isInSpace = isInSpace;
	}

	public EnvironmentalImpact getEnvironmentalImpact() {
		return environmentalImpact;
	}

	public void setEnvironmentalImpact(EnvironmentalImpact environmentalImpact) {
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
		PricingTable result = new PricingTable(timeShiftMS);
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
		this.deviceCategory = deviceProperties.getCategory();
		this.environmentalImpact = deviceProperties.getEnvironmentalImpact();
		this.electricalPanel = deviceProperties.getElectricalPanel();
		this.sensorNumber = deviceProperties.getSensorNumber();
		this.deviceLocation = deviceProperties.getLocation();
	}

	public DeviceProperties retrieveDeviceProperties() {
		if(deviceCategory==null)  {
			SapereLogger.getInstance().error("retrieveDeviceProperties " + this.deviceName + " : deviceCategory is null");
		}
		DeviceProperties result = new DeviceProperties(this.deviceName, deviceCategory, environmentalImpact);
		result.setLocation(deviceLocation);
		result.setElectricalPanel(electricalPanel);
		result.setSensorNumber(sensorNumber);
		return result;
	}

	public ProsumerProperties retrieveProsumerProperties() {
		ProsumerProperties issuerProperties = new ProsumerProperties(agentName, location, distance, timeShiftMS, retrieveDeviceProperties());
		return issuerProperties;
	}

	public PowerSlot generatePowerSlot() {
		PowerSlot result = PowerSlot.create(power);
		if(powerMin != null && powerMin > 0) {
			result.setMin(powerMin);
		}
		if(powerMax != null && powerMax > 0) {
			result.setMax(powerMax);
		}
		return result;
	}

	public EnergySupply getEnergySupply() {
		ProsumerProperties issuerProperties = retrieveProsumerProperties();
		return new EnergySupply(issuerProperties, false, generatePowerSlot(), this.beginDate,
				this.endDate, generatePricingTable());
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
		ProsumerProperties issuerProperties = retrieveProsumerProperties();
		return new EnergyRequest(issuerProperties, false, generatePowerSlot(), this.beginDate, this.endDate, this.delayToleranceMinutes,
				this.priorityLevel);
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

	public DeviceCategory getDeviceCategory() {
		return deviceCategory;
	}

	public void setDeviceCategory(DeviceCategory deviceCategory) {
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
		return  ProsumerRole.PRODUCER.equals(prosumerRole);
	}

	public boolean isConsumer() {
		return  ProsumerRole.CONSUMER.equals(prosumerRole);
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

	public boolean hasDeviceCategory(DeviceCategory aDeviceCategoryValue) {
		if(deviceCategory != null) {
			return deviceCategory.equals(aDeviceCategoryValue);
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

	public AgentForm(ProsumerRole _prosumerRole, String _agentName, String _deviceName, DeviceCategory _deviceCategory, EnvironmentalImpact _envImpact,
			Map<Long, Double> _mapPrices,
			Double _power, Date _beginDate, Date _enDate, long _timeShiftMS) {
		super();
		this.prosumerRole = _prosumerRole;
		this.agentName = _agentName;
		this.deviceName = _deviceName;
		this.deviceCategory = _deviceCategory;
		this.environmentalImpact = _envImpact;
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


	public void init(EnergyAgent energyAgent, boolean _isInSpace) {
		this.agentName = energyAgent.getAgentName();
		this.url = energyAgent.getAgentName();
		this.isInSpace = _isInSpace;
		if(energyAgent.isProducer()) {
			this.prosumerRole = ProsumerRole.PRODUCER;
		} else if (energyAgent.isConsumer()) {
			this.prosumerRole = ProsumerRole.CONSUMER;
		}
		if(energyAgent.getAuthentication()!=null) {
			this.location = energyAgent.getAuthentication().getNodeLocation();
		}
	}

	public AgentForm(EnergyAgent agent, boolean isInSpace, int _distance) {
		super();
		init(agent, isInSpace);
		this.id = agent.getId();
		EnergyFlow supplyOrRequest = agent.getProductionOrNeed();
		if (supplyOrRequest != null) {
			DeviceProperties deviceProperties = supplyOrRequest.getIssuerProperties().getDeviceProperties();
			this.updateDeviceProperties(deviceProperties);
		}
		this.power = (agent.hasExpired() || agent.isDisabled() || agent.isStartInFutur()) ? 0.0
				: supplyOrRequest.getPower();
		this.disabledPower = (agent.hasExpired() || agent.isDisabled()) ? supplyOrRequest.getPower()
				: Double.valueOf(0);
		this.beginDate = supplyOrRequest.getBeginDate();
		this.endDate = supplyOrRequest.getEndDate();
		this.duration = supplyOrRequest.getDuration();
		this.delayToleranceRatio = Double.valueOf(0);
		this.delayToleranceMinutes = Double.valueOf(0);
		this.location = agent.getAuthentication().getNodeLocation();
		this.distance = _distance;
		this.priorityLevel = PriorityLevel.LOW;
		if(supplyOrRequest instanceof EnergyRequest) {
			EnergyRequest request = (EnergyRequest) supplyOrRequest;
			this.delayToleranceMinutes = request.getDelayToleranceMinutes();
			this.priorityLevel = request.getPriorityLevel();
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
		this.ongoingContractsTotalLocal = agent.getOngoingContractsPowerSlot(location.getMainServiceAddress());
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
		if(this.prosumerRole == null) {
			System.err.println("AgentForm constructor : prosumerRole is null");
		}
	}


	/**
	 * AgentForm comparator
	 *
	 * @param other
	 * @return
	 */
	public int compareTo(AgentForm other) {
		int compareRoles = 0;
		if(this.prosumerRole != null && other.getProsumerRole() != null) {
			compareRoles = this.prosumerRole.name().compareTo(other.getProsumerRole().name());
		}
		if (compareRoles == 0) {
			return this.id - other.getId();
		} else {
			return compareRoles;
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
				result = result && ((this.prosumerRole==null)? (agentForm.getProsumerRole()==null) : this.prosumerRole.equals(agentForm.getProsumerRole()));
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
				SapereLogger.getInstance().error(e);
				//e.printStackTrace();
				return false;
			}
			return result;
		}
		return false;
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}

	public AgentInputForm generateInputForm() {
		 Map<Long, Double> mapPrices = new HashMap<>();
		 mapPrices.put(beginDate.getTime(), this.price);
		 AgentInputForm result = new AgentInputForm();
		 if(prosumerRole != null) {
			 result.setProsumerRole(prosumerRole);
		 }
		 result.setAgentName(agentName);
		 result.setDeviceCategory(deviceCategory);
		 result.setEnvironmentalImpact(environmentalImpact);
		 result.setDeviceName(deviceName);
		 result.setPrice(price);
		 result.setPower(power);
		 result.setBeginDate(beginDate);
		 result.setEndDate(endDate);
		 result.setTimeShiftMS(timeShiftMS);
		 result.setDuration(duration);
		 result.setDelayToleranceMinutes(delayToleranceMinutes);
		 result.setDelayToleranceRatio(delayToleranceRatio);
		 result.setPriorityLevel(priorityLevel);
		 return result;
	}
}
