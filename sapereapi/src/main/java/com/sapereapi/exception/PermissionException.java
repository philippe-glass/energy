package com.sapereapi.exception;

import com.sapereapi.model.HandlingException;

public class PermissionException extends HandlingException {
	private static final long serialVersionUID = 1521L;

	public PermissionException(String message) {
		super(message);
	}
}
