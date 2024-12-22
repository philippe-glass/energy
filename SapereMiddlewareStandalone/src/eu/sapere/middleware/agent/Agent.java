package eu.sapere.middleware.agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.AggregatorProperty;
import eu.sapere.middleware.lsa.AggregatorType;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.lsa.values.StandardAggregationOperator;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.lsaspace.ecolaws.Aggregation;
import eu.sapere.middleware.node.notifier.AbstractSubscriber;

/**
 * Abstract class that represents a Sapere Agent
 * 
 */
public abstract class Agent extends AbstractSubscriber implements Serializable {

	private static final long serialVersionUID = -4461048586246658298L;

	protected String agentName = null;
	/** The managed LSA */
	protected Lsa lsa;
	protected AgentLearningData learningData;
	protected List<Property> waitingProperties = new ArrayList<Property>();

	protected int debugLevel = 0;

	/**
	 * @param agentName       The name of this Agent
	 * @param _authentication
	 * @param subdescriptions
	 * @param propertiesName
	 * @param type
	 * @param activateQoS 
	 */
	public Agent(String agentName, AgentAuthentication _authentication, String[] subdescriptions,
			String[] propertiesName, LsaType type, boolean activateQoS) {
		this.agentName = agentName;
		lsa = new Lsa(_authentication);
		// Random r = new Random();
		// double randomValue = 3.0 * r.nextDouble();
		updateLsaPropertyTags(subdescriptions, propertiesName);
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, type);
		// lsa.addSyntheticProperty(SyntheticPropertyName.QOS, df.format(randomValue));
		if(activateQoS) {
			lsa.addSyntheticProperty(SyntheticPropertyName.STATE, "");
		}
		if (activateQoS) {
			learningData = new AgentLearningData(this.agentName);
		} else {
			learningData = null;
		}
		// qos = new QoS();
	}

	public void reInitializeLsa(String[] subdescriptions, String[] propertiesName, LsaType type) {
		lsa.removeContent();
		updateLsaPropertyTags(subdescriptions, propertiesName);
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, type);
	}

	public boolean isQoSactivated() {
		return learningData != null;
	}

	public int[] getRewardState(String state) {
		if (learningData == null) {
			return new int[0];
		} else {
			return learningData.getRewardState(state);
		}
	}

	public void addPreviousReward(String state, int[] previousReward) {
		if (learningData != null) {
			learningData.addPreviousReward(state, previousReward);
		}
	}

	public void addState(String state, int action, int reward, double maxQSt1) {
		if (learningData != null) {
			learningData.addState(state, action, reward, maxQSt1);
		}
	}

	public int getActionToTake(String crtState) {
		if (learningData == null) {
			return -1;
		} else {
			return learningData.getActionToTake(crtState);
		}
	}

	protected double getBestActionQvalue(String crtState) {
		if (learningData == null) {
			return -1;
		} else {
			return learningData.getBestActionQvalue(crtState);
		}
	}

	public void printQ() {
		if (learningData != null) {
			learningData.printQ();
		}
	}

	public void printR() {
		if (learningData != null) {
			learningData.printR();
		}
	}

	public Map<String, Double[]> getQ() {
		if (learningData == null) {
			return new HashMap<String, Double[]>();
		} else {
			return learningData.getQ();
		}
	}

	public void setQ(Map<String, Double[]> q) {
		if (learningData != null) {
			learningData.setQ(q);
		}
	}

	public double getEpsilon() {
		if (learningData == null) {
			return -1.0;
		} else {
			return learningData.getEpsilon();
		}
	}

	public void setEpsilon(double epsilon) {
		if (learningData != null) {
			learningData.setEpsilon(epsilon);
		}
	}

	public Lsa getLsa() {
		return lsa;
	}

	public void setLsa(Lsa lsa) {
		this.lsa = lsa;
	}

	public String getAgentName() {
		return agentName;
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}

	public AgentAuthentication getAuthentication() {
		return lsa.getAgentAuthentication();
	}

	public void addProperty(Property propertyToAdd) {
		if (lsa.getProperties().size() >= Lsa.PROPERTIESSIZE) {
			waitingProperties.add(propertyToAdd);
			MiddlewareLogger.getInstance().info(this.agentName + " addProperty : cannot post property " + propertyToAdd.getValue()
					+ " in lsa. Put it waiting queue.");
		} else {
			lsa.addProperty(propertyToAdd);
		}
	}

	protected void checkWaitingProperties() {
		while (this.waitingProperties.size() > 0 && lsa.getProperties().size() < Lsa.PROPERTIESSIZE) {
			Property prop = waitingProperties.remove(0);
			MiddlewareLogger.getInstance().info("checkWaitingProperties " + this.agentName + " : submit waiting property in LSA : " + prop);
			lsa.addProperty(prop);
		}
		if (this.waitingProperties.size() > 0) {
			MiddlewareLogger.getInstance().info("checkWaitingProperties : prwaitingPropertiesopertiesToPost.size = " + waitingProperties.size());
			for (Property nextProp : waitingProperties) {
				MiddlewareLogger.getInstance().info("checkWaitingProperties : nextProp is still waiting : " + nextProp.getValue());
			}
		}
	}

	public void replacePropertyWithName(Property prop) {
		String propName = prop.getName();
		if (lsa.hasProperty(propName)) {
			lsa.removePropertiesByName(propName);
		}
		addProperty(prop);
	}

	public void activateSpreading() {
		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP, "1");
	}

	/**
	 * @param nHop The number of hops
	 */
	public void addGradient(int nHop) {
		lsa.addSyntheticProperty(SyntheticPropertyName.GRADIENT_HOP, nHop + "");
		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocationAddress());
	}

	/**
	 * @param lsa
	 * @param ip
	 */
	public void sendTo(Lsa lsa, String ip) {
		lsa.addSyntheticProperty(SyntheticPropertyName.DESTINATION, ip);
		lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "1");
		lsa.addSyntheticProperty(SyntheticPropertyName.PREVIOUS, NodeManager.getLocationAddress());
	}

	/**
	 * Adds a decay property
	 *
	 * @param decayValue The initial decay value
	 */
	public void addDecay(int decayValue) {
		lsa.addSyntheticProperty(SyntheticPropertyName.DECAY, decayValue + "");
	}

	public void addStandardAggregation(StandardAggregationOperator aggregationOperator, String fieldName, boolean aggregateAllNodes) {
		Map<String, AggregatorProperty> mapAggregationProperties = Aggregation.getMapAggregationProperties(lsa);
		AggregatorProperty aggregatorProperty = aggregationOperator.getProperty();
		mapAggregationProperties.put(fieldName, aggregatorProperty);
		lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION, mapAggregationProperties);
	}

	public void addCustomizedAggregation(String customizedAggregationOp, String fieldName, boolean aggregateAllNodes) {
		Map<String, AggregatorProperty> mapAggregationProperties = Aggregation.getMapAggregationProperties(lsa);
		AggregatorProperty newAggregatorProperty = new AggregatorProperty(
				customizedAggregationOp, AggregatorType.CUSTOMIZED, fieldName, aggregateAllNodes);
		mapAggregationProperties.put(fieldName, newAggregatorProperty);
		lsa.addSyntheticProperty(SyntheticPropertyName.AGGREGATION, mapAggregationProperties);
	}

	public String[] getInput() {
		List<String> subDescription = lsa.getSubDescription();
		String[] input = new String[subDescription.size()];
		for(int i = 0; i < subDescription.size(); i++) {
			input[i] = subDescription.get(i);
		}
		return input;
	}

	public String[] getOutput() {
		String sOuputs = "" + lsa.getSyntheticProperty(SyntheticPropertyName.OUTPUT);
		String[] ouputs = sOuputs.split(",");
		return ouputs;
	}

	public String getUrl() {
		String url = lsa.getAgentAuthentication().getNodeLocation().getMainServiceAddress();
		return url;
	}

	protected boolean hasTargetedProperty(Lsa bondedLsa) {
		return lsa.hasTargetedProperty(bondedLsa);
	}

	public void updateLsaPropertyTags(String[] lsaInputTags, String[] lsaOutputTags) {
		lsa.getSubDescription().clear();
		lsa.addSubDescription(lsaInputTags);
		lsa.addSyntheticProperty(SyntheticPropertyName.OUTPUT, String.join(",", lsaOutputTags));
	}
}