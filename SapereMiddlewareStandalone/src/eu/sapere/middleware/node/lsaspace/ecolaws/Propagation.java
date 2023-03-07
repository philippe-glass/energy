package eu.sapere.middleware.node.lsaspace.ecolaws;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;

/**
 * The Propagation eco-law implementation.
 * 
 */
public class Propagation extends AbstractEcoLaw {

	/**
	 * Creates a new instance of the Propagation eco-law.
	 * 
	 * @param space
	 *            The space in which the eco-law executes.
	 * @param opManager
	 *            The OperationManager that manages operations in the space
	 * @param notifier
	 *            The Notifier that notifies agents with events happening to LSAs
	 * @param networkDeliveryManager
	 *            The interface for Network Delivery of LSAs
	 */
	public Propagation(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager) {
		super(space, opManager, notifier, networkDeliveryManager);
	}

	public void invoke() {
		for (Lsa lsa : getLSAs().values()) {
			//System.out.println(lsa.toVisualString());
			if (lsa.hasSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP)
					&& !lsa.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)
					&& lsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) { // propagate
				doGradientPropagation(lsa);
			}
			if (lsa.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)
					&& lsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) { // send direct
				doDirectPropagation(lsa, lsa.getSyntheticProperty(SyntheticPropertyName.DESTINATION).toString());
			}
		}
	}

	/**
	 * Propagate an LSA using gradient propagation
	 * 
	 * @param an_lsa
	 *            the LSA to propagate.
	 */
	private void doGradientPropagation(Lsa deliverLsa) {
		getNetworkDeliveryManager().doSpread(lsaCopy(deliverLsa));
		deliverLsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0"); // don't propagate
		PropagationEvent propagationEvent = new PropagationEvent(deliverLsa);
		propagationEvent.setRequiringAgent(deliverLsa.getAgentName());
		publish(propagationEvent);
	}

	/**
	 * Propagate an LSA
	 * 
	 * @param an_lsa
	 *            the LSA to propagate.
	 */
	private void doDirectPropagation(Lsa deliverLsa, String ip) {
		getNetworkDeliveryManager().sendTo(lsaCopy(deliverLsa), ip);
		deliverLsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0"); // don't propagate
		PropagationEvent propagationEvent = new PropagationEvent(deliverLsa);
		propagationEvent.setRequiringAgent(deliverLsa.getAgentName());
		publish(propagationEvent);
	}

	/**
	 * Creates a copy of an LSA to be propagated, removing the id, bonds and other
	 * properties.
	 * 
	 * @param an_lsa
	 *            the LSA to be copied.
	 * @param the_finalHop
	 *            true if this is the final hop for the LSA, false otherwise.
	 * @return the LSA copy.
	 */
	private Lsa lsaCopy(Lsa lsa) {
		Lsa copy = lsa.getCopy();
		if (!copy.getAgentName().contains("*")) {
			copy.setAgentName(copy.getAgentName() + "*");
		}
		return copy;
	}

}