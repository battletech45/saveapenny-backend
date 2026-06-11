# Security Overview

## Authentication

SaveAPenny uses a dual-token authentication system:

- **Access tokens**: short-lived JWTs (15 min) signed with HS512
- **Refresh tokens**: opaque, stored as bcrypt hashes in the database

See [Auth Flow](auth-flow.md) for the full lifecycle.

## Authorization

- All business endpoints require a valid JWT access token in the `Authorization: Bearer` header
- Resources are scoped to the authenticated user — users cannot access each other's data
- Public endpoints (auth, health, docs) are explicitly permitted

## Password Security

- Passwords are hashed with BCrypt via Spring Security's `PasswordEncoder`
- No plaintext password storage
- Password change revokes all active refresh tokens

## Token Security

| Property | Access Token | Refresh Token |
|----------|-------------|---------------|
| Format | JWT (signed) | Opaque UUID |
| Storage | Client-side | Hashed in database |
| Expiry | 15 minutes | 7 days |
| Rotation | Not applicable | Rotated on each refresh |
| Revocation | Not possible | Immediate on logout or password change |

## Rate Limiting

- Login: 5 POST requests per minute per IP
- API: 60 POST requests per minute per user
- HTTP 429 with `Retry-After` header on limit

See [Rate Limiting](rate-limiting.md) for details.

## Data Protection

- All data is stored in PostgreSQL with user-level scoping (every table has a `user_id` column)
- No PII is logged
- Database credentials are configured via environment variables, never hardcoded
- JWT secret must be at least 64 characters and kept confidential

## Transport Security

- The application does not handle TLS natively
- Deploy behind a reverse proxy (nginx, Caddy, ingress) for TLS termination
- See [Deployment & Operations](deployment-operations.md) for production deployment guidance

## Security Headers

The application does not currently add security headers (HSTS, CSP, X-Frame-Options). These should be configured at the reverse proxy layer in production.
