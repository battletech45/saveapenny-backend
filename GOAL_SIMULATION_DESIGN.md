# Goal Simulation Design (Phase 0)

This document is the Phase 0 design lock-in for the goal simulation feature. It captures the goal type catalog, scenario input schema, simulation output schema, MCP tool risk classification, and the open questions list that must be resolved before Phase 1 starts.

It is a planning artifact only. Code, migrations, handlers, and scheduled jobs are explicitly out of scope for this document.

The schema names in this document are the source of truth for the entities, JSON shapes, and MCP tool contracts that follow.

## 1. Conventions

These conventions apply throughout the design.

- **Currency**: ISO 4217 three-letter codes stored as `VARCHAR(3)`.
- **Money**: `DECIMAL(19,4)` in storage, `BigDecimal` in Java, never `double`.
- **Dates**: `LocalDate` in Java, `DATE` in PostgreSQL, ISO-8601 strings on the wire.
- **Timestamps**: `OffsetDateTime` in Java, `TIMESTAMP WITH TIME ZONE` in PostgreSQL.
- **IDs**: `UUID`.
- **Enum storage**: `VARCHAR` with `@Enumerated(EnumType.STRING)`. Snake case in the database, upper snake case in JSON.
- **Status enums**: lifecycle fields use closed enums, not free-form strings.
- **Amount sign convention**: all amounts are positive numbers. Direction is implied by goal type, not sign.
- **JSON columns**: `JSONB` for goal inputs, scenario overrides, and simulation outputs. Validation happens in the application layer.

## 2. Goal Type Catalog

The v1 catalog contains five goal types. Each type has a stable identifier, required and optional inputs, engine defaults, feasibility rules, and example prompts.

The stable identifier is used in URLs, persistence, JSON contracts, and MCP tool inputs. It must not change without a migration path.

### 2.1 `SAVINGS`

Reach a target amount by a target date by accumulating contributions, optionally with an expected annual return.

- **Required**:
  - `targetAmount` (number, > 0)
  - `targetDate` (date, > today)
  - `currency` (string, ISO 4217)
- **Optional**:
  - `monthlyContribution` (number, > 0)
  - `expectedAnnualReturn` (number, percent, 0 to 100, default 0)
  - `startBalance` (number, >= 0, default 0)
- **Engine model**: future value of an annuity with optional compounding.

```text
final = (startBalance * (1 + r)^n) + contribution * (((1 + r)^n - 1) / r)
```

Where `n` is months, `r` is monthly rate, and `r = 0` short-circuits to linear accumulation.

- **Required monthly contribution** when not supplied:

```text
if r == 0: required = (targetAmount - startBalance) / n
else:      required = (targetAmount - startBalance * (1 + r)^n) * r / ((1 + r)^n - 1)
```

- **Feasibility rules**:
  - `INFEASIBLE` if required contribution is more than 80% of average monthly net income.
  - `AT_RISK` if required contribution is between 50% and 80% of average monthly net income.
  - `TIGHT` if required contribution is between 30% and 50% of average monthly net income.
  - `ON_TRACK` otherwise.
- **Example prompts**:
  - "I want to save $20,000 in 3 years"
  - "Help me save 100,000 TRY for a wedding by 2028"
  - "Save $5,000 in 18 months for a vacation"

### 2.2 `DEBT_PAYOFF`

Pay off an existing debt by a target date, given the current balance, APR, and minimum payment.

- **Required**:
  - `currentBalance` (number, > 0)
  - `apr` (number, percent, 0 to 100)
  - `currency` (string, ISO 4217)
- **Optional**:
  - `minimumPayment` (number, > 0, defaults to interest-only plus 1% of balance if missing)
  - `monthlyBudget` (number, >= minimumPayment)
  - `targetPayoffDate` (date, > today)
  - `fixedPayment` (number, > 0) as an alternative to `monthlyBudget`
- **Engine model**: amortization schedule with month-end compounding. Iterative monthly loop until balance `<= 0` or target date is reached.
- **Feasibility rules**:
  - `INFEASIBLE` if `minimumPayment` is less than the monthly interest charge.
  - `INFEASIBLE` if `fixedPayment` is less than the monthly interest charge.
  - `INFEASIBLE` if `targetPayoffDate` cannot be met with the requested `fixedPayment` or `monthlyBudget`.
  - `AT_RISK` if payoff requires more than 60% of average monthly net income.
  - `TIGHT` if payoff requires between 30% and 60% of average monthly net income.
  - `ON_TRACK` otherwise.
- **Example prompts**:
  - "I want to pay off my $8,000 credit card in 18 months"
  - "Pay off my car loan by next year"
  - "Clear my debt in 2 years"

### 2.3 `PURCHASE`

Accumulate a down payment toward a future purchase, or determine when a recurring saving rate would reach a target down payment.

- **Required**:
  - `targetPrice` (number, > 0)
  - `targetDate` (date, > today)
  - `currency` (string, ISO 4217)
- **Optional**:
  - `downPaymentPercent` (number, percent, 0 to 100, default 20)
  - `currentDownPayment` (number, >= 0, default 0)
  - `monthlySaving` (number, > 0)
  - `expectedAnnualReturn` (number, percent, 0 to 100, default 0)
  - `expectedPriceInflation` (number, percent, 0 to 100, default 0)
- **Engine model**: same accumulation model as `SAVINGS`, applied to the down payment target. Inflation adjusts the target price before solving.
- **Derived target**:

```text
inflatedPrice = targetPrice * (1 + inflation)^n_years
requiredDownPayment = inflatedPrice * downPaymentPercent / 100
```

- **Feasibility rules**: same bands as `SAVINGS`, evaluated against `requiredDownPayment - currentDownPayment`.
- **Example prompts**:
  - "I want to buy a $300,000 house in 5 years"
  - "Save for a new car worth 1,500,000 TRY in 2 years"
  - "Buy a laptop in 6 months"

### 2.4 `RETIREMENT`

