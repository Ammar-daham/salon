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
