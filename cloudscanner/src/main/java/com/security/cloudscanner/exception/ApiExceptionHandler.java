package com.security.cloudscanner.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = ex.getStatusCode();
        String message = ex.getReason() != null ? ex.getReason() : "Request could not be processed.";
        return buildResponse(statusCode.value(), statusCode.toString(), message, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();
        String message = """
                Invalid JSON request body. Use POST /api/scans with {"resourceName":"file.txt","resourceType":"S3_OBJECT"} \
                or POST /api/scans/s3-event with {"Records":[{"s3":{"object":{"key":"file.txt"}}}]}.
                """.replaceAll("\\s+", " ").trim();
        return buildResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), message, path);
    }

    @ExceptionHandler(S3ObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleS3ObjectNotFound(
            S3ObjectNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(S3AccessException.class)
    public ResponseEntity<ApiErrorResponse> handleS3AccessException(
            S3AccessException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_GATEWAY.value(), HttpStatus.BAD_GATEWAY.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ScanProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleScanProcessingException(
            ScanProcessingException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        String message = "Unexpected server error while processing the scan. Check backend logs for more details.";
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), message, request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(int status, String error, String message, String path) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                status,
                error,
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }
}
