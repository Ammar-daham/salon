package com.example.salon.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler
{
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabaseException(DataAccessException ex)
    {
        ApiError error = new ApiError(ErrorCode.DATABASE_ERROR, null);
        return ResponseEntity
                .status(ErrorCode.DATABASE_ERROR.getStatus())
                .body(error);
    }
}
