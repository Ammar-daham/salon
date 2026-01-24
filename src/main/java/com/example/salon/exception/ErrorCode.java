package com.example.salon.exception;
import org.springframework.http.HttpStatus;


public enum ErrorCode
{
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT-FOUNT_404", "NOT_FOUND"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCES_409", "CONFLICT"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB_500", "DATABASE_ERROR"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST_400", "BAD_REQUEST"),
    NULL_VALUE(HttpStatus.BAD_REQUEST, "NULL_VALUE_400", "REQUIRED_FIELD_MISSING");

    private final HttpStatus status;

    ErrorCode(HttpStatus status, String message, String code)
    {
        this.status = status;
    }

    public HttpStatus getStatus()
    {
        return status;
    }

}
