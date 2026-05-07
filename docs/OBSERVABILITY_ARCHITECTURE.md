# Syndicati Observability & AI Logging Pipeline Architecture

**Document Version:** 1.0 | **Date:** April 24, 2026 | **Status:** Architecture Plan

---

## 1. Executive Summary

Syndicati is a JavaFX desktop application with an existing database-backed event logging system (`AppEventLog`). This document outlines the migration path to a comprehensive observability and AI-powered analytics pipeline integrating OpenObserve, LogAI, and Langfuse.

The good news: **70% of the infrastructure is already in place.** The existing schema supports correlation fields, risk scoring, and anomaly detection placeholders. The task is to:
1. Standardize and enrich event logging
2. Export structured events to OpenObserve
3. Feed events to LogAI for anomaly detection
4. Prepare Langfuse hooks for future LLM features
5. Build admin analytics features on top

---

## 2. Current Logging State

### 2.1 Existing Infrastructure

| Component | Location | Status | Notes |
|-----------|----------|--------|-------|
| **Event Model** | `models/log/AppEventLog.java` | ✅ In Place | 30+ fields, includes correlation & scoring fields |
| **Repository** | `models/log/data/AppEventLogRepository.java` | ✅ In Place | Full CRUD, batch queries |
| **Logger Service** | `services/log/UserActivityLogger.java` | ✅ In Place | High-level API, event enrichment |
| **Buffer/Cache** | `services/log/LogBuffer.java` | ✅ In Place | In-memory buffer for performance |
| **Admin Service** | `services/dashboard/DashboardAdminService.java` | ✅ In Place | Query methods for dashboard viz |
| **Frontend Integration** | `views/backend/dashboard/DashboardView.java` | ✅ In Place | Displays recent logs, analytics |

### 2.2 AppEventLog Schema (Current)

**Identity & Correlation:**
- `eventId` (UUID) - unique event identifier
- `sessionId` (UUID) - runtime session correlation
- `requestId` (UUID) - per-request correlation
- `traceId` (string) - distributed trace ID
- `spanId` (string) - distributed span ID

**Event Details:**
- `eventType` (string) - USER_LOGIN, PAGE_VIEW, CRUD, etc.
- `category` (string) - USER_ACTIVITY, SECURITY, PERFORMANCE, etc.
- `action` (string) - detailed action name
- `outcome` (string) - SUCCESS, FAILURE, PARTIAL, etc.
- `message` (string) - human-readable message
- `level` (string) - DEBUG, INFO, WARN, ERROR (default: INFO)

**Source Information:**
- `user` (FK to User) - authenticated user
- `ipAddress` (string) - client IP
- `userAgent` (string) - HTTP user agent
- `serviceName` (string) - app service name
- `environment` (string) - dev, staging, prod
- `applicationVersion` (string) - semver

**Business Context:**
- `entityType` (string) - SYNDICAT, USER, FORUM, EVENT, etc.
- `entityId` (integer) - foreign key to domain object

**Performance & Scoring:**
- `durationMs` (integer) - operation latency
- `riskScore` (decimal) - risk signal (0-1)
- `anomalyScore` (decimal) - anomaly signal (0-1)

**Extensibility:**
- `metadataJson` (text) - arbitrary JSON payload
- `createdAt`, `eventTimestamp` (LocalDateTime) - timing

**Total: 30 fields, well-designed for structured logging.**

### 2.3 Current Logging Patterns

#### UserActivityLogger
The main entry point for application logging.

```java
// High-level API
log(eventType, entityType, entityId, metadata, user)
logPageView(route, screenName, metadata)
logUiClick(target, text, metadata)
logCrudAction(action, entityType, entityId, metadata)
```

**Current Usage:**
- Used in controllers and views for user actions
- Automatically enriches metadata with default values
- Stores correlation IDs (generates if missing)
- Buffers logs in memory for performance

#### Gaps Identified
- **No centralized export mechanism** – Logs stay in DB only
- **No event validation/redaction** – Sensitive data not automatically scrubbed
- **No active tracing** – traceId/spanId generated but not propagated between service calls
- **Risk/anomaly scores manually populated** – No pipeline writes them
- **No OpenObserve/Langfuse integration** – No external telemetry
- **Limited admin analytics** – Dashboard queries are read-only, no anomaly API
- **No LogAI feedback loop** – No worker or batch job for scoring

---

## 3. Target Architecture

### 3.1 High-Level System Design

