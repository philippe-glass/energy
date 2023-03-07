package com.sapereapi.model.prediction;

import java.util.Date;
import java.util.List;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.UtilDates;

public class StatisticsRequest {
	Date minComputeDay;
	Date maxComputeDay;
	Date minTargetDay;
	Date maxTargetDay;
	Integer minTargetHour;
	Integer maxTargetHour;
	// int horizonInMinutes;
	String location;
	String variableName;
	Boolean useCorrectionFilter = null;
	boolean mergeHorizons;
	boolean mergeUseCorrections;
	List<String> fieldsToMerge;

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

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
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

	public List<String> getFieldsToMerge() {
		return fieldsToMerge;
	}

	public void setFieldsToMerge(List<String> fieldsToMerge) {
		this.fieldsToMerge = fieldsToMerge;
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
