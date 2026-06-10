# Usage Guide

## Overview

SaveAPenny is an API-first backend. Most users and integrators work through Swagger, Postman, or direct HTTP calls.

The typical order of use is:

1. register or log in
2. create one or more accounts
3. review or create categories
4. record transactions and transfers
5. create budgets
6. use reports and insights
7. optionally use imports, OCR, assistant chat, and goals

## Authentication Flow

Public auth endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Protected endpoints require:

```text
Authorization: Bearer <accessToken>
```

## Accounts

Use accounts to define where money is stored.

Common account types:

- `CASH`
- `BANK`
- `CREDIT`
- `SAVINGS`
- `INVESTMENT`

Main endpoints:

- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{accountId}`
- `PUT /api/v1/accounts/{accountId}`
- `DELETE /api/v1/accounts/{accountId}`

Recommended first account examples:

- Wallet
- Checking account
- Savings account

## Categories

Categories organize income and expense transactions.

Main endpoints:

- `POST /api/v1/categories`
- `GET /api/v1/categories`
- `GET /api/v1/categories/{categoryId}`
- `PUT /api/v1/categories/{categoryId}`
- `DELETE /api/v1/categories/{categoryId}`

The list endpoint returns system categories plus any user-created categories.

## Transactions And Transfers

Transactions are the core ledger records in the system.

Main endpoints:

- `POST /api/v1/transactions`
- `POST /api/v1/transactions/transfer`
- `GET /api/v1/transactions`
- `GET /api/v1/transactions/{transactionId}`
- `PUT /api/v1/transactions/{transactionId}`
- `DELETE /api/v1/transactions/{transactionId}`

Useful filters on `GET /api/v1/transactions`:

- `from`
- `to`
- `type`
- `accountId`
- `categoryId`
- `minAmount`
- `maxAmount`
- `keyword`
- `page`
- `size`
- `sort`

Use transfers when money moves between two owned accounts.

## Budgets

Budgets track spending against defined limits.

Main endpoints:

- `POST /api/v1/budgets`
- `GET /api/v1/budgets`
- `GET /api/v1/budgets/{budgetId}`
- `GET /api/v1/budgets/{budgetId}/status`
- `PUT /api/v1/budgets/{budgetId}`
- `DELETE /api/v1/budgets/{budgetId}`
- `DELETE /api/v1/budgets/batch`

Budget periods:

- `MONTHLY`
- `YEARLY`

## Recurring Transactions

Recurring transactions automate regular income or expense entries. Create one with a frequency, and the scheduler will automatically generate transactions.

Main endpoints:

- `POST /api/v1/automations/recurring-transactions`
- `GET /api/v1/automations/recurring-transactions`
- `GET /api/v1/automations/recurring-transactions/{id}`
- `PUT /api/v1/automations/recurring-transactions/{id}`
- `PATCH /api/v1/automations/recurring-transactions/{id}/pause`
- `PATCH /api/v1/automations/recurring-transactions/{id}/resume`
- `DELETE /api/v1/automations/recurring-transactions/{id}`
- `GET /api/v1/automations/recurring-transactions/{id}/history`
- `GET /api/v1/automations/recurring-transactions/upcoming`

### Lifecycle

Each recurring item has an explicit status:

- `ACTIVE` — the scheduler processes it normally
- `PAUSED` — temporarily suspended; use `resume` to reactivate
- `EXPIRED` — past its `endDate` or soft-deleted
- `FAILED` — the last execution attempt failed (will be retried)

### Execution History

The `history` endpoint returns per-run records with status (`SUCCESS`, `FAILED`, or `SKIPPED`), the generated transaction ID (if any), and the failure reason. The scheduler is idempotent — it skips dates that already have a `SUCCESS` history entry.

### Upcoming Preview

The `upcoming` endpoint projects future runs for all active recurring items, so you can preview expected cash flow.

### Classification

Optional classification field helps categorize items for UI display:

- `PAYCHECK`, `SUBSCRIPTION`, `RENT`, `UTILITY`, `LOAN_PAYMENT`, `SAVINGS_CONTRIBUTION`, `OTHER`

## Reports

Reports turn transaction history into financial summaries.

Main endpoints:

- `GET /api/v1/reports/monthly-summary?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/monthly-summary/export?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/category-spending?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/cash-flow?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/net-worth?snapshotDate=YYYY-MM-DD`

Net worth is computed as total assets minus total liabilities as of the given `snapshotDate`. Results are persisted on first access per (user, date), and a daily background job pre-computes snapshots so historical queries return stable, previously-captured values.

Use these for:

- spending review
- cash flow analysis
- category concentration
- net worth tracking
- monthly CSV exports

## Notifications And Insights

Notifications expose user-facing status changes and alerts.

Notification endpoints:

- `POST /api/v1/notifications`
- `GET /api/v1/notifications`
- `GET /api/v1/notifications/{notificationId}`
- `PUT /api/v1/notifications/{notificationId}`
- `DELETE /api/v1/notifications/{notificationId}`
- `GET /api/v1/notifications/unread-count`
- `PATCH /api/v1/notifications/mark-all-read`

Insights provide generated financial observations.

Insight endpoints:

- `GET /api/v1/insights`
- `GET /api/v1/insights/{id}`
- `PATCH /api/v1/insights/{id}/read`
- `PATCH /api/v1/insights/{id}/dismiss`
- `POST /api/v1/insights/generate`

## CSV Import Workflow

Transaction import uses a preview-confirm-status flow.

Endpoints:

- `POST /api/v1/imports/transactions/preview`
- `POST /api/v1/imports/transactions/confirm`
- `GET /api/v1/imports/transactions/{importId}/status`

Typical flow:

1. upload a CSV file for preview
2. inspect the parsed rows and validation feedback
3. confirm the import using the returned `importId`
4. poll status until the import is complete

## OCR Workflow

OCR endpoints:

- `POST /api/imports/ocr`
- `GET /api/imports/ocr/{jobId}`

Supported upload types:

- PNG
- JPEG
- PDF

Typical flow:

1. upload a file
2. receive a job id
3. poll job status
4. review parsed candidates and extracted text

## Assistant Workflow

Assistant endpoint:

- `POST /api/v1/assistant/chat`

Request supports:

- `message`
- optional `sessionId`
- optional `history`

Good assistant questions:

- "Where am I spending the most this month?"
- "Which categories are over budget?"
- "Why is my cash flow negative?"
- "What should I cut first?"

## Goals Workflow

Goals are covered in detail in [Goals Feature Guide](features/goals.md).

Typical flow:

1. create a goal
2. simulate or what-if the goal
3. add scenarios if needed
4. review runs and progress over time

## Best Way To Explore The Product

1. start the app
2. register a user
3. create an account
4. add a few income and expense transactions
5. create a budget
6. open reports
7. try CSV import or OCR
8. ask the assistant a budgeting question
9. create and simulate a goal
