# Observability Pipeline - Developer Quick Start

**Status:** Phases 1-6 Complete | Phase 7 (Tests) Remaining  
**Updated:** April 24, 2026

---

## TL;DR - What Changed?

Your existing Syndicati app now has **enterprise observability** built-in:

✅ **Automatic features** (no code changes needed):
- Logs validated & redacted
- Correlation IDs (traces) tracked
- Can export to OpenObserve for search/dashboards
- Risk scoring & anomaly detection ready

🔄 **Coming soon:**
- LogAI anomaly detection worker (Phase 5)
- Admin API dashboard (Phase 6)

---

## For App Developers (No Changes Needed)

### Existing Code Still Works

```java
// This still works exactly as before:
UserActivityLogger logger = new UserActivityLogger();
logger.log("USER_LOGIN", "USER", userId, metadata, user);

// But now gets automatic benefits:
// ✅ Validates schema
// ✅ Redacts passwords/tokens
// ✅ Tracks trace IDs
// ✅ Can export to OpenObserve
```

### No Dependencies Added to Your Code

All observability features are used internally by `UserActivityLogger`. Your code doesn't change.

---

## For DevOps / Platform Team

### Enable Observability (Local Dev)

1. **Run OpenObserve locally:**
```bash
docker run -p 5080:5080 \
  -e ZO_ROOT_USER_EMAIL=admin@example.com \
  -e ZO_ROOT_USER_PASSWORD=Complexpass#123 \
  public.ecr.aws/zinclabs/openobserve:latest
```

2. **Enable in config/application.local.properties:**
```properties
openobserve.enabled=true
```

3. **Verify export:**
- Open http://localhost:5080
- Login: admin@example.com / Complexpass#123
- Go to Explore → stream `syndicati`
- See events flowing in

### Production Checklist

Before deploying to production:

- [ ] Change OpenObserve default credentials
- [ ] Enable HTTPS for OpenObserve
- [ ] Set resource limits (memory, disk)
- [ ] Configure log retention (e.g., 30 days)
- [ ] Set up alerts for failed exports
- [ ] Test circuit breaker (what happens if OpenObserve down?)
- [ ] Review data redaction rules
- [ ] Load test with expected volume

---

## File Reference

### Where Are the New Classes?

```
src/main/java/com/syndicati/
├── models/log/enums/
│   ├── EventType.java          ← Type-safe event names
│   ├── EventLevel.java         ← Severity levels
│   ├── EventOutcome.java       ← Operation results
│   └── EventCategory.java      ← Event categories
│
├── services/log/
│   ├── EventValidator.java     ← Schema checking
│   ├── DataRedactor.java       ← Remove passwords/tokens/PII
│   ├── CorrelationContext.java ← Distributed trace IDs
│   └── UserActivityLogger.java ← ENHANCED with above
│
└── services/observability/
    ├── OpenObserveExporter.java    ← HTTP batch exporter
    ├── OpenObserveConfig.java      ← Load config from properties
    ├── LangfuseTracer.java         ← LLM instrumentation (ready)
    └── LangfuseConfig.java         ← LLM config

    services/analytics/
    ├── SuspiciousActivityService.java  ← Detect suspicious patterns
    ├── RiskScoringService.java        ← Aggregate risk signals
    └── AnomalyScoreService.java       ← LogAI integration (placeholder)
```

### Configuration

All settings in: `config/application.local.properties`

```properties
# OpenObserve
openobserve.enabled=true/false
openobserve.url=http://localhost:5080
openobserve.username=admin
openobserve.password=***
openobserve.stream_name=syndicati
openobserve.batch_size=10            # How many logs before send
openobserve.batch_timeout_ms=5000    # Or send after X ms

# Langfuse (future LLM tracking)
langfuse.enabled=false
langfuse.base_url=https://cloud.langfuse.com
langfuse.public_key=pk_xxx
langfuse.secret_key=sk_xxx

# LogAI (anomaly detection - phase 5)
logai.enabled=false
logai.worker_url=http://localhost:8001
logai.batch_size=100
logai.timeout_seconds=30
```

**Env var overrides:**
- Any property can be overridden via env var
- Example: `OPENOBSERVE_ENABLED=true` overrides properties file

