package com.security.cloudscanner.risk;

import com.security.cloudscanner.scanner.S3MetadataResult;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

    public int calculateRiskScore(String dataType, String sensitivity, String policyAction, S3MetadataResult metadata) {
        return calculateRiskScore(dataType, sensitivity, policyAction, metadata, 0);
    }

    public int calculateRiskScore(
            String dataType,
            String sensitivity,
            String policyAction,
            S3MetadataResult metadata,
            int detectionScore
    ) {
        return calculateRiskScore(dataType, sensitivity, policyAction, metadata, detectionScore, "SCANNED");
    }

    public int calculateRiskScore(
            String dataType,
            String sensitivity,
            String policyAction,
            S3MetadataResult metadata,
            int detectionScore,
            String extractionStatus
    ) {

        int score = 0;

        if (detectionScore > 0) {
            score += detectionScore;
        }

        if ("PII".equals(dataType)) score += 30;
        if ("HIGH".equals(sensitivity)) score += 30;
        else if ("MEDIUM".equals(sensitivity)) score += 20;

        if ("BLOCK".equals(policyAction)) score += 10;
        if (metadata.isPublicBucket()) score += 30;
        if (!metadata.isEncryptionEnabled()) score += 20;
        score += extractionRiskAdjustment(extractionStatus);

        return Math.min(score, 100);
    }

    private int extractionRiskAdjustment(String extractionStatus) {
        if (extractionStatus == null) {
            return 0;
        }

        return switch (extractionStatus) {
            case "ENCRYPTED" -> 60;
            case "EXTRACTION_FAILED" -> 50;
            case "UNSUPPORTED" -> 35;
            case "OCR_REQUIRED" -> 30;
            default -> 0;
        };
    }

    public String determineRiskLevel(int score) {
        if (score >= 70) {
            return "CRITICAL";
        } else if (score >= 50) {
            return "HIGH";
        } else if (score >= 30) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
