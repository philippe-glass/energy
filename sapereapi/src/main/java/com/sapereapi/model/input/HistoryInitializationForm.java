package com.sapereapi.model.input;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.PredictionScope;

public class HistoryInitializationForm {
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Date[] listDates;
	private Double[] requested;
	private Double[] produced;
	private Double[] consumed;
	private Double[] consumedLocally;
	private Double[] provided;
	private Double[] providedLocally;
	private Double[] available;
	private Double[] missing;
	private Double[] providedMargin;
	private String[] listVariables;
	private Boolean completeMatrices;
	private OptionItem scope = null;
	private LearningModelType usedModel = null;
	private String nodeName = null;
	private Date dateMin = null;
	private Date dateMax = null;

	public Date[] getListDates() {
		return listDates;
	}

	public void setListDates(Date[] listDates) {
		this.listDates = listDates;
	}

	public Double[] getRequested() {
		return requested;
	}

	public void setRequested(Double[] requested) {
		this.requested = requested;
	}

	public Double[] getProduced() {
		return produced;
	}

	public void setProduced(Double[] produced) {
		this.produced = produced;
	}

	public Double[] getConsumed() {
		return consumed;
	}

	public void setConsumed(Double[] consumed) {
		this.consumed = consumed;
	}

	public Double[] getConsumedLocally() {
		return consumedLocally;
	}

	public void setConsumedLocally(Double[] consumedLocally) {
		this.consumedLocally = consumedLocally;
	}

	public Double[] getProvided() {
		return provided;
	}

	public void setProvided(Double[] provided) {
		this.provided = provided;
	}

	public Double[] getProvidedLocally() {
		return providedLocally;
	}

	public void setProvidedLocally(Double[] providedLocally) {
		this.providedLocally = providedLocally;
	}

	public Double[] getAvailable() {
		return available;
	}

	public void setAvailable(Double[] available) {
		this.available = available;
	}

	public Double[] getMissing() {
		return missing;
	}

	public void setMissing(Double[] missing) {
		this.missing = missing;
	}

	public Double[] getProvidedMargin() {
		return providedMargin;
	}

	public void setProvidedMargin(Double[] providedMargin) {
		this.providedMargin = providedMargin;
	}

	public Boolean getCompleteMatrices() {
		return completeMatrices;
	}

	public void setCompleteMatrices(Boolean completeMatrices) {
		this.completeMatrices = completeMatrices;
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

	public LearningModelType getUsedModel() {
		return usedModel;
	}

	public void setUsedModel(LearningModelType usedModel) {
		this.usedModel = usedModel;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String[] getListVariables() {
		return listVariables;
	}

	public void setListVariables(String[] listVariables) {
		this.listVariables = listVariables;
	}

	public Date getDateMin() {
		return dateMin;
	}

	public void setDateMin(Date dateMin) {
		this.dateMin = dateMin;
	}

	public Date getDateMax() {
		return dateMax;
	}

	public void setDateMax(Date dateMax) {
		this.dateMax = dateMax;
	}

}
