# Error Codes

All errors return HTTP 4xx or 5xx with a consistent JSON envelope:

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

| Code | Meaning |
|------|---------|
| `ACCESS_DENIED` | Missing, invalid, or expired access token |
| `INVALID_CREDENTIALS` | Email or password is incorrect |
| `INVALID_REFRESH_TOKEN` | Refresh token is invalid, expired, or revoked |
| `ACCOUNT_LOCKED` | Account is temporarily locked due to suspicious activity |

### Validation (400)

| Code | Meaning |
|------|---------|
| `VALIDATION_FAILED` | Request body or query params failed validation. `details` contains field-level messages |
| `INVALID_TRANSACTION_CURRENCY` | Transaction currency does not match the account currency |
| `ACCOUNT_MUTATION_NOT_ALLOWED` | Attempt to change account type or currency after the account has been used |
| `INVALID_OCR_FILE` | Uploaded file is too large or has an unsupported format |
| `INVALID_CSV_FORMAT` | CSV file cannot be parsed |
| `INVALID_DATE_RANGE` | Date range parameters are invalid |

### Not Found (404)

| Code | Meaning |
|------|---------|
| `USER_NOT_FOUND` | User resource not found |
| `ACCOUNT_NOT_FOUND` | Account not found or not owned by the caller |
| `CATEGORY_NOT_FOUND` | Category not found or not owned by the caller |
| `TRANSACTION_NOT_FOUND` | Transaction not found or not owned by the caller |
| `BUDGET_NOT_FOUND` | Budget not found or not owned by the caller |
| `RECURRING_TRANSACTION_NOT_FOUND` | Recurring transaction not found or not owned by the caller |
| `NOTIFICATION_NOT_FOUND` | Notification not found or not owned by the caller |
| `IMPORT_NOT_FOUND` | Import job not found or not owned by the caller |
| `OCR_JOB_NOT_FOUND` | OCR job not found or not owned by the caller |
| `INSIGHT_NOT_FOUND` | Insight not found or not owned by the caller |
| `GOAL_NOT_FOUND` | Goal not found or not owned by the caller |
| `SCENARIO_NOT_FOUND` | Scenario not found or not owned by the caller |

### Conflict (409)

| Code | Meaning |
|------|---------|
| `EMAIL_ALREADY_EXISTS` | Registration email is already in use |
| `CATEGORY_ALREADY_EXISTS` | Category name conflicts with an existing category |
| `ACCOUNT_ALREADY_EXISTS` | Account name conflicts with an existing or deleted account |
| `BUDGET_ALREADY_EXISTS` | Budget already exists for the given period |

### Rate Limited (429)

| Code | Meaning |
|------|---------|
| `RATE_LIMITED` | Too many requests. Check `Retry-After` header |

### Feature Disabled (503)

| Code | Meaning |
|------|---------|
| `ASSISTANT_DISABLED` | Assistant feature is not enabled |
| `OCR_DISABLED` | OCR feature is not enabled |

## 5xx Server Errors

| Code | HTTP | Meaning |
|------|------|---------|
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error. Check server logs |

## Validation Details Format

When `VALIDATION_FAILED` is returned, `details` contains field-level messages:

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

The value is in seconds. Mobile clients should wait at least this long before retrying.
