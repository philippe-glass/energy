package com.sapereapi.model;

import java.io.Serializable;

import com.sapereapi.model.energy.StorageType;

public class EnergyStorageSetting implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Boolean activateStorage = Boolean.FALSE;
	private Boolean activateConsumption = Boolean.FALSE;
	private StorageType storageType = null;
	private double storageCapacityWH = 0.0;
	private double initalSavedWH = 0.0;

	public Boolean getActivateStorage() {
		return activateStorage;
	}

	public void setActivateStorage(Boolean activateStorage) {
		this.activateStorage = activateStorage;
	}

	public Boolean getActivateConsumption() {
		return activateConsumption;
	}

	public void setActivateConsumption(Boolean activateConsumption) {
		this.activateConsumption = activateConsumption;
	}

	public StorageType getStorageType() {
		return storageType;
	}

	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}

	public double getStorageCapacityWH() {
		return storageCapacityWH;
	}

	public void setStorageCapacityWH(double storageCapacity) {
		this.storageCapacityWH = storageCapacity;
	}

	public boolean canSaveEnergy() {
		return activateStorage && storageCapacityWH > 0.0;
	}

	public double getInitalSavedWH() {
		return initalSavedWH;
	}

	public void setInitalSavedWH(double initalSavedWH) {
		this.initalSavedWH = initalSavedWH;
	}

	public EnergyStorageSetting() {
		super();
		this.storageType = StorageType.PRIVATE;	// storage type by default
	}

	public EnergyStorageSetting(Boolean activateStorage, Boolean activateConsumption, StorageType storageType, double storageCapacityWH, double initalSavedWH) {
		super();
		this.activateStorage = activateStorage;
		this.activateConsumption = activateConsumption;
		this.storageType = storageType;
		this.storageCapacityWH = storageCapacityWH;
		this.initalSavedWH = initalSavedWH;
	}

	public boolean isPrivate() {
		return StorageType.PRIVATE.equals(storageType);
	}

	public boolean isCommon() {
		return StorageType.COMMON.equals(storageType);
	}

	public EnergyStorageSetting clone() {
		return new EnergyStorageSetting(activateStorage, activateConsumption, storageType, storageCapacityWH, initalSavedWH);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("activateStorage:").append(activateStorage).append(",activateConsumption:")
				.append(activateConsumption).append(":").append(storageType)
				.append(",capacityWH").append(storageCapacityWH)
				;
		return result.toString();
	}

}
