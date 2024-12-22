package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.node.NodeLocation;

public class SingleContribution implements Cloneable, Serializable {
	private static final long serialVersionUID = 10L;
	protected PowerSlot powerSlot = new PowerSlot();
	protected NodeLocation location;
	protected PricingTable pricingTable;
	protected List<Long> listOfferId = new ArrayList<Long>();

	public PowerSlot getPowerSlot() {
		return powerSlot;
	}

	public void setPowerSlot(PowerSlot powerSlot) {
		this.powerSlot = powerSlot;
	}

	public NodeLocation getLocation() {
		return location;
	}

	public void setLocation(NodeLocation location) {
		this.location = location;
	}

	public PricingTable getPricingTable() {
		return pricingTable;
	}

	public void setPricingTable(PricingTable pricingTable) {
		this.pricingTable = pricingTable;
	}

	public List<Long> getListOfferId() {
		return listOfferId;
	}

	public void setListOfferId(List<Long> listOfferId) {
		this.listOfferId = listOfferId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public SingleContribution(PowerSlot powerSlot, NodeLocation location, PricingTable pricingTable, List<Long> offerId) {
		super();
		this.powerSlot = powerSlot;
		this.location = location;
		this.pricingTable = pricingTable;
		this.listOfferId = offerId;
	}

	public void merge(SingleContribution otherContribution) throws HandlingException {
		if(!location.equals(otherContribution.getLocation())) {
			throw new HandlingException("SingleContribution.merge : try to merge with differnt location " + this.location + " and " + otherContribution.getLocation());
		}
		this.powerSlot.add(otherContribution.getPowerSlot());
		for(Long offerId : otherContribution.getListOfferId()) {
			if(!listOfferId.contains(offerId)) {
				this.listOfferId.add(offerId);
			}
		}
		// DO NOTHING ON PricingTable
	}

	public SingleContribution copy(boolean copyIds) {
		PricingTable clonePricingTable = pricingTable == null ? null : pricingTable.clone();
		List<Long> listOfferIds = new ArrayList<Long>();
		if(copyIds) {
			listOfferIds = SapereUtil.cloneListLong(listOfferId);
		}
		return new SingleContribution(powerSlot.clone(), location.clone(), clonePricingTable, listOfferIds);
	}

	public SingleContribution clone() {
		return copy(true);
	}

	public double getPower() {
		if (powerSlot != null) {
			return this.powerSlot.getCurrent();
		}
		return 0;
	}

	public double getPowerMin() {
		if (powerSlot != null) {
			return this.powerSlot.getMin();
		}
		return 0;
	}

	public double getPowerMax() {
		if (powerSlot != null) {
			return this.powerSlot.getMax();
		}
		return 0;
	}

	public boolean hasOffersId() {
		return listOfferId.size() > 0;
	}

	public boolean hasMainServerAddress(String serverAddress) {
		return location != null && serverAddress.equals(location.getMainServiceAddress());
	}

	public Double getPowerMargin() {
		if (powerSlot != null) {
			return powerSlot.getMargin();
		}
		return 0.0;
	}

	public boolean checkLocation() {
		return location != null;
	}

	public static Map<String, SingleContribution> cloneMapContribution(Map<String, SingleContribution> toCopy, boolean copyIds) {
		Map<String, SingleContribution> result = new HashMap<String, SingleContribution>();
		for (String supplier : toCopy.keySet()) {
			SingleContribution nextContribution = toCopy.get(supplier);
			result.put(supplier, nextContribution.clone());
			if (nextContribution.getLocation() == null) {
				SapereLogger.getInstance().error("cloneMapContribution clone location is null for supplier " + supplier);
			}
		}
		return result;
	}


	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(powerSlot);
		if(location != null) {
			result.append(" @").append(location.getName());
		}
		if (listOfferId.size() > 0) {
			result.append(", offer ids ").append(listOfferId);
		}
		return result.toString();
	}

}
