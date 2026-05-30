package com.security.cloudscanner.controller;

import com.security.cloudscanner.entity.ScanResult;
import com.security.cloudscanner.repository.ScanResultRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityDashboardController {

    private final ScanResultRepository scanResultRepository;

    public SecurityDashboardController(ScanResultRepository scanResultRepository) {
        this.scanResultRepository = scanResultRepository;
    }

    @GetMapping("/summary")
    public SecuritySummaryResponse getSummary() {
        long totalScans = scanResultRepository.count();
        long highRiskFiles = scanResultRepository.countByRiskLevel("HIGH")
                + scanResultRepository.countByRiskLevel("CRITICAL");
        long mediumRiskFiles = scanResultRepository.countByRiskLevel("MEDIUM");
        long lowRiskFiles = scanResultRepository.countByRiskLevel("LOW");

        return new SecuritySummaryResponse(
                totalScans,
                highRiskFiles,
                mediumRiskFiles,
                lowRiskFiles
        );
    }

    @GetMapping("/high-risk")
    public List<ScanResult> getHighRiskResults() {
        return scanResultRepository.findByRiskLevelInOrderByCreatedAtDesc(List.of("HIGH", "CRITICAL"));
    }
}
