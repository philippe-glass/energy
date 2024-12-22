package com.sapereapi.model.referential;

import eu.sapere.middleware.lsa.Lsa;

public enum AgentType {
	 CONSUMER("Consumer", "Consumer")
	,PRODUCER("Producer", "Prod")
	,PROSUMER("Prosumer", "Prosumer")
	,CONTRACT("Contract", "Contract")
	,LEARNING_AGENT("LearningAgent","Learning_agent")
	,REGULATOR("Regulator","Regulator_agent")
	,BLOOD_SEARCH("BloodSearch","BloodSearch")
	,TRANSPORT("Transport","Transport")
	,WEB_SERVICE("WebService","WebService")
	,GENERIC_SERVICE("GenericService","GenericService")
	,GENERIC_QUERY("GenericQuery","GenericQuery")
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

	public static AgentType getFromLSA(Lsa lsa) {
		if (lsa != null && lsa.getAgentAuthentication() != null
				&& lsa.getAgentAuthentication().getAgentType() != null) {
			String sType = lsa.getAgentAuthentication().getAgentType();
			return AgentType.valueOf(sType);
		}
		return null;
	}
}
