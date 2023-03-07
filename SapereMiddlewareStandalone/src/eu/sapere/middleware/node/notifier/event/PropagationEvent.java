package eu.sapere.middleware.node.notifier.event;

import eu.sapere.middleware.lsa.Lsa;

/**
 * An Event representing a LSA propagated by the Propagation Eco-law
 * 
 */
public class PropagationEvent extends AbstractSapereEvent {

	private static final long serialVersionUID = 6947777833234083649L;
	private Lsa lsa = null;

	/**
	 * Instantiates the Event
	 * 
	 * @param lsa
	 *            The propagated LSA
	 */
	public PropagationEvent(Lsa lsa) {
		this.lsa = lsa;
	}

	/**
	 * Gets the propagated LSA
	 * 
	 * @return The propagated LSA
	 */
	public Lsa getLsa() {
		return lsa;
	}
}
