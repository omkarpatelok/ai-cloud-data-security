package com.security.cloudscanner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "scan_results")
public class ScanResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String resourceName;
    private String resourceType;
    private String dataType;
    private String sensitivityLevel;
    private String extractionStatus;
    private String fileType;

    @Column(length = 2000)
    private String riskExplanation;

    @Column(length = 8000)
    private String maskedContent;

    @Column(length = 8000)
    private String regexFindings;

    @Column(length = 12000)
    private String nlpFindings;

    @Column(length = 12000)
    private String aiFindings;

    @Column(length = 20000)
    private String allFindings;

    private String policyAction;
    private String policyReason;
    private boolean publicBucket;
    private boolean encryptionEnabled;
    private boolean versioningEnabled;
    private long objectSize;
    private String lastModified;
    private int riskScore;
    private String riskLevel;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public String getExtractionStatus() {
        return extractionStatus;
    }

    public void setExtractionStatus(String extractionStatus) {
        this.extractionStatus = extractionStatus;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getRiskExplanation() {
        return riskExplanation;
    }

    public void setRiskExplanation(String riskExplanation) {
        this.riskExplanation = riskExplanation;
    }

    public String getMaskedContent() {
        return maskedContent;
    }

    public void setMaskedContent(String maskedContent) {
        this.maskedContent = maskedContent;
    }

    public String getRegexFindings() {
        return regexFindings;
    }

    public void setRegexFindings(String regexFindings) {
        this.regexFindings = regexFindings;
    }

    public String getNlpFindings() {
        return nlpFindings;
    }

    public void setNlpFindings(String nlpFindings) {
        this.nlpFindings = nlpFindings;
    }

    public String getAiFindings() {
        return aiFindings;
    }

    public void setAiFindings(String aiFindings) {
        this.aiFindings = aiFindings;
    }

    public String getAllFindings() {
        return allFindings;
    }

    public void setAllFindings(String allFindings) {
        this.allFindings = allFindings;
    }

    public String getPolicyAction() {
        return policyAction;
    }

    public void setPolicyAction(String policyAction) {
        this.policyAction = policyAction;
    }

    public String getPolicyReason() {
        return policyReason;
    }

    public void setPolicyReason(String policyReason) {
        this.policyReason = policyReason;
    }

    public boolean isPublicBucket() {
        return publicBucket;
    }

    public void setPublicBucket(boolean publicBucket) {
        this.publicBucket = publicBucket;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public boolean isVersioningEnabled() {
        return versioningEnabled;
    }

    public void setVersioningEnabled(boolean versioningEnabled) {
        this.versioningEnabled = versioningEnabled;
    }

    public long getObjectSize() {
        return objectSize;
    }

    public void setObjectSize(long objectSize) {
        this.objectSize = objectSize;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
