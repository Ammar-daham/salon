package com.example.salon.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        // A 5xx is a server fault worth a stack trace; a 4xx is expected client input, so keep it quiet.
        if (ex.getStatus().is5xxServerError()) {
            log.error("Server error at {}", request.getRequestURI(), ex);
        }
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * An AccessDeniedException thrown inside a controller/service (via AccessControl) would otherwise be
     * caught by the catch-all below and turned into a 500. Handle it here so it stays a 403 — matching the
     * filter-level RestAccessDeniedHandler for requests blocked before the controller.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN.getStatus(), ErrorCode.FORBIDDEN.getCode(),
                "You do not have permission to perform this action", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation at {}", request.getRequestURI(), ex);
        return build(ErrorCode.BAD_REQUEST.getStatus(), ErrorCode.BAD_REQUEST.getCode(),
                "One or more fields violate database constraints", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(ErrorCode.BAD_REQUEST.getStatus(), ErrorCode.BAD_REQUEST.getCode(),
                "Malformed JSON request body", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        log.error("Database error at {}", request.getRequestURI(), ex);
        return build(ErrorCode.DATABASE_ERROR.getStatus(), ErrorCode.DATABASE_ERROR.getCode(),
                "Database error occurred.", request);
    }

    /**
     * BE-27/BE-34: a dedicated NullPointerException handler used to turn every server-side NPE into a
     * 400 with nothing logged, hiding real bugs as client errors. Any unhandled exception (NPE included)
     * is now a logged 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR.getStatus(), ErrorCode.INTERNAL_ERROR.getCode(),
                "An unexpected error occurred.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setErrorCode(code);
        error.setMessage(message);
        error.setStatus(status);
        error.setTimestamp(Instant.now());
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
