package eu.sapere.middleware.lsa.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.Lsa;

public class CustomizedAggregationOperator extends AbstractAggregationOperator {
	private Date lastAggregationDate = null;

	@Override
	public Object apply(List<Lsa> allLsa, AgentAuthentication agentAuthentication, String customizedOperator) {
		try {
			//long timeBegin = new Date().getTime();
			int debugLevel = 1;
			boolean disactivate = false;
			if (disactivate) {
				return null;
			}
			String propertyName = property.getPropertyName();
			Map<String, String> mapNodes = new HashMap<String, String>();
			Map<String, IAggregateable> mapReceivedObjects = new HashMap<String, IAggregateable>();
			for (Lsa nextLsa : allLsa) {
				Object value = nextLsa.getOneValue(propertyName);
				if (value instanceof IAggregateable) {
					IAggregateable nextObj = (IAggregateable) value;
					String node = nextLsa.getAgentAuthentication().getNodeLocation().getName();
					if (!allNodes.contains(node)) {
						allNodes.add(node);
						Collections.sort(allNodes);
					}
					String agentName = nextLsa.getAgentAuthentication().getAgentName();
					mapReceivedObjects.put(agentName, nextObj);
					mapNodes.put(agentName, node);
				}
			}
			String tagBegin = "CustomizedAggregationOperator.apply [" + property + "] : ";
			List<String> alreadyAggreatedNodes = getAlreadyAggreatedNodes(mapReceivedObjects);
			// For debug
			String sReceivedObjects = "";
			String sep = "";
			for (String nextAgentName : mapReceivedObjects.keySet()) {
				IAggregateable nextObj = mapReceivedObjects.get(nextAgentName);
				String sObjNodes = nextObj.isAggregated() ? ":" + nextObj.getLogAggregatedNodes() : "";
				sReceivedObjects += sep;
				sReceivedObjects += nextAgentName;
				sReceivedObjects += sObjNodes;
				sep = ",";
			}
			MiddlewareLogger.getInstance().info(tagBegin + "Received objects : " + sReceivedObjects);
			Map<String, IAggregateable> mapObjToAggregate = new HashMap<String, IAggregateable>();
			List<String> sourcesToRemove = new ArrayList<String>();
			for (String nextAgentName : mapReceivedObjects.keySet()) {
				IAggregateable nextObj = mapReceivedObjects.get(nextAgentName);
				// MiddlewareLogger.getInstance().info(tagBegin + "next received obj " +
				// nextNode + " " + nextObj.getLogAggregatedNodes());
				if (!nextObj.isAggregated() && alreadyAggreatedNodes.contains(nextAgentName)) {
					MiddlewareLogger.getInstance().error(tagBegin + "node " + nextAgentName + " already aggregated");
					sourcesToRemove.add(nextAgentName);
				} else {
					mapObjToAggregate.put(nextAgentName, nextObj);
				}
			}
			for (String nextAgentName : sourcesToRemove) {
				mapReceivedObjects.remove(nextAgentName);
			}
			if (mapObjToAggregate.isEmpty()) {
				return null;
			}
			if (mapObjToAggregate.size() == 1 /* && NodeManager.getNodeName().equals(sNewNodes) */) {
				// Only one object from the current node
				return null;
			}
			List<String> listNewAggregatedNodes = new ArrayList<String>();// (listObj, listSourceNodes);
			Map<String, IAggregateable> mapSourceObjets = computeMapSourceObjects(mapReceivedObjects);
			for (String nextAgentName : mapSourceObjets.keySet()) {
				if (mapNodes.containsKey(nextAgentName)) {
					String nextNode = mapNodes.get(nextAgentName);
					listNewAggregatedNodes.add(nextNode);
				}
			}
			MiddlewareLogger.getInstance().info(tagBegin + "listNewAggregatedNodes = " + listNewAggregatedNodes
					+ " aggregatedNodes = " + aggregatedNodes);
			/*
			 * if(alreadyAggreatedNodes.isEmpty()) { aggregatedNodes = new
			 * ArrayList<String>(); }
			 */
			Date newUpdateDate = auxComputeUpdateDate(mapObjToAggregate);
			long spentTimeMS = new Date().getTime() - (lastAggregationDate == null ? 0 : lastAggregationDate.getTime());
			long timeSinceLastAggregationSec = spentTimeMS / 1000;
			// MiddlewareLogger.getInstance().info("CustomizedAggregationOperator.apply :
			// lastNodes = " + lastNodes + ", newNodes = " + newNodes);
			MiddlewareLogger.getInstance()
					.info(tagBegin + " lastUpdateDate = "
							+ (lastUpdateDate == null ? "" : format_time.format(lastUpdateDate)) + ", newUpdateDate = "
							+ (newUpdateDate == null ? "" : format_time.format(newUpdateDate)));
			boolean toExecute = false;
			if (!listNodesEquals(aggregatedNodes, listNewAggregatedNodes)) {
				// Change in nodes
				toExecute = true;
				MiddlewareLogger.getInstance().info(tagBegin + "change in aggregatedNodes -> execute");
			} else if (lastUpdateDate == null || newUpdateDate == null || newUpdateDate.after(lastUpdateDate)) {
				toExecute = true;
				long timeGap = (newUpdateDate == null ? 0 : newUpdateDate.getTime())
						- (lastUpdateDate == null ? 0 : lastUpdateDate.getTime());
				MiddlewareLogger.getInstance()
						.info(tagBegin + "newUpdateDate is after lastUpdateDate (" + timeGap + " MS) -> execute");
			} else if (timeSinceLastAggregationSec > 60) {
				// check time since the last aggregation
				MiddlewareLogger.getInstance().info(tagBegin + " 60 sec after last aggregation -> execute");
				if (debugLevel > 1) {
					MiddlewareLogger.getInstance().info(tagBegin + "force aggregation after 60 sec"
							+ " lastAggregationDate = " + format_time.format(lastAggregationDate));
				}
				toExecute = true;
			}
			// change in last updates
			if (toExecute) {
				try {
					if (debugLevel > 0) {
						MiddlewareLogger.getInstance()
								.info(tagBegin + "call executeAggregation : timeSinceLastAggregationSec = "
										+ timeSinceLastAggregationSec);
					}
					IAggregateable result = executeAggregation(mapObjToAggregate, agentAuthentication,
							customizedOperator, newUpdateDate);
					if (result != null) {
						// result.setAggregatedNodes(listNewAggregatedNodes);
						result.setMapNodes(mapNodes);
						result.setMapSourceObjects(mapSourceObjets);
						aggregatedNodes = listNewAggregatedNodes;
						lastUpdateDate = newUpdateDate;
						// result.setAggregatedNodes(aggregatedNodes);
						boolean hasAllNodes = listNodesEquals(allNodes, aggregatedNodes);
						result.setAggregationCompleted(hasAllNodes);
						// for (Lsa nextLsa : allLsa) {
						// MiddlewareLogger.getInstance().info(tagBegin + "call executeAggregation :
						// remove property " +fieldName + " on lsa " + nextLsa.getAgentName() );
						// nextLsa.removePropertiesByName(fieldName);
						// }
						// For debug!!!
						for (String nextNode : result.getMapSourceObjects().keySet()) {
							MiddlewareLogger.getInstance().info(tagBegin + " source from node " + nextNode + " : "
									+ result.getMapSourceObjects().get(nextNode));
						}
						MiddlewareLogger.getInstance().info(
								tagBegin + "end aggregation :  aggregatedNodes = " + String.join(",", aggregatedNodes));
						return result;
					}
				} catch (Throwable e) {
					MiddlewareLogger.getInstance().error(e);
				}
			}
			return null;
		} catch (Throwable e) {
			MiddlewareLogger.getInstance().error(e);
			return null;
		}
	}
	/*
	 * private List<String> getAggregatedNodes(List<IAggregateable> listObj,
	 * List<String> listSourceNodes) { List<String> result = new
	 * ArrayList<String>(); int idx = 0; for(IAggregateable nextObj : listObj) {
	 * if(nextObj.isAggregated()) { for(String node : nextObj.getAggregatedNodes())
	 * { if(!result.contains(node)) { result.add(node); } } } else { //TODO : add
	 * node of LSA String sourceNode = listSourceNodes.get(idx);
	 * if(!result.contains(sourceNode)) { result.add(sourceNode); } } idx++; }
	 * return result; }
	 */

