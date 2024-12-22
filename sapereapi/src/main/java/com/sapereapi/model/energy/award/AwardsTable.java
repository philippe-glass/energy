package com.sapereapi.model.energy.award;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sapereapi.util.UtilDates;

public class AwardsTable {
	private Map<String, AwardItem> table = new HashMap<String, AwardItem>();
	private Date expiryDate = null;
	private long timeShiftMS = 0;

	public Date getExpiryDate() {
		return expiryDate;
	}

	public Long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	public boolean hasExpired() {
		Date current = getCurrentDate();
		return current.after(expiryDate);
	}

	public AwardsTable() {
		super();
		this.table = new HashMap<String, AwardItem>();
	}

	public AwardsTable(long timeShiftMS, int expirationDelaySec) {
		super();
		this.timeShiftMS = timeShiftMS;
		Date current = getCurrentDate();
		this.expiryDate = UtilDates.shiftDateSec(current, expirationDelaySec);
		this.table = new HashMap<String, AwardItem>();
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}

	public void removeItem(String agentName) {
		this.table.remove(agentName);
	}

	public void addAward(String agentName, AwardItem item) {
		if(table.containsKey(agentName)) {
			AwardItem inItem = table.get(agentName);
			inItem.add(item);
		} else {
			this.table.put(agentName, item);
		}
	}

	public AwardItem getAward(String agentName) {
		if (table.containsKey(agentName)) {
			AwardItem item = table.get(agentName);
			return item;
		}
		return null;
	}

	public double getAwardTotal(String agentName) {
		AwardItem award = getAward(agentName);
		if (award != null) {
			return award.getTotal();
		}
		return 0.0;
	}

	public Set<String> getProsumers() {
		return table.keySet();
	}

	public void addTable(AwardsTable otherTable) {
		for(String prosumer : otherTable.getProsumers()) {
			AwardItem awardItem = otherTable.getAward(prosumer).clone();
			addAward(prosumer, awardItem);
		}
	}


	public boolean hasAgentAwardItem(String agentName) {
		return table.containsKey(agentName);
	}

	@Override
	public String toString() {
		return table.toString();
	}

	@Override
	public AwardsTable clone() {
		AwardsTable result = new AwardsTable();
		for(String agentName : table.keySet()) {
			AwardItem item = table.get(agentName);
			result.addAward(agentName, item.clone());
		}
		return result;
	}

	public Map<String, Double> getShortContent() {
		Map<String, Double> result = new HashMap<String, Double>();
		for(String agentName : table.keySet()) {
			AwardItem item = table.get(agentName);
			result.put(agentName, item.getTotal());
		}
		return result;
	}
}
