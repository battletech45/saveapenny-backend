# SaveAPenny API Endpoints (v1)

Base URL: `/api/v1`

Protected routes require:

`Authorization: Bearer <accessToken>`

## Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
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
    "details": ["email: must be a well-formed email address"]
  },
  "timestamp": "2026-05-12T10:30:00Z"
}
```

## Auth Endpoints

### POST `/auth/register`

Request:

```json
{
  "email": "john.doe@example.com",
  "password": "strong-pass-123",
  "fullName": "John Doe"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-access-token>",
    "refreshToken": "<refresh-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `409` `EMAIL_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### POST `/auth/login`

Request:

```json
{
  "email": "john.doe@example.com",
  "password": "strong-pass-123"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-access-token>",
    "refreshToken": "<refresh-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `INVALID_CREDENTIALS`
- `400` `VALIDATION_FAILED`

### POST `/auth/refresh`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<new-jwt-access-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `INVALID_REFRESH_TOKEN`
- `401` `REFRESH_TOKEN_EXPIRED`
- `400` `VALIDATION_FAILED`

### POST `/auth/logout`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

## User Endpoints

### GET `/users/me`

Headers:

`Authorization: Bearer <accessToken>`

Response `200`:

```json
{
  "success": true,
  "data": {
    "id": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "active": true,
    "createdAt": "2026-05-11T15:13:00Z",
    "updatedAt": "2026-05-12T09:20:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `USER_NOT_FOUND`

### PUT `/users/me`

Headers:

`Authorization: Bearer <accessToken>`

Request:

```json
{
  "fullName": "John A. Doe"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "id": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
    "email": "john.doe@example.com",
    "fullName": "John A. Doe",
    "active": true,
    "createdAt": "2026-05-11T15:13:00Z",
    "updatedAt": "2026-05-12T10:28:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `USER_NOT_FOUND`
- `400` `VALIDATION_FAILED`

### PUT `/users/me/password`

Headers:

`Authorization: Bearer <accessToken>`

Request:

```json
{
  "currentPassword": "strong-pass-123",
  "newPassword": "new-strong-pass-456"
}
```

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `USER_NOT_FOUND`
- `400` `INVALID_PASSWORD`
- `400` `PASSWORD_REUSE_NOT_ALLOWED`
- `400` `VALIDATION_FAILED`

## Account Endpoints

All account endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/accounts`

Request:

```json
{
  "name": "Wallet",
  "type": "CASH",
  "currency": "USD",
  "initialBalance": 250.0000
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "id": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
    "name": "Wallet",
    "type": "CASH",
    "currency": "USD",
    "balance": 250.0000,
    "initialBalance": 250.0000,
    "active": true,
    "createdAt": "2026-05-12T13:00:00Z",
    "updatedAt": "2026-05-12T13:00:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T13:00:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `409` `ACCOUNT_NAME_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### GET `/accounts`

Supports `page`, `size`, `sort` query params via Spring `Pageable`.

Response `200`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
        "name": "Wallet",
        "type": "CASH",
        "currency": "USD",
        "balance": 250.0000,
        "initialBalance": 250.0000,
        "active": true,
        "createdAt": "2026-05-12T13:00:00Z",
        "updatedAt": "2026-05-12T13:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "error": null,
  "timestamp": "2026-05-12T13:00:00Z"
}
```

### GET `/accounts/{accountId}`

Response `200`: same `AccountResponse` payload as create/update.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `ACCOUNT_NOT_FOUND`

## Category Endpoints

All category endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/categories`

Request:

```json
{
  "name": "Food",
  "type": "EXPENSE",
  "color": "#ff0000",
  "icon": "utensils"
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "id": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
    "userId": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
    "name": "Food",
    "type": "EXPENSE",
    "color": "#ff0000",
    "icon": "utensils",
    "createdAt": "2026-05-12T14:45:00Z",
    "updatedAt": "2026-05-12T14:45:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T14:45:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `409` `CATEGORY_NAME_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### GET `/categories?type=INCOME|EXPENSE`

Returns system categories (`userId=null`) plus categories owned by the current user.

Response `200`:

```json
{
  "success": true,
  "data": [
    {
      "id": "f3672791-2ca5-4b5f-aaf4-46de79f4b324",
      "userId": null,
      "name": "Salary",
      "type": "INCOME",
      "color": "#00aa00",
      "icon": "briefcase",
      "createdAt": "2026-05-01T10:00:00Z",
      "updatedAt": "2026-05-01T10:00:00Z"
    },
    {
      "id": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
      "userId": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
      "name": "Food",
      "type": "EXPENSE",
      "color": "#ff0000",
      "icon": "utensils",
      "createdAt": "2026-05-12T14:45:00Z",
      "updatedAt": "2026-05-12T14:45:00Z"
    }
  ],
  "error": null,
  "timestamp": "2026-05-12T14:45:00Z"
}
```

### GET `/categories/{categoryId}`

Response `200`: same `CategoryResponse` payload as create.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`

### PUT `/categories/{categoryId}`

Request:

```json
{
  "name": "Groceries",
  "type": "EXPENSE",
  "color": "#00ff00",
  "icon": "basket"
}
```

Response `200`: same `CategoryResponse` payload as create.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`
- `409` `CATEGORY_NAME_ALREADY_EXISTS`
- `400` `SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED`
- `400` `VALIDATION_FAILED`

### DELETE `/categories/{categoryId}`

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T14:45:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`
- `400` `SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED`

## Transaction Endpoints

All transaction endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/transactions`

Request:

```json
{
  "accountId": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
  "categoryId": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
  "type": "EXPENSE",
  "amount": 120.0000,
  "currency": "USD",
  "description": "Groceries",
  "transactionDate": "2026-05-12"
}
```

Response `201`: `TransactionResponse` envelope.

Common errors:

- `401` `ACCESS_DENIED`
- `400` `INSUFFICIENT_BALANCE`
- `400` `INVALID_TRANSFER`
- `400` `VALIDATION_FAILED`

### POST `/transactions/transfer`

Request:

```json
{
  "fromAccountId": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
  "toAccountId": "4e5e3fc6-35d4-4139-b705-efb285cc3f1f",
  "categoryId": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
  "amount": 50.0000,
  "currency": "USD",
  "description": "Move funds",
  "transactionDate": "2026-05-12"
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "transactionId": "b8da0e18-c8be-4f37-aea8-c18318f0f4d2",
    "fromAccountId": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
    "toAccountId": "4e5e3fc6-35d4-4139-b705-efb285cc3f1f",
    "categoryId": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
    "amount": 50.0000,
    "currency": "USD",
    "description": "Move funds",
    "transactionDate": "2026-05-12",
    "createdAt": "2026-05-12T18:10:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T18:10:00Z"
}
```

Common errors:

- `400` `INVALID_TRANSFER`
- `400` `INSUFFICIENT_BALANCE`

### GET `/transactions`

Supports filters: `from`, `to`, `type`, `accountId`, `categoryId`, `minAmount`, `maxAmount`, `keyword`, `page`, `size`, `sort`.

Response `200`: paged `TransactionResponse` envelope.

### GET `/transactions/{transactionId}`

Response `200`: `TransactionResponse` envelope.

Common errors:

- `404` `TRANSACTION_NOT_FOUND`

### PUT `/transactions/{transactionId}`

Request format same as create transaction.

Response `200`: updated `TransactionResponse` envelope.

Common errors:

- `404` `TRANSACTION_NOT_FOUND`
- `400` `INVALID_TRANSFER`
- `400` `INSUFFICIENT_BALANCE`

### DELETE `/transactions/{transactionId}`

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T18:10:00Z"
}
```

Common errors:

- `404` `TRANSACTION_NOT_FOUND`

### PUT `/accounts/{accountId}`

Request:

```json
{
  "name": "Main Wallet",
  "type": "BANK",
  "currency": "EUR"
}
```

Response `200`: same `AccountResponse` payload as create.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `ACCOUNT_NOT_FOUND`
- `409` `ACCOUNT_NAME_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### DELETE `/accounts/{accountId}`

Soft deletes account (`active=false`).

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T13:00:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `ACCOUNT_NOT_FOUND`

## Budget Endpoints

All budget endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/budgets`

Request:

```json
{
  "categoryId": "2dc59e57-bf15-4c21-9dd0-2e14ca3ecf85",
  "amount": 400.0000,
  "period": "MONTHLY",
  "startDate": "2026-05-01",
  "endDate": "2026-05-31"
}
```

Response `201`: `BudgetResponse` envelope.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`
- `409` `BUDGET_ALREADY_EXISTS`
- `400` `INVALID_BUDGET_DATE_RANGE`
- `400` `VALIDATION_FAILED`

### GET `/budgets`

Supports `period=MONTHLY|YEARLY` and `page`, `size`, `sort` query params.

Response `200`: paged `BudgetResponse` envelope.

### GET `/budgets/{budgetId}`

Response `200`: `BudgetResponse` envelope.

Common errors:

- `404` `BUDGET_NOT_FOUND`

### GET `/budgets/{budgetId}/status`

Response `200`:

```json
{
  "success": true,
  "data": {
    "category": "Food",
    "budgetAmount": 400.0000,
    "spentAmount": 275.0000,
    "remainingAmount": 125.0000,
    "usagePercentage": 68.75,
    "status": "ON_TRACK"
  },
  "error": null,
  "timestamp": "2026-05-12T18:30:00Z"
}
```

Status values:

- `ON_TRACK` (`< 80%`)
- `WARNING` (`>= 80%` and `<= 100%`)
- `EXCEEDED` (`> 100%`)

Common errors:

- `404` `BUDGET_NOT_FOUND`

### PUT `/budgets/{budgetId}`

Request format same as create budget.

Response `200`: updated `BudgetResponse` envelope.

Common errors:

- `404` `BUDGET_NOT_FOUND`
- `404` `CATEGORY_NOT_FOUND`
- `409` `BUDGET_ALREADY_EXISTS`
- `400` `INVALID_BUDGET_DATE_RANGE`
- `400` `VALIDATION_FAILED`

### DELETE `/budgets/{budgetId}`

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T18:30:00Z"
}
```

Common errors:

- `404` `BUDGET_NOT_FOUND`

## Report Endpoints

All report endpoints require:

`Authorization: Bearer <accessToken>`

### GET `/reports/monthly-summary`

Query params:

- `from` (required, `yyyy-MM-dd`)
- `to` (required, `yyyy-MM-dd`)

Response `200`:

```json
{
  "success": true,
  "data": {
    "startDate": "2026-05-01",
    "endDate": "2026-05-31",
    "totalIncome": 1500.0000,
    "totalExpense": 620.0000,
    "netSavings": 880.0000
  },
  "error": null,
  "timestamp": "2026-05-12T20:00:00Z"
}
```

Common errors:

- `400` `INVALID_REPORT_DATE_RANGE`

### GET `/reports/monthly-summary/export`

Query params:

- `from` (required, `yyyy-MM-dd`)
- `to` (required, `yyyy-MM-dd`)

Response `200`:

- Content-Type: `text/csv`
- Content-Disposition: `attachment; filename="monthly-summary-<from>-to-<to>.csv"`

CSV body:

```csv
startDate,endDate,totalIncome,totalExpense,netSavings
2026-05-01,2026-05-31,1500.0000,620.0000,880.0000
```

Common errors:

- `400` `INVALID_REPORT_DATE_RANGE`

### GET `/reports/category-spending`

Query params:

- `from` (required, `yyyy-MM-dd`)
- `to` (required, `yyyy-MM-dd`)

Response `200`:

```json
{
  "success": true,
  "data": [
    {
      "categoryId": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
      "categoryName": "Food",
      "totalAmount": 300.0000,
      "usagePercentage": 48.39
    }
  ],
  "error": null,
  "timestamp": "2026-05-12T20:00:00Z"
}
```

Common errors:

- `400` `INVALID_REPORT_DATE_RANGE`

### GET `/reports/cash-flow`

Query params:

- `from` (required, `yyyy-MM-dd`)
- `to` (required, `yyyy-MM-dd`)

Response `200`:

```json
{
  "success": true,
  "data": [
    {
      "date": "2026-05-10",
      "incomeAmount": 500.0000,
      "expenseAmount": 120.0000,
      "netAmount": 380.0000
    }
  ],
  "error": null,
  "timestamp": "2026-05-12T20:00:00Z"
}
```

Common errors:

- `400` `INVALID_REPORT_DATE_RANGE`

### GET `/reports/net-worth`

Query params:

- `snapshotDate` (required, `yyyy-MM-dd`, cannot be future date)

Response `200`:

```json
{
  "success": true,
  "data": {
    "snapshotDate": "2026-05-12",
    "totalAssets": 5200.0000,
    "totalLiabilities": 1100.0000,
    "netWorth": 4100.0000
  },
  "error": null,
  "timestamp": "2026-05-12T20:00:00Z"
}
```

Common errors:

- `400` `INVALID_NET_WORTH_SNAPSHOT_DATE`

## Automation Endpoints

All automation endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/automations/recurring-transactions`

