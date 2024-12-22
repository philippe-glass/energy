package eu.sapere.middleware.lsa.values;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.AggregatorProperty;
import eu.sapere.middleware.lsa.Lsa;

public class MapStandardOperators {
	public final static String STD_OP_NEWEST = "NEWEST";
	public final static String STD_OP_OLDEST = "OLDEST";
	public final static String STD_OP_MIN = "MIN";
	public final static String STD_OP_AVG = "AVG";
	public final static String STD_OP_TEST1 = "TEST1";

	public static StandardAggregationOperator createStandardAggregator_NEWEST(String aggregatorField) {
		StandardAggregationOperator aggregator = new StandardAggregationOperator(STD_OP_NEWEST, aggregatorField) {
			@Override
			public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
				if (allLsa.isEmpty())
					return null;
				Lsa nextLsa = allLsa.get(0);
				Date newest = nextLsa.getDateValue(aggregatorField);
				for (int i = 0; i < allLsa.size(); i++) {
					nextLsa = allLsa.get(i);
					Date nextDate = nextLsa.getDateValue(aggregatorField);
					if (nextDate.after(newest)) {
						newest = nextDate;
					}
				}
				return newest;
			}
		};
		mapOperators.put(aggregator.getProperty(), aggregator);
		return aggregator;
	}

	public static StandardAggregationOperator createStandardAggregator_OLDEST(String fieldName) {
		StandardAggregationOperator aggregator = new StandardAggregationOperator("OP_OLDEST", fieldName) {
			@Override
			public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
				if (allLsa.isEmpty())
					return null;
				Lsa nextLsa = allLsa.get(0);
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
		mapOperators.put(aggregator.getProperty(), aggregator);
		return aggregator;
	}

	public static StandardAggregationOperator createStandardAggregator_MAX(String fieldName) {
		StandardAggregationOperator aggregator = new StandardAggregationOperator("OP_OLDEST", fieldName) {
			@Override
			public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
				if (allLsa.isEmpty())
					return null;
				Lsa nextLsa = allLsa.get(0);
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
		mapOperators.put(aggregator.getProperty(), aggregator);
		return aggregator;
	}

	public static StandardAggregationOperator createStandardAggregator_MIN(String fieldName) {
		StandardAggregationOperator aggregator = new StandardAggregationOperator("MIN", fieldName) {
			@Override
			public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
				if (allLsa.isEmpty())
					return null;
				Lsa nextLsa = allLsa.get(0);
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
		mapOperators.put(aggregator.getProperty(), aggregator);
		return aggregator;
	}

	public static StandardAggregationOperator createStandardAggregator_AVG(String fieldName) {
		StandardAggregationOperator aggregator = new StandardAggregationOperator("AVG", fieldName) {
			@Override
			public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
				if (allLsa.isEmpty())
					return null;
				Lsa nextLsa = allLsa.get(0);
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
		mapOperators.put(aggregator.getProperty(), aggregator);
		return aggregator;
	}

	public static StandardAggregationOperator createStandardAggregator_TEST1(String fieldName) {
		StandardAggregationOperator aggregator = new StandardAggregationOperator("TEST1", fieldName) {
			@Override
			public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication) {
				if (allLsa.isEmpty())
					return null;
				Lsa firstLsa = allLsa.get(0);
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
		mapOperators.put(aggregator.getProperty(), aggregator);
		return aggregator;
	}

	private static Map<AggregatorProperty, AbstractAggregationOperator> mapOperators = new HashMap<AggregatorProperty, AbstractAggregationOperator>();
	static {
		// mapOperators.put(OP_MAX.getName(), OP_MAX);
	}

	public static AbstractAggregationOperator getOperator(AggregatorProperty prop) {
		if (!mapOperators.containsKey(prop)) {
			AbstractAggregationOperator aggregator = null;
			if (prop.isCustomized()) {
				aggregator = new CustomizedAggregationOperator();
				aggregator.setProperty(prop);
			}
			if (prop.isStandard()) {
				String aggregatorPropertyName = prop.getPropertyName();
				if (STD_OP_NEWEST.equals(prop.getOperator())) {
					aggregator = createStandardAggregator_NEWEST(aggregatorPropertyName);
				}
				if (STD_OP_OLDEST.equals(prop.getOperator())) {
					aggregator = createStandardAggregator_OLDEST(aggregatorPropertyName);
				}
				if (STD_OP_MIN.equals(prop.getOperator())) {
					aggregator = createStandardAggregator_MIN(aggregatorPropertyName);
				}
				if (STD_OP_AVG.equals(prop.getOperator())) {
					aggregator = createStandardAggregator_AVG(aggregatorPropertyName);
				}
				if (STD_OP_TEST1.equals(prop.getOperator())) {
					aggregator = createStandardAggregator_TEST1(aggregatorPropertyName);
				}
			}
			if (aggregator != null) {
				mapOperators.put(prop, aggregator);
			}
		}
		if (mapOperators.containsKey(prop)) {
			return mapOperators.get(prop);
		}
		return null;
	}
}
