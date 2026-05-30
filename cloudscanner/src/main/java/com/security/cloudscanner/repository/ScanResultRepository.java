package com.security.cloudscanner.repository;

import com.security.cloudscanner.entity.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {

    long countByRiskLevel(String riskLevel);

    List<ScanResult> findByRiskLevelInOrderByCreatedAtDesc(List<String> riskLevels);
}
