package com.sapereapi.model.learning.prediction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.lightserver.DisableJson;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.TimeWindow;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;

public class PredictionContext implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 17110984L;
	private NodeContext nodeContext;
	private int learningWindow;
	private List<TimeWindow> allTimeWindows;
	private PredictionScope scope;
	private LearningModelType modelType;
	private Long id;
	private LearningAggregationOperator aggregationOperator;
	private Map<FeaturesKey, Map<String, VariableFeaturesKey>> mapTransitionMatrixKeys = new HashMap<>();
	private String dumpHistoryFolder;
	// private boolean useCorrections;

	public PredictionContext(NodeContext _nodeContext
			,PredictionScope _scope
			,LearningModelType _modelType
			,int learningWindow
			,List<TimeWindow> _allTimeWindows
			,LearningAggregationOperator aggregationOperator
			,String _dumpDirectory
	// ,boolean _useCorrections
	) {
		this.nodeContext = _nodeContext;
		this.modelType = _modelType;
		this.learningWindow = learningWindow;
		this.allTimeWindows = _allTimeWindows;
		this.scope = _scope;
		this.aggregationOperator = aggregationOperator;
		this.dumpHistoryFolder = _dumpDirectory;
		// this.useCorrections = _useCorrections;
	}

	public PredictionContext() {
		super();
	}

	@DisableJson
	public String getScenario() {
		return nodeContext.getScenario();
	}

	public int getLearningWindow() {
		return learningWindow;
	}

	public void setLearningWindow(int learningWindow) {
		this.learningWindow = learningWindow;
	}

	public PredictionScope getScope() {
		return scope;
	}

	public void setScope(PredictionScope scope) {
		this.scope = scope;
	}

	public LearningModelType getModelType() {
		return modelType;
	}

	public void setModelType(LearningModelType modelType) {
		this.modelType = modelType;
	}

	private PredictionSetting getPredictionSetting() {
		PredictionSetting predictionOption = nodeContext.getPredictionSetting(scope);
		return predictionOption;
	}

	public List<OptionItem> getListScopeItems() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		if(nodeContext.getNodePredicitonSetting().isActivated()) {
			result.add(PredictionScope.NODE.toOptionItem());
		}
		if(nodeContext.getClusterPredictionSetting().isActivated() ) {
			result.add(PredictionScope.CLUSTER.toOptionItem());
		}
		return result;
	}

	public List<OptionItem> getListScopeItemsHavingAggregation() {
		List<OptionItem> result = new ArrayList<OptionItem>();
		PredictionSetting settingsNode = nodeContext.getNodePredicitonSetting();
		if(settingsNode.isActivated() && settingsNode.isAggregationActivated()) {
			result.add(PredictionScope.NODE.toOptionItem());
		}
		PredictionSetting settingsCluster = nodeContext.getClusterPredictionSetting();
		if(settingsCluster.isActivated() && settingsCluster.isAggregationActivated() ) {
			result.add(PredictionScope.CLUSTER.toOptionItem());
		}
		return result;
	}

	public Map<FeaturesKey, Map<String, VariableFeaturesKey>> getMapTransitionMatrixKeys() {
		return mapTransitionMatrixKeys;
	}

	public void setMapTransitionMatrixKeys(Map<FeaturesKey, Map<String, VariableFeaturesKey>> mapTransitionMatrixKeys) {
		this.mapTransitionMatrixKeys = mapTransitionMatrixKeys;
	}

	public List<TimeWindow> getAllTimeWindows() {
		return allTimeWindows;
	}

	public void setAllTimeWindows(List<TimeWindow> allTimeWindows) {
		this.allTimeWindows = allTimeWindows;
	}

	@DisableJson
	public long getTimeShiftMS() {
		return nodeContext.getTimeShiftMS();
		// return timeShiftMS;
	}

	public NodeContext getNodeContext() {
		return nodeContext;
	}

	public void setNodeContext(NodeContext nodeContext) {
		this.nodeContext = nodeContext;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		//return this.nodeContext.getId();
		return this.id;
	}

	public Date getCurrentDate() {
		return nodeContext.getCurrentDate();
	}

	@DisableJson
	public String getMainServiceAddress() {
		return this.nodeContext.getMainServiceAddress();
	}

	@DisableJson
	public NodeLocation getNodeLocation() {
		return this.nodeContext.getNodeLocation();
	}

	@DisableJson
	public boolean isPredictionsActivated() {
		PredictionSetting predictionSetting = getPredictionSetting();
		return predictionSetting.isActivated();
	}

	@DisableJson
	public boolean isAggregationsActivated() {
		PredictionSetting predictionSetting = getPredictionSetting();
		return predictionSetting.isAggregationActivated();
	}

	@DisableJson
	public boolean isModelAggregationsActivated() {
		PredictionSetting predictionSetting = getPredictionSetting();
		return predictionSetting.isModelAggregationActivated();
	}

	@DisableJson
	public boolean isPredictionAggregationsActivated() {
		PredictionSetting predictionSetting = getPredictionSetting();
		return predictionSetting.isPredictionAggregationActivated();
	}

	@DisableJson
	public Integer getNbOfSamplingsBeforeTraining() {
		PredictionSetting predictionSetting = getPredictionSetting();
		return predictionSetting.getNbOfSamplingsBeforeTraining();
	}

	@DisableJson
	public boolean isSupervisionDisabled() {
		return this.nodeContext.isSupervisionDisabled();
	}

	@DisableJson
	public boolean isLocal() {
		return this.nodeContext.isLocal();
	}

	public LearningAggregationOperator getAggregationOperator() {
		return aggregationOperator;
	}

	public void setAggregationOperator(LearningAggregationOperator aggregationOperator) {
		this.aggregationOperator = aggregationOperator;
	}

	public String getDumpHistoryFolder() {
		return dumpHistoryFolder;
	}

	public void setDumpHistoryFolder(String dumpHistoryDirectory) {
		this.dumpHistoryFolder = dumpHistoryDirectory;
	}

	public VariableFeaturesKey getTransitionMatrixKey(FeaturesKey featuresKey, String variable) {
		if (featuresKey != null) {
			if (mapTransitionMatrixKeys.containsKey(featuresKey)) {
				Map<String, VariableFeaturesKey> map1 = mapTransitionMatrixKeys.get(featuresKey);
				if (map1.containsKey(variable)) {
					return map1.get(variable);
				}
			}
		}
		return new VariableFeaturesKey(null, this.id, variable, featuresKey);
	}

	public FeaturesKey getFeaturesKey(Integer timeWindowId) {
		TimeWindow timeWindow = getTimeWindow(timeWindowId);
		return new FeaturesKey(timeWindow);
	}

	private TimeWindow getTimeWindow(Integer timeWindowId) {
		TimeWindow timeWindow = null;
		for (TimeWindow tw : allTimeWindows) {
			if (timeWindow == null && (tw.getId() == timeWindowId)) {
				timeWindow = tw;
			}
		}
		return timeWindow;
	}

	public PredictionStep getStep(Date aDate) throws HandlingException {
		FeaturesKey featureKey = getFeaturesKey2(aDate);
		if(featureKey != null) {
			Date startDate = aDate;
			Date timeWindowEndDate = featureKey.getTimeWindow().getEndDate(aDate);
			if(startDate.after(timeWindowEndDate)) {
				throw new HandlingException("PredictionTimeSlot : startDate " + UtilDates.format_date_time.format(startDate) + " is after end date " +  UtilDates.format_date_time.format(timeWindowEndDate));
			}
			Date endDate = UtilDates.shiftDateMinutes(aDate, LearningAgent.LEARNING_TIME_STEP_MINUTES);
			if(endDate.after(timeWindowEndDate)) {
				endDate = timeWindowEndDate;
			}
			return new PredictionStep(featureKey, startDate, endDate);
		}
		return null;
	}

	public PredictionStep generateStep(Date currentDate, Date targetDate) throws HandlingException {
		FeaturesKey featuresKey = getFeaturesKey2(currentDate);
		if (featuresKey != null) {
			Date startDate = currentDate;
			Date timeWindowEndDate = featuresKey.getTimeWindow().getEndDate(currentDate);
			if (startDate.after(timeWindowEndDate)) {
				throw new HandlingException("PredictionTimeSlot : startDate " + UtilDates.format_date_time.format(startDate)
						+ " is after end date " + UtilDates.format_date_time.format(timeWindowEndDate));
			}
			Date endDate = UtilDates.shiftDateMinutes(currentDate, LearningAgent.LEARNING_TIME_STEP_MINUTES);
			if (endDate.after(timeWindowEndDate)) {
				endDate = timeWindowEndDate;
			}
			if(endDate.after(targetDate)) {
				endDate = targetDate;
			}
			return new PredictionStep(featuresKey, startDate, endDate);
		}
		return null;
	}

	public FeaturesKey getFeaturesKey2(Date aDate) {
		TimeWindow timeWindows = TimeWindow.getTimeWindow(allTimeWindows, aDate);
		return new FeaturesKey(timeWindows);
	}

	public PredictionContext clone() {
		return copyContent(true);
	}

	public PredictionContext copyContent(boolean copyId) {
		PredictionContext result = new PredictionContext();
		NodeContext copyNodeContext = nodeContext.copyContent(copyId);
		if(copyId) {
			result.setId(id);
		}
		result.setNodeContext(copyNodeContext);
		result.setLearningWindow(learningWindow);
		result.setAllTimeWindows(allTimeWindows);
		result.setScope(scope);
		result.setAggregationOperator(aggregationOperator);
		Map<FeaturesKey, Map<String, VariableFeaturesKey>> copy = new HashMap<FeaturesKey, Map<String, VariableFeaturesKey>>();
		for (FeaturesKey key1 : mapTransitionMatrixKeys.keySet()) {
			Map<String, VariableFeaturesKey> map1 = mapTransitionMatrixKeys.get(key1);
			copy.put(key1, new HashMap<String, VariableFeaturesKey>());
			Map<String, VariableFeaturesKey> copyMap1 = copy.get(key1);
			for (String key2 : map1.keySet()) {
				VariableFeaturesKey value2 = map1.get(key2);
				copyMap1.put(key2, value2);
			}
		}
		result.setMapTransitionMatrixKeys(copy);
		return result;
	}


	public FeaturesKey parseFeaturesKey(String code) {
		if(code != null) {
			String[] listCodes = code.split(",");
			String sTimeWindow = listCodes[0];
			Integer timeWindowsStart = Integer.valueOf(sTimeWindow);
			if(timeWindowsStart != null) {
				for(TimeWindow nextTimeWindows : allTimeWindows) {
					if(timeWindowsStart.intValue() == nextTimeWindows.getStartHour()) {
						FeaturesKey result = new FeaturesKey(nextTimeWindows);
						return result;
					}
				}
			}
		}
		return null;
	}

	public List<FeaturesKey> getAllFeaturesKeys() {
		List<FeaturesKey> result = new ArrayList<FeaturesKey>();
		for(TimeWindow nextTimeWindow : allTimeWindows ) {
			result.add(new FeaturesKey(nextTimeWindow));
		}
		return result;
	}

	public List<VariableState> getStatesList() {
		return NodeStates.getStatesList();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (id != null) {
			result.append("id:").append(id);
		}
		result.append(",modelType:").append(modelType);
		result.append(",scope:").append(scope);
		result.append(",aggregationOperator:").append(aggregationOperator);
		result.append(",nodeContext:").append(this.nodeContext);
		return result.toString();
	}

}
