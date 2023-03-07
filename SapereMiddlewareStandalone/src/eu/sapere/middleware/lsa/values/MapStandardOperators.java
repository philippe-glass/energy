package eu.sapere.middleware.lsa.values;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.Lsa;

public class MapStandardOperators {

	public final static StandardAggregationOperator OP_NEWEST = new StandardAggregationOperator("NEWEST") {
		@Override
		public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
			if (allLsa.isEmpty())
				return null;
			Lsa nextLsa = allLsa.get(0);
			String fieldName = nextLsa.getAggregationBy();
			Date newest = nextLsa.getDateValue(fieldName);
			for (int i = 0; i < allLsa.size(); i++) {
				nextLsa = allLsa.get(i);
				Date nextDate = nextLsa.getDateValue(fieldName);
				if (nextDate.after(newest)) {
					newest = nextDate;
				}
			}
			return newest;
		}
	};
	public final static StandardAggregationOperator OP_OLDEST = new StandardAggregationOperator("OLDEST") {
		@Override
		public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
			if (allLsa.isEmpty())
				return null;
			Lsa nextLsa = allLsa.get(0);
			String fieldName = nextLsa.getAggregationBy();
			Date oldest = nextLsa.getDateValue(fieldName);
			for (int i = 0; i < allLsa.size(); i++) {
				nextLsa = allLsa.get(i);
				Date nextDate = nextLsa.getDateValue(fieldName);
				if (nextDate.before(oldest)) {
					oldest = nextDate;
				}
			}
			return oldest;
		}
	};

	public final static StandardAggregationOperator OP_MAX = new StandardAggregationOperator("MAX") {
		@Override
		public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
			if (allLsa.isEmpty())
				return null;
			Lsa nextLsa = allLsa.get(0);
			String fieldName = nextLsa.getAggregationBy();
			BigDecimal max = nextLsa.getBigDecimalValue(fieldName);
			for (int i = 0; i < allLsa.size(); i++) {
				nextLsa = allLsa.get(i);
				BigDecimal m = nextLsa.getBigDecimalValue(fieldName);
				if (m.max(max).equals(m)) {
					max = m;
				}
			}
			return max;
		}
	};

	public final static StandardAggregationOperator OP_MIN = new StandardAggregationOperator("MIN") {
		@Override
		public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
			if (allLsa.isEmpty())
				return null;
			Lsa nextLsa = allLsa.get(0);
			String fieldName = nextLsa.getAggregationBy();
			BigDecimal min = nextLsa.getBigDecimalValue(fieldName);
			for (int i = 0; i < allLsa.size(); i++) {
				nextLsa = allLsa.get(i);
				BigDecimal m = nextLsa.getBigDecimalValue(fieldName);
				if (m.min(min).equals(m)) {
					min = m;
				}
			}
			return min;
		}
	};

	public final static StandardAggregationOperator OP_AVG = new StandardAggregationOperator("AVG") {
		@Override
		public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
			if (allLsa.isEmpty())
				return null;
			Lsa nextLsa = allLsa.get(0);
			String fieldName = nextLsa.getAggregationBy();
			BigDecimal val = new BigDecimal("0");
			for (int i = 0; i < allLsa.size(); i++) {
				nextLsa = allLsa.get(i);
				BigDecimal nextValue = nextLsa.getBigDecimalValue(fieldName);
				val = val.add(nextValue);
			}
			val = val.divide(new BigDecimal(allLsa.size()));
			return val;
		}
	};

	public final static StandardAggregationOperator OP_TEST1 = new StandardAggregationOperator("TEST1") {
		@Override
		public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
			if (allLsa.isEmpty())
				return null;
			Lsa firstLsa = allLsa.get(0);
			String fieldName = firstLsa.getAggregationBy();
			BigDecimal max = firstLsa.getBigDecimalValue(fieldName);
			if (max != null) {
				for (int i = 0; i < allLsa.size(); i++) {
					Lsa nextLsa = allLsa.get(i);
					BigDecimal m = nextLsa.getBigDecimalValue(fieldName);
					if (m != null && m.max(max).equals(m)) {
						max = m;
					}
				}
			}
			return max;
		}
	};

	private static Map<String, StandardAggregationOperator> mapOperators = new HashMap<String, StandardAggregationOperator>();
	static {
		mapOperators.put(OP_MAX.getName(), OP_MAX);
	}

	public static StandardAggregationOperator getOperator(String opName) {
		return mapOperators.get(opName);
	}

}
