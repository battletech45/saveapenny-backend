# MVP Readiness Implementation Plan

## Goal

Prepare SaveAPenny for a practical MVP launch with safe authentication, correct financial behavior, stable deployment, and a reduced operational risk surface.

## Execution Order

1. Auth and session security
2. Ledger and account integrity
3. Safe deployment defaults
4. Frontend/runtime access readiness
5. CI and production-like verification
6. Scope cleanup and launch operations

## Epic 1: Auth And Session Security

### Task 1.1: Return rotated refresh token from refresh endpoint

Files:
- `src/main/java/com/saveapenny/auth/service/impl/AuthServiceImpl.java`
- `src/main/java/com/saveapenny/auth/service/impl/RefreshTokenServiceImpl.java`
- `src/main/java/com/saveapenny/auth/dto/RefreshTokenResponse.java`
- `src/main/java/com/saveapenny/auth/mapper/AuthMapper.java`
- auth controller/service tests

Implementation:
- Add `refreshToken` to `RefreshTokenResponse`
- Update `AuthMapper.toRefreshTokenResponse(...)` to include the rotated refresh token
- Update `AuthServiceImpl.refresh(...)` to return both:
  - new access token
  - new refresh token
- Add tests proving refresh can be called repeatedly using the newly returned refresh token

Acceptance criteria:
- Refresh endpoint returns a rotated refresh token
- Old refresh token is rejected after rotation
- New refresh token works for the next refresh request

### Task 1.2: Revoke all sessions on password change

Files:
- `src/main/java/com/saveapenny/user/service/impl/UserServiceImpl.java`
- `src/main/java/com/saveapenny/auth/service/RefreshTokenService.java`
- `src/main/java/com/saveapenny/auth/service/impl/RefreshTokenServiceImpl.java`
- user/auth tests

Implementation:
- Inject refresh token service into `UserServiceImpl`
- Revoke all active refresh tokens after successful password change
- Add tests that old refresh tokens fail after password change

Acceptance criteria:
- Password change revokes all active refresh tokens for the user
- Previously issued refresh tokens no longer work

## Epic 2: Ledger And Account Integrity

### Task 2.1: Enforce transaction currency integrity

Files:
- `src/main/java/com/saveapenny/transaction/service/impl/TransactionServiceImpl.java`
- transaction exception/handler layer if needed
- transaction tests/integration tests

Implementation:
- Validate request currency against account currency on transaction create
- Validate request currency against account currency on transaction update
- Return a clear `400` error on mismatch
- Add tests for create/update rejection

Acceptance criteria:
- Normal transaction create/update fails on currency mismatch
- Balances are not updated when currency is invalid

### Task 2.2: Restrict unsafe account type/currency edits after usage

Files:
- `src/main/java/com/saveapenny/account/service/impl/AccountServiceImpl.java`
- account repository if additional lookup is needed
- account exception/handler layer if needed
- account tests/integration tests

Implementation:
- Define an account as used when it has one of the following:
  - non-zero balance, or
  - transaction history, or
  - transfer history
- Reject account type changes for used accounts
- Reject account currency changes for used accounts
- Keep safe edits like name changes allowed
- Add tests for blocked updates

Recommendation:
- For MVP, blocking these edits is safer than reconciling historical data

Acceptance criteria:
- Type/currency changes are rejected for used accounts
- Name updates still work
- Error response explains why the update is blocked

### Task 2.3: Fix deleted-account name reuse behavior

Files:
- `src/main/java/com/saveapenny/account/service/impl/AccountServiceImpl.java`
- `src/main/java/com/saveapenny/account/repository/AccountRepository.java`
- `src/main/resources/db/migration/V13__add_unique_constraints.sql` or a new follow-up migration
- account tests/integration tests

Implementation:
- Choose one consistent rule:
  - deleted account names are reusable, or
  - deleted account names remain reserved
- Align service checks and database constraints to that rule
- Prefer a new migration instead of editing an already-applied migration
- Add tests for delete/recreate behavior

Recommendation:
- For MVP, keeping deleted names reserved is simpler and safer

Acceptance criteria:
- Service behavior matches DB behavior
- Delete/recreate flows do not fail unexpectedly at persistence time

## Epic 3: Safe Deployment Defaults

### Task 3.1: Disable OCR by default

Files:
- `src/main/resources/application.yml`
- OCR startup/runtime tests if present
- docs

Implementation:
- Change `ocr.enabled` default to `false`
- Verify app starts cleanly without Tesseract when OCR is disabled
- Keep current validation behavior when OCR is explicitly enabled

Acceptance criteria:
- Default startup does not require OCR runtime dependencies
- OCR still validates correctly when enabled

### Task 3.2: Confirm non-core feature defaults for MVP

Files:
- `src/main/resources/application.yml`
- docs

Implementation:
- Review defaults for:
  - assistant
  - insight generation
  - goal-related scheduled jobs
- Turn off any non-core scheduled/background features not required for MVP
- Document required feature flags and environment variables

Acceptance criteria:
- Non-core modules are either explicitly enabled or clearly documented as out of MVP scope

