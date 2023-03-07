package eu.sapere.middleware.node.notifier.event;

import eu.sapere.middleware.lsa.Lsa;

/**
 * An Event representing a LSA updated by an Eco-law
 * 
 */
public class LsaUpdatedEvent extends AbstractSapereEvent {

	private static final long serialVersionUID = 2291794225134181080L;
	private Lsa lsa = null;

	/**
	 * Instantiates the Event
	 * 
	 * @param lsa
	 *            The updated LSA
	 */
	public LsaUpdatedEvent(Lsa lsa) {
		this.lsa = lsa;
	}

	/**
	 * Gets the updated LSA
	 * 
	 * @return The updated LSA
	 */
	public Lsa getLsa() {
		return lsa;
	}

}
