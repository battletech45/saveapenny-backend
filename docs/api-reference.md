# API Reference

## Overview

This document lists all REST API endpoints. For the complete field-level schema details, use the Swagger UI or OpenAPI document.

| Source | URL |
|--------|-----|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

## Base URL

All endpoints are prefixed with `/api/v1` unless otherwise noted.

## Authentication

Public endpoints (no authentication required):

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Create account |
| POST | `/api/v1/auth/login` | Authenticate |
| POST | `/api/v1/auth/refresh` | Rotate tokens |
| POST | `/api/v1/auth/logout` | Revoke refresh token |
| GET | `/actuator/health` | Health check |
| GET | `/v3/api-docs` | OpenAPI JSON spec |
| GET | `/v3/api-docs.yaml` | OpenAPI YAML spec |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/swagger-ui/**` | Swagger UI assets |

All other endpoints require:

```text
Authorization: Bearer <accessToken>
```

## Standard Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-06-09T12:00:00Z"
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": []
  },
  "timestamp": "2026-06-09T12:00:00Z"
}
```

## Common Conventions

| Convention | Standard |
|------------|----------|
| IDs | UUID (v4) |
| Dates | ISO-8601 (e.g., `2026-06-09`) |
| Date-times | ISO-8601 with timezone (e.g., `2026-06-09T12:00:00Z`) |
| Currencies | ISO-4217 3-letter codes (e.g., `USD`, `EUR`, `TRY`) |
| Pagination | Spring-style: `page`, `size`, `sort` parameters |

## Authentication Endpoints

### Register

`POST /api/v1/auth/register`

```json
{
  "email": "demo@example.com",
  "password": "StrongPass123!",
  "fullName": "Demo User"
}
```

### Login

`POST /api/v1/auth/login`

```json
{
  "email": "demo@example.com",
  "password": "StrongPass123!"
}
```

### Refresh

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "<refreshToken>"
}
```

### Logout

`POST /api/v1/auth/logout`

```json
{
  "refreshToken": "<refreshToken>"
}
```

## Users

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/users/me` | Get current user profile |
| PUT | `/api/v1/users/me` | Update user profile |
| PUT | `/api/v1/users/me/password` | Change password (revokes all refresh tokens) |

## Accounts

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts` | List accounts (paginated) |
| GET | `/api/v1/accounts/{accountId}` | Get account details |
| PUT | `/api/v1/accounts/{accountId}` | Update account name |
| DELETE | `/api/v1/accounts/{accountId}` | Soft-delete an account |

See [Accounts](features/accounts.md) for mutation rules and currency constraints.

## Categories

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/categories` | Create a user category |
| GET | `/api/v1/categories` | List categories (system + user) |
| GET | `/api/v1/categories/{categoryId}` | Get category details |
| PUT | `/api/v1/categories/{categoryId}` | Update a category |
| DELETE | `/api/v1/categories/{categoryId}` | Delete a user category |

See [Categories](features/categories.md) for system vs. user category rules.

## Transactions

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/transactions` | Create an income or expense |
| POST | `/api/v1/transactions/transfer` | Transfer between owned accounts |
| GET | `/api/v1/transactions` | List transactions (paginated, filterable) |
| GET | `/api/v1/transactions/{transactionId}` | Get transaction details |
| PUT | `/api/v1/transactions/{transactionId}` | Update a transaction |
| DELETE | `/api/v1/transactions/{transactionId}` | Delete a transaction |

### Query Filters

`GET /api/v1/transactions` supports:

| Parameter | Type | Description |
|-----------|------|-------------|
| `from` | Date | Start date (inclusive) |
| `to` | Date | End date (inclusive) |
| `type` | String | `INCOME` or `EXPENSE` |
| `accountId` | UUID | Filter by account |
| `categoryId` | UUID | Filter by category |
| `minAmount` | Decimal | Minimum amount |
| `maxAmount` | Decimal | Maximum amount |
| `keyword` | String | Search in description and merchant |
| `page` | Integer | Page number (0-based) |
| `size` | Integer | Page size |
| `sort` | String | Sort field and direction |

See [Transactions](features/transactions.md) for balance impact and transfer behavior.

## Budgets

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/budgets` | Create a budget |
| GET | `/api/v1/budgets` | List budgets (paginated) |
| GET | `/api/v1/budgets/{budgetId}` | Get budget details |
| GET | `/api/v1/budgets/{budgetId}/status` | Get spending status |
| PUT | `/api/v1/budgets/{budgetId}` | Update budget |
| DELETE | `/api/v1/budgets/{budgetId}` | Delete a budget |
| DELETE | `/api/v1/budgets/batch` | Delete multiple budgets |

See [Budgets](features/budgets.md) for period types and status calculation.

## Reports

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/reports/monthly-summary?from=&to=` | Monthly income/expense summary |
| GET | `/api/v1/reports/monthly-summary/export?from=&to=` | CSV download |
| GET | `/api/v1/reports/category-spending?from=&to=` | Spending by category |
| GET | `/api/v1/reports/cash-flow?from=&to=` | Daily cash flow |
| GET | `/api/v1/reports/net-worth?snapshotDate=` | Net worth snapshot |

See [Reports](features/reports.md) for report details and best practices.

## Recurring Transactions

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/automations/recurring-transactions` | Create |
| GET | `/api/v1/automations/recurring-transactions` | List (paginated) |
| GET | `/api/v1/automations/recurring-transactions/{id}` | Get details |
| PUT | `/api/v1/automations/recurring-transactions/{id}` | Update |
| DELETE | `/api/v1/automations/recurring-transactions/{id}` | Soft-delete (→ EXPIRED) |
| PATCH | `/api/v1/automations/recurring-transactions/{id}/pause` | Pause |
| PATCH | `/api/v1/automations/recurring-transactions/{id}/resume` | Resume |
| GET | `/api/v1/automations/recurring-transactions/{id}/history` | Execution history |
| GET | `/api/v1/automations/recurring-transactions/upcoming?limit=10` | Upcoming projections |

See [Recurring Transactions](features/recurring-transactions.md) for lifecycle and classification.

## Notifications

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/notifications` | Create a notification |
| GET | `/api/v1/notifications` | List notifications (paginated) |
| GET | `/api/v1/notifications/{notificationId}` | Get notification details |
| PUT | `/api/v1/notifications/{notificationId}` | Update a notification |
| DELETE | `/api/v1/notifications/{notificationId}` | Delete a notification |
| GET | `/api/v1/notifications/unread-count` | Get unread count |
| PATCH | `/api/v1/notifications/mark-all-read` | Mark all notifications as read |

See [Notifications](features/notifications.md).

## Transaction Imports

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/imports/transactions/preview` | Upload and preview CSV |
| POST | `/api/v1/imports/transactions/confirm` | Confirm and start import |
| GET | `/api/v1/imports/transactions/{importId}/status` | Poll import status |

See [CSV Import](features/csv-import.md) for workflow details.

## OCR Imports

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/imports/ocr` | Upload a document for OCR |
| GET | `/api/imports/ocr/{jobId}` | Get job status and results |

See [OCR](features/ocr.md) for configuration and performance characteristics.

## Audits

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/audits` | Create an audit entry |
| GET | `/api/v1/audits` | List audit logs (paginated, filterable) |
| GET | `/api/v1/audits/{auditLogId}` | Get audit entry details |

See [Audit Logs](features/audit-logs.md) for tracked resources and query filters.

## Assistant

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/assistant/chat` | Send a message and get a response |

See [Assistant](features/assistant.md) for capabilities and example questions.

## Insights

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/insights` | List insights (paginated) |
| GET | `/api/v1/insights/{id}` | Get insight details |
| PATCH | `/api/v1/insights/{id}/read` | Mark as read |
| PATCH | `/api/v1/insights/{id}/dismiss` | Dismiss an insight |
| POST | `/api/v1/insights/generate` | Trigger on-demand generation |

See [Insights](features/insights.md) for detection methods and configuration.

## Goals

### Goal Management

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/goals` | Create a goal |
| GET | `/api/v1/goals` | List goals |
| GET | `/api/v1/goals/{goalId}` | Get goal details |
| PATCH | `/api/v1/goals/{goalId}` | Update goal fields |
| DELETE | `/api/v1/goals/{goalId}` | Delete a goal |
| PATCH | `/api/v1/goals/{goalId}/status` | Update goal status |

### Goals — Scenarios and History

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/goals/{goalId}/scenarios` | Create a scenario |
| GET | `/api/v1/goals/{goalId}/scenarios` | List scenarios |
| GET | `/api/v1/goals/{goalId}/runs` | List simulation run history |
| POST | `/api/v1/goals/{goalId}/scenarios/compare` | Compare scenarios |
| POST | `/api/v1/goals/{goalId}/what-if` | What-if analysis |

### Goals — Simulation

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/goals/simulate` | Prompt-based simulation |
| POST | `/api/v1/goals/simulate/draft` | Draft simulation (before saving) |
| POST | `/api/v1/goals/{goalId}/simulate` | Re-run saved goal simulation |

See [Goals](features/goals.md) for goal types, feasibility values, and warnings.

## Example Protected Request

```bash
curl -X GET "http://localhost:8080/api/v1/accounts?page=0&size=20&sort=name,asc" \
  -H "Authorization: Bearer <accessToken>"
```

## Common Error Cases

| HTTP | Code | When |
|------|------|------|
| 400 | `VALIDATION_FAILED` | Request body or query parameter validation failed |
| 401 | `ACCESS_DENIED` | Missing, invalid, or expired access token |
| 404 | `*_NOT_FOUND` | Resource does not exist or is not owned by the caller |
| 409 | `*_ALREADY_EXISTS` | Duplicate resource or conflicting request |
| 429 | `RATE_LIMITED` | Too many requests |
| 503 | `*_DISABLED` | Feature is not enabled (assistant, OCR) |

See [Error Codes](error-codes.md) for the complete error catalogue.

## Related Documents

- [Auth Flow](auth-flow.md) — Token lifecycle and mobile implementation
- [Error Codes](error-codes.md) — Complete error catalogue
- [Swagger UI](http://localhost:8080/swagger-ui.html) — Interactive API browser
- [OpenAPI JSON](http://localhost:8080/v3/api-docs) — Machine-readable API spec
