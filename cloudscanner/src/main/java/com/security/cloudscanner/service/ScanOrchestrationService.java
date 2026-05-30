package com.security.cloudscanner.service;

import com.security.cloudscanner.audit.AuditService;
import com.security.cloudscanner.entity.ScanResult;
import com.security.cloudscanner.extraction.ContentExtractionService;
import com.security.cloudscanner.extraction.ExtractionResult;
import com.security.cloudscanner.policy.PolicyDecisionResult;
import com.security.cloudscanner.policy.PolicyEngineService;
import com.security.cloudscanner.repository.ScanResultRepository;
import com.security.cloudscanner.risk.RiskScoringService;
import com.security.cloudscanner.scanner.S3MetadataResult;
import com.security.cloudscanner.scanner.S3ResourceScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ScanOrchestrationService {

    private static final int RISK_EXPLANATION_MAX = 1800;
    private static final int MASKED_CONTENT_MAX = 3500;
    private static final int REGEX_FINDINGS_MAX = 3500;
    private static final int NLP_FINDINGS_MAX = 3500;
    private static final int AI_FINDINGS_MAX = 3500;
    private static final int ALL_FINDINGS_MAX = 3500;

    private final ScanResultRepository repository;
    private final DataClassificationService classificationService;
    private final DataMaskingService maskingService;
    private final PolicyEngineService policyEngineService;
    private final AuditService auditService;
    private final S3ResourceScanner s3ResourceScanner;
    private final RiskScoringService riskScoringService;
    private final ContentExtractionService contentExtractionService;
    private final String sensitiveBucket;

    @Autowired
    public ScanOrchestrationService(
            ScanResultRepository repository,
            DataClassificationService classificationService,
            DataMaskingService maskingService,
            PolicyEngineService policyEngineService,
            AuditService auditService,
            S3ResourceScanner s3ResourceScanner,
            RiskScoringService riskScoringService,
            ContentExtractionService contentExtractionService,
            @Value("${aws.s3.sensitive-bucket}") String sensitiveBucket
    ) {
        this.repository = repository;
        this.classificationService = classificationService;
        this.maskingService = maskingService;
        this.policyEngineService = policyEngineService;
        this.auditService = auditService;
        this.s3ResourceScanner = s3ResourceScanner;
        this.riskScoringService = riskScoringService;
        this.contentExtractionService = contentExtractionService;
        this.sensitiveBucket = sensitiveBucket;
    }

    ScanOrchestrationService(
            ScanResultRepository repository,
            DataClassificationService classificationService,
            DataMaskingService maskingService,
            PolicyEngineService policyEngineService,
            AuditService auditService,
            S3ResourceScanner s3ResourceScanner,
            RiskScoringService riskScoringService
    ) {
        this(
                repository,
                classificationService,
                maskingService,
                policyEngineService,
                auditService,
                s3ResourceScanner,
                riskScoringService,
                null,
                "cloudscanner-sensitive-bucket-harshal-001"
        );
    }

    public ScanResult scanResource(String resourceName, String resourceType) {
        S3MetadataResult metadata = s3ResourceScanner.scanMetadata(resourceName);
        ExtractionResult extractionResult = extractContent(resourceName, metadata);
        String content = extractionResult.getExtractedText();

        ClassificationResult classification = classificationService.classify(content);
        String maskedContent = maskingService.maskContent(content);

        PolicyDecisionResult decision = policyEngineService.evaluate(
                classification.getDataType(),
                classification.getSensitivity()
        );

        int riskScore = riskScoringService.calculateRiskScore(
                classification.getDataType(),
                classification.getSensitivity(),
                decision.getAction(),
                metadata,
                classification.getDetectionScore(),
                extractionResult.getExtractionStatus()
        );

        String riskLevel = riskScoringService.determineRiskLevel(riskScore);

        if (riskScore >= 70) {
            decision = new PolicyDecisionResult(
                    "BLOCK",
                    "Critical risk detected based on data exposure, sensitivity, or extraction constraints"
            );

            try {
                s3ResourceScanner.moveObjectToSecureBucket(sensitiveBucket, resourceName, maskedContent);
            } catch (Exception e) {
                System.out.println("ERROR WHILE MOVING FILE");
                e.printStackTrace();
            }
        } else if (riskScore >= 50) {
            decision = new PolicyDecisionResult(
                    "ALERT",
                    "High risk detected based on contextual analysis"
            );
        }

        auditService.log(resourceName, decision.getAction(), decision.getReason());

        String riskExplanation =
                "Data type: " + classification.getDataType()
                        + ", Sensitivity: " + classification.getSensitivity()
                        + ", File type: " + extractionResult.getFileType()
                        + ", Extraction status: " + extractionResult.getExtractionStatus()
                        + ", Regex findings: " + classification.getRegexFindings().size()
                        + ", NLP entities: " + classification.getNlpEntities().size()
                        + ", AI predictions: " + classification.getAiPredictions()
                        + ", Detection score: " + classification.getDetectionScore()
                        + ", Bucket exposure: " + (metadata.isPublicBucket() ? "PUBLIC" : "PRIVATE")
                        + ", Encryption: " + (metadata.isEncryptionEnabled() ? "ENABLED" : "DISABLED");

        ScanResult scan = new ScanResult();
        scan.setResourceName(resourceName);
        scan.setResourceType(resourceType);
        scan.setDataType(classification.getDataType());
        scan.setSensitivityLevel(classification.getSensitivity());
        scan.setExtractionStatus(extractionResult.getExtractionStatus());
        scan.setFileType(extractionResult.getFileType());
        scan.setMaskedContent(sanitizeAndTruncate(maskedContent, MASKED_CONTENT_MAX));
        scan.setPolicyAction(decision.getAction());
        scan.setPolicyReason(decision.getReason());
        scan.setPublicBucket(metadata.isPublicBucket());
        scan.setEncryptionEnabled(metadata.isEncryptionEnabled());
        scan.setVersioningEnabled(metadata.isVersioningEnabled());
        scan.setObjectSize(metadata.getObjectSize());
        scan.setLastModified(metadata.getLastModified());
        scan.setRegexFindings(sanitizeAndTruncate(String.join(", ", classification.getRegexFindings()), REGEX_FINDINGS_MAX));
        scan.setNlpFindings(sanitizeAndTruncate(String.join(", ", classification.getNlpEntities()), NLP_FINDINGS_MAX));
        scan.setAiFindings(sanitizeAndTruncate(String.join(", ", classification.getAiPredictions()), AI_FINDINGS_MAX));
        scan.setAllFindings(sanitizeAndTruncate(String.join(", ", classification.getFindings()), ALL_FINDINGS_MAX));
        scan.setRiskScore(riskScore);
        scan.setRiskLevel(riskLevel);
        scan.setCreatedAt(LocalDateTime.now());
        scan.setRiskExplanation(sanitizeAndTruncate(riskExplanation, RISK_EXPLANATION_MAX));

        return repository.save(scan);
    }

    public List<ScanResult> scanEntireBucket() {
        List<ScanResult> results = new ArrayList<>();
        for (String objectKey : s3ResourceScanner.listAllObjects()) {
            results.add(scanResource(objectKey, "S3_OBJECT"));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractObjectKeys(Map<String, Object> eventPayload) {
        List<String> objectKeys = new ArrayList<>();
        Object recordsObject = eventPayload.get("Records");

        if (!(recordsObject instanceof List<?> records)) {
            return objectKeys;
        }

        for (Object recordObject : records) {
            if (!(recordObject instanceof Map<?, ?> record)) {
                continue;
            }

            Object s3Object = record.get("s3");
            if (!(s3Object instanceof Map<?, ?> s3)) {
                continue;
            }

            Object objectObject = s3.get("object");
            if (!(objectObject instanceof Map<?, ?> objectMap)) {
                continue;
            }

            Object keyObject = objectMap.get("key");
            if (keyObject == null) {
                continue;
            }

            String decodedKey = URLDecoder.decode(String.valueOf(keyObject), StandardCharsets.UTF_8)
                    .replace("+", " ");
            objectKeys.add(decodedKey);
        }

        return objectKeys;
    }

    private ExtractionResult extractContent(String resourceName, S3MetadataResult metadata) {
        if (metadata.getObjectSize() > s3ResourceScanner.getMaxFileSize()) {
            return ExtractionResult.unsupported("size-limit-exceeded");
        }

        try {
            byte[] contentBytes = s3ResourceScanner.scanBytes(resourceName);
            if (contentExtractionService == null) {
                return ExtractionResult.scanned(new String(contentBytes, StandardCharsets.UTF_8), "text/plain");
            }
            return contentExtractionService.extract(resourceName, contentBytes);
        } catch (Exception e) {
            System.out.println("Content extraction failed for " + resourceName + ": " + e.getMessage());
            return ExtractionResult.failed("application/octet-stream");
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        String suffix = "... [truncated]";
        int safeLength = Math.max(0, maxLength - suffix.length());
        return value.substring(0, safeLength) + suffix;
    }

    private String sanitizeAndTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String sanitized = value
                .replace('\u0000', ' ')
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return truncate(sanitized, maxLength);
    }
}
