package com.security.cloudscanner.controller;

public record ScanRequest(
        String resourceName,
        String resourceType
) {}