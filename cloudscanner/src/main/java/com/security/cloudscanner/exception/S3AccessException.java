package com.security.cloudscanner.exception;

public class S3AccessException extends RuntimeException {

    public S3AccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
