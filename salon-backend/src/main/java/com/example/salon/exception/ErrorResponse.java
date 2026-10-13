package com.example.salon.exception;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * The body of every error: {@code errorCode}, the stable code a client acts on, from {@link ErrorCode};
 * {@code message}, for people; the HTTP {@code status}; the {@code timestamp}; and the request's
 * {@code path}. Unlike every other body, its fields are camelCase.
 */
@Data
@NoArgsConstructor
public class ErrorResponse {
    private String errorCode;
    private String message;
    private HttpStatus status;
    private Instant timestamp;
    private String path;
}