	private List<String> getAlreadyAggreatedNodes(Map<String, IAggregateable> mapReceivedObjects) {
		List<String> result = new ArrayList<String>();
		for (IAggregateable nextObj : mapReceivedObjects.values()) {
			if (nextObj.isAggregated()) {
				for (String node : nextObj.getSourceNodes()) {
					if (!result.contains(node)) {
						result.add(node);
					}
				}
			}
		}
		return result;
	}

	private Map<String, IAggregateable> computeMapSourceObjects(Map<String, IAggregateable> mapReceivedObjects) {
		Map<String, IAggregateable> result = new HashMap<String, IAggregateable>();
		// int idx = 0;
		for (IAggregateable nextObj : mapReceivedObjects.values()) {
			if (nextObj.isAggregated()) {
				Map<String, IAggregateable> mapSourceObjects = nextObj.getMapSourceObjects();
				for (String agentName : mapSourceObjects.keySet()) {
					IAggregateable nextObject = mapSourceObjects.get(agentName);
					result.put(agentName, nextObject);
				}
			}
		}
		for (String nextAgentName : mapReceivedObjects.keySet()) {
			IAggregateable nextObj = mapReceivedObjects.get(nextAgentName);
			if (!nextObj.isAggregated()) {
				if (result.containsKey(nextAgentName)) {
					// MiddlewareLogger.getInstance().error("Node " + nextNode + " already
					// aggregated");
				} else {
					result.put(nextAgentName, nextObj);

				}
			}
		}
		return result;
	}

