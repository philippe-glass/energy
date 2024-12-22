package com.sapereapi.exception;

import com.sapereapi.model.HandlingException;

public class DoublonException extends HandlingException {
	private static final long serialVersionUID = 1779L;

	public DoublonException(String message) {
		super(message);
	}
}
