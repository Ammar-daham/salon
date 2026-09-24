package com.example.salon.exception;

import org.springframework.http.HttpStatus;


public enum ErrorCode
{
	NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN"),
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "CONFLICT"),
	DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR"),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST"),
	NULL_VALUE(HttpStatus.BAD_REQUEST, "REQUIRED_FIELD_MISSING"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");

	private final HttpStatus status;
	private final String code;

	// BE-18: the old constructor discarded its code argument (and the NOT_FOUND label was a typo,
	// "NOT-FOUNT_404"), so the wire never carried a stable code. Both are now stored and exposed.
	ErrorCode(HttpStatus status, String code)
	{
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus()
	{
		return status;
	}

	public String getCode()
	{
		return code;
	}
}
