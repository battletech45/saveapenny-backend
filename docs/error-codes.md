# Error Codes

## Overview

All API errors return an HTTP 4xx or 5xx status code with a consistent JSON envelope. The `code` field is a machine-readable identifier; the `message` field is a human-readable description; `details` contains field-level validation errors when applicable.

## Response Format

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable description",
    "details": []
  },
  "timestamp": "2026-06-10T12:00:00Z"
}
```

## 4xx Client Errors

### Authentication (401)

| Code | Message | Meaning |
|------|---------|---------|
| `ACCESS_DENIED` | "Unauthorized." or "Invalid or expired access token." | Missing, invalid, or expired JWT |
| `INVALID_CREDENTIALS` | — | Email or password is incorrect |
| `INVALID_REFRESH_TOKEN` | — | Refresh token is invalid, expired, or has been rotated |
| `REFRESH_TOKEN_EXPIRED` | — | Refresh token is past its 7-day expiry |

### Forbidden (403)

| Code | Message | Meaning |
|------|---------|---------|
| `ACCESS_DENIED` | — | Authenticated but insufficient permissions |
| `AUDIT_LOG_ACCESS_DENIED` | — | Attempt to access another user's audit log |
| `PLUS_REQUIRED` | — | Feature is enabled but requires an active Plus subscription |
| `FREE_PLAN_LIMIT_REACHED` | — | Free-tier usage cap reached (active budgets or active goals) |
| `REPORT_HISTORY_LIMIT_REACHED` | — | Report date range extends earlier than the Free plan's 3-month history window |

### Validation (400)

| Code | Meaning |
|------|---------|
| `VALIDATION_FAILED` | Request body or query parameters failed validation. `details` contains field-level messages |
| `INVALID_TRANSACTION_CURRENCY` | Transaction currency does not match the account currency |
| `ACCOUNT_MUTATION_NOT_ALLOWED` | Attempt to change account type or currency after the account has been used |
| `ACCOUNT_INACTIVE` | Account is soft-deleted or inactive |
| `INVALID_OCR_FILE` | Uploaded file exceeds size limit or has unsupported format |
| `INVALID_IMPORT_FILE` | CSV file cannot be parsed |
| `INVALID_BUDGET_DATE_RANGE` | Budget date range parameters are invalid |
| `INVALID_REPORT_DATE_RANGE` | Report date range parameters are invalid |
| `INVALID_NET_WORTH_SNAPSHOT_DATE` | Snapshot date is in the future |
| `INVALID_AUDIT_DATE_RANGE` | Audit date range parameters are invalid |
| `INVALID_TRANSFER` | Transfer source and destination are the same account or currencies do not match |
| `INSUFFICIENT_BALANCE` | Account balance is insufficient for the requested transfer |
| `INVALID_PASSWORD` | Password does not meet strength requirements |
| `PASSWORD_REUSE_NOT_ALLOWED` | Password matches a previously used password |
| `INVALID_RECURRING_TRANSACTION_NEXT_RUN_DATE` | Next run date is in the past |
| `INVALID_RECURRING_TRANSACTION_TYPE` | Invalid frequency or classification |
| `INVALID_RECURRING_TRANSACTION_STATUS_TRANSITION` | Cannot transition to the requested status |
| `SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED` | Attempt to modify or delete a system category |
| `INVALID_GOAL_DATE` | Goal date parameters are invalid |
| `INVALID_GOAL_STATUS_TRANSITION` | Cannot transition goal to the requested status |
| `INVALID_GOAL_TYPE` | Invalid goal type identifier |
| `INVALID_GOAL_SIMULATION_REQUEST` | Goal simulation request failed validation |
| `OCR_PROCESSING_FAILED` | OCR engine encountered an unrecoverable error |
| `INVALID_STOCK_SYMBOL` | Stock symbol or stock query parameter is invalid |

### Not Found (404)

| Code | Meaning |
|------|---------|
| `USER_NOT_FOUND` | User resource not found |
| `TRANSACTION_NOT_FOUND` | Transaction not found or not owned by the caller |
| `ACCOUNT_NOT_FOUND` | Account not found or not owned by the caller |
| `CATEGORY_NOT_FOUND` | Category not found or not owned by the caller |
| `BUDGET_NOT_FOUND` | Budget not found or not owned by the caller |
| `RECURRING_TRANSACTION_NOT_FOUND` | Recurring transaction not found or not owned by the caller |
| `RECURRING_TRANSACTION_DEPENDENCY_NOT_FOUND` | Referenced account or category not found |
| `NOTIFICATION_NOT_FOUND` | Notification not found or not owned by the caller |
| `IMPORT_NOT_FOUND` | Import job not found or not owned by the caller |
| `OCR_JOB_NOT_FOUND` | OCR job not found or not owned by the caller |
| `INSIGHT_NOT_FOUND` | Insight not found or not owned by the caller |
| `GOAL_NOT_FOUND` | Goal not found or not owned by the caller |
| `SCENARIO_NOT_FOUND` | Scenario not found or not owned by the caller |
| `ASSISTANT_CHAT_SESSION_NOT_FOUND` | Chat session not found or expired |
| `AUDIT_LOG_NOT_FOUND` | Audit entry not found or not owned by the caller |
| `LINKED_ACCOUNT_NOT_FOUND` | Linked account not found or not owned by the caller |
| `STOCK_QUOTE_NOT_AVAILABLE` | Stock provider returned no usable data for the requested symbol or indicator |
| `STOCK_HOLDING_NOT_FOUND` | Stock holding not found or not owned by the caller |

### Conflict (409)

| Code | Meaning |
|------|---------|
| `EMAIL_ALREADY_EXISTS` | Registration email is already in use |
| `CATEGORY_NAME_ALREADY_EXISTS` | Category name conflicts with an existing category |
| `ACCOUNT_NAME_ALREADY_EXISTS` | Account name conflicts with an existing or soft-deleted account |
| `BUDGET_ALREADY_EXISTS` | Budget already exists for the given category and period |
| `IMPORT_ALREADY_RUNNING` | An import is already in progress |
| `DUPLICATE_STOCK_HOLDING` | A holding for this symbol and purchase date already exists |

### Rate Limited (429)

| Code | Meaning |
|------|---------|
| `RATE_LIMITED` | Too many requests. Check `Retry-After` header |
| `STOCK_RATE_LIMIT_EXCEEDED` | App-side stock quota exceeded before calling Alpha Vantage |

### Feature Disabled (503)

| Code | Meaning |
|------|---------|
| `ASSISTANT_DISABLED` | Assistant feature is not enabled |
| `STOCK_DISABLED` | Stock feature is disabled or Alpha Vantage API key is missing |
| `REVENUECAT_DISABLED` | RevenueCat integration is disabled or the secret API key is missing |

## 5xx Server Errors

| Code | HTTP | Meaning |
|------|------|---------|
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error. Check server logs |
| `ASSISTANT_PROCESSING_FAILED` | 502 | AI provider returned an error or response could not be parsed |
| `INSIGHT_GENERATION_FAILED` | 500 | Insight generation job failed |
| `STOCK_PROVIDER_ERROR` | 502 | Alpha Vantage returned an error or note response, or the transport failed |
| `REVENUECAT_PROVIDER_ERROR` | 502 | RevenueCat returned an error or the transport failed |

## Validation Details Format

When `VALIDATION_FAILED` is returned, the `details` array contains field-level messages:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": [
      "amount: must not be null",
      "currency: must be a valid ISO-4217 currency code"
    ]
  }
}
```

## Retry-After Header

Rate-limited responses include the `Retry-After` header:

```text
Retry-After: 60
```

The value is an integer number of seconds. Clients should wait at least this long before retrying.

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/shared/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` handling all exception types |
| `src/main/java/com/saveapenny/shared/api/ApiError.java` | Error envelope model |
| `src/main/java/com/saveapenny/shared/api/ApiResponse.java` | Response envelope model |
