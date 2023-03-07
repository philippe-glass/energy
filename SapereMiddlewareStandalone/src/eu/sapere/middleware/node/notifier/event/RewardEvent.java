package eu.sapere.middleware.node.notifier.event;

import eu.sapere.middleware.lsa.Lsa;

/**
 * An event representing a Bond happened to a LSA
 * 
 */
public class RewardEvent extends AbstractSapereEvent {

	private static final long serialVersionUID = 899040460408932860L;
	private Lsa lsa = null;
	private String query;
	private int reward = 0;
	private double maxQst1;
	/**
	 * Instantiates the Event
	 * 
	 * @param lsa
	 * @param reward 
	 * @param query 
	 * @param maxQst1 
	 */
	public RewardEvent(Lsa lsa, String query, int reward, double maxQst1) { //add privious state
		this.reward = reward;
		this.lsa = lsa;
		this.query = query;
		this.maxQst1 = maxQst1;
	}

	public double getMaxSt1() {
		return maxQst1;
	}

	/**
	 * @return The Lsa
	 */
	public Lsa getLsa() {
		return lsa;
	}

	/**
	 * @return 
	 */
	public int getReward() {
		return reward;
	}

	/**
	 * @return property
	 */
	public String getQuery() {
		return query;
	}
}
