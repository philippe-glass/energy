package com.sapereapi.model.energy;

import com.sapereapi.model.HandlingException;

public interface IEnergyFlow extends IEnergyObject, Cloneable {
	public boolean isStartInFutur();

	public long getTotalDurationMS();

	public IEnergyFlow copy(boolean addIds);

	public boolean getDisabled();

	public int getTimeLeftSec(boolean addWaitingBeforeStart);

	public void checkDates() throws HandlingException;

}
