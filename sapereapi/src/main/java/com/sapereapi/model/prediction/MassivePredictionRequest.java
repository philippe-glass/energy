package com.sapereapi.model.prediction;

import java.util.Date;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.util.UtilDates;

public class MassivePredictionRequest {
	Date targetDay;
	int targetHour;
	int horizonInMinutes;
	String location;
	String variableName;
	boolean useCorrections;
	boolean generateCorrections;

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
		Date baseTargetDate = UtilDates.removeTime(targetDay);
		int targetHour = this.getTargetHour();
		Date targetDateMin = UtilDates.shiftDateMinutes(baseTargetDate, 60*targetHour);
		Date targetDateMax = UtilDates.shiftDateMinutes(targetDateMin, 60);
		return new TimeSlot(targetDateMin, targetDateMax);
	}

	public MassivePredictionRequest() {
		super();
	}

}
