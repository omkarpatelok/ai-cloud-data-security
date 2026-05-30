package com.security.cloudscanner.controller;

import com.security.cloudscanner.entity.ScanResult;
import com.security.cloudscanner.repository.ScanResultRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityDashboardControllerTest {

    @Test
    void returnsSummaryCounts() {
        ScanResultRepository repository = mock(ScanResultRepository.class);
        when(repository.count()).thenReturn(120L);
        when(repository.countByRiskLevel("HIGH")).thenReturn(10L);
        when(repository.countByRiskLevel("CRITICAL")).thenReturn(8L);
        when(repository.countByRiskLevel("MEDIUM")).thenReturn(37L);
        when(repository.countByRiskLevel("LOW")).thenReturn(65L);

        SecurityDashboardController controller = new SecurityDashboardController(repository);
        SecuritySummaryResponse response = controller.getSummary();

        assertEquals(120L, response.totalScans());
        assertEquals(18L, response.highRiskFiles());
        assertEquals(37L, response.mediumRiskFiles());
        assertEquals(65L, response.lowRiskFiles());
    }

    @Test
    void returnsHighRiskResults() {
        ScanResultRepository repository = mock(ScanResultRepository.class);
        ScanResult scanResult = new ScanResult();
        scanResult.setRiskLevel("CRITICAL");
        scanResult.setCreatedAt(LocalDateTime.now());

        when(repository.findByRiskLevelInOrderByCreatedAtDesc(List.of("HIGH", "CRITICAL")))
                .thenReturn(List.of(scanResult));

        SecurityDashboardController controller = new SecurityDashboardController(repository);

        assertEquals(1, controller.getHighRiskResults().size());
        assertEquals("CRITICAL", controller.getHighRiskResults().get(0).getRiskLevel());
    }
}
