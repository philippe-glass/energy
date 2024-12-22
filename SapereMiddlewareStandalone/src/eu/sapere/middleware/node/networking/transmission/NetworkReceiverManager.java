package eu.sapere.middleware.node.networking.transmission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.sapere.middleware.agent.BasicSapereAgent;
import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;

/**
 * Provides the tcp-ip implementation for the receiver interface of the Sapere
 * networking.
 * 
 */
public class NetworkReceiverManager implements LsaReceived {
	private Map<String, Integer> mapDistanceByAddress = new HashMap<String, Integer>();// Distance computed for each node
	private Map<String, NodeLocation> mapLocationByAddress = new HashMap<String, NodeLocation>();
	private boolean activateQoS = false;

	public NetworkReceiverManager(boolean qosActivated) {
		super();
		this.activateQoS = qosActivated;
	}

	/**
	 * start Network Receiver Manager
	 */
	
	public void start() {
		Server server = new Server(this);
		server.start(NodeManager.getLocalPort());
		mapDistanceByAddress.clear();
		mapDistanceByAddress.put(NodeManager.getLocationAddress(), 0);
		mapLocationByAddress.clear();
		mapLocationByAddress.put(NodeManager.getLocationAddress(), NodeManager.getNodeLocation());
	}

	public Map<String, Integer> getMapDistanceByLocation() {
		return mapDistanceByAddress;
	}

	public Map<String, Integer> getMapDistanceByNode() {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for(String locationAddress : mapDistanceByAddress.keySet()) {
			Integer distance = mapDistanceByAddress.get(locationAddress);
			if(mapLocationByAddress.containsKey(locationAddress)) {
				NodeLocation nodeLocation = mapLocationByAddress.get(locationAddress);
				result.put(nodeLocation.getName(), distance);
			}
		}
		return result;
	}

	public NodeLocation getLocationByName(String name) {
		for (NodeLocation location : mapLocationByAddress.values()) {
			if (name.equals(location.getName())) {
				return location;
			}
		}
		return null;
	}

	public Collection<NodeLocation> getAllLocations() {
		return mapLocationByAddress.values();
	}

	public Map<String, NodeLocation> getMapLocationsByNode(boolean addCurrentNode) {
		Map<String, NodeLocation> result = new HashMap<>();
		for(NodeLocation nodeLocation : mapLocationByAddress.values()) {
			if(addCurrentNode || !NodeManager.isLocal(nodeLocation) ) {
				result.put(nodeLocation.getName(), nodeLocation.clone());
			}
		}
		return result;
	}

	public int getAllNodesCount() {
		return mapLocationByAddress.size();
	}

	public void clearMapDistance() {
		mapDistanceByAddress.clear();
		// add the current location
		mapDistanceByAddress.put(NodeManager.getLocationAddress(), 0);
	}

	/**
	 * @param receivedLsa
	 */
	public void doInject(Lsa receivedLsa) {
		BasicSapereAgent networkReceiverManagerAgent = new BasicSapereAgent("networkReceiverManager", activateQoS);
		networkReceiverManagerAgent.injectOperation(receivedLsa);
	}
	
	public void doReward(Lsa receivedLsa,String query) {
		BasicSapereAgent networkReceiverManagerAgent = new BasicSapereAgent("networkReceiverManager", activateQoS);
		networkReceiverManagerAgent.rewardOperation(receivedLsa, query, 1, 0); // change reward
	}

	public int getDistance(String location) {
		if(!mapDistanceByAddress.containsKey(location)) {
			return 999;
		}
		return mapDistanceByAddress.get(location);
	}

	private void updateDistance(String location, int distance) {
		if(mapDistanceByAddress.containsKey(location)) {
			if(mapDistanceByAddress.get(location) > distance) {
				mapDistanceByAddress.put(location, distance);
			}
		} else {
			mapDistanceByAddress.put(location,distance);
		}
	}

	public void onLsaReceived(Lsa lsaReceived) {
		boolean toDebug = false;
		if(toDebug) {
			MiddlewareLogger.getInstance().info("onLsaReceived : Received LSA:--" + lsaReceived.toReducedString2());
		}
		boolean stopDiffuse = lsaReceived.hasAlreadyBeenReceivedIn(NodeManager.getLocationAddress()) || lsaReceived.isFrom(NodeManager.getLocationAddress());
		if(stopDiffuse) {
			MiddlewareLogger.getInstance().info("onLsaReceived : stopDiffuse = " + stopDiffuse);
		}
		lsaReceived.addLocationInPath(NodeManager.getLocationAddress());
		int sourceDistance = lsaReceived.getSourceDistance();
		NodeLocation sourceLocation = lsaReceived.getAgentAuthentication().getNodeLocation();
		String sourceAddress = sourceLocation.getMainServiceAddress();
		updateDistance(sourceAddress, sourceDistance);
		if(!mapLocationByAddress.containsKey(sourceAddress)) {
			//String sourceNode = lsaReceived.getAgentAuthentication().getNodeLocation().getName();
			mapLocationByAddress.put(sourceAddress, sourceLocation);
		}
		if(toDebug && sourceDistance > 1) {
			System.out.println("NetworkReceiverManager : onLsaReceived : for debug : path = " + lsaReceived.getPath());
		}
		if (lsaReceived.hasSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP)
				&& !lsaReceived.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)) {
			if(stopDiffuse) {
				if(toDebug) {
					MiddlewareLogger.getInstance().info("Stop to propagate received lsa " + lsaReceived.toReducedString2());
				}
				lsaReceived.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0");
				lsaReceived.removeSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP);
			} else {
				if(toDebug) {
					System.out.println("Continue to propagate received lsa " + lsaReceived.getAgentName());
				}
			}
			if(toDebug) {
				System.out.println("gradient");
			}
			doInject(lsaReceived);
		}
		if (lsaReceived.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)
				&& !lsaReceived.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Reward)
				&& lsaReceived.getSyntheticProperty(SyntheticPropertyName.DESTINATION).equals(NodeManager.getLocationAddress())) {
			lsaReceived.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0");
			lsaReceived.removeSyntheticProperty(SyntheticPropertyName.DESTINATION);
			System.out.println("diffuse");
			doInject(lsaReceived);
		}
		if (lsaReceived.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Reward)) {
			if(activateQoS) {
				if(lsaReceived.isPropagated()) {
					if(toDebug) {
						System.out.println("Reward LSA type received --");
					}
					String query = lsaReceived.getSyntheticProperty(SyntheticPropertyName.QUERY).toString();
					Lsa lsaToReward = NodeManager.instance().getSpace().getLsa(lsaReceived.getAgentName().substring(0, lsaReceived.getAgentName().length() - 1));
					for (Property prop : lsaReceived.getPropertiesByQuery(query)) {
						if (prop.getChosen()) {
							lsaToReward.addProperty(prop);
							break;
						}
					}
					if(toDebug) {
						System.out.println("lsaToReward: "+lsaToReward.toReducedString2());
					}
					doReward(lsaToReward, query); // add reward and maxQt
				}
			}
		}
	}
}
