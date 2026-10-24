# Monitoring, Alerts & Load Test — VYRA Backend

Phase 2.9. VYRA is not deployed yet: everything below is ready in the code and is switched on when the
first environment exists.

## 1. Endpoints

| Endpoint | Access | Use |
|---|---|---|
| `GET /actuator/health` | public | External uptime monitor. `{"status":"UP"}` when the app and the database answer; no details are shown. |
| `GET /actuator/prometheus` | header `X-Metrics-Token: <VYRA_METRICS_TOKEN>` | Metrics for the dashboard (Prometheus / Grafana Cloud or the host's own). Without `VYRA_METRICS_TOKEN` nobody can read it. |

Configuration (`application.yaml`, env vars):

| Env var | Meaning |
|---|---|
| `VYRA_METRICS_TOKEN` | Long random value shared with the scraper only. |
| `VYRA_SCHEDULER_ENABLED` | `true` (default): reminder, push and export jobs run (ShedLock keeps each run on one instance). |
| `FIREBASE_CREDENTIALS`, `FIREBASE_PROJECT_ID` | Push notifications (service account JSON or a path to it). Empty = push off. |
| `VYRA_PUBLIC_BASE_URL` | Public URL of the API, used in the e-mailed export link. |

## 2. Dashboard

| Panel | Query (Prometheus) |
|---|---|
| Requests / s | `sum(rate(http_server_requests_seconds_count[5m]))` |
| Error rate (5xx) | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))` |
| Latency p95 per endpoint | `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[5m])))` |
| DB pool in use | `hikaricp_connections_active` / `hikaricp_connections_max` |
| JVM heap | `jvm_memory_used_bytes{area="heap"}` |
| Scheduled jobs | log lines `Habit reminders created`, `Goal due notifications created`, `Data export failed` (log search) |

## 3. Alerts

| Alert | Condition | Action |
|---|---|---|
| API down | uptime monitor: `/actuator/health` not 200 for 2 checks (1 min apart) | Check the host, then the database |
| Error rate | 5xx > 2% for 5 min | Look at the logs by traceId |
| Slow | p95 > 500 ms for 10 min on any member endpoint | Check DB pool, slow queries |
| DB pool full | active = max for 5 min | Raise `DB_POOL_SIZE` or find the slow query |

## 4. Load test

Script: `scripts/loadtest/read-endpoints.js` ([k6](https://k6.io)). Run it against a staging copy with a test
member that has realistic data, never against production members:

```
k6 run -e BASE_URL=https://<staging host> -e TOKEN=<test member token> scripts/loadtest/read-endpoints.js
```

It covers the home summary, transaction history, budgets, finance summary, today's habits, the weekly
insight and the mood trend. It fails when p95 > 500 ms or more than 1% of requests fail. Keep the k6
summary of each run with the release notes.

## 5. Logs

Amounts, balances, journal text, habit / goal / category names, notification texts, push tokens and
export tokens are never logged. Logs carry ids (memberId, habitId, exportId, ...) and the traceId only.
