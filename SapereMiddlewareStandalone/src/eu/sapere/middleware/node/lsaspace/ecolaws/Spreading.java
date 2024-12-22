package eu.sapere.middleware.node.lsaspace.ecolaws;

import java.util.Date;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.SpreadingEvent;

/**
 * The Spreading eco-law implementation.
 * 
 */
public class Spreading extends AbstractEcoLaw {

	/**
	 * Creates a new instance of the Spreading eco-law.
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
	public Spreading(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager) {
		super(space, opManager, notifier, networkDeliveryManager);
	}

	public void invoke() {
		boolean logTime = false;
		long timeBegin = new Date().getTime();
		if(logTime) {
			MiddlewareLogger.getInstance().info("Spreading.begin");
		}
		for (Lsa lsa : getLSAs().values()) {
			//System.out.println(lsa.toVisualString());
			long timeBegin2 = new Date().getTime();
			if (lsa.hasSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP)
					&& !lsa.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)
					&& lsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) { // propagate
				doGradientSpreading(lsa);
			}
			if (lsa.hasSyntheticProperty(SyntheticPropertyName.DESTINATION)
					&& lsa.getSyntheticProperty(SyntheticPropertyName.DIFFUSE).equals("1")) { // send direct
				doDirectSpreading(lsa, lsa.getSyntheticProperty(SyntheticPropertyName.DESTINATION).toString());
			}
			if(logTime) {
				long timeEnd2 = new Date().getTime();
				long timeSpentSec2 = timeEnd2 - timeBegin2;
				MiddlewareLogger.getInstance().info("Spreading of LSA " + lsa.getAgentName() + " : time spend (MS) = " + timeSpentSec2);
			}
		}
		long timeEnd = new Date().getTime();
		long timeSpentSec = (timeEnd - timeBegin);
		if(timeEnd > 2 || logTime) {
			MiddlewareLogger.getInstance().info("Spreading.end : time spend (MS) = " + timeSpentSec);
		}
	}

	/**
	 * Propagate an LSA using gradient Spreading
	 * 
	 * @param an_lsa
	 *            the LSA to propagate.
	 */
	private void doGradientSpreading(Lsa deliverLsa) {
		/*
		if(!deliverLsa.hasSyntheticProperty(SyntheticPropertyName.LAST_SENDING)) {
			deliverLsa.addSyntheticProperty(SyntheticPropertyName.LAST_SENDING, new Date());
		}*/
		getNetworkDeliveryManager().doSpread(lsaCopy(deliverLsa));
		deliverLsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0"); // don't propagate
		SpreadingEvent spreadingEvent = new SpreadingEvent(deliverLsa);
		spreadingEvent.setRequiringAgent(deliverLsa.getAgentName());
		publish(spreadingEvent);
	}

	/**
	 * Propagate an LSA
	 * 
	 * @param an_lsa
	 *            the LSA to propagate.
	 */
	private void doDirectSpreading(Lsa deliverLsa, String ip) {
		/*
		if(!deliverLsa.hasSyntheticProperty(SyntheticPropertyName.LAST_SENDING)) {
			deliverLsa.addSyntheticProperty(SyntheticPropertyName.LAST_SENDING, new Date());
		}*/
		getNetworkDeliveryManager().sendTo(lsaCopy(deliverLsa), ip);
		deliverLsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "0"); // don't propagate
		SpreadingEvent spreadingEvent = new SpreadingEvent(deliverLsa);
		spreadingEvent.setRequiringAgent(deliverLsa.getAgentName());
		publish(spreadingEvent);
	}

	/**
	 * Creates a copy of an LSA to be propagated, removing the id, bonds and other
	 * properties.
	 * 
	 * @param an_lsa
	 *            the LSA to be copied.
	 * @param the_finalHop
	 *            true if this is the final hop for the LSA, false otherwise.
	 * @return the LSA copy.
	 */
	private Lsa lsaCopy(Lsa lsa) {
		Lsa copy = lsa.copy();
		return copy;
	}

}