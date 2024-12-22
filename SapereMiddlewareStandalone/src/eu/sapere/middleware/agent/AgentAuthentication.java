package eu.sapere.middleware.agent;

import java.io.Serializable;

import eu.sapere.middleware.node.NodeLocation;

public class AgentAuthentication implements Serializable {
	private static final long serialVersionUID = 1784L;
	private String agentName;
	private String agentType;
	private String authenticationKey;
	private NodeLocation nodeLocation;

	public AgentAuthentication(String agentName, String agentType, String authenticationKey
			, NodeLocation _nodeLocation
			) {
		super();
		this.agentName = agentName;
		this.agentType = agentType;
		this.authenticationKey = authenticationKey;
		this.nodeLocation = _nodeLocation;
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

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public void setAgentType(String agentType) {
		this.agentType = agentType;
	}

	public void setAuthenticationKey(String authenticationKey) {
		this.authenticationKey = authenticationKey;
	}

	public NodeLocation getNodeLocation() {
		return nodeLocation;
	}

	public void setNodeLocation(NodeLocation nodeLocation) {
		this.nodeLocation = nodeLocation;
	}

	public AgentAuthentication copy() {
		NodeLocation nodeLocationCopy = nodeLocation.copy();
		return new AgentAuthentication(agentName, agentType, authenticationKey, nodeLocationCopy);
	}
}