```
┌─────────────────────────────────────────────────────────────────┐
│ Syndicati Desktop App (JavaFX)                                  │
│  - Controllers/Views log events via UserActivityLogger          │
│  - Events stored in local MySQL (app_event_log table)           │
│  - DashboardAdminService queries for visualization              │
└────────────────────┬────────────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
   ┌─────────────────┐  ┌──────────────────┐
   │ DB (MySQL)      │  │ Event Exporter   │
   │ app_event_log   │  │ (OpenObserve)    │
   └────────┬────────┘  └────────┬─────────┘
            │                    │
            │         ┌──────────┴────────────┐
            │         ▼                       ▼
            │   ┌──────────────┐      ┌────────────────┐
            │   │ OpenObserve  │      │ LogAI Worker   │
            │   │ - Search     │      │ - Anomaly      │
            │   │ - Dashboards │      │ - Clustering   │
            │   │ - Alerts     │      │ - Scoring      │
            │   └──────────────┘      └────────┬───────┘
            │                                  │
            │    ┌────────────────────────────┘
            │    │ Anomaly Results (async)
            │    ▼
            └──► AnomalyScoreService
                 - Write scores back to DB
                 - Risk signals for admins

         ┌──────────────────────────────────────┐
         │ Langfuse (prepared for LLM tracking)  │
         │ - Future AI feature instrumentation   │
         └──────────────────────────────────────┘
```

### 3.2 Data Flow

1. **Event Capture** → UserActivityLogger.log() captures user action
2. **DB Persistence** → AppEventLogRepository.create() writes to app_event_log
3. **Structured Export** → OpenObserveExporter reads from DB, transforms to JSON, posts to OpenObserve
4. **Analysis** → LogAI Worker consumes exports, computes anomalies
5. **Scoring Feedback** → Anomaly scores written back to app_event_log.anomaly_score
6. **Admin Queries** → SuspiciousActivityService, RiskScoringService expose insights
7. **Langfuse Hooks** → Future LLM calls instrumented if AI features added

### 3.3 Integration Points

| Integration | Purpose | Transport | Async | Enabled By |
|-------------|---------|-----------|-------|-----------|
| **OpenObserve** | Centralized log ingestion, search, dashboards | OTLP/HTTP | Yes | Config flag |
| **LogAI** | Anomaly detection, clustering | REST/batch | Yes | Scheduled job |
| **Langfuse** | LLM observability | OTEL/HTTP | Yes | Config flag |

---

## 4. Implementation Phases

### Phase 1: Audit ✅ COMPLETED
**Deliverable:** This document. Current logging state understood, gaps identified, architecture validated.

### Phase 2: Event Standardization
- Refactor UserActivityLogger to enforce schema validation
- Add event enums (EventType, EventLevel, EventOutcome, etc.)
- Implement automatic redaction of sensitive fields
- Add MDC (Mapped Diagnostic Context) for correlation propagation
- Ensure all log calls include proper metadata

**Outcome:** Logs are standardized, consistent, and safe for export.

### Phase 3: OpenObserve Integration
- Add Maven dependencies (OpenTelemetry SDK)
- Create `OpenObserveExporter` service
- Add config properties (application.local.properties)
- Implement HTTP exporter with retry logic
- Create sample OpenObserve queries/dashboards document
- Test local export to OpenObserve Docker instance

**Outcome:** Logs flow from MySQL → OpenObserve in real-time or batch.

### Phase 4: Langfuse Integration
- Add Langfuse Maven dependencies
- Create `LangfuseTracer` service with OTEL support
- Add config properties for Langfuse connection
- Document extension points for future AI features
- Create Langfuse setup guide

**Outcome:** Langfuse endpoints ready; hooks in place for LLM instrumentation.

### Phase 5: LogAI Anomaly Detection
- Create `/workers/logai-anomaly-detection/` Python folder
- Set up LogAI batch job workflow
- Define anomaly scoring service (Java)
- Implement scheduled task to fetch anomaly scores, update DB
- Create LogAI setup guide with examples

**Outcome:** Anomaly scores automatically populated in app_event_log.anomaly_score.

### Phase 6: Advanced Admin Functionality
- Create `SuspiciousActivityService` – detect repeated failures, abnormal frequency
- Create `RiskScoringService` – aggregate rule signals + anomaly scores
- Add admin API endpoints:
  - GET /api/admin/anomalies/recent
  - GET /api/admin/suspicious-users
  - GET /api/admin/user/{id}/timeline
  - GET /api/admin/analytics/feature-usage
- Create alert hooks for high-risk events
- Add explainability fields

**Outcome:** Admin dashboard can visualize security signals and risk scores.

### Phase 7: Code Quality & Documentation
- Write comprehensive README
- Add unit tests for event mapping, redaction, export retry logic
- Document troubleshooting steps
- Add feature flags for safe rollout
- Polish code, remove TODOs

**Outcome:** Production-ready, well-documented, testable system.

---

## 5. Key Decisions & Rationale

