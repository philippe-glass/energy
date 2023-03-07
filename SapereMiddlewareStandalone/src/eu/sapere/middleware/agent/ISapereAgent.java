package eu.sapere.middleware.agent;

import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

/**
 * Interface for react to events happening in the LSA space
 * 
 */
public interface ISapereAgent {

	/**
	 * Called when a new Bond happens.
	 * 
	 * @param event
	 *            The BondEvent
	 */
	public void onBondNotification(BondEvent event);

	/**
	 * Called when the LSA is removed by the Decay eco-law
	 * 
	 * @param event
	 *            The DecayedEvent
	 */
	public void onDecayedNotification(DecayedEvent event);

	/**
	 * Called when the LSA is propagated
	 * 
	 * @param event
	 *            The PropagationEvent
	 */
	public void onPropagationEvent(PropagationEvent event);

	/**
	 * Called when the LSA is rewarded 
	 * 
	 * @param event
	 *            The RewardEvent
	 */
	public void onRewardEvent(RewardEvent event);
	
	/**
	 * Called when the LSA is updated by an eco-law
	 * 
	 * @param event
	 *            The LsaUpdatedEvent
	 */
	public void onLsaUpdatedEvent(LsaUpdatedEvent event);

}
