# Insights

## Overview

Financial insights are automatically generated observations about user spending patterns, trends, and anomalies. They are created by a daily scheduled job that analyzes transaction data. It is an optional feature disabled by default.

## Enabling

```yaml
insight:
  enabled: true
```

Or via environment variable:

```env
INSIGHT_ENABLED=true
```

## How Insights Work

A daily scheduled job (`insight.cron`, defaults to `0 30 6 * * *`) analyzes transaction data for all users and generates insights. Each insight has:

| Component | Description |
|-----------|-------------|
| Category | Type of observation (spending trend, budget warning, anomaly) |
| Severity | Relative importance |
| Message | Human-readable observation |
| Supporting data | Quantitative context (amounts, percentages, dates) |

## Detection Methods

| Method | Description |
|--------|-------------|
| **Spending trends** | Compares current period spending to historical averages |
| **Anomaly detection** | Flags transactions deviating > `stddev-threshold` (default 3.0σ) from the mean |
| **Budget warnings** | Alerts when spending approaches or exceeds budget limits |
| **Category concentration** | Identifies over-reliance on specific spending categories |

## AI Enhancement

When `insight.ai-enhanced=true`, the rule-based candidates are sent through an LLM rewrite step that polishes `title`, `summary`, and `detail` while preserving the underlying facts and severity. If the AI call fails, the original rule-based text is used.

## Example Insights

- "You spent 40% more on dining out this month compared to last month"
- "Your grocery spending is consistently 15% above budget"
- "Subscription expenses have increased by $25/month over the last 3 months"
- "Utility spending spiked in January — 2.5× your monthly average"

## Insight Lifecycle

| Status | Description |
|--------|-------------|
| `GENERATED` | Created by the scheduled job |
| `READ` | Marked as seen by the user |
| `DISMISSED` | User has dismissed the insight |

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
| `insight.enabled` | `false` | Enable insight generation |
| `insight.cron` | `0 30 6 * * *` | Cron schedule for generation |
| `insight.max-insights-per-generation` | `10` | Max insights per scheduled run |
| `insight.deduplication-window-days` | `7` | Suppress duplicate insights within N days |
| `insight.stddev-threshold` | `3.0` | Standard deviation threshold for anomaly detection |
| `insight.max-amount-ratio` | `0.5` | Max ratio for amount-based comparisons |
| `insight.ai-enhanced` | `false` | Rewrite generated insight text with AI |
| `insight.provider` | `openrouter` | AI provider for enhancement (`openrouter` or `openai`) |
| `insight.model` | `poolside/laguna-xs.2:free` | Model used for insight enhancement |

## Failure Behavior

If AI enhancement is enabled but the provider is not configured, the model call fails, or the response cannot be parsed, insight generation falls back to the original rule-based candidates. The scheduled generation job still completes.

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `INSIGHT_NOT_FOUND` | 404 | Insight not found or not owned by the caller |
| `INSIGHT_GENERATION_FAILED` | 500 | Scheduled generation job encountered an error |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Rule-based detection as primary method | Works without AI provider; deterministic and predictable |
| AI enhancement as opt-in | Adds polish without depending on external API availability |
| Daily scheduled generation | Balances recency with computational cost |
| Deduplication window | Prevents repetitive or spam-like insights |
| Fallback on AI failure | Generation always completes; AI enhancement is best-effort |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/insight/entity/InsightEntity.java` | JPA entity |
| `src/main/java/com/saveapenny/insight/controller/InsightController.java` | REST endpoints |
| `src/main/java/com/saveapenny/insight/service/impl/InsightGenerationPipeline.java` | Scheduled generation and detection logic |
| `src/main/resources/application.yml` | Insight configuration |
