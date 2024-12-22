package com.sapereapi.model.energy.reschedule;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.referential.WarningType;

public class RescheduleTable {
	private Map<String, RescheduleItem> table = new HashMap<>();

	public RescheduleTable() {
		super();
		this.table = new HashMap<String, RescheduleItem>();
	}

	public void removeItem(String agentName) {
		this.table.remove(agentName);
	}

	public void addItem(String agentName, RescheduleItem item) {
		if(table.containsKey(agentName)) {
			RescheduleItem inItem = table.get(agentName);
			inItem.setPower(item.getPower());
			if(inItem.getStopEnd().before(item.getStopEnd())) {
				inItem.setStopEnd(item.getStopEnd());
			}
			if(inItem.getStopBegin().after(item.getStopBegin())) {
				inItem.setStopBegin(item.getStopBegin());
			}
		} else {
			this.table.put(agentName, item);
		}
	}

	public RescheduleItem getItem(String agentName) {
		if (table.containsKey(agentName)) {
			RescheduleItem item = table.get(agentName);
			return item;
		}
		return null;
	}

	public String getExpiredItemKey() {
		for (String agentName : table.keySet()) {
			RescheduleItem item = table.get(agentName);
			if (item.hasExpired()) {
				return agentName;
			}
		}
		return null;
	}

	public boolean hasExpiredItem() {
		String key = getExpiredItemKey();
		return (key != null);
	}

	public void cleanExpiredDate() {
		String key = null;
		while ((key = getExpiredItemKey()) != null) {
			table.remove(key);
		}
	}

	public boolean hasItem(String agentName) {
		return table.containsKey(agentName);
	}

	@Override
	public String toString() {
		return table.toString();
	}

	@Override
	public RescheduleTable clone() {
		RescheduleTable result = new RescheduleTable();
		for(String agentName : table.keySet()) {
			RescheduleItem item = table.get(agentName);
			result.addItem(agentName, item.clone());
		}
		return result;
	}

	public double computeRescheduledPower(Date aDate, WarningType warningType) {
		double result = 0;
		for(RescheduleItem item : table.values()) {
			if(warningType.equals(item.getWarningType())) {
				if(item.isInSlot(aDate)) {
					result+=item.getPower();
				}
			}
		}
		return result;
	}

}
