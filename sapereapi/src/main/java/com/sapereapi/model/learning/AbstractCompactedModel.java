package com.sapereapi.model.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.values.AbstractAggregatable;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public abstract class AbstractCompactedModel extends AbstractAggregatable implements ICompactedModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Map<String, Map<String, Double>> aggregationWeights = new HashMap<String, Map<String, Double>>();
	protected PredictionContext predictionContext;
	protected CompactNodeStatesTransitions nodeStateTransitions = null;
	protected String currentCodeFeaturesKey;
	protected Date[] historyDates;
	protected Integer[][] historyStateIds;
	protected Double[][] historyValues;
	protected boolean isComplete;
	protected Map<String, Map<String, String>> zippedContent = new HashMap<String, Map<String, String>>();

	@Override
	public IAggregateable aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Map<String, Double>> getAggregationWeights() {
		return aggregationWeights;
	}

	public void setAggregationWeights(Map<String, Map<String, Double>> aggregationWeights) {
		this.aggregationWeights = aggregationWeights;
	}

	public PredictionContext getPredictionContext() {
		return predictionContext;
	}

	public void setPredictionContext(PredictionContext predictionContext) {
		this.predictionContext = predictionContext;
	}

	public Date[] getHistoryDates() {
		return historyDates;
	}

	public void setHistoryDates(Date[] historyDates) {
		this.historyDates = historyDates;
	}

	public Double[][] getHistoryValues() {
		return historyValues;
	}

	public void setHistoryValues(Double[][] historyValues) {
		this.historyValues = historyValues;
	}

	public Integer[][] getHistoryStateIds() {
		return historyStateIds;
	}

	public void setHistoryStateIds(Integer[][] historyStateIds) {
		this.historyStateIds = historyStateIds;
	}

	public CompactNodeStatesTransitions getNodeStateTransitions() {
		return nodeStateTransitions;
	}

	public void setNodeStatevTransitions(CompactNodeStatesTransitions _nodeStateTransitions) {
		this.nodeStateTransitions = _nodeStateTransitions;
	}

	public String getCurrentCodeFeaturesKey() {
		return currentCodeFeaturesKey;
	}

	public void setCurrentCodeFeaturesKey(String currentFeaturesKey) {
		this.currentCodeFeaturesKey = currentFeaturesKey;
	}

	public Map<String, Map<String, String>> getZippedContent() {
		return zippedContent;
	}

	public void setZippedContent(Map<String, Map<String, String>> content) {
		this.zippedContent = content;
	}

	public void clearZippedContent() {
		this.zippedContent.clear();
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		List<NodeLocation> result = new ArrayList<NodeLocation>();
		if (this.predictionContext != null) {
			result.add(predictionContext.getNodeLocation());
		}
		return result;
	}

	public Map<String, String> getVariableSubContent(String variable) {
		if (!zippedContent.containsKey(variable)) {
			return new HashMap<String, String>();
		}
		return zippedContent.get(variable);
	}

	public void auxCopyForLSA(AbstractCompactedModel result, AbstractLogger logger) {
		if (predictionContext != null) {
			result.setPredictionContext(predictionContext.copyContent(false));
		}
		if (nodeStateTransitions != null) {
			result.setNodeStatevTransitions(nodeStateTransitions.clone());
		}
		if (aggregationWeights != null && aggregationWeights.size() > 0) {
			result.setAggregationWeights(SapereUtil.cloneMap2StringDouble(aggregationWeights));
		}
		result.setComplete(isComplete);
		result.setLastUpdate(lastUpdate);
		Date[] cloneHistoryDates = new Date[historyDates.length];
		System.arraycopy(historyDates, 0, cloneHistoryDates, 0, historyDates.length);
		result.setHistoryDates(cloneHistoryDates);
		Double[][] cloneHistoryValues = new Double[historyValues.length][];
		System.arraycopy(historyValues, 0, cloneHistoryValues, 0, historyValues.length);
		result.setHistoryValues(cloneHistoryValues);
		Integer[][] cloneHistoryStateIds = new Integer[historyStateIds.length][];
		System.arraycopy(historyStateIds, 0, cloneHistoryStateIds, 0, historyStateIds.length);
		result.setHistoryStateIds(cloneHistoryStateIds);
		result.setCurrentCodeFeaturesKey(currentCodeFeaturesKey);
		result.clearZippedContent();
		for (String variable :zippedContent.keySet()) {
			Map<String, String> varContent = zippedContent.get(variable);
			for (String nextKey : varContent.keySet()) {
				String nexSubContent = varContent.get(nextKey);
				result.addZippedContentForKey(variable, nextKey, nexSubContent);
			}
		}
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation,
			AbstractLogger logger) {
		// TODO Auto-generated method stub

	}

	public static SingleNodeStateItem auxGenerateSingleNodeStateItem(Date nextDate, String[] listVariables,
			Double[] values, Integer[] stateIds) {
		SingleNodeStateItem nodeStateItem = new SingleNodeStateItem();
		nodeStateItem.setDate(nextDate);
		int idxVariable = 0;
		for (String variable : listVariables) {
			if (idxVariable < values.length) {
				Double nextValue = values[idxVariable];
				nodeStateItem.setValue(variable, nextValue);
			}
			if (idxVariable < stateIds.length) {
				Integer nextStateId = stateIds[idxVariable];
				nodeStateItem.setStateId(variable, nextStateId);
			}
			idxVariable++;
		}
		return nodeStateItem;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(super.toString());
		if (predictionContext != null) {
			result.append(" node:").append(predictionContext.getNodeLocation());
			result.append(", scope ").append(predictionContext.getScope());
		}
		result.append("}");
		if (lastUpdate != null) {
			result.append(", last update:").append(UtilDates.format_time.format(lastUpdate));
		} else {
			result.append(", last update IS NULL");
		}
		result.append(SapereUtil.CR);
		result.append(" current key:").append(currentCodeFeaturesKey);
		String[] listVariables = predictionContext.getNodeContext().getVariables();
		int idxDate = 0;
		String sep = "";
		if (historyDates != null && historyDates.length > 0) {
			result.append(", StateHistory : ");
			for (Date nextDate : historyDates) {
				if (idxDate < 10 && idxDate < historyValues.length && idxDate < historyStateIds.length) {
					Double[] values = historyValues[idxDate];
					Integer[] stateIds = historyStateIds[idxDate];
					SingleNodeStateItem nodeStateItem = auxGenerateSingleNodeStateItem(nextDate, listVariables, values,
							stateIds);
					result.append(sep).append(nodeStateItem);
					sep = ", ";
				}
				idxDate++;
			}
		}
		return result.toString();
	}

	public void fillCompactModel(AbstractCompactedModel aCompactModel) {
		aCompactModel.setLastUpdate(lastUpdate);
		aCompactModel.setMapNodes(mapNodes);
		aCompactModel.setHistoryDates(historyDates);
		aCompactModel.setHistoryStateIds(historyStateIds);
		aCompactModel.setHistoryValues(historyValues);
		aCompactModel.setPredictionContext(predictionContext);
		aCompactModel.setAggregationWeights(aggregationWeights);
		aCompactModel.setNodeStatevTransitions(nodeStateTransitions);
		aCompactModel.setCurrentCodeFeaturesKey(currentCodeFeaturesKey);
		aCompactModel.setZippedContent(zippedContent);
		aCompactModel.setComplete(isComplete);
	}

	public void fillLearninModel(AbstractLearningModel learningModel) {
		learningModel.setPredictionContext(predictionContext.clone());
		FeaturesKey currentFeaturesKey = predictionContext.parseFeaturesKey(currentCodeFeaturesKey);
		learningModel.setCurrentFeaturesKey(currentFeaturesKey);
		if (aggregationWeights != null && aggregationWeights.size() > 0) {
			learningModel.setAggregationWeights(SapereUtil.cloneMap2StringDouble(aggregationWeights));
		}
		if (nodeStateTransitions != null) {
			learningModel.setNodeStatesTransitions(nodeStateTransitions.generateNodeStateTransitions());
		}
		int idxDate = 0;
		String[] listVariables = predictionContext.getNodeContext().getVariables();
		List<SingleNodeStateItem> stateHistory = new ArrayList<SingleNodeStateItem>();
		if (historyDates != null && historyValues != null && historyStateIds != null) {
			for (Date nextDate : historyDates) {
				if (idxDate < historyValues.length && idxDate < historyStateIds.length) {
					Double[] values = historyValues[idxDate];
					Integer[] stateIds = historyStateIds[idxDate];
					SingleNodeStateItem nodeStateItem = auxGenerateSingleNodeStateItem(nextDate, listVariables, values,
							stateIds);
					stateHistory.add(nodeStateItem);
				}
				idxDate++;
			}
		}
		learningModel.setStateHistory(stateHistory);
	}

	public String getIterationMatrix2(String variable, String codeFeaturesKey) {
		if (zippedContent.containsKey(variable)) {
			Map<String, String> varContent = zippedContent.get(variable);
			if (varContent.containsKey(codeFeaturesKey)) {
				return varContent.get(codeFeaturesKey);
			}
		}
		return null;
	}

	public List<String> getKeys() {
		List<String> result = new ArrayList<String>();
		for (String variable : zippedContent.keySet()) {
			Map<String, String> varContent = zippedContent.get(variable);
			for (String nextKey : varContent.keySet()) {
				if (!result.contains(nextKey)) {
					result.add(nextKey);
				}
			}
		}
		// sort the items
		Collections.sort(result);
		return result;
	}

	public void addZippedContentForKey(String variable, String codeKey, String subContent) {
		if (!zippedContent.containsKey(variable)) {
			zippedContent.put(variable, new HashMap<String, String>());
		}
		Map<String, String> varContent = zippedContent.get(variable);
		varContent.put(codeKey, subContent);
	}

	public void addZippedContent(String variable, Map<String, String> zippedVariableContent) {
		if (!zippedContent.containsKey(variable)) {
			zippedContent.put(variable, new HashMap<String, String>());
		}
		Map<String, String> varContent = zippedContent.get(variable);
		for (String codeFeaturesKey : zippedVariableContent.keySet()) {
			varContent.put(codeFeaturesKey, zippedVariableContent.get(codeFeaturesKey));
		}
	}

	public int getTotalKeyNb() {
		int result = 0;
		for (String variable : zippedContent.keySet()) {
			Map<String, String> varContent = getVariableSubContent(variable);
			result += varContent.size();
		}
		return result;
	}

	public Map<String, ILearningModel> retrieveSourceModels(boolean refreshAllMatrices, AbstractLogger logger) {
		Map<String, ILearningModel> result = new HashMap<String, ILearningModel>();
		for(String nextAgentName : mapSourceObjects.keySet()) {
			IAggregateable nextObj = mapSourceObjects.get(nextAgentName);
			if(nextObj instanceof ICompactedModel) {
				ICompactedModel nextCompactedModel = (ICompactedModel) nextObj;
				ILearningModel nextCompleteModel = nextCompactedModel.generateCompleteModel(logger);
				result.put(nextAgentName, nextCompleteModel);
			}
		}
		return result;
	}

	public boolean hasModelAggregator() {
		LearningAggregationOperator aggregator = predictionContext.getAggregationOperator();
		return (aggregator != null && aggregator.isModelAggregation());
	}

	public boolean isReadyForAggregation() {
		if(this.hasModelAggregator() && this.isComplete() && lastUpdate != null) {
			Date lastAggregation = this.getAggregationDate();
			//Date current = predictionContext.getCurrentDate();
			int waitingMinutesBetweenAggragations = this.predictionContext.getAggregationOperator().getWaitingMinutesBetweenAggragations();
			Date minAggregationDate = lastAggregation == null ? lastUpdate : UtilDates.shiftDateMinutes(lastAggregation, waitingMinutesBetweenAggragations);
			if(lastAggregation == null || lastUpdate.after(minAggregationDate)) {
				// send input for a new aggregation
				return true;
			}
		}
		return false;
	}
}
