package com.sapereapi.model.prediction.input;

import java.util.Date;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeConfig;

public class StatisticsRequest {
	/*
	private Date minComputeDay;
	private Date maxComputeDay;
	private Date minTargetDay;
	private Date maxTargetDay;
	*/
	private Long longMinComputeDay;
	private Long longMaxComputeDay;
	private Long longMinTargetDay;
	private Long longMaxTargetDay;
	private Integer minTargetHour;
	private Integer maxTargetHour;
	// int horizonInMinutes;
	private NodeConfig nodeLocation;
	private String variableName;
	private Boolean useCorrectionFilter = null;
	boolean mergeHorizons;
	boolean mergeUseCorrections;
	private String[] fieldsToMerge;

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

	public Long getLongMinComputeDay() {
		return longMinComputeDay;
	}

	public void setLongMinComputeDay(Long longMinComputeDay) {
		this.longMinComputeDay = longMinComputeDay;
	}

	public Long getLongMaxComputeDay() {
		return longMaxComputeDay;
	}

	public void setLongMaxComputeDay(Long longMaxComputeDay) {
		this.longMaxComputeDay = longMaxComputeDay;
	}

	public Long getLongMinTargetDay() {
		return longMinTargetDay;
	}

	public void setLongMinTargetDay(Long longMinTargetDay) {
		this.longMinTargetDay = longMinTargetDay;
	}

	public Long getLongMaxTargetDay() {
		return longMaxTargetDay;
	}

	public void setLongMaxTargetDay(Long longMaxTargetDay) {
		this.longMaxTargetDay = longMaxTargetDay;
	}

	public NodeConfig getNodeLocation() {
		return nodeLocation;
	}

	public void setNodeLocation(NodeConfig nodeLocation) {
		this.nodeLocation = nodeLocation;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
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

	public TimeSlot getTimeSlot() {
		Date baseMinTargetDate = UtilDates.removeTime(new Date(longMinTargetDay));
		Date targetDateMin = UtilDates.shiftDateMinutes(baseMinTargetDate, 60 * getMinTargetHour());
		Date baseMaxTargetDate = UtilDates.removeTime(new Date(longMaxTargetDay));
		Date targetDateMax = UtilDates.shiftDateMinutes(baseMaxTargetDate, 60 * (1 + getMaxTargetHour()));
		return new TimeSlot(targetDateMin, targetDateMax);
	}

	public StatisticsRequest() {
		super();
	}

}
