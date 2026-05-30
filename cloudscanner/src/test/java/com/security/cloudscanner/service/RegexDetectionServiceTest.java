package com.security.cloudscanner.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexDetectionServiceTest {

    private final RegexDetectionService regexDetectionService = new RegexDetectionService();

    @Test
    void detectsSensitiveDocumentPatterns() {
        String content = """
                PAN: ABCDE1234F
                Aadhaar: 1234 5678 9012
                Card: 4111 1111 1111 1111
                api_key = "supersecretapikey12345"
                """;

        List<String> findings = regexDetectionService.detect(content);

        assertTrue(findings.contains("PAN"));
        assertTrue(findings.contains("AADHAAR"));
        assertTrue(findings.contains("CREDIT_CARD"));
        assertTrue(findings.contains("API_KEY"));
    }

    @Test
    void detectsSourceCodeSecrets() {
        String content = """
                aws_key=AKIA1234567890ABCDEF
                jwt_secret=my-jwt-secret-value
                -----BEGIN PRIVATE KEY-----
                password=ProdSecret123
                """;

        List<String> findings = regexDetectionService.detect(content);

        assertTrue(findings.contains("AWS_ACCESS_KEY"));
        assertTrue(findings.contains("JWT_SECRET"));
        assertTrue(findings.contains("PRIVATE_KEY"));
        assertTrue(findings.contains("DB_CREDENTIAL"));
    }
}
