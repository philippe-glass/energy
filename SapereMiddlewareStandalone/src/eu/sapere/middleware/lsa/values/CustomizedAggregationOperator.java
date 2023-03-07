package eu.sapere.middleware.lsa.values;

import java.util.ArrayList;
import java.util.List;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;

public class CustomizedAggregationOperator extends AbstractAggregationOperator {

	@Override
	public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator) {
		Lsa firstLsa = allLsa.get(0);
		String fieldName = firstLsa.getAggregationBy();
		List<IAggregateable> listObj = new ArrayList<IAggregateable>();
		for (Lsa nextLsa : allLsa) {
			Object value = nextLsa.getOneValue(fieldName);
			if (value instanceof IAggregateable) {
				listObj.add((IAggregateable) value);
			}
		}
		if (listObj.isEmpty()) {
			return null;
		}
		IAggregateable firstObj = listObj.get(0);
		IAggregateable result = firstObj.aggregate(customizedOperator, listObj, agentAuthentication);
		return result;
	}

}
