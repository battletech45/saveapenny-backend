# Testing Audit — Findings & Improvement Proposal

Status: **proposal, not yet implemented.** This document records the findings from a
review of `src/test/` and a comparative review of an external open-source project
(`epistola-app/epistola-suite`), and proposes concrete follow-up work. See
[`testing-guide.md`](testing-guide.md) for how to write and run tests as they exist today.

## 1. Baseline: what the current suite is

- 181 test classes, 1,206 `@Test` methods, 0 `@Disabled`.
- Covers unit (Mockito), slice (`@WebMvcTest`), repository (`@DataJpaTest`), full-flow
  integration (`@SpringBootTest` + `MockMvc`), and architecture/fitness-function tests
  (`ExceptionHierarchyTest`, `NoHardcodedSecretsTest`, `ServiceInterfacePatternTest`,
  `ClockUsageTest`).
- Not happy-path-only: services and controllers consistently assert failure branches
  (wrong password, duplicate email, invalid/expired token, 401/404/400 with specific
  error codes). `SimulationMathTest` covers null/zero/negative boundary cases in the
  financial math, not just golden values.
- Mappers, repositories, and pure math/strategy classes reasonably have no
  exception-path tests — expected, not a gap.

## 2. Trust gaps in the existing suite

### 2.1 "Integration" tests mostly don't hit real Postgres
`IntegrationTestBase` and most `@SpringBootTest`/`@DataJpaTest` classes run against H2
in `MODE=PostgreSQL`, not actual Postgres. A `TestPostgresContainer` (Testcontainers)
exists and is wired via `TestcontainersIntegrationTest`, but it only activates when the
`pg-integration` Maven profile is explicitly passed — and **neither `ci.yml` nor
`test.yml` passes it.** Flyway is also disabled in tests
(`spring.flyway.enabled=false`), so migrations themselves never run in CI. Net effect:
Postgres-specific behavior (JSONB, native functions, real constraint enforcement, actual
migrations) is unverified by default.

### 2.2 Coverage gate is weak and not enforced
A `coverage-check` JaCoCo profile exists with a 30% line-coverage minimum, but it's
never invoked by CI — `mvn clean verify` doesn't trigger the `check` goal. The number is
purely informational (uploaded as an artifact), not a merge gate.

### 2.3 Duplicate CI workflows
`ci.yml` and `test.yml` both run `mvn -B clean verify` with only cosmetic differences —
redundant compute, and ambiguity about which one is "the" gate.

### 2.4 No mutation testing
Line/branch coverage doesn't prove assertions are meaningful. No mutation-testing tool
(e.g. PIT) is present to catch weak assertions, particularly relevant in
`goal/simulation` and `insight/analytics`, which carry dense business logic.

## 3. Coverage gaps (concrete, verified)

- **`push/controller/DeviceTokenController` + `push/service/impl/DeviceTokenServiceImpl`
  + `push/repository/DeviceTokenRepository`** — the entire device-token
  register/list/delete vertical has zero tests at any layer.
- **`stock/controller/StockController`** — every other controller has a dedicated
  `@WebMvcTest`; this one only gets indirect coverage via `StockFlowIntegrationTest`.
  Rate-limit-exceeded (429), invalid-symbol, and disabled-feature HTTP mappings are
  unverified in isolation.
- **Ownership/authorization isolation** — only 8 test files assert cross-user access
  denial. For a finance app, every resource-scoped endpoint (accounts, transactions,
  budgets, goals, imports, reports, insights, stock holdings) should prove user A cannot
  read/update/delete user B's resource.
- **Concurrency** — only `AutomationDistributedLockServiceImplTest` touches
  concurrency. The recurring-transaction execution path is exactly where a race
  condition causes duplicate charges; no test currently fires concurrent executions
  against the same recurring transaction to prove the lock actually serializes them.
- **Hostile-input tests** — no systematic pass of negative-amount, oversized-decimal,
  malformed-currency, XSS-shaped, or SQL-metacharacter inputs on money/free-text fields.
- **Zero `@ParameterizedTest` usage** across all 1,206 tests — several files
  (`SimulationMathTest`, `StrongPasswordValidatorTest`, mapper tests) manually repeat
  near-identical methods for different inputs instead of using `@CsvSource`/
  `@MethodSource`.
- **External-API resilience** — confirm `AlphaVantageClientTest` and
  `RevenueCatClientTest` cover upstream timeout, malformed/partial response, and
  rate-limit backoff, not just the golden success payload.
- **Scheduler/job partial-failure paths** — confirm `GoalProgressJobTest`,
  `NetWorthSnapshotSchedulerTest`, `InsightGenerationJobTest`,
  `RecurringTransactionSchedulerTest` each prove that one bad item in a batch doesn't
  abort the whole run.

## 4. External reference: epistola-suite's testing strategy

