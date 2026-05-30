package com.security.cloudscanner.extraction;

public class ExtractionResult {

    public static final String SCANNED = "SCANNED";
    public static final String OCR_REQUIRED = "OCR_REQUIRED";
    public static final String ENCRYPTED = "ENCRYPTED";
    public static final String UNSUPPORTED = "UNSUPPORTED";
    public static final String EXTRACTION_FAILED = "EXTRACTION_FAILED";

    private final String extractedText;
    private final String extractionStatus;
    private final String fileType;
    private final boolean success;

    public ExtractionResult(String extractedText, String extractionStatus, String fileType, boolean success) {
        this.extractedText = extractedText == null ? "" : extractedText;
        this.extractionStatus = extractionStatus;
        this.fileType = fileType;
        this.success = success;
    }

    public static ExtractionResult scanned(String text, String fileType) {
        return new ExtractionResult(text, SCANNED, fileType, true);
    }

    public static ExtractionResult ocrRequired(String fileType) {
        return new ExtractionResult("", OCR_REQUIRED, fileType, false);
    }

    public static ExtractionResult encrypted(String fileType) {
        return new ExtractionResult("", ENCRYPTED, fileType, false);
    }

    public static ExtractionResult unsupported(String fileType) {
        return new ExtractionResult("", UNSUPPORTED, fileType, false);
    }

    public static ExtractionResult failed(String fileType) {
        return new ExtractionResult("", EXTRACTION_FAILED, fileType, false);
    }

    public String getExtractedText() {
        return extractedText;
    }

    public String getExtractionStatus() {
        return extractionStatus;
    }

    public String getFileType() {
        return fileType;
    }

    public boolean isSuccess() {
        return success;
    }
}
