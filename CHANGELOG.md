# Changelog

All notable changes to the AI Cloud Data Security Scanner are documented in this file.

## [Unreleased]

### Added — Content Extraction Layer
- New `extraction` package with `ContentExtractionService`, `FileTypeDetector`, and `ExtractionResult`
- Six specialized extractors:
  - `PlainTextExtractor` — text, JSON, XML, YAML, properties, JavaScript, SQL
  - `PdfExtractor` — PDF parsing via Apache PDFBox (encrypted PDFs flagged as `ENCRYPTED`)
  - `OfficeExtractor` — Word (.docx), Excel (.xlsx), PowerPoint (.pptx) via Apache POI
  - `ImageOcrExtractor` — OCR via Tess4J (optional; falls back to `OCR_REQUIRED` if Tesseract is unavailable)
  - `BinaryExtractor` — Printable string extraction from binary files
  - `EncryptedFileHandler` — Detection and flagging of encrypted files
- Extraction status tracking: `SCANNED`, `OCR_REQUIRED`, `ENCRYPTED`, `UNSUPPORTED`, `EXTRACTION_FAILED`
- `ScanResult` entity extended with `extractionStatus` and `fileType` fields
- Extraction-aware risk scoring adjustments:
  - `ENCRYPTED` → +60 points
  - `EXTRACTION_FAILED` → +50 points
  - `UNSUPPORTED` → +35 points
  - `OCR_REQUIRED` → +30 points
- Performance safety: large files checked against `aws.s3.max-file-size`; OCR limited to files ≤ 5 MB

### Added — AI/NLP Integration
- `ai-services/` directory with two Python FastAPI microservices
- `nlp_service.py` — SpaCy NER service (PERSON, ORG, LOCATION, EMAIL detection) on port 9000
- `bert_classifier.py` — HuggingFace BART zero-shot classifier (CONFIDENTIAL, PAYMENT, INTERNAL, PUBLIC) on port 9100
- `AiDetectionService.java` — Backend integration for BERT classifier
- `NlpDetectionService.java` — Backend integration for SpaCy NLP
- `AiServiceResilienceSupport.java` — Retry + circuit-breaker resilience wrapper
- `HttpClientConfig.java` — RestTemplate configuration for AI service HTTP calls
- AI service configuration in `application.yml` (URLs, retry counts, cooldown)

### Changed — Classification Pipeline
- `DataClassificationService` now combines regex + NLP + AI results
- `ClassificationResult` carries richer findings and detection score
- `ScanOrchestrationService` uses combined findings in risk calculation and persistence
- `RiskScoringService` supports detection-score-based risk calculation
- `SecurityConfig` includes role-based API protection and dev profile relaxation

### Removed — Backend Cleanup
- SNS-specific controller logic (subscription confirmation, notification parsing)
- Lambda/ngrok/dead infrastructure artifacts
- Unused ML heuristic service
- Dead/demo-only scanner code
- Duplicate configuration entries from `application.properties`
- Old `aws.s3.source-bucket` usage (normalized to `aws.s3.bucket` only)

### Preserved
- Core API surface: `POST /api/scans`, `POST /api/scans/batch`, `GET /api/scans`, `POST /api/scans/s3-event`
- Complete orchestration flow from scanning through remediation
- AI integration with resilience fallback behavior
- Direct S3 event ingestion via `/api/scans/s3-event`

### Architecture (Post-Cleanup)
```
Controller → ScanOrchestrationService → S3ResourceScanner
  → ContentExtractionService → DataClassificationService
    → RegexDetectionService → NlpDetectionService → AiDetectionService
  → PolicyEngineService → RiskScoringService
  → Remediation (to sensitive bucket) → AuditService → ScanResultRepository
```
