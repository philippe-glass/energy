package eu.sapere.middleware.agent;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.notifier.AbstractSubscriber;

/**
 * Abstract class that represents a Sapere Agent
 * 
 */
public abstract class Agent extends AbstractSubscriber implements Serializable {

	private static final long serialVersionUID = -4461048586246658298L;

	protected String agentName = null;
	protected AgentAuthentication authentication = null;
	/** The managed LSA */
	protected Lsa lsa;
	//private QoS qos;
	private double epsilon = 0.2; // greedy policy 0.2
	private Map<String, int[]> R; // Reward lookup
	private Map<String, Double[]> Q; // Q learning
	private final double alpha = 0.3; // Learning rate 0.3
	private final double gamma = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future
	private final int actionsCount = 2; // Non react, React, -Spread
	private Random rand;
    //private static DecimalFormat df = new DecimalFormat("0.00");
    protected int debugLevel = 0;

	/**
	 * @param agentName       The name of this Agent
	 * @param _authentication 
	 * @param subdescriptions
	 * @param propertiesName
	 * @param type
	 */
	public Agent(String agentName, AgentAuthentication _authentication, String[] subdescriptions, String[] propertiesName, LsaType type) {
		this.agentName = agentName;
		this. authentication = _authentication;
		lsa = new Lsa("");
		//Random r = new Random();
		//double randomValue =  3.0 * r.nextDouble();
		lsa.addSyntheticProperty(SyntheticPropertyName.OUTPUT, String.join(",", propertiesName));
		lsa.addSyntheticProperty(SyntheticPropertyName.STATE, "");
		lsa.addSyntheticProperty(SyntheticPropertyName.TYPE, type);
	//	lsa.addSyntheticProperty(SyntheticPropertyName.QOS, df.format(randomValue));
		lsa.addSubDescription(subdescriptions);
		R = new HashMap<String, int[]>();
		Q = new HashMap<String, Double[]>();
		rand = new Random();
		//qos = new QoS();
	}

	public int[] getRewardState(String state) {
		return R.get(state);
	}

	public void addPreviousReward(String state, int[] previousReward) {
		if (!Q.containsKey(state)) {
			Q.put(state, new Double[] { 0.0, 5.0, 0.0 });
		}
		R.put(state, previousReward);
	}

	public void addState(String state, int action, int reward, double maxQSt1) {
		System.out.println(state + " , "+action+" updated by "+reward + "- "+maxQSt1);
		if (!R.containsKey(state)) {
			R.put(state, new int[] { 0, 0, 0 });
			Q.put(state, new Double[] { 0.0, 5.0, 0.0 });
		}
		if (action == 0 ) // NReact
		{
			R.get(state)[0] = 10;
			updateQ(state, action, 0);
		} else if (action == 1) { // React
			if (reward > 0) { // check
				R.get(state)[0] = -10; // update reward function
				R.get(state)[1] = reward; // update reward function
			} else {
				R.get(state)[0] = 10;
				R.get(state)[1] = reward;
			}
			updateQ(state, 1, maxQSt1);
			updateQ(state, 0, 0);
			//updateQ(state, 0, getBestActionQvalue(state));
		}
	}


	public int getActionToTake(String crtState) {
		if (!R.containsKey(crtState)) {
			R.put(crtState, new int[] { 0, 0, 0 });
			Q.put(crtState, new Double[] { 0.0, 5.0, 0.0 });
		}
		int action = 0; // NReact
		double max = -100.0;
		if (rand.nextDouble() > epsilon) {
			for (int i = 0; i < 2; i++) { // replace 2 with actionsCount
				if (Q.get(crtState) != null && Q.get(crtState)[i] >= max) {
					max = Q.get(crtState)[i];
					action = i;
				}
			}
		} else {
			if(Q.get(crtState)[0]==0) {
				action=1;}
			else {
			action = rand.nextInt(2);}
		}
		if(debugLevel>2) {
			System.out.println("from " + this.agentName + "State " + crtState + " ->action to take: " + action);
		}
		return action;
	}

	protected double getBestActionQvalue(String crtState) {
		double qValue = -100;
		for (int i = 0; i < actionsCount; i++) {
			if (Q.get(crtState) != null && Q.get(crtState)[i] > qValue) {
				qValue = Q.get(crtState)[i];
			}
		}
		return qValue;
	}

	private void updateQ(String crtState, int crtAction, double maxQSt1) {
		double q = Q.get(crtState)[crtAction];
		int r = R.get(crtState)[crtAction];

		double value = (1 - alpha) * q + alpha * (r + gamma * maxQSt1);
		Q.get(crtState)[crtAction] = value;
		System.out.println(q + " - "+ r + " - "+ maxQSt1);

	}

	public void printQ() {
		System.out.println("**** Q - " + agentName + " ****");
		for (Map.Entry<String, Double[]> row : Q.entrySet()) {
			System.out.println(row.getKey() + " " + Arrays.toString(row.getValue()));
		}
	}

	public void printR() {
		System.out.println("**** R - " + agentName + " ****");
		for (Map.Entry<String, int[]> row : R.entrySet()) {
			System.out.println(row.getKey() + " " + Arrays.toString(row.getValue()));
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

	public Map<String, Double[]> getQ() {
		return Q;
	}

	public void setQ(Map<String, Double[]> q) {
		Q = q;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}

	public AgentAuthentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(AgentAuthentication authentication) {
		this.authentication = authentication;
	}
	

}