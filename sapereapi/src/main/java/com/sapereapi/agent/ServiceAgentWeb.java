package com.sapereapi.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
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

public class ServiceAgentWeb extends SapereAgent {
	private static final long serialVersionUID = 203L;
	Random rand = new Random();
	private String[] output;
	private String[] input;
	HashMap<String, String> inputValue;
	String appid;
	String webServiceLink;
	static SapereLogger logger = SapereLogger.getInstance();


	public ServiceAgentWeb(String webServiceLink, AgentAuthentication authentication, String[] input, String[] output, String appid,
			LsaType type) {
		super(authentication.getAgentName(), authentication, input, output, type);
		this.agentName = authentication.getAgentName();
		this.webServiceLink = webServiceLink;
		this.output = output;
		this.input = input;
		this.appid = appid;
		inputValue = new HashMap<String, String>();
	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

	@Override
	public void onBondNotification(BondEvent event) {
		Lsa bondedLsa = event.getBondedLsa();
		String query = bondedLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
		logger.info("** ServiceAgent bonding ** " + agentName + " Q: " + query);
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Service); // check
		this.addBondedLSA(bondedLsa);

		int action = getActionToTake(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString()); // add
		action = 1; // greedy
		if (lsa.getSubDescription().size() == input.length) {
			Lsa chosenLSA = getBondedLsaByQuery(query).get(rand.nextInt(getBondedLsaByQuery(query).size()));
			String state = chosenLSA.getSyntheticProperty(SyntheticPropertyName.STATE).toString() + ","
					+ lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString();

			if (action == 0) {
				addState(bondedLsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString(), action, 0, 0);
				lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(), null,
						query, chosenLSA.getAgentName(), state,
						chosenLSA.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));
			} else if (action == 1) {
				logger.info("Action --> 1");
				
				String recValue [] = new String[input.length];
				for (int i = 0; i < input.length; i++) {
					recValue[i] = bondedLsa.getPropertiesByQueryAndName(query, input[0]).get(0).getValue().toString();
					inputValue.put(input[i], recValue[i]);
				}
				String result = sendingPostRequest(webServiceLink, inputValue);

				lsa.addProperty(new Property(lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString(),
						result + "", query, chosenLSA.getAgentName(), state,
						chosenLSA.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString(), false));

				chosenLSA.getPropertiesByQueryAndName(query, input[0])
						.get(rand.nextInt(chosenLSA.getPropertiesByQueryAndName(query, input[0]).size()))
						.setChosen(true); // one Lsa can contain many property for same query
				lsa.addSyntheticProperty(SyntheticPropertyName.STATE, state);
			}
			this.removeBondedLsasOfQuery(query);
		}

		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP, "3");
		logger.info("boundedLsa" + bondedLsa.toVisualString());
		logger.info("this.lsa" + lsa.toVisualString());
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
		Lsa lsaReward = NodeManager.instance().getSpace().getLsa(previousAgent);
		if (lsaReward != null && lsaReward.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Service)) {
			rewardLsa(lsaReward, event.getQuery(), event.getReward(),
					getBestActionQvalue(getPreviousState(newState, this.output))); // maxQSt1
		}
		addState(getPreviousState(newState, this.output), 1, event.getReward(), event.getMaxSt1());
		printQ();
		printR();
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

	public String sendingPostRequest(String url, HashMap<String, String> values) {
		String link = url + "?";
		for (int i = 0; i < input.length; i++) {
			if (i != 0) {
				link += "&";
			}
			link += input[i] + "=" + inputValue.get(input[i]);
		}
		if (!appid.equals("")) {
			link += "&appid=" + appid;
		}
		logger.info(link);
		HttpURLConnection c = null;
		try {
			URL u = new URL(link);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("GET");
			c.setRequestProperty("Content-length", "0");
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.connect();
			logger.info(link);
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
				
				logger.info(sb.toString());
				return sb.toString();
			}
		} catch (Exception e) {
			logger.info("in");
			logger.error(e);
			//e.printStackTrace();
		}
		return null;

	}

}
