# Architecture Guide

> A comprehensive reference for understanding the AI Cloud Data Security Scanner's architecture, data flow, and implementation details.

---

## 1. System Overview

The AI Cloud Data Security Scanner is a cloud-native security platform with two main components:

### `cloudscanner/` — Spring Boot Backend
- Scans S3 objects for sensitive data
- Classifies content using regex, NLP, and AI
- Calculates composite risk scores
- Stores scan results in H2 (in-memory)
- Applies policy enforcement and audit logging
- Auto-quarantines high-risk files

### `ai-services/` — Python AI Microservices
- **NLP Service** (`nlp_service.py`) — SpaCy NER for entity detection
- **BERT Classifier** (`bert_classifier.py`) — HuggingFace zero-shot classification

### `frontend/` — SPA Dashboard
- Vanilla HTML/CSS/JS single-page application
- Glassmorphism dark theme with responsive design
- Real-time dashboard with KPIs and risk charts

---

## 2. Core Data Flow

```
Client/API Request
 → ScanController
   → ScanOrchestrationService
     → S3ResourceScanner (reads S3 object content + metadata)
       → ContentExtractionService (text/PDF/Office/OCR/binary)
         → DataClassificationService
           → RegexDetectionService (local pattern matching)
           → NlpDetectionService (HTTP → localhost:9000/detect)
           → AiDetectionService (HTTP → localhost:9100/classify)
         → PolicyEngineService (REVIEW for PII, else ALLOW)
         → RiskScoringService (composite scoring, capped at 100)
         → Remediation (risk ≥ 70 → move to secure bucket)
         → AuditService (log action + reason)
         → ScanResultRepository (persist ScanResult)
```

### Dashboard Flow
```
ScanResultRepository → SecurityDashboardController → /api/security/summary, /api/security/high-risk
```

### Admin Flow
```
AuditLogRepository → AuditController → /api/audit-logs
S3MetadataRepository → MetadataController → /api/metadata
```

---

## 3. End-to-End Scan Workflow

The core scan path is implemented in `ScanOrchestrationService.java`:

1. **Request Entry** — Scan request arrives at one of: `POST /api/scans`, `POST /api/scans/s3-event`, or `POST /api/scans/batch`

2. **S3 Object Retrieval** — `S3ResourceScanner.scan(resourceName)` reads the object from S3 with a size guard before deep scanning

3. **Content Extraction** — `ContentExtractionService` extracts text from the raw bytes based on file type (plain text, PDF, Office, image OCR, binary)

4. **Multi-Layer Classification** — `DataClassificationService.classify(content)` runs 3 detection stages:
   - **Regex** — Pattern matching for Aadhaar, PAN, SSN, credit cards, API keys, etc.
   - **NLP** — HTTP call to `localhost:9000/detect` for named entities (PERSON, ORG, LOCATION, EMAIL)
   - **AI** — HTTP call to `localhost:9100/classify` for zero-shot classification (CONFIDENTIAL, PAYMENT, INTERNAL, PUBLIC)

5. **Classification Output** — Combined findings, data type (`PII`/`PUBLIC`), sensitivity level (`HIGH`/`MEDIUM`/`LOW`), and detection score

6. **Metadata Scan** — `S3ResourceScanner.scanMetadata(resourceName)` reads encryption status, public access, versioning, size, last modified

7. **Policy Evaluation** — `PolicyEngineService.evaluate()` → `REVIEW` for high-sensitivity PII, otherwise `ALLOW`

8. **Risk Scoring** — `RiskScoringService.calculateRiskScore()` computes composite score (0–100) based on detection, policy, exposure, and extraction factors

9. **Risk Level Derivation** — CRITICAL (≥70), HIGH (≥50), MEDIUM (≥30), LOW (<30)

10. **Remediation** — Risk ≥ 70 triggers `BLOCK` → object moved to `sensitive-bucket`. Risk ≥ 50 triggers `ALERT`.

11. **Audit Logging** — `AuditService.log()` records the action and reason

12. **Persistence** — `ScanResult` entity saved via `ScanResultRepository`

---

## 4. Security Model

Defined in `SecurityConfig.java`:

### Authentication
- HTTP Basic Authentication via Spring Security
- In-memory user store (development configuration)

### Default Users
| Username | Password | Role |
|----------|----------|------|
| `user` | `user123` | USER |
| `admin` | `admin123` | ADMIN |

### Authorization Rules (non-dev profile)
| Path | Required Role |
|------|--------------|
| `/h2-console/**` | Public |
| `/api/scans/**` | USER or ADMIN |
| `/api/security/**` | USER or ADMIN |
| `/api/upload-url` | USER or ADMIN |
| `/api/metadata/**` | ADMIN only |
| `/api/audit-logs/**` | ADMIN only |

> In `dev` profile, all requests are permitted for development convenience.

---

## 5. AI/NLP Integration

### NLP Service (Port 9000)
- **File:** `ai-services/nlp_service.py`
- **Framework:** FastAPI
- **Endpoint:** `POST /detect`
- **Model:** SpaCy `en_core_web_sm`
- **Output:** Named entities (PERSON, ORG, LOCATION, EMAIL)

