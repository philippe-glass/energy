package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.NodeContext;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class EnergyStorage implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	protected long timeShiftMS;
	private double totalSavedWH = 0.0;
	private boolean activated = false;
	private Map<Date, Double> withdrawalsForProduction = new HashMap<Date, Double>();
	private Map<Date, Double> withdrawalsForNeeds = new HashMap<Date, Double>();
	private ExtraSupply extraForProduction = null; // TODO : MOVE to globalProduction
	private ExtraSupply extraForNeed = null; // TODO : MOVE to globalNeed

	public EnergyStorage(NodeContext nodeContext) {
		super();
		this.timeShiftMS = nodeContext.getTimeShiftMS();
		this.activated = nodeContext.isEnergyStorageActivated();
		this.withdrawalsForNeeds.clear();
		this.withdrawalsForProduction.clear();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("EnergyStorage ").append("").append(" : {");
		if (Math.abs(totalSavedWH) > 0) {
			result.append("totalSavedWH = " + SapereUtil.roundPower(totalSavedWH));
		}
		if (this.withdrawalsForNeeds.size() > 0) {
			result.append(", usage for need :");
			String sep = "";
			for(Date usageDate : withdrawalsForNeeds.keySet()) {
				String sDate = UtilDates.format_time.format(usageDate);
				double power = SapereUtil.roundPower(withdrawalsForNeeds.get(usageDate));
				result.append(sep).append(power ).append(" at ").append(sDate);
				sep = ", ";
			}
		}
		if (this.withdrawalsForProduction.size() > 0) {
			result.append(", usage for production :");
			String sep = "";
			for(Date usageDate : withdrawalsForProduction.keySet()) {
				String sDate = UtilDates.format_time.format(usageDate);
				double power = SapereUtil.roundPower(withdrawalsForProduction.get(usageDate));
				result.append(sep).append(power ).append(" at ").append(sDate);
				sep = ", ";
			}
		}
		result.append(", balance :").append(SapereUtil.roundPower(computeBalanceSavedWH()));
		result.append("}");
		return result.toString();
	}

	public double getTotalSavedWH() {
		return totalSavedWH;
	}

	public boolean isActivated() {
		return activated;
	}

	public double computeBalanceSavedWH() {
		if (!activated) {
			return 0;
		}
		double result = totalSavedWH;
		for (Double withdrawal : withdrawalsForProduction.values()) {
			result -= withdrawal;
		}
		for (Double withdrawal : withdrawalsForNeeds.values()) {
			result -= withdrawal;
		}
		return result;
	}

	public void addSavedWH(double toAddInWH) {
		if (activated) {
			totalSavedWH += Math.max(0, toAddInWH);
		}
	}

	public ExtraSupply withdrawEnergy(Date aDate, Date endDate, Double toWithdrawWH, ProsumerRole purpose) {
		if (!activated) {
			return null;
		}
		double balance = computeBalanceSavedWH();
		double effectiveWithdrawalWH = Math.min(balance, toWithdrawWH);
		double durationHours = UtilDates.computeDurationHours(aDate, endDate);
		double extraSupplyPower = SapereUtil.roundPower(effectiveWithdrawalWH / durationHours);
		ExtraSupply extraSupply = new ExtraSupply(extraSupplyPower, aDate, endDate, timeShiftMS);
		if (ProsumerRole.PRODUCER.equals(purpose)) {
			withdrawalsForProduction.put(aDate, effectiveWithdrawalWH);
			extraForProduction = extraSupply;
		}
		if (ProsumerRole.CONSUMER.equals(purpose)) {
			withdrawalsForNeeds.put(aDate, effectiveWithdrawalWH);
			extraForNeed = extraSupply;
		}
		return extraSupply;
	}

	public ExtraSupply getExtraForProduction() {
		return extraForProduction;
	}

	public void setExtraForProduction(ExtraSupply extraForProduction) {
		this.extraForProduction = extraForProduction;
	}

	public ExtraSupply getExtraForNeed() {
		return extraForNeed;
	}

	public void setExtraForNeed(ExtraSupply extraForNeed) {
		this.extraForNeed = extraForNeed;
	}

}
