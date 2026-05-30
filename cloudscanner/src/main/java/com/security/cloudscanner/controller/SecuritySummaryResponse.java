package com.security.cloudscanner.controller;

public record SecuritySummaryResponse(
        long totalScans,
        long highRiskFiles,
        long mediumRiskFiles,
        long lowRiskFiles
) {
}
