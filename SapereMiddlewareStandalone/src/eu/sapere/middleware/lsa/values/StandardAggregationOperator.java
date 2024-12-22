package eu.sapere.middleware.lsa.values;

import java.util.List;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.AggregatorProperty;
import eu.sapere.middleware.lsa.AggregatorType;
import eu.sapere.middleware.lsa.Lsa;

public abstract class StandardAggregationOperator extends AbstractAggregationOperator {
	abstract Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication);

	public StandardAggregationOperator(String name, String aggregatorField) {
		super();
		this.property = new AggregatorProperty(name, AggregatorType.STANDARD, aggregatorField, Boolean.FALSE);
	}

	@Override
	public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator) {
		return apply(allLsa, agentAuthentication);
	}

}
