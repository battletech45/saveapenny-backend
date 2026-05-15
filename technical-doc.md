---

> **Version:** 1.0 · **Audience:** Backend Engineers · **Stack:** Java 21 · Spring Boot · PostgreSQL · Modular Monolith
> 

---

## 1. Overview

SaveAPenny is a personal finance platform for tracking accounts, transactions, budgets, and spending patterns. The system is designed as a **modular monolith** first, with a clear extraction path toward microservices in later phases.

This document covers architecture decisions, module boundaries, data model, API contracts, and phased delivery milestones.

---

## 2. Architecture

### 2.1 Pattern: Modular Monolith

Single deployable unit, internally divided into isolated modules. Each module owns its entities, services, repositories, and DTOs. Cross-module communication happens through well-defined service interfaces — never direct repository access.

```
com.saveapenny
 ├── auth
 ├── user
 ├── account
 ├── transaction
 ├── category
 ├── budget
 ├── report
 ├── automation
 ├── notification
 ├── imports
 ├── audit
 ├── shared
 └── config
```

Each module's internal structure:

```
{module}
 ├── controller      # REST endpoints
 ├── service         # Business logic
 ├── repository      # JPA repositories
 ├── entity          # JPA entities
 ├── dto             # Request/Response objects
 ├── mapper          # MapStruct mappers
 └── exception       # Module-specific exceptions
```

### 2.2 Layer Diagram

```
┌─────────────────────────────────────────────┐
│              REST Controllers                │
│         (Spring MVC, validation)             │
├─────────────────────────────────────────────┤
│              Service Layer                   │
│     (business rules, @Transactional)         │
├─────────────────────────────────────────────┤
│            Repository Layer                  │
│       (Spring Data JPA, Specifications)      │
├─────────────────────────────────────────────┤
│              Database                        │
│         PostgreSQL via Flyway                │
└─────────────────────────────────────────────┘
```

### 2.3 Cross-Cutting Concerns

| Concern | Solution |
| --- | --- |
| Authentication | JWT (access + refresh tokens) |
| Authorization | Spring Security + `@PreAuthorize` |
| Validation | Hibernate Validator on DTOs |
| Error handling | `@RestControllerAdvice` + standard envelope |
| DB migrations | Flyway |
| Object mapping | MapStruct |
| Documentation | Springdoc OpenAPI (Swagger UI) |
| Logging | SLF4J + structured JSON output |

### 2.4 Module Implementation Sequence

Use this sequence to implement each module consistently from start to finish:

1. **entity/**
    - Define JPA models first (table mapping, relations, constraints).
2. **repository/**
    - Create `JpaRepository` interfaces to access entities.
3. **dto/**
    - Create request/response contracts + validation annotations.
4. **exception/**
    - Define module-specific business exceptions early.
5. **mapper/**
    - Add MapStruct/manual mappers between entity and DTO.
6. **service/** (interface)
    - Define use-case contracts (clean signatures).
7. **service/impl/**
    - Implement business logic, transactions, ownership checks, and exception throwing.
8. **controller/**
    - Expose endpoints, add `@Valid`, map to service methods, and return standard envelope.
9. **shared/config integration**
    - Wire needed cross-cutting pieces (security, global handler mappings, bean config).
10. **test/**
    - Unit tests for service logic first.
    - Controller/web tests next.
    - Integration tests last for core flows.
11. **cleanup & docs**
    - Remove stale `.gitkeep` files.
    - Add temporary notes (if any).
    - Run compile/tests before commit.

### 2.5 Spring Boot Config and Secret Management

Configuration and secret handling follows these rules:

- `application.yml` references `${ENV_VAR}` only for sensitive values.
- No fallback secret values are allowed in code or config (for example: no default DB password in `application.yml`).
- Profile-based non-secret configuration is kept in `application-*.yml` files (for example `application-dev.yml`, `application-prod.yml`).
- Secret values are provided externally by environment variables and/or a secret manager.

Planned future implementation:

- Local development: untracked `.env` with a tracked `.env.example` template.
- CI/CD: pipeline secret store injects runtime environment variables.
- Production: managed secret service (for example Vault / cloud secret manager) with rotation policy.

---

## 3. Tech Stack

### Core

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Build | Maven or Gradle |
| Boilerplate | Lombok |
| Mapping | MapStruct |

### Testing

| Type | Tool |
| --- | --- |
| Unit / service tests | JUnit 5 + Mockito |
| Integration tests | Spring Boot Test + Testcontainers |
| API tests | RestAssured |

### Later Phases

| Concern | Technology |
| --- | --- |
| Caching | Redis + Spring Cache |
| Messaging | Kafka or RabbitMQ |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Metrics | Prometheus + Grafana |
| Tracing | OpenTelemetry |

---

## 4. Data Model

### Core Tables

```
users
  id, email, password_hash, full_name, created_at, updated_at, active

roles / user_roles
  id, name / user_id, role_id

refresh_tokens
  id, user_id, token, expiry_date, revoked

accounts
  id, user_id, name, type (CASH|BANK|CREDIT|SAVINGS|INVESTMENT),
  currency, balance, initial_balance, created_at, updated_at

categories
  id, user_id (nullable for system), name, type (INCOME|EXPENSE), color, icon

transactions
  id, user_id, account_id, category_id, type (INCOME|EXPENSE|TRANSFER),
  amount, currency, description, transaction_date, created_at, updated_at

transfers
  id, transaction_id, from_account_id, to_account_id, amount

budgets
  id, user_id, category_id, amount, period (MONTHLY|YEARLY), start_date, end_date

recurring_transactions
  id, user_id, account_id, category_id, type, amount,
  frequency (DAILY|WEEKLY|MONTHLY|YEARLY), next_run_date, active

notifications
  id, user_id, type, title, message, read, created_at

audit_logs
  id, user_id, action, entity_type, entity_id, old_value, new_value, created_at

imports / import_rows
  id, user_id, file_name, status, total_rows, imported_rows, failed_rows, created_at
  id, import_id, row_number, raw_data, status, error_message
```

### Key Constraints

- All monetary values stored as `NUMERIC(19,4)` — never `FLOAT`.
- `account.balance` is always derived from `initial_balance + SUM(transactions)`. Direct mutation outside the transaction service is forbidden.
- `category.user_id` is `NULL` for system seed categories.
- Soft delete (`active = false`) on users and accounts; hard delete on transactions is allowed but audited.

---

## 5. API Design

### Base URL

```
/api/v1
```

### Standard Response Envelope

```json
{
  "success": true,
  "data": { },
  "error": null,
  "timestamp": "2026-05-08T10:30:00Z"
}
```

Error shape:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BUDGET_EXCEEDED",
    "message": "Monthly food budget has been exceeded.",
    "details": []
  },
  "timestamp": "2026-05-08T10:30:00Z"
}
```

### Endpoint Reference

**Auth**

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/auth/register` |  |
| POST | `/auth/login` | Returns JWT pair |
| POST | `/auth/refresh` | Rotates refresh token |
| POST | `/auth/logout` | Revokes refresh token |

**Users**

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/users/me` |  |
| PUT | `/users/me` |  |
| PUT | `/users/me/password` |  |

**Accounts / Categories / Transactions / Budgets** — standard CRUD at their respective paths. Transactions support full query params:

```
?from=&to=&type=&accountId=&categoryId=&minAmount=&maxAmount=&keyword=&page=&size=&sort=
```

**Budget status response:**

```json
{
  "category": "Food",
  "budgetAmount": 400.00,
  "spentAmount": 275.00,
  "remainingAmount": 125.00,
  "usagePercentage": 68.75,
  "status": "ON_TRACK"   // ON_TRACK | WARNING (≥80%) | EXCEEDED (>100%)
}
```

**Reports:** `GET /reports/monthly-summary`, `GET /reports/monthly-summary/export` (CSV), `/category-spending`, `/cash-flow`, `/net-worth`

**CSV Import:** `POST /imports/transactions/preview` → `POST /imports/transactions/confirm` → `GET /imports/transactions/{id}/status` (duplicate rows are marked `SKIPPED`)

Import status values: `PENDING | RUNNING | COMPLETED | FAILED`

Import row status values: `VALID | IMPORTED | FAILED | SKIPPED`

**Audit Logs:** `POST /audits` → `GET /audits` → `GET /audits/{id}`

---

## 6. Security Design

### Authentication Flow

```
Client                         Server
  │                               │
  ├──POST /auth/login ────────────►│ validate credentials
  │                               │ issue accessToken (15min) + refreshToken (7d)
  │◄── { accessToken, refreshToken } ──┤
  │                               │
  ├──GET /users/me ───────────────►│ validate JWT signature + expiry
  │  Authorization: Bearer <token> │
  │◄── user data ─────────────────┤
  │                               │
  ├──POST /auth/refresh ──────────►│ validate + rotate (old token revoked)
  │◄── { new accessToken } ───────┤
```

### JWT Claims

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1715000000,
  "exp": 1715000900
}
```

### Resource Ownership

Enforced at the **service layer** — not just the controller. A shared `ResourceOwnershipValidator` validates `currentUserId == resource.userId` uniformly across all modules.

---

## 7. Key Business Rules

| Rule | Where enforced |
| --- | --- |
| Transaction creation updates balance atomically | `@Transactional` on `TransactionService` |
| Transfer creates debit + credit in single boundary | Single `@Transactional` |
| Deleting a category used by transactions is blocked | Pre-delete check in `CategoryService` |
| Budget WARNING fires at ≥80% | `BudgetService.calculateStatus()` |
| CSV duplicate transactions are skipped | Hash of `(account_id, amount, date, description)` |
| Recurring transactions are idempotent | `next_run_date` checked before generation; distributed lock on scheduler |
| CSV import confirm is asynchronous | `ImportService.confirm()` sets `RUNNING`; `ImportAsyncJobService` processes in background |

---

## 8. Phased Delivery

| Phase | Scope |
| --- | --- |
| 0 | Spring Boot scaffold, PostgreSQL, Docker Compose, Flyway, error handler, Swagger (completed) |
| 1 | Auth: register, login, JWT, refresh tokens, roles, profile (completed) |
| 2 | Accounts: CRUD, ownership, balance tracking, pagination (completed) |
| 3 | Categories: seed data, custom categories, delete guard (completed) |
| 4 | Transactions: income/expense/transfer, balance updates, audit log (completed) |
| 5 | Budgets: monthly budgets, spending aggregation, threshold alerts (completed) |
| 6 | Reports: monthly summary, category spending, cash flow, net worth, CSV export (completed) |
| 7 | Recurring transactions: rules, Spring Scheduler, idempotency (completed) |
| 8 | Notifications: in-app, event-triggered, email, preferences (completed) |
| 9 | CSV import: upload, preview, validation, async job (completed) |
| 10 | Testing: unit + integration, Testcontainers, 70%+ coverage |
| 11 | Redis: caching, token blacklist, rate limiting |
| 12 | Event-driven: App Events → Kafka/RabbitMQ, retry, DLQ |
| 13 | Security hardening: lockout, token rotation, headers, CORS |
| 14 | Deployment: Docker, GitHub Actions, cloud, HTTPS |
| 15 | Observability: Actuator, Prometheus, Grafana, OpenTelemetry |
| 16 | Microservice extraction: auth, notification, report services + API Gateway |

---

## 9. MVP Acceptance Criteria

- [x]  All auth endpoints work with JWT enforcement
- [x]  Private routes return 401 when unauthenticated
- [x]  Account data is correctly scoped to the authenticated user
- [x]  Transaction creation updates account balance atomically
- [x]  Transfer correctly debits and credits in a single transaction boundary
- [x]  Budget status reflects spending percentage accurately
- [x]  CSV import preview/confirm/status flow works for authenticated users
- [x]  Audit log create/list/detail endpoints enforce user ownership
- [ ]  Monthly summary report returns correct aggregates
- [ ]  Flyway migrations run cleanly on a fresh database
- [x]  Core business logic covered by unit tests
- [ ]  App boots with `docker compose up`

---

## 10. Architecture Decision Records

| ADR | Decision | Rationale |
| --- | --- | --- |
| 001 | Modular monolith over microservices | Lower operational complexity early; clear extraction path later |
| 002 | PostgreSQL | ACID required for financial data; strong JSON support for audit logs |
| 003 | JWT with refresh token rotation | Stateless auth scales well; rotation limits token reuse attacks |
| 004 | Spring Application Events → Kafka | Decoupled from day one; swap transport without touching business logic |
| 005 | Report module as query aggregation layer (no persisted report tables) | Avoids duplicated financial data and sync drift; computes reports from source-of-truth tables with projections/DTOs |
| 006 | Recurring transactions in dedicated automation module with soft delete | Isolates automation concerns, enforces ownership/visibility checks, and preserves historical auditability by deactivating rules instead of hard delete |
| 007 | In-app notifications first, event/email channels incrementally | Delivers immediate user value with lower complexity, while keeping extension path for event-driven and email delivery |
| 008 | Flyway-first schema governance for persistent entities | Prevents entity/table drift and startup failures; every new table must be introduced through versioned migrations |
| 009 | Global exception mapping with stable API error codes | Ensures predictable client integration and consistent error envelopes across modules |

---

*SaveAPenny Technical Proposal — v1.0*
