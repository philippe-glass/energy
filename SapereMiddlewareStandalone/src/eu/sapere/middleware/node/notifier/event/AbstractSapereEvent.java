package eu.sapere.middleware.node.notifier.event;

import java.io.Serializable;

public abstract class AbstractSapereEvent implements Serializable {

	private static final long serialVersionUID = 1037415768070620266L;
	private String requiringAgent = null;

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

}