| Decision | Rationale |
|----------|-----------|
| **No Spring Boot** | App uses JavaFX desktop model; lightweight custom JDBC works fine |
| **Async Export** | Keep event capture fast; defer slow network operations to background |
| **LogAI as sidecar** | Don't block UI with ML; batch scoring in separate Python worker |
| **Langfuse via OTEL** | Standard protocol, future-proof, avoids proprietary locks |
| **Config externalization** | All secrets and URLs in application.local.properties; never in code |
| **Backward compat** | Keep AppEventLog schema intact; add new features via new services |
| **Feature flags** | Can toggle integrations off without code changes |

---

## 6. Success Criteria

By end of Phase 7, you will have:

- ✅ Standardized, validated event logging across app
- ✅ Real-time structured log export to OpenObserve
- ✅ Automated anomaly detection via LogAI
- ✅ Admin API endpoints for suspicious activity, risk scoring, timelines
- ✅ Langfuse instrumentation hooks for future LLM features
- ✅ Comprehensive documentation and tests
- ✅ Feature flags to safely enable/disable integrations

---

## 7. Estimated Timeline

| Phase | Effort | Timeline |
|-------|--------|----------|
| Phase 1 | 1 hr | ✅ Complete |
| Phase 2 | 3-4 hrs | Today |
| Phase 3 | 4-5 hrs | Today/Tomorrow |
| Phase 4 | 2-3 hrs | Tomorrow |
| Phase 5 | 3-4 hrs | Tomorrow |
| Phase 6 | 4-5 hrs | Day 3 |
| Phase 7 | 2-3 hrs | Day 3 |
| **Total** | **~20-24 hrs** | **~3 days** |

---

## 8. Appendix: File Structure After Implementation

```
src/main/java/com/syndicati/
├── models/
│   └── log/
│       ├── AppEventLog.java (existing, no changes)
│       ├── enums/
│       │   ├── EventType.java (NEW)
│       │   ├── EventLevel.java (NEW)
│       │   ├── EventOutcome.java (NEW)
│       │   └── EventCategory.java (NEW)
│       └── data/
│           └── AppEventLogRepository.java (existing)
│
├── services/
│   ├── log/
│   │   ├── UserActivityLogger.java (ENHANCED)
│   │   ├── LogBuffer.java (existing)
│   │   ├── EventValidator.java (NEW)
│   │   ├── DataRedactor.java (NEW)
│   │   └── CorrelationContext.java (NEW)
│   │
│   ├── observability/
│   │   ├── OpenObserveExporter.java (NEW)
│   │   ├── OpenObserveConfig.java (NEW)
│   │   ├── LangfuseTracer.java (NEW)
│   │   └── LangfuseConfig.java (NEW)
│   │
│   └── analytics/
│       ├── SuspiciousActivityService.java (NEW)
│       ├── RiskScoringService.java (NEW)
│       ├── AnomalyScoreService.java (NEW)
│       └── models/
│           ├── SuspiciousActivity.java (NEW)
│           ├── RiskScore.java (NEW)
│           └── AnomalyResult.java (NEW)
│
├── controllers/
│   └── admin/
│       └── AnalyticsController.java (NEW)
│
└── utils/
    └── logging/
        ├── MdcContext.java (NEW)
        └── TraceHelper.java (NEW)

config/
├── application.local.properties (ENHANCED)
└── observability.properties (NEW)

workers/
└── logai-anomaly-detection/ (NEW)
    ├── requirements.txt
    ├── setup.py
    ├── README.md
    └── anomaly_service.py

docs/
├── OBSERVABILITY_ARCHITECTURE.md (this file)
├── OBSERVABILITY_README.md (NEW - comprehensive guide)
├── OPENOBSERVE_SETUP.md (NEW - local dev setup)
├── LANGFUSE_SETUP.md (NEW - integration guide)
└── LOGAI_SETUP.md (NEW - worker setup)

tests/
└── java/com/syndicati/services/
    ├── observability/
    │   ├── OpenObserveExporterTest.java (NEW)
    │   └── LangfuseTracerTest.java (NEW)
    ├── analytics/
    │   ├── SuspiciousActivityServiceTest.java (NEW)
    │   └── RiskScoringServiceTest.java (NEW)
    └── log/
        ├── EventValidatorTest.java (NEW)
        └── DataRedactorTest.java (NEW)
```

---

## 9. Next Steps

1. **Proceed to Phase 2**: Event Standardization
   - Create EventType, EventLevel enums
   - Add validators and redactors
   - Enhance UserActivityLogger

2. **Parallel**: Prepare Phase 3 dependencies
   - Add OpenTelemetry to pom.xml
   - Research OpenObserve Docker setup for local testing

3. **Prepare Phase 5**: Python environment
   - Set up workers/logai-anomaly-detection with venv
   - Install LogAI, required deps

---

**Document Prepared By:** Senior Staff Engineer  
**For:** Syndicati Project Team  
**Version Control:** Commit to repo after review

