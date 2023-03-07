package com.sapereapi.model.referential;

import java.util.ArrayList;
import java.util.List;

import eu.sapere.middleware.lsa.Lsa;

public enum AgentType {
	 CONSUMER("Consumer", "Consumer")
	,PRODUCER("Producer", "Prod")
	,CONTRACT("Contract", "Contract")
	,LEARNING_AGENT("LearningAgent","Learning_agent")
	,REGULATOR("Regulator","Regulator_agent")
	,BLOOD_SEARCH("BloodSearch","BloodSearch")
	,TRANSPORT("Transport","Transport")
	,WEB_SERVICE("WebService","WebService")
	,GENERIC_SERVICE("GenericSercice","GenericSercice")
;
	private String label;
	private String preffix;

	AgentType(String _label, String _preffix) {
		this.label = _label;
		this.preffix = _preffix;
	}

	public String getLabel() {
		return label;
	}

	public String getPreffix() {
		return preffix;
	}

	public static AgentType getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (AgentType pLevel : AgentType.values()) {
			if (pLevel.getLabel().equals(label2)) {
				return pLevel;
			}
		}
		return null;
	}

	public static AgentType getFromLSA(Lsa lsa) {
		if(lsa!=null && lsa.getAgentAuthentication()!=null && lsa.getAgentAuthentication().getAgentType()!=null) {
			String sType = lsa.getAgentAuthentication().getAgentType();
			return AgentType.getByLabel(sType) ;
		}
		return null;
	}

	public static List<String> getLabels() {
		List<String> result = new ArrayList<>();
		for (AgentType pLevel : AgentType.values()) {
			result.add(pLevel.getLabel());
		}
		return result;
	}

	public static List<AgentType> getList() {
		List<AgentType> result = new ArrayList<AgentType>();
		for (AgentType pLevel : AgentType.values()) {
			result.add(pLevel);
		}
		return result;
	}
}
