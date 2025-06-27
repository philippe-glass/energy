package com.sapereapi.model.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.referential.ProsumerRole;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.IPropertyObject;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class ConfirmationTable implements IPropertyObject {
	private static final long serialVersionUID = 3L;
	private String issuer = null;
	private long timeShiftMS = 0;
	//protected Boolean isComplementary = Boolean.FALSE;
	private Map<ProsumerRole, Map<String, Map<Boolean, ConfirmationItem>>> table = new HashMap<>();

	public ConfirmationTable(String _issuer, long timeShiftMS) {
		super();
		this.issuer = _issuer;
		this.timeShiftMS = timeShiftMS;
		this.table = new HashMap<ProsumerRole, Map<String, Map<Boolean, ConfirmationItem>>>();
		;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String _issuer) {
		this.issuer = _issuer;
	}

	public void removeConfirmation(String receiver, boolean isComplementary, ProsumerRole issuerRole) {
		if (table.containsKey(issuerRole)) {
			Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 =  table.get(issuerRole);
			Boolean bIsComplementary = Boolean.valueOf(isComplementary);
			if(mapConfirmationItems1.containsKey(receiver)) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
				if(mapConfirmationItems2.containsKey(bIsComplementary)) {
					mapConfirmationItems2.remove(bIsComplementary);
				}
				if(mapConfirmationItems2.size() == 0) {
					mapConfirmationItems1.remove(receiver);
				}
			}
			if(mapConfirmationItems1.size() == 0) {
				this.table.remove(issuerRole);
			}
		}
	}

	private Map<String, Map<Boolean, ConfirmationItem>> getConfirmationTableAs(ProsumerRole issuerRole) {
		if(table.containsKey(issuerRole)) {
			return table.get(issuerRole);
		}
		return new HashMap<String, Map<Boolean,ConfirmationItem>>();
	}

	public void confirm(String receiver, boolean isComplementary, ProsumerRole issuerRole, Boolean value, String comment, int nbOfRenewals) {
		ConfirmationItem newItem = new ConfirmationItem(this.issuer, issuerRole, receiver, isComplementary, value, comment, nbOfRenewals, timeShiftMS);
		putConfirmationItem(newItem);
	}

	public boolean hasConfirmationItem(String receiver, boolean isComplementary, ProsumerRole issuerRole) {
		if (table.containsKey(issuerRole)) {
			Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 = table.get(issuerRole);
			Boolean bIsComplementary = Boolean.valueOf(isComplementary);
			if(mapConfirmationItems1.containsKey(receiver)) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
				return mapConfirmationItems2.containsKey(bIsComplementary);
			}
		}
		return false;
	}

	public ConfirmationItem getConfirmationItem(String receiver, boolean isComplementary, ProsumerRole issuerRole) {
		if (table.containsKey(issuerRole)) {
			Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 = table.get(issuerRole);
			if(mapConfirmationItems1.containsKey(receiver)) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
				Boolean bIsComplementary = Boolean.valueOf(isComplementary);
				if(mapConfirmationItems2.containsKey(bIsComplementary)) {
					return mapConfirmationItems2.get(bIsComplementary);
				}
			}
		}
		return null;
	}

	public void putConfirmationItem(ConfirmationItem item) {
		ProsumerRole role = item.getIssuerRole();
		if(!table.containsKey(role)) {
			table.put(role, new Hashtable<String, Map<Boolean, ConfirmationItem>>());
		}
		String receiver = item.getReceiver();
		Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 = table.get(role);
		if(!mapConfirmationItems1.containsKey(receiver)) {
			mapConfirmationItems1.put(receiver, new HashMap<Boolean, ConfirmationItem>());
		}
		Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
		Boolean isComplementary = Boolean.valueOf(item.isComplementary());
		mapConfirmationItems2.put(isComplementary, item);
	}

	public ConfirmationItem getExpiredItem() {
		for(ProsumerRole issuerRole : table.keySet()) {
			Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 = table.get(issuerRole);
			for(String receiver : mapConfirmationItems1.keySet()) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
				for(Boolean isComplementary: mapConfirmationItems2.keySet()) {
					ConfirmationItem item = mapConfirmationItems2.get(isComplementary);
					if (item.hasExpired()) {
						return item;
					}
				}
			}
		}
		return null;
	}

	public boolean hasExpiredItem() {
		ConfirmationItem item = getExpiredItem();
		return (item!=null);
	}

	public void cleanExpiredDate() {
		ConfirmationItem item = null;
		while ((item = getExpiredItem()) != null) {
			this.removeConfirmation(item.getReceiver(), item.isComplementary(), item.getIssuerRole());
		}
	}

	public boolean hasIssuer(String agentName) {
		return issuer!=null && this.issuer.equals(agentName);
	}

	public boolean hasReceiver(String agentName) {
		return hasReceiver(ProsumerRole.CONSUMER, agentName) || hasReceiver(ProsumerRole.PRODUCER, agentName);
	}
	public boolean hasReceiver(ProsumerRole issuerRole, String agentName) {
		if(table.containsKey(issuerRole)) {
			Map<String, Map<Boolean, ConfirmationItem>> confirmationsAsRole =  table.get(issuerRole);
			return confirmationsAsRole.containsKey(agentName);
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (ProsumerRole issuerRole : ProsumerRole.values()) {
			Map<String, Map<Boolean, ConfirmationItem>> confirmationsAsRole = getConfirmationTableAs(issuerRole);
			String sRole = issuerRole.getLabel().toLowerCase();
			//String issuer2 = issuer.replace("Prosumer", "Prs") + " as " + sRole;
			result.append(" as " + sRole).append(":{");
			for (String receiver : confirmationsAsRole.keySet()) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems1 = confirmationsAsRole.get(receiver);
				String sep = "";
				if (mapConfirmationItems1.size() > 0) {
					String received2 = receiver.replace("Prosumer", "Prs");
					result.append(sep).append(received2).append(":");
					for (Boolean bIsComplentary : mapConfirmationItems1.keySet()) {
						if (bIsComplentary) {
							result.append("(complementary)");
						}
						ConfirmationItem item = mapConfirmationItems1.get(bIsComplentary);
						result.append(item);
					}
					sep = ",";
				}
			}
			result.append("}");
		}
		return result.toString();
	}

	public boolean renewConfirmations() {
		boolean updated = false;
		for (ProsumerRole issuerRole : table.keySet()) {
			Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 = table.get(issuerRole);
			for (String receiver : mapConfirmationItems1.keySet()) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
				for (Boolean bIsComplementary : mapConfirmationItems2.keySet()) {
					ConfirmationItem item = mapConfirmationItems2.get(bIsComplementary);
					if (item.getNbOfRenewals() > 0) {
						updated = updated || item.renewConfirmation();
					}
				}
			}
		}
		return updated;
	}

	@Override
	public ConfirmationTable copyForLSA(AbstractLogger logger) {
		ConfirmationTable result = new ConfirmationTable(this.issuer, this.timeShiftMS);
		for(ProsumerRole issuerRole : table.keySet()) {
			Map<String, Map<Boolean, ConfirmationItem>> mapConfirmationItems1 = table.get(issuerRole);
			for(String receiver : mapConfirmationItems1.keySet()) {
				Map<Boolean, ConfirmationItem> mapConfirmationItems2 = mapConfirmationItems1.get(receiver);
				for(Boolean bIsComplementary: mapConfirmationItems2.keySet()) {
					ConfirmationItem item = mapConfirmationItems2.get(bIsComplementary);
					result.putConfirmationItem(item.clone());
				}
			}
		}
		return result;
	}

	@Override
	public ConfirmationTable clone() {
		return copyForLSA(SapereLogger.getInstance());
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		return new ArrayList<NodeLocation>();
	}
}
