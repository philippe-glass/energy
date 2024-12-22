package com.sapereapi.model.learning.prediction.input;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;

public class StatisticsRequest {
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Date minComputeDay;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Date maxComputeDay;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Date minTargetDay;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Date maxTargetDay;

	private Integer minTargetHour;
	private Integer maxTargetHour;
	// int horizonInMinutes;
	private NodeLocation nodeLocation;
	private String[] listVariableNames;
	private Boolean useCorrectionFilter = null;
	boolean mergeHorizons;
	boolean mergeUseCorrections;
	private String[] fieldsToMerge;
	private Integer minPredictionsCount;
	private OptionItem scope = null;
	private Long predictionContextId = null;
	private Boolean includeTrivials = null;

	public Integer getMinTargetHour() {
		return minTargetHour;
	}

	public void setMinTargetHour(Integer minTargetHour) {
		this.minTargetHour = minTargetHour;
	}

	public Integer getMaxTargetHour() {
		return maxTargetHour;
	}

	public void setMaxTargetHour(Integer maxTargetHour) {
		this.maxTargetHour = maxTargetHour;
	}

	public Date getMinComputeDay() {
		return minComputeDay;
	}

	public void setMinComputeDay(Date minComputeDay) {
		this.minComputeDay = minComputeDay;
	}

	public Date getMaxComputeDay() {
		return maxComputeDay;
	}

	public void setMaxComputeDay(Date maxComputeDay) {
		this.maxComputeDay = maxComputeDay;
	}

	public Date getMinTargetDay() {
		return minTargetDay;
	}

	public void setMinTargetDay(Date minTargetDay) {
		this.minTargetDay = minTargetDay;
	}

	public Date getMaxTargetDay() {
		return maxTargetDay;
	}

	public void setMaxTargetDay(Date maxTargetDay) {
		this.maxTargetDay = maxTargetDay;
	}

	public NodeLocation getNodeLocation() {
		return nodeLocation;
	}

	public void setNodeLocation(NodeLocation nodeLocation) {
		this.nodeLocation = nodeLocation;
	}

	public String[] getListVariableNames() {
		return listVariableNames;
	}

	public void setListVariableNames(String[] listVariableNames) {
		this.listVariableNames = listVariableNames;
	}

	public boolean isMergeHorizons() {
		return mergeHorizons;
	}

	public void setMergeHorizons(boolean mergeHorizons) {
		this.mergeHorizons = mergeHorizons;
	}

	public boolean isMergeUseCorrections() {
		return mergeUseCorrections;
	}

	public void setMergeUseCorrections(boolean mergeUseCorrections) {
		this.mergeUseCorrections = mergeUseCorrections;
	}

	public Boolean getUseCorrectionFilter() {
		return useCorrectionFilter;
	}

	public void setUseCorrectionFilter(Boolean useCorrectionFilter) {
		this.useCorrectionFilter = useCorrectionFilter;
	}

	public String[] getFieldsToMerge() {
		return fieldsToMerge;
	}

	public void setFieldsToMerge(String[] fieldsToMerge) {
		this.fieldsToMerge = fieldsToMerge;
	}

	public Integer getMinPredictionsCount() {
		return minPredictionsCount;
	}

	public void setMinPredictionsCount(Integer minPredictionsCount) {
		this.minPredictionsCount = minPredictionsCount;
	}

	public OptionItem getScope() {
		return scope;
	}

	public void setScope(OptionItem scope) {
		this.scope = scope;
	}

	public Long getPredictionContextId() {
		return predictionContextId;
	}

	public void setPredictionContextId(Long predictionContextId) {
		this.predictionContextId = predictionContextId;
	}

	public PredictionScope getScopeEnum() {
		return PredictionScope.valueOf(scope.getLabel());
	}

	public Boolean getIncludeTrivials() {
		return includeTrivials;
	}

	public void setIncludeTrivials(Boolean includeTrivials) {
		this.includeTrivials = includeTrivials;
	}

	public TimeSlot getTimeSlot() {
		Date baseMinTargetDate = UtilDates.removeTime(minTargetDay);
		Date targetDateMin = UtilDates.shiftDateMinutes(baseMinTargetDate, 60 * getMinTargetHour());
		Date baseMaxTargetDate = UtilDates.removeTime(maxTargetDay);
		Date targetDateMax = UtilDates.shiftDateMinutes(baseMaxTargetDate, 60 * (1 + getMaxTargetHour()));
		return new TimeSlot(targetDateMin, targetDateMax);
	}

	public StatisticsRequest() {
		super();
	}

}
