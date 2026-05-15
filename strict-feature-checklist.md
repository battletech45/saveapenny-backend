# SaveAPenny Strict Feature Checklist

Status legend:
- Partial: present but incomplete versus technical-doc scope.
- Missing: not found in current codebase.

Reference baseline: `technical-doc.md` sections 5, 7, 8, and 9.

## 1) API Design (Section 5)

| Feature | Status | Evidence |
| --- | --- | --- |

## 2) Key Business Rules (Section 7)

| Rule | Status | Evidence |
| --- | --- | --- |
| Recurring transactions idempotency with `next_run_date` and distributed lock | Partial | Recurring CRUD and due-selection exist (`src/main/java/com/saveapenny/automation/service/impl/RecurringTransactionServiceImpl.java`), but no scheduler/distributed lock execution path found |

## 3) Phased Delivery (Section 8)

| Phase | Scope | Status | Evidence |
| --- | --- | --- | --- |
| 0 | Scaffold, PostgreSQL, Flyway, global errors, Swagger | Partial | Scaffold/Flyway/error handling present; Swagger integration not verified in code scan |
| 6 | Reports + CSV export | Partial | Monthly summary CSV export implemented in `src/main/java/com/saveapenny/report/controller/ReportController.java`; remaining report CSV exports not found |
| 7 | Recurring transactions + scheduler + idempotency | Partial | Recurring rule management implemented; scheduler/lock-based generation not found |
| 8 | Notifications (in-app + event + email + preferences) | Partial | In-app notification API present in `src/main/java/com/saveapenny/notification/`; event/email/preferences not found |
| 10 | Unit + integration + Testcontainers + 70%+ coverage | Partial | Unit/integration tests strong; Testcontainers usage not found in test classes |
| 11 | Redis (cache/token blacklist/rate limiting) | Missing | No Redis config/module found in main code |
| 12 | Event-driven (Kafka/RabbitMQ + retry + DLQ) | Missing | No event bus producer/consumer messaging modules found |
| 13 | Security hardening (lockout, headers, CORS hardening) | Partial | Baseline Spring Security present; lockout and hardening set not fully found |
| 14 | Deployment (Docker image, CI/CD, cloud, HTTPS) | Partial | `docker-compose.yml` exists for Postgres only; app container/pipeline/cloud/HTTPS not found |
| 15 | Observability (Actuator, Prometheus, Grafana, OTel) | Missing | No observability stack integration found |
| 16 | Microservice extraction + API gateway | Missing | Current architecture remains modular monolith |

## 4) MVP Acceptance Criteria (Section 9)

| Criterion | Status | Evidence |
| --- | --- | --- |
| Flyway migrations run cleanly on fresh database | Partial | Migrations exist in `src/main/resources/db/migration/`, but integration tests disable Flyway |
| App boots with `docker compose up` | Missing | `docker-compose.yml` starts PostgreSQL only; app service is not defined |

## 5) Current Quality Gate Snapshot

- Coverage threshold check (70%+): Partial (no explicit coverage gate configured in `pom.xml`).