	private static boolean listNodesEquals(List<String> listNode1, List<String> listNode2) {
		if (listNode1 == null) {
			return (listNode2 == null);
		} else if (listNode2 == null) {
			return false;
		}
		Collections.sort(listNode1);
		Collections.sort(listNode2);
		String sNodes1 = String.join(",", listNode1);
		String sNodes2 = String.join(",", listNode2);
		return sNodes1.equals(sNodes2);
	}

	private IAggregateable executeAggregation(Map<String, IAggregateable> mapObj,
			AgentAuthentication agentAuthentication, String customizedOperator, Date newUpdateDate) {
		int debugLevel = 1;
		if (debugLevel > 1) {
			MiddlewareLogger.getInstance()
					.info("CustomizedAggregationOperator.executeAggregation [" + property + "] begin");
		}
		long timeBegin = new Date().getTime();
		String localNode = agentAuthentication.getNodeLocation().getName();// NodeManager.getNodeName();
		IAggregateable firstObj = mapObj.containsKey(localNode) ? mapObj.get(localNode) : null;
		for (String nextNode : mapObj.keySet()) {
			if (firstObj == null) {
				firstObj = mapObj.get(nextNode);
			}
		}
		IAggregateable result = firstObj.aggregate(customizedOperator, mapObj, agentAuthentication,
				MiddlewareLogger.getInstance());
		long timeEnd = new Date().getTime();
		long timeSpent = timeEnd - timeBegin;
		if (result != null) {
			result.setLastUpdate(newUpdateDate);
			lastAggregationDate = new Date();
			result.setAggregationDate(lastAggregationDate);
		}
		if (debugLevel > 0) {
			MiddlewareLogger.getInstance().info("CustomizedAggregationOperator.executeAggregation [" + property
					+ "] : result = " + result + ", time spent (MS) = " + timeSpent);
		}
		return result;
	}
}
