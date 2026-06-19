# Getting Started

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 24 (JDK) | Required |
| Maven | 3.9+ | Build tool |
| PostgreSQL | 16+ | Database |
| Docker | Latest | Recommended (Docker Compose for local dev) |
| Tesseract | Latest | Optional, needed only if OCR is enabled |

## Quick Start (Docker Compose)

### 1. Clone and configure

```bash
git clone <repo-url> && cd save-a-penny
cp .env.example .env
```

Edit `.env` with the minimum required values:

```env
POSTGRES_DB=saveapenny
POSTGRES_USER=saveapenny_app
POSTGRES_PASSWORD=change_me_local_only
DB_USERNAME=saveapenny_app
DB_PASSWORD=change_me_local_only
JWT_SECRET=change_me_to_a_64_plus_char_secret_for_hs512_signing_key
```

### 2. Start the stack

```bash
docker compose up --build
```

This starts PostgreSQL 16 and the application. The app is available at `http://localhost:8080`.

### 3. Verify startup

Open these URLs to confirm the application is running:

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/actuator/health` | Health check (expected: `{"status":"UP"}`) |
| `http://localhost:8080/swagger-ui.html` | Interactive API browser |
| `http://localhost:8080/v3/api-docs` | OpenAPI specification |

### 4. Register and authenticate

```bash
# Register a new user
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"StrongPass123!","fullName":"Demo User"}'

# Log in
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"StrongPass123!"}'
```

Save the `accessToken` and `refreshToken` from the response.

### 5. Create an account and transaction

```bash
# Create an account
curl -X POST "http://localhost:8080/api/v1/accounts" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Main Account","type":"BANK","currency":"USD","initialBalance":1000.00}'

# Record a transaction
curl -X POST "http://localhost:8080/api/v1/transactions" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<accountId>","categoryId":"<categoryId>","type":"EXPENSE","amount":25.00,"currency":"USD","description":"Lunch"}'
```

## Running Without Docker

Start PostgreSQL separately, then:

```bash
mvn spring-boot:run
```

The application expects a PostgreSQL database named `saveapenny` on `localhost:5432` with the credentials configured in `.env`.

## Running Tests

```bash
# Full test suite
mvn test

# Single test class
mvn -Dtest=AuthFlowIntegrationTest test

# Single test method
mvn -Dtest=AuthFlowIntegrationTest#loginThenRefresh_thenOldTokenFails test
```

See [Testing Guide](testing-guide.md) for the complete test architecture.

## Smoke Test Checklist

1. `GET /actuator/health` returns `200 UP`
2. `POST /api/v1/auth/register` returns tokens
3. `POST /api/v1/auth/login` returns tokens
4. `POST /api/v1/auth/refresh` returns new tokens
5. Protected endpoints return `200` with `Authorization: Bearer <accessToken>`
6. Invalid JWT returns `401 ACCESS_DENIED`
7. Rate-limited path returns `429 RATE_LIMITED` after exceeding limit

## Next Steps

- [Usage Guide](usage-guide.md) — Common workflows and API usage patterns
- [API Reference](api-reference.md) — Complete endpoint listing
- [Auth Flow](auth-flow.md) — Token lifecycle and mobile client implementation
- [Architecture](architecture.md) — System architecture and module structure
- [Deployment & Operations](deployment-operations.md) — Production deployment configuration

## Referenced Files

| File | Purpose |
|------|---------|
| `.env.example` | Environment variable template |
| `docker-compose.yml` | Local development stack |
| `Dockerfile` | Multi-stage production build |
| `pom.xml` | Maven build configuration |
