package com.security.cloudscanner.exception;

public class ScanProcessingException extends RuntimeException {

    public ScanProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