### BERT Classifier (Port 9100)
- **File:** `ai-services/bert_classifier.py`
- **Framework:** FastAPI
- **Endpoint:** `POST /classify`
- **Model:** HuggingFace `facebook/bart-large-mnli`
- **Labels:** CONFIDENTIAL_DOCUMENT, PAYMENT_INFORMATION, INTERNAL_DATA, PUBLIC_CONTENT

### Resilience
- `AiServiceResilienceSupport.java` wraps AI HTTP calls with:
  - Configurable retry count (`ai.resilience.max-retries`)
  - Circuit-breaker style cooldown (`ai.resilience.cooldown-seconds`)
  - Failure threshold tracking (`ai.resilience.failure-threshold`)
  - Graceful degradation — scan continues even if AI services are unavailable

---

## 6. Content Extraction Pipeline

### Extractors

| Extractor | File Types | Library |
|-----------|-----------|---------|
| `PlainTextExtractor` | .txt, .csv, .json, .xml, .yml, .java, .py, .js, .sql, .log, .md, .html | — |
| `PdfExtractor` | .pdf | Apache PDFBox |
| `OfficeExtractor` | .docx, .xlsx, .pptx | Apache POI |
| `ImageOcrExtractor` | .png, .jpg, .jpeg, .tiff, .bmp | Tess4J (Tesseract) |
| `BinaryExtractor` | Binary files | String extraction |
| `EncryptedFileHandler` | Encrypted files | Detection only |

### Extraction Statuses
| Status | Meaning |
|--------|---------|
| `SCANNED` | Content successfully extracted |
| `OCR_REQUIRED` | Image file but OCR unavailable/failed |
| `ENCRYPTED` | File is encrypted, no decryption attempted |
| `UNSUPPORTED` | File format not supported |
| `EXTRACTION_FAILED` | Extraction error occurred |

---

## 7. Data Stored Per Scan

`ScanResult` entity fields:
- Resource name and resource type
- Data type (`PII`/`PUBLIC`) and sensitivity level
- Masked content (sanitized version of detected PII)
- Regex findings, NLP findings, AI findings, combined findings
- Policy action (`ALLOW`/`REVIEW`/`ALERT`/`BLOCK`) and reason
- S3 exposure metadata (encryption, public access)
- Risk score (0–100) and risk level (CRITICAL/HIGH/MEDIUM/LOW)
- Human-readable risk explanation
- Extraction status and file type
- Creation timestamp

---

## 8. API Endpoints

### Scanning
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/scans` | USER/ADMIN | Scan one resource |
| `POST` | `/api/scans/batch` | USER/ADMIN | Scan all objects in bucket |
| `POST` | `/api/scans/s3-event` | Token | S3 event-driven scan |
| `GET` | `/api/scans` | USER/ADMIN | Get all scan results |

### Dashboard
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/security/summary` | USER/ADMIN | Dashboard KPIs |
| `GET` | `/api/security/high-risk` | USER/ADMIN | High-risk results only |

### Upload
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/upload-url` | USER/ADMIN | Pre-signed S3 upload URL |
| `PUT` | `/api/local-upload` | USER/ADMIN | Local upload fallback |

### Admin
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/metadata` | ADMIN | S3 metadata |
| `GET` | `/api/audit-logs` | ADMIN | Audit trail |

---

## 9. Configuration Reference

### `application.yml`
```yaml
server.port: 8081
aws.region: ap-south-1
aws.s3.bucket: <your-bucket-name>
aws.s3.sensitive-bucket: <your-secure-bucket>
aws.s3.max-file-size: 10485760  # 10 MB
ai.nlp.url: http://localhost:9000/detect
ai.classifier.url: http://localhost:9100/classify
ai.resilience.max-retries: 2
ai.resilience.failure-threshold: 3
ai.resilience.cooldown-seconds: 30
```

### Environment Variables (`.env`)
```bash
AWS_ACCESS_KEY_ID=<your-key>
AWS_SECRET_ACCESS_KEY=<your-secret>
AWS_DEFAULT_REGION=ap-south-1
```

> **Note:** Configuration is split between `application.yml` (primary) and `application.properties` (supplementary). For S3 bucket wiring, `application.yml` is the canonical source.

---

## 10. Suggested Reading Order

For the fastest understanding of the codebase, read in this order:

1. `ScanController.java` — Entry point for all scan requests
2. `ScanOrchestrationService.java` — Core pipeline orchestrator
3. `DataClassificationService.java` — Multi-layer detection coordinator
4. `RegexDetectionService.java` — Regex pattern matching
5. `NlpDetectionService.java` — SpaCy NER integration
6. `AiDetectionService.java` — BART classifier integration
7. `RiskScoringService.java` — Composite risk scoring
8. `SecurityConfig.java` — Authentication and authorization
9. `nlp_service.py` — Python NLP microservice
10. `bert_classifier.py` — Python classifier microservice
