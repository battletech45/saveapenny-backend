# Assistant Technical Documentation

## Overview

The assistant module adds an authenticated AI chat capability focused on personal finance and savings guidance.

Current capabilities:

- synchronous chat endpoint
- finance and savings system prompting
- Spring AI OpenAI integration
- backend tool-calling for user-specific financial facts
- persisted chat sessions and messages

The assistant is backend-only in this repository.

## Endpoint

- `POST /api/v1/assistant/chat`

Authentication:

- requires `Authorization: Bearer <accessToken>`

Request fields:

- `sessionId` optional
- `message` required
- `history` optional

Session behavior:

- omit `sessionId` to create a new persisted session
- provide `sessionId` to continue an existing owned session
- if `history` is omitted, persisted session history is loaded automatically

## Module Layout

```text
com.saveapenny.assistant
├── config
├── controller
├── dto
├── entity
├── exception
├── prompt
├── repository
├── service
└── tool
```

Key classes:

- `AssistantController`
- `AssistantService`
- `AssistantServiceImpl`
- `AssistantAiConfig`
- `FinancePromptBuilder`
- `AssistantReportTool`
- `AssistantBudgetTool`
- `AssistantTransactionTool`
- `AssistantToolContextHolder`

## Runtime Flow

1. request reaches `AssistantController`
2. authenticated user id is resolved from `CurrentUserPrincipal`
3. `AssistantServiceImpl` checks feature availability
4. session is created or loaded
5. request history or persisted session history is prepared
6. system prompt is built
7. Spring AI `ChatClient` executes the request with registered tools
8. assistant reply is returned and both user/assistant messages are persisted

## Tool-Calling Model

The assistant uses Spring AI tool-calling instead of injecting large raw financial payloads into prompts.

Registered tools:

- `AssistantReportTool`
  - current month summary
  - top spending categories
- `AssistantBudgetTool`
  - monthly budget status
- `AssistantTransactionTool`
  - recent transactions from the last 30 days

Tool scoping rule:

- tools never accept arbitrary user ids from the request body
- tools resolve the authenticated user through `AssistantToolContextHolder`

## Persistence Model

### Tables

- `assistant_chat_sessions`
- `assistant_chat_messages`

### Session fields

- `id`
- `user_id`
- `title`
- `created_at`
- `updated_at`

### Message fields

- `id`
- `session_id`
- `role`
- `content`
- `created_at`

Supported persisted message roles:

- `user`
- `assistant`

## Configuration

Main keys:

- `assistant.enabled`
- `assistant.max-history`
- `assistant.system-prompt`
- `spring.ai.openai.api-key`
- `spring.ai.openai.chat.options.model`

Environment variables:

- `ASSISTANT_ENABLED`
- `OPENAI_API_KEY`

Important behavior:

- when `ASSISTANT_ENABLED=false`, the endpoint still exists and returns `503 ASSISTANT_DISABLED`
- when `ASSISTANT_ENABLED=true`, `OPENAI_API_KEY` must be configured

## Error Model

Assistant-specific errors:

- `ASSISTANT_DISABLED`
- `ASSISTANT_PROCESSING_FAILED`
- `ASSISTANT_CHAT_SESSION_NOT_FOUND`

Shared errors still apply:

- `VALIDATION_FAILED`
- `ACCESS_DENIED`

## Validation Rules

- `message` must be non-blank
- message length is capped
- unsupported history roles are ignored
- persisted/request history is trimmed using `assistant.max-history`

## Testing

Assistant-focused test coverage includes:

- controller response and auth behavior
- disabled assistant behavior
- prompt construction rules
- tool registration path
- provider failure wrapping
- session reuse and session-not-found behavior
- assistant tool output formatting

Useful commands:

- `mvn -Dtest=AssistantControllerTest,AssistantServiceImplTest,AssistantReportToolTest,AssistantBudgetToolTest,AssistantTransactionToolTest test`
- `mvn test`

## Known Boundaries

- no frontend/UI in this repository
- no RAG or vector store
- no streaming responses
- no session listing or message retrieval endpoints yet

## Suggested Next Steps

1. add session retrieval/list endpoints
2. add assistant-specific observability and rate limiting
3. add streaming response support if needed
4. integrate a frontend in a separate client application
