package com.sapereapi.exception;

import com.sapereapi.model.HandlingException;

public class MissingMeasureException extends HandlingException {
	private static final long serialVersionUID = 1221L;

	public MissingMeasureException(String message) {
		super(message);
	}
}
