package eu.sapere.middleware.lsa.values;

import java.util.List;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;

public abstract class StandardAggregationOperator extends AbstractAggregationOperator {
	abstract Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication);

	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StandardAggregationOperator(String name) {
		super();
		this.name = name;
	}

	@Override
	public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator) {
		return apply(allLsa, agentAuthentication);
	}

}
