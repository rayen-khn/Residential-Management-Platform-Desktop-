# Syndicati Observability & AI Logging Pipeline - Complete Guide

**Version:** 1.0 | **Last Updated:** April 24, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [Phase-by-Phase Setup](#phase-by-phase-setup)
5. [Configuration](#configuration)
6. [Usage Examples](#usage-examples)
7. [Troubleshooting](#troubleshooting)
8. [Production Hardening](#production-hardening)
9. [Future Roadmap](#future-roadmap)

---

## Overview

Syndicati now includes an **enterprise-grade observability pipeline** that takes application logs and user activity to the next level:

- **Centralized Log Storage** via OpenObserve (search, dashboards, alerts)
- **Anomaly Detection** via LogAI (automatic intelligence on log streams)
- **LLM Observability** via Langfuse (track AI model calls)
- **Advanced Analytics** for security, risk scoring, and suspicious activity detection
- **Privacy-First Design** (automatic redaction, IP anonymization, no secrets logged)
- **Production Ready** (retry logic, circuit breakers, graceful degradation)

### Key Features

| Feature | Status | Details |
|---------|--------|---------|
| **Event Standardization** | ✅ Complete | Type-safe enums, validation, redaction |
| **Correlation Tracking** | ✅ Complete | Thread-local trace/span/session IDs |
| **OpenObserve Export** | ✅ Complete | Batch async HTTP ingestion with retries |
| **Langfuse Preparation** | ✅ Complete | Ready for LLM instrumentation |
| **LogAI Anomaly Detection** | 🔄 In Progress | Python worker service |
| **Admin Analytics APIs** | 🔄 In Progress | Risk scoring, suspicious activity |
| **Documentation** | 🔄 In Progress | Setup guides for each tool |

---

## Architecture

### System Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ Syndicati Desktop Application                               │
│ - Controllers/Services/Views call UserActivityLogger        │
│ - Events captured with correlation IDs                      │
│ - Automatic data redaction applied                          │
└─────────────────┬───────────────────────────────────────────┘
                  │
       ┌──────────┴──────────┐
       ▼                     ▼
  ┌─────────┐         ┌──────────────┐
  │ MySQL   │         │ OpenObserve  │
  │ (Local) │         │ Exporter     │
  └────┬────┘         └──────┬───────┘
       │                     │ (batch, retry, circuit breaker)
       │                     ▼
       │              ┌────────────────┐
       │              │ OpenObserve    │
       │              │ - Search       │
       │              │ - Dashboards   │
       │              │ - Alerts       │
       │              └────────────────┘
       │
       │              ┌────────────────┐
       └─────────────▶│ LogAI Worker   │
                      │ - Anomalies    │
                      │ - Clustering   │
                      └────────┬───────┘
                               │
                      ┌────────▼────────┐
                      │ Anomaly Scores  │
                      │ (back to MySQL) │
                      └─────────────────┘

           ┌──────────────────────────────────┐
           │ Langfuse (LLM Observability)     │
           │ - Ready for future AI features   │
           └──────────────────────────────────┘
```

### Data Flow

1. **Capture** → User action triggers code → `UserActivityLogger.log()`
2. **Enrich** → Automatic enrichment with correlation IDs, metadata
3. **Validate** → EventValidator checks schema compliance
4. **Redact** → DataRedactor removes sensitive data
5. **Store** → Persist to local MySQL (app_event_log table)
6. **Export** → OpenObserveExporter batches and ships to OpenObserve
7. **Analyze** → OpenObserve indexes for search/dashboards
8. **Detect** → LogAI worker processes logs, detects anomalies
9. **Score** → Anomaly results written back to DB
10. **Alert** → Admin APIs surface insights for security team

---

## Quick Start

### 1. Enable Observability (Local Dev)

Edit `config/application.local.properties`:

```properties
# Enable OpenObserve export (requires OpenObserve running)
openobserve.enabled=false

# Will enable once OpenObserve is running locally
openobserve.url=http://localhost:5080
openobserve.username=admin
openobserve.password=Complexpass#123
openobserve.stream_name=syndicati
```

### 2. Run OpenObserve Locally (Docker)

```bash
docker run -p 5080:5080 \
  -e ZO_ROOT_USER_EMAIL=admin@example.com \
  -e ZO_ROOT_USER_PASSWORD=Complexpass#123 \
  public.ecr.aws/zinclabs/openobserve:latest
```

Then:
- Open http://localhost:5080
- Login: admin@example.com / Complexpass#123
- Navigate to **Streams** → you should see `syndicati` appear as app exports logs

### 3. Verify Logs Are Being Exported

After running the app for a few minutes:
1. Open OpenObserve dashboard
2. Go to **Explore** → select stream `syndicati`
3. You should see events like `PAGE_VIEW`, `UI_CLICK`, `USER_LOGIN`
4. Click on any log to see full structured data (traceId, metadata, risk scores, etc.)

### 4. Example Search Queries (in OpenObserve)

**Failed authentications:**
```sql
SELECT * FROM syndicati WHERE eventType = "AUTH_FAILURE"
```

**Slow operations:**
```sql
SELECT * FROM syndicati WHERE durationMs > 1000
```

**Recent security events:**
```sql
SELECT * FROM syndicati 
WHERE category = "SECURITY" 
ORDER BY eventTimestamp DESC 
LIMIT 20
```

**User timeline:**
```sql
SELECT eventType, action, outcome, durationMs, eventTimestamp 
FROM syndicati 
WHERE userId = 42 
ORDER BY eventTimestamp DESC
```

---

## Phase-by-Phase Setup

### Phase 1: Audit ✅ DONE

**What happened:** Analyzed existing logging system.

**Files created:**
- `docs/OBSERVABILITY_ARCHITECTURE.md` - Current state + target architecture

**Next:** Phase 2

### Phase 2: Event Standardization ✅ DONE

**What was implemented:**
- Event enums: `EventType`, `EventLevel`, `EventOutcome`, `EventCategory`
- `EventValidator` - Validates log schema
- `DataRedactor` - Redacts passwords, tokens, emails, credit cards, SSNs, PII
- `CorrelationContext` - Thread-local trace/span/session ID management
- **Enhanced** `UserActivityLogger` to use above services

**Key improvements:**
- Logs now use enums (type-safe)
- Sensitive data automatically redacted
- Correlation IDs propagate across threads
- IP addresses anonymized (last octet → 0)
- User agent capped at 2048 chars

**Files created:**
- `models/log/enums/EventType.java`
- `models/log/enums/EventLevel.java`
- `models/log/enums/EventOutcome.java`
- `models/log/enums/EventCategory.java`
- `services/log/EventValidator.java`
- `services/log/DataRedactor.java`
- `services/log/CorrelationContext.java`
- **Modified:** `services/log/UserActivityLogger.java`

**Usage in your code (no changes needed):**
Existing calls to `UserActivityLogger.log()` still work, but now with automatic validation and redaction.

### Phase 3: OpenObserve Integration ✅ DONE

**What was implemented:**
- `OpenObserveExporter` - Async batch HTTP exporter to OpenObserve
  - Batches logs before sending (configurable batch size/timeout)
  - Retry with exponential backoff (3 retries, 100ms-5000ms delays)
  - Circuit breaker (opens after 5 consecutive errors, cool-down 60s)
  - Thread-safe, daemon threads, graceful shutdown
- `OpenObserveConfig` - Loads config from `application.local.properties` or env vars
- **Configuration added** to `config/application.local.properties`

**How it works:**
1. Each log is queued asynchronously (non-blocking)
2. Batch sent every `batch_size` logs OR `batch_timeout_ms` milliseconds
3. Converted to JSON and POSTed to OpenObserve `/api/default/_json_post`
4. Failed batches retried with backoff
5. If export fails repeatedly, circuit breaker stops trying (logs still stored locally)

**Files created:**
- `services/observability/OpenObserveExporter.java`
- `services/observability/OpenObserveConfig.java`
- **Modified:** `pom.xml` (added Apache Commons)
- **Modified:** `config/application.local.properties` (added OpenObserve config)

**To enable:**
```properties
openobserve.enabled=true
```

### Phase 4: Langfuse Integration ✅ DONE

**What was implemented:**
- `LangfuseTracer` - Trace/span context for LLM observability
  - `Trace` class - represents end-to-end operation
  - `Span` class - represents sub-operation
  - `SpanEvent` - events within spans
  - Thread-local span stack for context propagation
  - Ready for future LLM instrumentation (no AI in app yet)
- `LangfuseConfig` - Loads config from properties/env

**How to use (when you add LLM features later):**
```java
LangfuseTracer tracer = new LangfuseTracer(config);
Span span = tracer.recordLLMCall("gpt-4", prompt, 200);
try {
    String response = callLLM(prompt);
    tracer.recordLLMResponse(span, response, tokensUsed, cost);
    tracer.endSpan(span, "success");
} catch (Exception e) {
    tracer.endSpan(span, "error");
}
```

**Files created:**
- `services/observability/LangfuseConfig.java`
- `services/observability/LangfuseTracer.java`
- **Modified:** `config/application.local.properties` (added Langfuse config)

**To enable (future):**
```properties
langfuse.enabled=true
langfuse.base_url=https://cloud.langfuse.com
langfuse.public_key=pk_xxx
langfuse.secret_key=sk_xxx
```

### Phase 5: LogAI Anomaly Detection 🔄 IN PROGRESS

**What needs to be done:**
- Create Python worker service (`workers/logai-anomaly-detection/`)
- Set up LogAI pipeline for batch scoring
- Create `AnomalyScoreService` in Java to call worker and update DB
- Implement scheduled job to periodically run scoring

**Placeholder in properties:**
```properties
logai.enabled=false
logai.worker_url=http://localhost:8001
logai.batch_size=100
logai.timeout_seconds=30
```

**See:** `LOGAI_SETUP.md` (coming)

### Phase 6: Advanced Analytics 🔄 IN PROGRESS

**What needs to be done:**
- `SuspiciousActivityService` - detect repeated failures, abnormal patterns
- `RiskScoringService` - aggregate risk signals
- `AnalyticsController` - admin API endpoints
- DTOs for results: `SuspiciousActivity`, `RiskScore`, `AnomalyResult`

**See:** `ANALYTICS_SETUP.md` (coming)

### Phase 7: Documentation & Tests 🔄 IN PROGRESS

**What needs to be done:**
- Unit tests for validators, redactors, exporters
- Integration tests for end-to-end flow
- Troubleshooting guide
- Production deployment guide

---

## Configuration

### Config File: `config/application.local.properties`

All observability features configured via properties or environment variables.

#### OpenObserve

```properties
# Enable/disable export (default: false)
openobserve.enabled=false

# OpenObserve instance URL (default: http://localhost:5080)
openobserve.url=http://localhost:5080

# Credentials
openobserve.username=admin
openobserve.password=Complexpass#123

# Stream name in OpenObserve (default: syndicati)
openobserve.stream_name=syndicati

# Batch configuration (default: 10 logs, 5000ms timeout)
openobserve.batch_size=10
openobserve.batch_timeout_ms=5000
```

#### Environment Variable Overrides

Any property can be overridden via environment variables:

```bash
# e.g., set OPENOBSERVE_ENABLED=true
export OPENOBSERVE_ENABLED=true
export OPENOBSERVE_URL=http://my-observeobserve.example.com:5080
export OPENOBSERVE_USERNAME=my-user
export OPENOBSERVE_PASSWORD=my-secret
```

Environment variables take precedence over properties file.

#### Langfuse

```properties
langfuse.enabled=false
langfuse.base_url=https://cloud.langfuse.com
langfuse.public_key=pk_xxxxxxxxxxxx
langfuse.secret_key=sk_xxxxxxxxxxxx
```

#### LogAI

```properties
logai.enabled=false
logai.worker_url=http://localhost:8001
logai.batch_size=100
logai.timeout_seconds=30
```

---

## Usage Examples

### Example 1: Logging a User Action

```java
// In your controller or service:
UserActivityLogger logger = new UserActivityLogger();

Map<String, Object> metadata = Map.of(
    "form_name", "edit_profile",
    "fields_changed", 5,
    "validation_errors", 0
);

logger.log(
    "USER_UPDATE",        // eventType
    "USER",               // entityType
    userId,               // entityId
    metadata,             // additional context
    currentUser           // user (optional)
);
```

**Result in database + OpenObserve:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "USER_UPDATE",
  "category": "USER_ACTIVITY",
  "action": "USER_UPDATE",
  "outcome": "SUCCESS",
  "level": "INFO",
  "userId": 42,
  "userEmail": "user@example.com",
  "traceId": "a1b2c3d4e5f6...",
  "spanId": "f7e8d9c0b1a2...",
  "requestId": "req-12345",
  "sessionId": "sess-abc123",
  "ipAddress": "192.168.1.0",
  "entityType": "USER",
  "entityId": 42,
  "serviceName": "SyndicatiDesktop",
  "environment": "desktop",
  "durationMs": 234,
  "riskScore": 0.1,
  "anomalyScore": 0.05,
  "metadata": {
    "form_name": "edit_profile",
    "fields_changed": 5,
    "validation_errors": 0,
    "host": "desktop-pc",
    "os": "Windows 10"
  },
  "_timestamp": 1713899123456
}
```

### Example 2: Tracking Page Views

```java
Map<String, Object> context = Map.of(
    "referrer", "/home",
    "viewport_width", 1920,
    "device_type", "desktop"
);

logger.logPageView("/profile/settings", "Profile Settings", context);
```

### Example 3: UI Click Tracking

```java
Map<String, Object> context = Map.of(
    "button_id", "save-profile-btn",
    "form_valid", true
);

logger.logUiClick("button#save-profile-btn", "Save Profile", context);
```

### Example 4: CRUD Operation

```java
Map<String, Object> metadata = Map.of(
    "previous_value", "old email",
    "new_value", "[EMAIL_REDACTED]",  // Auto-redacted!
    "timestamp_ms", 1500
);

logger.logCrudAction("UPDATE", "USER", userId, metadata);
```

### Example 5: Security Event

```java
Map<String, Object> securityData = Map.of(
    "failed_attempt_count", 3,
    "lockout_duration_min", 15
);

logger.log(
    "AUTH_FAILURE",
    "USER",
    null,
    securityData,
    null
);
```

---

## Troubleshooting

### Issue: Logs not appearing in OpenObserve

**Checklist:**
1. Is `openobserve.enabled=true` in properties?
2. Is OpenObserve running? (`docker ps` should show running container)
3. Can you reach it? (`curl http://localhost:5080` should return HTML)
4. Check logs for "Circuit breaker is open" messages
5. Check OpenObserve web UI for errors in the stream

**Debug:**
```bash
# Check console output from app
# Look for: "[OpenObserveExporter] Exported X logs to OpenObserve"
```

### Issue: "Circuit breaker is open"

Means OpenObserve exporter has failed 5 consecutive times. Reasons:
- OpenObserve not running
- Network unreachable
- Authentication failed (wrong username/password)
- OpenObserve server error (5xx status)

**Fix:**
1. Check OpenObserve is running
2. Verify credentials in properties
3. Wait 60 seconds for circuit breaker to reset
4. Restart app to force immediate retry

### Issue: Performance degradation

Observability shouldn't slow your app. If it does:
- Reduce batch size: `openobserve.batch_size=5`
- Disable OpenObserve: `openobserve.enabled=false`
- Disable data redaction (contact us for config flag)

### Issue: "Could not find artifact io.opentelemetry..."

This project uses OkHttp (already included) instead of full OpenTelemetry SDK. No action needed.

---

## Production Hardening

### Before Going to Production

#### 1. Security

- [ ] Change default OpenObserve credentials
- [ ] Run OpenObserve with TLS/HTTPS enabled
- [ ] Use strong passwords for all services
- [ ] Enable network segmentation (logs not accessible from internet)
- [ ] Audit redaction rules (ensure all PII is redacted)

#### 2. Performance

- [ ] Increase batch size for lower network overhead
  ```properties
  openobserve.batch_size=50
  ```
- [ ] Increase batch timeout to reduce frequency
  ```properties
  openobserve.batch_timeout_ms=10000  # 10 seconds
  ```
- [ ] Monitor circuit breaker status (alert if open for >5 min)

#### 3. Retention & Cleanup

- [ ] Set OpenObserve retention policy (e.g., 30 days)
- [ ] Archive old logs to S3/cold storage
- [ ] Set up log rotation in OpenObserve
- [ ] Monitor disk usage

#### 4. Monitoring

- [ ] Alert when circuit breaker opens
- [ ] Alert when export latency >2s
- [ ] Track successful vs failed exports
- [ ] Monitor anomaly detection accuracy (false positives)

#### 5. Backup & Disaster Recovery

- [ ] Backup OpenObserve configuration
- [ ] Test restore procedure
- [ ] Ensure local MySQL logs continue if OpenObserve down
- [ ] Plan for OpenObserve failover

### Scaling Considerations

For large deployments:
- Deploy OpenObserve cluster (HA)
- Use load balancer for OpenObserve instances
- Increase Java thread pools if thousands of concurrent users
- Consider time-series database for metrics (Prometheus + Grafana)

---

## Future Roadmap

### Short Term (Next Sprint)

- [ ] Implement LogAI worker (Python)
- [ ] Implement admin analytics APIs
- [ ] Add risk scoring algorithm
- [ ] Create Grafana dashboards

### Medium Term (2-3 Months)

- [ ] Multi-cloud support (AWS, GCP, Azure)
- [ ] Alert integration (email, Slack, PagerDuty)
- [ ] Custom rule engine for alerts
- [ ] User behavior analysis

### Long Term (6+ Months)

- [ ] Real-time anomaly detection (streaming)
- [ ] Integration with Langfuse for LLM tracing
- [ ] Federated observability (multi-instance Syndicati)
- [ ] Advanced ML models for prediction

---

## Resources

- **OpenObserve Docs:** https://openobserve.ai/docs/
- **Langfuse Docs:** https://langfuse.com/docs/
- **LogAI Docs:** https://github.com/logpai/logai
- **OpenTelemetry:** https://opentelemetry.io/
- **Distributed Tracing 101:** https://lightstep.com/learn/distributed-tracing

---

## Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review log files (check console output)
3. Check OpenObserve UI for hints
4. Contact team with:
   - Description of issue
   - Steps to reproduce
   - Console output/logs
   - Configuration (sanitized)

---

**Last Updated:** April 24, 2026  
**Next Review:** May 24, 2026

