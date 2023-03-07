package eu.sapere.middleware.node.networking.transmission;

import eu.sapere.middleware.agent.BasicSapereAgent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;

/**
 * Provides the tcp-ip implementation for the receiver interface of the Sapere
 * networking.
 * 
 */
public class NetworkReceiverManager implements LsaReceived {

	/**
	 * start Network Receiver Manager
	 */
	
	public void start() {
		Server server = new Server(this);
		server.start(NodeManager.getLocalPort());
	}

	/**
	 * @param receivedLsa
	 */
	public void doInject(Lsa receivedLsa) {
		BasicSapereAgent networkReceiverManagerAgent = new BasicSapereAgent("networkReceiverManager");
		networkReceiverManagerAgent.injectOperation(receivedLsa);
	}
	
	public void doReward(Lsa receivedLsa,String query) {
		BasicSapereAgent networkReceiverManagerAgent = new BasicSapereAgent("networkReceiverManager");
		networkReceiverManagerAgent.rewardOperation(receivedLsa, query, 1, 0); // change reward
	}

	public void onLsaReceived(Lsa lsaReceived) {
		System.out.println("Received LSA:--" + lsaReceived.toVisualString());
		if (lsaReceived.hasSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP)
				&& !lsaReceived.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)) {
			lsaReceived.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0");
			lsaReceived.removeSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP);
			System.out.println("gradient");
			doInject(lsaReceived);
		}
		if (lsaReceived.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)
				&& !lsaReceived.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Reward)
				&& lsaReceived.getSyntheticProperty(SyntheticPropertyName.DESTINATION).equals(NodeManager.getLocation())) {
			lsaReceived.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0");
			lsaReceived.removeSyntheticProperty(SyntheticPropertyName.DESTINATION);
			System.out.println("diffuse");
			doInject(lsaReceived);
		}
		if (lsaReceived.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Reward)) {
			if(lsaReceived.getAgentName().contains("*")) {
				System.out.println("Reward LSA type received --");
				String query = lsaReceived.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
				Lsa lsaToReward = NodeManager.instance().getSpace().getLsa(lsaReceived.getAgentName().substring(0, lsaReceived.getAgentName().length() - 1));
				for (Property prop : lsaReceived.getPropertiesByQuery(query)) { 
					if (prop.getChosen()) {
						lsaToReward.addProperty(prop);
						break;
					}
				}
				System.out.println("lsaToReward: "+lsaToReward.toVisualString());
				doReward(lsaToReward, query); // add reward and maxQt
			}
		
		
		}
	}
}
