package com.sapereapi.agent.energy;

import java.util.Date;

import com.sapereapi.model.NodeContext;

import eu.sapere.middleware.agent.AgentAuthentication;

public abstract class SupervisionAgent extends MicroGridAgent {
	private static final long serialVersionUID = 77271L;
	protected NodeContext nodeContext = null;
	/*
	protected long timeShiftMS = 0;
	protected String scenario = "";
	protected static Double maxTotalPower = null;
		protected Map<Integer, Integer> datetimeShifts = null;

	*/
	protected boolean stopped = false;
	protected Date beginDate = null;

	public SupervisionAgent(String name, AgentAuthentication authentication, String[] _lsaInputTags,
			String[] _lsaOutputTags, NodeContext _nodeContext) {
		super(name, authentication, _lsaInputTags, _lsaOutputTags);
		this.nodeContext = _nodeContext;
		this.beginDate = getCurrentDate();
		logger.info(this.agentName + " : lsa = " + lsa.toVisualString());
	}

	public void stopAgent() {
		stopped = true;
	}

	public Date getCurrentDate() {
		return nodeContext.getCurrentDate();
	}
}
