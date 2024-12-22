package com.sapereapi.model;

public class HandlingException extends Exception {
	// String message;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HandlingException(String message) {
		super(message);
	}

	@Override
	public String toString() {
		String message = getLocalizedMessage();
        return (message != null) ?  message : getClass().getName();
	}
}
