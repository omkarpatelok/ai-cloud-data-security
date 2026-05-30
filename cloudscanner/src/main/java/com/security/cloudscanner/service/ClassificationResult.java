package com.security.cloudscanner.service;

import java.util.List;

public class ClassificationResult {

    private final String dataType;
    private final String sensitivity;
    private final List<String> findings;
    private final List<String> regexFindings;
    private final List<String> nlpEntities;
    private final List<String> aiPredictions;
    private final int detectionScore;

    public ClassificationResult(
            String dataType,
            String sensitivity,
            List<String> findings,
            List<String> regexFindings,
            List<String> nlpEntities,
            List<String> aiPredictions,
            int detectionScore
    ) {
        this.dataType = dataType;
        this.sensitivity = sensitivity;
        this.findings = findings;
        this.regexFindings = regexFindings;
        this.nlpEntities = nlpEntities;
        this.aiPredictions = aiPredictions;
        this.detectionScore = detectionScore;
    }

    public String getDataType() {
        return dataType;
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public List<String> getFindings() {
        return findings;
    }

    public List<String> getRegexFindings() {
        return regexFindings;
    }

    public List<String> getNlpEntities() {
        return nlpEntities;
    }

    public List<String> getAiPredictions() {
        return aiPredictions;
    }

    public int getDetectionScore() {
        return detectionScore;
    }
}
