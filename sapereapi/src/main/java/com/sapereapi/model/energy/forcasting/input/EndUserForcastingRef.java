package com.sapereapi.model.energy.forcasting.input;

import java.util.List;

import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.OptionItem;

import java.util.ArrayList;

public class EndUserForcastingRef {
	List<OptionItem> listOfYears = new ArrayList<>();
	List<OptionItem> listOfMonths = new ArrayList<>();
	List<OptionItem> listOfDays = new ArrayList<>();
	List<OptionItem> listOfTimes = new ArrayList<>();
	String defaultTime = null;
	TimeSlot datesInterval = new TimeSlot();

	public List<OptionItem> getListOfTimes() {
		return listOfTimes;
	}

	public void setListOfTimes(List<OptionItem> listOfTimes) {
		this.listOfTimes = listOfTimes;
	}

	public List<OptionItem> getListOfYears() {
		return listOfYears;
	}

	public void setListOfYears(List<OptionItem> listOfYears) {
		this.listOfYears = listOfYears;
	}

	public List<OptionItem> getListOfMonths() {
		return listOfMonths;
	}

	public void setListOfMonths(List<OptionItem> listOfMonths) {
		this.listOfMonths = listOfMonths;
	}

	public List<OptionItem> getListOfDays() {
		return listOfDays;
	}

	public void setListOfDays(List<OptionItem> listOfDays) {
		this.listOfDays = listOfDays;
	}

	public String getDefaultTime() {
		return defaultTime;
	}

	public void setDefaultTime(String defaultTime) {
		this.defaultTime = defaultTime;
	}

	public TimeSlot getDatesInterval() {
		return datesInterval;
	}

	public void setDatesInterval(TimeSlot datesInterval) {
		this.datesInterval = datesInterval;
	}

}
