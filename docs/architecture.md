# Architecture

## Overview

SaveAPenny is a standard Spring Boot 3 application with a layered architecture: controller → service → repository → database. The application is organized by domain module.

## Module Structure

```
com.saveapenny
├── SaveAPennyApplication.java        # Entry point
├── config/                            # Shared configuration
│   ├── security/                      # Auth filter chain, rate limiting
│   ├── CorsConfig.java                # CORS configuration
│   ├── CorsProperties.java            # CORS properties
│   ├── OcrProperties.java             # OCR configuration
│   ├── OpenApiConfig.java             # Swagger/OpenAPI setup
│   ├── AsyncConfig.java               # Async task executor
│   └── SecurityBeansConfig.java        # Security beans
├── auth/                              # Authentication and sessions
├── user/                              # User management
├── account/                           # Accounts
├── category/                          # Categories
├── transaction/                       # Transactions and transfers
├── budget/                            # Budgets
├── automation/                        # Recurring transactions
├── report/                            # Financial reports
├── imports/                           # CSV transaction import
├── ocr/                               # OCR receipt processing
├── notification/                      # Notifications
├── audit/                             # Audit history
├── assistant/                         # AI assistant chat
├── insight/                           # Financial insights
├── goal/                              # Goal tracking and simulation
└── shared/                            # Shared DTOs, exceptions, utilities
    ├── api/                           # ApiResponse envelope
    └── exception/                     # GlobalExceptionHandler
```

## Module Dependencies

Each domain module follows this internal structure:

```
<module>/
├── config/              # Module-specific configuration
├── controller/          # REST controllers
├── dto/                 # Request/response DTOs
├── entity/              # JPA entities
├── exception/           # Module-specific exceptions
├── mapper/              # MapStruct mappers
├── repository/          # Spring Data JPA repositories
└── service/
    ├── <Module>Service.java       # Service interface
    └── impl/<Module>ServiceImpl.java  # Implementation
```

## Request Lifecycle

```
Client
  │
  ▼
SecurityFilterChain
  ├── RateLimitingFilter (POST only)
  ├── HeaderUserAuthenticationFilter (JWT validation)
  └── AnonymousAuthenticationFilter
  │
  ▼
DispatcherServlet
  │
  ▼
Controller (@RestController)
  │
  ▼
Service (@Service, @Transactional)
  │
  ▼
Repository (Spring Data JPA)
  │
  ▼
Database (PostgreSQL)
```

## Response Envelope

All responses use a consistent envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-06-10T12:00:00+03:00"
}
```

See [Error Codes](error-codes.md) for error formats.

## Authentication

- **Access tokens**: JWTs signed with HS512, 15 min expiry
- **Refresh tokens**: Opaque UUIDs stored as bcrypt hashes, 7 day expiry
- Both tokens are returned on login/register
- Refresh token is rotated on each use (old token revoked, new token issued)
- Password change revokes all refresh tokens for the user

See [Auth Flow](auth-flow.md) for details.

## Database

- PostgreSQL 16+
- Schema managed by Flyway migrations
- Hibernate validates schema on startup (`ddl-auto: validate`)
- All user-data tables have a `user_id` column for data isolation
- Soft deletes used where applicable (accounts, categories)

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Dual tokens (JWT + opaque) | JWT for fast stateless auth, opaque refresh for secure rotation |
| Flyway for migrations | Version-controlled, repeatable schema changes |
| `ddl-auto: validate` | Catches entity/migration drift at startup |
| MapStruct for mappers | Compile-time, no reflection overhead |
| In-memory rate limiting | Simple, sufficient for single-node MVP |
| H2 for integration tests | Fast, isolated, PostgreSQL-compatible mode |