Request:

```json
{
  "accountId": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
  "categoryId": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
  "type": "EXPENSE",
  "amount": 100.0000,
  "frequency": "MONTHLY",
  "nextRunDate": "2026-06-01"
}
```

Response `201`: `RecurringTransactionResponse` envelope.

Common errors:

- `400` `INVALID_RECURRING_TRANSACTION_TYPE`
- `400` `INVALID_RECURRING_TRANSACTION_NEXT_RUN_DATE`
- `404` `RECURRING_TRANSACTION_DEPENDENCY_NOT_FOUND`

### GET `/automations/recurring-transactions`

Supports `page`, `size`, `sort` query params via Spring `Pageable`.

Response `200`: paged `RecurringTransactionResponse` envelope.

### GET `/automations/recurring-transactions/{recurringTransactionId}`

Response `200`: `RecurringTransactionResponse` envelope.

Common errors:

- `404` `RECURRING_TRANSACTION_NOT_FOUND`

### PUT `/automations/recurring-transactions/{recurringTransactionId}`

Request:

```json
{
  "accountId": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
  "categoryId": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
  "type": "INCOME",
  "amount": 250.0000,
  "frequency": "WEEKLY",
  "nextRunDate": "2026-06-05",
  "active": true
}
```

Response `200`: updated `RecurringTransactionResponse` envelope.

