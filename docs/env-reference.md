# Environment Variables Reference

## Required

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | PostgreSQL database user |
| `DB_PASSWORD` | PostgreSQL database password |
| `JWT_SECRET` | HS512 signing key, minimum 64 characters |

## Application

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP server port |
| `SPRING_PROFILES_ACTIVE` | — | Active Spring profile(s) |

## Database

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/saveapenny` | JDBC connection URL |
| `DB_USERNAME` | — | Database user |
| `DB_PASSWORD` | — | Database password |

The database URL can be overridden via `spring.datasource.url` if a different host, port, or database name is needed.

## JWT

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | — | HS512 key, 64+ characters. Generate with: `openssl rand -base64 64` |
| `JWT_REFRESH_EXPIRY_DAYS` | `7` | Refresh token validity in days |

See [Auth Flow](auth-flow.md) for token lifecycle details.

## CORS

| Variable | Default | Description |
|----------|---------|-------------|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated list of allowed origins |

See [Deployment & Operations](deployment-operations.md) for CORS behavior.

## Rate Limiting

| Variable | Default | Description |
|----------|---------|-------------|
| `RATE_LIMIT_LOGIN_MAX_PER_MINUTE` | `5` | Login POST requests per minute per IP |
| `RATE_LIMIT_API_MAX_PER_MINUTE` | `60` | API POST requests per minute per user |

See [Rate Limiting](rate-limiting.md) for details.

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
| `OCR_ENABLED` | `false` | Enable OCR receipt processing |

OCR is configured in `application.yml` and does not use environment variables directly. Key settings:

| Property | Default | Description |
|----------|---------|-------------|
| `ocr.tessdata-path` | `/opt/homebrew/share/tessdata` | Path to Tesseract language data |
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
| `insight.ai-enhanced` | `false` | Use AI for insight descriptions |

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

## .env File

Copy `.env.example` to `.env` and fill in the values. The application loads `.env` via `spring.config.import=optional:file:.env[.properties]`.

### Minimal .env

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
```
