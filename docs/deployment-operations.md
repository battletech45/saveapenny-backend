# Deployment And Operations

## Runtime Dependencies

| Dependency | Required | Notes |
|------------|----------|-------|
| Java 24 | Yes | JVM runtime |
| PostgreSQL 16+ | Yes | All data storage |
| Tesseract | No | Needed only if OCR is enabled |
| OpenAI / OpenRouter | No | Needed only if assistant is enabled |

## Environment Variables

### Required

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | PostgreSQL user |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | HS512 key, 64+ characters |

### Application Configuration (all have safe defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP listen port |
| `ASSISTANT_ENABLED` | `false` | Enable AI assistant chat |
| `ASSISTANT_AI_PROVIDER` | `openrouter` | `openrouter` or `openai` |
| `OPENROUTER_API_KEY` | — | OpenRouter API key |
| `OPENAI_API_KEY` | — | OpenAI API key |
| `INSIGHT_ENABLED` | `false` | Enable financial insight generation |
| `GOAL_PROGRESS_ENABLED` | `false` | Enable scheduled goal progress checks |

### Docker Compose Only

| Variable | Description |
|----------|-------------|
| `POSTGRES_DB` | Database name |
| `POSTGRES_USER` | PostgreSQL admin user |
| `POSTGRES_PASSWORD` | PostgreSQL admin password |

## Database

- Flyway applies migrations on startup
- Hibernate validates schema against entities (`ddl-auto: validate`)
- Default connection: `localhost:5432/saveapenny`
- Migrations are in `src/main/resources/db/migration/`

## Health Checks

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /actuator/health` | Public | Liveness and readiness |
| `GET /v3/api-docs` | Public | OpenAPI spec |
| `GET /swagger-ui.html` | Public | Interactive API browser |

## Docker Compose

The included `docker-compose.yml` starts PostgreSQL 16 + the app. Health check polls `/actuator/health`.

To run:

```bash
cp .env.example .env   # fill in values
docker compose up --build
```

## Reverse Proxy Recommendations

In production, place the service behind a reverse proxy (nginx, Caddy, Cloudflare, or a Kubernetes ingress) that handles:

- TLS termination
- Host-based routing
- DDoS protection
- Request logging

The application does not handle TLS natively.

## Security

- All business endpoints require `Authorization: Bearer <accessToken>`
- Auth, health, and docs endpoints are public
- Resources are scoped per authenticated user
- Rate limiting: 5 POST/min for login, 60 POST/min for API (other methods are not rate-limited)
- Tokens are stored hashed (refresh tokens) or signed (access tokens via JWT)

## Monitoring

| Metric | Source |
|--------|--------|
| Health | `/actuator/health` |
| JVM metrics | `/actuator/metrics` (if enabled) |
| Logs | Configured via `logging.level` in `application.yml` |

## Deployment Smoke Checks

1. `GET /actuator/health` returns `200 UP`
2. `POST /api/v1/auth/register` succeeds
3. `POST /api/v1/auth/login` returns tokens
4. Call a protected endpoint with the access token
5. `POST /api/v1/auth/refresh` returns new tokens
6. Flyway migrations applied (check startup logs for `Successfully validated N migrations`)

## Troubleshooting

### App fails to start

Check startup logs for:

- **Database connection**: verify PostgreSQL is running and `DB_USERNAME`/`DB_PASSWORD` are correct
- **Flyway migration**: `Schema-validation: missing column` — the database schema is behind the code. Run all migrations or repair the schema
- **JWT secret**: ensure `JWT_SECRET` is at least 64 characters for HS512
- **OCR validation**: if OCR is enabled, Tesseract must be installed and tessdata path must exist

### 401 on all endpoints

- The access token is expired or invalid
- The refresh token was revoked (e.g., by a password change on another device)
- Solution: re-authenticate via login

### 429 Too Many Requests

- Login rate limit: 5 POST requests per minute per IP
- API rate limit: 60 POST requests per minute per user
- Response includes `Retry-After: 60` header
- Only POST requests are rate-limited
