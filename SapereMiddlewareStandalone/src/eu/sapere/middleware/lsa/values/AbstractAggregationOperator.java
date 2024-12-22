package eu.sapere.middleware.lsa.values;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.AggregatorProperty;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.lsa.SyntheticPropertyName;

public abstract class AbstractAggregationOperator {
	protected AggregatorProperty property;
	protected Date lastUpdateDate = null;
	protected List<String> aggregatedNodes = null;
	protected List<String> allNodes = new ArrayList<String>();
	public static SimpleDateFormat format_time = new SimpleDateFormat("HH:mm:ss");

	public abstract Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator);

	public AggregatorProperty getProperty() {
		return property;
	}

	public void setProperty(AggregatorProperty property) {
		this.property = property;
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}

	public List<String> getAggregatedNodes() {
		return aggregatedNodes;
	}

	public void setAggregatedNodes(List<String> lastNodes) {
		this.aggregatedNodes = lastNodes;
	}

	public int getAggregationCardinality() {
		if(aggregatedNodes != null) {
			return aggregatedNodes.size();
		}
		return 0;
	}

	protected static Date auxComputeUpdateDate(Map<String, IAggregateable> mapObj) {
		Date result = null;
		for(IAggregateable nextObj : mapObj.values()) {
			if(nextObj.getLastUpdate() != null) {
				if(result == null || nextObj.getLastUpdate().after(result)) {
					result = nextObj.getLastUpdate();
				}
			}
		}
		return result;
	}

	public Lsa applyInNewLsa(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator) {
		Object updatedValue = apply(allLsa, agentAuthentication, customizedOperator);
		if(updatedValue == null) {
			return null;
		}
		Lsa result = allLsa.get(0).copy();
		String propertyName = property.getPropertyName();
		result.replacePropertyWithName(new Property(propertyName, updatedValue));
		Date date = new Date();
		result.addSyntheticProperty(SyntheticPropertyName.LAST_AGGREGATION, date.getTime());
		return result;
	}

}
