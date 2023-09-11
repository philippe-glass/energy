package com.sapereapi.model.prediction.input;

import com.sapereapi.model.markov.MarkovTimeWindow;

public class MatrixFilter {
	//private NodeConfig nodeLocation;
	private String nodeName;
	private String variableName;
	private Integer startHourMin;
	private Integer startHourMax;

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public Integer getStartHourMin() {
		return startHourMin;
	}

	public Integer getStartHourMax() {
		return startHourMax;
	}

	public void setStartHourMin(Integer startHourMin) {
		this.startHourMin = startHourMin;
	}

	public void setStartHourMax(Integer startHourMax) {
		this.startHourMax = startHourMax;
	}

	public boolean applyFilter(MarkovTimeWindow timeWindow) {
		boolean isOk = true;
		if(startHourMin!=null) {
			isOk = isOk && startHourMin.intValue()<=timeWindow.getStartHour();
		}
		if(isOk && startHourMax!=null) {
			isOk = isOk && startHourMax.intValue()>=timeWindow.getStartHour();
		}
		return isOk;
	}
	/*
	public boolean applyFilter(SapereAgent agent) {
		boolean isOk = true;
		if (agent instanceof ConsumerAgent) {
			ConsumerAgent consumer = (ConsumerAgent) agent;
			if (consumerDeviceCategories != null && consumerDeviceCategories.length > 0) {
				String deviceCategory = consumer.getNeed().getDeviceCategory().name();
				isOk = isOk && SapereUtil.isInStrArray(consumerDeviceCategories, deviceCategory);
			}
			if (hideExpiredAgents) {
				isOk = isOk && !consumer.hasExpired();
			}
		}
		if (agent instanceof ProducerAgent) {
			ProducerAgent producer = (ProducerAgent) agent;
			if (producerDeviceCategories != null && producerDeviceCategories.length > 0) {
				String deviceCategory = producer.getGlobalSupply().getDeviceCategory().name();
				isOk = isOk && SapereUtil.isInStrArray(producerDeviceCategories, deviceCategory);
			}
			if (hideExpiredAgents) {
				isOk = isOk && !producer.hasExpired();
			}
		}
		return isOk;
	}*/
}
