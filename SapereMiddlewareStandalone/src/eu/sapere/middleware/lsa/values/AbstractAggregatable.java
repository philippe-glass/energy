package eu.sapere.middleware.lsa.values;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import eu.sapere.middleware.log.AbstractLogger;

public abstract class AbstractAggregatable implements IAggregateable {
	private static final long serialVersionUID = 1L;
	protected Map<String, IAggregateable> mapSourceObjects = new HashMap<String, IAggregateable>();
	protected Map<String, String> mapNodes = new HashMap<String, String>();
	protected boolean aggregationCompleted = false;
	protected Date aggregationDate = null;
	protected Date lastUpdate = null;


	@Override
	public List<String> getSourceNodes() {
		List<String> result = new ArrayList<String>();
		for(String agentName : mapSourceObjects.keySet()) {
			if(mapNodes.containsKey(agentName)) {
				String node = mapNodes.get(agentName);
				if(!result.contains(node)) {
					result.add(node);
				}
			}
		}
		return result;
	}

	@Override
	public boolean isAggregated() {
		return mapSourceObjects != null && mapSourceObjects.size() > 0;
	}

	public boolean isAggregationCompleted() {
		return aggregationCompleted;
	}

	public void setAggregationCompleted(boolean aggregationCompleted) {
		this.aggregationCompleted = aggregationCompleted;
	}


	@Override
	public void setMapSourceObjects(Map<String, IAggregateable> sourceObject) {
		this.mapSourceObjects = sourceObject;
	}

	@Override
	public Map<String, IAggregateable> getMapSourceObjects() {
		return mapSourceObjects;
	}

	public Date getAggregationDate() {
		return aggregationDate;
	}

	public void setAggregationDate(Date aggregationDate) {
		this.aggregationDate = aggregationDate;
	}

	public Map<String, String> getMapNodes() {
		return mapNodes;
	}

	public void setMapNodes(Map<String, String> mapNodes) {
		this.mapNodes = mapNodes;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public void refreshLastUpdate() {
		this.lastUpdate = new Date();
	}

	@Override
	public String getLogAggregatedNodes() {
		StringBuffer result = new StringBuffer();
		String sep="";
		for(String nextAgentName : mapSourceObjects.keySet()) {
			IAggregateable nextObject = mapSourceObjects.get(nextAgentName);
			result.append(sep);
			if(nextObject.isAggregated()) {
				result.append(nextObject.getLogAggregatedNodes());
			} else {
				result.append(nextAgentName);
			}
			sep=",";
		}
		return "[" + result.toString() + "]";
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		if(isAggregated()) {
			result.append("{");
			List<String> sourceNodes = getSourceNodes();
			if(sourceNodes.size() >= 0) {
				result.append("aggregatedNodes:").append(sourceNodes);
			}
			if(aggregationCompleted) {
				result.append("(completed)");
			} else {
				result.append("(NOT COMPLETED)");
			}
			if(aggregationDate != null) {
				result.append(", last aggregationDate:").append(AbstractAggregationOperator.format_time.format(aggregationDate));
			} else {
				result.append(AbstractLogger.CR).append(", last aggregationDate IS NULL");
			}
			result.append("} ");
		} else {
		}
		return result.toString();
	}
}
