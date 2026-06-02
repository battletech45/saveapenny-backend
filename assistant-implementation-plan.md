# Assistant Implementation Plan

## Goal

Add a new AI assistant module to the Spring Boot backend with the following phase 1 scope:

- one new `assistant` module
- one synchronous chat endpoint
- one finance/savings system prompt
- one provider first, behind Spring AI abstractions
- no DB persistence in phase 1
- no vector store or RAG in phase 1
- optional tool-calling in phase 2 using existing `budget`, `report`, and `transaction` services

## Current Project Fit

The current backend already follows a feature-oriented package structure with separate modules such as `budget`, `report`, `transaction`, `imports`, and `ocr`.

Relevant patterns already in use:

- controllers under `feature/controller`
- service contracts under `feature/service`
- service implementations under `feature/service/impl`
- DTOs under `feature/dto`
- authenticated endpoints using `@AuthenticationPrincipal CurrentUserPrincipal`
- endpoint security using `@PreAuthorize("isAuthenticated()")`
- standard API envelope using `ApiResponse<T>`
- centralized security configuration in `config/security/SecurityConfig.java`
- centralized exception handling in `shared/exception/GlobalExceptionHandler.java`

The assistant should follow the same structure rather than being mixed into `report`, `notification`, or `shared`.

## Phase 1 Scope

Phase 1 delivers a stateless assistant endpoint that:

- accepts a user message
- optionally accepts recent chat history from the request body
- prepends a fixed finance/savings system prompt
- calls one hosted model through Spring AI
- returns a concise assistant reply in the existing API response envelope

Phase 1 explicitly does not include:

- database persistence for sessions or messages
- vector database
- retrieval-augmented generation
- raw transaction dumps sent to the model
- tool/function calling
- investment, tax, or legal advisory behavior

## Provider Choice

Phase 1 should start with OpenAI through Spring AI.

Reasoning:

- fast to integrate
- good model quality for conversational finance guidance
- supported cleanly by Spring AI abstractions
- allows later provider swapping with minimal service-layer change

If provider choice changes later, the assistant module should keep the application code behind Spring AI interfaces instead of provider-specific SDK code.

## Dependency Plan

Update `pom.xml` to add Spring AI BOM and one provider starter.

Planned additions:

- Spring AI BOM in `dependencyManagement`
- `org.springframework.ai:spring-ai-starter-model-openai`

Do not add in phase 1:

- vector store starters
- embedding model starters
- tool-specific libraries beyond what Spring AI already provides

## Configuration Plan

Extend `src/main/resources/application.yml` with both provider config and assistant-specific config.

Planned properties:

```yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-mini

assistant:
  enabled: true
  max-history: 8
  system-prompt: |
    You are SaveAPenny's finance and savings assistant.
    Give practical, concise budgeting and savings guidance.
    Focus on spending awareness, budgeting discipline, cash flow habits,
    and sustainable savings behavior.
    Do not present yourself as a financial advisor.
    Do not provide tax, legal, or investment advice.
    If the user asks for risky or regulated advice, clearly state the limitation
    and redirect to general budgeting guidance.
```

Notes:

- use environment variable `OPENAI_API_KEY`
- keep the model configurable through YAML
- keep the system prompt configurable so wording can evolve without code changes
- defer user finance context injection to phase 2 when compact summaries or tool-calling are introduced

## Module Structure

The assistant module should use this package layout:

```text
src/main/java/com/saveapenny/
├── assistant/
│   ├── controller/
│   │   └── AssistantController.java
│   ├── service/
│   │   ├── AssistantService.java
│   │   └── impl/
│   │       └── AssistantServiceImpl.java
│   ├── dto/
│   │   ├── AssistantChatRequest.java
│   │   ├── AssistantChatResponse.java
│   │   └── AssistantMessageDto.java
│   ├── config/
│   │   └── AssistantProperties.java
│   ├── prompt/
│   │   └── FinancePromptBuilder.java
│   ├── tool/
│   │   ├── AssistantBudgetTool.java
│   │   ├── AssistantReportTool.java
│   │   └── AssistantTransactionTool.java
│   └── exception/
│       ├── AssistantDisabledException.java
│       └── AssistantProcessingException.java
```

