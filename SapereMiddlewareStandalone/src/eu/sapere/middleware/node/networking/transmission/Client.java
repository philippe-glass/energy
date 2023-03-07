package eu.sapere.middleware.node.networking.transmission;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;

/**
 * The tcp/ip client for the Sapere network interface.
 * 
 */
public class Client {

	Lsa deliverLsa = null;

	/**
	 * Delivers a Lsa 
	 * 
	 * @param deliverLsa
	 *            The Lsa to be delivered.
	 * @param neighbours
	 */
	public void deliver(Lsa deliverLsa, String[] neighbours) {
		this.deliverLsa = deliverLsa;
		for (int i = 0; i < neighbours.length; i++) {
			if (deliverLsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) {
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocation());
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.SOURCE, NodeManager.getLocation());
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.DECAY, "100");
				PoolThread.getInstance().pushLsa(deliverLsa, neighbours[i]);
			}
		}
	}
	
	/**
	 * Delivers a Lsa to the node specified.
	 * 
	 * @param deliverLsa
	 *            The Lsa to be delivered.
	 * @param ip
	 */
	public void deliver(Lsa deliverLsa, String ip) {
		this.deliverLsa = deliverLsa;
			if (deliverLsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) {
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocation());
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.SOURCE, NodeManager.getLocation());
				PoolThread.getInstance().pushLsa(deliverLsa, ip);
			}
	}
}
