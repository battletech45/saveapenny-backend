# Deployment and Operations

## Overview

SaveAPenny can be built locally as a Docker image and deployed behind a reverse proxy for TLS termination.

## Runtime Dependencies

| Dependency | Required | Notes |
|------------|----------|-------|
| Java 24 (JRE) | Yes | Included in the Docker image |
| PostgreSQL 16+ | Yes | All data storage |
| Tesseract | No | Needed only if OCR is enabled |
| OpenAI / OpenRouter | No | Needed only if assistant or insights are enabled |

## Environment Variables

### Required

| Variable | Description | Validation |
|----------|-------------|------------|
| `DB_USERNAME` | PostgreSQL application user | Must exist and have schema permissions |
| `DB_PASSWORD` | PostgreSQL application password | — |
| `JWT_SECRET` | HS512 signing key | Minimum 64 characters |

### Application Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP listen port |
| `SPRING_PROFILES_ACTIVE` | — | Active Spring profile(s) |
| `ASSISTANT_ENABLED` | `false` | Enable AI assistant chat |
| `INSIGHT_ENABLED` | `false` | Enable financial insight generation |
| `GOAL_PROGRESS_ENABLED` | `false` | Enable scheduled goal progress checks |
| `OCR_ENABLED` | `true` | Enable OCR receipt processing |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed CORS origins |

### Docker Compose Only

| Variable | Description |
|----------|-------------|
| `POSTGRES_DB` | Database name for the PostgreSQL container |
| `POSTGRES_USER` | PostgreSQL admin user |
| `POSTGRES_PASSWORD` | PostgreSQL admin password |

See [Environment Variables Reference](env-reference.md) for the complete list.

## Docker Image

Build the image locally using the included Dockerfile:

```bash
# Build using included Docker Compose
docker compose up --build

# Build manually
docker build -t save-a-penny .
```

The Dockerfile uses a multi-stage build:
1. **Builder stage**: Maven + Temurin 24 JDK — compiles the application
2. **Runtime stage**: Temurin 24 JRE + Tesseract OCR — runs the application

## Database

| Aspect | Detail |
|--------|--------|
| Engine | PostgreSQL 16+ |
| Migration | Flyway (classpath:db/migration, 19 migrations V1–V19) |
| Schema validation | Hibernate `ddl-auto: validate` |
| Default connection | `localhost:5432/saveapenny` |
| Migration files | `src/main/resources/db/migration/` |

Migrations are applied on startup. If the schema is behind the entities, the application fails to start with a validation error.

## Health Checks

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /actuator/health` | Public | Liveness and readiness probe |
| `GET /v3/api-docs` | Public | OpenAPI specification |
| `GET /swagger-ui.html` | Public | Interactive API browser |

## Docker Compose

The included `docker-compose.yml` starts PostgreSQL 16 and the application together. The health check polls `/actuator/health` and waits for the app to be ready.

```bash
cp .env.example .env   # fill in values
docker compose up --build
```

The compose file builds the image locally from the Dockerfile on startup.

## Reverse Proxy Recommendations

In production, place the service behind a reverse proxy that handles:

| Concern | Recommended Approach |
|---------|---------------------|
| **TLS termination** | Let's Encrypt via cert-manager, Caddy, or nginx |
| **Host-based routing** | Path or host-based routing to the app container |
| **DDoS protection** | Cloudflare, AWS WAF, or rate limiting at proxy level |
| **Request logging** | Structured logging at proxy level, forwarded to aggregator |
| **Security headers** | Configure HSTS, CSP, etc. at both proxy and application layer |

The application does not handle TLS natively — all TLS concerns are delegated to the proxy.

## Monitoring

| Capability | Source | Notes |
|------------|--------|-------|
| Health | `GET /actuator/health` | Liveness probe, includes DB health check |
| JVM metrics | `/actuator/metrics` | Enabled and exposed by default |
| Logs | `logging.level` in `application.yml` | Configured via environment or config file |

## Deployment Smoke Checks

After deploying, verify the following sequence:

1. `GET /actuator/health` returns `200 UP`
2. `POST /api/v1/auth/register` returns tokens
3. `POST /api/v1/auth/login` returns tokens
4. Protected endpoint returns `200` with valid access token
5. `POST /api/v1/auth/refresh` returns new tokens
6. Startup logs confirm `Successfully validated N migrations`

## Troubleshooting

### Application fails to start

Check startup logs for:

| Symptom | Likely Cause | Resolution |
|---------|-------------|------------|
| Database connection refused | PostgreSQL not running or credentials wrong | Verify `DB_USERNAME`/`DB_PASSWORD` and database host |
| Schema validation error | Flyway migrations not applied | Run all migrations or repair schema |
| JWT secret too short | `JWT_SECRET` < 64 characters | Generate a 64+ char key: `openssl rand -base64 64` |
| Tesseract not found | OCR enabled but Tesseract not installed | Install Tesseract or disable OCR |

### 401 on all endpoints

| Cause | Symptom | Resolution |
|-------|---------|------------|
| Expired access token | `ACCESS_DENIED` | Refresh via `/auth/refresh` or re-authenticate |
| Revoked refresh token | `INVALID_REFRESH_TOKEN` | Re-authenticate via login |
| Missing `Authorization` header | `ACCESS_DENIED` | Include `Authorization: Bearer <token>` in all requests |

### 429 Too Many Requests

| Scenario | Limit | Resolution |
|----------|-------|------------|
| Login rate limited | 5 POST/min per IP | Wait 60 seconds (check `Retry-After` header) |
| API rate limited | 60 POST/min per user | Reduce request frequency, respect `Retry-After` |

## Related Documents

- [Getting Started](getting-started.md) — Local development setup
- [Architecture](architecture.md) — System architecture and module structure
- [Security](security.md) — Authentication, headers, rate limiting
- [Environment Variables Reference](env-reference.md) — Complete env var catalogue
