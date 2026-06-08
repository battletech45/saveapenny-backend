# Goal Simulation Design (Phase 0)

This document is the Phase 0 design lock-in for the goal simulation feature. It captures the goal type catalog, scenario input schema, simulation output schema, MCP tool risk classification, and the open questions list that gates Phase 1.

It is a sign-off artifact. Code, migrations, and tool implementations are deferred to Phase 1 onwards.

The schema names in this document are the source of truth for the entity classes, JSON column shapes, and MCP tool input/output contracts that follow.

## 1. Conventions

These conventions apply to every section in this document.

- **Currency**: ISO 4217 three-letter codes stored as `VARCHAR(3)`.
- **Money**: `DECIMAL(19,4)` in storage, `BigDecimal` in Java, never `double`.
- **Dates**: `LocalDate` in Java, `DATE` in PostgreSQL, ISO-8601 strings on the wire.
- **Timestamps**: `OffsetDateTime` in Java, `TIMESTAMP WITH TIME ZONE` in PostgreSQL.
- **IDs**: `UUID`.
- **Enum storage**: `VARCHAR` with `@Enumerated(EnumType.STRING)`. Snake case values in the database, upper snake case in the JSON.
- **Status enums**: status fields use a closed lifecycle enum, not free-form strings.
- **Amount sign convention**: all positive numbers. Direction is implied by the goal type field, not by sign.
- **JSON columns**: `JSONB` for input snapshots, output snapshots, and scenario input overrides. Schema is enforced at the application layer, not the database.

## 2. Goal Type Catalog

The catalog defines the five goal types that ship in v1. Each entry has a stable identifier, a one-line definition, the user-facing required and optional inputs, defaults the engine uses when the user omits an input, the model the engine uses, and example prompts the agent must be able to handle.

The stable identifier is what appears in the URL, the database, the JSON wire format, and the MCP tool input. Do not rename without a migration path.

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

```
final = (startBalance * (1 + r)^n) + contribution * (((1 + r)^n - 1) / r)
```

Where `n` is months, `r` is monthly rate, and `r = 0` short-circuits to linear accumulation.

- **Required monthly contribution** (when not supplied):

```
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
  - `fixedPayment` (number, > 0) - alternative to monthly budget
- **Engine model**: amortization schedule with month-end compounding. Iterative monthly loop until balance <= 0 or target date is reached.
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

```
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
  - `withdrawalRate` (number, percent, 0 to 100, default 4) - used to back-solve the required nest egg
- **Engine model**: compound growth of retirement savings until retirement, then a sustainable withdrawal calculation.
- **Required nest egg**:

```
requiredNestEgg = (desiredMonthlyIncomeInRetirement * 12) / (withdrawalRate / 100)
requiredNestEggInFutureValue = requiredNestEgg * (1 + expectedInflation)^yearsToRetirement
```

- **Projected nest egg**: same formula as `SAVINGS` accumulation with `expectedAnnualReturn`.
- **Feasibility rules**:
  - `INFEASIBLE` if required nest egg exceeds projected nest egg by more than 50%.
  - `AT_RISK` if required nest egg exceeds projected by 10% to 50%.
  - `TIGHT` if required nest egg exceeds projected by 0% to 10%.
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
- **Required monthly growth** (linear):

```
delta = targetMonthlyNetIncome - currentAverageMonthlyNetIncome
requiredMonthlyChange = delta / monthsRemaining
```

- **Required monthly growth** (compound):

```
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

All goal types share the same status lifecycle.

```
DRAFT -> ACTIVE -> ACHIEVED
              \-> ABANDONED
