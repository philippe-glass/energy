package eu.sapere.middleware.lsa.values;

import java.util.Date;
import java.util.List;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;

public abstract class AbstractAggregationOperator {

	public abstract Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator);

	public Lsa applyInNewLsa(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator) {
		Object updatedValue = apply(allLsa, agentAuthentication, customizedOperator);
		Lsa result = allLsa.get(0).getCopy();
		String fieldName = allLsa.get(0).getAggregationBy();
		result.replacePropertyWithName(new Property(fieldName, updatedValue));
		Date date = new Date();
		result.addSyntheticProperty(SyntheticPropertyName.LAST_MODIFIED, date.getTime());
		return result;
	}
}
