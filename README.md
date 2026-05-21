# SaveAPenny
Users can track accounts, transactions, budgets, spending patterns, recurring payments, alerts, and eventually investment/analytics features.

## Auth status
Auth endpoints are available under `/api/v1/auth`:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Protected endpoints require `Authorization: Bearer <accessToken>`.

## Account status
Account endpoints are available under `/api/v1/accounts`:

- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{accountId}`
- `PUT /api/v1/accounts/{accountId}`
- `DELETE /api/v1/accounts/{accountId}`

Account data is ownership-scoped to the authenticated user and uses soft delete (`active=false`).

## Category status
Category endpoints are available under `/api/v1/categories`:

- `POST /api/v1/categories`
- `GET /api/v1/categories?type=INCOME|EXPENSE`
- `GET /api/v1/categories/{categoryId}`
- `PUT /api/v1/categories/{categoryId}`
- `DELETE /api/v1/categories/{categoryId}`

Category visibility includes system categories (`userId=null`) and user-owned categories. System categories cannot be modified or deleted.

## Transaction status
Transaction endpoints are available under `/api/v1/transactions`:

- `POST /api/v1/transactions`
- `POST /api/v1/transactions/transfer`
- `GET /api/v1/transactions`
- `GET /api/v1/transactions/{transactionId}`
- `PUT /api/v1/transactions/{transactionId}`
- `DELETE /api/v1/transactions/{transactionId}`

Transaction module supports income/expense entries, transfer flow, ownership checks, and account balance impact rules.
Transaction list endpoint supports combined filters: `from`, `to`, `type`, `accountId`, `categoryId`, `minAmount`, `maxAmount`, `keyword`, plus pageable params.

## Budget status
Budget endpoints are available under `/api/v1/budgets`:

- `POST /api/v1/budgets`
- `GET /api/v1/budgets?period=MONTHLY|YEARLY`
- `GET /api/v1/budgets/{budgetId}`
- `GET /api/v1/budgets/{budgetId}/status`
- `PUT /api/v1/budgets/{budgetId}`
- `DELETE /api/v1/budgets/{budgetId}`

Budget status returns `budgetAmount`, `spentAmount`, `remainingAmount`, `usagePercentage`, and `status` (`ON_TRACK`, `WARNING`, `EXCEEDED`).

Budget module flag: `COMPLETE` (entity/repository/dto/exception/mapper/service/controller/shared integration/tests/docs).

## Report status
Report endpoints are available under `/api/v1/reports`:

- `GET /api/v1/reports/monthly-summary?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/monthly-summary/export?from=YYYY-MM-DD&to=YYYY-MM-DD` (CSV download)
- `GET /api/v1/reports/category-spending?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/cash-flow?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/net-worth?snapshotDate=YYYY-MM-DD`

Report module uses query-based aggregation from existing account/transaction/category data and returns the standard API envelope.

Report module flag: `PARTIAL` (report JSON endpoints and monthly-summary CSV export are complete; CSV export for category-spending/cash-flow/net-worth is pending).

## Automation status
Recurring transaction endpoints are available under `/api/v1/automations/recurring-transactions`:

- `POST /api/v1/automations/recurring-transactions`
- `GET /api/v1/automations/recurring-transactions`
- `GET /api/v1/automations/recurring-transactions/{recurringTransactionId}`
- `PUT /api/v1/automations/recurring-transactions/{recurringTransactionId}`
- `DELETE /api/v1/automations/recurring-transactions/{recurringTransactionId}`

Automation module manages recurring income/expense definitions, scheduled due-run execution, distributed lock based idempotency, and ownership-scoped soft delete.

Automation module flag: `COMPLETE` (entity/repository/dto/exception/mapper/service/controller/shared tests/docs).

## Notification status
Notification endpoints are available under `/api/v1/notifications`:

- `POST /api/v1/notifications`
- `GET /api/v1/notifications?read=true|false`
- `GET /api/v1/notifications/{notificationId}`
- `PUT /api/v1/notifications/{notificationId}`
- `DELETE /api/v1/notifications/{notificationId}`
- `GET /api/v1/notifications/unread-count`
- `PATCH /api/v1/notifications/mark-all-read`

Notification module supports in-app notifications with read/unread filtering, unread count, and mark-all-read flow.
Event-triggered notifications, email delivery, and user preference management are still pending.

Notification module flag: `PARTIAL` (in-app notification flows complete; event/email/preferences pending).

## Import status
CSV import endpoints are available under `/api/v1/imports/transactions`:

- `POST /api/v1/imports/transactions/preview` (multipart form-data with `file`)
- `POST /api/v1/imports/transactions/confirm`
- `GET /api/v1/imports/transactions/{importId}/status`

Import module supports preview validation, persisted import/import_rows tracking, async confirm processing, and duplicate row skipping by hash of `account_id + amount + date + description`.

Import module statuses:

- Import: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`
- Import row: `VALID`, `IMPORTED`, `FAILED`, `SKIPPED`

