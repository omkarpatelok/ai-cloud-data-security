package com.security.cloudscanner.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataClassificationService {

    private final RegexDetectionService regexService;
    private final NlpDetectionService nlpService;
    private final AiDetectionService aiService;

    public DataClassificationService(
            RegexDetectionService regexService,
            NlpDetectionService nlpService,
            AiDetectionService aiService
    ) {
        this.regexService = regexService;
        this.nlpService = nlpService;
        this.aiService = aiService;
    }

    public ClassificationResult classify(String content) {

        List<String> findings = new ArrayList<>();
        List<String> regexFindings = regexService.detect(content);
        List<String> nlpEntities = nlpService.detect(content);
        List<String> aiPredictions = aiService.classify(content);

        findings.addAll(regexFindings);
        findings.addAll(nlpEntities);
        findings.addAll(aiPredictions);

        int detectionScore = 0;
        if (!regexFindings.isEmpty()) {
            detectionScore += 40;
        }
        if (!nlpEntities.isEmpty()) {
            detectionScore += 20;
        }
        if (aiService.hasSensitiveSignal(aiPredictions)) {
            detectionScore += 20;
        }

        String dataType = detectionScore > 0 ? "PII" : "PUBLIC";
        String sensitivity = detectionScore >= 60
                ? "HIGH"
                : detectionScore > 0 ? "MEDIUM" : "LOW";

        return new ClassificationResult(
                dataType,
                sensitivity,
                findings,
                regexFindings,
                nlpEntities,
                aiPredictions,
                detectionScore
        );
    }
}
