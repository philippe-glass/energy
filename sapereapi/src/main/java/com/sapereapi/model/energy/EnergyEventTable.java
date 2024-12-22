package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.util.SapereUtil;

public class EnergyEventTable implements Serializable {
	private static final long serialVersionUID = 16683438676L;
	public final static String EVT_KEY_AGENT = "agent";
	public final static String EVT_KEY_MAIN_CONTRACT = "main_ctr";
	public final static String EVT_KEY_COMPLEMENTARY_CONTRACT = "second_ctr";
	Map<String, EnergyEvent> mapEvents = null;

	public EnergyEventTable() {
		super();
		mapEvents = new HashMap<String, EnergyEvent>();
	}

	public EnergyEvent getEvent(String key) {
		if (mapEvents.containsKey(key)) {
			return mapEvents.get(key);
		}
		return null;
	}

	public void reset() {
		mapEvents.clear();
	}

	public void putEvent(EnergyEvent event) throws HandlingException {
		String key = event.getKey();
		if (mapEvents.containsKey(key)) {
			EnergyEvent eventIn = mapEvents.get(key);
			if(eventIn.getKey().equals(event.getKey())) {
				// Same event already posted : nothing to do
			} else {
				// Another event already posted
				throw new HandlingException("putEvent : cannot put the event " + event + " in LSA properties. Another event " +  SapereUtil.addDoubleQuote(key) + " is already in put on the same key : " + eventIn);
			}
		}
		mapEvents.put(key, event);
	}

	public Collection<EnergyEvent> getEvents() {
		return mapEvents.values();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (String key : mapEvents.keySet()) {
			EnergyEvent event = mapEvents.get(key);
			result.append("").append(" : ").append(event);
		}
		return result.toString();
	}

}