Phase 1 implementation rule:

- create `tool/` package now only if you want the package reserved
- do not implement tool-calling classes yet

## Test Structure [Completed]

Planned test locations:

```text
src/test/java/com/saveapenny/assistant/
├── controller/
│   └── AssistantControllerTest.java
├── service/
│   └── impl/
│       └── AssistantServiceImplTest.java
```

Optional later tests:

- security integration test for authenticated access
- provider failure mapping test
- history truncation test

## Binary Tree: Structure

```text
AI Assistant
├── Reuse Existing Patterns?
│   ├── Yes
│   │   ├── New top-level module: `assistant`
│   │   └── Follow same layering: controller -> service -> dto/config/exception
│   └── No
│       ├── Mix into `report` or `notification`
│       └── Not recommended: assistant is cross-domain
└── Persist Chat Data Now?
    ├── No
    │   ├── Stateless endpoint
    │   └── Faster MVP, fewer schema changes
    └── Yes
        ├── Add `assistant_chat_session` / `assistant_chat_message`
        └── Better for history/audit, but more work
```

## Binary Tree: Implementation

```text
Implementation
├── Phase 1: MVP Chat
│   ├── Provider integration only?
│   │   ├── Yes
│   │   │   ├── Add Spring AI starter
│   │   │   ├── Create `AssistantController`
│   │   │   ├── Create `AssistantService`
│   │   │   ├── Inject `ChatClient`
│   │   │   └── Use fixed finance/savings system prompt
│   │   └── No
│   │       ├── Add tool-calling immediately
│   │       └── More value, but more moving parts
│   └── Include user app data?
│       ├── No
│       │   ├── Generic savings coach
│       │   └── Lowest privacy risk
│       └── Yes
│           ├── Inject small summaries only
│           └── Better answers, but must scope carefully
└── Phase 2: Smart Assistant
    ├── Prompt-only context?
    │   ├── Yes
    │   │   ├── Pull monthly summary/budget status
    │   │   └── Add to prompt as compact text
    │   └── No
    │       ├── Use Spring AI tools/function calling
    │       └── Let model request report/budget/transaction data on demand
    └── Need long-term memory/search?
        ├── No
        │   ├── Stay with service-backed tools
        │   └── Likely enough for this app
        └── Yes
            ├── Add vector store / RAG later
            └── Useful for docs/help, less necessary for transactional finance data
```

## File-by-File Implementation Plan

### 1. `pom.xml` [Completed]

Add:

- Spring AI BOM
- OpenAI model starter dependency

Outcome:

- Spring AI beans become available for injection
- provider details stay outside business logic

### 2. `src/main/resources/application.yml` [Completed]

Add:

- `spring.ai.openai.api-key`
- `spring.ai.openai.chat.options.model`
- `assistant.enabled`
- `assistant.max-history`
- `assistant.system-prompt`

Outcome:

- assistant behavior can be managed without changing code

### 3. `assistant/config/AssistantProperties.java` [Completed]

Responsibility:

- bind `assistant.*` config properties
- expose flags and limits to service layer

Expected fields:

- `boolean enabled`
- `int maxHistory`
- `String systemPrompt`

Implementation notes:

- use `@ConfigurationProperties(prefix = "assistant")`
- validate reasonable defaults if needed
- no extra registration is needed because `@ConfigurationPropertiesScan` is already enabled in `SaveAPennyApplication`

### 4. `assistant/dto/AssistantMessageDto.java` [Completed]

Responsibility:

- represent one chat message from request history

Expected fields:

- `String role`
- `String content`

Validation rules:

- role required
- content required
- unsupported roles are accepted at validation time and ignored during prompt construction

### 5. `assistant/dto/AssistantChatRequest.java` [Completed]

Responsibility:

- request body for `POST /api/v1/assistant/chat`

Expected fields:

- `String message`
- `List<AssistantMessageDto> history`

Validation rules:

- `message` required and non-blank
- apply a reasonable maximum length
- `history` optional
- use `assistant.max-history` as the single source of truth for history count
- truncate history in service if it exceeds the configured max count