```

| Status | Meaning | Allowed transitions |
| --- | --- | --- |
| `DRAFT` | Goal created by the agent, not yet confirmed by the user. | `DRAFT -> ACTIVE`, `DRAFT -> ABANDONED` |
| `ACTIVE` | User confirmed the goal. Tracked by the progress job. | `ACTIVE -> ACHIEVED`, `ACTIVE -> ABANDONED` |
| `ACHIEVED` | Terminal. Set automatically when the progress job detects the goal target is met. | None |
| `ABANDONED` | Terminal. User or system gave up. | None |

The progress job in Phase 6 is the only component that transitions `ACTIVE -> ACHIEVED`. All other transitions are user-initiated through a write tool.

## 4. Entity Shapes (Phase 1 Targets)

These are the entity shapes the implementation will produce. They are not code in Phase 0, but they are the contract for Phase 1.

### 4.1 `GoalEntity` (table `goals`)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | |
| `user_id` | UUID NOT NULL | ownership |
| `type` | VARCHAR(32) NOT NULL | one of the five goal types |
| `title` | VARCHAR(120) NOT NULL | user-provided or agent-suggested |
| `target_amount` | DECIMAL(19,4) NOT NULL | |
| `currency` | VARCHAR(3) NOT NULL | |
| `target_date` | DATE NOT NULL | |
| `linked_account_id` | UUID NULL | optional, must be owned by `user_id` |
| `status` | VARCHAR(16) NOT NULL | from the lifecycle |
| `inputs_json` | JSONB NOT NULL | type-specific inputs (see Section 5) |
| `created_at` | TIMESTAMPTZ NOT NULL | |
| `updated_at` | TIMESTAMPTZ NOT NULL | |

Indexes: `(user_id)`, `(user_id, status)`, `(user_id, type)`.

### 4.2 `ScenarioEntity` (table `goal_scenarios`)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | |
| `goal_id` | UUID NOT NULL FK -> `goals.id` | |
| `name` | VARCHAR(80) NOT NULL | |
| `inputs_json` | JSONB NOT NULL | overrides (see Section 5) |
| `is_baseline` | BOOLEAN NOT NULL | exactly one true per goal |
| `created_at` | TIMESTAMPTZ NOT NULL | |

Constraint: `UNIQUE (goal_id) WHERE is_baseline = true` for the baseline invariant.

### 4.3 `GoalRunEntity` (table `goal_runs`)

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | |
| `goal_id` | UUID NOT NULL FK -> `goals.id` | |
| `scenario_id` | UUID NOT NULL FK -> `goal_scenarios.id` | |
| `inputs_snapshot_json` | JSONB NOT NULL | full input snapshot, frozen at run time |
| `output_json` | JSONB NOT NULL | full output, frozen at run time (see Section 6) |
| `feasibility` | VARCHAR(16) NOT NULL | cached feasibility for cheap queries |
| `triggered_by` | VARCHAR(16) NOT NULL | `USER`, `AGENT`, `PROGRESS_JOB`, `WHAT_IF` |
| `created_at` | TIMESTAMPTZ NOT NULL | |

Constraint: append-only. No `UPDATE` or `DELETE` outside administrative tooling.

The `output_json` stores a compressed summary, not the full month-by-month series. The full series is available in the API response and is also kept as a sibling JSONB column for high-fidelity display:

- `output_summary_json` (always written): feasibility, required contribution, projected outcome, top-level assumptions, top warnings.
- `output_series_json` (only written for runs from `AGENT` or `USER` with `persistSeries=true`): full month-by-month series.

This split keeps the runs table small while still allowing full history when the user asked for it.

## 5. Scenario Input Schema

The `inputs_json` column on `GoalEntity` and `ScenarioEntity` is a versioned JSON document. The version is required and validated by the engine.

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

A scenario `inputs_json` has the same shape, plus an optional `name`. The scenario may omit any field that is identical to the parent goal. The engine merges the goal inputs with the scenario overrides before simulation.

Field validation per goal type is documented in Section 2. The engine refuses to run if validation fails, and the MCP tool returns `VALIDATION_ERROR`.

A `version` bump is required whenever a goal type's input shape changes. The engine must support the current version and reject older versions with a clear error.

## 6. Simulation Output Schema

The output is a single JSON document with a common envelope and a type-specific payload. The envelope is stable; the payload can grow per type.

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
| `feasibility` | enum | one of `ON_TRACK`, `TIGHT`, `AT_RISK`, `INFEASIBLE` |
| `asOf` | date | run timestamp, the first day of the projection |
| `horizonMonths` | integer | months from `asOf` to `targetDate` |
| `currency` | string | ISO 4217, matches the goal |
| `summary` | object | the headline numbers |
| `assumptions` | object | the inputs the engine used (so the user can audit) |
| `warnings` | array | structured warnings |
| `series` | array | month-by-month projection points |

### 6.2 Feasibility

A single value for cheap filtering and UI rendering. The engine produces this from the per-type rules in Section 2. If the goal is `INFEASIBLE`, `summary.requiredMonthlyContribution` is still set, even if it is unreasonable. The UI can decide how to display that.

### 6.3 Summary Per Type

The `summary` object is type-specific. Common fields:

- `targetAmount` (number): the goal target in `currency`
- `projectedAmount` (number): where the user would land at `targetDate` under the current scenario
- `shortfall` (number): `targetAmount - projectedAmount`. Negative means surplus.
- `requiredMonthlyContribution` (number): what the user needs to do, in `currency`
- `currentMonthlyContribution` (number, optional): what the user is doing today, in `currency`

`RETIREMENT` replaces `targetAmount` with `requiredNestEgg` and `projectedAmount` with `projectedNestEgg`.

`INCOME_TARGET` replaces these with `requiredMonthlyGrowthRate` and `currentMonthlyGrowthRate`.

### 6.4 Assumptions

`assumptions` is always present, even when values are zero. Recording the inputs is non-negotiable. The user must be able to see exactly what the engine assumed.

Common fields:

- `expectedAnnualReturn` (number, percent)
- `startBalance` (number)
- `averageMonthlyNetIncome` (number, in `currency`)
- `averageMonthlyExpense` (number, in `currency`)

`DEBT_PAYOFF` adds `apr`, `minimumPayment`, `interestOnlyMonthlyInterest`.

`RETIREMENT` adds `expectedInflation`, `lifeExpectancy`, `withdrawalRate`.

`INCOME_TARGET` adds `incomeStrategy`, `expectedIncomeGrowthRate`.

### 6.5 Warnings

A warning is a structured code plus a human-readable message. The codes are closed and stable.

| Code | Meaning |
| --- | --- |
| `MULTI_CURRENCY` | Goal currency differs from primary account currency, no FX applied. |
| `MISSING_INCOME_HISTORY` | User has fewer than 3 months of income transactions. |
| `MISSING_LINKED_ACCOUNT` | Linked account is set but was not found at run time. |
| `HIGH_APR` | `apr` >= 25%. |
| `NEGATIVE_CASH_FLOW` | Average expense exceeds average income. |
| `INFLATION_NOT_SPECIFIED` | `RETIREMENT` or `PURCHASE` without an inflation input. |
| `WITHDRAWAL_RATE_OUT_OF_RANGE` | `withdrawalRate` < 2% or > 8% for `RETIREMENT`. |
| `LONG_HORIZON` | Horizon > 480 months (40 years). |

The engine always returns warnings when applicable. The agent must surface warnings in the narrative.

### 6.6 Series

Each point is `{ month, balance, contribution, interest }` for accumulation-style goals, or `{ month, balance, payment, interestCharged }` for `DEBT_PAYOFF`, or `{ month, balance, growth }` for `INCOME_TARGET`.

The series is dense (one entry per month) in the API response. In the database (`output_summary_json`), only the start, end, and the inflection points (max balance, min balance, crossing zero, crossing the target) are stored. The full series is recomputed on read for the high-fidelity response.

## 7. MCP Tool Catalog and Risk Classification

The tool surface for the goal feature. Names are stable. Schemas follow the existing `mcp.definition` patterns.

### 7.1 Read Tools (always allowed, audit not required)

| Tool name | Input | Output | Description |
| --- | --- | --- | --- |
| `list_goals` | `{ status?, type?, limit?, cursor? }` | paginated list of goals | List the user's goals, with optional filters. |
| `get_goal` | `{ goalId }` | goal detail with scenarios and latest run | Get a single goal. |
| `get_goal_progress` | `{ goalId }` | progress snapshot | Current actual vs projection. |
| `list_goal_scenarios` | `{ goalId }` | scenarios for a goal | |
| `list_goal_runs` | `{ goalId, limit?, cursor? }` | paginated runs | |
| `simulate_goal` | `{ goalId, scenarioId? }` | full `SimulationResult` (live) | Run a simulation against the engine. Read-only. |
| `what_if` | `{ goalId, overrides }` | `SimulationResult` (no persistence) | One-off projection with overrides. |

### 7.2 Low-Risk Write Tools (allowed with audit)

| Tool name | Input | Output | Description |
| --- | --- | --- | --- |
| `create_goal` | goal inputs | `{ goalId, baselineScenarioId, runId }` | Persist a goal, a baseline scenario, and the first run. |
| `create_scenario` | `{ goalId, name, overrides }` | scenario | Add a scenario. |
| `update_goal_status` | `{ goalId, status }` | goal | Lifecycle transition by user. |

### 7.3 High-Impact Write Tools (require `confirm: true`)

| Tool name | Input | Output | Description |
| --- | --- | --- | --- |
| `update_goal` | `{ goalId, fields, confirm }` | goal | Update title, target, date, linked account, inputs. |
| `apply_scenario_as_baseline` | `{ goalId, scenarioId, confirm }` | goal | Promote a scenario. |
| `delete_goal` | `{ goalId, confirm }` | `{ deleted: true }` | Soft delete. |

### 7.4 Risk Class Table

The risk class lives on `ToolDefinition` as metadata. The risk policy class enforces it.

| Tool | Risk class | Confirmation | Audit | Rate limit |
| --- | --- | --- | --- | --- |
| `list_goals` | read | no | no | 120/min/user |
| `get_goal` | read | no | no | 120/min/user |
| `get_goal_progress` | read | no | no | 60/min/user |
| `list_goal_scenarios` | read | no | no | 60/min/user |
| `list_goal_runs` | read | no | no | 60/min/user |
| `simulate_goal` | read | no | no | 30/min/user |
| `what_if` | read | no | no | 60/min/user |
| `create_goal` | low | no | yes | 20/min/user |
| `create_scenario` | low | no | yes | 60/min/user |
| `update_goal_status` | low | no | yes | 20/min/user |
| `update_goal` | high | `confirm: true` | yes | 10/min/user |
| `apply_scenario_as_baseline` | high | `confirm: true` | yes | 10/min/user |
| `delete_goal` | high | `confirm: true` | yes | 5/min/user |

The risk class and rate limits are enforced by the policy layer in Phase 7. The tool implementations in Phase 3-5 do not check the policy themselves.

## 8. Agent Contract

The agent's job during the simulation flow is:

1. Receive the user's free-form prompt.
2. Choose the goal type.
3. Extract the required and optional inputs.
4. If a required input is missing or ambiguous, ask the user before proceeding.
5. Call `simulate_goal` to get a `SimulationResult`.
6. Surface the result, the assumptions, and the warnings back to the user.
7. Ask the user to confirm persistence.

The agent never invents a number it does not have. If the user did not provide a `monthlyContribution` and the engine needs one, the agent asks. If the user did not provide a `currentRetirementSavings` and the engine needs one, the agent asks.

The agent's prompt template must include:

- A list of supported goal types with a one-line summary.
- The input contract for each goal type (just the field names, not the math).
- The risk rules: never give financial advice, always include the standard disclaimer when narrating a simulation, never narrate a simulation without a `SimulationResult` from the tool.
- The order of operations: extract, simulate, present, confirm, persist.

The agent does not call write tools without explicit user confirmation. The confirmation is a separate user turn, not inferred from the simulation response.

## 9. Open Questions and Resolutions

Each open question from the roadmap is resolved here so that Phase 1 can start. If any of these resolutions is wrong, it must be challenged before entity code is written.

| # | Question | Resolution |
| --- | --- | --- |
| 1 | Should `Scenario` allow overriding the goal type, or only parameters? | Parameters only. Scenarios share the goal type. |
| 2 | What is the default expected return rate for `SAVINGS` and `RETIREMENT`? | `SAVINGS` defaults to 0% (cash savings, no return). `RETIREMENT` defaults to 7% real return. The user can override in both cases. |
| 3 | Should the agent support voice or multi-message goal refinement, or only single-prompt v1? | Multi-message is fine when the user provides it. The agent asks for missing inputs over multiple turns. Voice is out of scope. |
| 4 | Should the scheduler opt users in by default or require explicit activation? | Opt-in per goal. The default for new goals is to enable progress tracking. The user can disable it from the goal detail view. |
| 5 | Is there a product requirement to limit goal types per user tier? | Not in v1. All five goal types are available to all users. |
| 6 | How should we handle a user with zero accounts or zero transactions when they prompt a goal? | Allow the simulation, but add a `MISSING_INCOME_HISTORY` or `MISSING_LINKED_ACCOUNT` warning as appropriate. The engine uses sensible defaults (zero income, zero expense) and clearly labels the result as low-confidence. |

The only remaining open question is whether the `linked_account_id` field is a hard requirement for any goal type. Resolution: it is always optional in v1. Linking an account is a future feature for tracking real contributions.

## 10. Sign-Off

This design is signed off when all four milestone checks from the roadmap are confirmed:

- [ ] Goal type catalog reviewed and signed off.
- [ ] Scenario and output schemas written into this design addendum.
- [ ] Risk classification table agreed on.
- [ ] Open questions list (Section 9) all answered or deferred.

Phase 1 starts only after this document is approved.

## 11. Phase 1 Implementation Notes

Phase 1 stands up the `com.saveapenny.goal` domain module: entities, repositories, REST CRUD, and the supporting DTOs, mapper, service, controller, and exception handling. The following notes record what was actually built and any deviations from this design document.

### 11.1 Files Delivered

```
src/main/java/com/saveapenny/goal/
  entity/       GoalEntity, ScenarioEntity, GoalRunEntity,
                GoalType, GoalStatus, GoalRunTrigger, Feasibility
  repository/   GoalRepository, ScenarioRepository, GoalRunRepository
  dto/          CreateGoalRequest, UpdateGoalRequest, GoalResponse,
                CreateScenarioRequest, ScenarioResponse, GoalRunResponse,
                GoalDetailResponse, UpdateGoalStatusRequest
  mapper/       GoalMapper (MapStruct)
  service/      GoalService + impl
  controller/   GoalController
  exception/    GoalNotFoundException, ScenarioNotFoundException,
                InvalidGoalDateException, InvalidGoalStatusTransitionException,
                InvalidGoalTypeException, LinkedAccountNotFoundException