Common errors:

- `404` `RECURRING_TRANSACTION_NOT_FOUND`
- `404` `RECURRING_TRANSACTION_DEPENDENCY_NOT_FOUND`
- `400` `INVALID_RECURRING_TRANSACTION_TYPE`
- `400` `INVALID_RECURRING_TRANSACTION_NEXT_RUN_DATE`

### DELETE `/automations/recurring-transactions/{recurringTransactionId}`

Soft deletes recurring transaction (`active=false`).

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-13T12:00:00Z"
}
```

Common errors:

- `404` `RECURRING_TRANSACTION_NOT_FOUND`

Automation execution behavior:

- A background scheduler periodically processes active recurring transactions with `nextRunDate <= today`.
- Execution is lock-protected (distributed advisory lock) to avoid duplicate generation across concurrent nodes.
- On successful generation, `nextRunDate` is advanced by frequency (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`).

## Notification Endpoints

All notification endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/notifications`

Request:

```json
{
  "type": "SYSTEM",
  "title": "Welcome",
  "message": "Your account has been created."
}
```

Response `201`: `NotificationResponse` envelope.

Common errors:

- `400` `VALIDATION_FAILED`

### GET `/notifications?read=true|false`

Supports `read` filter and `page`, `size`, `sort` query params.

Response `200`: paged `NotificationResponse` envelope.

### GET `/notifications/{notificationId}`

Response `200`: `NotificationResponse` envelope.

Common errors:

- `404` `NOTIFICATION_NOT_FOUND`

### PUT `/notifications/{notificationId}`

Request:

```json
{
  "read": true
}
```

Response `200`: updated `NotificationResponse` envelope.

Common errors:

- `404` `NOTIFICATION_NOT_FOUND`
- `400` `VALIDATION_FAILED`

### DELETE `/notifications/{notificationId}`

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-13T12:30:00Z"
}
```

