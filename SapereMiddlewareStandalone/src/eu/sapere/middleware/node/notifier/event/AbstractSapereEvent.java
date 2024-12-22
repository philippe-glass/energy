package eu.sapere.middleware.node.notifier.event;

import java.io.Serializable;

import eu.sapere.middleware.lsa.Lsa;

public abstract class AbstractSapereEvent implements Serializable {

	private static final long serialVersionUID = 1037415768070620266L;
	private String requiringAgent = null;
	protected Lsa lsa = null;


	/**
	 * Gets the requiring Agent
	 * 
	 * @return the requiringAgent
	 */
	public String getRequiringAgent() {
		return requiringAgent;
	}

	/**
	 * Sets the requiring Agent
	 * 
	 * @param requiringAgent
	 *            the requiringAgent
	 */
	public void setRequiringAgent(String requiringAgent) {
		this.requiringAgent = requiringAgent;
	}

	public Lsa getLsa() {
		return lsa;
	}

	public void setLsa(Lsa lsa) {
		this.lsa = lsa;
	}

}