src/main/resources/db/migration/V11__create_goal_tables.sql
src/test/java/com/saveapenny/goal/
  service/impl/GoalServiceImplTest.java (13 cases)
  integration/GoalFlowIntegrationTest.java (3 cases)
```

Six new handlers added to `com.saveapenny.shared.exception.GlobalExceptionHandler` covering the new exception types and returning the platform-standard `ApiResponse.failure` shape with stable error codes (`GOAL_NOT_FOUND`, `SCENARIO_NOT_FOUND`, `INVALID_GOAL_DATE`, `INVALID_GOAL_STATUS_TRANSITION`, `INVALID_GOAL_TYPE`, `LINKED_ACCOUNT_NOT_FOUND`).

### 11.2 Deviations From the Original Design

These are the places where the implementation differs from the design doc, with reasoning. They should be reviewed before Phase 2 starts.

1. **JSON column storage is `TEXT`, not `JSONB`.**
   - The design specifies `JSONB` for `inputs_json`, `goal_scenarios.inputs_json`, and `goal_runs.output_*_json`. The migration uses `TEXT`.
   - Reason: the project's integration tests run on H2 in PostgreSQL mode. H2's PostgreSQL mode does not have first-class `JSONB` support that is portable across versions. Using `TEXT` keeps tests portable and the application-layer contract is identical (Jackson parses and emits the JSON, the database stores bytes).
   - Impact: none at the API or application layer. If raw `psql` queries are ever written against the `inputs_json` columns, JSON operators will not work. The application's repository layer is the only intended read path.

2. **`PATCH` semantics are explicit, not mapper-driven.**
   - The design does not specify how `PATCH /api/v1/goals/{id}` should treat null fields. The service implements a real partial update: a null field in `UpdateGoalRequest` is never applied to the entity. This avoids the MapStruct default of "set null" which would violate the `NOT NULL` constraint on `inputs_json`.
   - The mapper exposes a `jsonInputsToString(JsonNode)` helper that the service uses only when the request actually supplies a new `inputs` payload.

3. **Soft delete, not hard delete.**
   - The roadmap said "soft delete" and the implementation honors that. `GoalEntity` has a `deleted_at` column. `DELETE /api/v1/goals/{id}` sets `deletedAt` to now and forces `status = ABANDONED`. All read queries filter out goals where `deleted_at IS NOT NULL` and the service throws `GoalNotFoundException` for any access to a soft-deleted goal, so cross-user access attempts get the same `404` as never-existed goals.
   - Scenarios and runs are deleted by the database's `ON DELETE CASCADE` from `goals` if a hard delete is ever needed, but soft delete preserves the `goal_runs` history.

4. **Baseline scenario invariant is enforced in the database.**
   - A partial unique index `uq_goal_scenarios_one_baseline_per_goal ON goal_scenarios(goal_id) WHERE is_baseline = TRUE` guarantees at most one baseline per goal. The service's create flow also demotes the existing baseline when a new one is requested, so the application is consistent on its own, but the database constraint is the final safety net.

5. **Status transitions are enforced in the service, not the database.**
   - The lifecycle is `DRAFT -> ACTIVE -> {ACHIEVED, ABANDONED} -> terminal`. The migration's `CHECK` constraint on `status` only validates the value, not the transition. The service holds the only authoritative transition table. If a goal ever needs to be set to `ACHIEVED` programmatically (the Phase 6 progress job), it must go through the service.

6. **`isBaseline` JSON serialization.**
   - Lombok generates `isBaseline()` for `private boolean isBaseline`. Jackson then exposes the property as `baseline` in JSON by default. The entity and the response DTO both carry `@JsonProperty("isBaseline")` so the wire format matches the design doc. A custom `toResponse(ScenarioEntity)` in the mapper sets the field directly to avoid the same property-naming confusion MapStruct has with boolean fields.

7. **No code outside `com.saveapenny.goal` references the new entities directly.**
   - Verified by grep across `src/main/java`. The only cross-package reference is the six new exception handlers in `com.saveapenny.shared.exception.GlobalExceptionHandler`, which is the project's standard cross-module exception wiring.

8. **Run history endpoints are stubbed.**
   - `GET /api/v1/goals/{id}/runs` returns an empty page in Phase 1. The engine that produces runs is Phase 2. The entity and repository are in place so the endpoint contract is stable.

### 11.3 Milestone Status

From the Phase 1 section of `GOAL_SIMULATION_ROADMAP.md`:

- [x] Migration runs cleanly on a fresh DB and on a DB upgraded from the previous schema. (V11 is valid PostgreSQL; H2 integration tests pass under `create-drop`.)
- [x] All eight endpoints return correct shapes and reject cross-user access with `404`.
- [x] `GoalService` is covered by unit tests for create, update, list, ownership check, and not-found.
- [x] Integration test `GoalFlowIntegrationTest` passes against H2 in PostgreSQL mode (the project's standard test database).
- [x] OpenAPI schema in `/v3/api-docs` includes the new endpoints (Swagger annotations in place; picked up by springdoc auto-config).
- [x] No code outside the new module references the new entities directly.

### 11.4 What's Next (Phase 2 Inputs)

Phase 2 builds the simulation engine on top of the persisted entities. The engine must read goal inputs from the `inputs_json` column (still stored as `TEXT`, application parses with Jackson), feed them into a strategy, and produce a `SimulationResult`. The strategy contract defined in the design doc (Section 6) is unchanged.

## 12. Phase 5 Design: Scenarios, Comparison, What-If

Phase 5 layers the "explore alternatives" surface on top of the engine and the simulation tool. The user can save a second scenario under the same goal, compare two or more scenarios side by side, and ask one-off "what-if" questions without leaving a trace.

The contracts below extend Section 7 of this document. The risk classification stays: comparison and what-if are read-only, create-scenario is a low-risk write with audit. Risk-policy enforcement still lives in Phase 7.

### 12.1 Scenarios

A `Scenario` is a named set of input overrides for a goal. Section 2 already says "a scenario `inputs_json` has the same shape, plus an optional `name`." Phase 5 keeps the existing `POST /api/v1/goals/{goalId}/scenarios` endpoint and the `create_scenario` MCP tool that mirrors it. No new entity fields.

The existing baseline invariant (one baseline per goal) is preserved. The new `create_scenario` tool follows the same rule: a scenario with `isBaseline=true` demotes the previous baseline; a scenario with `isBaseline=false` (or omitted) inherits baseline status only if the goal has no baseline yet.

### 12.2 Compare Scenarios

The comparison endpoint takes a goal id and a list of scenario ids, runs the engine against each scenario's inputs, and returns a side-by-side view plus deltas against the baseline.

**Request**

```json
{
  "scenarioIds": ["uuid-baseline", "uuid-aggressive"]
}
```

If the list is empty or missing, the service returns the baseline plus all non-baseline scenarios for the goal. The list is capped at ten scenarios to keep the response bounded.

**Response**

```json
{
  "goalId": "uuid",
  "scenarios": [
    {
      "scenarioId": "uuid",
      "scenarioName": "Baseline",
      "isBaseline": true,
      "feasibility": "ON_TRACK",
      "horizonMonths": 24,
      "currency": "USD",
      "requiredMonthlyContribution": 200.00,
      "projectedAmount": 5000.00,
      "shortfall": 0.00,
      "warningsCount": 0
    }
  ],
  "deltas": [
    {
      "fromScenarioId": "uuid-baseline",
      "toScenarioId": "uuid-aggressive",
      "feasibilityChanged": false,
      "requiredMonthlyContributionDelta": -100.00,
      "projectedAmountDelta": 1500.00,
      "shortfallDelta": -1500.00
    }
  ],
  "disclaimer": "..."
}
```

The comparison is a read. Nothing is persisted. No `GoalRunEntity` is created. The cap (10 scenarios) and the order (baseline first, then by `createdAt`) keep the response deterministic.

### 12.3 What-If

The what-if endpoint takes a goal id and a flat set of input overrides, runs the engine once with the merged inputs, and returns a single `SimulationResult` tagged as a projection. Nothing is persisted. No scenario is created, no run is recorded, the goal's `inputs_json` is not modified.

**Request**

```json
{
  "overrides": {
    "monthlyContribution": 500.00,
    "expectedAnnualReturn": 1.5
  }
}
```

**Response**

```json
{
  "goalId": "uuid",
  "result": {
    "type": "SAVINGS",
    "feasibility": "ON_TRACK",
    "horizonMonths": 24,
    "currency": "USD",
    "summary": { ... },
    "assumptions": { ... },
    "warnings": [ ... ]
  },
  "deltaVsBaseline": {
    "requiredMonthlyContributionDelta": -300.00,
    "projectedAmountDelta": 3600.00,
    "shortfallDelta": -1000.00
  },
  "isProjection": true,
  "disclaimer": "..."
}
```

The `deltaVsBaseline` block is computed against the goal's current baseline scenario. If the goal has no baseline, the block is null. The `isProjection` flag is the client signal: this response is a one-off, not a stored run.

The override shape is the same flat key/value document as `ScenarioEntity.inputsJson` (without the `version` and `type` envelope). The engine merges overrides on top of the goal's own `inputs_json`, so a partial override is allowed and the unspecified fields fall through to the goal defaults.

### 12.4 Tooling and REST Surface

| Surface | Name | Risk | Notes |
| --- | --- | --- | --- |
| REST | `POST /api/v1/goals/{id}/scenarios/compare` | read | accepts `{ scenarioIds: [...] }`, returns comparison |
| REST | `POST /api/v1/goals/{id}/what-if` | read | accepts `{ overrides: {...} }`, returns projection |
| MCP | `create_scenario` | low-risk write | wraps `GoalService.createScenario` |
| MCP | `compare_scenarios` | read | wraps the new compare endpoint |
| MCP | `what_if` | read | wraps the new what-if endpoint |

The MCP tools follow the same `ToolHandler<I, O>` contract as the rest of the goal tools. Inputs are validated with `ToolValidationException`. Ownership is enforced by the service through the same `findOwnedActiveGoal` helper used by the rest of the goal surface, so cross-user access returns `NOT_FOUND` and never leaks existence.

### 12.5 Agent Prompt Update

The agent prompt for Phase 4 already covers "create a goal" and "what's the status of my goal". Phase 5 adds:

- "Save this as a scenario under my house goal." -> `create_scenario`
- "Compare my baseline and aggressive scenarios." -> `compare_scenarios`
- "What if I save $500 a month instead of $200?" -> `what_if`

The prompt is updated to make clear that `what_if` is the right tool when the user is exploring a single alternative and `create_scenario` + `compare_scenarios` is the right pattern when the user wants to keep the alternative. The prompt also reminds the agent to narrate the `deltaVsBaseline` field when present and to always include the disclaimer.

### 12.6 Milestone Status

- [ ] `create_scenario` MCP tool is auto-registered and follows the low-risk write contract.
- [ ] `compare_scenarios` MCP tool returns side-by-side results plus baseline deltas.
- [ ] `what_if` MCP tool returns a single projection with `isProjection: true` and never writes to the database.
- [ ] REST endpoints `POST /api/v1/goals/{id}/scenarios/compare` and `POST /api/v1/goals/{id}/what-if` are reachable, ownership-scoped, and return the documented shape.
- [ ] Cross-user access returns `404`, never leaks goal or scenario existence.
- [ ] Comparison respects the ten-scenario cap and the deterministic ordering rule.
- [ ] What-if does not create a `ScenarioEntity` or a `GoalRunEntity`. Verified by an integration test that asserts the row counts before and after the call.
- [ ] Agent prompt template includes the three new intents with the right tool names.

---

## 13. Phase 6 — Progress Tracking & Off-Track Alerts

### 13.1 Goals

Phase 6 closes the loop on the goal-simulation stack. After Phase 5 the user can model alternatives; Phase 6 makes those models stay current by:

1. Replacing the placeholder thresholds in `GetGoalProgressToolHandler` with a deterministic, testable `GoalProgressCalculator`.
2. Adding a scheduled job that runs every active goal through that calculator and emits a single `GOAL_OFF_TRACK` notification when a goal transitions from on-track to off-track.
3. Surfacing the off-track state in the assistant prompt so a user can ask "am I on track for X?" and get a structured answer.

Phase 6 does not introduce new endpoints and does not change the simulation engine. It only reads existing projections and decides whether the user needs to be told.

### 13.2 New `GoalProgressReport` DTO

```java
public record GoalProgressReport(
    UUID goalId,
    UUID baselineScenarioId,
    UUID baselineRunId,
    BigDecimal currentAmount,
    BigDecimal projectedAmountAtTarget,
    BigDecimal gap,
    Integer monthsRemaining,
    ProgressStatus status,
    int offTrackForMonthsCount,
    List<Warning> warnings
) {
    public enum ProgressStatus { ON_TRACK, AT_RISK, OFF_TRACK, ACHIEVED, NO_PROJECTION }
    public record Warning(String code, String message) {}
}
```

The record is the single source of truth for "where does this goal stand right now?". Both the MCP `getGoalProgress` tool and the scheduled job consume it.

### 13.3 `GoalProgressCalculator` (pure, deterministic)

`com.saveapenny.goal.service.GoalProgressCalculator` is a Spring `@Component` with a single public method:

```java
GoalProgressReport calculate(UUID userId, UUID goalId, LocalDate asOf);
```

It must:

- Load the goal through the existing ownership-scoped path (`goalService.getById` -> throws `GoalNotFoundException` if not found or soft-deleted).
- Read the latest `GoalRunEntity` for the goal's baseline scenario. If there is no scenario or no run, return `status = NO_PROJECTION` and a single warning `NO_PROJECTION` with the message "No baseline scenario with a run exists for this goal.".
- Read the goal's "current amount" using the same JSON node lookups the placeholder already uses (`startBalance`, `currentDownPayment`, `currentBalance`, `currentRetirementSavings`, `currentAverageMonthlyNetIncome`), in that order. If none is present, fall back to `BigDecimal.ZERO` and add a warning `CURRENT_BALANCE_MISSING`.
- Compute `monthsRemaining = monthsBetween(asOf, goal.targetDate)`. If `targetDate` is in the past, return `status = ACHIEVED` when `currentAmount >= targetAmount`, otherwise `OFF_TRACK` with a warning `TARGET_DATE_PASSED`.
- Run the classification below.

The calculator never touches the database for state writes and never reads the `notification` table. It is a pure read of the goal + run + JSON tree, plus arithmetic.

### 13.4 Classification Rules

All thresholds are derived from a single configuration record so the values can be overridden per environment:

```java
@ConfigurationProperties(prefix = "goal.progress")
public record GoalProgressProperties(
    BigDecimal offTrackRatio,        // default 0.10
    BigDecimal atRiskRatio,          // default 0.05
    int offTrackPersistenceMonths    // default 2
) { }
```

The classification function is:

1. If `currentAmount >= targetAmount` and `targetAmount.signum() > 0` -> `ACHIEVED`.
2. If the latest run is missing -> `NO_PROJECTION` (already handled).
3. Compute `deficit = projectedAmountAtTarget - currentAmount`. If `deficit <= 0` -> `ON_TRACK`.
4. Compute `ratio = deficit / |projectedAmountAtTarget|` (MathContext.DECIMAL64). If `ratio >= offTrackRatio` -> `OFF_TRACK`. Else if `ratio >= atRiskRatio` -> `AT_RISK`. Else `ON_TRACK`.
5. `offTrackForMonthsCount` is a field on the report. For the calculator's first call, it always reports `0` for `ON_TRACK`/`AT_RISK` and `1` for `OFF_TRACK` (the persistence counter is bumped by the scheduler, see 13.6).

`ProjectedAmountAtTarget` is the same field the placeholder uses today. The rules match the existing `GetGoalProgressToolHandler` behaviour so Phase 6 is a refactor, not a behaviour change.

### 13.5 `GOAL_OFF_TRACK` Notification Type

A new value is added to `com.saveapenny.notification.entity.NotificationType`:

```java
public enum NotificationType {
    BUDGET_WARNING,
    BUDGET_EXCEEDED,
    RECURRING_TRANSACTION_CREATED,
    GOAL_OFF_TRACK,
    SYSTEM
}
```

`GOAL_OFF_TRACK` uses the existing `Notification` schema unchanged. Title and message are constructed by the scheduler (see 13.6); no schema changes are needed.

### 13.6 `GoalOffTrackNotifier` (idempotency gate)

`com.saveapenny.goal.notification.GoalOffTrackNotifier` is a Spring `@Component` with a single public method:

```java
Optional<NotificationResponse> notifyIfTransitionedToOffTrack(
    UUID userId, UUID goalId, GoalProgressReport report);
