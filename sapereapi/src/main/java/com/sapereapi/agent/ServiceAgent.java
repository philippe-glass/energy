package com.sapereapi.agent;

import java.util.Random;

import com.sapereapi.log.SapereLogger;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

public class ServiceAgent extends SapereAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 14987L;
	Random rand = new Random();
	private String[] output;
	private String[] input;
	private static SapereLogger logger = SapereLogger.getInstance();

	public ServiceAgent(AgentAuthentication authentication, String[] input, String[] output, LsaType type, boolean activateQOS) {
		super(authentication.getAgentName(), authentication, input, output, type, activateQOS);
		this.agentName = authentication.getAgentName();
		this.output = output;
		this.input= input;
	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

	@Override
	public void onBondNotification(BondEvent event) {
		Lsa bondedLsa = event.getBondedLsa();
		String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
		logger.info("** ServiceAgent bonding ** " + agentName +" Q: "+ query);
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
		this.addBondedLSA(bondedLsa);
		
		int action = getActionToTake(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString()); //add greedy
		
		//	if (!this.hasBondedBefore(bondedLsa.getAgentName(), query)) {
			if (lsa.getSubDescription().size() == 1) { //output
				Lsa chosenLSA = getBondedLsaByQuery(query).get(rand.nextInt(getBondedLsaByQuery(query).size()));
				String state = chosenLSA.getSyntheticProperty(SyntheticPropertyName.STATE).toString() +","
						+ lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString();

				if(action == 0) {
					addState(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString(), action, 0,0);
				lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(),null, query, chosenLSA.getAgentName(), state,
						chosenLSA.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));}
					else if(action ==1){
				lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(),
						rand.nextInt(10) + "", query, chosenLSA.getAgentName(), state,
						chosenLSA.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));
					
				chosenLSA.getPropertiesByQueryAndName(query, input[0]).get(rand.nextInt(chosenLSA.getPropertiesByQueryAndName(query, input[0]).size()))
						.setChosen(true); // one Lsa can contain many property for same query
				lsa.addSyntheticProperty(SyntheticPropertyName.STATE, state);

					}
				
				this.removeBondedLsasOfQuery(query);

		}
	

		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP, "3");
		
	}

	@Override
	public void onPropagationEvent(PropagationEvent event) {
	}

	@Override
	public void onDecayedNotification(DecayedEvent event) {
	}

	@Override
	public void onLsaUpdatedEvent(LsaUpdatedEvent event) {
		logger.info("onLsaUpdatedEvent:" + agentName);
	}

	@Override
	public void onRewardEvent(RewardEvent event) {
		String previousAgent = "";
		String newState = "";
		for (Property prop : event.getLsa().getPropertiesByQuery(event.getQuery())) { 
			if (prop.getChosen()) {
				previousAgent = prop.getBond();
				newState = prop.getState();
				break;
			}
		}
		logger.info("State to reward "+newState +" by "+event.getReward() +" - "+event.getMaxSt1());
		if(!newState.equals(""))
			addState(getPreviousState(newState, this.output), 1, event.getReward(), event.getMaxSt1());
	
		logger.info("reward previous service "+previousAgent);

		Lsa lsaReward = NodeManager.instance().getSpace().getLsa(previousAgent);
		if (lsaReward != null && lsaReward.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Service)) {
				rewardLsa(lsaReward, event.getQuery(), event.getReward(),getBestActionQvalue(getPreviousState(newState, this.output))); //  maxQSt1
				lsaReward.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		}
		
		if (lsaReward != null) {
			if (previousAgent.contains("*") && !lsaReward.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Query)) {
				lsaReward.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Reward);
				lsaReward.addSyntheticProperty(SyntheticPropertyName.QUERY, event.getQuery());
				logger.info("lsaReward "+lsaReward.toVisualString());
				logger.info("send to -> "+lsaReward.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString());
				sendTo(lsaReward, lsaReward.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString());
			}
			
		}
	
	}

	public String[] getOutput() {
		return output;
	}

	public void setOutput(String[] output) {
		this.output = output;
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	
}
