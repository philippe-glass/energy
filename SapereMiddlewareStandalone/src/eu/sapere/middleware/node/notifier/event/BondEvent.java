package eu.sapere.middleware.node.notifier.event;

import eu.sapere.middleware.lsa.Lsa;

/**
 * An event representing a Bond happened to a LSA
 * 
 */
public class BondEvent extends AbstractSapereEvent {

	private static final long serialVersionUID = 899040460408932860L;
	private Lsa lsa = null;
	private Lsa bondedLsa = null;

	/**
	 * Instantiates the Event
	 * 
	 * @param lsa
	 * @param bondId
	 * @param bondedLsa
	 */
	public BondEvent(Lsa lsa, Lsa bondedLsa) {
		this.lsa = lsa;
		this.bondedLsa = bondedLsa;
	}

	/**
	 * @return The Lsa
	 */
	public Lsa getLsa() {
		return lsa;
	}

	/**
	 * @return The binded LSA
	 */
	public Lsa getBondedLsa() {
		return bondedLsa;
	}

}
