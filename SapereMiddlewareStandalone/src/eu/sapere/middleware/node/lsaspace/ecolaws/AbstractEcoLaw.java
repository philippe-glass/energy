package eu.sapere.middleware.node.lsaspace.ecolaws;

import java.util.Map;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.lsaspace.Operation;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;

/**
 * The abstract class for eco-laws.
 * 
 */
public abstract class AbstractEcoLaw implements IEcoLaw {

	/** The space in which the eco-law executes. */
	protected final Space space;
	/** The local Notifier */
	private Notifier notifier;
	/** The local Operation Manager */
	private OperationManager opManager;
	/** The interface for Network Delivery of LSAs */
	private NetworkDeliveryManager networkDeliveryManager;

	/**
	 * Constructs the Eco-Law.
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
	public AbstractEcoLaw(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager) {
		this.space = space;
		this.opManager = opManager;
		this.notifier = notifier;
		this.networkDeliveryManager = networkDeliveryManager;
	}

	/**
	 * The space in which the eco-law executes.
	 * 
	 * @return the space
	 */
	protected Map<String, Lsa> getLSAs() {
		return space.getAllLsa();
	}

	/**
	 * The network delivery manager of the space.
	 * 
	 * @return the network delivery manager
	 */
	protected NetworkDeliveryManager getNetworkDeliveryManager() {
		return networkDeliveryManager;
	}

	/**
	 * Removes an Lsa from a space.
	 * 
	 * @param lsa
	 *            the lsa to remove
	 */
	protected void remove(Lsa lsa) {
		Operation op = new Operation().removeOperation(lsa, null);
		opManager.queueOperation(op);
	}

	/**
	 * Injects an Lsa in a space.
	 * 
	 * @param lsa
	 *            the lsa to update
	 */
	protected void inject(Lsa lsa) {
		Operation op = new Operation().injectOperation(lsa, null);
		opManager.queueOperation(op);
	}

	/**
	 * Updates an Lsa in a space.
	 * 
	 * @param lsa
	 *            the lsa to update
	 */
	protected void update(Lsa lsa) {
		Operation op = new Operation().updateOperation(lsa, null);
		opManager.queueOperation(op);
	}

	/**
	 * Publishes an event using the notifier
	 * 
	 * @param event
	 *            the event to publish
	 */
	protected void publish(final AbstractSapereEvent event) {
		notifier.publish(event);
	}

}
