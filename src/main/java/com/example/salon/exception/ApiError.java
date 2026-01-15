package com.example.salon.exception;

public class ApiError {

    private final String code;
    private final String message;
    private final int status;

    public ApiError(ErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message != null ? message : errorCode.getMessage();
        this.status = errorCode.getStatus().value();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
