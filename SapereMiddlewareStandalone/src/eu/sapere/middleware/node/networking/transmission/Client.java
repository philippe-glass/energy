package eu.sapere.middleware.node.networking.transmission;

import java.util.ArrayList;
import java.util.List;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;

/**
 * The tcp/ip client for the Sapere network interface.
 * 
 */
public class Client {

	Lsa deliverLsa = null;

	private List<String> getDestinationsToDeliver(Lsa deliverLsa, String[] neighbours) {
		List<String> listToDeliver = new ArrayList<String>();
		for(String nextNeighbour : neighbours) {
			if(!deliverLsa.hasAlreadyBeenSentTo(nextNeighbour) && !deliverLsa.isFrom(nextNeighbour)) {
				listToDeliver.add(nextNeighbour);
			}
		}
		return listToDeliver;
	}

	/**
	 * Delivers a Lsa 
	 * 
	 * @param deliverLsa
	 *            The Lsa to be delivered.
	 * @param neighbours
	 */
	public void deliver(Lsa deliverLsa, String[] neighbours) {
		this.deliverLsa = deliverLsa;
		if (deliverLsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) {
			// Check if the LSA has already be sent to the neighbour destinations or the current destination
			List<String> listToDeliver = getDestinationsToDeliver(deliverLsa, neighbours);
			if(listToDeliver.isEmpty()) {
				// Lsa already sent to all neighbour and current destination destinations : stop the diffusion.
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0");
				//MiddlewareLogger.getInstance().info("client.deliver : stop diffusion on lsa " + deliverLsa.toReducedString2());
			} else {
				if(deliverLsa.getSendings().size() > 0) {
					MiddlewareLogger.getInstance().info("client.deliver : continue lsa delivery " + deliverLsa.toReducedString2() + " from " + NodeManager.getLocationAddress()  + " to " + listToDeliver);
				}
				for (String nextNeighbour : listToDeliver) {
					deliverLsa.addLocationInSending(nextNeighbour);
				}
				for (String nextNeighbour : listToDeliver) {
					deliverLsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocationAddress());
					deliverLsa.addSyntheticProperty(SyntheticPropertyName.SOURCE, NodeManager.getLocationAddress());
					deliverLsa.addSyntheticProperty(SyntheticPropertyName.DECAY, "100");
					// TODO add neighbours[i] to the path
					PoolThread.getInstance().pushLsa(deliverLsa, nextNeighbour);
				}
			}
		}
		/*
		for (int i = 0; i < neighbours.length; i++) {
			if (deliverLsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) {
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocation());
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.SOURCE, NodeManager.getLocation());
				deliverLsa.addSyntheticProperty(SyntheticPropertyName.DECAY, "100");
				// TODO add neighbours[i] to the path
				PoolThread.getInstance().pushLsa(deliverLsa, neighbours[i]);
			}
		}*/
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
			deliverLsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocationAddress());
			deliverLsa.addSyntheticProperty(SyntheticPropertyName.SOURCE, NodeManager.getLocationAddress());
			//if(!deliverLsa.hasSyntheticProperty(SyntheticPropertyName.LAST_SENDING)) {
			//	deliverLsa.addSyntheticProperty(SyntheticPropertyName.LAST_SENDING, new Date());
			//}
			PoolThread.getInstance().pushLsa(deliverLsa, ip);
		}
	}
}
