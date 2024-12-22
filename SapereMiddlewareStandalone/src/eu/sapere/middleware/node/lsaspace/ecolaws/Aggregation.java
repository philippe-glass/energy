package eu.sapere.middleware.node.lsaspace.ecolaws;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.AggregatorProperty;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.lsa.values.AbstractAggregationOperator;
import eu.sapere.middleware.lsa.values.MapStandardOperators;
import eu.sapere.middleware.node.lsaspace.OperationManager;
import eu.sapere.middleware.node.lsaspace.Space;
import eu.sapere.middleware.node.networking.transmission.NetworkDeliveryManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;
import eu.sapere.middleware.node.notifier.event.AggregationEvent;

/**
 * LSA that wants to be propagated MUST have the following fields defined in
 * PropertyValue: aggregation_op: Aggregation Operators aggregation_by: Field to
 * which the Aggregation Operator will be applied source: an id to identify thse
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
	 * @param aggrationRule
	 * 			variant of aggregation rule
	 * @param networkDeliveryManager
	 *            The interface for Network Delivery of LSAs
	 */
	public Aggregation(Space space, OperationManager opManager, Notifier notifier,
			NetworkDeliveryManager networkDeliveryManager, int aggrationRule) {
		super(space, opManager, notifier, networkDeliveryManager);
		this.aggrationRule = aggrationRule;
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
		boolean logTime = false;
		long timeBegin = new Date().getTime();
		if(logTime) {
			MiddlewareLogger.getInstance().info("Aggregation.begin");
		}
		selfAggregation();
		otherAggregation();
		long timeEnd = new Date().getTime();
		long timeSpentSec = (timeEnd - timeBegin);
		if(timeSpentSec > 1 || logTime) {
			MiddlewareLogger.getInstance().info("Aggregation.end : time spend (MS) = " + timeSpentSec);
		}
	}

	private void selfAggregation() {
		Map<String, List<String>> mapSourceProcessed = new HashMap<String, List<String>>();
		for (Lsa lsa : getLSAs().values()) {
			if(lsa.getAggregationSource() != null && lsa.getSourceDistance() == 0 && lsa.hasAggregation()) {
				List<Lsa> compatible = getAggregationCompatibleLsa(lsa, getLSAs());
				if (compatible.size() >= 2) {
					List<AggregatorProperty> listAggregationProperties = getListAggregationProperties(lsa);
					for(AggregatorProperty aggregationProperty : listAggregationProperties) {
						String propertyName = aggregationProperty.getPropertyName();
						if(!mapSourceProcessed.containsKey(propertyName)) {
							mapSourceProcessed.put(propertyName,new ArrayList<String>());
						}
						List<String> listSourceProcessed = mapSourceProcessed.get(propertyName);
						if (lsa.getAggregationSource() != null
							&& lsa.hasSource()
							&& !listSourceProcessed.contains(lsa.getAggregationSource()))
						{
							List<Lsa> toRemove = new ArrayList<Lsa>();
							// Execute the aggregator
							executeAggregator(lsa, compatible, getGenerateNewPropertyy());
							if(getReduceLsaList()) {
								// Remove the aggregated LSAs
								AbstractAggregationOperator aggregationOperator =  MapStandardOperators.getOperator(aggregationProperty);
								String customizedAggregationOp = aggregationProperty.getOperator();
								Lsa newLsa = aggregationOperator.applyInNewLsa(compatible,  lsa.getAgentAuthentication(), customizedAggregationOp);
								if(newLsa != null) {
									for (int k = 0; k < compatible.size(); k++) {
										toRemove.add(compatible.get(k));
									}
									inject(newLsa);
								}
							}
							for (int j = 0; j < toRemove.size(); j++) {
								remove(toRemove.get(j));
							}
							listSourceProcessed.add(lsa.getAggregationSource());
						}
					}
				}
			}
		}
	}



	protected void otherAggregation() {
		for (Lsa lsa : getLSAs().values()) {
			List<Lsa> compatible = getRequestAggregationCompatibleLsa(lsa, getLSAs());
			if (!lsa.hasSource()) {
				executeAggregator(lsa, compatible, getGenerateNewPropertyy());
			}
			/*
			for(int aggregatorIdx = 0; aggregatorIdx < SyntheticPropertyName.getNbAggregatorPropertyNb(); aggregatorIdx++) {
				if (lsa.requestedAggregationApplies(aggregatorIdx)) {
					executeAggregator(lsa, compatible, getGenerateNewPropertyy());
				}
			}*/
		}
	}

	protected void executeAggregator(Lsa lsa, List<Lsa> listCompatibleLsa, boolean generateNewProperty) {
		List<AggregatorProperty> listAggregationProperties = getListAggregationProperties(lsa);
		for(AggregatorProperty aggregationProperty : listAggregationProperties) {
			AbstractAggregationOperator aggregationOperator = MapStandardOperators.getOperator(aggregationProperty);
			if(aggregationOperator != null) {
				String propertyName = aggregationProperty.getPropertyName();
				if(aggregationProperty != null && aggregationProperty.isCustomized())  {
					String customizedAggregationOp = aggregationProperty.getOperator();
					Object aggregatedValue =  aggregationOperator.apply(listCompatibleLsa, lsa.getAgentAuthentication(), customizedAggregationOp);
					if(aggregatedValue != null) {
						lsa.setAggredatedValue(propertyName, aggregatedValue);
						// Triggers the AggregationEvent
						AbstractSapereEvent aggregationEvent = new AggregationEvent(lsa);
						aggregationEvent.setRequiringAgent(lsa.getAgentName());
						publish(aggregationEvent);
					}
				}
			}
		}
	}

	public static Map<String, AggregatorProperty> getMapAggregationProperties(Lsa lsa) {
		Map<String, AggregatorProperty> result = new HashMap<String, AggregatorProperty>();
		if (lsa.hasSyntheticProperty(SyntheticPropertyName.AGGREGATION)) {
			Object obj = lsa.getSyntheticProperty(SyntheticPropertyName.AGGREGATION);
			if (obj instanceof Map<?, ?>) {
				Map<String, AggregatorProperty> mapProp = (Map<String, AggregatorProperty>) obj;
				return mapProp;
			}
		}
		return result;
	}

	public static List<AggregatorProperty> getListAggregationProperties(Lsa lsa) {
		List<AggregatorProperty> listAggregationProperties = new ArrayList<AggregatorProperty>();
		Map<String, AggregatorProperty> mapAggregationProperties = getMapAggregationProperties(lsa);
		for (AggregatorProperty nextprop : mapAggregationProperties.values()) {
			listAggregationProperties.add(nextprop);
		}
		return listAggregationProperties;
	}

	private List<Lsa> getRequestAggregationCompatibleLsa(Lsa lsa, Map<String,Lsa> allLsa) {
		List<Lsa> ret = new ArrayList<Lsa>();
		List<AggregatorProperty> aggregationProperties = getListAggregationProperties(lsa);
		for (Lsa secondLsa : allLsa.values()) {
			Map<String, AggregatorProperty> mapSeoncdLsaAggreationProperties  = getMapAggregationProperties(secondLsa);
			boolean secondLsaAdded = false;
			for(AggregatorProperty aggregationProperty : aggregationProperties) {
				String propertyName = aggregationProperty.getPropertyName();
				if (
						!secondLsaAdded
						//&& !secondLsa.requestedAggregationApplies(fieldName)
						//&& !secondLsa.explicitAggregationApplies(fieldName)
						//&& !secondLsa.hasAggregationOp(fieldName)
						&& secondLsa.hasProperty(propertyName)
						&& !mapSeoncdLsaAggreationProperties.containsKey(propertyName)
						// check if the LSA is not subject to the Aggregation
						//&& !(mapSeoncdLsaAggreationProperties.containsKey(fieldName) && secondLsa.hasSource())
						// check if the LSA is not subject to other Aggregation
						//&& !(mapSeoncdLsaAggreationProperties.containsKey(fieldName) && !secondLsa.hasSource())
						)  {
					ret.add(secondLsa.copy());
					secondLsaAdded = true;
				}
			}
		}
		return ret;
	}

	private List<Lsa> getAggregationCompatibleLsa(Lsa lsa, Map<String,Lsa> allLsa) {
		List<Lsa> result = new ArrayList<Lsa>();
		Map<String, AggregatorProperty> mapAggregationProperties = getMapAggregationProperties(lsa);
		for (Lsa secondLsa : allLsa.values()) {
			boolean secondLsaAdded = false;
			Map<String, AggregatorProperty> mapSeoncdLsaAggreationProperties  = getMapAggregationProperties(secondLsa);
			//for(AggregatorProperty aggregationProperty : aggregationProperties) {
			for(String propertyName  : mapAggregationProperties.keySet()) {
				AggregatorProperty aggregationProperty = mapAggregationProperties.get(propertyName);
				// this option gives the possibilty to aggregate LSA from other nodes
				boolean aggregateOtherNodes = aggregationProperty.getActivateGossip();
				//String propertyName = aggregationProperty.getPropertyName();
				if (	!secondLsaAdded
					//&& secondLsa.aggregationApplies(fieldName)
					&& mapSeoncdLsaAggreationProperties.containsKey(propertyName)
					&& !secondLsa.getId().toString().equals(lsa.getId().toString())
					&& (aggregateOtherNodes || secondLsa.getAggregationSource().equals(lsa.getAggregationSource()))) {
						result.add(secondLsa.copy());
						secondLsaAdded = true;
				}
			}
		}
		result.add(lsa);
		return result;
	}

}
