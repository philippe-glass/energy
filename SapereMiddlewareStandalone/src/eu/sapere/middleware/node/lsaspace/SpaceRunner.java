package eu.sapere.middleware.node.lsaspace;

import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.lsaspace.ecolaws.EcoLawsEngine;

/**
 * Manages the local LSA space life circle
 * 
 */
public class SpaceRunner implements Runnable {

	private boolean stop = false;
	/** The Operation Manager */
	private OperationManager operationManager = null;
	/** The eco-laws engine */
	private EcoLawsEngine ecoLawsEngine = null;

	/**
	 * Creates an instance of the SpaceRunner and initializes the OperationManager
	 * and the Ecolaws Engine
	 * 
	 * @param node
	 *            a reference to the local Node
	 */
	public SpaceRunner(NodeManager node) {
		operationManager = new OperationManager(node.getSpace(), node.getNotifier());
		ecoLawsEngine = new EcoLawsEngine(node.getSpace(), operationManager, node.getNotifier(),
				node.getNetworkDeliveryManager());
	}


	@Override
	public void run() {
		while (!stop) {
			operationManager.exec();
			ecoLawsEngine.exec();
			try {
				Thread.sleep(NodeManager.SLEEPTIME);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
	}

	/**
	 * @return true if the SpaceRunner is stopped, false otherwise
	 */
	public boolean isStop() {
		return stop;
	}

	/**
	 * Stops the SpaceRunner
	 */
	public void stop() {
		this.stop = true;
	}

	/**
	 * @return a reference to the OperationManager
	 */
	public OperationManager getOperationManager() {
		return operationManager;
	}

	/**
	 * @return a reference to the EcoLawsEngine
	 */
	public EcoLawsEngine getEcoLawsEngine() {
		return ecoLawsEngine;
	}

}