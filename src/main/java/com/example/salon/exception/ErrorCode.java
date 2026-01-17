package com.example.salon.exception;
import org.springframework.http.HttpStatus;


public enum ErrorCode
{
    NOT_FOUND(HttpStatus.NOT_FOUND, "404", "NOT_FOUND"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCES_409", "CONFLICT"),

    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB_500", "DATABASE_ERROR");


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
