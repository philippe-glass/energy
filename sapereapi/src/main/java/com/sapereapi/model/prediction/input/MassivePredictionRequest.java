package com.sapereapi.model.prediction.input;

import java.util.Date;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.UtilDates;

public class MassivePredictionRequest {
	//private NodeConfig nodeLocation;
	private String nodeName;
	//private Date targetDay;
	private Long longTargetDay;
	private int targetHour;
	private int horizonInMinutes;
	private String variableName;
	private boolean useCorrections;
	private boolean generateCorrections;

	public Long getLongTargetDay() {
		return longTargetDay;
	}

	public void setLongTargetDay(Long longTargetDay) {
		this.longTargetDay = longTargetDay;
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

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
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

	public TimeSlot getTimeSlot() {
		Date baseTargetDate = UtilDates.removeTime(new Date(longTargetDay));
		int targetHour = this.getTargetHour();
		Date targetDateMin = UtilDates.shiftDateMinutes(baseTargetDate, 60*targetHour);
		Date targetDateMax = UtilDates.shiftDateMinutes(targetDateMin, 60);
		return new TimeSlot(targetDateMin, targetDateMax);
	}

	public MassivePredictionRequest() {
		super();
	}

}
