# SaveAPenny

Personal finance API. Spring Boot backend for budgeting, transaction tracking, imports, and financial reports.

## Quick Start

### Docker Compose

```bash
cp .env.example .env   # fill in values
docker compose up --build
```

`docker compose` is the recommended way to run the app because it keeps PostgreSQL and application settings together and waits for the database health check before starting the app.

### Docker Run

Use this only if you want to start the database and app containers manually.

Create a shared Docker network first:

```bash
docker network create saveapenny-net
```

Start PostgreSQL:

```bash
docker run -d \
  --name saveapenny-postgres \
  --network saveapenny-net \
  -e POSTGRES_DB="saveapenny" \
  -e POSTGRES_USER="postgres" \
  -e POSTGRES_PASSWORD="postgres" \
  -p 5432:5432 \
  postgres:16
```

Wait until PostgreSQL is ready:

```bash
docker logs saveapenny-postgres
```

Build and start the app locally:

```bash
docker build -t save-a-penny .
docker run -d \
  --name saveapenny-app \
  --network saveapenny-net \
  -p 8080:8080 \
  -e JWT_SECRET="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="postgres" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://saveapenny-postgres:5432/saveapenny" \
  save-a-penny
```

The sample `JWT_SECRET` and database credentials are for local development only.

App starts at `http://localhost:8080`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

### Local

Requirements: Java 24, Maven 3.9+, PostgreSQL 16+.

Before starting the app locally, make sure PostgreSQL is running and export the required environment variables:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/saveapenny
```

```bash
mvn spring-boot:run
```

## Configuration

### Required For The Application

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | Application database user |
| `DB_PASSWORD` | Application database password |
| `JWT_SECRET` | HS512 key (64+ characters) |

### Required For Docker PostgreSQL

| Variable | Description |
|----------|-------------|
| `POSTGRES_DB` | PostgreSQL database name |
| `POSTGRES_USER` | PostgreSQL admin user |
| `POSTGRES_PASSWORD` | PostgreSQL admin password |

### Application (all have safe defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP listen port |
| `ASSISTANT_ENABLED` | `false` | Enable AI assistant chat |
| `ASSISTANT_AI_PROVIDER` | `openrouter` | `openrouter` or `openai` |
| `OPENROUTER_API_KEY` | — | OpenRouter API key |
| `OPENAI_API_KEY` | — | OpenAI API key |
| `ALPHA_VANTAGE_API_KEY` | — | Alpha Vantage API key for stock market endpoints |
| `INSIGHT_ENABLED` | `false` | Enable financial insight generation |
| `GOAL_PROGRESS_ENABLED` | `false` | Enable scheduled goal progress checks |
| `STOCK_ENABLED` | `true` | Enable stock market endpoints (API key still required) |

### Database

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL (default: `jdbc:postgresql://postgres:5432/saveapenny`) |
| `DB_USERNAME` | PostgreSQL user |
| `DB_PASSWORD` | PostgreSQL password |

- Flyway applies migrations on startup
- Hibernate validates schema (`ddl-auto: validate`)
- Default host when the app runs in Docker Compose: `postgres:5432`
- Default host when the app runs on your machine: `localhost:5432`

See `.env.example` for all available settings.

## Documentation

| Doc | What it covers |
|-----|----------------|
| [Getting Started](docs/getting-started.md) | Setup, first run, smoke test |
| [Usage Guide](docs/usage-guide.md) | Common workflows and endpoints |
| [API Reference](docs/api-reference.md) | Endpoint list, conventions |
| [Architecture](docs/architecture.md) | Module structure, request lifecycle |
| [Auth Flow](docs/auth-flow.md) | Token lifecycle, mobile client guide |
| [Error Codes](docs/error-codes.md) | Complete error code reference |
| [Environment Variables](docs/env-reference.md) | All configurable properties |
| [Rate Limiting](docs/rate-limiting.md) | Limits, headers, mobile best practices |
| [Security](docs/security.md) | Auth, token handling, data protection |
| [Testing Guide](docs/testing-guide.md) | How to run and write tests |
| [Deployment & Operations](docs/deployment-operations.md) | Runtime deps, env vars, troubleshooting |
| [Accounts](docs/features/accounts.md) | Account types, mutation rules |
| [Transactions](docs/features/transactions.md) | Income, expense, transfers |
| [Categories](docs/features/categories.md) | System vs user categories |
| [Budgets](docs/features/budgets.md) | Monthly/yearly budgets, status |
| [Recurring Transactions](docs/features/recurring-transactions.md) | Scheduling, lifecycle |
| [Reports](docs/features/reports.md) | Monthly summary, net worth |
| [CSV Import](docs/features/csv-import.md) | Preview-confirm workflow |
| [Notifications](docs/features/notifications.md) | Read/unread tracking |
| [Audit Logs](docs/features/audit-logs.md) | Change tracking |
| [OCR](docs/features/ocr.md) | Receipt processing (enabled by default, requires Tesseract) |
| [Assistant](docs/features/assistant.md) | AI chat (disabled by default) |
| [Insights](docs/features/insights.md) | Financial observations (disabled by default) |
| [Goals](docs/features/goals.md) | Goal tracking, scenarios, and simulation |
| [Stocks](docs/features/stocks.md) | Alpha Vantage powered stock market data |
| [Billing](docs/features/billing.md) | RevenueCat subscriptions, entitlement, plan enforcement |

## Features

### Core (always enabled)

| Feature | Native Dependency | Doc |
|---------|-------------------|-----|
| Accounts | — | [docs/features/accounts.md](docs/features/accounts.md) |
| Transactions & Transfers | — | [docs/features/transactions.md](docs/features/transactions.md) |
| Budgets | — | [docs/features/budgets.md](docs/features/budgets.md) |
| Recurring Transactions | — | [docs/features/recurring-transactions.md](docs/features/recurring-transactions.md) |
| Reports & Net Worth | — | [docs/features/reports.md](docs/features/reports.md) |
| CSV Import | — | [docs/features/csv-import.md](docs/features/csv-import.md) |
| Notifications | — | [docs/features/notifications.md](docs/features/notifications.md) |
| OCR Receipt Processing | Tesseract | [docs/features/ocr.md](docs/features/ocr.md) |

### Optional / Configurable

| Feature | Enable via | Doc |
|---------|------------|-----|
| AI Assistant Chat | `ASSISTANT_ENABLED=true` | [docs/features/assistant.md](docs/features/assistant.md) |
| Goal Progress Checks | `GOAL_PROGRESS_ENABLED=true` | [docs/features/goals.md](docs/features/goals.md) |
| Financial Insights | `insight.enabled: true` | [docs/features/insights.md](docs/features/insights.md) |
| Stock Market Data | `STOCK_ENABLED=true` with `ALPHA_VANTAGE_API_KEY` set for live requests | [docs/features/stocks.md](docs/features/stocks.md) |
| Billing (RevenueCat) | `REVENUECAT_ENABLED=true` with `REVENUECAT_SECRET_API_KEY` set | [docs/features/billing.md](docs/features/billing.md) |
| Firebase Analytics | `FIREBASE_ANALYTICS_ENABLED=true` with app id/API secret set | [docs/features/firebase-analytics.md](docs/features/firebase-analytics.md) |

## Project

Backend API only. No built-in frontend.
