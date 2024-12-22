package eu.sapere.middleware.agent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import eu.sapere.middleware.log.MiddlewareLogger;

public class AgentLearningData {
	// private QoS qos;
	private String agentName = null;
	private double epsilon = 0.2; // greedy policy 0.2
	private Map<String, int[]> R; // Reward lookup
	private Map<String, Double[]> Q; // Q learning
	private final double alpha = 0.3; // Learning rate 0.3
	private final double gamma = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future
	private final int actionsCount = 2; // Non react, React, -Spread
	private Random rand;
	private int debugLevel = 0;

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public Map<String, int[]> getR() {
		return R;
	}

	public Map<String, Double[]> getQ() {
		return Q;
	}

	public void setQ(Map<String, Double[]> q) {
		Q = q;
	}

	public double getAlpha() {
		return alpha;
	}

	public double getGamma() {
		return gamma;
	}

	public int getActionsCount() {
		return actionsCount;
	}

	public AgentLearningData(String _agentName) {
		super();
		agentName = _agentName;
		R = new HashMap<String, int[]>();
		Q = new HashMap<String, Double[]>();
		rand = new Random();
	}

	public int[] getRewardState(String state) {
		return R.get(state);
	}

	void addPreviousReward(String state, int[] previousReward) {
		MiddlewareLogger.getInstance()
				.info("Agent.addPreviousReward " + this.agentName + ", " + state + " " + previousReward);
		if (!Q.containsKey(state)) {
			Q.put(state, new Double[] { 0.0, 5.0, 0.0 });
		}
		R.put(state, previousReward);
	}

	void addState(String state, int action, int reward, double maxQSt1) {
		MiddlewareLogger.getInstance().info("Agent.addState " + this.agentName + ", " + state + " , " + action
				+ " updated by " + reward + "- " + maxQSt1);
		if (!R.containsKey(state)) {
			R.put(state, new int[] { 0, 0, 0 });
			Q.put(state, new Double[] { 0.0, 5.0, 0.0 });
		}
		if (action == 0) // NReact
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
			// updateQ(state, 0, getBestActionQvalue(state));
		}
	}

	int getActionToTake(String crtState) {
		MiddlewareLogger.getInstance().info("Agent.getActionToTake " + this.agentName + ", " + crtState);
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
			if (Q.get(crtState)[0] == 0) {
				action = 1;
			} else {
				action = rand.nextInt(2);
			}
		}
		if (debugLevel > 2) {
			MiddlewareLogger.getInstance()
					.info("from " + this.agentName + "State " + crtState + " ->action to take: " + action);
		}
		return action;
	}

	double getBestActionQvalue(String crtState) {
		double qValue = -100;
		for (int i = 0; i < actionsCount; i++) {
			if (Q.get(crtState) != null && Q.get(crtState)[i] > qValue) {
				qValue = Q.get(crtState)[i];
			}
		}
		return qValue;
	}

	void updateQ(String crtState, int crtAction, double maxQSt1) {
		double q = Q.get(crtState)[crtAction];
		int r = R.get(crtState)[crtAction];

		double value = (1 - alpha) * q + alpha * (r + gamma * maxQSt1);
		Q.get(crtState)[crtAction] = value;
		MiddlewareLogger.getInstance().info("Agent.updateQ " + q + " - " + r + " - " + maxQSt1);
	}

	void printQ() {
		MiddlewareLogger.getInstance().info("**** Q - " + agentName + " ****");
		for (Map.Entry<String, Double[]> row : Q.entrySet()) {
			MiddlewareLogger.getInstance().info(row.getKey() + " " + Arrays.toString(row.getValue()));
		}
	}

	public void printR() {
		MiddlewareLogger.getInstance().info("**** R - " + agentName + " ****");
		for (Map.Entry<String, int[]> row : R.entrySet()) {
			MiddlewareLogger.getInstance().info(row.getKey() + " " + Arrays.toString(row.getValue()));
		}
	}

}
