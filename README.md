# SaveAPenny
Users can track accounts, transactions, budgets, spending patterns, recurring payments, alerts, and eventually investment/analytics features.

## Auth status
Auth endpoints are available under `/api/v1/auth`:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Protected endpoints require `Authorization: Bearer <accessToken>`.

## Configuration
Set the following environment variables before running the app:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (a strong secret key, at least 64 characters for HS512)

## Test commands

- Run full test suite: `mvn test`
- Run auth integration flow only: `mvn -Dtest=AuthFlowIntegrationTest test`
