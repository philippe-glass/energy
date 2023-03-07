package eu.sapere.middleware.agent;

import java.io.Serializable;

public class AgentAuthentication implements Serializable {
	private static final long serialVersionUID = 1784L;
	private String agentName;
	private String agentType;
	private String authenticationKey;
	private String agentNode;
	private String agentLocation;

	public AgentAuthentication(String agentName, String agentType, String authenticationKey, String agentNode,
			String _agentLocation) {
		super();
		this.agentName = agentName;
		this.agentType = agentType;
		this.authenticationKey = authenticationKey;
		this.agentNode = agentNode;
		this.agentLocation = _agentLocation;
	}

	public String getAgentName() {
		return agentName;
	}

	public String getAgentType() {
		return agentType;
	}

	public String getAuthenticationKey() {
		return authenticationKey;
	}

	public String getAgentNode() {
		return agentNode;
	}

	public String getAgentLocation() {
		return agentLocation;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public void setAgentType(String agentType) {
		this.agentType = agentType;
	}

	public void setAuthenticationKey(String authenticationKey) {
		this.authenticationKey = authenticationKey;
	}

	public void setAgentNode(String agentNode) {
		this.agentNode = agentNode;
	}

	public void setAgentLocation(String _agentLocation) {
		this.agentLocation = _agentLocation;
	}

}
