package com.sapereapi.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

public class QueryAgent extends SapereAgent {
	private static final long serialVersionUID = 202L;
	protected List<Lsa> results = new ArrayList<Lsa>();
	protected String selectedResult;
	protected Lsa selectedLsa;
	protected String[] prop;
	protected String[] waiting;
	protected Object[] values;
	protected Random rand = new Random();
	protected boolean alreadyRewarded;
	protected static SapereLogger logger = SapereLogger.getInstance();

	public QueryAgent(String agentName, AgentAuthentication authentication, String[] subdescription, String[] propertiesName, Object[] values,
			LsaType type, boolean activateQOS) {
		super(agentName, authentication, subdescription, propertiesName, type, activateQOS);
		initFields(agentName, subdescription, propertiesName, values, type);

		setInitialLSA();
		query();
	}

	public void initFields(String agentName, String[] subdescription, String[] propertiesName, Object[] values,LsaType type) {
		selectedLsa = null;
		selectedResult = "";
		this.agentName = agentName;
		this.prop = propertiesName;
		this.waiting = subdescription;
		this.values = values;
		alreadyRewarded = false;
	}

	public void reinitialize(String agentName, String[] subdescription, String[] propertiesName, Object[] values,LsaType type) {
		initFields(agentName, subdescription, propertiesName, values, type);
		this.lsa.setAgentName("");
		this.lsa.removeAllProperties();
		setInitialLSA();
		query();
	}

	private void reward(Lsa lsaResult, int reward) {
		logger.info("rewarded-->" + lsaResult.getAgentName() +" by "+ reward);
		logger.info("lsaResult: "+lsaResult.toVisualString());
		
		if(lsaResult.getAgentName().contains("*")) {
			lsaResult.addSyntheticProperty(SyntheticPropertyName.TYPE, LsaType.Reward);
			sendTo(lsaResult, lsaResult.getSyntheticProperty(SyntheticPropertyName.SOURCE).toString());
			}
		else
			this.rewardLsa(lsaResult, agentName, reward, 0.0);
	}

	public void query() {
		if(lsa.getProperties().size()>0) {
			lsa.removeAllProperties();
		}
		for (int i = 0; i < prop.length; i++) {
			if(values[i]!=null) {
				Property proprety = new Property(prop[i], values[i], agentName, "",
						Arrays.toString(waiting).replace("[", "").replace("]", "") + "|"
								+ Arrays.toString(prop).replace("[", "").replace("]", ""),
						NodeManager.getLocation(), false);
				lsa.addProperty(proprety);
			}
		}
		lsa.addSyntheticProperty(SyntheticPropertyName.QUERY, lsa.getAgentName());
		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.DECAY, "4");
		lsa.addSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP, "3");
		lsa.addSyntheticProperty(SyntheticPropertyName.STATE, Arrays.toString(waiting).replaceAll("\\[|\\]", "") + "|"
				+ Arrays.toString(prop).replaceAll("\\[|\\]", ""));
		//lsa.addSyntheticProperty(SyntheticPropertyName.STATE, "PROD|REQ,Value,Date,Duration");
		if(debugLevel>0) {
			logger.info("query injected" + lsa.toVisualString());
		}
	}

	@Override
	public void onBondNotification(BondEvent event) {

		Lsa boundedLsa = event.getBondedLsa();
		logger.info("Bond event : ** Agent ** " + agentName + " - " + boundedLsa.getAgentName());
		if (!boundedLsa.getPropertiesByQueryAndName(agentName, waiting[0]).isEmpty()) {
			if (!results.contains(boundedLsa)) {
				results.add(boundedLsa);
			}
		}

	}

	@Override
	public void onDecayedNotification(DecayedEvent event) { // change to return only one result
		if (Integer.parseInt(lsa.getSyntheticProperty(SyntheticPropertyName.DECAY).toString()) == 0) {
			if (!results.isEmpty()) {
				Collections.shuffle(results);
				selectedLsa = results.get(0);
				Property propResult = selectedLsa.getPropertiesByQueryAndName(agentName, waiting[0]).get(0);
				selectedResult = propResult.toString();
				propResult.setChosen(true);
				logger.info("-->result: " + propResult.toString());
			} else {
				logger.info("no result found");
			}
		}
	}

	public boolean rewardLsaFromApi(int reward) {
		if (selectedLsa != null && !alreadyRewarded) {
			reward(selectedLsa,reward);
			return true;
		}
		return false;
	}

	public String getSelectedResult() {
		return selectedResult;
	}

	public void setSelectedResult(String selectedResult) {
		this.selectedResult = selectedResult;
	}

	public Lsa getSelectedLsa() {
		return selectedLsa;
	}

	public void setSelectedLsa(Lsa selectedLsa) {
		this.selectedLsa = selectedLsa;
	}

	@Override
	public void onPropagationEvent(PropagationEvent event) {

	}

	@Override
	public void onRewardEvent(RewardEvent event) {

	}

	@Override
	public void onLsaUpdatedEvent(LsaUpdatedEvent event) {

	}

	@Override
	public void setInitialLSA() {
		this.submitOperation();
	}

}
