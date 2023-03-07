package eu.sapere.middleware.node.lsaspace.ecolaws;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;

/**
 * The Decay eco-law implementation.
 * 
 */
public class Decay extends AbstractEcoLaw {

	/**
	 * Creates a new instance of the decay eco-law.
	 * 
	 * @param space                  The space in which the eco-law executes.
	 * @param opManager              The OperationManager that manages operations in
	 *                               the space
	 * @param notifier               The Notifier that notifies agents with events
	 *                               happening to LSAs
	 * @param networkDeliveryManager The interface for Network Delivery of LSAs
	 */
	public Decay(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager) {
		super(space, opManager, notifier, networkDeliveryManager);
	}

	@Override
	public void invoke() {
		for (Lsa lsa : getLSAs().values()) {
			if (lsa.hasSyntheticProperty(SyntheticPropertyName.DECAY)) {
				applyDecayToLsa(lsa);
			}
		}
	}

	/**
	 * Applies the decay transformation to an LSA.
	 * 
	 * @param lsa the LSA to decay.
	 */
	private void applyDecayToLsa(Lsa lsa) {
		int decay = decrement(lsa.getSyntheticProperty(SyntheticPropertyName.DECAY).toString());
		if (decay >= 0) {
			lsa.addSyntheticProperty(SyntheticPropertyName.DECAY, decay + "");
			AbstractSapereEvent decayEvent = new DecayedEvent(lsa);
			decayEvent.setRequiringAgent(lsa.getAgentName());
			publish(decayEvent);
		} else {
			remove(lsa);
		}

	}

	/**
	 * Decrements the integer value provided
	 * 
	 * @param n the integer value to be decremented
	 * @return The decremented value
	 */
	private Integer decrement(String n) {
		int i = 0;
		try {
			i = Integer.parseInt(n.trim()) - 1;
		} catch (NumberFormatException nfe) {
			System.err.println(nfe.toString());
		}
		return i;
	}

}
