# Technical Report: AI Cloud Data Security Scanner

## 1. Project Overview

| Field | Value |
|-------|-------|
| **Project Name** | AI Cloud Data Security Scanner |
| **Domain** | Cloud Security & Data Loss Prevention |
| **Architecture** | Service-Oriented Architecture (SOA) |

**Problem Statement:** Organizations storing data in cloud object stores (AWS S3) face the risk of unintentional PII and secrets exposure. Manual scanning is error-prone, slow, and doesn't scale.

**Solution:** An automated multi-layered scanning system that discovers, classifies, and mitigates sensitive data exposures using regex pattern matching, NLP entity recognition, and zero-shot AI classification.

---

## 2. Tech Stack

| Layer | Technology | Role |
|-------|-----------|------|
| Backend | Java 21, Spring Boot 3.4.2 | REST API, Orchestration |
| Persistence | H2 (In-Memory), Spring Data JPA | Scan result storage |
| Security | Spring Security (HTTP Basic Auth) | Role-based access control |
| Cloud | AWS SDK v2 (S3, STS) | Object storage interaction |
| Extraction | Apache Tika, PDFBox, POI, Tess4J | Multi-format content extraction |
| AI Services | Python 3, FastAPI | NLP & Classification microservices |
| NLP | SpaCy (`en_core_web_sm`) | Named Entity Recognition |
| Classification | HuggingFace (`facebook/bart-large-mnli`) | Zero-shot text classification |
| Frontend | HTML5, CSS3, Vanilla JavaScript | SPA Dashboard |

---

## 3. System Architecture

```
Client/API Request
 → ScanController
   → ScanOrchestrationService
     → S3ResourceScanner (read object + metadata)
       → ContentExtractionService (text/PDF/Office/OCR/binary)
         → DataClassificationService
           → RegexDetectionService (pattern matching)
           → NlpDetectionService (SpaCy NER via HTTP)
           → AiDetectionService (BART zero-shot via HTTP)
         → PolicyEngineService (REVIEW/ALLOW decisions)
         → RiskScoringService (composite risk scoring)
         → Remediation (auto-quarantine if risk ≥ 70)
         → AuditService (action logging)
         → ScanResultRepository (persistence)
```

---

## 4. Database Design

**Engine:** In-memory H2 Database with Hibernate JPA auto-DDL.

### Tables

#### `scan_results` (from `ScanResult`)
Primary logging table for parsed files. Stores:
- Resource name, resource type
- Data type (`PII` / `PUBLIC`), sensitivity level
- Masked content (sanitized version)
- Regex findings, NLP findings, AI findings, combined findings
- Policy action and reason
- S3 exposure metadata (encryption, public access)
- Risk score (0–100) and risk level
- Extraction status, file type
- Creation timestamp

#### `s3_metadata` (from `S3MetadataEntity`)
Bucket-level metadata:
- Bucket name, encryption enabled flag
- Public access flag, timestamps, size limits

#### `audit_logs` (from `AuditLog`)
Action timeline for remediation events:
- Action type, reason, timestamp
- Associated resource name

> **Note:** Tables use loose correlation via string keys (`resourceName`) rather than rigid `@ManyToOne` foreign keys, enabling processing modularity.

---

## 5. Risk Scoring Algorithm

Risk scoring is performed by `RiskScoringService` using a composite formula:

| Factor | Points |
|--------|--------|
| PII data detected | +30 |
| HIGH sensitivity | +30 |
| MEDIUM sensitivity | +20 |
| Public bucket | +30 |
| Encryption disabled | +20 |
| BLOCK policy action | +10 |
| ENCRYPTED extraction | +60 |
| EXTRACTION_FAILED | +50 |
| UNSUPPORTED format | +35 |
| OCR_REQUIRED | +30 |

**Score is capped at 100.**

### Severity Levels

| Score Range | Level | Action |
|-------------|-------|--------|
| ≥ 70 | `CRITICAL` | Auto-quarantine (BLOCK) |
| ≥ 50 | `HIGH` | Alert (ALERT) |
| ≥ 30 | `MEDIUM` | Review (REVIEW) |
| < 30 | `LOW` | Allow (ALLOW) |

---

## 6. Data Protection & Masking

`DataMaskingService` performs regex-based substitution to sanitize detected patterns before persistence:

| Pattern | Example | Masked Output |
|---------|---------|---------------|
| Aadhaar | 1234 5678 9012 | `XXXX XXXX XXXX` |
| PAN | ABCDE1234F | `XXXXX****X` |
| SSN | 123-45-6789 | `***-**-****` |
| Credit Card | 4111111111111111 | `****-****-****-1111` |
| API Keys | `AKIA...` | `[MASKED_KEY]` |
| JWT Secrets | `eyJ...` | `[MASKED_TOKEN]` |

---

## 7. Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| S3 network dropouts blocking unauthenticated execution | `LocalMockStorage` implementation for local dev fallback |
| ML model timeouts causing pipeline hangs | `AiServiceResilienceSupport` with threshold retries and cooldown mechanisms |
| Large S3 objects overwhelming memory | Maximum file size barrier (`app.upload.max-file-size` = 10 MB) |
| OCR dependencies not always available | Graceful fallback to `OCR_REQUIRED` status without crashing the pipeline |
| Encrypted files blocking content extraction | Detection and flagging as `ENCRYPTED` with elevated risk score |

---

## 8. Future Improvements

### Scalability
- Decouple scanning from synchronous HTTP flows using async task queues (Apache Kafka, RabbitMQ)
- Migrate from H2 to PostgreSQL for production persistence
- Containerize with Docker Compose for reproducible deployments

### Features
- Modernize frontend to React/Vite SPA
- Integrate cloud-based OCR for enhanced image/scanned PDF extraction
- Upgrade authentication from HTTP Basic to OAuth2/OIDC
- Add real-time WebSocket notifications for scan completion
- Expand unit and integration test coverage

### Security
- Externalize credentials via secrets management (AWS Secrets Manager, HashiCorp Vault)
- Add rate limiting and API throttling
- Implement comprehensive input validation and OWASP protections

---

## 9. Conclusion

The AI Cloud Data Security Scanner provides a comprehensive, automated solution for cloud data leak prevention. By combining Java-based orchestration with Python AI inference services, the platform enables organizations to trace data leakage vectors and enforce automatic quarantine boundaries autonomously and accurately.
