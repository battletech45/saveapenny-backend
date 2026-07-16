# Recurring Transactions

## Overview

Recurring transactions automate regular income or expense entries. Create one with a frequency, and the scheduler automatically generates transactions on the scheduled dates. Recurring transactions are user-scoped.

## Status Lifecycle

| Status | Description |
|--------|-------------|
| `ACTIVE` | Normal scheduling — transactions are generated on each `nextRunDate` |
| `PAUSED` | Temporarily suspended. Use `resume` to reactivate |
| `EXPIRED` | Past its `endDate` or explicitly deleted. No further processing |
| `FAILED` | Last execution attempt failed. Will be retried on next scheduled run |

Status transitions:

```
ACTIVE ──pause──▶ PAUSED
PAUSED ──resume─▶ ACTIVE
ACTIVE ──delete──▶ EXPIRED
ACTIVE ──error──▶ FAILED
```

## Frequencies

| Frequency | Description |
|-----------|-------------|
| `DAILY` | Every day |
| `WEEKLY` | Every 7 days |
| `MONTHLY` | Same day each month |
| `YEARLY` | Same date each year |

## Classification

Optional metadata for UI display and grouping:

| Value | Example Use |
|-------|-------------|
| `PAYCHECK` | Monthly salary |
| `SUBSCRIPTION` | Netflix, Spotify |
| `RENT` | Monthly rent payment |
| `UTILITY` | Electricity, water |
| `LOAN_PAYMENT` | Car loan, mortgage |
| `SAVINGS_CONTRIBUTION` | Monthly savings transfer |
| `OTHER` | Default |

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `accountId` | Yes | Transactions are posted to this account |
| `categoryId` | Yes | Transaction category |
| `type` | Yes | `INCOME` or `EXPENSE` |
| `amount` | Yes | Positive decimal value |
| `frequency` | Yes | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `nextRunDate` | Yes | First scheduled date |
| `name` | No | Display name |
| `description` | No | Copied into generated transactions |
| `startDate` | No | When the recurring item takes effect |
| `endDate` | No | Auto-expires after this date |
| `classification` | No | Metadata for UI grouping and filtering |

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/automations/recurring-transactions` | Create |
| GET | `/api/v1/automations/recurring-transactions` | List (shared paginated response) |
| GET | `/api/v1/automations/recurring-transactions/{id}` | Get details |
| PUT | `/api/v1/automations/recurring-transactions/{id}` | Update |
| DELETE | `/api/v1/automations/recurring-transactions/{id}` | Soft-delete (transitions to `EXPIRED`) |
| PATCH | `/api/v1/automations/recurring-transactions/{id}/pause` | Pause (`ACTIVE → PAUSED`) |
| PATCH | `/api/v1/automations/recurring-transactions/{id}/resume` | Resume (`PAUSED → ACTIVE`) |
| GET | `/api/v1/automations/recurring-transactions/{id}/history` | Execution history (shared paginated response) |
| GET | `/api/v1/automations/recurring-transactions/upcoming?limit=10` | Upcoming projections |

## List Response Shape

Both list endpoints below use the shared pagination contract from [API Reference](../api-reference.md):
- `GET /api/v1/automations/recurring-transactions`
- `GET /api/v1/automations/recurring-transactions/{id}/history`

Returned records are exposed in `items`.

## Execution History

The `history` endpoint returns per-run records with:

| Field | Description |
|-------|-------------|
| `status` | `SUCCESS`, `FAILED`, or `SKIPPED` |
| `generatedTransactionId` | The created transaction ID (if successful) |
| `failureReason` | Error details (if failed) |
| `runDate` | The date the run was attempted |

The scheduler is **idempotent** — it skips dates that already have a `SUCCESS` history entry. It runs on `automation.recurring.cron` (default `0 */5 * * * *`, every 5 minutes); there is no top-level `automation:` block in `application.yml` today, so this is only overridable via the `AUTOMATION_RECURRING_CRON` environment variable (Spring's relaxed binding), not a documented yml default.

## Upcoming Preview

The `upcoming` endpoint projects future runs for all active recurring items, showing expected dates and amounts. Useful for cash flow forecasting.

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `RECURRING_TRANSACTION_NOT_FOUND` | 404 | Entity not found or not owned by the caller |
| `RECURRING_TRANSACTION_DEPENDENCY_NOT_FOUND` | 404 | Referenced account or category not found |
| `INVALID_RECURRING_TRANSACTION_NEXT_RUN_DATE` | 400 | Next run date is in the past |
| `INVALID_RECURRING_TRANSACTION_TYPE` | 400 | Invalid frequency or classification |
| `INVALID_RECURRING_TRANSACTION_STATUS_TRANSITION` | 400 | Cannot transition from current status (e.g., pausing an already expired item) |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Soft delete (→ EXPIRED) | Preserves execution history; avoids cascading deletion of generated transactions |
| Idempotent scheduler | Safe to run multiple times; skips dates with existing successful runs |
| Explicit status lifecycle | Clear state machine; no implicit transitions |
| Classification as metadata | UI grouping without affecting scheduling logic |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/automation/entity/RecurringTransaction.java` | JPA entity |
| `src/main/java/com/saveapenny/automation/controller/RecurringTransactionController.java` | REST endpoints |
| `src/main/java/com/saveapenny/automation/service/impl/RecurringTransactionServiceImpl.java` | Business logic and scheduling |
