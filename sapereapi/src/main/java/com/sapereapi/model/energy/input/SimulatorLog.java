package com.sapereapi.model.energy.input;

public class SimulatorLog {
	private int loopNumber;
	private String deviceCategoryCode;
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

	public String getDeviceCategoryCode() {
		return deviceCategoryCode;
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

	public void setDeviceCategoryCode(String deviceCategoryCode) {
		this.deviceCategoryCode = deviceCategoryCode;
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

	public SimulatorLog(int loopNumber, String deviceCategoryCode, double powerTarget, double powerTargetMin,
			double powerTargetMax, double power, boolean reached, int _nbDevices) {
		super();
		this.loopNumber = loopNumber;
		this.deviceCategoryCode = deviceCategoryCode;
		this.powerTarget = powerTarget;
		this.powerTargetMin = powerTargetMin;
		this.powerTargetMax = powerTargetMax;
		this.power = power;
		this.reached = reached;
		this.nbDevices = _nbDevices;
	}

	@Override
	public String toString() {
		return "SimulatorLog [loopNumber=" + loopNumber + ", deviceCategoryCode=" + deviceCategoryCode
				+ ", powerTarget=" + powerTarget + ", powerTargetMin=" + powerTargetMin + ", powerTargetMax="
				+ powerTargetMax + ", power=" + power + ", reached=" + reached + ", nbStarted=" + nbStarted
				+ ", nbModified=" + nbModified + ", nbStopped=" + nbStopped + ", nbDevices=" + nbDevices
				+ ", targetDeviceCombinationFound=" + targetDeviceCombinationFound + "]";
	}

}
