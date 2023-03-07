package com.sapereapi.model;

public class OperationResult {
	public static final long serialVersionUID = 1456L;
	private Boolean isSuccessful;
	private String comment;

	public Boolean getIsSuccessful() {
		return isSuccessful;
	}

	public void setIsSuccessful(Boolean isSuccessful) {
		this.isSuccessful = isSuccessful;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public OperationResult() {
		super();
	}

	public OperationResult(Boolean isSuccessful, String _comment) {
		super();
		this.isSuccessful = isSuccessful;
		this.comment = _comment;
	}

}
