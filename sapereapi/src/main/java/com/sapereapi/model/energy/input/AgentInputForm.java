package com.sapereapi.model.energy.input;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;

public class AgentInputForm {
	private ProsumerRole prosumerRole;
	private int id;
	private String url;
	private String agentName;
	private NodeLocation location;
	private String nodeName;
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
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ssZ")
	private Date beginDate;
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ssZ")
	private Date endDate;
	private Double duration;
	private Double delayToleranceMinutes;
	private Double delayToleranceRatio;
	private PriorityLevel priorityLevel;
	private Double price;
	private long timeShiftMS;
	private String producerPolicyId = null;
	private boolean useAwardCredits = false;
	private String consumerPolicyId = null;

	public AgentInputForm() {
		super();
		this.power = 0.0;
		this.powerMin = 0.0;
		this.powerMax = 0.0;
		this.duration = 0.0;
		this.delayToleranceMinutes = 0.0;
		this.price = 0.0;
		this.environmentalImpact = null;
		this.distance = 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
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

	public String getProducerPolicyId() {
		return producerPolicyId;
	}

	public void setProducerPolicyId(String producerPolicyId) {
		this.producerPolicyId = producerPolicyId;
	}

	public String getConsumerPolicyId() {
		return consumerPolicyId;
	}

	public void setConsumerPolicyId(String consumerPolicyId) {
		this.consumerPolicyId = consumerPolicyId;
	}

	public boolean isUseAwardCredits() {
		return useAwardCredits;
	}

	public void setUseAwardCredits(boolean useAwardCredits) {
		this.useAwardCredits = useAwardCredits;
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

	public Double getDelayToleranceMinutes() {
		return delayToleranceMinutes;
	}

	public void setDelayToleranceMinutes(Double delayToleranceMinutes) {
		this.delayToleranceMinutes = delayToleranceMinutes;
	}

	public PriorityLevel getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(PriorityLevel priorityLevel) {
		this.priorityLevel = priorityLevel;
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
		 * Double price = null; Double lastPrice = null; Date date = null; Date lastDate
		 * = null;
		 */
		/*
		 * for(Long longDate : mapPrices.keySet()) { lastDate = date; lastPrice = price;
		 * price = mapPrices.get(longDate); date = new Date(longDate); if(lastDate!=null
		 * && lastPrice !=null ) { result.addPrice(lastDate, date, lastPrice); } }
		 * if(date!=null && price !=null ) { result.addPrice(date, this.endDate, price);
		 * }
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
		if (deviceCategory == null) {
			SapereLogger.getInstance()
					.error("retrieveDeviceProperties " + this.deviceName + " : deviceCategory is null");
		}
		DeviceProperties result = new DeviceProperties(this.deviceName,	deviceCategory, environmentalImpact);
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

	public EnergySupply retrieveEnergySupply() {
		ProsumerProperties issuerProperties = retrieveProsumerProperties();
		return new EnergySupply(issuerProperties, false, generatePowerSlot(), this.beginDate,
				this.endDate, generatePricingTable());
	}

	public EnergyRequest retrieveEnergyRequest() {
		if (delayToleranceMinutes == null) {
			// logger.warning("AgentForm.getEnergyRequest " + this.agentName + " :
			// delayToleranceMinutes is null");
			delayToleranceMinutes = Double.valueOf(0);
		}
		if (delayToleranceRatio == null) {
			delayToleranceRatio = Double.valueOf(0);
		}
		if (this.delayToleranceMinutes == 0 && delayToleranceRatio > 0) {
			delayToleranceMinutes = delayToleranceRatio * UtilDates.computeDurationMinutes(beginDate, endDate);
		}
		ProsumerProperties issuerProperties = retrieveProsumerProperties();
		return new EnergyRequest(issuerProperties, false, generatePowerSlot(),
				this.beginDate, this.endDate, this.delayToleranceMinutes, this.priorityLevel);
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	/*
	 * public Map<Long, Double> getMapPrices() { return mapPrices; }
	 *
	 * public void setMapPrices(Map<Long, Double> mapPrices) { this.mapPrices =
	 * mapPrices; }
	 */
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

	public boolean isProducer() {
		return ProsumerRole.PRODUCER.equals(prosumerRole);
	}

	public boolean isConsumer() {
		return ProsumerRole.CONSUMER.equals(prosumerRole);
	}

	public long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public boolean hasDeviceCategory(DeviceCategory deviceCategoryValue) {
		if (deviceCategory != null) {
			return deviceCategory.equals(deviceCategoryValue);
		}
		return false;
	}

	public void generateSimplePricingTable(double rateKWH) {
		this.price = rateKWH;
		/*
		 * this.mapPrices = new HashMap<>(); this.mapPrices.put(beginDate.getTime(),
		 * rateKWH);
		 */
		// this.mapPrices = new PricingTable(beginDate, endDate, rateKWH);
	}

	public boolean isStartInFutur() {
		Date current = this.generateCurrentDate();
		return current.before(beginDate);
	}

	private AgentInputForm(ProsumerRole _prosumerRole, String _agentName, String _deviceName, DeviceCategory _deviceCategory,
			EnvironmentalImpact _envImpact, Map<Long, Double> _mapPrices, Double _power, Date _beginDate, Date _enDate,
			long _timeShiftMS) {
		super();
		this.prosumerRole = _prosumerRole;
		this.agentName = _agentName;
		this.deviceName = _deviceName;
		this.deviceCategory = _deviceCategory;
		this.environmentalImpact = _envImpact;
		this.power = _power;
		this.powerMin = _power;
		this.powerMax = _power;
		this.beginDate = _beginDate;
		this.endDate = _enDate;
		for (Double nextPrice : _mapPrices.values()) {
			this.price = nextPrice;
		}
		// this.mapPrices = _mapPrices;
		this.timeShiftMS = _timeShiftMS;
		//Date current = generateCurrentDate();
	}

	public AgentInputForm(ProsumerRole _prosumerRole, String _agentName, String deviceName, DeviceCategory deviceCategory,
			EnvironmentalImpact _envImpact, Map<Long, Double> mapPrices, Double _power, Date _beginDate, Date _enDate,
			PriorityLevel priorityLevel, Double delayToleranceMinutes, long timeShiftMS) {
		this(_prosumerRole, _agentName, deviceName, deviceCategory, _envImpact, mapPrices, _power, _beginDate, _enDate,
				timeShiftMS);
		this.priorityLevel = priorityLevel;
		this.delayToleranceMinutes = delayToleranceMinutes;
		this.duration = UtilDates.computeDurationHours(_beginDate, _enDate);
		this.delayToleranceRatio = Double.valueOf(0);
		if (duration > 0) {
			this.delayToleranceRatio = Math.min(1.0, delayToleranceMinutes / duration);
		}
	}

	/**
	 * AgentForm comparator
	 *
	 * @param other
	 * @return
	 */
	public int compareTo(AgentInputForm other) {
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
		if (obj instanceof AgentInputForm) {
			boolean result;
			try {
				AgentInputForm agentForm = (AgentInputForm) obj;
				result = ((this.power == null) ? (agentForm.getPower() == null)
						: this.power.floatValue() == agentForm.getPower().floatValue());
				result = result && ((this.powerMin == null) ? (agentForm.getPowerMin() == null)
						: this.powerMin.floatValue() == agentForm.getPowerMin().floatValue());
				result = result && ((this.powerMax == null) ? (agentForm.getPowerMax() == null)
						: this.powerMax.floatValue() == agentForm.getPowerMax().floatValue());
				result = result && ((this.agentName == null) ? (agentForm.getAgentName() == null)
						: this.agentName.equals(agentForm.getAgentName()));
				result = result && (this.id == agentForm.getId());
				result = result && ((this.beginDate == null) ? (agentForm.getBeginDate() == null)
						: this.beginDate.equals(agentForm.getBeginDate()));
				result = result && ((this.endDate == null) ? (agentForm.getEndDate() == null)
						: this.endDate.equals(agentForm.getEndDate()));
				result = result && ((this.duration == null) ? (agentForm.getDuration() == null)
						: this.duration.floatValue() == agentForm.getDuration().floatValue());
				result = result
						&& ((this.url == null) ? (agentForm.getUrl() == null) : this.url.equals(agentForm.getUrl()));
				result = result && ((this.prosumerRole == null) ? (agentForm.getProsumerRole() == null)
						: this.prosumerRole.equals(agentForm.getProsumerRole()));
				result = result && ((this.deviceName == null) ? (agentForm.getDeviceName() == null)
						: this.deviceName.equals(agentForm.getDeviceName()));
				result = result && ((this.deviceCategory == null) ? (agentForm.getDeviceCategory() == null)
						: this.deviceCategory.equals(agentForm.getDeviceCategory()));
				result = result && (this.delayToleranceMinutes.floatValue() == agentForm.getDelayToleranceMinutes()
						.floatValue());
				result = result && ((this.priorityLevel == null) ? (agentForm.getPriorityLevel() == null)
						: this.priorityLevel.equals(agentForm.getPriorityLevel()));
				result = result && (this.delayToleranceMinutes.floatValue() == agentForm.getDelayToleranceMinutes()
						.floatValue());
			} catch (Exception e) {
				SapereLogger.getInstance().error(e);
				//e.printStackTrace();
				return false;
			}
			return result;
		}
		return false;
	}

	public Date generateCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}

	public boolean checkContent() throws HandlingException {
		if(environmentalImpact  == null) {
			throw new HandlingException("EnvironmentalImpact is not set");
		}
		if(deviceCategory == null) {
			throw new HandlingException("device category is not set");
		}
		if (prosumerRole == null) {
			throw new HandlingException("agent type is not set");
		}
		if(ProsumerRole.CONSUMER.equals(prosumerRole) && delayToleranceMinutes  <= 0.001 && delayToleranceRatio <= 0.0) {
			throw new HandlingException("delayToleranceMinutes and delayToleranceRatio are not set");
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (id > 0) {
			result.append("id:").append(id);
		}
		if (agentName != null && agentName.length() > 0) {
			result.append(", agentName:").append(agentName);
		}
		if (prosumerRole != null) {
			result.append(", prosumerRole:").append(prosumerRole);
		}
		result.append(", deviceName:").append(deviceName);
		result.append(", deviceCategory:").append(deviceCategory);
		result.append(", power:").append(power);
		if (powerMin != null && powerMin > 0) {
			result.append(", powerMin:").append(powerMin);
		}
		if (powerMax != null && powerMax > power) {
			result.append(", powerMax:").append(powerMax);
		}
		if (beginDate != null) {
			result.append(", beginDate:").append(UtilDates.format_date_time.format(beginDate));
		}
		if (endDate != null) {
			result.append(", endDate:").append(UtilDates.format_date_time.format(endDate));
		}
		if (priorityLevel != null) {
			result.append(", priorityLevel:").append(priorityLevel);
		}
		if (delayToleranceMinutes != null) {
			result.append(", delayToleranceMinutes:").append(delayToleranceMinutes);
		}
		return result.toString();
	}

}
