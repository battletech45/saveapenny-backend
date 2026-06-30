# Usage Guide

## Overview

SaveAPenny is an API-first backend. Most users and integrators work through Swagger UI, Postman, or direct HTTP calls. The typical usage order follows the financial workflow: authenticate → create accounts → record transactions → create budgets → review reports.

For paginated list endpoints, the API now uses one shared response contract with `items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`, and `hasPrevious`. See [API Reference](api-reference.md).

## Authentication

All business endpoints require an `Authorization: Bearer <accessToken>` header. Public auth endpoints:

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/v1/auth/register` | Create account |
| POST | `/api/v1/auth/login` | Log in |
| POST | `/api/v1/auth/refresh` | Rotate tokens |
| POST | `/api/v1/auth/logout` | Revoke refresh token |

See [Auth Flow](auth-flow.md) for the complete token lifecycle and mobile client guidance.

## Accounts

Accounts represent where money is held. Common account types:

| Type | Use Case |
|------|----------|
| `CASH` | Physical wallet |
| `BANK` | Checking or current account |
| `CREDIT` | Credit card / credit line |
| `SAVINGS` | Savings account |
| `INVESTMENT` | Investment or brokerage account |

**Recommended first accounts:** Wallet, Checking account, Savings account.

See [Accounts](features/accounts.md) for mutation rules and deletion behavior.

## Categories

Categories organize transactions into income and expense groups. The system provides default categories; users can create their own.

| Type | Examples |
|------|----------|
| `INCOME` | Salary, Freelance, Investment income |
| `EXPENSE` | Groceries, Rent, Utilities, Dining |

See [Categories](features/categories.md) for system vs. user category rules.

## Transactions and Transfers

Transactions are the core ledger. Each transaction records an income or expense against an account and category.

| Operation | Endpoint | Description |
|-----------|----------|-------------|
| Income/Expense | `POST /api/v1/transactions` | Single transaction |
| Transfer | `POST /api/v1/transactions/transfer` | Move money between accounts |
| List | `GET /api/v1/transactions` | Filterable, paginated list |
| Update | `PUT /api/v1/transactions/{id}` | Modify existing transaction |
| Delete | `DELETE /api/v1/transactions/{id}` | Remove and reverse balance impact |

Useful query filters: `from`, `to`, `type`, `accountId`, `categoryId`, `minAmount`, `maxAmount`, `keyword`.

## Budgets

Budgets track spending against defined limits per category and period.

| Period | Example |
|--------|---------|
| `MONTHLY` | $500/month on groceries |
| `YEARLY` | $6,000/year on dining |

The `GET /api/v1/budgets/{id}/status` endpoint returns current spending, remaining amount, and percentage used.

See [Budgets](features/budgets.md) for details.

## Recurring Transactions

Recurring transactions automate regular entries. The scheduler generates transactions on the configured frequency.

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Normal scheduling |
| `PAUSED` | Temporarily suspended |
| `EXPIRED` | Past end date or deleted |
| `FAILED` | Last execution failed |

See [Recurring Transactions](features/recurring-transactions.md) for lifecycle, classification, and upcoming projections.

## Reports

Reports turn transaction history into financial summaries:

| Report | Purpose |
|--------|---------|
| Monthly Summary | Income/expense grouped by month |
| Category Spending | Totals grouped by category |
| Cash Flow | Daily income/expense |
| Net Worth | Assets minus liabilities on a given date |

See [Reports](features/reports.md) for details.

## Optional Features

### CSV Import

Bulk-import transactions using a preview-confirm-status workflow:

1. `POST /api/v1/imports/transactions/preview` — upload and parse
2. `POST /api/v1/imports/transactions/confirm` — start import
3. `GET /api/v1/imports/transactions/{importId}/status` — poll completion

See [CSV Import](features/csv-import.md).

### OCR Receipt Processing

Upload receipt images (PNG, JPEG, PDF) for automatic text extraction:

1. `POST /api/imports/ocr` — upload file
2. `GET /api/imports/ocr/{jobId}` — poll results

See [OCR](features/ocr.md).

### AI Assistant (Penny Dog)

Ask financial questions in natural language:

```bash
curl -X POST "http://localhost:8080/api/v1/assistant/chat" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"message":"Where am I spending the most this month?"}'
```

Good questions: spending breakdown, budget status, cash flow analysis, goal progress.

See [Assistant](features/assistant.md).

### Goals and Simulations

Plan savings, debt payoff, purchases, retirement, or income targets. Create a goal, run simulations, and compare scenarios.

See [Goals](features/goals.md).

### Financial Insights

Automatically generated observations about spending patterns, trends, and anomalies. Generated by a daily scheduled job.

See [Insights](features/insights.md).

## Recommended Exploration Order

1. Start the application
2. Register a user
3. Create an account (BANK type)
4. Add a few income and expense transactions
5. Create a budget for a category
6. Open reports to review spending
7. Try CSV import or OCR
8. Ask the assistant a budgeting question
9. Create and simulate a goal

## Referenced Files

| File | Purpose |
|------|---------|
| `docs/api-reference.md` | Complete endpoint listing |
| `docs/auth-flow.md` | Token lifecycle |
| `docs/error-codes.md` | Error response catalogue |
| `docs/features/*.md` | Per-feature documentation |
