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
- `GET /api/v1/reports/category-spending?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/cash-flow?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/reports/net-worth?snapshotDate=YYYY-MM-DD`

Report module uses query-based aggregation from existing account/transaction/category data and returns the standard API envelope.

Report module flag: `COMPLETE` (repository/dto/exception/mapper/service/controller/shared integration/tests/docs).

## Configuration
Set the following environment variables before running the app:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (a strong secret key, at least 64 characters for HS512)

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
