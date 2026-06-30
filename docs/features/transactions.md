# Transactions

## Overview

Transactions are the core ledger records. Each transaction records a financial event — income or expense — linked to an account and category. Transactions are user-scoped. Transfer operations create paired transactions between two accounts.

## Transaction Types

| Type | Description | Balance Impact |
|------|-------------|----------------|
| `INCOME` | Money received | Increases account balance |
| `EXPENSE` | Money spent | Decreases account balance |

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `accountId` | Yes | UUID of the target account |
| `categoryId` | Yes | UUID of the category |
| `type` | Yes | `INCOME` or `EXPENSE` |
| `amount` | Yes | Positive decimal value |
| `currency` | Yes | ISO-4217 code, must match the account's currency |
| `description` | No | Free text |
| `date` | No | Defaults to current date (ISO-8601) |
| `merchant` | No | Merchant or payee name |

## Currency Validation

The transaction currency must exactly match the currency of the linked account. A transaction with `USD` cannot be posted to an account configured with `EUR`. This validation runs on both create and update.

## Balance Impact

| Operation | Income | Expense |
|-----------|--------|---------|
| Create | Balance + amount | Balance − amount |
| Delete | Balance − amount | Balance + amount |
| Update | Adjusts by the difference | Adjusts by the difference |

## Transfers

Transfers move money between two accounts owned by the same user. A transfer creates two transactions:

- An **expense** on the source account
- An **income** on the destination account

Both transactions share the same amount and currency. Source and destination currencies must match.

```json
{
  "fromAccountId": "<source-account-uuid>",
  "toAccountId": "<destination-account-uuid>",
  "amount": 500.00,
  "currency": "USD",
  "description": "Transfer to savings"
}
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/transactions` | Create an income or expense |
| POST | `/api/v1/transactions/transfer` | Transfer between owned accounts |
| GET | `/api/v1/transactions` | List transactions (shared paginated response, filterable) |
| GET | `/api/v1/transactions/{id}` | Get transaction details |
| PUT | `/api/v1/transactions/{id}` | Update a transaction |
| DELETE | `/api/v1/transactions/{id}` | Delete a transaction |

## List Response Shape

`GET /api/v1/transactions` returns the shared pagination contract from [API Reference](../api-reference.md). Transaction records are returned in `items`.

## Query Filters

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
| `size` | Integer | Page size (default 20) |
| `sort` | String | Sort field and direction (e.g., `date,desc`) |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `TRANSACTION_NOT_FOUND` | 404 | Transaction not found or not owned by the caller |
| `INVALID_TRANSACTION_CURRENCY` | 400 | Currency does not match the account |
| `INVALID_TRANSFER` | 400 | Transfer source and destination are the same account or currencies mismatch |
| `INSUFFICIENT_BALANCE` | 400 | Transfer would overdraw the source account |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Transfers create paired transactions | Maintains full audit trail; each leg is a standard transaction record |
| Currency must match account | Prevents silent currency conversion; consistent financial reporting |
| Update adjusts balance by difference | Accurately reflects the net change rather than reversing and recreating |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/transaction/entity/Transaction.java` | JPA entity |
| `src/main/java/com/saveapenny/transaction/controller/TransactionController.java` | REST endpoints |
| `src/main/java/com/saveapenny/transaction/service/impl/TransactionServiceImpl.java` | Business logic including balance updates |
