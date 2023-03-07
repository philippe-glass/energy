package eu.sapere.middleware.node.lsaspace.ecolaws;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;
import eu.sapere.middleware.node.notifier.event.BondEvent;

public class Bonding extends AbstractEcoLaw {

	/**
	 * Creates a new instance of the bonding eco-law.
	 * 
	 * @param space                  The space in which the eco-law executes.
	 * @param opManager              The OperationManager that manages operations in
	 *                               the space
	 * @param notifier               The Notifier that notifies agents with events
	 *                               happening to LSAs
	 * @param networkDeliveryManager The interface for Network Delivery of LSAs
	 */
	public Bonding(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager) {
		super(space, opManager, notifier, networkDeliveryManager);
	}

	@Override
	public void invoke() {
		execBondsFromLSA();
	}

	private void execBondsFromLSA() {
		for (Lsa outerLsa : getLSAs().values()) {
			if (!outerLsa.isSubdescriptionEmpty()) {
				for (Lsa targetLsa : getLSAs().values()) {
					if (syntactic_match(outerLsa, targetLsa) && outerLsa.shouldBound(targetLsa)
							&& !targetLsa.checkNullPropertiesByQuery(
									targetLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString())
							&& !targetLsa.getProperties().isEmpty() && !outerLsa.getAgentName().contains("*")) {
						bondLSAToLSA(outerLsa, targetLsa);
					}
				}
			}
		}
	}

	/**
	 * check if input matches with at least one output
	 * 
	 * @param outerLsa
	 * @param targetLsa
	 * @return
	 */
	public boolean syntactic_match(Lsa outerLsa, Lsa targetLsa) {
		for (String subdesc : outerLsa.getSubDescription()) {
			if (targetLsa.hasSyntheticProperty(SyntheticPropertyName.OUTPUT)) {
				for (String s : targetLsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT).toString().split(",")) {
					if (s.equals(subdesc)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean semantic_match(Lsa outerLsa, Lsa targetLsa) {
		// TODO
		return false;
	}

	private void bondLSAToLSA(Lsa outerLsa, Lsa targetLsa) {

		outerLsa.addSyntheticProperty(SyntheticPropertyName.BOND, targetLsa.getAgentName());
		if (!outerLsa.getSyntheticProperty(SyntheticPropertyName.TYPE).equals(LsaType.Query)) {
			outerLsa.addSyntheticProperty(SyntheticPropertyName.QUERY,
					targetLsa.getSyntheticProperty(SyntheticPropertyName.QUERY));
		}

		if (!outerLsa.hasBondedBefore(targetLsa.getAgentName(),
				targetLsa.getSyntheticProperty(SyntheticPropertyName.QUERY).toString())) {
			AbstractSapereEvent lsaBondedEvent = new BondEvent(outerLsa, targetLsa);
			lsaBondedEvent.setRequiringAgent(outerLsa.getAgentName());
			publish(lsaBondedEvent);
		}
	}
}
