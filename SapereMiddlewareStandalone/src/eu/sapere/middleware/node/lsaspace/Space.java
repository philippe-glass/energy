package eu.sapere.middleware.node.lsaspace;

import java.util.LinkedHashMap;
import java.util.Map;

import eu.sapere.middleware.agent.Agent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.INotifier;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

/**
 * The SAPERE LSA space implementation.
 */
public class Space {

	private Map<String, Lsa> spaceMap = null;
	private INotifier notifier = null;

	/**
	 * Instantiates a LSA space.
	 * 
	 * @param notifier
	 *            The reference to the Notifier
	 */
	public Space(INotifier notifier) {
		spaceMap = new LinkedHashMap<String, Lsa>();
		this.notifier = notifier;
	}

	/**
	 * Injects a copy of the LSA into the LSA space
	 * 
	 * @param lsa
	 *            the LSA to be injected
	 * @param creatorId
	 *            the name of the Agent that requests the injection
	 */
	public void inject(Lsa lsa) {
		lsa.addSyntheticProperty(SyntheticPropertyName.LOCATION, NodeManager.getNodeName());
		if (!lsa.hasSyntheticProperty(SyntheticPropertyName.SOURCE))
			lsa.addSyntheticProperty(SyntheticPropertyName.SOURCE, NodeManager.getLocationAddress());
		if (!lsa.hasSyntheticProperty(SyntheticPropertyName.BOND))
			lsa.addSyntheticProperty(SyntheticPropertyName.BOND, "");
		
		spaceMap.put(lsa.getAgentName(), lsa);
	}

	/**
	 * Reward a LSA in the LSA space
	 * 
	 * @param lsa
	 * @param query 
	 * @param reward 
	 * @param maxQst1 
	 * @param previousReward 
	 */
	public void reward(Lsa lsa, String query, int reward, double maxQst1) {
		AbstractSapereEvent lsaRewardEvent = new RewardEvent(lsa, query, reward, maxQst1);
		lsaRewardEvent.setRequiringAgent(lsa.getAgentName());
		notifier.publish(lsaRewardEvent);
	}

	/**
	 * Updates a LSA in the LSA space
	 * 
	 * @param lsaId
	 *            the id of the LSA to be updated
	 * @param lsa
	 *            the new content for the LSA
	 * @param requestingAgent
	 *            the name of the Agent that requests the update
	 * @param destroyBonds
	 *            true if already existing bonds can be destroyed, false otherwise
	 * @param generateEvents
	 *            true if events must be triggered, false otherwise
	 */
	public void update(Lsa lsa, Agent requestingAgent, boolean destroyBonds, boolean generateEvents) {
		
	}

	/**
	 * Removes a LSA from the LSA space
	 * 
	 * @param lsaId
	 *            the Id of the LSA to be removed
	 */
	public void remove(String lsaId) {
		if (spaceMap.get(lsaId) != null) {
			spaceMap.remove(lsaId);
		}
	}

	/**
	 * @param agentName
	 * @return Lsa
	 */
	public Lsa getLsa(String agentName) {
		Lsa ret = null;
		if (spaceMap.containsKey(agentName)) {
			ret = spaceMap.get(agentName);
		}
		return ret;
	}

	/**
	 * @return an array containing all the LSAs in this Space
	 */
	public Map<String, Lsa> getAllLsa() {
		return spaceMap;
	}

}