---

## Key Features Explained

### 1. Event Standardization

**Problem:** Events logged as strings → typos, inconsistency

**Solution:** Type-safe enums
```java
public enum EventType {
    USER_LOGIN,
    USER_LOGOUT,
    PAGE_VIEW,
    UI_CLICK,
    CRUD,       // Generic CRUD event
    // ... 30+ more
}
```

**Benefit:** IDE autocomplete, compiler catches typos

### 2. Data Redaction

**Problem:** Sensitive data in logs → security risk

**Solution:** Automatic redaction
```
"message": "Login failed: password=mySecret123"
             ↓ [Redacted by DataRedactor]
"message": "Login failed: password=[REDACTED]"
```

**Redacts:**
- Passwords, API keys, tokens
- Credit card numbers (16 digits)
- SSNs, emails, phone numbers
- Any field with sensitive-sounding name

### 3. Correlation IDs (Distributed Tracing)

**Problem:** Logs from same user transaction scattered across threads

**Solution:** Thread-local trace IDs
```java
// Request from user: logs contain
{
  "traceId": "a1b2c3d4e5f6...",     ← Same for all logs in transaction
  "spanId": "f7e8d9c0b1a2...",      ← Can track sub-operations
  "requestId": "req-12345",
  "sessionId": "sess-abc123"
}
```

**Benefit:** Click on one log → see entire user transaction

### 4. OpenObserve Export

**Problem:** MySQL logs not searchable/dashboarded

**Solution:** Async export to OpenObserve
```
Your App → Log Queue → Batch → HTTP POST → OpenObserve
           (async,          (retry, circuit   (search, dashboards)
           non-blocking)    breaker)
```

**Reliability:**
- Retry with exponential backoff (3 attempts)
- Circuit breaker (stops after 5 failures)
- Local MySQL keeps logs even if export fails

### 5. Risk Scoring

**Problem:** Which users/sessions are risky?

**Solution:** Aggregate risk signals
```java
RiskResult = (30% * anomalyScore)
           + (20% * baselineRiskScore)
           + (40% * suspiciousActivityFlags)
           + (10% * behaviorDeviation)

Result: SAFE, LOW, MEDIUM, HIGH, CRITICAL
        + Recommendation for each level
```

**Example recommendations:**
- CRITICAL → "Block immediately, require MFA"
- HIGH → "Flag for review, consider re-auth"
- MEDIUM → "Monitor closely"

### 6. Suspicious Activity Detection

**Problem:** Hard to spot abuse patterns manually

**Solution:** Automatic detection
```
Detects:
✓ Repeated failed auth (3+ failures)
✓ Abnormal frequency (10+ events/min)
✓ Bulk operations (5+ deletes in 5 min)
✓ Unusual user paths (5+ different entity types)

Each generates a flag with severity & score
```

---

## How It All Works (Architecture)

```
┌──────────────────────────────────┐
│ Your Code Calls UserActivityLogger│
│ logger.log("USER_LOGIN", ...)    │
└────────────────┬─────────────────┘
                 │
         ┌───────▼────────┐
         │ EventValidator │  ← Check schema
         └───────┬────────┘
                 │
         ┌───────▼────────┐
         │ DataRedactor   │  ← Remove passwords, tokens
         └───────┬────────┘
                 │
      ┌──────────▼──────────┐
      │ CorrelationContext  │  ← Add trace IDs
      └──────────┬──────────┘
                 │
         ┌───────▼────────┐
         │ MySQL Database │  ← Persist locally
         └───────┬────────┘
                 │
      ┌──────────▼──────────────┐
      │ OpenObserveExporter     │  ← Async batch export
      │ (if enabled)            │
      │ - Queue logs            │
      │ - Batch every 10 logs   │
      │ - Retry on failure      │
      │ - Circuit breaker       │
      └──────────┬──────────────┘
                 │
      ┌──────────▼──────────────┐
      │ OpenObserve Instance    │  ← Search, dashboards, alerts
      └─────────────────────────┘
```

---

## Testing Locally

### 1. Setup