Common errors:

- `404` `NOTIFICATION_NOT_FOUND`

### GET `/notifications/unread-count`

Response `200`:

```json
{
  "success": true,
  "data": {
    "unreadCount": 3
  },
  "error": null,
  "timestamp": "2026-05-13T12:30:00Z"
}
```

### PATCH `/notifications/mark-all-read`

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-13T12:30:00Z"
}
```

## Import Endpoints

All import endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/imports/transactions/preview`

Request: `multipart/form-data` with file part named `file` (`.csv` only).

Response `201`:

```json
{
  "success": true,
  "data": {
    "importId": "f6f2ddca-cd29-4f11-a877-fefa093f0f3e",
    "fileName": "transactions.csv",
    "totalRows": 2,
    "validRows": 1,
    "invalidRows": 1,
    "errors": [
      {
        "rowNumber": 3,
        "errorMessage": "Amount is invalid",
        "rawData": "EXPENSE,2026-05-02,not-a-number,USD,a2,c2"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-05-14T11:00:00Z"
}
```

Common errors:

- `400` `INVALID_IMPORT_FILE`

### POST `/imports/transactions/confirm`

Request:

```json
{
  "importId": "f6f2ddca-cd29-4f11-a877-fefa093f0f3e"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "importId": "f6f2ddca-cd29-4f11-a877-fefa093f0f3e",
    "status": "RUNNING",
    "totalRows": 2,
    "importedRows": 0,
    "failedRows": 1,
    "createdAt": "2026-05-14T11:00:00Z",
    "updatedAt": "2026-05-14T11:00:02Z"
  },
  "error": null,
  "timestamp": "2026-05-14T11:00:02Z"
}
```

Common errors:

- `404` `IMPORT_NOT_FOUND`
- `409` `IMPORT_ALREADY_RUNNING`
- `400` `VALIDATION_FAILED`

### GET `/imports/transactions/{importId}/status`

Response `200`: same payload shape as confirm response.

