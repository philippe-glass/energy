package com.sapereapi.agent.energy;

import java.util.Date;
import java.util.Random;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.NodeContext;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.LsaType;

public abstract class SupervisionAgent extends SapereAgent {
	private static final long serialVersionUID = 77271L;
	protected static SapereLogger logger = SapereLogger.getInstance();
	protected NodeContext nodeContext = null;
	/*
	protected long timeShiftMS = 0;
	protected String scenario = "";
	protected static Double maxTotalPower = null;
		protected Map<Integer, Integer> datetimeShifts = null;

	*/
	protected boolean stopped = false;
	protected Date beginDate = null;
	protected Random rand = new Random();

	public SupervisionAgent(String name, AgentAuthentication authentication, String[] _lsaInputTags,
			String[] _lsaOutputTags, NodeContext _nodeContext) {
		super(name, authentication, _lsaInputTags, _lsaOutputTags, LsaType.Service);
		super.setInput(_lsaInputTags);
		super.setOutput(_lsaOutputTags);
		setUrl(authentication.getAgentLocation());
		setEpsilon(0); // No greedy policy
		this.agentName = name;
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
