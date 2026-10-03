package com.aicodereview.exception;

import org.springframework.http.HttpStatus;

/** Failure of an external dependency (GitHub, LLM...). Mapped to the given HTTP status (default 502). */
public class ExternalServiceException extends RuntimeException {
    private final HttpStatus status;
    public ExternalServiceException(String message) { this(message, HttpStatus.BAD_GATEWAY); }
    public ExternalServiceException(String message, HttpStatus status) { super(message); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