Behavior notes:

- Duplicate rows are marked as `SKIPPED` during async confirm processing.
- Duplicate detection key: hash of `(account_id, amount, date, description)`.

Status values:

- Import: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`
- Import row: `VALID`, `IMPORTED`, `FAILED`, `SKIPPED`

Common errors:

- `404` `IMPORT_NOT_FOUND`

## OCR Import Endpoints

All OCR import endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/imports/ocr`

Consumes `multipart/form-data` with `file`.

Allowed content types:

- `image/png`
- `image/jpeg`
- `application/pdf`

Response `202`:

```json
{
  "success": true,
  "data": {
    "jobId": "2c31230f-5f77-49d2-b113-7abf8eb20b49",
    "status": "PENDING"
  },
  "error": null,
  "timestamp": "2026-05-21T10:30:00Z"
}
```

Common errors:

- `400` `INVALID_OCR_FILE`

### GET `/imports/ocr/{jobId}`

Response `200`:

```json
{
  "success": true,
  "data": {
    "jobId": "2c31230f-5f77-49d2-b113-7abf8eb20b49",
    "status": "COMPLETED",
    "originalFileName": "receipt.png",
    "errorMessage": null,
    "resultSnippet": "SAVEAPENNY RECEIPT TOTAL 19.99",
    "rawText": "SAVEAPENNY RECEIPT\nTOTAL 19.99",
    "transactionCandidates": [
      {
        "date": "2026-05-20",
        "amount": 19.99,
        "description": "2026-05-20 market 19.99",
        "categoryHint": "FOOD"
      }
    ],
    "createdAt": "2026-05-21T10:29:58Z",
    "updatedAt": "2026-05-21T10:30:01Z"
  },
  "error": null,
  "timestamp": "2026-05-21T10:30:01Z"
}
```

Common errors:

- `404` `OCR_JOB_NOT_FOUND`
- `400` `OCR_PROCESSING_FAILED`

## Audit Endpoints

All audit endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/audits`

Request:

```json
{
  "action": "UPDATE",
  "entityType": "ACCOUNT",
  "entityId": "11111111-1111-1111-1111-111111111111",
  "oldValue": "Wallet",
  "newValue": "Wallet Main"
}
```

Response `201`: `AuditLogResponse` envelope.

Common errors:

- `400` `VALIDATION_FAILED`

### GET `/audits`

Supports optional filters:

- `entityType`
- `entityId`
- `from` (ISO datetime)
- `to` (ISO datetime)

Also supports `page`, `size`, `sort` via `Pageable`.

Response `200`: paged `AuditLogResponse` envelope.

Common errors:

- `400` `INVALID_AUDIT_DATE_RANGE`

### GET `/audits/{auditLogId}`

Response `200`: `AuditLogResponse` envelope.

Common errors:

- `404` `AUDIT_LOG_NOT_FOUND`
- `403` `AUDIT_LOG_ACCESS_DENIED`

## Quick cURL

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"strong-pass-123"}'
```

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Wallet","type":"CASH","currency":"USD","initialBalance":250.0000}'
```

```bash
curl -X GET "http://localhost:8080/api/v1/categories?type=EXPENSE" \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<account-id>","categoryId":"<category-id>","type":"EXPENSE","amount":120.0000,"currency":"USD","description":"Groceries","transactionDate":"2026-05-12"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/budgets \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"categoryId":"<category-id>","amount":400.0000,"period":"MONTHLY","startDate":"2026-05-01","endDate":"2026-05-31"}'
```

```bash
curl "http://localhost:8080/api/v1/budgets/<budget-id>/status" \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl -X POST http://localhost:8080/api/v1/imports/transactions/preview \
  -H "Authorization: Bearer <access-token>" \
  -F "file=@transactions.csv"
```

```bash
curl -X POST http://localhost:8080/api/v1/imports/transactions/confirm \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"importId":"<import-id>"}'
```

```bash
curl "http://localhost:8080/api/v1/imports/transactions/<import-id>/status" \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl -X POST http://localhost:8080/api/imports/ocr \
  -H "Authorization: Bearer <access-token>" \
  -F "file=@receipt.png"
```

```bash
curl "http://localhost:8080/api/imports/ocr/<job-id>" \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl -X POST http://localhost:8080/api/v1/audits \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"action":"UPDATE","entityType":"ACCOUNT","entityId":"11111111-1111-1111-1111-111111111111","oldValue":"Wallet","newValue":"Wallet Main"}'
```

```bash
curl "http://localhost:8080/api/v1/audits?entityType=ACCOUNT" \
  -H "Authorization: Bearer <access-token>"
```
