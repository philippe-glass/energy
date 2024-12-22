package com.sapereapi.model.input;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.prediction.PredictionContext;

public class HistoryInitializationRequest {
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssZ")
	private Boolean completeMatrices;
	private OptionItem scope = null;
	private LearningModelType usedModel = null;
	private String nodeName = null;
	private String[] listVariables;
	private Date dateMin = null;
	private Date dateMax = null;

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

	public static HistoryInitializationRequest generateHistoryInitReuqest(PredictionContext predictionContext) {
		HistoryInitializationRequest historyInitRequest = new HistoryInitializationRequest();
		historyInitRequest.setNodeName(predictionContext.getNodeLocation().getName());
		historyInitRequest.setScope(predictionContext.getScope().toOptionItem());
		historyInitRequest.setListVariables(predictionContext.getNodeContext().getVariables());
		historyInitRequest.setUsedModel(predictionContext.getModelType());
		return historyInitRequest;
	}
}