Project whether the user's current retirement savings and contributions are on track to fund a target monthly income in retirement.

- **Required**:
  - `currentAge` (integer, 18 to 100)
  - `targetRetirementAge` (integer, > currentAge, <= 100)
  - `currentRetirementSavings` (number, >= 0)
  - `currency` (string, ISO 4217)
- **Optional**:
  - `monthlyContribution` (number, > 0)
  - `expectedAnnualReturn` (number, percent, 0 to 100, default 7)
  - `expectedInflation` (number, percent, 0 to 100, default 3)
  - `desiredMonthlyIncomeInRetirement` (number, > 0, in today's currency)
  - `lifeExpectancy` (integer, > targetRetirementAge, default 85)
  - `withdrawalRate` (number, percent, 0 to 100, default 4)
- **Engine model**: compound growth of retirement savings until retirement using a nominal annual return assumption, then a sustainable withdrawal calculation. Inflation is modeled separately when converting the desired retirement income from today's currency into the retirement-date target nest egg.
- **Required nest egg**:

```text
requiredNestEgg = (desiredMonthlyIncomeInRetirement * 12) / (withdrawalRate / 100)
requiredNestEggInFutureValue = requiredNestEgg * (1 + expectedInflation)^yearsToRetirement
```

- **Projected nest egg**: same accumulation formula as `SAVINGS`, using `expectedAnnualReturn`.
- **Feasibility rules**:
  - `INFEASIBLE` if required nest egg exceeds projected nest egg by more than 50%.
  - `AT_RISK` if required nest egg exceeds projected nest egg by 10% to 50%.
  - `TIGHT` if required nest egg exceeds projected nest egg by 0% to 10%.
  - `ON_TRACK` otherwise.
- **Example prompts**:
  - "Will I be able to retire at 65 with $3,000 a month?"
  - "Help me plan for retirement"
  - "Am I saving enough for retirement?"

### 2.5 `INCOME_TARGET`

Reach a target monthly net income by a target date, given the user's current average monthly net income.

- **Required**:
  - `targetMonthlyNetIncome` (number, > 0)
  - `targetDate` (date, > today)
  - `currency` (string, ISO 4217)
- **Optional**:
  - `currentAverageMonthlyNetIncome` (number, >= 0, derived from the last 3 months of transactions if missing)
  - `expectedIncomeGrowthRate` (number, percent, 0 to 100, default 0)
  - `incomeStrategy` (string, `LINEAR` or `COMPOUND`, default `COMPOUND`)
- **Engine model**: linear or compound growth from current income to target.
- **Required monthly growth** for `LINEAR`:

```text
delta = targetMonthlyNetIncome - currentAverageMonthlyNetIncome
requiredMonthlyChange = delta / monthsRemaining
```

- **Required monthly growth** for `COMPOUND`:

```text
requiredMonthlyGrowthRate = (targetMonthlyNetIncome / currentAverageMonthlyNetIncome)^(1 / monthsRemaining) - 1
```

- **Feasibility rules**:
  - `INFEASIBLE` if required growth rate is more than 5% per month.
  - `AT_RISK` if required growth rate is between 2% and 5% per month.
  - `TIGHT` if required growth rate is between 0.5% and 2% per month.
  - `ON_TRACK` otherwise.
- **Example prompts**:
  - "I want to make $10,000 a month in 2 years"
  - "Help me double my income in 5 years"
  - "Reach $15k monthly in 18 months"

## 3. Goal Status Lifecycle

All goal types share the same lifecycle.

```text
DRAFT -> ACTIVE -> ACHIEVED
              \-> ABANDONED
```

| Status | Meaning | Allowed transitions |
| --- | --- | --- |
| `DRAFT` | Goal created by the agent, not yet confirmed by the user. | `DRAFT -> ACTIVE`, `DRAFT -> ABANDONED` |
| `ACTIVE` | User confirmed the goal. Tracked by the progress job in later phases. | `ACTIVE -> ACHIEVED`, `ACTIVE -> ABANDONED` |
| `ACHIEVED` | Terminal. Set automatically when the system detects the target is met. | None |
| `ABANDONED` | Terminal. User or system gave up on the goal. | None |

Status transitions are enforced in application logic, not by free-form user input.

## 4. Entity Shapes (Phase 1 Targets)

These are the Phase 1 persistence contracts. They are design targets, not code.

### 4.1 `GoalEntity` (table `goals`)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | |
| `user_id` | UUID NOT NULL | ownership |
| `type` | VARCHAR(32) NOT NULL | one of the five goal types |
| `title` | VARCHAR(120) NOT NULL | user-provided or agent-suggested |
| `target_amount` | DECIMAL(19,4) NOT NULL | normalized headline target |
| `currency` | VARCHAR(3) NOT NULL | |
| `target_date` | DATE NOT NULL | |
| `linked_account_id` | UUID NULL | optional, must be owned by `user_id` |
| `status` | VARCHAR(16) NOT NULL | from the lifecycle |
| `inputs_json` | JSONB NOT NULL | type-specific inputs |
| `deleted_at` | TIMESTAMPTZ NULL | soft delete marker |
| `created_at` | TIMESTAMPTZ NOT NULL | |
| `updated_at` | TIMESTAMPTZ NOT NULL | |

Indexes: `(user_id)`, `(user_id, status)`, `(user_id, type)`.

### 4.2 `ScenarioEntity` (table `goal_scenarios`)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | |
| `goal_id` | UUID NOT NULL FK -> `goals.id` | |
| `name` | VARCHAR(80) NOT NULL | |
| `inputs_json` | JSONB NOT NULL | overrides |
| `is_baseline` | BOOLEAN NOT NULL | exactly one true per goal |
| `created_at` | TIMESTAMPTZ NOT NULL | |

Constraint: `UNIQUE (goal_id) WHERE is_baseline = true`.

### 4.3 `GoalRunEntity` (table `goal_runs`)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | |
| `goal_id` | UUID NOT NULL FK -> `goals.id` | |
| `scenario_id` | UUID NOT NULL FK -> `goal_scenarios.id` | |
| `inputs_snapshot_json` | JSONB NOT NULL | full input snapshot frozen at run time |
| `output_summary_json` | JSONB NOT NULL | compact output persisted for every run |
| `output_series_json` | JSONB NULL | full month-by-month series only when persisted |
| `feasibility` | VARCHAR(16) NOT NULL | cached feasibility for cheap queries |
| `triggered_by` | VARCHAR(16) NOT NULL | `USER`, `AGENT`, `PROGRESS_JOB`, `WHAT_IF` |
| `created_at` | TIMESTAMPTZ NOT NULL | |

Constraint: append-only. No `UPDATE` or `DELETE` outside administrative tooling.

Storage policy:

- `output_summary_json` is always written.
- `output_series_json` is only written when the caller explicitly asks to persist the full series.

## 5. Scenario Input Schema

The `inputs_json` field on `GoalEntity` and `ScenarioEntity` is a versioned JSON document.

```json
{
  "version": 1,
  "type": "SAVINGS",
  "values": {
    "targetAmount": 20000.00,
    "currency": "USD",
    "targetDate": "2029-06-06",
    "monthlyContribution": 555.00,
    "expectedAnnualReturn": 0.0,
    "startBalance": 0.00
  }
}
```

A scenario uses the same shape and may omit any field that is identical to the parent goal. The engine merges goal inputs with scenario overrides before simulation.

Rules:

- `version` is required.
- `type` is required and must match the parent goal type for scenarios.
- `values` contains the per-goal-type fields.
- Validation rules come from Section 2.
- The engine rejects invalid inputs with `VALIDATION_ERROR`.
- A version bump is required whenever a goal type input shape changes.

## 6. Simulation Output Schema

The output is a single JSON document with a common envelope and a type-specific payload.

```json
{
  "version": 1,
  "type": "SAVINGS",
  "feasibility": "TIGHT",
  "asOf": "2026-06-06T00:00:00Z",
  "horizonMonths": 36,
  "currency": "USD",
  "summary": {
    "targetAmount": 20000.00,
    "projectedAmount": 19980.00,
    "shortfall": 20.00,
    "requiredMonthlyContribution": 555.56,
    "currentMonthlyContribution": 555.00
  },
  "assumptions": {
    "expectedAnnualReturn": 0.0,
    "startBalance": 0.00,
    "averageMonthlyNetIncome": 3200.00,
    "averageMonthlyExpense": 2400.00
  },
  "warnings": [
    {
      "code": "MULTI_CURRENCY",
      "message": "Goal is in USD but primary account is in TRY. Using default conversion is disabled."
    }
  ],
  "series": [
    { "month": "2026-07-01", "balance": 555.00, "contribution": 555.00, "interest": 0.00 },
    { "month": "2026-08-01", "balance": 1110.00, "contribution": 555.00, "interest": 0.00 }
  ]
}
```

### 6.1 Envelope Fields

| Field | Type | Notes |
| --- | --- | --- |
| `version` | integer | output schema version, starts at 1 |
| `type` | string | echoes the goal type |
| `feasibility` | enum | `ON_TRACK`, `TIGHT`, `AT_RISK`, `INFEASIBLE` |
| `asOf` | timestamp | run timestamp |
| `horizonMonths` | integer | months from `asOf` to target date |
| `currency` | string | ISO 4217, matches the goal |
| `summary` | object | headline numbers |
| `assumptions` | object | explicit inputs used by the engine |
| `warnings` | array | structured warnings |
| `series` | array | month-by-month projection points |

### 6.2 Summary Per Type

Common summary fields:

- `targetAmount`
- `projectedAmount`
- `shortfall`
- `requiredMonthlyContribution`
- `currentMonthlyContribution` (optional)

Type adjustments:

- `RETIREMENT` uses `requiredNestEgg` and `projectedNestEgg`.
- `INCOME_TARGET` uses `requiredMonthlyGrowthRate` and `currentMonthlyGrowthRate`.

### 6.3 Assumptions

`assumptions` is always present, even when a value is zero.

Common fields:

- `expectedAnnualReturn`
- `startBalance`
- `averageMonthlyNetIncome`
- `averageMonthlyExpense`

Type-specific additions:

- `DEBT_PAYOFF`: `apr`, `minimumPayment`, `interestOnlyMonthlyInterest`
- `RETIREMENT`: `expectedInflation`, `lifeExpectancy`, `withdrawalRate`
- `INCOME_TARGET`: `incomeStrategy`, `expectedIncomeGrowthRate`

### 6.4 Warnings

Warning codes are closed and stable.

| Code | Meaning |
| --- | --- |
| `MULTI_CURRENCY` | Goal currency differs from primary account currency, no FX applied. |
| `MISSING_INCOME_HISTORY` | User has fewer than 3 months of income transactions. |
| `MISSING_LINKED_ACCOUNT` | Linked account is set but was not found at run time. |
| `HIGH_APR` | `apr` >= 25%. |
| `NEGATIVE_CASH_FLOW` | Average expense exceeds average income. |
| `INFLATION_NOT_SPECIFIED` | `RETIREMENT` or `PURCHASE` without an inflation input. |
| `WITHDRAWAL_RATE_OUT_OF_RANGE` | `withdrawalRate` < 2% or > 8%. |
| `LONG_HORIZON` | Horizon > 480 months. |

The engine always returns applicable warnings. The agent must narrate them.

### 6.5 Series

Series points vary by goal type:

- accumulation goals: `{ month, balance, contribution, interest }`
- `DEBT_PAYOFF`: `{ month, balance, payment, interestCharged }`
- `INCOME_TARGET`: `{ month, balance, growth }`

The API response returns a dense monthly series. Persisted storage uses `output_summary_json` for the compact view and `output_series_json` only when full-series persistence is requested.

## 7. MCP Tool Catalog and Risk Classification

Tool names are stable. Schemas follow the existing `mcp.definition` pattern.

### 7.1 Read Tools

| Tool name | Input | Output | Description |
| --- | --- | --- | --- |
| `list_goals` | `{ status?, type?, limit?, cursor? }` | paginated goals | List the user's goals. |
| `get_goal` | `{ goalId }` | goal detail with scenarios and latest run | Fetch one goal. |
| `get_goal_progress` | `{ goalId }` | progress snapshot | Current actual vs projection. |
| `list_goal_scenarios` | `{ goalId }` | scenarios | List scenarios for a goal. |
| `list_goal_runs` | `{ goalId, limit?, cursor? }` | paginated runs | List run history. |
| `simulate_goal` | `{ goalId, scenarioId? }` | `SimulationResult` | Run a live simulation. |
| `compare_scenarios` | `{ goalId, scenarioIds? }` | comparison result | Compare scenarios side by side. |
| `what_if` | `{ goalId, overrides }` | `SimulationResult` | One-off projection without persistence. |

### 7.2 Low-Risk Write Tools

| Tool name | Input | Output | Description |
| --- | --- | --- | --- |
| `create_goal` | goal inputs | `{ goalId, baselineScenarioId, runId }` | Persist a goal, baseline scenario, and initial run. |
| `create_scenario` | `{ goalId, name, overrides }` | scenario | Add a scenario. |
| `update_goal_status` | `{ goalId, status }` | goal | Apply a valid lifecycle transition. |

### 7.3 High-Impact Write Tools

| Tool name | Input | Output | Description |
| --- | --- | --- | --- |
| `update_goal` | `{ goalId, fields, confirm }` | goal | Update title, target, date, linked account, or inputs. |
| `apply_scenario_as_baseline` | `{ goalId, scenarioId, confirm }` | goal | Promote a scenario to baseline. |
| `delete_goal` | `{ goalId, confirm }` | `{ deleted: true }` | Soft delete a goal. |

### 7.4 Risk Class Table

| Tool | Risk class | Confirmation | Audit | Rate limit |
| --- | --- | --- | --- | --- |
| `list_goals` | read | no | no | 120/min/user |
| `get_goal` | read | no | no | 120/min/user |
| `get_goal_progress` | read | no | no | 60/min/user |
| `list_goal_scenarios` | read | no | no | 60/min/user |
| `list_goal_runs` | read | no | no | 60/min/user |
| `simulate_goal` | read | no | no | 30/min/user |
| `compare_scenarios` | read | no | no | 60/min/user |
| `what_if` | read | no | no | 60/min/user |
| `create_goal` | low | no | yes | 20/min/user |
| `create_scenario` | low | no | yes | 60/min/user |
| `update_goal_status` | low | no | yes | 20/min/user |
| `update_goal` | high | `confirm: true` | yes | 10/min/user |
| `apply_scenario_as_baseline` | high | `confirm: true` | yes | 10/min/user |
| `delete_goal` | high | `confirm: true` | yes | 5/min/user |

The policy layer in later phases enforces risk class, confirmation, audit, and rate limits. Tool implementations do not duplicate that policy logic.

## 8. Agent Contract

The agent flow for simulations is:

1. Receive the user's free-form prompt.
2. Choose the goal type.
3. Extract required and optional inputs.
4. Ask follow-up questions if required inputs are missing or ambiguous.
5. Call `simulate_goal`.
6. Present the result, assumptions, and warnings.
7. Ask the user whether to persist the goal.

Rules:

- The agent never invents numbers it does not have.
- The agent never narrates a simulation without a real `SimulationResult`.
- The agent includes the standard non-advisory disclaimer in every simulation response.
- The agent does not call write tools without explicit user confirmation in a separate user turn.

The prompt template must include:

- supported goal types with one-line summaries
- input contracts per goal type
- the disclaimer and no-advice rule
- the order of operations: extract, simulate, present, confirm, persist

## 9. Open Questions and Resolutions

These Phase 0 questions are resolved here so Phase 1 can start cleanly.

| # | Question | Resolution |
| --- | --- | --- |
| 1 | Should `Scenario` allow overriding the goal type, or only parameters? | Parameters only. Scenarios share the goal type. |
| 2 | What is the default expected return rate for `SAVINGS` and `RETIREMENT`? | `SAVINGS` defaults to 0%. `RETIREMENT` defaults to 7% nominal annual return, with inflation modeled separately. |
| 3 | Should the agent support voice or multi-message goal refinement, or only single-prompt v1? | Multi-message refinement is supported. Voice is out of scope. |
| 4 | Should the scheduler opt users in by default or require explicit activation? | New goals are progress-tracked by default, with user opt-out later in the product flow. |
| 5 | Is there a product requirement to limit goal types per user tier? | No. All five goal types are available to all users in v1. |
| 6 | How should we handle users with zero accounts or zero transactions? | Allow the simulation, use zero-based defaults where needed, and emit low-confidence warnings such as `MISSING_INCOME_HISTORY` or `MISSING_LINKED_ACCOUNT`. |

`linked_account_id` is optional for every v1 goal type.

## 10. Sign-Off

Phase 0 design lock-in is complete. The design baseline for goal simulation is now established by this document.

- [x] Goal type catalog reviewed and signed off.
- [x] Scenario and output schemas approved.
- [x] MCP risk classification table approved.
- [x] Open questions resolved or explicitly deferred.

Phase 1 can start from this document as the source of truth.

## 11. Phase 1 Implementation Notes

Phase 1 is now implemented. This section records what was actually built and where the implementation differs from the Phase 0 target design.

### 11.1 Files Delivered

```text
src/main/java/com/saveapenny/goal/
  controller/   GoalController
  dto/          CreateGoalRequest, UpdateGoalRequest, GoalResponse,
                CreateScenarioRequest, ScenarioResponse, GoalRunResponse,
                GoalDetailResponse, UpdateGoalStatusRequest
  entity/       GoalEntity, ScenarioEntity, GoalRunEntity,
                GoalType, GoalStatus, GoalRunTrigger, Feasibility
  exception/    GoalNotFoundException, ScenarioNotFoundException,
                InvalidGoalDateException, InvalidGoalStatusTransitionException,
                InvalidGoalTypeException, LinkedAccountNotFoundException
  mapper/       GoalMapper
  repository/   GoalRepository, ScenarioRepository, GoalRunRepository
  service/      GoalService
  service/impl/ GoalServiceImpl
src/main/resources/db/migration/V11__create_goal_tables.sql
src/test/java/com/saveapenny/goal/
  integration/  GoalFlowIntegrationTest
  service/impl/ GoalServiceImplTest
```

`com.saveapenny.shared.exception.GlobalExceptionHandler` was extended with handlers for the six goal-specific exceptions and stable error codes:

- `GOAL_NOT_FOUND`
- `SCENARIO_NOT_FOUND`
- `INVALID_GOAL_DATE`
- `INVALID_GOAL_STATUS_TRANSITION`
- `INVALID_GOAL_TYPE`
- `LINKED_ACCOUNT_NOT_FOUND`

### 11.2 Implemented REST Surface

The shipped Phase 1 REST surface is:

- `POST /api/v1/goals`
- `GET /api/v1/goals`
- `GET /api/v1/goals/{id}`
- `PATCH /api/v1/goals/{id}`
- `DELETE /api/v1/goals/{id}`
- `PATCH /api/v1/goals/{id}/status`
- `POST /api/v1/goals/{id}/scenarios`
- `GET /api/v1/goals/{id}/scenarios`
- `GET /api/v1/goals/{id}/runs`

Compared with the original Phase 1 target, the implementation adds one explicit lifecycle endpoint, `PATCH /api/v1/goals/{id}/status`, so status transitions do not have to be overloaded onto the general `PATCH` endpoint.

### 11.3 Behavior Implemented

- Goal creation validates that `targetDate` is in the future.
- Goal creation validates that `inputs.type` matches the requested `GoalType` and that the input envelope contains `version`, `type`, and `values`.
- If `linkedAccountId` is provided, ownership is enforced through `AccountRepository.existsByIdAndUserIdAndActiveTrue(...)`.
- Goal creation persists a baseline scenario automatically with the name `Baseline` and `isBaseline = true`.
- Goal update uses partial `PATCH` semantics: only supplied fields are changed.
- Goal delete is soft delete: `deleted_at` is set and `status` is forced to `ABANDONED`.
- All read paths exclude soft-deleted goals.
- `GET /api/v1/goals/{id}/runs` is functional, but returns an empty page until later phases start persisting `GoalRunEntity` rows.

### 11.4 Deviations From The Phase 0 Target Design

1. **JSON persistence is `TEXT`, not `JSONB`.**
   - The Phase 0 design uses `JSONB` as the target contract.
   - `V11__create_goal_tables.sql` stores `inputs_json`, `inputs_snapshot_json`, `output_summary_json`, and `output_series_json` as `TEXT`.
   - Reason: the current project test pattern uses H2 in PostgreSQL mode for integration tests. `TEXT` keeps those tests portable while preserving the application-level JSON contract.

2. **`deleted_at` was added to support soft delete.**
   - The roadmap called for soft delete, but the Phase 0 entity table did not yet include the column.
   - The implementation adds `deleted_at` to `goals` and treats a soft-deleted goal as not found on all reads.

3. **Lifecycle transitions ship as a dedicated endpoint.**
   - The design already defined lifecycle rules and a low-risk `update_goal_status` tool.
   - Phase 1 exposes the same separation in REST with `PATCH /api/v1/goals/{id}/status`.

4. **No initial `GoalRunEntity` row is created in Phase 1.**
   - The persistence schema for runs exists.
   - The simulation engine does not exist yet, so no initial run is created during goal creation.
   - The first persisted run is deferred to Phase 4 when simulation is wired end to end.

5. **Baseline scenario creation happens automatically in the service.**
   - The design defined the baseline invariant.
   - The implementation enforces it immediately by creating a baseline scenario when the goal is created, and by demoting any existing baseline if a new scenario is created with `isBaseline = true`.

### 11.5 Phase 1 Milestone Status

- [ ] Migration execution against a real Flyway-managed database still needs explicit verification.
- [x] Goal CRUD, scenario listing/creation, and run-history endpoints are implemented and ownership-scoped.
- [x] `GoalServiceImplTest` covers create, list, update, status transition, soft delete, and not-found paths.
- [x] `GoalFlowIntegrationTest` passes against H2 in PostgreSQL mode.
- [ ] OpenAPI annotations are in place on `GoalController`, but `/v3/api-docs` coverage has not yet been explicitly verified.
- [x] No code outside the new module references goal entities directly, other than the shared exception handler wiring.

### 11.6 What's Next

Phase 2 builds the deterministic simulation engine on top of the new persistence layer. The Phase 1 module now provides the storage, ownership checks, and REST surface that the simulation layer will plug into.

## 12. Phase 2 Implementation Notes

Phase 2 is now implemented as a pure simulation package under `com.saveapenny.goal.simulation`. The implementation focuses on deterministic strategy execution and isolated unit coverage, without any database or Spring wiring inside the math layer.

### 12.1 Files Delivered

```text
src/main/java/com/saveapenny/goal/simulation/
  SimulationEngine
  SimulationInput
  SimulationResult
  MonthlyProjectionPoint
  AssumptionSet
  SimulationWarning
  IncomeStrategy
  math/         SimulationMath
  strategy/     GoalSimulationStrategy,
                AbstractGoalSimulationStrategy,
                SavingsGoalStrategy,
                DebtPayoffGoalStrategy,
                PurchasePlanningGoalStrategy,
                RetirementGoalStrategy,
                IncomeTargetGoalStrategy
src/test/java/com/saveapenny/goal/simulation/
  SimulationEngineTest
  strategy/     SavingsGoalStrategyTest,
                DebtPayoffGoalStrategyTest,
                PurchasePlanningGoalStrategyTest,
                RetirementGoalStrategyTest,
                IncomeTargetGoalStrategyTest
```

### 12.2 Behavior Implemented

- `SimulationEngine` dispatches by `GoalType` and has a `defaultEngine()` factory with all five v1 strategies registered.
- Each strategy accepts a normalized `SimulationInput` and returns a `SimulationResult` with:
  - feasibility
  - summary map
  - assumptions map
  - warnings
  - dense monthly projection series
- Shared math is centralized in `SimulationMath` for:
  - horizon calculation
  - monthly-rate conversion
  - future value
  - required contribution
  - money rounding
- Common warnings are applied consistently across strategies:
  - `MULTI_CURRENCY`
  - `MISSING_INCOME_HISTORY`
  - `MISSING_LINKED_ACCOUNT`
  - `HIGH_APR`
  - `NEGATIVE_CASH_FLOW`
  - `INFLATION_NOT_SPECIFIED`
  - `WITHDRAWAL_RATE_OUT_OF_RANGE`
  - `LONG_HORIZON`

### 12.3 Deviations From The Phase 0 / Phase 2 Target Design

1. **`Feasibility` remains in `com.saveapenny.goal.entity`.**
   - The roadmap originally listed `Feasibility` under the new simulation package.
   - The implementation reuses the Phase 1 enum so persisted runs and simulation output share the same feasibility type.

2. **Summary and assumptions are modeled as maps.**
   - `SimulationResult.summary` is a `Map<String, Object>`.
   - `AssumptionSet` wraps a `Map<String, Object>`.
   - This keeps the engine flexible while the per-type output contract is still evolving in later phases.

3. **Phase 2 is still engine-only.**
   - No goal service wiring, REST simulation endpoint, or MCP simulation tool is introduced here.
   - Those remain Phase 4 work.

### 12.4 Phase 2 Milestone Status

- [x] Unit tests cover at least 3 cases per strategy: easy feasibility, tight feasibility, infeasible.
- [ ] Edge case tests for zero APR, zero contribution, very long horizon, leap years, end-of-month boundaries, and currency mismatch warnings are only partially covered and still need expansion.
- [x] `SimulationResult` includes feasibility, required change, projected outcome, assumptions, warnings, and a month-by-month series.
- [x] No strategy imports from repository packages.
- [ ] Determinism is implicit in the pure design and fixed-input tests, but it has not yet been called out with an explicit duplicate-input assertion.

### 12.5 What's Next

Phase 3 exposes persisted goal state through MCP read tools. Phase 4 will connect the new engine to user-facing simulation flows.

## 13. Phase 3 Implementation Notes

Phase 3 is now implemented. The persisted goal state is exposed through read-only MCP handlers and made available to the Spring AI adapter.

### 13.1 Files Delivered

```text
src/main/java/com/saveapenny/mcp/goal/
  ListGoalsToolInput, ListGoalsToolResult, ListGoalsToolHandler
  GetGoalToolInput, GetGoalToolResult, GetGoalToolHandler
  GetGoalProgressToolInput, GetGoalProgressToolResult, GetGoalProgressToolHandler
  ListGoalScenariosToolInput, ListGoalScenariosToolResult, ListGoalScenariosToolHandler
  ListGoalRunsToolInput, ListGoalRunsToolResult, ListGoalRunsToolHandler
  GoalToolModels
  GoalToolMappingSupport
src/test/java/com/saveapenny/mcp/goal/
  GoalToolHandlersTest
  GoalToolHandlersIntegrationTest
```

`SpringAiMcpToolAdapter` now exposes the following goal read tools to the assistant layer:

- `list_goals`
- `get_goal`
- `get_goal_progress`
- `list_goal_scenarios`
- `list_goal_runs`

### 13.2 Behavior Implemented

- All goal read handlers are registered automatically through the existing `InMemoryToolRegistry`.
- Ownership is enforced through the `GoalService` paths already implemented in Phase 1.
- Goal-not-found cases are translated to MCP `NOT_FOUND` errors.
- Missing required ids are rejected with `VALIDATION_ERROR`.
- The Spring AI adapter formats each goal tool response into short assistant-friendly text.

### 13.3 `get_goal_progress` Placeholder Behavior

`GetGoalProgressToolHandler` is intentionally a placeholder in Phase 3.

Current behavior:

- If the goal has no persisted run, the tool returns `status = NO_PROJECTION` and a warning with code `NO_PROJECTION`.
- `currentAmount` is inferred from the goal input envelope using the first matching field from:
  - `startBalance`
  - `currentDownPayment`
  - `currentBalance`
  - `currentRetirementSavings`
  - `currentAverageMonthlyNetIncome`
- If a latest run exists in later phases, the handler reads a projected amount from `outputSummary` using the first matching field from:
  - `projectedAmount`
  - `projectedNestEgg`
  - `projectedMonthlyNetIncome`
- The current placeholder classification uses simple bands:
  - `ACHIEVED` when current amount already meets the goal target
  - `NO_PROJECTION` when there is no run
  - `OFF_TRACK` when shortfall ratio is at least 10%
  - `AT_RISK` when shortfall ratio is at least 5%
  - `ON_TRACK` otherwise

This placeholder is expected to be replaced by the dedicated progress-calculation flow in Phase 6.

### 13.4 Deviations From The Original Phase 3 Target

1. **Tool names are snake_case.**
   - The broader platform still has older camelCase MCP tool names.
   - Goal tools follow the goal-simulation design contract instead: `list_goals`, `get_goal`, and so on.

2. **Progress is intentionally provisional.**
   - The roadmap called for a current-vs-projection read tool in Phase 3.
   - The implementation ships that surface now, but with a stable placeholder classification until the richer progress job lands in Phase 6.

### 13.5 Phase 3 Milestone Status

- [x] All read tools are implemented and registered in `ToolRegistry`.
- [x] Each tool has a stable name, input schema, and output schema.
- [x] Validation rejects missing required fields with `VALIDATION_ERROR`.
- [x] Cross-user access returns `NOT_FOUND`.
- [x] The assistant can call at least `list_goals` and `get_goal` through `SpringAiMcpToolAdapter`.
- [x] Unit and integration test coverage exists for the new goal read handlers.

### 13.6 What's Next

Phase 4 wires the simulation engine into MCP and assistant-facing simulation flows so a user can ask for a new goal simulation instead of only reading persisted goal state.

## 14. Phase 4 Implementation Notes

Phase 4 is now implemented as a thin simulation slice. The engine is wired into a real orchestration service, a read-only `simulate_goal` MCP tool, and new goal-simulation REST endpoints. The free-form prompt path currently supports `SAVINGS` prompts through a deterministic parser rather than a full LLM extraction loop.

### 14.1 Files Delivered

```text
src/main/java/com/saveapenny/goal/
  controller/   GoalSimulationController
  exception/    GoalSimulationValidationException
  service/      GoalContextProvider, GoalSimulationService
  service/impl/ GoalContextProviderImpl, GoalPromptParser,
                GoalSimulationServiceImpl
  simulation/
    GoalContextSnapshot
    dto/        DraftGoalSimulationRequest,
                GoalSimulationPromptRequest,
                ParsedGoalDraft,
                GoalSimulationResponse
src/main/java/com/saveapenny/mcp/goal/
  SimulateGoalToolInput
  SimulateGoalToolHandler
src/test/java/com/saveapenny/goal/
  controller/   GoalSimulationControllerTest
  integration/  GoalSimulationFlowIntegrationTest
src/test/java/com/saveapenny/mcp/goal/
  SimulateGoalToolHandlerTest
```

`TransactionRepository` was extended with a list query for `findAllByUserIdAndTypeAndTransactionDateBetween(...)` so the orchestration layer can compute lightweight goal context without pushing any database logic into the simulation engine.

### 14.2 Implemented REST Surface

The Phase 4 simulation endpoints now available are:

- `POST /api/v1/goals/simulate`
- `POST /api/v1/goals/simulate/draft`
- `POST /api/v1/goals/{id}/simulate`

### 14.3 Behavior Implemented

- `GoalSimulationServiceImpl` orchestrates:
  - loading goal context
  - merging goal and scenario inputs when needed
  - building a `SimulationInput`
  - calling the pure `SimulationEngine`
- `GoalContextProviderImpl` derives lightweight context from the user's active accounts and the last 3 months of transactions:
  - primary account currency
  - average monthly income
  - average monthly expense
  - missing-income-history flag
- `SimulateGoalToolHandler` exposes read-only simulation for an existing goal and optional scenario id.
- `SpringAiMcpToolAdapter` now exposes `simulate_goal` to the assistant layer.
- `POST /api/v1/goals/{id}/simulate` runs a live simulation for an existing goal without persistence.
- `POST /api/v1/goals/simulate` and `POST /api/v1/goals/simulate/draft` return:
  - parsed draft goal
  - `SimulationResult`
  - assistant-style narrative
  - standard disclaimer

### 14.4 Deviations From The Original Phase 4 Target

1. **Prompt extraction is deterministic, not LLM-driven yet.**
   - The roadmap target said the LLM extracts goal type and parameters.
   - The current implementation uses `GoalPromptParser` and supports `SAVINGS` prompts only.
   - This keeps the endpoint testable and stable while the richer assistant flow is still pending.

2. **Phase 4 currently supports only the minimal `SAVINGS` free-form prompt slice.**
   - Existing-goal simulation works for all goal types that the engine supports, as long as structured inputs already exist.
   - Free-form draft prompt parsing is intentionally narrower and throws `INVALID_GOAL_SIMULATION_REQUEST` for unsupported prompt shapes.

3. **No persistence path from the simulation endpoint yet.**
   - The roadmap described a later confirmation flow that creates a goal, baseline scenario, and run.
   - The current endpoints are draft/read-only only.
   - Persist-after-confirm remains future work.

4. **The existing `/api/v1/assistant/chat` flow is not yet using the new goal-simulation prompt path.**
   - The MCP tool is exposed to Spring AI.
   - The dedicated goal simulation endpoint is separate and does not yet route through the general assistant chat service.

### 14.5 Phase 4 Milestone Status

- [x] End-to-end test for a simple savings prompt returns feasibility, required contribution, projection series, and a narrative.
- [x] The draft simulation path does not write to the database.
- [ ] The persist path that creates a `GoalEntity`, baseline `ScenarioEntity`, and `GoalRunEntity` after explicit confirmation is not implemented yet.
- [x] Simulation responses include the standard disclaimer.
- [x] Invalid prompt shapes return a stable validation error instead of a `500`.
- [ ] The existing `/api/v1/assistant/chat` flow is not yet wired to run savings-goal simulations through this new prompt path.

### 14.6 What's Next

Phase 5 adds saved scenarios, comparison, and what-if projections on top of the now-working engine and simulation endpoint.

## 15. Phase 5 Implementation Notes

Phase 5 is now implemented. Users can compare saved scenarios side by side and run non-persistent what-if projections against an existing goal.

### 15.1 Files Delivered

```text
src/main/java/com/saveapenny/goal/simulation/dto/
  CompareScenariosRequest
  WhatIfRequest
  GoalScenarioComparisonResponse
  GoalWhatIfResponse
src/main/java/com/saveapenny/goal/
  controller/   GoalSimulationController (extended)
  service/      GoalSimulationService (extended)
  service/impl/ GoalSimulationServiceImpl (extended)
src/main/java/com/saveapenny/mcp/goal/
  CompareScenariosToolInput
  CompareScenariosToolHandler
  WhatIfToolInput
  WhatIfToolHandler
src/test/java/com/saveapenny/mcp/goal/
  Phase5GoalToolHandlersTest
src/test/java/com/saveapenny/goal/integration/
  GoalScenarioExplorationIntegrationTest
```

`SpringAiMcpToolAdapter` now exposes two additional read tools to the assistant layer:

- `compare_scenarios`
- `what_if`

### 15.2 Implemented REST Surface

The Phase 5 REST endpoints now available are:

- `POST /api/v1/goals/{id}/scenarios/compare`
- `POST /api/v1/goals/{id}/what-if`

### 15.3 Behavior Implemented

- Scenario comparison reuses the existing simulation engine and runs each selected scenario as a read-only projection.
- If `scenarioIds` is omitted, the comparison uses the goal's scenarios ordered deterministically with baseline first, then by `createdAt`.
- Comparison is capped at 10 scenarios.
- The comparison response returns:
  - scenario summaries
  - baseline deltas
  - disclaimer
- What-if accepts a flat overrides object and merges it into the goal's `inputs.values` object.
- What-if returns:
  - `SimulationResult`
  - `deltaVsBaseline`
  - `projection = true`
  - disclaimer
- Neither comparison nor what-if creates a new `ScenarioEntity` or `GoalRunEntity`.

### 15.4 Deviations From The Original Phase 5 Design

1. **Comparison and what-if are implemented only for persisted goals.**
   - This matches the intended Phase 5 direction.
   - Draft prompt scenarios are not part of the comparison flow.

2. **What-if overrides are applied only at the `values` level.**
   - The override object is intentionally flat.
   - Envelope fields such as `version` and `type` are inherited from the goal and are not overrideable.

3. **Assistant formatting is minimal.**
   - The Spring AI adapter exposes the new tools.
   - The natural-language rendering is intentionally compact rather than fully narrative.

### 15.5 Phase 5 Milestone Status

- [x] A user can save a second scenario under the same goal and compare it with the baseline.
- [x] Comparison highlights deltas in feasibility, required monthly contribution, and projected outcome.
- [x] What-if is explicitly non-persistent.
- [x] The assistant can run compare and what-if through the internal MCP adapter.

### 15.6 What's Next

Phase 6 adds progress tracking and off-track alerts on top of the persisted goals, scenarios, and simulation outputs.

## 16. Phase 6 Implementation Notes

Phase 6 is now implemented. Goal progress can be calculated deterministically, surfaced through the MCP read tool, and evaluated by a scheduler that emits one off-track notification per unread episode.

### 16.1 Files Delivered

```text
src/main/java/com/saveapenny/goal/
  config/       GoalConfig, GoalProgressProperties
  notification/ GoalOffTrackNotifier
  scheduler/    GoalProgressJob
  service/      GoalProgressCalculator, GoalProgressReport
  service/impl/ GoalProgressCalculatorImpl
src/main/resources/db/migration/
  V12__add_goal_off_track_notification_type.sql
src/test/java/com/saveapenny/goal/
  notification/ GoalOffTrackNotifierTest
  integration/  GoalProgressJobIntegrationTest
  service/impl/ GoalProgressCalculatorImplTest
```

Supporting updates:

- `NotificationType` now includes `GOAL_OFF_TRACK`.
- `NotificationRepository` now includes `findAllByUserIdAndTypeAndReadFalse(...)` for unread off-track checks.
- `GetGoalProgressToolHandler` now delegates to the new calculator and returns `baselineRunId` plus `offTrackForMonthsCount`.
- `application.yml` now contains `goal.progress.*` properties and an updated assistant prompt paragraph for goal-status questions.

### 16.2 Behavior Implemented

- `GoalProgressCalculatorImpl` is a deterministic read-only component.
- It loads the goal through the ownership-scoped `GoalService` path.
- It identifies the baseline scenario and uses the latest baseline run when one exists.
- It derives `currentAmount` from the goal input envelope using the current Phase 6 fallback order.
- It returns structured warnings for:
  - `NO_PROJECTION`
  - `CURRENT_BALANCE_MISSING`
  - `TARGET_DATE_PASSED`
- `GoalProgressJob` runs on the configured cron and evaluates all `ACTIVE` goals with `deleted_at IS NULL`.
- The job tracks consecutive off-track observations in memory and passes the streak count into the notifier.
- `GoalOffTrackNotifier` creates a `GOAL_OFF_TRACK` notification only when:
  - the goal is currently `OFF_TRACK`
  - the off-track streak has reached the configured persistence threshold
  - there is no existing unread off-track notification for the same goal title

### 16.3 Deviations From The Original Phase 6 Target

1. **Off-track persistence is tracked in memory by the scheduler.**
   - The schema still has no dedicated persistence column for streak state.
   - The current implementation keeps the consecutive off-track count in the `GoalProgressJob` component while the application is running.

2. **Baseline run resolution uses the latest run exposed by Phase 1/4 surfaces.**
   - Because run persistence is still minimal, the current implementation uses the goal detail's latest run when it belongs to the baseline scenario.
   - A richer baseline-run lookup can be tightened further if later phases begin storing multiple runs per scenario at scale.

3. **The calculator follows the Phase 6 deterministic formula even though it is intentionally conservative.**
   - `deficit = projectedAmountAtTarget - currentAmount`
   - This means progress is measured against the saved projection snapshot rather than against a recomputed ideal trajectory.

### 16.4 Phase 6 Milestone Status

- [x] `GoalProgressCalculator` is deterministic and unit-tested across achieved, on-track, at-risk, off-track, no-projection, missing-balance, and target-date-passed cases.
- [x] `GoalOffTrackNotifier` unit tests cover idempotency and threshold gating.
- [x] `GoalProgressJob` integration test verifies one off-track notification is created on the second run and not duplicated on later runs.
- [x] `GetGoalProgressToolHandler` is rewired to the calculator and returns `baselineRunId` and `offTrackForMonthsCount`.
- [x] `GoalProgressProperties` is bound from `application.yml`.
- [x] Assistant prompt configuration now includes the on-track status intent.
- [ ] A full project-wide `mvn test` run for all modules has not been executed as part of this Phase 6 step.

### 16.5 What's Next

Phase 7 hardens the goal write surface with explicit risk-policy enforcement, audit coverage, metrics, and rate limits.
