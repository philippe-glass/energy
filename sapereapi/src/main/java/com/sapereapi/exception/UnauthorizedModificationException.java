package com.sapereapi.exception;

import com.sapereapi.model.HandlingException;

public class UnauthorizedModificationException extends HandlingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnauthorizedModificationException(String message) {
		super(message);
	}
}
