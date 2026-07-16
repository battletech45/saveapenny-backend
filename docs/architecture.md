# Architecture

## Overview

SaveAPenny is a Spring Boot 4.1 backend API. Core domain modules follow a layered architecture (controller → service → repository → database), while the OCR module employs hexagonal architecture. The application is organized by domain module. The architecture prioritizes data isolation (user-scoped resources), stateless authentication via dual tokens, and optional AI-powered features that do not affect the core financial domain.

## Module Structure

```
com.saveapenny/
├── SaveAPennyApplication.java        # @SpringBootApplication entry point
├── config/                            # Shared configuration
│   ├── CorsConfig.java                # CORS configuration
│   ├── CorsProperties.java            # CORS properties
│   ├── OpenApiConfig.java             # Swagger/OpenAPI setup
│   ├── AsyncConfig.java               # Async task executor
│   ├── OcrProperties.java             # OCR configuration
│   ├── SecurityBeansConfig.java       # PasswordEncoder, etc.
│   └── security/                      # Auth filter chain, rate limiting
│       ├── SecurityConfig.java        # SecurityFilterChain
│       ├── HeaderUserAuthenticationFilter.java
│       ├── RateLimiter.java
│       ├── RateLimitingFilter.java
│       ├── RateLimitProperties.java
│       └── CurrentUserPrincipal.java
├── auth/                              # Registration, login, token lifecycle
├── user/                              # Profile, password change
├── account/                           # Financial accounts
├── category/                          # System + user categories
├── transaction/                       # Income, expense, transfers
├── budget/                            # Monthly/yearly budgets
├── automation/                        # Recurring transactions scheduler
├── report/                            # Monthly summaries, net worth
├── imports/                           # CSV import with preview/confirm
├── ocr/                               # Tesseract-based receipt OCR (hexagonal: domain/application/infrastructure/interfaces/support)
├── stock/                             # Read-through Alpha Vantage market data
├── stockholding/                      # User stock holdings CRUD + portfolio summary
├── notification/                      # User-facing notifications
├── audit/                             # Entity change tracking
├── assistant/                         # AI chat ("Penny Dog")
├── insight/                           # Automated financial insights (includes analytics/, scheduler/, config/ sub-packages)
├── goal/                              # Financial goal simulation
├── mcp/                               # MCP tool infrastructure
├── analytics/                         # Firebase/GA4 Measurement Protocol event publishing (not to be confused with insight/analytics/ above, which is unrelated anomaly-detection logic)
├── admin/                             # Admin-only metrics endpoint
└── shared/                            # ApiResponse, GlobalExceptionHandler
    ├── api/                           # ApiResponse / ApiError envelope
    └── exception/                     # GlobalExceptionHandler
```

## Module Internal Structure

Each domain module follows a consistent pattern:

```
<module>/
├── config/              # Module-specific configuration
├── controller/          # REST controllers
├── dto/                 # Request/response DTOs
├── entity/              # JPA entities
├── exception/           # Module-specific exceptions
├── mapper/              # MapStruct mappers (compile-time)
├── repository/          # Spring Data JPA repositories
└── service/
    ├── <Module>Service.java       # Interface
    └── impl/<Module>ServiceImpl.java  # Implementation
```

## Request Lifecycle

```
                        ┌─────────────────────────┐
                        │       Client            │
                        │  (Mobile / Web / CLI)   │
                        └───────────┬─────────────┘
                                    │ POST /api/v1/...
                                    │ Authorization: Bearer <jwt>
                                    ▼
              ┌───────────────────────────────────────────┐
              │           SecurityFilterChain              │
              │                                            │
              │  ┌─────────────────────────────────────┐   │
              │  │ HeaderUserAuthenticationFilter      │   │
              │  │  - Extracts JWT from Bearer header   │   │
              │  │  - Validates signature via JwtService│   │
              │  │  - Sets CurrentUserPrincipal         │   │
              │  └────────────────┬────────────────────┘   │
              │                   │                        │
              │  ┌────────────────▼────────────────────┐   │
              │  │ RateLimitingFilter (POST only)      │   │
              │  │  - Token bucket per client           │   │
              │  │  - Returns 429 if exhausted          │   │
              │  └────────────────┬────────────────────┘   │
              │                   │                        │
              │  ┌────────────────▼────────────────────┐   │
              │  │ AnonymousAuthenticationFilter       │   │
              │  └────────────────┬────────────────────┘   │
              └───────────────────┼───────────────────────┘
                                  │
                                  ▼
              ┌───────────────────────────────────────────┐
              │         DispatcherServlet                  │
              └───────────────────┬───────────────────────┘
                                  │
                                  ▼
              ┌───────────────────────────────────────────┐
              │    Controller (@RestController)            │
              │    - @Valid request body validation        │
              │    - Returns ApiResponse<T> envelope       │
              └───────────────────┬───────────────────────┘
                                  │
                                  ▼
              ┌───────────────────────────────────────────┐
              │    Service (@Service, @Transactional)      │
              │    - Business logic                        │
              │    - User-scoped queries (user_id filter)  │
              └───────────────────┬───────────────────────┘
                                  │
                                  ▼
              ┌───────────────────────────────────────────┐
              │    Repository (Spring Data JPA)            │
              │    - JPA/Hibernate                         │
              │    - User-scoped WHERE clauses             │
              └───────────────────┬───────────────────────┘
                                  │
                                  ▼
              ┌───────────────────────────────────────────┐
              │    PostgreSQL 16+                          │
              │    - Flyway-managed schema                 │
              │    - ddl-auto: validate                    │
              └───────────────────────────────────────────┘
```

## Response Envelope

All responses use a consistent envelope via `ApiResponse<T>`:

Success:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-06-10T12:00:00Z"
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": []
  },
  "timestamp": "2026-06-10T12:00:00Z"
}
```

See [Error Codes](error-codes.md) for the full error catalogue.

## Authentication Model

```
┌──────────────┐         ┌──────────────────┐         ┌──────────────┐
│  Login/      │ ──────▶ │  Token Pair      │ ──────▶ │  API Access  │
│  Register    │         │  ├─ JWT (15 min)  │         │  (Bearer)    │
│              │         │  └─ Refresh(7d)   │         │              │
└──────────────┘         └──────────────────┘         └──────────────┘
                               │
                               │ Refresh (rotate)
                               ▼
                         ┌──────────────────┐
                         │  New Token Pair  │
                         │  Old refresh     │
                         │  revoked         │
                         └──────────────────┘
```

| Token Type | Format | Expiry | Revocable | Stored Server-Side |
|------------|--------|--------|-----------|-------------------|
| Access token | JWT (HS512-signed) | 15 minutes | No | No (stateless) |
| Refresh token | Opaque Base64URL string | 7 days | Yes | Stored in DB |

## Database

| Property | Value |
|----------|-------|
| Engine | PostgreSQL 16+ |
| Migration | Flyway (version-controlled, classpath:`db/migration`) |
| Schema validation | Hibernate `ddl-auto: validate` (catches drift at startup) |
| Data isolation | Every user-data table has a `user_id` column |
| Soft deletes | Accounts, categories (name stays reserved) |
| Open-in-view | Disabled (`spring.jpa.open-in-view: false`) |

## CORS

| Property | Default | Configurable |
|----------|---------|-------------|
| Allowed origins | `http://localhost:3000` | `cors.allowed-origins` |
| Methods | GET, POST, PUT, PATCH, DELETE, OPTIONS | — |
| Headers | Authorization, Content-Type, Accept, Origin, X-Requested-With, Cache-Control, Pragma | — |
| Credentials | Allowed | — |
| Max age | 3600s | — |

## Key Design Decisions

### Dual tokens (JWT + opaque refresh)

**Decision:** JWT for stateless access, opaque refresh tokens for secure rotation.

**Why not a single long-lived JWT?** A stolen long-lived JWT cannot be revoked. By keeping the JWT short (15 min) and rotating refresh tokens on each use, theft is detected quickly and the damage window is limited.

**Why not opaque tokens for both?** Each API call would require a database lookup to validate the token. JWT is stateless — no DB hit on the hot path.

### MapStruct over manual mapping

**Decision:** Compile-time DTO mapping via MapStruct.

**Alternatives considered:**
- **Manual mapping** — Verbose, error-prone, no compiler safety for field changes.
- **ModelMapper** — Runtime reflection, slower, masking errors with silent no-ops when field names change.
- **Lombok `@Builder` with `toBuilder`** — Works for simple cases but no cross-type mapping.

MapStruct generates code at compile time with zero runtime overhead and explicit mapping contracts.

### Flyway + `ddl-auto: validate`

**Decision:** Schema migrations managed by Flyway; Hibernate validates schema against entities at startup.

**Why not `ddl-auto: update`?** In production, automatic DDL changes are dangerous — drop columns can silently delete data. Flyway gives version-controlled, reviewed migrations. `validate` catches drift between entities and the actual schema before the app serves traffic.

### In-memory rate limiting

**Decision:** Token bucket backed by `ConcurrentHashMap`.

**Why not Redis?** Zero infrastructure dependency. Single-node deployments don't need distributed state. If horizontal scaling is required, the token bucket algorithm maps cleanly to Redis sorted sets.

**Limitation:** State resets on server restart. Idle keys accumulate until restart (no eviction).

### Feature flags for optional capabilities

**Decision:** Optional capabilities are enabled or disabled via properties, with conservative defaults for AI-driven and scheduled jobs.

**Rationale:** External API keys (OpenAI, OpenRouter) should not be required for the core financial domain. Each optional feature has a clear enablement path with documented prerequisites.

## Feature Enablement

| Feature | Default | Property/Env |
|---------|---------|-------------|
| OCR (Tesseract receipt scanning) | Enabled | `ocr.enabled` |
| AI Assistant ("Penny Dog") | Disabled | `ASSISTANT_ENABLED=true` |
| Financial Insights | Disabled | `insight.enabled` |
| Goal Progress Checks | Disabled | `goal.progress.enabled` |
| Stock Market Data | Enabled | `STOCK_ENABLED=true` |

## Troubleshooting

### Application fails to start

| Symptom | Likely Cause | Resolution |
|---------|-------------|------------|
| Schema validation error | Flyway migrations not applied | Apply all migrations or repair schema |
| Database connection refused | PostgreSQL not running or wrong credentials | Verify `DB_USERNAME`/`DB_PASSWORD` |
| JWT secret too short | Secret < 64 characters | Generate 64+ char key: `openssl rand -base64 64` |
| Tesseract not found | OCR enabled but Tesseract not installed | Install Tesseract or disable OCR |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/resources/application.yml` | Main configuration (datasource, security, rate limits, features) |
| `src/main/resources/application-local.yml` | Local dev overrides |
| `docker-compose.yml` | Local development stack (PostgreSQL + app) |
| `Dockerfile` | Multi-stage build (Maven → JRE 24) |
| `pom.xml` | Maven build with Spring Boot 4.1, Java 24 |

## Related Documents

- [Auth Flow](auth-flow.md) — Token lifecycle and mobile client implementation
- [Security](security.md) — Headers, rate limiting, authentication details
- [Rate Limiting](rate-limiting.md) — Token bucket algorithm and client best practices
- [Deployment & Operations](deployment-operations.md) — Production deployment configuration
- [Error Codes](error-codes.md) — Complete error catalogue
