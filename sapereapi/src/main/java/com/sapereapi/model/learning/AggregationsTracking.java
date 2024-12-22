package com.sapereapi.model.learning;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.AbstractAggregatable;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class AggregationsTracking extends AbstractAggregatable implements IAggregateable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Map<PredictionScope, Integer> availabilityTable = new HashMap<PredictionScope, Integer>();
	Map<PredictionScope, Integer> nbOfAggregationTable = new HashMap<PredictionScope, Integer>();
	Map<PredictionScope, Date> lastAggregationTable = new HashMap<PredictionScope, Date>();
	//Date lastUpdate = new Date();

	public void clear() {
		availabilityTable.clear();
	}

	public Map<PredictionScope, Integer> getAvailabilityTable() {
		return availabilityTable;
	}

	public void setAvailabilityTable(Map<PredictionScope, Integer> availabilityTable) {
		this.availabilityTable = availabilityTable;
	}

	public Map<PredictionScope, Integer> getNbOfAggregationTable() {
		return nbOfAggregationTable;
	}

	public void setNbOfAggregationTable(Map<PredictionScope, Integer> nbOfAggregationTable) {
		this.nbOfAggregationTable = nbOfAggregationTable;
	}

	public Map<PredictionScope, Date> getLastAggregationTable() {
		return lastAggregationTable;
	}

	public void setLastAggregationTable(Map<PredictionScope, Date> lastAggregationTable) {
		this.lastAggregationTable = lastAggregationTable;
	}

	public void setModelAvailability(PredictionScope scope, int availability) {
		availabilityTable.put(scope, availability);
		lastUpdate = new Date();
	}

	public int getObjectAvailability(PredictionScope scope) {
		if (availabilityTable.containsKey(scope)) {
			return availabilityTable.get(scope);
		}
		return 0;
	}

	public void addAggregation(PredictionScope scope, Date aDate) {
		Date lastAggregationDate = null;
		if (lastAggregationTable.containsKey(scope)) {
			lastAggregationDate = lastAggregationTable.get(scope);
		}
		if (lastAggregationDate == null || lastAggregationDate.before(aDate)) {
			lastAggregationTable.put(scope, aDate);
			if (!nbOfAggregationTable.containsKey(scope)) {
				nbOfAggregationTable.put(scope, 0);
			}
			int lastNbOfAggregations = nbOfAggregationTable.get(scope);
			nbOfAggregationTable.put(scope, 1 + lastNbOfAggregations);
		}
		availabilityTable.put(scope, 0);
	}

	@Override
	public IAggregateable aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, mapObjects, agentAuthentication, logger);
	}

	public static AggregationsTracking aggregate2(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		Map<PredictionScope, Integer> resultTable = new HashMap<PredictionScope, Integer>();
		for (String agent : mapObjects.keySet()) {
			IAggregateable nextObj = mapObjects.get(agent);
			if (nextObj instanceof AggregationsTracking) {
				AggregationsTracking nextModelAvailability = (AggregationsTracking) nextObj;
				for (PredictionScope scope : PredictionScope.values()) {
					int availability = nextModelAvailability.getObjectAvailability(scope);
					if (availability > 0) {
						if (!resultTable.containsKey(scope)) {
							resultTable.put(scope, 0);
						}
						int lastAvailAbility = resultTable.get(scope);
						resultTable.put(scope, lastAvailAbility + availability);
					}
				}
			}
		}
		AggregationsTracking result = new AggregationsTracking();
		for (PredictionScope scope : resultTable.keySet()) {
			int totalAvailiabilty = resultTable.get(scope);
			result.setModelAvailability(scope, totalAvailiabilty);
		}
		return result;
	}

	public boolean isEmpty() {
		return availabilityTable.isEmpty();
	}


	@Override
	public AggregationsTracking copyForLSA(AbstractLogger logger) {
		AggregationsTracking result = new AggregationsTracking();		
		Map<PredictionScope, Integer> copyAvailabilityTable = new HashMap<PredictionScope, Integer>();
		for(PredictionScope key : this.availabilityTable.keySet()) {
			copyAvailabilityTable.put(key, availabilityTable.get(key));
		}
		result.setAvailabilityTable(copyAvailabilityTable);
		Map<PredictionScope, Integer> copyNbOfAggregationTable = new HashMap<PredictionScope, Integer>();
		for(PredictionScope key : this.nbOfAggregationTable.keySet()) {
			copyNbOfAggregationTable.put(key, nbOfAggregationTable.get(key));
		}
		result.setNbOfAggregationTable(copyNbOfAggregationTable);
		Map<PredictionScope, Date> copyLastAggregationTable = new HashMap<PredictionScope, Date>();
		for(PredictionScope key : this.lastAggregationTable.keySet()) {
			copyLastAggregationTable.put(key, lastAggregationTable.get(key));
		}
		result.setLastAggregationTable(copyLastAggregationTable);
		return result;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(super.toString());
		result.append("availability: ");
		for (PredictionScope scope : availabilityTable.keySet()) {
			result.append(scope).append(":").append(availabilityTable.get(scope));
		}
		if(lastAggregationTable.size() > 0) {
			result.append(", lastAggregation:");
			for (PredictionScope scope : lastAggregationTable.keySet()) {
				Date lastAggregation = lastAggregationTable.get(scope);
				String sLastAggregation = UtilDates.format_time.format(lastAggregation);
				result.append(scope).append(":").append(sLastAggregation);
			}
		}
		if(nbOfAggregationTable.size() > 0) {
			result.append(", nbOfAggregations:");
			for (PredictionScope scope : nbOfAggregationTable.keySet()) {
				result.append(scope).append(":").append(nbOfAggregationTable.get(scope));
			}
		}
		return result.toString();
	}

	@Override
	public boolean isReadyForAggregation() {
		return true;
	}
}
