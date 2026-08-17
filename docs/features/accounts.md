# Accounts

## Overview

Accounts represent where money is held. Every transaction and transfer is linked to an account. Accounts are user-scoped — each user manages their own set of accounts independently.

## Account Types

| Type | Use Case | Balance Interpretation |
|------|----------|----------------------|
| `CASH` | Physical cash / wallet | Positive = money on hand |
| `BANK` | Checking or current account | Positive = deposit |
| `CREDIT` | Credit card / credit line | Positive = debt owed (spending increases it, up to `creditLimit`) |
| `SAVINGS` | Savings account | Positive = savings |
| `INVESTMENT` | Investment or brokerage account | Positive = portfolio value |

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `name` | Yes | Must be unique per user (including soft-deleted accounts) |
| `type` | Yes | One of: `CASH`, `BANK`, `CREDIT`, `SAVINGS`, `INVESTMENT` |
| `currency` | Yes | ISO-4217 code (e.g., `USD`, `EUR`, `TRY`) |
| `initialBalance` | Only if `type != CREDIT` | Starting balance. Required (send `0` explicitly if there's none) for every type except `CREDIT`, where it's optional and defaults to `0` — it represents starting debt there and must not exceed `creditLimit` |
| `creditLimit` | Only if `type == CREDIT` | Maximum debt the account can carry |
| `apr` | Only if `type == CREDIT` | Annual percentage rate applied to any balance carried past the due date |
| `statementDay` | Only if `type == CREDIT` | Day of month (1–28) the billing cycle closes on |

`CREDIT` accounts behave like a real credit card rather than a funds-on-hand account — see [Credit Cards](credit-cards.md) for the full billing cycle, interest, and payment model.

## Currency Rules

- Each account has a single ISO-4217 currency
- All transactions against the account must use the same currency
- Currency cannot be changed after the account has been used

## Mutation Rules

| Field | Can Change? | Constraint |
|-------|------------|------------|
| Name | Yes | Must remain unique per user |
| Type | No | Blocked after account has been used; changing to/from `CREDIT` is always blocked (use a dedicated account instead) |
| Currency | No | Blocked after account has been used |
| Balance | No | Updated automatically by transactions |

An account is considered **used** if any of these are true:

- `balance != 0`
- `initialBalance != 0`
- Any transaction references the account
- Any transfer references the account

## Deletion

- Accounts are **soft-deleted** — the record remains but is marked inactive
- The account name stays **reserved** and cannot be reused
- Transactions and audit history for the account are preserved

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts` | List accounts (shared paginated response, sorted) |
| GET | `/api/v1/accounts/{id}` | Get account details |
| PUT | `/api/v1/accounts/{id}` | Update account name |
| DELETE | `/api/v1/accounts/{id}` | Soft-delete an account |

`CREDIT` accounts additionally expose `/api/v1/accounts/{id}/credit/**` for limit/APR updates, statement history, and payments — see [Credit Cards](credit-cards.md).

## List Response Shape

`GET /api/v1/accounts` returns the shared pagination contract described in [API Reference](../api-reference.md): `items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`, and `hasPrevious`.

## Typical Workflow

1. Create a wallet (CASH, USD, 0 balance)
2. Create a checking account (BANK, USD, initial deposit)
3. Record income/expense transactions against the accounts
4. Create a savings account (SAVINGS, USD) and transfer money to it

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `ACCOUNT_NOT_FOUND` | 404 | Account does not exist or is not owned by the caller |
| `ACCOUNT_NAME_ALREADY_EXISTS` | 409 | Name conflicts with an existing or soft-deleted account |
| `ACCOUNT_MUTATION_NOT_ALLOWED` | 400 | Attempt to change type or currency after account has been used |
| `ACCOUNT_INACTIVE` | 400 | Account is soft-deleted or inactive |
| `INITIAL_BALANCE_REQUIRED` | 400 | `initialBalance` omitted for a non-`CREDIT` account |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Soft delete with name reservation | Prevents account name reuse, preserving data integrity for historical transactions |
| Immutable type/currency after use | Ensures consistent financial reporting; prevents account reinterpretation |
| User-scoped isolation | Each user's accounts are fully isolated via `user_id` |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/account/entity/Account.java` | JPA entity |
| `src/main/java/com/saveapenny/account/controller/AccountController.java` | REST endpoints |
| `src/main/java/com/saveapenny/account/service/impl/AccountServiceImpl.java` | Business logic |
