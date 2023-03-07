package eu.sapere.middleware.node.lsaspace.ecolaws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.values.AbstractAggregationOperator;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;

/**
 * LSA that wants to be propagated MUST have the following fields defined in
 * PropertyValue: aggregation_op: Aggregation Operators aggregation_by: Field to
 * which the Aggregation Operator will be applied source: an id to identify the
 * LSAs to be aggregated together
 *
 * @author Gabriella Castelli (UNIMORE)
 * @author Graeme Stevenson (STA)
 */
public class Aggregation extends AbstractEcoLaw {

	public static final int RULE_REPLACE_LSA = 1;
	public static final int RULE_REPLACE_PROPERY = 2;
	public static final int RULE_CREATE_NEW_PROPERTY = 3;

	public int aggrationRule = RULE_CREATE_NEW_PROPERTY;

	/**
	 * Creates a new instance of the aggregation eco-law.
	 *
	 * @param space
	 *            The space in which the eco-law executes.
	 * @param opManager
	 *            The OperationManager that manages operations in the space
	 * @param notifier
	 *            The Notifier that notifies agents whith events happening to LSAs
	 * @param _aggrationRule
	 * 			variant of aggregation rule
	 * @param networkDeliveryManager
	 *            The interface for Network Delivery of LSAs
	 */
	public Aggregation(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager, int _aggrationRule) {
		super(space, opManager, notifier, networkDeliveryManager);
		this.aggrationRule = _aggrationRule;
	}

	protected boolean getGenerateNewPropertyy() {
		return (aggrationRule == RULE_CREATE_NEW_PROPERTY);
	}

	protected boolean getReduceLsaList() {
		return (aggrationRule == RULE_REPLACE_LSA);
	}

	/**
	 * {@inheritDoc}
	 */
	public void invoke() {
		selfAggregation();
		otherAggregation();
	}

	private void selfAggregation() {
		List<String> sourceProcessed = new ArrayList<String>();
		List<Lsa> toRemove = new ArrayList<Lsa>();
		for (Lsa lsa : getLSAs().values()) {
			if (lsa.getAggregationSource() != null && lsa.explicitAggregationApplies()
					&& !sourceProcessed.contains(lsa.getAggregationSource())) {
				sourceProcessed.add(lsa.getAggregationSource());
				List<Lsa> compatible = getAggregationCompatibleLsaI(lsa, getLSAs());
				if (compatible.size() >= 2) {
					executeAggregator(lsa, compatible, getGenerateNewPropertyy());
					if(getReduceLsaList()) {
						AbstractAggregationOperator aggregationOperator = lsa.getAggregationOperator();
						String customizedAggregationOp = lsa.getCustomizedAggregationOp();
						Lsa newLsa = aggregationOperator.applyInNewLsa(compatible,  lsa.getAgentAuthentication(), customizedAggregationOp);
						for (int k = 0; k < compatible.size(); k++) {
							toRemove.add(compatible.get(k));
						}
						inject(newLsa);
					}
				}
			}
		}
		for (int j = 0; j < toRemove.size(); j++) {
			remove(toRemove.get(j));
		}
	}




	protected void otherAggregation() {
		for (Lsa lsa : getLSAs().values()) {
			if (lsa.requestedAggregationApplies()) {
				List<Lsa> compatible = getRequestAggregationCompatibleLsa(lsa, getLSAs());
				executeAggregator(lsa, compatible, getGenerateNewPropertyy());
			}
		}
	}

	protected void executeAggregator(Lsa lsa, List<Lsa> listCompatibleLsa, boolean generateNewProperty) {
		AbstractAggregationOperator aggregationOperator = lsa.getAggregationOperator();
		if(aggregationOperator != null) {
			String pName = lsa.getAggregationBy();
			String customizedAggregationOp = lsa.getCustomizedAggregationOp();
			Object aggregatedValue =  aggregationOperator.apply(listCompatibleLsa, lsa.getAgentAuthentication(), customizedAggregationOp);
			if(aggregatedValue != null) {
				String pName2 = pName + (generateNewProperty ?  "_AGGR" : "");
				lsa.replacePropertyWithName(new Property(pName2, aggregatedValue));
				// Triggers the LsaUpdatedEvent
				AbstractSapereEvent lsaUpdatedEvent = new LsaUpdatedEvent(lsa);
				lsaUpdatedEvent.setRequiringAgent(lsa.getAgentName());
				publish(lsaUpdatedEvent);
			}
		}
	}

	private List<Lsa> getRequestAggregationCompatibleLsa(Lsa lsa, Map<String,Lsa> allLsa) {
		List<Lsa> ret = new ArrayList<Lsa>();
		String pName =  lsa.getAggregationBy();
		for (Lsa secondLsa : allLsa.values()) {
			if (!secondLsa.requestedAggregationApplies() && !secondLsa.explicitAggregationApplies()
					&& !secondLsa.hasAggregationOp()
					&& secondLsa.hasProperty(pName)) {
				ret.add(secondLsa.getCopy());
			}
		}
		return ret;
	}

	private List<Lsa> getAggregationCompatibleLsaI(Lsa lsa, Map<String,Lsa> allLsa) {
		List<Lsa> result = new ArrayList<Lsa>();
		// this option gives the possibilty to aggregate LSA from other nodes
		boolean aggregateAllNodes = lsa.getAggregationAllNodes();
		for (Lsa secondLsa : allLsa.values()) {
			if (	secondLsa.aggregationApplies()
				&& !secondLsa.getId().toString().equals(lsa.getId().toString())
				&& (aggregateAllNodes || secondLsa.getAggregationSource().equals(lsa.getAggregationSource()))) {
					result.add(secondLsa.getCopy());
			}
		}
		result.add(lsa);
		return result;
	}

}
