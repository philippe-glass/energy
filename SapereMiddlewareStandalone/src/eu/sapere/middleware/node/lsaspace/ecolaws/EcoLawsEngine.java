package eu.sapere.middleware.node.lsaspace.ecolaws;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;

/**
 * Manages the execution of eco-laws.
 * 
 */
public class EcoLawsEngine {

	private final List<IEcoLaw> sapereEcoLaws = new ArrayList<IEcoLaw>();

	/**
	 * Creates an instance of the eco-laws engine that manages the execution of
	 * eco-laws.
	 * 
	 * @param space
	 *            The space in which the eco-law executes.
	 * @param opManager
	 *            The OperationManager that manages operations in the space
	 * @param notifier
	 *            The Notifier that notifies agents with events happening to LSAs
	 * @param networkDeliveryManager
	 *            The interface for Network Delivery of LSAs
	 */
	public EcoLawsEngine(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager) {
		// Add the eco-laws in execution order.
		sapereEcoLaws.add(new Decay(space, opManager, notifier, networkDeliveryManager));
		sapereEcoLaws.add(new Bonding(space, opManager, notifier, networkDeliveryManager));
		sapereEcoLaws.add(new Spreading(space, opManager, notifier, networkDeliveryManager));
		sapereEcoLaws.add(new Aggregation(space, opManager, notifier, networkDeliveryManager, Aggregation.RULE_CREATE_NEW_PROPERTY));
	}

	/**
	 * Launches the ordered execution of eco-laws.
	 */
	public void exec() {
		long startTime = new Date().getTime();
		for (IEcoLaw law : sapereEcoLaws) {
			law.invoke();
		}
		long endTime = new Date().getTime();
		MiddlewareLogger.getInstance().info("EcoLawsEngine.exec spentTime (MS) = " + (endTime - startTime));
	}
}