## Epic 4: Frontend And Runtime Access Readiness

### Task 4.1: Add CORS strategy or confirm same-origin deployment

Files:
- `src/main/java/com/saveapenny/config/security/SecurityConfig.java`
- possibly a new config properties class
- security tests
- docs

Implementation:
- Choose one deployment model:
  - same-origin only, documented
  - configurable CORS allowlist
- If frontend is cross-origin, add explicit CORS config for allowed frontend origins
- Add tests for expected browser/API access behavior if applicable

Recommendation:
- MVP-safe option: configurable allowlist via application properties

Acceptance criteria:
- Browser frontend can call the API in the intended deployment model
- CORS behavior is documented

## Epic 5: CI And Production-Like Verification

### Task 5.1: Add GitHub Actions CI

Files:
- `.github/workflows/*.yml`

Implementation:
- Add CI workflow for:
  - checkout
  - Java setup
  - Maven build
  - test run
- Split jobs if helpful:
  - unit/H2 tests
  - PostgreSQL/Flyway validation path

Acceptance criteria:
- Repo has CI on push and pull request
- Failures are visible before merge

### Task 5.2: Add PostgreSQL + Flyway verification path

Files:
- test config
- selected integration tests
- CI workflow

Implementation:
- Add at least one automated test path that runs with PostgreSQL
- Ensure Flyway migrations apply before tests execute
- Prefer a focused smoke/integration suite instead of converting every H2 test immediately

Acceptance criteria:
- At least one automated test path reflects production DB + migrations
- Migration/dialect regressions are caught automatically

### Task 5.3: Review OCR test reliability

Files:
- OCR integration/regression tests
- CI config

Implementation:
- Identify OCR tests that depend on native tooling or local machine setup
- Mark or separate heavy native tests if needed
- Keep default CI reliable without depending on local Homebrew-style paths

Acceptance criteria:
- CI is stable
- OCR-specific tests are clearly scoped

## Epic 6: Scope Cleanup Decisions

### Task 6.1: Lock MVP feature set in docs

Files:
- `README.md`
- relevant docs files

Implementation:
- Explicitly define MVP core as:
  - auth
  - accounts
  - categories
  - transactions
  - transfers
  - budgets
  - reports
  - recurring transactions
- Mark deferred or disabled-by-default features:
  - OCR
  - assistant
  - insights
  - goals

Acceptance criteria:
- Docs no longer imply all modules are launch-ready if they are not

### Task 6.2: Decide OCR scope

Files:
- OCR controller/service/docs

Implementation:
- Choose one of the following:
  - defer OCR for MVP, or
  - finish OCR confirm-to-transaction workflow
- If deferred:
  - document it
  - keep OCR disabled by default
- If included:
  - implement import confirmation endpoint
  - add happy-path tests

Acceptance criteria:
- OCR is either complete enough for MVP or clearly out of scope

### Task 6.3: Define assistant and OCR data retention policy

Files:
- assistant persistence docs
- OCR persistence docs
- optionally cleanup code if implemented now

Implementation:
- Document what data is stored
- Define retention window and deletion expectations
- Decide whether implementation is required pre-MVP or only documentation for now

Acceptance criteria:
- Sensitive data handling is documented and reviewable

## Epic 7: Launch Operations

### Task 7.1: Add launch smoke-test checklist

Files:
- new doc under `docs/`

Implementation:
- Create a repeatable checklist covering:
  - register
  - login
  - refresh token twice in a row
  - logout
  - password change invalidates old sessions
  - create account/category/transaction
  - create transfer
  - delete and recreate account name behavior
  - recurring transaction lifecycle
  - reports and net worth
  - startup without OCR installed when OCR is disabled

Acceptance criteria:
- Team has a repeatable pre-launch verification checklist

### Task 7.2: Review secrets handling guidance

Files:
- `.env.example`
- docs

Implementation:
- Verify example env file contains placeholders only
- Add docs note to rotate any previously exposed real keys
- Ensure tracked docs do not contain real secret values

Acceptance criteria:
- Secret handling guidance is explicit and safe

## Suggested Implementation Batches

### Batch 1
- Task 1.1
- Task 1.2

### Batch 2
- Task 2.1
- Task 2.2
- Task 2.3

### Batch 3
- Task 3.1
- Task 3.2
- Task 4.1

### Batch 4
- Task 5.1
- Task 5.2
- Task 5.3

### Batch 5
- Task 6.1
- Task 6.2
- Task 6.3
- Task 7.1
- Task 7.2

## Recommended First Work

1. Fix refresh token rotation
2. Revoke sessions on password change
3. Enforce transaction currency integrity
4. Block unsafe account edits
5. Fix account delete/name uniqueness behavior

## Exit Criteria

The project is MVP-ready when:
- refresh works repeatedly with rotated tokens
- password changes invalidate old sessions
- currency/account mutations cannot corrupt ledger correctness
- account delete/recreate behavior is consistent
- app starts cleanly without OCR dependencies unless OCR is enabled
- CI runs build/tests/migrations
- at least one PostgreSQL-backed verification path passes
- frontend access model is confirmed
- optional features are either production-ready or disabled