Import module flag: `COMPLETE` (entity/repository/dto/exception/mapper/service/controller/shared integration/tests/docs).

## OCR import status
OCR import endpoints are available under `/api/imports/ocr`:

- `POST /api/imports/ocr` (multipart form-data with `file`, returns async job id)
- `GET /api/imports/ocr/{jobId}`

OCR import supports `image/png`, `image/jpeg`, and `application/pdf`, persists raw OCR output in `ocr_jobs`, and returns parsed transaction candidates from extracted text.

OCR job statuses:

- `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`

OCR module flag: `COMPLETE` (async processing, timeout/retry guardrails, validation, health indicator, unit/integration tests, golden regression test).

## Audit status
Audit endpoints are available under `/api/v1/audits`:

- `POST /api/v1/audits`
- `GET /api/v1/audits?entityType=&entityId=&from=&to=`
- `GET /api/v1/audits/{auditLogId}`

Audit module supports ownership-scoped audit entry creation and retrieval with optional entity/date filtering.

Audit module flag: `COMPLETE` (entity/repository/dto/exception/mapper/service/controller/shared integration/tests/docs).

## Configuration
Set the following environment variables before running the app:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (a strong secret key, at least 64 characters for HS512)

## OCR setup (Tess4J + Tesseract)

The project uses `tess4j`, which requires native Tesseract binaries and language data files at runtime.

- Default OCR config is in `src/main/resources/application.yml` under `ocr.*`.
- Important keys:
  - `ocr.enabled`
  - `ocr.tessdata-path`
  - `ocr.language`
  - `ocr.psm`
  - `ocr.max-file-size-bytes`
  - `ocr.job-timeout-millis`
  - `ocr.max-retries`
  - `ocr.debug-logging`

### Local (macOS)

- Install Tesseract: `brew install tesseract`
- Verify install: `tesseract --version`
- Verify tessdata path exists: `ls /opt/homebrew/share/tessdata`
- Ensure `ocr.tessdata-path` matches your machine path.

### CI environment

- Install Tesseract in the CI job before tests/build (example: apt/brew package install step).
- Ensure requested language files exist in tessdata (for default: `eng.traineddata`).
- Set/override `ocr.tessdata-path` for the CI runner filesystem.

### Docker/production

- Install native Tesseract package in the application image.
- Copy or install required `.traineddata` language files.
- Set `ocr.tessdata-path` to the path inside the container (common Linux path: `/usr/share/tesseract-ocr/4.00/tessdata` or distro equivalent).

If Tesseract binary or tessdata files are missing, OCR requests will fail at runtime.

## Health and observability

- Actuator health endpoint: `GET /actuator/health`
- OCR health contributes status details when OCR is enabled.
- OCR processing logs duration and success/failure events; raw extracted text is masked unless `ocr.debug-logging=true`.

## Test commands

- Run full test suite: `mvn test`
- Run auth integration flow only: `mvn -Dtest=AuthFlowIntegrationTest test`
- Run account integration flow only: `mvn -Dtest=AccountFlowIntegrationTest test`
- Run category integration flow only: `mvn -Dtest=CategoryFlowIntegrationTest test`
- Run transaction integration flow only: `mvn -Dtest=TransactionFlowIntegrationTest test`
- Run budget integration flow only: `mvn -Dtest=BudgetFlowIntegrationTest test`
- Run budget controller/service tests only: `mvn -Dtest=BudgetControllerTest,BudgetServiceImplTest test`
- Run report integration flow only: `mvn -Dtest=ReportFlowIntegrationTest test`
- Run report controller/service tests only: `mvn -Dtest=ReportControllerTest,ReportServiceImplTest test`
- Run automation controller/service tests only: `mvn -Dtest=RecurringTransactionControllerTest,RecurringTransactionServiceImplTest,RecurringTransactionExecutionServiceImplTest test`
- Run notification integration flow only: `mvn -Dtest=NotificationFlowIntegrationTest test`
- Run notification controller/service tests only: `mvn -Dtest=NotificationControllerTest,NotificationServiceImplTest test`
- Run import integration flow only: `mvn -Dtest=ImportFlowIntegrationTest test`
- Run import controller/service tests only: `mvn -Dtest=ImportControllerTest,ImportServiceImplTest test`
- Run OCR import integration flow only: `mvn -Dtest=OcrImportFlowIntegrationTest,OcrImportDisabledIntegrationTest test`
- Run OCR service unit tests only: `mvn -Dtest=TesseractOcrServiceTest test`
- Run OCR golden regression test only: `mvn -Dtest=OcrGoldenImageRegressionTest test`
- Run audit integration flow only: `mvn -Dtest=AuditFlowIntegrationTest test`
- Run audit controller/service tests only: `mvn -Dtest=AuditLogControllerTest,AuditLogServiceImplTest test`
