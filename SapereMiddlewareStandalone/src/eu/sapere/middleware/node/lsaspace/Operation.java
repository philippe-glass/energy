package eu.sapere.middleware.node.lsaspace;

import java.io.Serializable;
import eu.sapere.middleware.agent.Agent;
import eu.sapere.middleware.lsa.Lsa;

/**
 * Represents an operation submitted to the LSA space.
 * 
 */
public class Operation implements Serializable {

	private static final long serialVersionUID = -5118578211958268938L;
	private Lsa lsa = null;
	private OperationType opType = null;
	private Agent requestingAgent = null;
	public String query;
	public int reward;
	public double maxQst1;

	public double getMaxQst1() {
		return maxQst1;
	}

	/**
	 * Gets the Lsa involved in the Operation
	 * 
	 * @return the lsa
	 */
	public Lsa getLsa() {
		return lsa;
	}

	/**
	 * Sets the lsa involved in the Operation
	 * 
	 * @param lsa
	 *            the lsa to set
	 */
	public void setLsa(Lsa lsa) {
		this.lsa = lsa;
	}

	public int getReward() {
		return reward;
	}

	/**
	 * Sets the Id of the lsa involved in the Operation
	 * 
	 * @param lsaAgentName
	 *            the lsaId of the involved Lsa
	 */
	public void setLsaAgentName(String lsaAgentName) {
		this.lsa.setAgentName(lsaAgentName);
	}

	/**
	 * Gets the type of the Operation
	 * 
	 * @return opType the type of the Operation
	 */
	public OperationType getOpType() {
		return opType;
	}

	/**
	 * Sets the type of the Operation
	 * 
	 * @param opType
	 *            the type of the Operation
	 */
	public void setOpType(OperationType opType) {
		this.opType = opType;
	}

	/**
	 * Gets the reference to the Agent that requested the Operation
	 * 
	 * @return the reference to the Agent that requested the Operation
	 */
	public Agent getRequestingAgent() {
		return requestingAgent;
	}

	/**
	 * An Operation to inject an LSA in the local space
	 * 
	 * @param lsa
	 *            the LSA to be injected
	 * @param requestingAgent
	 * @return the reference to the Operation
	 */
	public Operation injectOperation(Lsa lsa, Agent requestingAgent) {
		this.opType = OperationType.INJECT;
		this.lsa = lsa;
		this.requestingAgent = requestingAgent;
		return this;
	}

	/**
	 * An Operation to update the content of an LSA in the local space
	 * 
	 * @param lsa
	 *            the new LSA content
	 * @param requestingAgent
	 * @return the reference to the Operation
	 */
	public Operation updateOperation(Lsa lsa, Agent requestingAgent) {
		this.opType = OperationType.UPDATE;
		this.lsa = lsa;
		this.requestingAgent = requestingAgent;
		return this;
	}

	/**
	 * An Operation to remove a LSA from the local space
	 * 
	 * @param lsa
	 * @param requestingAgent
	 * @return the reference to the Operation
	 */
	public Operation removeOperation(Lsa lsa, Agent requestingAgent) {
		this.opType = OperationType.REMOVE;
		this.requestingAgent = requestingAgent;
		this.lsa = lsa;
		return this;
	}

	/**
	 * An Operation to reward a LSA
	 * 
	 * @param lsa
	 * @param requestingAgent
	 * @param query
	 * @param reward
	 * @param maxQst1 
	 * @return the reference to the Operation
	 */
	public Operation rewardOperation(Lsa lsa, Agent requestingAgent, String query, int reward, double maxQst1) {
		this.opType = OperationType.REWARD;
		this.requestingAgent = requestingAgent;
		this.lsa = lsa;
		this.query = query;
		this.reward = reward;
		this.maxQst1 = maxQst1;
		return this;
	}

}