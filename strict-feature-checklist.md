# SaveAPenny Strict Feature Checklist

Status legend:
- Partial: present but incomplete versus technical-doc scope.
- Missing: not found in current codebase.

Reference baseline: `technical-doc.md` sections 5, 7, 8, and 9.

## 1) API Design (Section 5)

| Feature | Status | Evidence |
| --- | --- | --- |
| Reports CSV export coverage | Partial | Monthly summary CSV export exists in `src/main/java/com/saveapenny/report/controller/ReportController.java`; category-spending/cash-flow/net-worth CSV exports are not implemented |
| Notification channel coverage | Partial | In-app notification endpoints are implemented in `src/main/java/com/saveapenny/notification/controller/NotificationController.java`; event-triggered/email/preferences flows are not implemented |
| OCR import API coverage | Complete | OCR submit/status endpoints implemented in `src/main/java/com/saveapenny/imports/controller/OcrImportController.java` with validation and async job tracking |

## 2) Key Business Rules (Section 7)

| Rule | Status | Evidence |
| --- | --- | --- |

## 3) Phased Delivery (Section 8)

| Phase | Scope | Status | Evidence |
| --- | --- | --- | --- |
| 10 | Unit + integration + Testcontainers + 70%+ coverage | Partial | Unit/integration tests strong; Testcontainers usage not found in test classes |
| 11 | Redis (cache/token blacklist/rate limiting) | Missing | No Redis config/module found in main code |
| 12 | Event-driven (Kafka/RabbitMQ + retry + DLQ) | Missing | No event bus producer/consumer messaging modules found |
| 13 | Security hardening (lockout, headers, CORS hardening) | Partial | Baseline Spring Security present; lockout and hardening set not fully found |
| 14 | Deployment (Docker image, CI/CD, cloud, HTTPS) | Partial | `Dockerfile` added and `docker-compose.yml` includes app + Postgres + healthcheck; CI/CD/cloud/HTTPS are still pending |
| 15 | Observability (Actuator, Prometheus, Grafana, OTel) | Partial | Actuator health endpoint and OCR health indicator/counters/logging added; Prometheus/Grafana/OTel are pending |
| 16 | Microservice extraction + API gateway | Missing | Current architecture remains modular monolith |

## 4) MVP Acceptance Criteria (Section 9)

| Criterion | Status | Evidence |
| --- | --- | --- |
| Flyway migrations run cleanly on fresh database | Partial | Migrations exist in `src/main/resources/db/migration/`, but integration tests disable Flyway |
| App boots with `docker compose up` | Partial | App service and container healthcheck are defined; environment/runtime verification still pending |

## 5) Current Quality Gate Snapshot

- Coverage threshold check (70%+): Partial (no explicit coverage gate configured in `pom.xml`).
