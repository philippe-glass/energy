package com.sapereapi.model.learning.prediction.input;

import com.sapereapi.model.OptionItem;
import com.sapereapi.model.learning.PredictionScope;

public class PredictionRequest {
	//private NodeLocation nodeLocation;
	private String nodeName;
	private Long longInitDate;
	private Long longTargetDate;
	//private Date initDate;
	//private Date targetDate;
	private boolean useCorrections = false;
	private OptionItem scope = null;


	public Long getLongInitDate() {
		return longInitDate;
	}

	public void setLongInitDate(Long longInitDate) {
		this.longInitDate = longInitDate;
	}

	public Long getLongTargetDate() {
		return longTargetDate;
	}

	public void setLongTargetDate(Long longTargetDate) {
		this.longTargetDate = longTargetDate;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public boolean isUseCorrections() {
		return useCorrections;
	}

	public void setUseCorrections(boolean useCorrections) {
		this.useCorrections = useCorrections;
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

	public PredictionRequest() {
		super();
	}

}
