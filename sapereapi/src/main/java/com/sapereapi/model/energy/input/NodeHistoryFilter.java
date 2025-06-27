package com.sapereapi.model.energy.input;

public class NodeHistoryFilter {
	private String agentName;
	private int processingTimeToleranceSec;

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public int getProcessingTimeToleranceSec() {
		return processingTimeToleranceSec;
	}

	public void setProcessingTimeToleranceSec(int processingTimeToleranceSec) {
		this.processingTimeToleranceSec = processingTimeToleranceSec;
	}


}
