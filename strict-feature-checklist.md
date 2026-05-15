# SaveAPenny Strict Feature Checklist

Status legend:
- Implemented: present in code and covered by tests.
- Partial: present but incomplete versus technical-doc scope.
- Missing: not found in current codebase.

Reference baseline: `technical-doc.md` sections 5, 7, 8, and 9.

## 1) API Design (Section 5)

| Feature | Status | Evidence |
| --- | --- | --- |
| Standard response envelope (`success`, `data`, `error`, `timestamp`) | Implemented | `src/main/java/com/saveapenny/shared/api/ApiResponse.java`, `src/main/java/com/saveapenny/shared/exception/GlobalExceptionHandler.java` |
| Auth endpoints (`/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`) | Implemented | `src/main/java/com/saveapenny/auth/controller/AuthController.java`, `src/test/java/com/saveapenny/auth/integration/AuthFlowIntegrationTest.java` |
| User profile endpoints (`GET/PUT /users/me`, `PUT /users/me/password`) | Implemented | `src/main/java/com/saveapenny/user/controller/UserController.java`, `src/test/java/com/saveapenny/user/controller/UserControllerTest.java` |
| Accounts CRUD | Implemented | `src/main/java/com/saveapenny/account/controller/AccountController.java`, `src/test/java/com/saveapenny/account/integration/AccountFlowIntegrationTest.java` |
| Categories CRUD + delete guard behavior | Implemented | `src/main/java/com/saveapenny/category/controller/CategoryController.java`, `src/test/java/com/saveapenny/category/integration/CategoryFlowIntegrationTest.java` |
| Transactions CRUD + transfer endpoint | Implemented | `src/main/java/com/saveapenny/transaction/controller/TransactionController.java`, `src/test/java/com/saveapenny/transaction/integration/TransactionFlowIntegrationTest.java` |
| Transactions full filtering contract (`minAmount`, `maxAmount`, `keyword` included in spec) | Partial | Controller currently exposes `from`, `to`, `type`, `accountId`, `categoryId` in `src/main/java/com/saveapenny/transaction/controller/TransactionController.java` |
| Budgets CRUD + budget status endpoint | Implemented | `src/main/java/com/saveapenny/budget/controller/BudgetController.java`, `src/test/java/com/saveapenny/budget/integration/BudgetFlowIntegrationTest.java` |
| Reports endpoints (`monthly-summary`, `category-spending`, `cash-flow`, `net-worth`) | Implemented | `src/main/java/com/saveapenny/report/controller/ReportController.java`, `src/test/java/com/saveapenny/report/integration/ReportFlowIntegrationTest.java` |
| Reports CSV export | Missing | No report CSV endpoint/service found under `src/main/java/com/saveapenny/report/` |
| CSV import flow (`preview -> confirm -> status`) | Implemented | `src/main/java/com/saveapenny/imports/controller/ImportController.java`, `src/test/java/com/saveapenny/imports/integration/ImportFlowIntegrationTest.java` |
| Audit logs endpoints (`POST /audits`, `GET /audits`, `GET /audits/{id}`) | Implemented | `src/main/java/com/saveapenny/audit/controller/AuditLogController.java`, `src/test/java/com/saveapenny/audit/integration/AuditFlowIntegrationTest.java` |

## 2) Key Business Rules (Section 7)

| Rule | Status | Evidence |
| --- | --- | --- |
| Transaction creation updates account balance atomically | Implemented | `src/main/java/com/saveapenny/transaction/service/impl/TransactionServiceImpl.java`, `src/test/java/com/saveapenny/transaction/service/impl/TransactionServiceImplTest.java` |
| Transfer performs debit + credit in one transaction boundary | Implemented | `src/main/java/com/saveapenny/transaction/service/impl/TransactionServiceImpl.java`, `src/test/java/com/saveapenny/transaction/integration/TransactionFlowIntegrationTest.java` |
| Category delete blocked when used by transactions | Implemented | `src/main/java/com/saveapenny/category/service/impl/CategoryServiceImpl.java`, `src/test/java/com/saveapenny/category/service/impl/CategoryServiceImplTest.java` |
| Budget WARNING threshold at >=80% | Implemented | `src/main/java/com/saveapenny/budget/service/impl/BudgetServiceImpl.java`, `src/test/java/com/saveapenny/budget/service/impl/BudgetServiceImplTest.java` |
| CSV duplicate detection via hash (`account_id, amount, date, description`) | Missing | Not found in import processing (`src/main/java/com/saveapenny/imports/service/impl/ImportServiceImpl.java`, `src/main/java/com/saveapenny/imports/service/impl/ImportAsyncJobService.java`) |
| Recurring transactions idempotency with `next_run_date` and distributed lock | Partial | Recurring CRUD and due-selection exist (`src/main/java/com/saveapenny/automation/service/impl/RecurringTransactionServiceImpl.java`), but no scheduler/distributed lock execution path found |
| CSV confirm runs asynchronously | Implemented | Async job via `@Async` in `src/main/java/com/saveapenny/imports/service/impl/ImportAsyncJobService.java` and executor config in `src/main/java/com/saveapenny/config/AsyncConfig.java` |

