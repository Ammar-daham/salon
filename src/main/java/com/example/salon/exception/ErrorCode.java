package com.example.salon.exception;
import org.springframework.http.HttpStatus;


public enum ErrorCode
{
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB_500", "Database error");

    private final HttpStatus status;
    private final String message;
    private final String code;

    ErrorCode(HttpStatus status, String message, String code)
    {
        this.status = status;
        this.message = message;
        this.code = code;
    }

    public HttpStatus getStatus()
    {
        return status;
    }

    public String getMessage()
    {
        return message;
    }

    public String getCode()
    {
        return code;
    }
}
