#!/usr/bin/env python3
"""
LogAI anomaly scoring worker.

Exposes:
- GET /health
- POST /score

Input shape:
{
  "events": [
    {
      "id": 123,
      "eventType": "AUTH_FAILURE",
      "durationMs": 42,
      "riskScore": 0.3,
      "level": "WARN",
      ...
    }
  ]
}
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

import numpy as np
import pandas as pd
from flask import Flask, jsonify, request
from sklearn.ensemble import IsolationForest

app = Flask(__name__)


def _normalize_events(rows: list[dict[str, Any]]) -> pd.DataFrame:
    records: list[dict[str, Any]] = []
    for row in rows:
        event_type = str(row.get("eventType") or "UNKNOWN")
        outcome = str(row.get("outcome") or "UNKNOWN")
        level = str(row.get("level") or "INFO")
        category = str(row.get("category") or "UNKNOWN")

        duration = row.get("durationMs")
        try:
            duration_value = float(duration) if duration is not None else 0.0
        except (TypeError, ValueError):
            duration_value = 0.0

        risk = row.get("riskScore")
        try:
            risk_value = float(risk) if risk is not None else 0.0
        except (TypeError, ValueError):
            risk_value = 0.0

        records.append(
            {
                "id": row.get("id"),
                "eventType": event_type,
                "outcome": outcome,
                "level": level,
                "category": category,
                "durationMs": max(0.0, duration_value),
                "riskScore": min(max(risk_value, 0.0), 1.0),
                "hasTrace": 1.0 if row.get("traceId") else 0.0,
                "hasSession": 1.0 if row.get("sessionId") else 0.0,
                "hasMessage": 1.0 if row.get("message") else 0.0,
            }
        )

    return pd.DataFrame.from_records(records)


def _feature_matrix(df: pd.DataFrame) -> pd.DataFrame:
    base = df[["durationMs", "riskScore", "hasTrace", "hasSession", "hasMessage"]].copy()
    cat = pd.get_dummies(df[["eventType", "outcome", "level", "category"]], dtype=float)
    return pd.concat([base, cat], axis=1)


def _score_events(df: pd.DataFrame) -> np.ndarray:
    if df.empty:
        return np.array([])

    X = _feature_matrix(df)

    # IsolationForest is deterministic here for repeatable behavior in tests.
    model = IsolationForest(
        n_estimators=200,
        contamination="auto",
        random_state=42,
        n_jobs=1,
    )
    model.fit(X)

    # decision_function: higher is more normal; invert and normalize to anomaly score.
    raw = -model.decision_function(X)
    min_v = float(np.min(raw))
    max_v = float(np.max(raw))

    if max_v - min_v < 1e-9:
        return np.clip(raw, 0.0, 1.0)

    return np.clip((raw - min_v) / (max_v - min_v), 0.0, 1.0)


def _label(score: float) -> str:
    if score >= 0.8:
        return "CRITICAL_ANOMALY"
    if score >= 0.65:
        return "ANOMALY"
    if score >= 0.45:
        return "SUSPICIOUS"
    return "NORMAL"


def _reason(row: pd.Series, score: float) -> str:
    if score < 0.45:
        return "No significant anomaly detected"

    reasons: list[str] = []
    if row["eventType"] == "AUTH_FAILURE":
        reasons.append("authentication failure pattern")
    if float(row["durationMs"]) > 1500:
        reasons.append("high latency event")
    if float(row["riskScore"]) >= 0.6:
        reasons.append("high risk score in source log")
    if not reasons:
        reasons.append("deviates from batch behavior baseline")
    return ", ".join(reasons)


@app.get("/health")
def health() -> Any:
    return jsonify({"status": "ok", "service": "logai-anomaly-worker", "time": datetime.now(timezone.utc).isoformat()})


@app.post("/score")
def score() -> Any:
    payload = request.get_json(silent=True) or {}
    events = payload.get("events") or []
    if not isinstance(events, list):
        return jsonify({"error": "events must be a list"}), 400

    if len(events) == 0:
        return jsonify({"results": []})

    df = _normalize_events(events)
    scores = _score_events(df)

    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    results = []

    for idx, row in df.iterrows():
        score_value = float(scores[idx])
        label = _label(score_value)
        results.append(
            {
                "eventId": int(row["id"]),
                "anomalyScore": round(score_value, 6),
                "anomalyLabel": label,
                "anomalyReason": _reason(row, score_value),
                "detectedAt": now,
            }
        )

    return jsonify({"results": results})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8001, debug=False)