### 6. `assistant/dto/AssistantChatResponse.java` [Completed]

Responsibility:

- response payload inside `ApiResponse`

Expected fields:

- `String reply`
- `String disclaimer`

### 7. `assistant/service/AssistantService.java` [Completed]

Responsibility:

- define assistant chat contract

Suggested method:

```java
AssistantChatResponse chat(UUID userId, AssistantChatRequest request);
```

### 8. `assistant/prompt/FinancePromptBuilder.java` [Completed]

Responsibility:

- build the system prompt and full chat prompt payload cleanly
- keep prompt assembly logic out of the controller

Phase 1 behavior:

- use configured `assistant.system-prompt`
- include disclaimer-oriented constraints
- include only request message and supported history
- ignore unsupported history roles during prompt construction

Future behavior:

- optionally append compact summaries from `report` and `budget`

### 9. `assistant/service/impl/AssistantServiceImpl.java` [Completed]

Responsibility:

- check feature flag
- sanitize and trim history
- build prompt through `FinancePromptBuilder`
- call Spring AI `ChatClient`
- map provider failures to domain-specific exceptions

Dependencies:

- `ChatClient` or Spring AI chat model abstraction
- `AssistantProperties`
- `FinancePromptBuilder`

Phase 1 behavior:

- no database access
- no service-backed tools yet
- no user finance context in phase 1; add it in phase 2 with compact summaries or tool-calling

Error handling:

- throw `AssistantDisabledException` when disabled
- wrap provider errors in `AssistantProcessingException`

### 10. `assistant/controller/AssistantController.java` [Completed]

Responsibility:

- expose synchronous chat endpoint

Endpoint:

- `POST /api/v1/assistant/chat`

Security and controller style should match existing modules:

- `@RestController`
- `@RequestMapping("/api/v1/assistant")`
- `@PreAuthorize("isAuthenticated()")`
- `@AuthenticationPrincipal CurrentUserPrincipal principal`

Response style:

- `ResponseEntity<ApiResponse<AssistantChatResponse>>`

Controller behavior:

- validate request
- resolve current user id
- delegate to `AssistantService`
- wrap result in `ApiResponse.success(...)`
- keep the endpoint registered even when the feature is disabled so requests return `503 ASSISTANT_DISABLED` instead of `404`

### 11. `assistant/exception/AssistantDisabledException.java` [Completed]

Responsibility:

- indicate assistant feature is disabled by config

Expected HTTP mapping:

- `503 Service Unavailable` or `403 Forbidden`

Recommended choice:

- `503 Service Unavailable`, because this is a feature availability issue

### 12. `assistant/exception/AssistantProcessingException.java` [Completed]

Responsibility:

- wrap model/provider failures

Expected HTTP mapping:

- `502 Bad Gateway` or `500 Internal Server Error`

Recommended choice:

- `502 Bad Gateway`, because the failure comes from an upstream AI provider

### 13. `shared/exception/GlobalExceptionHandler.java` [Completed]

Add handlers for:

- `AssistantDisabledException`
- `AssistantProcessingException`
- request validation failures if assistant DTO constraints are added

Planned error codes:

- `ASSISTANT_DISABLED`
- `ASSISTANT_PROCESSING_FAILED`

## Endpoint Contract

### Request

`POST /api/v1/assistant/chat`

```json
{
  "message": "How can I save more this month?",
  "history": [
    {
      "role": "user",
      "content": "I overspent on food."
    },
    {
      "role": "assistant",
      "content": "Let's review your spending habits."
    }
  ]
}
```

### Response

```json
{
  "success": true,
  "data": {
    "reply": "You can start by reducing variable food expenses and setting a weekly cap.",
    "disclaimer": "This assistant provides general budgeting guidance, not financial, tax, or legal advice."
  }
}
```

## Finance Prompt Rules

The phase 1 system prompt should enforce these rules:

- focus on budgeting, savings, and spending discipline
- keep answers practical and concise
- avoid regulated advice categories
- avoid pretending to know user-specific finances unless context was intentionally provided
- recommend review of actual reports or budgets when user-specific conclusions cannot be safely made
- avoid hallucinating account balances, spending totals, or trends

