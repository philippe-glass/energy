package com.sapereapi.model.energy.input;

import com.sapereapi.model.referential.DeviceCategory;

public class SimulatorLog {
	private int loopNumber;
	private DeviceCategory deviceCategory;
	private double powerTarget;
	private double powerTargetMin;
	private double powerTargetMax;
	private double power;
	private boolean reached;
	private int nbStarted;
	private int nbModified;
	private int nbStopped;
	private int nbDevices;
	private boolean targetDeviceCombinationFound;

	public int getLoopNumber() {
		return loopNumber;
	}

	public double getPowerTarget() {
		return powerTarget;
	}

	public double getPowerTargetMin() {
		return powerTargetMin;
	}

	public double getPowerTargetMax() {
		return powerTargetMax;
	}

	public double getPower() {
		return power;
	}

	public boolean isReached() {
		return reached;
	}

	public int getNbStarted() {
		return nbStarted;
	}

	public int getNbModified() {
		return nbModified;
	}

	public void setLoopNumber(int loopNumber) {
		this.loopNumber = loopNumber;
	}

	public void setPowerTarget(double powerTarget) {
		this.powerTarget = powerTarget;
	}

	public void setPowerTargetMin(double powerTargetMin) {
		this.powerTargetMin = powerTargetMin;
	}

	public void setPowerTargetMax(double powerTargetMax) {
		this.powerTargetMax = powerTargetMax;
	}

	public DeviceCategory getDeviceCategory() {
		return deviceCategory;
	}

	public void setDeviceCategory(DeviceCategory deviceCategory) {
		this.deviceCategory = deviceCategory;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public void setReached(boolean reached) {
		this.reached = reached;
	}

	public void setNbStarted(int nbStarted) {
		this.nbStarted = nbStarted;
	}

	public void setNbModified(int nbModified) {
		this.nbModified = nbModified;
	}

	public int getNbStopped() {
		return nbStopped;
	}

	public void setNbStopped(int nbStopped) {
		this.nbStopped = nbStopped;
	}

	public boolean isTargetDeviceCombinationFound() {
		return targetDeviceCombinationFound;
	}

	public void setTargetDeviceCombinationFound(boolean targetDeviceCombinationFound) {
		this.targetDeviceCombinationFound = targetDeviceCombinationFound;
	}

	public int getNbDevices() {
		return nbDevices;
	}

	public void setNbDevices(int nbDevices) {
		this.nbDevices = nbDevices;
	}

	public SimulatorLog(int loopNumber, DeviceCategory deviceCategory, double powerTarget, double powerTargetMin,
			double powerTargetMax, double power, boolean reached, int _nbDevices) {
		super();
		this.loopNumber = loopNumber;
		this.deviceCategory = deviceCategory;
		this.powerTarget = powerTarget;
		this.powerTargetMin = powerTargetMin;
		this.powerTargetMax = powerTargetMax;
		this.power = power;
		this.reached = reached;
		this.nbDevices = _nbDevices;
	}

	@Override
	public String toString() {
		return "SimulatorLog [loopNumber=" + loopNumber + ", deviceCategory=" + deviceCategory
				+ ", powerTarget=" + powerTarget + ", powerTargetMin=" + powerTargetMin + ", powerTargetMax="
				+ powerTargetMax + ", power=" + power + ", reached=" + reached + ", nbStarted=" + nbStarted
				+ ", nbModified=" + nbModified + ", nbStopped=" + nbStopped + ", nbDevices=" + nbDevices
				+ ", targetDeviceCombinationFound=" + targetDeviceCombinationFound + "]";
	}

}
