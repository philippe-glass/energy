package eu.sapere.middleware.node.networking.transmission;

import eu.sapere.middleware.lsa.Lsa;

/**
 * Provides the tcp-ip implementation for the delivery interface of the Sapere
 * networking.
 * 
 */
public class NetworkDeliveryManager {

	private Client client;
	private String[] neighbours;

	public String[] getNeighbours() {
		return neighbours;
	}

	public void setNeighbours(String[] neighbours) {
		this.neighbours = neighbours;
	}

	/**
	 * Constructor
	 */
	public NetworkDeliveryManager() {
		client = new Client();
	}

	/**
	 * @param deliverLsa
	 * @return
	 */
	public boolean doSpread(Lsa deliverLsa) {
		if(neighbours!=null && neighbours.length>0)
		client.deliver(deliverLsa, neighbours);
		return true;
	}

	/**
	 * @param deliverLsa
	 * @param ip
	 * @return
	 */
	public boolean sendTo(Lsa deliverLsa, String ip) {
		client.deliver(deliverLsa, ip);
		return true;
	}
}
