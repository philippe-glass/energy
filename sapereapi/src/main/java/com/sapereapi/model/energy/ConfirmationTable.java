package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.lsa.IPropertyObject;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeConfig;

public class ConfirmationTable implements Serializable , IPropertyObject {
	private static final long serialVersionUID = 3L;
	private String issuer = null;
	//protected Boolean isComplementary = Boolean.FALSE;
	private Map<String, Map<Boolean, ConfirmationItem>> table = new HashMap<>();

	public ConfirmationTable(String _issuer
			//, Boolean _isComplementary
			) {
		super();
		this.issuer = _issuer;
		this.table = new HashMap<String, Map<Boolean, ConfirmationItem>>();
		;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String _issuer) {
		this.issuer = _issuer;
	}

	public void removeConfirmation(String receiver, boolean isComplementary) {
		if (table.containsKey(receiver)) {
			Map<Boolean, ConfirmationItem> mapConfirmationItems =  table.get(receiver);
			Boolean bIsComplementary = Boolean.valueOf(isComplementary);
			if(mapConfirmationItems.containsKey(bIsComplementary)) {
				mapConfirmationItems.remove(bIsComplementary);
			}
			if(mapConfirmationItems.size() == 0) {
				this.table.remove(receiver);
			}
		}
	}

	public void confirm(String receiver, boolean isComplementary, Boolean value, String comment, long timeShiftMS) {
		putConfirmationItem(receiver, new ConfirmationItem(receiver, isComplementary, value, comment, timeShiftMS));
	}

	public ConfirmationItem getConfirmationItem(String receiver, boolean isComplementary) {
		if (table.containsKey(receiver)) {
			Map<Boolean, ConfirmationItem> mapConfirmationItems =  table.get(receiver);
			Boolean bIsComplementary = Boolean.valueOf(isComplementary);
			if(mapConfirmationItems.containsKey(bIsComplementary)) {
				return mapConfirmationItems.get(bIsComplementary);
			}
		}
		return null;
	}

	public void putConfirmationItem(String receiver, ConfirmationItem item) {
		if(!table.containsKey(receiver)) {
			table.put(receiver, new Hashtable<Boolean, ConfirmationItem>());
		}
		Map<Boolean, ConfirmationItem> mapConfirmationItems =  table.get(receiver);
		Boolean isComplementary = Boolean.valueOf(item.isComplementary());
		mapConfirmationItems.put(isComplementary, item);
	}

	public ConfirmationItem getExpiredItem() {
		for(String receiver : table.keySet()) {
			Map<Boolean, ConfirmationItem> mapConfirmationItems =  table.get(receiver);
			for(Boolean bIsComplentary : mapConfirmationItems.keySet()) {
				ConfirmationItem item = mapConfirmationItems.get(bIsComplentary);
				if (item.hasExpired()) {
					return item;
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
			this.removeConfirmation(item.getReceiver(), item.isComplementary());
		}
	}

	public boolean hasIssuer(String agentName) {
		return issuer!=null && this.issuer.equals(agentName);
	}

	public boolean hasReceiver(String agentName) {
		return table.containsKey(agentName);
	}

	@Override
	public String toString() {
		return issuer + "   " + table.toString();
	}

	@Override
	public ConfirmationTable copyForLSA() {
		ConfirmationTable result = new ConfirmationTable(this.issuer);
		for(String receiver : table.keySet()) {
			Map<Boolean, ConfirmationItem> mapConfirmationItems =  table.get(receiver);
			for(Boolean bIsComplentary : mapConfirmationItems.keySet()) {
				ConfirmationItem item = mapConfirmationItems.get(bIsComplentary);
				result.putConfirmationItem(receiver, item.clone());
			}
		}
		return result;
	}

	@Override
	public ConfirmationTable clone() {
		return copyForLSA();
	}

	@Override
	public void completeContent(Lsa bondedLsa, Map<String, NodeConfig> mapNodeLocation) {
	}

	@Override
	public List<NodeConfig> retrieveInvolvedLocations() {
		return new ArrayList<NodeConfig>();
	}
}
