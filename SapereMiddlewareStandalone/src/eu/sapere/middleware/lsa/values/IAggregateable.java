package eu.sapere.middleware.lsa.values;

import java.util.List;

import eu.sapere.middleware.agent.AgentAuthentication;

public interface IAggregateable {
	public IAggregateable aggregate(String operator, List<IAggregateable> listObjects,  AgentAuthentication agentAuthentication);
}
