package eu.sapere.middleware.agent;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.lsaspace.Operation;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.notifier.INotifier;
import eu.sapere.middleware.node.notifier.Subscription;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

/**
 * Internal class that implements actions for Agents that manages LSA
 * 
 */
public abstract class LsaAgent extends Agent implements ISapereAgent {

	private static final long serialVersionUID = -2348175537981046617L;

	/** The OperationManager of the local SAPERE node */
	private OperationManager opMng = null;
	/** The Notifier of the local SAPERE node */
	private INotifier notifier = null;

	/**
	 * Instantiates this LsaAgent
	 * 
	 * @param agentName
	 *            The name of this Agent
	 * @param authentication 
	 * @param subdescriptions 
	 * @param propertiesName 
	 * @param type 
	 */
	public LsaAgent(String agentName, AgentAuthentication authentication, String[] subdescriptions, String[] propertiesName, LsaType type) {
		super(agentName, authentication, subdescriptions, propertiesName, type);
		this.opMng = NodeManager.instance().getOperationManager();
		this.notifier = NodeManager.instance().getNotifier();
	}

	/**
	 * Removes the managed LSA from the local LSA space
	 * 
	 * @param lsa
	 */
	public void removeLsa(Lsa lsa) {
		Operation op = new Operation().removeOperation(lsa, this);
		opMng.queueOperation(op);
	}

	/**
	 * Reward the managed LSA
	 * 
	 * @param lsa
	 * @param reward
	 * @param query
	 * @param maxQst1 
	 * @param maxQSt1 
	 */
	public void rewardLsa(Lsa lsa, String query, int reward, double maxQst1) {
		Operation op = new Operation().rewardOperation(lsa, this, query, reward, maxQst1);
		opMng.queueOperation(op);
	}

	/**
	 * Attributes Id to LSA
	 */
	protected void submitOperation() {
		if (lsa.getAgentName().equals("")) {
			lsa.setAgentName(getAgentName());
			lsa.setAgentAuthentication(authentication);
			Operation op = new Operation().injectOperation(lsa, this);
			opMng.queueOperation(op);
		} else {
			Operation op = new Operation().updateOperation(lsa, this);
			opMng.queueOperation(op);
		}
	}

	@Override
	public void onNotification(AbstractSapereEvent event) {
		if (event.getClass().isAssignableFrom(BondEvent.class)) {
			onBondNotification((BondEvent) event);
		}
		if (event.getClass().isAssignableFrom(RewardEvent.class)) {
			onRewardEvent((RewardEvent) event);
		}
		if (event.getClass().isAssignableFrom(PropagationEvent.class)) {
			onPropagationEvent((PropagationEvent) event);
		}
		if (event.getClass().isAssignableFrom(LsaUpdatedEvent.class)) {
			onLsaUpdatedEvent((LsaUpdatedEvent) event);
		}
		if (event.getClass().isAssignableFrom(DecayedEvent.class)) {
			onDecayedNotification((DecayedEvent) event);
			Subscription s = new Subscription(event, this, this.getAgentName());
			notifier.unsubscribe(s);
		}
	}
}
