package com.example.salon.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException 
{
    private final String errorCode;
    private final HttpStatus status;

    public BaseException(String message, ErrorCode errorCode) 
    {
        super(message);
        this.errorCode = errorCode.getCode();
        this.status = errorCode.getStatus();
    }
}
