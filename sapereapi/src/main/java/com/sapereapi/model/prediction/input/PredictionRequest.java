package com.sapereapi.model.prediction.input;

public class PredictionRequest {
	//private NodeConfig nodeLocation;
	private String nodeName;
	private Long longInitDate;
	private Long longTargetDate;
	//private Date initDate;
	//private Date targetDate;
	private boolean useCorrections = false;


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

	public PredictionRequest() {
		super();
	}

}
