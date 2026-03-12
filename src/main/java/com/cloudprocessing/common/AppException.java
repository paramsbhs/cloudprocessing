package com.cloudprocessing.common;

import org.springframework.http.HttpStatus;

/**
 * Domain-level exception that carries an HTTP status.
 * Thrown anywhere in the service layer; caught by GlobalExceptionHandler.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // ── Factory helpers ───────────────────────────────────────────────

    public static AppException notFound(String message) {
        return new AppException(message, HttpStatus.NOT_FOUND);
    }

    public static AppException conflict(String message) {
        return new AppException(message, HttpStatus.CONFLICT);
    }

    public static AppException badRequest(String message) {
        return new AppException(message, HttpStatus.BAD_REQUEST);
    }

    public static AppException forbidden(String message) {
        return new AppException(message, HttpStatus.FORBIDDEN);
    }

    public static AppException unauthorized(String message) {
        return new AppException(message, HttpStatus.UNAUTHORIZED);
    }
}
