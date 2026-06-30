# Goals

## Overview

Goals provide financial planning and simulation for savings, debt payoff, purchases, retirement, and income targets. The goal system is designed for repeatable, user-scoped projections. Saved goals, scenarios, and simulation runs can be revisited over time instead of relying on one-time calculations.

## Supported Goal Types

| Goal Type | Use Case | Key Inputs |
|-----------|----------|------------|
| `SAVINGS` | Reach a target balance by a target date | Monthly contribution, expected return, start balance |
| `DEBT_PAYOFF` | Estimate debt payoff timing | Monthly payment, interest rate, current balance |
| `PURCHASE` | Save toward a purchase or down payment | Target amount, monthly savings, target date |
| `RETIREMENT` | Project retirement readiness | Current savings, monthly contribution, expected return, retirement age |
| `INCOME_TARGET` | Estimate growth needed to hit a target income | Current income, target income, time horizon |

## Main Endpoints

### Goal Management

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/goals` | Create a goal |
| GET | `/api/v1/goals` | List goals (shared paginated response) |
| GET | `/api/v1/goals/{goalId}` | Get goal details |
| PATCH | `/api/v1/goals/{goalId}` | Update goal fields |
| DELETE | `/api/v1/goals/{goalId}` | Delete a goal |
| PATCH | `/api/v1/goals/{goalId}/status` | Update goal status |

### Scenarios and History

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/goals/{goalId}/scenarios` | Create a scenario |
| GET | `/api/v1/goals/{goalId}/scenarios` | List scenarios |
| GET | `/api/v1/goals/{goalId}/runs` | List simulation run history (shared paginated response) |
| POST | `/api/v1/goals/{goalId}/scenarios/compare` | Compare multiple scenarios |
| POST | `/api/v1/goals/{goalId}/what-if` | What-if analysis |

## Paginated Goal Responses

The two paginated goal endpoints below use the shared pagination contract from [API Reference](../api-reference.md):
- `GET /api/v1/goals`
- `GET /api/v1/goals/{goalId}/runs`

Returned resources are exposed in `items`.

### Simulation

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/goals/simulate` | Prompt-based simulation (natural language) |
| POST | `/api/v1/goals/simulate/draft` | Draft simulation (before saving a goal) |
| POST | `/api/v1/goals/{goalId}/simulate` | Re-run saved goal simulation |

## Typical User Flow

1. Create a goal with target amount, date, and input assumptions
2. Run a simulation to check feasibility
3. Save alternate scenarios for comparison
4. Compare scenarios to evaluate tradeoffs
5. Monitor progress and run history over time

## Create a Goal

Example savings goal:

```json
{
  "type": "SAVINGS",
  "title": "Emergency Fund",
  "targetAmount": 10000,
  "currency": "USD",
  "targetDate": "2027-12-31",
  "inputs": {
    "monthlyContribution": 350,
    "expectedAnnualReturn": 0,
    "startBalance": 1500
  },
  "linkedAccountId": "<optional-account-uuid>"
}
```

Required fields: `type`, `title`, `targetAmount`, `currency`, `targetDate`, `inputs`.

## Simulation Modes

### Draft Simulation

Use when you want a projection before saving a goal:

`POST /api/v1/goals/simulate/draft`

Takes the same body as goal creation. Returns the projection without persisting a goal.

### Prompt-Based Simulation

Use natural language to describe the goal:

`POST /api/v1/goals/simulate`

```json
{
  "prompt": "I want to save 10,000 USD by the end of next year."
}
```

The backend parses the prompt and returns a simulation result.

### Existing Goal Simulation

Re-run the currently saved version of a goal:

`POST /api/v1/goals/{goalId}/simulate`

Useful after updating transactions, balances, or scenario assumptions.

## Scenarios

Scenarios let you compare alternate assumptions for the same goal. Common use cases:

- Compare two savings rates
- Compare conservative vs. optimistic assumptions
- Test alternate debt payoff strategies
- Test different time horizons

```json
{
  "name": "More aggressive savings",
  "inputs": {
    "monthlyContribution": 500,
    "expectedAnnualReturn": 0,
    "startBalance": 1500
  },
  "isBaseline": false
}
```

## Feasibility

| Value | Meaning |
|-------|---------|
| `ON_TRACK` | Target appears achievable under current assumptions |
| `TIGHT` | Target may be achievable, but with limited margin |
| `AT_RISK` | Target depends on aggressive assumptions or stronger cash flow |
| `INFEASIBLE` | Current assumptions do not support the target |

## Goal Statuses

| Status | Meaning |
|--------|---------|
| `DRAFT` | Goal exists but is not yet actively tracked |
| `ACTIVE` | Goal is active and monitored over time |
| `ACHIEVED` | Goal target has been met |
| `ABANDONED` | Goal is no longer being pursued |

## Warnings

Simulations may include warnings:

| Warning | Meaning |
|---------|---------|
| `MULTI_CURRENCY` | Goal involves multiple currencies |
| `MISSING_INCOME_HISTORY` | Insufficient income data for projections |
| `MISSING_LINKED_ACCOUNT` | Linked account is not found or inactive |
| `NEGATIVE_CASH_FLOW` | Projected spending exceeds income |
| `LONG_HORIZON` | Projection horizon is very long (reduced confidence) |

Warnings do not always block a result but indicate lower confidence or important context gaps.

## Important Limits

- Simulations are informational and not professional financial advice
- Multi-currency situations may return warnings instead of performing conversion
- Results depend on available historical data and supplied assumptions

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `GOAL_NOT_FOUND` | 404 | Goal not found or not owned by the caller |
| `SCENARIO_NOT_FOUND` | 404 | Scenario not found |
| `INVALID_GOAL_DATE` | 400 | Date parameters are invalid |
| `INVALID_GOAL_STATUS_TRANSITION` | 400 | Cannot transition to the requested status |
| `INVALID_GOAL_TYPE` | 400 | Invalid goal type identifier |
| `INVALID_GOAL_SIMULATION_REQUEST` | 400 | Simulation request failed validation |
| `LINKED_ACCOUNT_NOT_FOUND` | 404 | Linked account not found or not owned by the caller |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Draft simulation before saving | Users can test assumptions without committing to a persisted goal |
| Scenarios for comparison | Structured comparison of alternate assumptions; better than one-off calculations |
| Prompt-based simulation | Natural language entry point for quick feasibility checks |
| Warnings over errors | Provides context without blocking results; users decide how to interpret |

## Related Documents

- [Getting Started](../getting-started.md)
- [Usage Guide](../usage-guide.md)
- [API Reference](../api-reference.md)

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/goal/entity/GoalEntity.java` | JPA entity |
| `src/main/java/com/saveapenny/goal/interfaces/http/GoalController.java` | REST endpoints |
| `src/main/java/com/saveapenny/goal/service/impl/GoalServiceImpl.java` | Business logic and simulation |
| `src/main/java/com/saveapenny/goal/simulation/strategy/` | Strategy pattern implementations per goal type |