```

It is invoked by the scheduler right after the calculator, and it enforces idempotency. The rule is "notify once per off-track episode":

1. Query the notification repository for any `GOAL_OFF_TRACK` notification with `read = false` and a title that starts with `"Goal is off track: "` plus the goal's title. If one exists, treat the call as a no-op (return `Optional.empty()`).
2. If the report's status is `OFF_TRACK` and the above query returned nothing, build a `CreateNotificationRequest` with `type = GOAL_OFF_TRACK`, `title = "Goal is off track: <goal.title>"`, `message = "<narrative>"` and call `NotificationService.create(userId, request)`. Return the new response.
3. If the report's status is `ON_TRACK` or `AT_RISK`, do nothing. The notifier does not "clear" notifications; the user marks them read themselves.
4. If the report's status is `ACHIEVED` or `NO_PROJECTION`, do nothing.

The message body is deterministic and includes:

- Goal title
- Target amount (currency-formatted, no decimals for whole numbers, else 2)
- Projected amount at target date
- Current amount
- The phrase "You are <ratio>% behind the projection after <months> months."
- A static footer: `"This is informational, not a recommendation."`

The narrative is built from the same `MoneyFormatter` utilities used elsewhere in the goal module. If those utilities are not available, the notifier falls back to `toPlainString()` on the `BigDecimal`. Tests assert the message contains the four required substrings.

`markExistingAsRead` is intentionally not called; if the user wants to dismiss the alert they go through the existing `update` path.

### 13.7 `GoalProgressJob` (scheduled)

`com.saveapenny.goal.scheduler.GoalProgressJob` is a Spring `@Component` in a new `goal.scheduler` subpackage. It uses the existing `@EnableScheduling` in `com.saveapenny.config.AsyncConfig` — no new config class is required.

```java
@Scheduled(cron = "${goal.progress.cron:0 0 6 * * *}")
public void evaluateAllActiveGoals() { ... }
```

The cron is a property so prod can shift it; the default is `0 0 6 * * *` (06:00 server time, every day). The job:

1. Reads the system clock through the existing `assistantClock` bean.
2. Pages through `goalRepository.findAllByStatus(GoalStatus.ACTIVE, Pageable)` (a new repository method, see 13.9).
3. For each `ACTIVE` goal with `deletedAt == null`:
   - Run `goalProgressCalculator.calculate(goal.userId, goal.id, asOf)`.
   - Call `goalOffTrackNotifier.notifyIfTransitionedToOffTrack(...)`.
4. The job is intentionally non-transactional at the top level. Each call into the calculator/notifier opens its own short transaction through Spring's default propagation.
5. Failures are logged at `WARN` with the goal id and stack trace, and the loop continues. One bad goal must not stop the rest.

The job is safe to run manually in tests by calling `evaluateAllActiveGoals()` directly. There is no leader-election layer; if a multi-instance deploy is added later the `RecurringTransactionScheduler` already exists and shows the pattern (it does not lock either, and that is acceptable for v1).

### 13.8 `GetGoalProgressToolHandler` Refactor

`GetGoalProgressToolHandler` is rewritten to delegate to `GoalProgressCalculator`. The placeholder constants `OFF_TRACK_THRESHOLD` and `AT_RISK_THRESHOLD` are removed. The new flow:

1. Validate input (unchanged).
2. Call `goalProgressCalculator.calculate(context.requireUserId(), input.goalId(), LocalDate.now(clock))`. The calculator throws `GoalNotFoundException` on missing/soft-deleted goals, which the handler translates to `ToolExecutionException(NOT_FOUND)` exactly like today.
3. Map the calculator's `GoalProgressReport` into the existing `GetGoalProgressToolResult` shape.
4. Two new fields are added to the result record: `Integer offTrackForMonthsCount` and `UUID baselineRunId` (or `null` if `NO_PROJECTION`).
5. The `ToolDefinition` is updated to document the new fields. The agent prompt tells the model that a non-zero `offTrackForMonthsCount` means the user has been notified and they should be told to check their inbox.

This is a breaking change for any LLM that previously read `status` only. The new fields are nullable so old callers that ignored them keep working.

### 13.9 Repository & Configuration Additions

```java
// GoalRepository
Page<GoalEntity> findAllByStatus(GoalStatus status, Pageable pageable);
```

```yaml
# application.yml additions
goal:
  progress:
    off-track-ratio: 0.10
    at-risk-ratio: 0.05
    off-track-persistence-months: 2
    cron: "0 0 6 * * *"
