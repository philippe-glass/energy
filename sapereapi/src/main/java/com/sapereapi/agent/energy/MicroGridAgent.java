package com.sapereapi.agent.energy;

import java.util.Map;
import java.util.Random;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.log.SapereLogger;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.node.NodeConfig;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

public abstract class MicroGridAgent extends SapereAgent {
	private static final long serialVersionUID = 77271L;
	protected static SapereLogger logger = SapereLogger.getInstance();
	protected Random rand = new Random();

	public MicroGridAgent(String name, AgentAuthentication authentication, String[] _lsaInputTags,
			String[] _lsaOutputTags) {
		super(name, authentication, _lsaInputTags, _lsaOutputTags, LsaType.Service, false);
		initSGAgent(name, authentication, _lsaInputTags, _lsaOutputTags);
	}

	protected void initSGAgent(String name, AgentAuthentication _authentication, String[] _lsaInputTags,
			String[] _lsaOutputTags) {
		this.agentName = name;
		this.authentication = _authentication;
		super.setInput(_lsaInputTags);
		super.setOutput(_lsaOutputTags);
		setUrl(authentication.getNodeLocation().getMainServiceAddress());
		setEpsilon(0); // No greedy policy
		logger.info(this.agentName + " : lsa = " + lsa.toVisualString());
	}

	@Override
	public void onRewardEvent(RewardEvent event) {
		logger.info("onRewardEvent " + this.agentName);
	}

	@Override
	public void onLsaUpdatedEvent(LsaUpdatedEvent event) {
		logger.info("onLsaUpdatedEvent:" + agentName);
	}

	@Override
	public void setInitialLSA() {
		// TODO Auto-generated method stub
	}

	public Lsa completeContent(Lsa bondedLsa) {
		if(!NodeManager.isLocal(bondedLsa.getAgentAuthentication().getNodeLocation())) {
			for(NodeConfig nodeConfig : bondedLsa.retrieveInvolvedLocations()) {
				EnergyDbHelper.retrieveNodeConfigByName(nodeConfig.getName());
			}
			Map<String, NodeConfig> mapNodeLocation = EnergyDbHelper.getCashNodeConfig2();
			bondedLsa.completeContent(mapNodeLocation);
		}
		return bondedLsa;
	}
}
