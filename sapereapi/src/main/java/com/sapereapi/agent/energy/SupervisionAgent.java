package com.sapereapi.agent.energy;

import java.util.Date;

import com.sapereapi.model.NodeContext;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.notifier.event.SpreadingEvent;

public abstract class SupervisionAgent extends MicroGridAgent {
	private static final long serialVersionUID = 77271L;
	protected boolean stopped = false;
	protected Date beginDate = null;

	public SupervisionAgent(String name, AgentAuthentication authentication, String[] lsaInputTags,
			String[] lsaOutputTags, NodeContext nodeContext) {
		super(name, authentication, lsaInputTags, lsaOutputTags, nodeContext);
		this.beginDate = getCurrentDate();
		logger.info(this.agentName + " : lsa = " + lsa.toVisualString());
	}

	@Override
	public void onSpreadingEvent(SpreadingEvent event) {
		boolean debugTimeElapsed = false;
		if(debugTimeElapsed) {
			Lsa eventLsa = event.getLsa();
			long timeSpent = eventLsa.getTimeElapsedSinceSendingMS();
			logger.info("onSpreadingEvent : " + eventLsa.getAgentName() + " time since sendingTime (MS) = " + timeSpent);
			//logger.info("onSpreadingEvent timeSpent = " + event.getTimeSpentMS() + " " + event.getRequiringAgent());
		}
	}

	public void stopAgent() {
		stopped = true;
	}

	public Date getCurrentDate() {
		return nodeContext.getCurrentDate();
	}
}