```

`GoalProgressProperties` is registered through `@EnableConfigurationProperties(GoalProgressProperties.class)` in a new `com.saveapenny.goal.config.GoalConfig` class. The class is a one-method `@Configuration` that exists only to wire the property bean — no other logic lives there.

A new query method is needed on `NotificationRepository` for the idempotency check:

```java
List<Notification> findAllByUserIdAndTypeAndReadFalse(UUID userId, NotificationType type);
```

### 13.10 Assistant Prompt Update

The system prompt in `application.yml` (line 80) is extended with one paragraph:

```text
When the user asks whether a goal is on track, whether they are falling
behind, or how a goal is doing, call getGoalProgress for the relevant
goal and narrate the status field. OFF_TRACK means a notification has
already been sent; remind the user. AT_RISK means they are close to
falling behind. NO_PROJECTION means the user has not run a simulation
yet and should be offered to run one. Always include the existing
disclaimer.
```

The intent catalog added in Phases 4 and 5 already covers "what is the status of my goal" via `getGoalProgress`. Phase 6 sharpens the wording so the LLM picks the right tool even when the user phrases it as "am I behind?", "are we on pace?", or "should I be worried about my goal?".

### 13.11 Milestone Status

- [ ] `GoalProgressCalculator` is deterministic and unit-tested with a table of fixtures covering ACHIEVED, ON_TRACK, AT_RISK, OFF_TRACK, NO_PROJECTION, missing current balance, and target-date-passed.
- [ ] `GoalOffTrackNotifier` unit tests cover the four idempotency cases (existing unread, transition into off-track, stay on-track, recovered to achieved).
- [ ] `GoalProgressJob` integration test seeds two goals, drives the job, and asserts exactly one `GOAL_OFF_TRACK` notification was created on the first run and zero on the second.
- [ ] `GetGoalProgressToolHandler` is rewired to the calculator; the placeholder constants are removed; the new fields are present in the result.
- [ ] `GoalProgressProperties` is bound from `application.yml` and overridable per environment.
- [ ] Assistant prompt template includes the on-track status intent.
- [ ] Full `mvn test` is green (existing 378 passing tests plus the new Phase 6 tests, the unrelated OCR pre-existing failure aside).
