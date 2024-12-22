package eu.sapere.middleware.node.notifier.event;

import eu.sapere.middleware.lsa.Lsa;

/**
 * An Event representing a LSA propagated by the Spreading Eco-law
 * 
 */
public class SpreadingEvent extends AbstractSapereEvent {

	private static final long serialVersionUID = 6947777833234083649L;

	/**
	 * Instantiates the Event
	 * 
	 * @param lsa
	 *            The propagated LSA
	 */
	public SpreadingEvent(Lsa lsa) {
		this.lsa = lsa;
	}


}
