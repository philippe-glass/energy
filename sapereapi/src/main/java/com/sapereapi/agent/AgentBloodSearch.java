package com.sapereapi.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.spatial_services.Blood;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.AggregationEvent;
import eu.sapere.middleware.node.notifier.event.SpreadingEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

public class AgentBloodSearch extends SapereAgent {
	private static final long serialVersionUID = 5920535286218204783L;
	private String[] input;
	private String[] output;
	Random rand = new Random();
	private static SapereLogger logger = SapereLogger.getInstance();

	public AgentBloodSearch(AgentAuthentication authentication, String[] input, String[] output, LsaType type, boolean enableQoS) {
		super(authentication.getAgentName(), authentication, input, output, type, enableQoS);
		this.input = input;
		this.output = output;
	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

	@Override
	public void onBondNotification(BondEvent event) {
		Lsa bondedLsa = event.getBondedLsa();
		String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
		logger.info("Bond event : ** Agent ** " + agentName + " -- " + bondedLsa.getAgentName() + " : " + query);
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
		this.addBondedLSA(bondedLsa);
		int action = getActionToTake(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString()); // add //
		
		if (lsa.getSubDescription().size() == 1) { // output
			Lsa chosenLSA = getBondedLsaByQuery(query).get(rand.nextInt(getBondedLsaByQuery(query).size()));
			String state = chosenLSA.getSyntheticProperty(SyntheticPropertyName.STATE).toString() + ","
					+ lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString();
			if (action == 0) {
				addState(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString(), action, 0, 0);
				lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(), null,
						query, chosenLSA.getAgentName(), state,
						chosenLSA.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));
			} else if (action == 1) {
				Blood blood = new Blood("x", "1");
				if (blood != null && Integer.valueOf(blood.getBloodBags()) >= 0) {
					lsa.addProperty(new Property("Position", blood.getPosition(), query, bondedLsa.getAgentName(),
							state, bondedLsa.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));
					lsa.addProperty(new Property("BloodBags", blood.getBloodBags(), query, bondedLsa.getAgentName(),
							state, bondedLsa.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));

					chosenLSA.getPropertiesByQueryAndName(query, input[0])
							.get(rand.nextInt(chosenLSA.getPropertiesByQueryAndName(query, input[0]).size()))
							.setChosen(true); // one Lsa can contain many property for same query
					lsa.addSyntheticProperty(SyntheticPropertyName.STATE, state);
				}
			}
		}
		this.removeBondedLsasOfQuery(query);
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
		Lsa lsaReward = NodeManager.instance().getSpace().getLsa(previousAgent);
		if (lsaReward != null && lsaReward.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Service)) {
			rewardLsa(lsaReward, event.getQuery(), event.getReward(),
					getBestActionQvalue(SapereUtil.getPreviousState(newState, this.output))); // maxQSt1
		}
		addState(SapereUtil.getPreviousState(newState, this.output), 1, event.getReward(), event.getMaxSt1());

		printQ();
	}

	public static String sendingPostRequest(String type) {
		String url = "http://localhost:3000/availableBloodBags?Blood=" + type;
		HttpURLConnection c = null;
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("GET");
			c.setRequestProperty("Content-length", "0");
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.connect();
			int status = c.getResponseCode();

			switch (status) {
			case 200:
			case 201:
				BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				return sb.toString();
			}
		} catch (Exception e) {
		}
		return null;

	}

	@Override
	public void onSpreadingEvent(SpreadingEvent event) {
	}

	@Override
	public void onDecayedNotification(DecayedEvent event) {
	}

	@Override
	public void onAggregationEvent(AggregationEvent event) {
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
