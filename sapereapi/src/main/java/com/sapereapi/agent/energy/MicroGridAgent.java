package com.sapereapi.agent.energy;

import java.util.Date;
import java.util.Map;
import java.util.Random;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.AggregationEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

public abstract class MicroGridAgent extends SapereAgent {
	private static final long serialVersionUID = 77271L;
	protected static SapereLogger logger = SapereLogger.getInstance();
	protected Random rand = new Random();
	protected NodeContext nodeContext;

	public MicroGridAgent(String name, AgentAuthentication authentication
			, String[] lsaInputTags
			, String[] lsaOutputTags
			, NodeContext nodeContext) {
		super(name, authentication, lsaInputTags, lsaOutputTags, LsaType.Service, false);
		initSGAgent(name, authentication, nodeContext);
	}

	protected void initSGAgent(String name
			, AgentAuthentication _authentication
			, NodeContext _nodeContext) {
		this.agentName = name;
		this.nodeContext = _nodeContext;
		setEpsilon(0); // No greedy policy
		logger.info(this.agentName + " : lsa = " + lsa.toVisualString());
	}

	public NodeContext getNodeContext() {
		return nodeContext;
	}

	public long getTimeShiftMS() {
		return nodeContext.getTimeShiftMS();
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(nodeContext.getTimeShiftMS());
	}

	@Override
	public void onRewardEvent(RewardEvent event) {
		logger.info("onRewardEvent " + this.agentName);
	}

	@Override
	public void onAggregationEvent(AggregationEvent event) {
	}

	@Override
	public void setInitialLSA() {
		// TODO Auto-generated method stub
	}

	public String getIpSource() {
		if(nodeContext == null) {
			return null;
		}
		return nodeContext.getNodeLocation().getMainServiceAddress();
	}

	public Lsa completeInvolvedLocations(Lsa bondedLsa, AbstractLogger logger) throws HandlingException {
		if(!NodeManager.isLocal(bondedLsa.getAgentAuthentication().getNodeLocation())) {
			for(NodeLocation nodeLocation : bondedLsa.retrieveInvolvedLocations()) {
				if(nodeLocation != null) {
					EnergyDbHelper.retrieveNodeLocationByName(nodeLocation.getName());
				} else {
					logger.error("MicrogridAgent.completeInvolvedLocations : next nodeLocation is null ");
				}
			}
			Map<String, NodeLocation> mapNodeLocation = EnergyDbHelper.getCashNodeLocation2();
			bondedLsa.completeInvolvedLocations(mapNodeLocation);
		}
		return bondedLsa;
	}

}
