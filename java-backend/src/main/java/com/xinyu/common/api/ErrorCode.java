package com.xinyu.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, "success", HttpStatus.OK),
    VALIDATION_ERROR(40000, "request validation failed", HttpStatus.BAD_REQUEST),
    INVALID_ARGUMENT(40002, "invalid request", HttpStatus.BAD_REQUEST),
    AUTH_INVALID_CREDENTIALS(40100, "invalid credentials", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID(40101, "invalid or expired token", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_REUSED(40102, "refresh token is no longer valid", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "access denied", HttpStatus.FORBIDDEN),
    USER_DISABLED(40301, "user account is disabled", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(40900, "resource already exists", HttpStatus.CONFLICT),
    INTERNAL_ERROR(50000, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
