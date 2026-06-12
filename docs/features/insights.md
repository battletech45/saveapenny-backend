# Insights

## Overview

Financial insights are generated observations about user spending patterns, trends, and anomalies. They are an optional feature disabled by default.

## Enabling

```yaml
insight:
  enabled: true
  ai-enhanced: true
```

Or via environment variables:

```env
INSIGHT_ENABLED=true
INSIGHT_AI_PROVIDER=openrouter
OPENROUTER_API_KEY=<your-api-key>
```

For OpenAI, set `INSIGHT_AI_PROVIDER=openai` and provide `spring.ai.openai.api-key`.

## How Insights Work

A daily scheduled job analyzes transaction data for all users and generates insights. Each insight has:

- A category (e.g., spending trend, budget warning)
- A severity level
- A human-readable message
- Supporting data

When `insight.ai-enhanced=true`, the rule-based candidates are sent through an LLM rewrite step that polishes `title`, `summary`, and `detail` while preserving the underlying facts and severity.

## Examples

- "You spent 40% more on dining out this month compared to last month"
- "Your grocery spending is consistently 15% above budget"
- "Subscription expenses have increased by $25/month over the last 3 months"

## Insight Lifecycle

| Status | Description |
|--------|-------------|
| Generated | Created by the scheduled job |
| Read | Marked as seen by the user |
| Dismissed | User has dismissed the insight |

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/insights` | List insights (paginated) |
| GET | `/api/v1/insights/{id}` | Get insight details |
| PATCH | `/api/v1/insights/{id}/read` | Mark as read |
| PATCH | `/api/v1/insights/{id}/dismiss` | Dismiss an insight |
| POST | `/api/v1/insights/generate` | Trigger on-demand generation |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `insight.max-insights-per-generation` | 10 | Max insights per run |
| `insight.deduplication-window-days` | 7 | Suppress duplicates within N days |
| `insight.stddev-threshold` | 3.0 | Standard deviation threshold for anomaly detection |
| `insight.max-amount-ratio` | 0.5 | Max ratio for amount-based comparisons |
| `insight.ai-enhanced` | false | Rewrite generated insight text with AI |
| `insight.provider` | `openrouter` | AI provider for insight enhancement (`openrouter` or `openai`) |
| `insight.model` | `poolside/laguna-xs.2:free` | Model used for insight enhancement |
| `insight.openrouter-api-key` | empty | Required when `insight.provider=openrouter` |
| `insight.openrouter-base-url` | `https://openrouter.ai/api` | Base URL for OpenRouter requests |

## Failure Behavior

If AI enhancement is enabled but the client is not configured, the model call fails, or the response cannot be parsed, insight generation falls back to the original rule-based candidates. The scheduled generation job still completes.
