package com.security.cloudscanner.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void returnsClearMessageForResponseStatusException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scans/s3-event");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "No S3 object keys found in event payload"),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals("No S3 object keys found in event payload", response.getBody().message());
        assertEquals("/api/scans/s3-event", response.getBody().path());
    }

    @Test
    void returnsNotFoundForMissingS3Object() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scans");

        ResponseEntity<ApiErrorResponse> response = handler.handleS3ObjectNotFound(
                new S3ObjectNotFoundException("S3 object 'sample-data.txt' was not found.", null),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals("S3 object 'sample-data.txt' was not found.", response.getBody().message());
    }

    @Test
    void returnsBadGatewayForS3AccessProblems() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scans");

        ResponseEntity<ApiErrorResponse> response = handler.handleS3AccessException(
                new S3AccessException("Access denied while reading object.", null),
                request
        );

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("Access denied"));
    }
}
