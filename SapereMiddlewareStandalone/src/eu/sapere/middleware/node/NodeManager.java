package eu.sapere.middleware.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.lsaspace.SpaceRunner;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.networking.transmission.NetworkReceiverManager;
import eu.sapere.middleware.node.notifier.Notifier;

/**
 * Initializes the Local Sapere Node
 */
public final class NodeManager {
	public static final int DEFAULT_PORT = 10009;
	public static final int DEFAULT_REST_PORT = 9090;

	/** The Network Delivery Manager */
	public static NetworkDeliveryManager networkDeliveryManager = null;
	/** The Network Delivery Manager */
	private static NetworkReceiverManager networkReceiverManager = null;
	/** The local LSA space */
	private Space space = null;
	/** The local Notifier */
	private Notifier notifier = null;
	/** The SpaceRunner of the Node */
	private SpaceRunner runner = null;

	private boolean qosActivated = false;
	/**
	 * This is a reference to the one and only instance of this singleton object.
	 */
	private static NodeManager singleton = null;
	/** Sleep time */
	public static final long SLEEPTIME = 1000;

	/** Node configuration */
	private static NodeLocation nodeLocation = new NodeLocation("", "localhost", DEFAULT_PORT, DEFAULT_REST_PORT);

	/**
	 * @param aNodeLocation */
	public static void setConfiguration(NodeLocation aNodeLocation) {
		nodeLocation = aNodeLocation;
	}

	public static NodeLocation getNodeLocation() {
		return nodeLocation;
	}

	public static String getNodeName() {
		return nodeLocation.getName();
	}

	public static String getLocalIP() {
		return nodeLocation.getHost();
	}

	public static int getLocalPort() {
		return nodeLocation.getMainPort();
	}

	public boolean isQosActivated() {
		return qosActivated;
	}

	public void setQosActivated(boolean qosActivated) {
		this.qosActivated = qosActivated;
	}

	/**
	 * This is the only way to access the singleton instance. Provides well-known
	 * access point to singleton NodeManager
	 * 
	 * @return A reference to the singleton.
	 */
	public static NodeManager instance() {
		if (singleton == null)
			singleton = new NodeManager();
		return singleton;
	}

	/**
	 * Starts the Node Manager Prevents direct instantiation of the event service
	 */
	private NodeManager() {
		notifier = new Notifier();
		space = new Space(notifier);
		networkDeliveryManager = new NetworkDeliveryManager();
		networkReceiverManager = new NetworkReceiverManager(qosActivated);
		networkReceiverManager.start();
		runner = new SpaceRunner(this);
		new Thread(runner).start();
	}

	/**
	 * Retrieves the local Operation Manager
	 * 
	 * @return the local Operation Manager
	 */
	public OperationManager getOperationManager() {
		return runner.getOperationManager();
	}

	/**
	 * Retireves the local Notifier
	 * 
	 * @return the local Notifier
	 */
	public Notifier getNotifier() {
		return notifier;
	}

	/**
	 * Retrieves a reference to the network delivery manager.
	 * 
	 * @return the reference to the network delivery manager
	 */
	public NetworkDeliveryManager getNetworkDeliveryManager() {
		return networkDeliveryManager;
	}

	/**
	 * Returns the space associated with this host.
	 * 
	 * @return The Name of this Node
	 */
	public Space getSpace() {
		return space;
	}

	/**
	 * Stops the local Sapere Node.
	 */
	public void stopServices() {
		runner.stop();
	}

	public static String getLocationAddress() {
		return nodeLocation.getMainServiceAddress();
	}

	public static boolean isLocal(NodeLocation aNodeLocation) {
		return nodeLocation!=null && nodeLocation.equals(aNodeLocation);
	}

	public static boolean isLsaLocal(Lsa aLsa) {
		return isLocal(aLsa.getAgentAuthentication().getNodeLocation());
	}

	public static int getDistance(NodeLocation aNodeLocation) {
		return networkReceiverManager.getDistance(aNodeLocation.getMainServiceAddress());
	}


	public static NodeLocation getLocationByName(String name) {
		if(nodeLocation.getName().equals(name)) {
			return nodeLocation;
		}
		return networkReceiverManager.getLocationByName(name);
	}

	public static Map<String, Integer> getMapDistanceByNode() {
		return networkReceiverManager.getMapDistanceByNode();
	}

	public static int getAllNodesCount() {
		return networkReceiverManager.getAllNodesCount();
	}

	public static Map<String, NodeLocation> getMapLocationsByNode(boolean addCurrentNode) {
		return networkReceiverManager.getMapLocationsByNode(addCurrentNode);
	}

	public static Collection<NodeLocation> getAllLocations(boolean addCurrentNode) {
		Collection<NodeLocation> result = new ArrayList<NodeLocation>();
		Collection<NodeLocation> allLocations = networkReceiverManager.getAllLocations();
		for (NodeLocation nextlocation : allLocations) {
			if (addCurrentNode || !nodeLocation.getName().equals(nextlocation.getName())) {
				result.add(nextlocation);
			}
		}
		return result;
	}

	public static void clearMapDistance() {
		networkReceiverManager.clearMapDistance();
	}
}
