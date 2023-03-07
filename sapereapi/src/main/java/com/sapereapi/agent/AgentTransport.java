package com.sapereapi.agent;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class AgentTransport extends SapereAgent {

	private static final long serialVersionUID = 5920535286218204783L;

	private Map<String, Lsa> selectedPropreties;
	private String[] input;
	private String[] output;
	Random rand = new Random();
	SapereLogger logger = SapereLogger.getInstance();

	public AgentTransport(AgentAuthentication authentication, String[] input, String[] output, LsaType type) {
		super(authentication.getAgentName(), authentication, input, output, type);
		this.input = input;
		this.output = output;
		selectedPropreties = new HashMap<>();
	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

	@Override
	public void onBondNotification(BondEvent event) {
		Lsa bondedLsa = event.getBondedLsa();
		String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
		this.addBondedLSA(bondedLsa);
		int action = getActionToTake(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString()); // add
		logger.info("**" + agentName + "  -> " + bondedLsa.getAgentName() + " Q: " + query + " action:"+action);

		if (checkInputs(bondedLsa, query)) {
			// greedy
			if (lsa.getSubDescription().size() == 2) { // output
				String state = fusionStates(
						selectedPropreties.get(input[0]).getSyntheticProperty(SyntheticPropertyName.STATE).toString()
								.split(","),
						selectedPropreties.get(input[1]).getSyntheticProperty(SyntheticPropertyName.STATE).toString()
								.split(","));
				state += "," + lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString();

				if (action == 0) {
					addState(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString(), action, 0, 0);
					lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(),
							null, query, selectedPropreties.get(input[0]).getAgentName() + ","
									+ selectedPropreties.get(input[1]).getAgentName(),
							state, "", false));
					logger.info("lsa add empty proprty");
					// this.removeBondedLsasOfQuery(query);
				} else if (action == 1) {
					Property p = new Property(output[0], "1", query,
							selectedPropreties.get(input[0]).getAgentName() + "," + selectedPropreties.get(input[1]).getAgentName(), state,	
									selectedPropreties.get(input[0]).getSyntheticProperty(SyntheticPropertyName.SOURCE).toString() + ","
									+ selectedPropreties.get(input[1])	.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(),false);
					Property p2 = new Property(output[1], "drone", query,
							selectedPropreties.get(input[0]).getAgentName() + "," + selectedPropreties.get(input[1]).getAgentName(), state,	
									selectedPropreties.get(input[0]).getSyntheticProperty(SyntheticPropertyName.SOURCE).toString() + ","
									+ selectedPropreties.get(input[1])	.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(),false);
					if (!lsa.contains(p)) {
						selectedPropreties.get(input[0]).getPropertiesByQueryAndName(query, input[0]).get(0)
								.setChosen(true);
						selectedPropreties.get(input[1]).getPropertiesByQueryAndName(query, input[1]).get(0)
								.setChosen(true);

						lsa.addProperty(p);
						lsa.addProperty(p2);

						lsa.addSyntheticProperty(SyntheticPropertyName.STATE, state);
						// this.removeBondedLsasOfQuery(query);

					}
				}

			}
		}
	}

	private boolean checkInputs(Lsa boundedLsa, String query) {
		boolean full = true;
		List<Lsa> queryLsas = getBondedLsaByQuery(query);
		selectedPropreties.clear();
		if (getBondedLsaByQuery(query) == null || !getBondedLsaByQuery(query).contains(boundedLsa)) {
			addBondedLSA(boundedLsa);
		}
		queryLsas = getBondedLsaByQuery(query);
		Collections.shuffle(queryLsas);
		// nous avons enregistre un LSA qui contient la meme requete
		if (queryLsas != null) {
			for (int i = 0; i < this.input.length && full; i++) {
				for (Lsa lsa : queryLsas) {
					if (!lsa.getPropertiesByQueryAndName(query, input[i]).isEmpty()) {
						this.selectedPropreties.put(input[i], lsa);
						full = true;
						break;
					}
					full = false;
				}
			}
		}
		return full;
	}

	@Override
	public void onPropagationEvent(PropagationEvent event) {
	}

	@Override
	public void onDecayedNotification(DecayedEvent event) {
	}

	@Override
	public void onLsaUpdatedEvent(LsaUpdatedEvent event) {
	}

	@Override
	public void onRewardEvent(RewardEvent event) {
		String[] previousAgent = null;
		String newState = "";
		for (Property prop : event.getLsa().getPropertiesByQuery(event.getQuery())) {
			if (prop.getChosen()) {
				previousAgent = prop.getBond().split(",");
				for (int i = 0; i < previousAgent.length; i++) {
					newState = prop.getState();
					break;
				}
			}
			for (int i = 0; i < previousAgent.length; i++) {
				Lsa lsaReward = NodeManager.instance().getSpace().getLsa(previousAgent[i]);
				if (lsaReward != null
						&& lsaReward.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Service)) {
					rewardLsa(lsaReward, event.getQuery(), event.getReward(),
							getBestActionQvalue(getPreviousState(newState, this.output))); // maxQSt1
				}
				addState(getPreviousState(newState, this.output), 1, event.getReward(), event.getMaxSt1());
			}
			printQ();
		}
	}

	public String replaceState(String state) {
		String newState = state;
		for (int i = 0; i < output.length; i++) {

			newState = newState.replace(output[i], " ").replaceAll(" ", "");
			newState = newState.replace(",,", ",");
			if (newState.endsWith(",")) {
				newState = newState.substring(0, newState.length() - 1);
			}
		}
		return newState;
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	public String[] getOutput() {
		return output;
	}

	public void setOutput(String[] output) {
		this.output = output;
	}

}
