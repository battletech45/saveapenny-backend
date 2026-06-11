# SaveAPenny

Personal finance API. Spring Boot backend for budgeting, transaction tracking, imports, and financial reports.

## Quick Start

### Docker Compose

```bash
cp .env.example .env   # fill in values
docker compose up --build
```

App starts at `http://localhost:8080`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

### Local

Requirements: Java 24, Maven 3.9+, PostgreSQL 16+.

```bash
mvn spring-boot:run
```

## Required Configuration

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | PostgreSQL user |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | HS512 key (64+ chars) |

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
| [OCR](docs/features/ocr.md) | Receipt processing (disabled) |
| [Assistant](docs/features/assistant.md) | AI chat (disabled) |
| [Insights](docs/features/insights.md) | Financial observations (disabled) |
| [Goals](docs/features/goals.md) | Goal tracking and simulation (disabled) |

## Features

### Core (always enabled)

| Feature | Doc |
|---------|-----|
| Accounts | [docs/features/accounts.md](docs/features/accounts.md) |
| Transactions & Transfers | [docs/features/transactions.md](docs/features/transactions.md) |
| Budgets | [docs/features/budgets.md](docs/features/budgets.md) |
| Recurring Transactions | [docs/features/recurring-transactions.md](docs/features/recurring-transactions.md) |
| Reports & Net Worth | [docs/features/reports.md](docs/features/reports.md) |
| CSV Import | [docs/features/csv-import.md](docs/features/csv-import.md) |
| Notifications | [docs/features/notifications.md](docs/features/notifications.md) |

### Optional (disabled by default)

| Feature | Enable via | Doc |
|---------|------------|-----|
| OCR Receipt Processing | `ocr.enabled: true` | [docs/features/ocr.md](docs/features/ocr.md) |
| AI Assistant Chat | `ASSISTANT_ENABLED=true` | [docs/features/assistant.md](docs/features/assistant.md) |
| Goal Tracking & Simulation | `goal.progress.enabled: true` | [docs/features/goals.md](docs/features/goals.md) |
| Financial Insights | `insight.enabled: true` | [docs/features/insights.md](docs/features/insights.md) |

## Project

Backend API only. No built-in frontend.