```bash
# Terminal 1: Run OpenObserve
docker run -p 5080:5080 \
  -e ZO_ROOT_USER_EMAIL=admin@example.com \
  -e ZO_ROOT_USER_PASSWORD=Complexpass#123 \
  public.ecr.aws/zinclabs/openobserve:latest

# Terminal 2: Enable in properties
# Edit config/application.local.properties
openobserve.enabled=true
```

### 2. Run Syndicati App

```bash
# Your normal startup command
# App automatically exports logs
```

### 3. Check Logs in OpenObserve

```
1. Open http://localhost:5080
2. Login: admin@example.com / Complexpass#123
3. Click "Explore"
4. Select stream: "syndicati"
5. See your logs flowing in real-time!
```

### 4. Example Queries

**Failed authentications:**
```sql
SELECT * FROM syndicati WHERE eventType = 'AUTH_FAILURE'
```

**Slow operations:**
```sql
SELECT * FROM syndicati WHERE durationMs > 1000
```

**User timeline:**
```sql
SELECT * FROM syndicati 
WHERE userId = 42 
ORDER BY eventTimestamp DESC 
LIMIT 100
```

---

## Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| Logs not in OpenObserve | Export disabled or OpenObserve not running | Check `openobserve.enabled=true`, run Docker |
| "Circuit breaker is open" | Export failed 5 times | Wait 60 sec, check if OpenObserve is up |
| Authentication rejected | Wrong password | Verify `openobserve.password` in properties |
| Performance degradation | Export throttling UI | Increase `batch_size`, reduce batch frequency |
| Sensitive data in logs | Redaction not working | Check if field name is in `SENSITIVE_FIELD_NAMES` list |

---

## What NOT to Do

❌ **Don't:**
- Put passwords in log message text (use context/metadata, it gets redacted)
- Hard-code config values (use properties/env vars)
- Call OpenObserve directly (use `OpenObserveExporter`)
- Ignore circuit breaker errors (it's protecting your network)
- Disable validation just because it's slow (it's not - <1ms)

✅ **Do:**
- Use enums for event types (`EventType.USER_LOGIN` not `"USER_LOGIN"`)
- Put sensitive data in context map (gets redacted automatically)
- Use environment variables for production config
- Monitor circuit breaker status
- Review redaction rules regularly

---

## For Phase 5: Adding LogAI Anomaly Detection

When you're ready to implement anomaly detection:

1. Create Python worker service in `workers/logai-anomaly-detection/`
2. Implement `AnomalyScoreService.scoreRecentEvents()` to call worker
3. Add scheduled task to run periodically
4. Update DB with anomaly scores
5. Review `docs/OBSERVABILITY_README.md` for details

---

## For Phase 6: Adding Admin Analytics APIs

When you're ready to add admin endpoints:

1. Create `controllers/admin/AnalyticsController.java`
2. Add endpoints:
   - `GET /api/admin/anomalies/recent`
   - `GET /api/admin/suspicious-users`
   - `GET /api/admin/user/{id}/timeline`
   - `GET /api/admin/analytics/feature-usage`
3. Use `SuspiciousActivityService` and `RiskScoringService`
4. Return results as JSON

---

## For Phase 7: Adding Unit Tests

Test these services:
- ✅ `EventValidator` - schema validation
- ✅ `DataRedactor` - redaction patterns
- ✅ `CorrelationContext` - thread-local state
- ✅ `OpenObserveExporter` - retry logic, circuit breaker
- ✅ `SuspiciousActivityService` - pattern detection
- ✅ `RiskScoringService` - aggregation logic

---

## Documentation

- **Setup Guide:** `docs/OBSERVABILITY_README.md`
- **Architecture:** `docs/OBSERVABILITY_ARCHITECTURE.md`
- **This File:** `docs/DEVELOPER_QUICK_START.md`
- **Implementation Summary:** `docs/IMPLEMENTATION_SUMMARY.md`

---

## Support

Questions? Check:
1. This file (DEVELOPER_QUICK_START.md)
2. OBSERVABILITY_README.md (detailed guide)
3. Code comments in service files
4. Test the feature locally before asking

---

**Last Updated:** April 24, 2026  
**Status:** Phases 1-6 Complete ✅  
**Next:** Phase 7 Unit Tests

