package com.saperetest.model;

import com.sapereapi.util.UtilDates;

public class DeviceItem {
	String name;
	String node;
	boolean isProducer;
	Double power = 0.0;
	Double powerProvided = 0.0;
	Double powerConsumed = 0.0;

	public DeviceItem(String name, Double power, String node, boolean isProducer) {
		super();
		this.name = name;
		this.power = power;
		this.node = node;
		this.isProducer = isProducer;
		this.powerProvided = 0.0;
		this.powerConsumed = 0.0;
	}

	public boolean getIsProducer() {
		return isProducer;
	}

	public void setIsProducer(boolean isProducer) {
		this.isProducer = isProducer;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public void setProducer(boolean isProducer) {
		this.isProducer = isProducer;
	}

	public Double getPowerProvided() {
		return powerProvided;
	}

	public Double getPowerAvailable() {
		if (isProducer) {
			return Math.max(0, power - powerProvided);
		}
		return 0.0;
	}

	public Double getPowerMissing() {
		if (!isProducer) {
			return Math.max(0, power - powerConsumed);
		}
		return 0.0;
	}

	public boolean isSatisfied() {
		Double missing = getPowerMissing();
		return missing < 0.0001;
	}

	public void setPowerProvided(Double powerProvided) {
		this.powerProvided = powerProvided;
	}

	public Double getPowerConsumed() {
		return powerConsumed;
	}

	public void setPowerConsumed(Double powerConsumed) {
		this.powerConsumed = powerConsumed;
	}

	@Override
	public String toString() {
		return "[" + node + "]" + name + ":" + UtilDates.df3.format(power) + "W";
	}

}