## Validation and Guardrails

Phase 1 should include basic request guardrails:

- reject blank messages
- cap message length
- cap history count using `assistant.max-history`
- ignore unsupported history roles
- avoid passing unbounded user text to provider

Operational guardrails:

- do not log raw prompts at info level
- avoid storing conversation history in DB in phase 1
- keep provider secrets only in environment config

## Security Plan

The endpoint should follow the same security pattern as other authenticated modules.

Requirements:

- authenticated user required
- resolve `UUID userId` from `CurrentUserPrincipal`
- never trust user-submitted ids for assistant ownership or scope
- do not expose assistant endpoint as public in `SecurityConfig`

Phase 1 note:

- the user id is mainly for ownership context and future extensibility, even if no DB persistence exists yet

## Testing Plan

### Controller tests

Create `AssistantControllerTest.java` to verify:

- authenticated request returns `200`
- request validation failures return `400`
- service result is wrapped in `ApiResponse.success(...)`
- disabled assistant returns `503` with `ASSISTANT_DISABLED`

### Service tests

Create `AssistantServiceImplTest.java` to verify:

- disabled assistant throws `AssistantDisabledException`
- history is trimmed to configured max
- unsupported history roles are ignored
- system prompt is included
- provider exceptions are wrapped in `AssistantProcessingException`

### Security/auth behavior

Verify:

- unauthenticated access is rejected
- authenticated principal is required for controller flow

## Implementation Sequence

1. [x] Add Spring AI BOM and OpenAI starter to `pom.xml`.
2. [x] Add assistant and provider configuration to `application.yml`.
3. [x] Create `AssistantProperties` and register configuration properties binding.
4. [x] Create assistant DTOs.
5. [x] Create `AssistantService` contract.
6. [x] Create `FinancePromptBuilder`.
7. [x] Create `AssistantServiceImpl` using Spring AI `ChatClient`.
8. [x] Create `AssistantController` at `POST /api/v1/assistant/chat`.
9. [x] Add assistant-specific exceptions.
10. [x] Extend `GlobalExceptionHandler` with assistant mappings.
11. [x] Add controller and service tests.
12. [x] Run focused tests, then full test suite if needed.

## Bug TODOs

- [x] Fix disabled-mode behavior mismatch: `assistant.enabled=false` currently removes the endpoint and returns `404`, but the intended contract and exception mapping expect `503 ASSISTANT_DISABLED`.
- [x] Either implement `assistant.allow-user-finance-context` in the assistant flow or remove/defer the property until phase 2, because it is currently configured but unused.
- [x] Align history limit behavior so there is a single source of truth: `AssistantChatRequest.history` has a hard `@Size(max = 20)` limit while runtime trimming uses `assistant.max-history`.
- [x] Align unsupported role handling with the intended design: the current DTO validation rejects non-`user|assistant` roles with `400`, while the plan says unsupported roles should be ignored.
- [x] Replace or refine the placeholder OpenAI API key fallback and startup workaround so non-AI environments remain safe without relying on a fake key value.

## Phase 2 Upgrade Path

After phase 1 is stable, add service-backed assistant tools around existing modules.

## Remaining Implementation Roadmap

Phase 1 is complete. The remaining work is phase 2 and later enhancements.

Completed after phase 2 core:

- backend tool-calling over report, budget, and transaction assistant tools
- persisted assistant chat sessions and messages
- assistant API documentation updates in `README.md` and `api-contract.md`

### Phase 2 Goal

Make the assistant answer user-specific finance questions safely by using existing backend services instead of relying only on prompt text.

### Phase 2A: Service-Backed Tools

Create the following tool classes under `assistant/tool/`:

- `AssistantBudgetTool.java` [Completed]
- `AssistantReportTool.java` [Completed]
- `AssistantTransactionTool.java` [Completed]

Recommended first methods:

- `AssistantBudgetTool`
  - `getBudgetStatus(...)`
- `AssistantReportTool`
  - `getMonthlySummary(...)`
  - `getCategorySpending(...)`
  - `getCashFlow(...)`
- `AssistantTransactionTool`
  - `getRecentTransactions(...)`

