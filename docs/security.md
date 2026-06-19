# Security Overview

## Overview

SaveAPenny implements defense-in-depth security across authentication, authorization, transport, and deployment layers. The application is designed to be deployed behind a reverse proxy for TLS termination and security headers.

| Layer | Mechanism | Status |
|-------|-----------|--------|
| Authentication | Dual-token (JWT access + opaque refresh) | Implemented |
| Authorization | User-scoped data, `@EnableMethodSecurity` | Implemented |
| Password hashing | BCrypt via Spring Security `PasswordEncoder` | Implemented |
| Input validation | `spring-boot-starter-validation` (`@Valid`) | Implemented |
| Rate limiting | Token bucket per client | Implemented |
| Security headers | HSTS, CSP, X-Frame-Options, etc. | Delegated to reverse proxy |
| CORS | Configurable origin whitelist | Implemented |
| TLS termination | Deferred to reverse proxy (nginx, Caddy, ingress) | Not in app |
| Vulnerability scanning | Dependabot, CodeQL | CI-integrated |

## Authentication

The application uses a dual-token system:

| Property | Access Token | Refresh Token |
|----------|-------------|---------------|
| Format | JWT (HS512-signed) | Opaque UUID (v4) |
| Storage | Client-side only | Bcrypt hash in database |
| Expiry | 15 minutes | 7 days |
| Rotation | Not applicable | Rotated on each refresh |
| Revocation | Not possible (stateless) | Immediate on logout or password change |

See [Auth Flow](auth-flow.md) for the full lifecycle.

## Authorization

- All business endpoints require a valid JWT in the `Authorization: Bearer` header
- Resources are scoped to the authenticated user via `user_id` on every table
- Auth, health, and docs endpoints are explicitly permitted
- `@EnableMethodSecurity` is configured for method-level access control
- Invalid or missing tokens return `401` with structured JSON error

## Password Security

| Measure | Detail |
|---------|--------|
| Algorithm | BCrypt via Spring Security `PasswordEncoder` |
| Plaintext storage | Never |
| Password change | Revokes all active refresh tokens |
| Reuse prevention | `PasswordReuseNotAllowedException` |
| Validation | `StrongPasswordValidator` enforces minimum requirements |

## Security Headers

Security headers (HSTS, CSP, X-Frame-Options, etc.) are delegated to the reverse proxy layer. Configure them in nginx, Caddy, or your ingress controller. See [Deployment & Operations](deployment-operations.md) for reverse proxy recommendations.

## Rate Limiting

| Bucket | Limit | Scope | Key |
|--------|-------|-------|-----|
| Login | 5 POST/min | Per IP | Client IP |
| General API | 60 POST/min | Per user | User ID (falls back to IP for anonymous) |

Only POST requests to `/api/v1/*` paths are rate-limited. Rate-limited responses return `429 Too Many Requests` with a `Retry-After` header and a structured JSON error body.

See [Rate Limiting](rate-limiting.md) for details.

## CORS

| Property | Default | Configurable |
|----------|---------|-------------|
| Allowed origins | `http://localhost:3000` | `cors.allowed-origins` |

CORS is configurable via the `cors.allowed-origins` property. When set to an empty list, CORS headers are not added (useful when the API is consumed by a same-origin frontend or mobile client).

## Data Protection

- All user data is stored in PostgreSQL with `user_id` column for isolation
- No PII is logged
- Database credentials are configured via environment variables, never hardcoded
- JWT secret must be at least 64 characters and kept confidential
- Refresh tokens are stored as bcrypt hashes (never plaintext)

## Transport Security

The application does not handle TLS natively. Deploy behind a reverse proxy (nginx, Caddy, Kubernetes ingress) for TLS termination. See [Deployment & Operations](deployment-operations.md) for production guidance.

## Vulnerability Scanning

| Tool | Schedule | Scope |
|------|----------|-------|
| Dependabot | Weekly | Maven dependencies + GitHub Actions |
| CodeQL | Weekly + on PR | Java code security analysis |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Security headers delegated to reverse proxy | TLS and headers are handled more efficiently at the proxy layer; consistent with transport security separation |
| In-memory rate limiter | No infrastructure dependency; sufficient for single-node deployments |
| No native TLS | Separates transport security concerns; reverse proxy handles it more efficiently |
| BCrypt over scrypt/argon2 | Battle-tested, Spring Security built-in, sufficient for API auth |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/config/security/SecurityConfig.java` | HTTP security chain configuration |
| `src/main/java/com/saveapenny/config/security/HeaderUserAuthenticationFilter.java` | JWT extraction and validation |
| `src/main/java/com/saveapenny/config/security/RateLimiter.java` | Token bucket rate limiter |
| `src/main/java/com/saveapenny/config/security/RateLimitingFilter.java` | Rate limiting HTTP filter |
| `src/main/resources/application.yml` | Security header, CORS, rate limit configuration |
