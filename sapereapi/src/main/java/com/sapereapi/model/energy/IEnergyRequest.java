package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.model.referential.PriorityLevel;

public interface IEnergyRequest extends IEnergyFlow {
	public Long getEventId();

	public PriorityLevel getPriorityLevel();

	public Double getDelayToleranceMinutes();

	public Date getMaxBeginDate();

	public double getAwardsCredit();

	public boolean canBeSupplied();

	public boolean isOK(SingleOffer singleOffer);

	public IEnergySupply generateSupply();

	public IEnergyRequest clone();

	public IEnergyRequest generateComplementaryRequest(double powerToSet);
}
