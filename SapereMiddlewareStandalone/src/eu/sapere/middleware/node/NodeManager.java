package eu.sapere.middleware.node;

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

	/** The Network Delivery Manager */
	public NetworkDeliveryManager networkDeliveryManager = null;
	/** The Network Delivery Manager */
	private NetworkReceiverManager networkReceiverManager = null;
	/** The local LSA space */
	private Space space = null;
	/** The local Notifier */
	private Notifier notifier = null;
	/** The SpaceRunner of the Node */
	private SpaceRunner runner = null;

	/**
	 * This is a reference to the one and only instance of this singleton object.
	 */
	private static NodeManager singleton = null;
	/** Sleep time */
	public static final long SLEEPTIME = 1000;

	private static String nodeName;
	private static String localIP;
	/** Port */
	private static int localPort = DEFAULT_PORT;

	public static void setConfiguration(String _nodeName, String _localIP, int _localPort) {
		nodeName = _nodeName;
		localIP = _localIP;
		localPort = _localPort;
	}

	public static String getNodeName() {
		return nodeName;
	}

	public static String getLocalIP() {
		return localIP;
	}

	public static int getLocalPort() {
		return localPort;
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
		networkReceiverManager = new NetworkReceiverManager();
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

	public static String getLocation() {
		return localIP + ":" + localPort;
	}

}
