package com.sapereapi.model.energy.award;

import java.io.Serializable;
import java.util.Date;
import java.util.TreeMap;

import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class AwardItem implements Serializable {

	private static final long serialVersionUID = 1L;
	private String agentName;
	private Date calculationDate = null;
	private double scoreSupplies;
	private double scoreConsumptions;
	private double scoreEquity;
	private TreeMap<Date, Double> usages = new TreeMap<Date, Double>();

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public Date getCalculationDate() {
		return calculationDate;
	}

	public void setCalculationDate(Date calculationDate) {
		this.calculationDate = calculationDate;
	}

	public double getScoreSupplies() {
		return scoreSupplies;
	}

	public void setScoreSupplies(double scoreSupplies) {
		this.scoreSupplies = scoreSupplies;
	}

	public double getScoreConsumptions() {
		return scoreConsumptions;
	}

	public void setScoreConsumptions(double scoreConsumptions) {
		this.scoreConsumptions = scoreConsumptions;
	}

	public double getScoreEquity() {
		return scoreEquity;
	}

	public void setScoreEquity(double scoreEquity) {
		this.scoreEquity = scoreEquity;
	}

	public double getTotal() {
		return scoreSupplies + scoreConsumptions + scoreEquity;
	}

	public double computeBalance() {
		double balance = getTotal();
		for(double nextUsage : usages.values()) {
			balance-= nextUsage;
		}
		return balance;
	}

	public void add(AwardItem toAdd) {
		if (agentName != null && agentName.equals(toAdd.getAgentName())) {
			this.calculationDate = toAdd.getCalculationDate();
			this.scoreSupplies += toAdd.getScoreSupplies();
			this.scoreConsumptions += toAdd.getScoreConsumptions();
			this.scoreEquity += toAdd.getScoreEquity();
		}
	}

	public void addUsage(Date aDate, double usedCredit) {
		if(Math.abs(usedCredit) > 0) {
			this.usages.put(aDate, usedCredit);
		}
	}

	public void reset() {
		this.calculationDate = null;
		this.scoreSupplies = 0;
		this.scoreConsumptions = 0;
		this.scoreEquity = 0;
		this.usages.clear();
	}

	public AwardItem(String agentName, Date calculationDate, double scoreSupplies, double scoreConsumptionsr,
			double scoreEquity) {
		super();
		this.agentName = agentName;
		this.calculationDate = calculationDate;
		this.scoreSupplies = scoreSupplies;
		this.scoreConsumptions = scoreConsumptionsr;
		this.scoreEquity = scoreEquity;
		this.usages = new TreeMap<Date, Double>();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("AwardItem ").append(agentName).append(" : {");
		if (Math.abs(scoreSupplies) > 0) {
			result.append("supplies = " + SapereUtil.roundPower(scoreSupplies));
		}
		if (Math.abs(scoreConsumptions) > 0) {
			result.append(", consumptions = {" + SapereUtil.roundPower(scoreConsumptions));
		}
		if (Math.abs(scoreEquity) > 0) {
			result.append(", equity = {" + SapereUtil.roundPower(scoreEquity));
		}
		if (this.usages.size() > 0) {
			result.append(", usage :");
			String sep = "";
			for(Date usageDate : usages.keySet()) {
				String sDate = UtilDates.format_time.format(usageDate);
				double power = SapereUtil.roundPower(usages.get(usageDate));
				result.append(sep).append(power ).append(" at ").append(sDate);
				sep = ", ";
			}
			result.append(", balance :").append(SapereUtil.roundPower(computeBalance()));
		}
		result.append("}");
		return result.toString();
	}

	public AwardItem clone() {
		AwardItem copy = new AwardItem(agentName, calculationDate, scoreSupplies, scoreConsumptions, scoreEquity);
		return copy;
	}
}
