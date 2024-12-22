package com.sapereapi.model.energy;

import java.util.Date;

import com.sapereapi.model.energy.pricing.ComposedRate;
import com.sapereapi.model.energy.pricing.PricingTable;

public interface IEnergySupply extends IEnergyFlow {
	public Long getEventId();

	public PricingTable getPricingTable();

	public ComposedRate getRate(Date aDate);

	public IEnergyRequest generateRequest();

	public IEnergySupply clone();
}
