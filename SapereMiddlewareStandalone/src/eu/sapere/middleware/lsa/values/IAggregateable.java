package eu.sapere.middleware.lsa.values;

import java.util.Date;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.IPropertyObject;

public interface IAggregateable extends IPropertyObject {

	List<String> getSourceNodes();

	String getLogAggregatedNodes();

	Map<String, IAggregateable> getMapSourceObjects();

	void setMapSourceObjects(Map<String, IAggregateable> sourceObjects);

	void setMapNodes(Map<String, String> mapNodes);

	boolean isAggregated();

	boolean isAggregationCompleted();

	void setAggregationCompleted(boolean hasAllNodes);

	IAggregateable aggregate(String operator, Map<String,IAggregateable> mapObjects, AgentAuthentication agentAuthentication, AbstractLogger logger);

	Date getLastUpdate();

	void setLastUpdate(Date aDate);

	void refreshLastUpdate();

	Date getAggregationDate();

	void setAggregationDate(Date aggregationDate);

	boolean isReadyForAggregation();
}
