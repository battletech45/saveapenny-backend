# Environment Variables Reference

## Overview

The application loads environment variables from a `.env` file (via `spring.config.import=optional:file:.env[.properties]`) and standard system environment variables. Variables are grouped by concern below.

## Required

| Variable | Description | Validation |
|----------|-------------|------------|
| `DB_USERNAME` | PostgreSQL application user | Must have schema permissions |
| `DB_PASSWORD` | PostgreSQL password | — |
| `JWT_SECRET` | HS512 signing key | Minimum 64 characters; generate with `openssl rand -base64 64` |

## Application

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP server listen port |
| `SPRING_PROFILES_ACTIVE` | — | Active Spring profile(s), comma-separated |

## Database

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/saveapenny` | JDBC connection URL |
| `DB_USERNAME` | — | Database user |
| `DB_PASSWORD` | — | Database password |

## JWT

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | — | HS512 signing key, 64+ characters |
| `SECURITY_JWT_REFRESH_TOKEN_EXPIRY_DAYS` | `7` | Refresh token validity in days |

See [Auth Flow](auth-flow.md) for token lifecycle details.

## CORS

| Variable | Default | Description |
|----------|---------|-------------|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated list of allowed origins. Empty = no CORS headers |

## Rate Limiting

| Variable | Default | Description |
|----------|---------|-------------|
| `RATE_LIMIT_LOGIN_MAX_PER_MINUTE` | `5` | Login POST requests per minute per IP |
| `RATE_LIMIT_API_MAX_PER_MINUTE` | `60` | API POST requests per minute per user |

See [Rate Limiting](rate-limiting.md) for algorithm details and client best practices.

## Assistant (AI Chat)

| Variable | Default | Description |
|----------|---------|-------------|
| `ASSISTANT_ENABLED` | `false` | Enable the AI assistant |
| `ASSISTANT_AI_PROVIDER` | `openrouter` | Provider: `openrouter` or `openai` |
| `ASSISTANT_MODEL` | `cohere/north-mini-code:free` | AI model identifier |
| `OPENROUTER_API_KEY` | — | OpenRouter API key |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api` | OpenRouter base URL |
| `OPENROUTER_SITE_URL` | — | Optional `HTTP-Referer` sent to OpenRouter for attribution |
| `OPENROUTER_APP_NAME` | `SaveAPenny` | Optional `X-Title` sent to OpenRouter for attribution |
| `OPENAI_API_KEY` | — | OpenAI API key (when using OpenAI provider) |

See [Assistant](features/assistant.md) for feature details.

## OCR

| Variable | Default | Description |
|----------|---------|-------------|
| `OCR_ENABLED` | `true` | Enable OCR receipt processing |
| `OCR_TESSDATA_PATH` | auto-detected | Override Tesseract `tessdata` directory |

Additional OCR properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `ocr.language` | `eng` | Tesseract language pack |
| `ocr.max-file-size-bytes` | `10485760` | Max upload size (10 MB) |
| `ocr.job-timeout-millis` | `30000` | Per-job timeout (30s) |
| `ocr.max-retries` | `2` | Retry count on failure |

See [OCR](features/ocr.md) for feature details.

## Stocks

| Variable | Default | Description |
|----------|---------|-------------|
| `STOCK_ENABLED` | `true` | Enable stock market endpoints |
| `ALPHA_VANTAGE_API_KEY` | — | Alpha Vantage API key used for all stock requests |

Additional stock properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `stock.base-url` | `https://www.alphavantage.co` | Base URL for Alpha Vantage |
| `stock.rate-limit-per-minute` | `5` | App-side per-minute stock quota |
| `stock.rate-limit-per-day` | `25` | App-side per-day stock quota |

Notes:

- `stock.enabled=true` alone is not sufficient for live calls; a blank `ALPHA_VANTAGE_API_KEY` causes stock requests to return `STOCK_DISABLED`.
- Stock endpoints are read-only and authenticated.

See [Stocks](features/stocks.md) for feature details.

## Billing (RevenueCat)

| Variable | Default | Description |
|----------|---------|-------------|
| `REVENUECAT_ENABLED` | `false` | Enable RevenueCat entitlement sync |
| `REVENUECAT_SECRET_API_KEY` | — | RevenueCat secret API key, required when enabled |
| `REVENUECAT_BASE_URL` | `https://api.revenuecat.com/v1` | RevenueCat REST API base URL (override for testing) |
| `REVENUECAT_ENTITLEMENT_IDENTIFIER` | `Save A Penny Pro` | RevenueCat entitlement **identifier** (not the display name) that maps to the app's `PLUS` plan |

Notes:

- `REVENUECAT_ENABLED=true` alone is not sufficient; a blank `REVENUECAT_SECRET_API_KEY` causes `/billing/sync` to fail with `REVENUECAT_DISABLED`.
- No webhook receiver exists — RevenueCat webhooks require a paid plan this project isn't on. Entitlement state is pull-only: the backend re-fetches `/subscribers/{appUserId}` whenever `POST /billing/sync` is called, plus a local time-based downgrade (`BillingEntitlement.effectiveStatus`) so a lapsed subscription can't stay "active" forever between syncs. The Flutter client should call `/billing/sync` after purchase/restore and on app launch/resume.
- The RevenueCat `appUserID` must always be the backend user UUID, never the user's email.

