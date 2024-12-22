package eu.sapere.middleware.node.notifier.event;

import eu.sapere.middleware.lsa.Lsa;

/**
 * An Event representing a LSA updated by an Eco-law
 * 
 */
public class AggregationEvent extends AbstractSapereEvent {

	private static final long serialVersionUID = 2291794225134181080L;

	/**
	 * Instantiates the Event
	 * 
	 * @param lsa
	 *            The updated LSA
	 */
	public AggregationEvent(Lsa lsa) {
		this.lsa = lsa;
	}

}