## 3) Phased Delivery (Section 8)

| Phase | Scope | Status | Evidence |
| --- | --- | --- | --- |
| 0 | Scaffold, PostgreSQL, Flyway, global errors, Swagger | Partial | Scaffold/Flyway/error handling present; Swagger integration not verified in code scan |
| 1 | Auth + roles + profile | Implemented | `src/main/java/com/saveapenny/auth/`, `src/main/java/com/saveapenny/user/`, related tests under `src/test/java/com/saveapenny/auth/` |
| 2 | Accounts | Implemented | `src/main/java/com/saveapenny/account/`, `src/test/java/com/saveapenny/account/` |
| 3 | Categories | Implemented | `src/main/java/com/saveapenny/category/`, `src/test/java/com/saveapenny/category/` |
| 4 | Transactions + balance updates + audit log | Implemented | `src/main/java/com/saveapenny/transaction/`, `src/main/java/com/saveapenny/audit/` |
| 5 | Budgets | Implemented | `src/main/java/com/saveapenny/budget/`, tests under `src/test/java/com/saveapenny/budget/` |
| 6 | Reports + CSV export | Partial | Report endpoints implemented; CSV export missing from report module |
| 7 | Recurring transactions + scheduler + idempotency | Partial | Recurring rule management implemented; scheduler/lock-based generation not found |
| 8 | Notifications (in-app + event + email + preferences) | Partial | In-app notification API present in `src/main/java/com/saveapenny/notification/`; event/email/preferences not found |
| 9 | CSV import async flow | Implemented | `src/main/java/com/saveapenny/imports/`, `src/test/java/com/saveapenny/imports/integration/ImportFlowIntegrationTest.java` |
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
| Auth endpoints work with JWT enforcement | Implemented | `src/test/java/com/saveapenny/auth/integration/AuthFlowIntegrationTest.java` |
| Private routes return 401 when unauthenticated | Implemented | Controller tests across modules (e.g. `src/test/java/com/saveapenny/account/controller/AccountControllerTest.java`) |
| Account data scoped to authenticated user | Implemented | `src/test/java/com/saveapenny/account/integration/AccountFlowIntegrationTest.java` |
| Transaction creation updates balance atomically | Implemented | `src/test/java/com/saveapenny/transaction/integration/TransactionFlowIntegrationTest.java` |
| Transfer debits/credits in single boundary | Implemented | `src/test/java/com/saveapenny/transaction/integration/TransactionFlowIntegrationTest.java` |
| Budget status spending accuracy | Implemented | `src/test/java/com/saveapenny/budget/integration/BudgetFlowIntegrationTest.java` |
| CSV import preview/confirm/status for authenticated users | Implemented | `src/test/java/com/saveapenny/imports/integration/ImportFlowIntegrationTest.java` |
| Audit create/list/detail enforce ownership | Implemented | `src/test/java/com/saveapenny/audit/integration/AuditFlowIntegrationTest.java` |
| Monthly summary report returns correct aggregates | Implemented | `src/test/java/com/saveapenny/report/integration/ReportFlowIntegrationTest.java` |
| Flyway migrations run cleanly on fresh database | Partial | Migrations exist in `src/main/resources/db/migration/`, but integration tests disable Flyway |
| Core business logic covered by unit tests | Implemented | Service tests across modules under `src/test/java/com/saveapenny/**/service/impl/*Test.java` |
| App boots with `docker compose up` | Missing | `docker-compose.yml` starts PostgreSQL only; app service is not defined |

## 5) Current Quality Gate Snapshot

- Test suite status: Implemented (`mvn test` passed, 147 tests, 0 failures).
- Coverage threshold check (70%+): Partial (no explicit coverage gate configured in `pom.xml`).