Reviewed `github.com/epistola-app/epistola-suite` (Gradle/Kotlin Spring Boot, backend +
server-rendered UI). Its `docs/testing.md` documents a materially more mature setup.
Key patterns and their applicability here:

| Pattern | Applies to saveapenny-backend? | Notes |
|---|---|---|
| One shared Testcontainers Postgres container for the whole run, **fresh logical database per test context** (`CREATE DATABASE ctx_N`) | **Yes — directly fixes §2.1** | Keeps per-class isolation without paying container-boot cost per class; makes real-Postgres testing fast enough to be the *default* instead of an unused opt-in profile. |
| UNLOGGED tables after Flyway migration in tests (disables WAL for test writes) | Yes | Removes the main performance argument for staying on H2 once real Postgres is the default. |
| Deterministic clock **and** deterministic scheduler substrate (`scheduling.advanceTimeBy(...)`, `scheduling.runDue()`) instead of real wall-clock scheduler loops or `Thread.sleep`/Awaitility polling | Yes — relevant to `RecurringTransactionScheduler`, `GoalProgressJob`, `NetWorthSnapshotScheduler`, `InsightGenerationJob`, and the concurrency gap in §3 | Also the right substrate for a lock-contention test on `AutomationDistributedLockService`. |
| Documented table mapping each architecture-enforcement test to what it guards and why | Yes, cheap | We have the tests (`ClockUsageTest`, `ExceptionHierarchyTest`, `NoHardcodedSecretsTest`, `ServiceInterfacePatternTest`); we don't have the one-paragraph "why" per test anywhere. |
| CI job that parses the coverage XML and commits a coverage badge to the repo | Yes | Turns §2.2 from "report nobody opens" into an ambient, visible number every merge. |
| `TestExecutionListener` timing every test class + a Spring `ApplicationContextInitializer` counting context-cache misses | Yes, likely underrated here | 181 test classes with heavy `@WebMvcTest`/`@SpringBootTest`/`@MockitoBean` usage is a plausible source of hidden Spring-context fragmentation cost; currently unmeasured. |
| Dedicated gitleaks pre-commit hook (staged-diff, fast) + full-history CI job, kept separate from the hand-rolled secret-hygiene test | Yes | No secret scanner currently present at all; the two-tool split (entropy-based vs policy-based) is worth copying regardless of the rest. |
| Non-JUnit chaos/burst shell scripts for multi-instance race conditions, run by hand, explicitly outside `mvn test` | Conditional | Only worth it if `saveapenny-backend` runs multiple instances behind `AutomationDistributedLockService` in production. |
| Playwright UI test hygiene rules | No | `saveapenny-backend` has no server-rendered UI (confirmed: no Thymeleaf/HTMX, no HTML resources). |
| Mediator/command-handler wiring test | Not directly | No mediator/CQRS pattern here; `ServiceInterfacePatternTest` already plays the equivalent role for this codebase's actual pattern. |

**Single highest-leverage idea to adopt:** the shared-container-plus-per-context-database
trick (row 1) — it resolves the biggest fidelity gap in the current suite (§2.1) without
the CI-time cost that is presumably why real Postgres isn't the default already.

## 5. Proposed sequencing

1. **Infra first** (no new tests, but changes what "green CI" means):
   - Wire `coverage-check` into the CI `verify` step (§2.2); set the floor just under
     today's real measured coverage, then ratchet up in stages.
   - Consolidate `ci.yml`/`test.yml` (§2.3).
   - Adopt the shared-container/per-context-database pattern to make real Postgres +
     real Flyway migrations the CI default (§2.1, §4 row 1).
   - Add the UNLOGGED-tables test config once real Postgres is default (§4 row 2).
2. **Baseline** — generate the JaCoCo HTML report locally and use actual per-class
   coverage data (not the file-existence heuristic used in §3) to refine the gap list.
3. **Concrete zero-coverage verticals** — `DeviceTokenController`/`DeviceTokenServiceImpl`
   trio and `StockController` slice test (§3).
4. **Ownership isolation pass** — systematic cross-user-access-denied test per
   resource-scoped endpoint (§3) — highest risk-reduction per test written for a finance
   app.
5. **Concurrency + failure-path tests** on the riskiest business logic: recurring
   transaction execution/locking, scheduler partial-failure handling, external-API
   resilience (§3), using a deterministic scheduling substrate if adopted in step 1
   (§4 row 3).
6. **Ongoing hygiene**: convert repetitive cases to `@ParameterizedTest`, add
   hostile-input tests as new endpoints/fields are added (§3).
7. **Visibility tooling**: coverage badge, test-timing/context-boot instrumentation,
   gitleaks (§4), once the above is stable.
8. **Mutation testing** (PIT, scheduled/nightly, not per-PR) once coverage numbers are
   healthy, to validate the *quality* of everything added above (§2.4).
