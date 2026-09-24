package com.example.salon.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Application error carrying a stable wire code and HTTP status, both taken from {@link ErrorCode}
 * so call sites can't hand-type a code (BE-18 - that is how "NOT_FOUNT" reached the wire).
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public BaseException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.status = errorCode.getStatus();
    }
}