See [Billing](features/billing.md) for feature details.

## Firebase Analytics

| Variable | Default | Description |
|----------|---------|-------------|
| `FIREBASE_ANALYTICS_ENABLED` | `false` | Enable server-side event dispatch to Firebase/GA4 |
| `FIREBASE_ANDROID_APP_ID` | — | Firebase App ID for the Android app, required when enabled |
| `FIREBASE_ANDROID_API_SECRET` | — | Measurement Protocol API secret for the Android data stream, required when enabled |
| `FIREBASE_IOS_APP_ID` | — | Firebase App ID for the iOS app, required when enabled |
| `FIREBASE_IOS_API_SECRET` | — | Measurement Protocol API secret for the iOS data stream, required when enabled |
| `FIREBASE_ANALYTICS_VALIDATE_ONLY` | `false` | Route events to the DebugView endpoint instead of recording them; use in staging |

Additional analytics properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `firebase.analytics.endpoint` | `https://www.google-analytics.com/mp/collect` | Production Measurement Protocol endpoint |
| `firebase.analytics.debug-endpoint` | `https://www.google-analytics.com/debug/mp/collect` | DebugView validation endpoint |
| `firebase.analytics.timeout-millis` | `2000` | HTTP timeout; failures are logged and swallowed, never surfaced to the caller |

Notes:

- When disabled (default), a no-op publisher is wired in and no HTTP calls are made.
- Two separate Firebase apps (Android/iOS) require two separate credential pairs — a single App ID can't be used for both. The client must send `X-Client-Platform: android` or `X-Client-Platform: ios` so the backend picks the right pair; requests without it (e.g. events fired from scheduled jobs with no HTTP context) are dropped rather than sent under the wrong platform's credentials.
- Correlates to the mobile app's Firebase Installations ID via the `X-Analytics-Client-Id` request header.

See [Firebase Analytics](features/firebase-analytics.md) for feature details.

## Insights

| Variable | Default | Description |
|----------|---------|-------------|
| `INSIGHT_ENABLED` | `false` | Enable financial insight generation |
| `INSIGHT_MODEL` | `poolside/laguna-xs.2:free` | AI model for enhanced insights |
| `INSIGHT_AI_PROVIDER` | `openrouter` | AI provider for enhanced insights |

Additional insight properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `insight.max-insights-per-generation` | `10` | Max insights per scheduled run |
| `insight.deduplication-window-days` | `7` | Suppress duplicate insights within N days |
| `insight.stddev-threshold` | `3.0` | Anomaly detection sensitivity |
| `insight.max-amount-ratio` | `0.5` | Max ratio for amount comparisons |
| `insight.ai-enhanced` | `false` | Rewrite insight text with AI after rule-based generation |
| `insight.cron` | `0 30 6 * * *` | Schedule for the daily insight-generation job |

See [Insights](features/insights.md) for feature details.

## Goals

| Variable | Default | Description |
|----------|---------|-------------|
| `GOAL_PROGRESS_ENABLED` | `false` | Enable scheduled goal progress checks |

Additional goal properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `goal.progress.off-track-ratio` | `0.10` | Threshold for OFF_TRACK status |
| `goal.progress.at-risk-ratio` | `0.05` | Threshold for AT_RISK status |
| `goal.progress.off-track-persistence-months` | `2` | Months before OFF_TRACK notification |
| `goal.progress.cron` | `0 0 6 * * *` | Schedule for the daily goal progress job |

See [Goals](features/goals.md) for feature details.

## Automation (Recurring Transactions)

| Property | Default | Description |
|----------|---------|-------------|
| `automation.recurring.cron` | `0 */5 * * * *` | Schedule for processing due recurring transactions |

See [Recurring Transactions](features/recurring-transactions.md) for feature details.

## Docker Compose

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_DB` | `saveapenny` | PostgreSQL database name |
| `POSTGRES_USER` | `saveapenny_app` | PostgreSQL admin user |
| `POSTGRES_PASSWORD` | — | PostgreSQL admin password |

## .env File Examples

### Minimal

```env
POSTGRES_DB=saveapenny
POSTGRES_USER=saveapenny_app
POSTGRES_PASSWORD=change_me
DB_USERNAME=saveapenny_app
DB_PASSWORD=change_me
JWT_SECRET=your-64-plus-character-hs512-secret-key-goes-here
```

### With Optional Features

```env
# Core
POSTGRES_DB=saveapenny
POSTGRES_USER=saveapenny_app
POSTGRES_PASSWORD=change_me
DB_USERNAME=saveapenny_app
DB_PASSWORD=change_me
JWT_SECRET=your-64-plus-character-hs512-secret-key-goes-here

# Assistant
ASSISTANT_ENABLED=true
ASSISTANT_AI_PROVIDER=openrouter
OPENROUTER_API_KEY=sk-or-v1-...

# Insights
INSIGHT_ENABLED=true
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| `.env` file support via `spring.config.import` | Familiar, portable, works with Docker Compose and local dev |
| Environment variables over config files for secrets | Keeps secrets out of version control, works with container orchestration |
| Sensible defaults for all optional variables | Application runs without configuration for basic use cases |
