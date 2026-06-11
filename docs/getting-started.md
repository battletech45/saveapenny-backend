# Getting Started

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 24 | JDK required |
| Maven | 3.9+ | Build tool |
| PostgreSQL | 16+ | Database |
| Docker | Latest | Optional, for Docker Compose |
| Tesseract | Latest | Optional, only if OCR is enabled |

## 1. Configure Environment

Copy the example env file and fill in the values:

```bash
cp .env.example .env
```

Minimum required values in `.env`:

```env
POSTGRES_DB=saveapenny
POSTGRES_USER=saveapenny_app
POSTGRES_PASSWORD=change_me_local_only
DB_USERNAME=saveapenny_app
DB_PASSWORD=change_me_local_only
JWT_SECRET=change_me_to_a_64_plus_char_secret_for_hs512_signing_key
```

## 2. Start the Application

### Option A: Docker Compose

```bash
docker compose up --build
```

Starts PostgreSQL and the application together. Exposes app on `:8080` and PostgreSQL on `:5432`.

### Option B: Maven (PostgreSQL must be running separately)

```bash
mvn spring-boot:run
```

## 3. Verify Startup

Open these URLs after the app starts:

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Interactive API browser |
| `http://localhost:8080/v3/api-docs` | OpenAPI spec (JSON) |
| `http://localhost:8080/actuator/health` | Health check |

Expected health response:

```json
{
  "status": "UP"
}
```

## 4. Smoke Test

### Register a user

```bash
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "StrongPass123!",
    "fullName": "Demo User"
  }'
```

### Log in

```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "StrongPass123!"
  }'
```

Save the `accessToken` and `refreshToken` from the response.

### Create an account

```bash
curl -X POST "http://localhost:8080/api/v1/accounts" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Main Account",
    "type": "BANK",
    "currency": "USD",
    "initialBalance": 1000.00
  }'
```

### Refresh tokens

```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refreshToken>"
  }'
```

## Running Tests

```bash
# Full suite
mvn test

# Single test
mvn -Dtest=TransactionFlowIntegrationTest test
```

See [Testing Guide](testing-guide.md) for details.

## Next Steps

- [Usage Guide](usage-guide.md) — common workflows
- [API Reference](api-reference.md) — endpoint list
- [Auth Flow](auth-flow.md) — token lifecycle for mobile clients
- [Deployment & Operations](deployment-operations.md) — production setup
