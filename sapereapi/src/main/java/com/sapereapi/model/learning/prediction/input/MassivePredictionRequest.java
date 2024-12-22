package com.sapereapi.model.learning.prediction.input;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.util.UtilDates;

public class MassivePredictionRequest {
	//private NodeLocation nodeLocation;
	private String nodeName;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Date targetDay;
	//private Long longTargetDay;
	private int targetHour;
	private int horizonInMinutes;
	//private String variableName;
	private String[] listVariableNames;
	private boolean useCorrections;
	private boolean generateCorrections;
	private boolean savePredictions;
	private OptionItem scope = null;

	public Date getTargetDay() {
		return targetDay;
	}

	public void setTargetDay(Date targetDay) {
		this.targetDay = targetDay;
	}

	public int getTargetHour() {
		return targetHour;
	}

	public void setTargetHour(int targetHour) {
		this.targetHour = targetHour;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public int getHorizonInMinutes() {
		return horizonInMinutes;
	}

	public void setHorizonInMinutes(int horizonInMinutes) {
		this.horizonInMinutes = horizonInMinutes;
	}

	public boolean isUseCorrections() {
		return useCorrections;
	}

	public void setUseCorrections(boolean useCorrections) {
		this.useCorrections = useCorrections;
	}

	public boolean isGenerateCorrections() {
		return generateCorrections;
	}

	public void setGenerateCorrections(boolean generateCorrections) {
		this.generateCorrections = generateCorrections;
	}

	public boolean isSavePredictions() {
		return savePredictions;
	}

	public void setSavePredictions(boolean savePrediction) {
		this.savePredictions = savePrediction;
	}

	public String[] getListVariableNames() {
		return listVariableNames;
	}

	public void setListVariableNames(String[] listVariableNames) {
		this.listVariableNames = listVariableNames;
	}

	public OptionItem getScope() {
		return scope;
	}

	public void setScope(OptionItem scope) {
		this.scope = scope;
	}

	public PredictionScope getScopeEnum() {
		return PredictionScope.valueOf(scope.getLabel());
	}

	public TimeSlot getTimeSlot() {
		Date baseTargetDate = UtilDates.removeTime(targetDay /*new Date(longTargetDay)*/);
		int targetHour = this.getTargetHour();
		Date targetDateMin = UtilDates.shiftDateMinutes(baseTargetDate, 60*targetHour);
		Date targetDateMax = UtilDates.shiftDateMinutes(targetDateMin, 60);
		return new TimeSlot(targetDateMin, targetDateMax);
	}

	public MassivePredictionRequest() {
		super();
	}

}
