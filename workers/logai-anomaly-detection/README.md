# LogAI Anomaly Worker

This worker scores batches of application events and returns anomaly metadata.

## Endpoints

- GET /health
- POST /score

## Setup

1. Create venv:
   - python -m venv .venv
   - .\.venv\Scripts\activate
2. Install deps:
   - pip install -r requirements.txt
3. Run worker:
   - python anomaly_service.py

Worker URL defaults to http://localhost:8001 and is configured by logai.worker_url.

## Request Example

POST /score

{
  "events": [
    {
      "id": 101,
      "eventType": "AUTH_FAILURE",
      "riskScore": 0.7,
      "durationMs": 1900,
      "level": "WARN",
      "category": "SECURITY"
    }
  ]
}

## Response Example

{
  "results": [
    {
      "eventId": 101,
      "anomalyScore": 0.91,
      "anomalyLabel": "CRITICAL_ANOMALY",
      "anomalyReason": "authentication failure pattern, high latency event",
      "detectedAt": "2026-04-24T20:00:00+00:00"
    }
  ]
}
