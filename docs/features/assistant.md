# Assistant (Penny Dog)

## Overview

The assistant provides AI-powered financial guidance through a chat interface. Named "Penny Dog", it uses your financial data (transactions, budgets, goals) to provide context-aware answers. It is an optional feature disabled by default.

## Enabling

```env
ASSISTANT_ENABLED=true
ASSISTANT_AI_PROVIDER=openrouter
OPENROUTER_API_KEY=<your-api-key>
```

Supported providers: `openrouter`, `openai`.

## Provider Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `ASSISTANT_ENABLED` | `false` | Enable the assistant |
| `ASSISTANT_AI_PROVIDER` | `openrouter` | `openrouter` or `openai` |
| `ASSISTANT_MODEL` | `poolside/laguna-xs.2:free` | AI model identifier |
| `OPENROUTER_API_KEY` | — | OpenRouter API key |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api` | OpenRouter base URL |
| `OPENAI_API_KEY` | — | OpenAI API key (when using OpenAI provider) |

## Chat Endpoint

`POST /api/v1/assistant/chat`

```json
{
  "message": "Where am I spending the most this month?",
  "sessionId": "<optional-session-uuid>",
  "history": []
}
```

The assistant uses your financial data to provide context-aware answers. Multi-turn conversations are supported via `sessionId`.

## Capabilities

| Capability | Description |
|-----------|-------------|
| Spending analysis | Recent transactions, spending by category |
| Budget status | Current budget standing and alerts |
| Goal progress | Goal tracking and simulation results |
| Cash flow | Income/expense patterns |
| General advice | Budgeting guidance and savings tips |

## Example Questions

- "Where am I spending the most this month?"
- "Which categories are over budget?"
- "Why is my cash flow negative?"
- "What should I cut first?"
- "How is my emergency fund goal progressing?"
- "How much did I spend on dining out last month?"

## Limitations

| Limitation | Detail |
|-----------|--------|
| Not a financial advisor | Cannot provide tax, legal, or investment advice |
| No external accounts | Cannot execute trades or access external financial institutions |
| Data-dependent | Response quality depends on available transaction data |
| When disabled | Returns `503 ASSISTANT_DISABLED` |

## System Prompt

The assistant uses a fixed system prompt that defines its persona as "Penny Dog," a finance and savings assistant. It is instructed to:

- Give practical, concise budgeting and savings guidance
- Focus on spending awareness, budgeting discipline, and sustainable savings
- Clearly state limitations on risky or regulated advice
- Use goal progress tools when available for goal-related questions

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/assistant/chat` | Send a message and get a response |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `ASSISTANT_DISABLED` | 503 | Assistant feature is not enabled |
| `ASSISTANT_PROCESSING_FAILED` | 502 | AI provider returned an error or response could not be parsed |
| `ASSISTANT_CHAT_SESSION_NOT_FOUND` | 404 | Session ID does not exist or has expired |

## Configuration

```yaml
assistant:
  enabled: ${ASSISTANT_ENABLED:false}
  max-history: 8  # Max messages retained per conversation
  model: ${ASSISTANT_MODEL:poolside/laguna-xs.2:free}
  provider: ${ASSISTANT_AI_PROVIDER:openrouter}
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Backend-mediated AI calls | API keys stay server-side; no browser-to-provider calls |
| Session support for multi-turn | Maintains conversation context across messages |
| Disabled by default | No external API key pressure unless explicitly configured |
| Max history limit | Controls token usage; prevents unbounded context growth |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/assistant/controller/AssistantController.java` | Chat endpoint |
| `src/main/java/com/saveapenny/assistant/service/impl/AssistantServiceImpl.java` | AI provider orchestration |
| `src/main/resources/application.yml` | Assistant configuration |
