# Credit Cards

## Overview

`CREDIT`-type accounts simulate a real credit card instead of a funds-on-hand account: spending increases the balance (debt owed) up to a credit limit, a monthly statement is generated with a minimum payment due, and any balance not paid off by the due date accrues interest. This is layered on top of the [Accounts](accounts.md) and [Transactions](transactions.md) modules.

## How Balance Works for CREDIT Accounts

Unlike `CASH`/`BANK`/`SAVINGS`/`INVESTMENT` accounts, `balance` on a `CREDIT` account represents **debt owed**, not funds available:

| Transaction type | Effect on balance |
|-------------------|-------------------|
| `EXPENSE` | Increases the balance (debt). Rejected with `CREDIT_LIMIT_EXCEEDED` if it would exceed `creditLimit` |
| `INCOME` (e.g. a merchant refund) | Decreases the balance (debt) |
| `TRANSFER` | Not supported to or from a `CREDIT` account — use the payment endpoint below instead |

## Setup

Credit fields are supplied on account creation (`POST /api/v1/accounts` with `type: "CREDIT"`):

| Field | Notes |
|-------|-------|
| `creditLimit` | Maximum debt the card can carry |
| `apr` | Annual percentage rate (e.g. `24.99`) |
| `statementDay` | Day of month (1–28) the billing cycle closes on |

`creditLimit`, `apr`, and `statementDay` can be changed later via `PATCH /api/v1/accounts/{accountId}/credit`.

## Statement Cycle

A daily scheduled job (`CreditCardStatementScheduler`, cron `credit-card.statement.cron`) runs the billing cycle:

1. **Close statements** — for every credit account whose `statementDay` falls today: any balance carried over from the previous statement (i.e. not paid off in full) accrues interest at `apr / 12` for the cycle, posted as an `EXPENSE` transaction under the system "Interest & Fees" category. The current balance becomes the new statement's `newBalance`, and a `minimumPaymentDue` is computed as `max($25, 2% of balance)` (capped at the balance) plus any shortfall carried over from a missed payment. The due date is `statementDate + gracePeriodDays` (default 21 days).
2. **Evaluate due statements** — any open statement whose due date is today is marked `PAID` if at least the minimum was paid, otherwise `MISSED`; the shortfall rolls into the next cycle's minimum payment.

## Making a Payment

`POST /api/v1/accounts/{accountId}/credit/payments` moves funds from another (non-credit) account to pay down the balance:

| Field | Notes |
|-------|-------|
| `sourceAccountId` | Account to debit; must be active, non-`CREDIT`, and match the credit account's currency |
| `paymentType` | `MINIMUM_DUE`, `FULL_BALANCE`, or `CUSTOM` |
| `amount` | Required only for `CUSTOM`; must be `> 0` and cannot exceed the outstanding balance |

The payment is recorded as a `TRANSFER` transaction (source → credit account) for ledger history, and is applied against the current open statement.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| PATCH | `/api/v1/accounts/{accountId}/credit` | Update `creditLimit`, `apr`, `statementDay` |
| GET | `/api/v1/accounts/{accountId}/credit/statements` | Paginated statement history |
| POST | `/api/v1/accounts/{accountId}/credit/payments` | Make a payment (minimum, full, or custom) |

`GET /api/v1/accounts/{accountId}` also returns a nested `creditCard` object for `CREDIT` accounts: `creditLimit`, `apr`, `statementDay`, `gracePeriodDays`, `availableCredit`, `currentStatementBalance`, `minimumPaymentDue`, `statementDate`, `paymentDueDate`, `statementStatus`.

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `CREDIT_LIMIT_EXCEEDED` | 400 | An expense would push the balance above `creditLimit` |
| `INVALID_CREDIT_CARD_DETAILS` | 400 | Missing/invalid `creditLimit`, `apr`, or `statementDay`, or initial balance exceeds the limit |
| `INVALID_CREDIT_CARD_PAYMENT` | 400 | Invalid payment amount, wrong account type/currency, or no outstanding balance |
| `CREDIT_CARD_DETAILS_NOT_FOUND` | 404 | Credit details missing for the account (data integrity issue) |

`INVALID_TRANSFER` is also returned if a transfer is attempted to or from a `CREDIT` account.

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Separate `credit_card_details`/`credit_card_statements` tables instead of extra `Account` columns | Keeps `Account` lean; credit-specific fields only apply to one account type |
| Dedicated payment endpoint instead of reusing `/transactions/transfer` | Lets the API resolve "minimum" vs. "full" amounts server-side and enforce credit-specific rules (no overpay, no cash advances) |
| No cash advances / transfers from a `CREDIT` account in v1 | Avoids ambiguous balance polarity in the shared transfer code path; scoped out for a later iteration |
| Interest computed from what was actually carried over (not the statement's `PAID`/`MISSED` label) | A statement can be `PAID` (minimum met) while still carrying a balance that owes interest |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/creditcard/entity/CreditCardDetails.java` | Per-account limit/APR/billing-day configuration |
| `src/main/java/com/saveapenny/creditcard/entity/CreditCardStatement.java` | Per-cycle statement history |
| `src/main/java/com/saveapenny/creditcard/service/impl/CreditCardServiceImpl.java` | Details management and payment logic |
| `src/main/java/com/saveapenny/creditcard/scheduler/CreditCardStatementScheduler.java` | Daily statement close / due-date evaluation job |
| `src/main/java/com/saveapenny/creditcard/controller/CreditCardController.java` | REST endpoints |
| `src/main/java/com/saveapenny/transaction/service/impl/TransactionServiceImpl.java` | CREDIT-aware balance math (`applyCreditTransactionImpact`) |
