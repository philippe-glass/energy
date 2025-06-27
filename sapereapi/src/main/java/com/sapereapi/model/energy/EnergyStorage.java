package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sapereapi.agent.energy.ProsumerAgent;
import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class EnergyStorage implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	protected long timeShiftMS;
	private double totalSavedWH = 0.0;
	private String agentName = null;
	private Map<String, Map<Date, EnergyWithdrawal>> receivedDonations = new TreeMap<String, Map<Date,EnergyWithdrawal>>();
	private EnergyStorageSetting setting = new EnergyStorageSetting(false, false, StorageType.PRIVATE, 0.0, 0.0);
	private Map<Date, Double> withdrawalsForProduction = new HashMap<Date, Double>();
	private Map<Date, Double> withdrawalsForNeeds = new HashMap<Date, Double>();
	private Map<String, StorageSupply> lastReservations = new HashMap<String, StorageSupply>();

	public EnergyStorage(NodeContext nodeContext, EnergyStorageSetting setting, String agentName) {
		super();
		this.timeShiftMS = nodeContext.getTimeShiftMS();
		this.setting = setting;
		this.agentName = agentName;
		this.withdrawalsForNeeds.clear();
		this.withdrawalsForProduction.clear();
		this.receivedDonations.clear();
		this.totalSavedWH = Math.min(setting.getInitalSavedWH(), setting.getStorageCapacityWH());
	}

	public EnergyStorageSetting getSetting() {
		return setting;
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
		if (this.receivedDonations.size() > 0) {
			result.append(",Donations:{");
			for (String issuer : receivedDonations.keySet()) {
				Map<Date, EnergyWithdrawal> issuerDonations = receivedDonations.get(issuer);
				if (issuerDonations.size() > 0) {
					result.append(SapereUtil.CR).append(issuer).append(":[");
					String sep = "";
					for (Date date : issuerDonations.keySet()) {
						EnergyWithdrawal nextDonation = issuerDonations.get(date);
						result.append(sep).append(nextDonation.toStringShort());
						sep = ", ";
					}
					result.append("]");
				}
				result.append("}");
			}
		}
		result.append(", balance :").append(SapereUtil.roundPower(computeBalanceSavedWH()));
		result.append("}");
		return result.toString();
	}

	public String getAgentName() {
		return agentName;
	}

	public double getTotalSavedWH() {
		return totalSavedWH;
	}
/*
	public void setTotalSavedWH(double totalSavedWH) {
		this.totalSavedWH = totalSavedWH;
	}
*/
	public boolean canSaveEnergy() {
		return setting.canSaveEnergy();
	}

	public void compactWithdrawal() {
		if(withdrawalsForNeeds.size() +  withdrawalsForNeeds.size() > 10) {
			double balance = computeBalanceSavedWH();
			withdrawalsForNeeds.clear();
			withdrawalsForProduction.clear();
			this.totalSavedWH = balance;
		}
	}

	private double computeTotalWithdrawals() {
		double result = 0.0;
		for (Double withdrawal : withdrawalsForProduction.values()) {
			result += withdrawal;
		}
		for (Double withdrawal : withdrawalsForNeeds.values()) {
			result += withdrawal;
		}
		return result;
	}
	public double computeBalanceSavedWH() {
		if (!setting.canSaveEnergy()) {
			return 0;
		}
		return totalSavedWH - computeTotalWithdrawals();
	}

	public void addSavedWH(double toAddInWH) {
		if (setting.getActivateStorage() && setting.getStorageCapacityWH() > 0 && toAddInWH > 0) {
			double withdrawalsTotal = computeTotalWithdrawals();
			// The new balance cannot exceed the storage capacity
			double newtotalSavedWH = Math.min(setting.getStorageCapacityWH() + withdrawalsTotal,
					totalSavedWH + toAddInWH);
			if (newtotalSavedWH > totalSavedWH) {
				totalSavedWH = newtotalSavedWH;
			}
		}
	}

	public void withdrawWH(double toWindraw, ProsumerRole purpose, ProsumerAgent agent,  AbstractLogger logger) {
		StorageSupply storageSupply = agent.getStorageSupply(purpose);
		if (storageSupply == null) {
			logger.error("withdrawWH " + toWindraw + " " + purpose + " : storageSupply not found");
		}
		if (storageSupply != null) {
			Date withdrawDate = storageSupply.getBeginDate();
			if (ProsumerRole.PRODUCER.equals(purpose)) {
				if (!withdrawalsForProduction.containsKey(withdrawDate)) {
					withdrawalsForProduction.put(withdrawDate, 0.0);
				}
				Double withdrawal = withdrawalsForProduction.get(withdrawDate);
				withdrawal += toWindraw;
				withdrawalsForProduction.put(withdrawDate, withdrawal);
			}
			if (ProsumerRole.CONSUMER.equals(purpose)) {
				if (!withdrawalsForNeeds.containsKey(withdrawDate)) {
					withdrawalsForNeeds.put(withdrawDate, 0.0);
				}
				Double withdrawal = withdrawalsForNeeds.get(withdrawDate);
				withdrawal += toWindraw;
				withdrawalsForNeeds.put(withdrawDate, withdrawal);
			}
		}
	}


	public void addDonation(EnergyWithdrawal donation) {
		if(agentName.equals(donation.getRecipient())) {
			if(!this.receivedDonations.containsKey(donation.getIssuer())) {
				receivedDonations.put(donation.getIssuer(), new TreeMap<Date, EnergyWithdrawal>());
			}
			Map<Date, EnergyWithdrawal> issuerDonations = receivedDonations.get(donation.getIssuer());
			if(!issuerDonations.containsKey(donation.getDate()) ) {
				issuerDonations.put(donation.getDate(), donation);
				this.addSavedWH(donation.getEnergyWH());
			}
		}
	}

	public void addReservation(String consumer, StorageSupply storageSupply) {
		this.lastReservations.put(consumer, storageSupply);
	}

	public boolean hasReservation(String consumer) {
		return this.lastReservations.containsKey(consumer);
	}

	public void cleanReservations() {
		double reservationDurationSec = 15;
		Date current = UtilDates.getNewDateNoMilliSec(timeShiftMS);
		Date minCurrent = UtilDates.shiftDateSec(current, -1*reservationDurationSec);
		List<String> keyToRemove = new ArrayList<String>();
		for (String consumer : lastReservations.keySet()) {
			StorageSupply storageSupply = lastReservations.get(consumer);
			if (storageSupply.getBeginDate().before(minCurrent)) {
				keyToRemove.add(consumer);
			}
		}
		for (String consumer : keyToRemove) {
			lastReservations.remove(consumer);
		}
	}

	public Date getLastReservationDate() {
		Date result = null;
		for (StorageSupply nextSupply : lastReservations.values()) {
			if (result == null || result.before(nextSupply.getBeginDate())) {
				result = nextSupply.getBeginDate();
			}
		}
		return result;
	}
}