Implementation rules:

- always derive user scope from authenticated context
- never accept arbitrary user ids from the request body for tool access
- prefer compact structured outputs over raw entity dumps

### Phase 2B: Tool Calling Integration

Add assistant tool-calling support through Spring AI.

Status: [Completed]

Recommended steps:

1. add assistant config flags for tool calling
2. register assistant tools in assistant AI configuration
3. update `AssistantServiceImpl` to use tool-aware chat flow when enabled
4. keep the existing system prompt and disclaimer behavior

Recommended config additions:

- `assistant.tool-calling-enabled`
- optional future per-tool toggles if needed

Implemented outcome:

- assistant tool-calling is now wired through Spring AI over:
  - `AssistantReportTool`
  - `AssistantBudgetTool`
  - `AssistantTransactionTool`
- authenticated user scope is passed through an assistant tool context holder
- unsupported roles are ignored before the model request is built

### Phase 2C: Prompt and Context Rules

When tool calling is added:

- keep the system prompt concise
- instruct the assistant to use tools for account-specific facts
- do not inject large raw finance payloads into prompts by default
- do not let the model invent balances, totals, or trends

Preferred strategy:

- use tools for factual user data
- use prompt text for behavior, tone, and safety boundaries

### Phase 2D: Testing

Add tests for the next implementation stage:

- tool unit tests for each assistant tool class
- service tests for tool-enabled assistant responses
- failure-path tests for tool exceptions and upstream provider issues
- ownership-scope tests to verify tool access stays bound to authenticated user context
- integration tests for user questions such as:
  - "Which categories are over budget?"
  - "Why is my cash flow negative?"
  - "How did I do this month?"

### Phase 3 Options

Optional later enhancements:

- compact finance context injection in addition to tools
- documentation/help RAG
- frontend chat UI integration

Completed later enhancements:

- chat/session persistence

Frontend note:

- this repository currently contains backend code only, so frontend chat UI integration could not be implemented here

### Recommended Order

Implement remaining work in this order:

1. `AssistantReportTool`
2. `AssistantBudgetTool`
3. tests for tool-backed assistant responses
4. `AssistantTransactionTool`
5. tool-calling wiring in assistant AI configuration and `AssistantServiceImpl`
6. optional frontend integration

Completed so far:

- `AssistantReportTool`
- `AssistantBudgetTool`
- `AssistantTransactionTool`
- tool-calling wiring in assistant AI configuration and `AssistantServiceImpl`
- tests for tool-backed assistant responses
- persisted chat sessions and messages via `sessionId`
- assistant endpoint documentation in `README.md` and `api-contract.md`

### First Concrete Deliverable

The best next implementation slice is:

1. `AssistantReportTool.getMonthlySummary(...)` [Completed]
2. `AssistantReportTool.getCategorySpending(...)` [Completed]
3. `AssistantBudgetTool.getBudgetStatus(...)` [Completed via monthly budget context]
4. connect those tools to the assistant chat flow [Completed]

This enables valuable questions immediately:

- "Which categories are over budget?"
- "Why is my cash flow negative?"
- "How did I do this month?"

Recommended first integrations:

- `BudgetService.getStatus(...)`
- `ReportService.getMonthlySummary(...)`
- `ReportService.getCategorySpending(...)`
- transaction lookup flow for recent spending patterns

Phase 2 options:

- prompt-only compact summaries
- Spring AI tool/function calling with explicit methods

Preferred direction:

- use Spring AI tool-calling rather than dumping large raw finance data into prompts

Why:

- better accuracy
- lower token usage
- clearer authorization boundaries
- easier future auditability

## Out of Scope

Do not implement in phase 1:

- vector DB
- embeddings
- long-term memory
- session persistence tables
- auto-analysis jobs
- advisor-like recommendations on investments, taxes, or legal matters

## Deliverable Summary

Phase 1 is complete when the backend has:

- Spring AI configured with one provider
- an `assistant` module aligned with existing project structure
- a secured `POST /api/v1/assistant/chat` endpoint
- a configurable finance/savings system prompt
- a stateless synchronous response flow
- controller and service tests covering the main path and core failures
