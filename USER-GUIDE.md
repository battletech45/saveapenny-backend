# SaveAPenny User Guide

## What SaveAPenny Is

SaveAPenny is a personal finance backend for tracking:

- accounts
- income and expense transactions
- transfers between accounts
- budgets
- reports and spending summaries
- recurring transactions
- notifications
- CSV imports
- OCR receipt/document imports
- audit history
- an AI assistant focused on finance and savings

This repository currently provides the backend API. There is no built-in web UI in this project.

## Who This Guide Is For

Use this guide if you want to:

- run the backend locally
- configure the required environment variables
- test the API with Swagger, cURL, or Postman
- understand the main usage flows
- use the AI assistant endpoint

## What You Need

### Required

- Java 24
- Maven 3.9+
- PostgreSQL 16+

### Optional

- Docker and Docker Compose
- Tesseract OCR if you want OCR import features
- OpenAI API key if you want the AI assistant enabled

## Configuration

Create a local `.env` file using `.env.example` as a starting point.

Required values:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

Recommended database values for local Docker/Postgres:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

Assistant-related values:

- `ASSISTANT_ENABLED=true` to enable the AI assistant
- `OPENAI_API_KEY=<your-key>` when assistant is enabled

Example:

```env
POSTGRES_DB=saveapenny
POSTGRES_USER=saveapenny_app
POSTGRES_PASSWORD=change_me_local_only
DB_USERNAME=saveapenny_app
DB_PASSWORD=change_me_local_only
JWT_SECRET=change_me_to_a_64_plus_char_secret_for_hs512_signing_key
ASSISTANT_ENABLED=true
OPENAI_API_KEY=your_openai_api_key
```

## Running the Project

### Option 1: Run with Docker Compose

This is the easiest way to start the backend and PostgreSQL together.

```bash
docker compose up --build
```

What this does:

- starts PostgreSQL
- builds the Spring Boot app
- exposes the app on `http://localhost:8080`

Health check:

```bash
curl http://localhost:8080/actuator/health
```

### Option 2: Run Locally with Maven

1. Start PostgreSQL yourself.
2. Make sure your `.env` file is present.
3. Run:

```bash
mvn spring-boot:run
```

The app will start on:

- `http://localhost:8080`

## API Documentation

After startup, use Swagger UI:

- `http://localhost:8080/swagger-ui.html`

OpenAPI JSON:

- `http://localhost:8080/v3/api-docs`

Swagger is the easiest way to explore and manually test the endpoints.

## First-Time Usage Flow

The normal usage order is:

1. register or log in
2. create accounts
3. create or review categories
4. add transactions
5. create budgets
6. use reports
7. optionally use recurring transactions, imports, OCR, notifications, and the assistant

## Core API Workflows

### 1. Register

Endpoint:

- `POST /api/v1/auth/register`

### 2. Log In

Endpoint:

- `POST /api/v1/auth/login`

After login, use:

- `Authorization: Bearer <accessToken>`

for protected routes.

### 3. Create an Account

Endpoint:

- `POST /api/v1/accounts`

Examples:

- cash wallet
- bank account
- savings account
- credit account

### 4. Add Categories

Endpoint:

- `POST /api/v1/categories`

System categories are also available automatically.

### 5. Add Transactions

Endpoints:

- `POST /api/v1/transactions`
- `POST /api/v1/transactions/transfer`

Use these for:

- salary/income entries
- expense tracking
- moving money between accounts

### 6. Create Budgets

Endpoint:

- `POST /api/v1/budgets`

Check budget status with:

- `GET /api/v1/budgets/{budgetId}/status`

### 7. View Reports

Endpoints:

- `GET /api/v1/reports/monthly-summary`
- `GET /api/v1/reports/category-spending`
- `GET /api/v1/reports/cash-flow`
- `GET /api/v1/reports/net-worth`

Use reports for:

- income vs expense review
- top categories
- cash flow trends
- net worth snapshot

## AI Assistant Usage

Assistant endpoint:

- `POST /api/v1/assistant/chat`

Assistant capabilities:

- general savings guidance
- finance-focused chat behavior
- tool-backed answers using your reports, budgets, and recent transactions
- persisted session continuity through `sessionId`

### Enable the Assistant

Set:

- `ASSISTANT_ENABLED=true`
- `OPENAI_API_KEY=<your-key>`

If disabled, the endpoint will return:

- `503 ASSISTANT_DISABLED`

### Start a New Chat Session

Request:

```json
{
  "message": "How can I save more this month?"
}
```

Response includes a `sessionId`:

```json
{
  "success": true,
  "data": {
    "sessionId": "11111111-1111-1111-1111-111111111111",
    "reply": "You can start by setting a category cap for variable spending and reviewing your top expense categories.",
    "disclaimer": "This assistant provides general budgeting guidance, not financial, tax, or legal advice."
  },
  "error": null,
  "timestamp": "2026-06-02T10:30:00Z"
}
```

### Continue an Existing Session

Request:

```json
{
  "sessionId": "11111111-1111-1111-1111-111111111111",
  "message": "Which categories are hurting me the most?"
}
```

Notes:

- if you omit `history`, the backend uses persisted session history
- if you send `history`, that request history is used directly
- unsupported history roles are ignored

### Good Assistant Questions

- "How did I do this month?"
- "Which categories are over budget?"
- "Why is my cash flow negative?"
- "What recent spending should I reduce first?"
- "Give me 3 ways to save more based on my activity"

## CSV Import Usage

Endpoints:

- `POST /api/v1/imports/transactions/preview`
- `POST /api/v1/imports/transactions/confirm`
- `GET /api/v1/imports/transactions/{importId}/status`

Typical flow:

1. upload a CSV for preview
2. review validation issues
3. confirm the import
4. check status until completed

## OCR Import Usage

Endpoints:

- `POST /api/imports/ocr`
- `GET /api/imports/ocr/{jobId}`

Supported file types:

- PNG
- JPEG
- PDF

### OCR Requirements

OCR requires Tesseract.

On macOS:

```bash
brew install tesseract
```

Verify tessdata exists:

```bash
ls /opt/homebrew/share/tessdata
```

## Common Problems

### Assistant returns `503 ASSISTANT_DISABLED`

Cause:

- `ASSISTANT_ENABLED` is not `true`

### Assistant returns processing error

Cause examples:

- missing `OPENAI_API_KEY`
- invalid OpenAI key
- upstream provider issue

### App cannot connect to database

Check:

- PostgreSQL is running
- `DB_USERNAME` and `DB_PASSWORD` match the database
- database URL points to the right host/port

### OCR endpoints fail

Check:

- Tesseract is installed
- tessdata path exists
- OCR is enabled

## Recommended Ways to Explore the Project

1. Start with Swagger UI.
2. Register a user.
3. Create one account.
4. Add a few expense and income transactions.
5. Create a monthly budget.
6. Open reports.
7. Ask the assistant questions about your finances.

## Related Documents

- `README.md`
- `api-contract.md`
- `assistant-technical-doc.md`
- `assistant-implementation-plan.md`
