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
| `DB_URL` | `jdbc:postgresql://localhost:5432/saveapenny` | JDBC connection URL |
| `DB_USERNAME` | — | Database user |
| `DB_PASSWORD` | — | Database password |

## JWT

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | — | HS512 signing key, 64+ characters |
| `JWT_REFRESH_EXPIRY_DAYS` | `7` | Refresh token validity in days |

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
| `ASSISTANT_MODEL` | `poolside/laguna-xs.2:free` | AI model identifier |
| `OPENROUTER_API_KEY` | — | OpenRouter API key |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api` | OpenRouter base URL |
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

See [Goals](features/goals.md) for feature details.

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
