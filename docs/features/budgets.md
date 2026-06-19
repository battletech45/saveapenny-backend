# Budgets

## Overview

Budgets track spending against defined limits per category and time period. A budget is linked to a single category and a period (monthly or yearly). Budgets are user-scoped.

## Budget Periods

| Period | Description | Example |
|--------|-------------|---------|
| `MONTHLY` | Resets each calendar month | $500/month on groceries |
| `YEARLY` | Resets each calendar year | $6,000/year on dining out |

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `categoryId` | Yes | UUID of the category to budget |
| `period` | Yes | `MONTHLY` or `YEARLY` |
| `limitAmount` | Yes | Maximum spending limit for the period |
| `month` | No | Month/year for the budget (e.g., `2026-06`) |
| `name` | No | Display name |

## Budget Status

`GET /api/v1/budgets/{budgetId}/status` returns current spending against the budget:

```json
{
  "budgetId": "<uuid>",
  "spent": 320.50,
  "limitAmount": 500.00,
  "remaining": 179.50,
  "percentageUsed": 64.1,
  "period": "MONTHLY"
}
```

Spent amount is calculated from transactions matching the category within the budget period.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/budgets` | Create a budget |
| GET | `/api/v1/budgets` | List budgets (paginated) |
| GET | `/api/v1/budgets/{id}` | Get budget details |
| GET | `/api/v1/budgets/{id}/status` | Get spending status vs limit |
| PUT | `/api/v1/budgets/{id}` | Update budget limit or period |
| DELETE | `/api/v1/budgets/{id}` | Delete a budget |
| DELETE | `/api/v1/budgets/batch` | Delete multiple budgets |

## Rules

- Budget amounts are in the account currency
- A budget can only be created for categories the user owns or system categories
- The same category cannot have duplicate budgets for the same period
- Spent amount is calculated from actual transactions (not pending)
- Budget status reflects real-time spending vs. the configured limit

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `BUDGET_NOT_FOUND` | 404 | Budget not found or not owned by the caller |
| `BUDGET_ALREADY_EXISTS` | 409 | Budget already exists for the given category and period |
| `INVALID_BUDGET_DATE_RANGE` | 400 | Budget month or period parameters are invalid |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Status endpoint separate from budget resource | Allows efficient polling without returning full budget data; status can be cached independently |
| Per-category budgets | Simple, predictable model; multi-category budgets can be aggregated client-side |
| Real-time spending calculation | Always reflects current financial state without delay |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/budget/entity/Budget.java` | JPA entity |
| `src/main/java/com/saveapenny/budget/controller/BudgetController.java` | REST endpoints |
| `src/main/java/com/saveapenny/budget/service/impl/BudgetServiceImpl.java` | Budget logic and spending calculation |
