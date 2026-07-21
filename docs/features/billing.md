# Billing (RevenueCat Subscriptions)

> Status: backend implemented and tested. `REVENUECAT_ENABLED` defaults to `false`; enabling it requires `REVENUECAT_SECRET_API_KEY` to be configured, or `/billing/sync` fails with `REVENUECAT_DISABLED`.
>
> **No webhooks:** RevenueCat webhooks are a paid-plan feature this project doesn't have, so there is no webhook receiver. Entitlement state is pull-only — see [RevenueCat Integration](#revenuecat-integration).
>
> **Mobile side — status as of this check:** the mobile team's Flutter implementation already exists (RevenueCat SDK wrapper, session sync, `features/billing/` + `features/upgrade/` slices, paywall gates, error codes, localized copy) but is **uncommitted** in `saveapenny-app`. A field-by-field DTO check found the client and server agree on every JSON field name.
>
> **Casing bug (fixed on the backend side):** the backend used to send `plan`/`status` as Java enum names (`"FREE"`, `"PLUS"`, `"ACTIVE"`, `"GRACE_PERIOD"`, ...) while the Flutter `Plan.fromWire`/`EntitlementStatus.fromWire` expect lowercase wire values (`"free"`, `"active"`, `"grace_period"`, ...). Since `fromWire` does exact-match `firstWhere`, this always fell back to the default (`Plan.free` / `EntitlementStatus.inactive`) — the client would silently forget the user's subscription on every app launch even though the backend was returning the correct persisted entitlement. `EntitlementResponse` now emits lowercase `plan`/`status` values (`BillingEntitlementServiceImpl.toResponse`); no Flutter-side change needed.
>
> **Migration numbering collision (resolved):** this feature originally added `V21__add_billing.sql`, colliding with branch `integration/firebase-messaging`'s own `V21__add_device_tokens.sql` (confirmed against a local dev database that already had that branch's `V21` applied, causing a Flyway checksum-mismatch failure on startup). Renumbered to `V22__add_billing.sql` to resolve it without touching any database's `flyway_schema_history`.

## Overview

RevenueCat handles App Store / Play Store billing lifecycle. This backend module is the source of truth for entitlement, feature access, and free-tier limits — the Flutter client's RevenueCat SDK usage is for offerings/purchase/restore UX only, never for enforcement. See the cross-repo plan at `../../../saveapenny-app/docs/MONETIZATION_IMPLEMENTATION_PLAN.md` for the original design rationale.

## Product Model

| Plans | Billing periods | Entitlement statuses |
|-------|------------------|------------------------|
| `FREE`, `PLUS` | `monthly`, `annual` (store-side only, not modeled in the backend) | `INACTIVE`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `CANCELED`, `EXPIRED` |

A user counts as "Plus access" only when `plan == PLUS` **and** `status` is one of `ACTIVE`, `TRIALING`, `GRACE_PERIOD`. `CANCELED`/`EXPIRED` Plus rows are treated as FREE for enforcement purposes.

### Capability matrix

| Capability | Free | Plus |
|---|---|---|
| Accounts, transactions, categories | Unlimited | Unlimited |
| Active budgets | Max 3 | Unlimited |
| Active goals | Max 1 | Unlimited |
| Report history | Last 3 months | Full history |
| Report export (CSV) | No | Yes |
| Recurring transactions (automated) | No — manual entry only via the plain transactions API | Yes |
| Goal what-if simulation | No | Yes |
| OCR receipt scanning | No | Yes |
| Assistant chat | No | Yes |
| Financial insights | No | Yes |
| Stock market data | No | Yes |
| CSV import | No | Yes |

## Module Layout

`com.saveapenny.billing`, following the existing per-feature module convention:

```
src/main/java/com/saveapenny/billing/
├── config/
│   ├── RevenueCatProperties.java      # @ConfigurationProperties(prefix = "revenuecat")
│   └── RevenueCatConfig.java          # RestClient bean, RevenueCatClient bean
├── controller/
│   └── BillingController.java
├── domain/                            # RevenueCat REST API response DTOs
│   ├── RevenueCatSubscriberResponse.java / RevenueCatSubscriber.java
│   └── RevenueCatEntitlement.java / RevenueCatSubscription.java
├── dto/                               # API response DTOs
│   ├── EntitlementResponse.java
│   ├── FeatureAccessResponse.java
│   └── EntitlementLimitsResponse.java
├── entity/
│   ├── BillingCustomer.java / BillingEntitlement.java
│   └── Plan.java / EntitlementStatus.java
├── exception/
│   ├── PlusRequiredException.java / FreePlanLimitReachedException.java / ReportHistoryLimitReachedException.java
│   └── RevenueCatDisabledException.java / RevenueCatClientException.java
├── infrastructure/
│   └── RevenueCatClient.java          # GET /subscribers/{app_user_id}
├── repository/
│   ├── BillingCustomerRepository.java / BillingEntitlementRepository.java
└── service/
    ├── BillingEntitlementService.java (+impl)   # entitlement resolution, sync
    ├── BillingAccessService.java (+impl)         # plan enforcement — feature gates + usage caps
    ├── RevenueCatEntitlementResolver.java         # maps RevenueCat subscriber state -> ResolvedEntitlement
    └── PlanCapabilities.java                      # static FREE/PLUS caps and feature-flag table
```

## Database

Migration `V22__add_billing.sql`:

| Table | Purpose |
|-------|---------|
| `billing_customer` | Maps `user_id` -> `revenuecat_app_user_id` (the backend user UUID, stringified) |
| `billing_entitlement` | Canonical per-user snapshot: `plan`, `status`, `store`, `product_id`, `entitlement_id`, `expires_at`, `trial_ends_at`, `grace_period_ends_at`, `will_renew`, `last_synced_at` |

The entitlement snapshot is deliberately separate from `users` — renewals, cancellations, and auditability need their own lifecycle.

## RevenueCat Integration

There is no webhook receiver — RevenueCat webhooks require a paid dashboard plan this project isn't on. Entitlement state is **pull-only**:

- `RevenueCatClient` fetches subscriber state from `GET {revenuecat.base-url}/subscribers/{appUserId}` using `Authorization: Bearer {REVENUECAT_SECRET_API_KEY}`. The `appUserId` is always the backend user UUID (`user.getId().toString()`) — RevenueCat's `appUserID` must never be the user's email.
- `POST /billing/sync` is the only way the backend learns of a change; the Flutter client must call it after purchase/restore completes and on app launch/resume. There is no server-initiated notification path.
- Because nothing pushes expiry events to the backend, `BillingEntitlement.effectiveStatus(now)` locally downgrades a stale `ACTIVE`/`TRIALING`/`GRACE_PERIOD` row to `GRACE_PERIOD`/`EXPIRED` once its stored `expiresAt`/`gracePeriodEndsAt` has passed, even without a fresh sync. This only ever downgrades — renewals/reactivations still require an actual `/sync` call, since only RevenueCat knows about those. Both `BillingAccessServiceImpl.hasPlusAccess()` (feature gating) and `BillingEntitlementServiceImpl.toResponse()` (`GET /entitlement`) use this instead of the raw stored `status`.

`RevenueCatEntitlementResolver` maps the subscriber's `entitlements[revenuecat.entitlement-identifier]` (default `"Save A Penny Pro"` — the RevenueCat entitlement's **identifier**, not its dashboard display name) plus its matching `subscriptions[productId]` into a `ResolvedEntitlement`:

- `expiresAt` in the future (or null) → `ACTIVE`, or `TRIALING` if `period_type == "TRIAL"`.
- `expiresAt` in the past but `grace_period_expires_date` still in the future → `GRACE_PERIOD`.
- Otherwise → `EXPIRED` (and plan downgrades to `FREE`).
- `willRenew` is `false` whenever the matching subscription has `unsubscribe_detected_at` or `billing_issues_detected_at` set, or the entitlement is expired/inactive.

## API

All endpoints under `/api/v1/billing`.

| Method | Path | Auth | Description |
|--------|------|------|--------------|
| GET | `/entitlement` | Bearer | Returns the canonical entitlement snapshot for the current user (no RevenueCat call — reads the local `billing_entitlement` row through `effectiveStatus()`; defaults to `FREE`/`INACTIVE` if no row exists yet) |
| POST | `/sync` | Bearer | Fetches the latest subscriber state from RevenueCat, persists it, and returns the updated entitlement. Client must call this after purchase/restore and on app launch/resume — there is no webhook to catch changes otherwise |

### Entitlement response shape

```json
{
  "plan": "plus",
  "status": "active",
  "isActive": true,
  "willRenew": true,
  "expiresAt": "2026-08-21T00:00:00Z",
  "trialEndsAt": null,
  "features": {
    "assistant": true,
    "insights": true,
    "stocks": true,
    "ocr": true,
    "csvImport": true,
    "reportExport": true,
    "advancedRecurring": true,
    "goalWhatIf": true
  },
  "limits": {
    "activeBudgets": 4,
    "maxActiveBudgets": null,
    "activeGoals": 2,
    "maxActiveGoals": null,
    "reportHistoryMonths": 2147483647
  }
}
```

`maxActiveBudgets`/`maxActiveGoals` are `null` for Plus (unlimited); `reportHistoryMonths` is `Integer.MAX_VALUE` for Plus rather than a sentinel, since it's compared directly against date-range floors.

> `plan`/`status` are lowercased enum names (`"plus"`, `"grace_period"`) — see the casing-bug callout at the top of this doc.

## Enforcement

`BillingAccessService` is injected directly into feature services (not controllers, except Stocks — see below) and is a distinct concern from the existing feature-flag checks (`ASSISTANT_ENABLED`, `STOCK_ENABLED`, etc.):

- Globally disabled feature (`ASSISTANT_DISABLED`, `STOCK_DISABLED`, ...) → **503**
- Feature enabled but locked by plan → **403 `PLUS_REQUIRED`**

| Method | Used for |
|---|---|
| `requireFeature(userId, featureName)` | Hard gate: assistant, insights, stocks, ocr, csvImport, reportExport, advancedRecurring, goalWhatIf |
| `enforceBudgetCreationLimit(userId)` | Blocks budget creation past the FREE cap (3 active, by `endDate >= today`) |
| `enforceGoalCreationLimit(userId)` | Blocks goal creation past the FREE cap (1 active, by `status = ACTIVE`) |
| `enforceReportHistoryWindow(userId, from)` | Blocks report queries with `from` earlier than `today - 3 months` for FREE |

### Where each check is wired in

| Feature | File | Check |
|---|---|---|
| Assistant chat | `assistant/service/impl/AssistantServiceImpl.java` | `requireFeature(userId, "assistant")` |
| Insights generation | `insight/service/impl/InsightServiceImpl.java` | `requireFeature(userId, "insights")` (on-demand `generate()` only — the scheduled batch job is unaffected) |
| Stocks | `stock/controller/StockController.java` | `requireFeature` via a single `@ModelAttribute` method run before every handler — `StockService` has no per-user context, so this is enforced at the controller as a deliberate exception to the "enforce in services" rule |
| OCR | `ocr/application/service/OcrJobServiceImpl.java` | `requireFeature(userId, "ocr")` |
| CSV import | `imports/service/impl/ImportServiceImpl.java` | `requireFeature(userId, "csvImport")` |
| Recurring transactions | `automation/service/impl/RecurringTransactionServiceImpl.java` | `requireFeature(userId, "advancedRecurring")` on `create()` only — the whole feature is Plus-only; FREE users use the plain transactions API for manual entries |
| Goal what-if | `goal/service/impl/GoalSimulationServiceImpl.java` | `requireFeature(userId, "goalWhatIf")` on `whatIf()` |
| Budgets | `budget/service/impl/BudgetServiceImpl.java` | `enforceBudgetCreationLimit(userId)` on `create()` |
| Goals | `goal/service/impl/GoalServiceImpl.java` | `enforceGoalCreationLimit(userId)` on `create()` |
| Reports | `report/service/impl/ReportServiceImpl.java` | `enforceReportHistoryWindow` on `getMonthlySummary`/`getCategorySpending`/`getCashFlow`; `requireFeature(userId, "reportExport")` on `exportMonthlySummaryCsv` |

## Configuration

See [Environment Variables](../env-reference.md#billing-revenuecat) for `REVENUECAT_*` variables.

## Error Codes

See [Error Codes](../error-codes.md) for the full catalogue. Billing-specific codes: `PLUS_REQUIRED` (403), `FREE_PLAN_LIMIT_REACHED` (403), `REPORT_HISTORY_LIMIT_REACHED` (403), `REVENUECAT_DISABLED` (503), `REVENUECAT_PROVIDER_ERROR` (502).

## Analytics

Emitted via the existing `com.saveapenny.analytics` module (see [Firebase Analytics](firebase-analytics.md)): `subscription_started`, `trial_started`. Both fire from `POST /sync` (`BillingEntitlementServiceImpl.emitPurchaseSignal`) when a user transitions from not-Plus-active to Plus-active. There is no webhook to source `subscription_renewed`/`subscription_canceled` from, so those events are not currently emitted.

## Tests

- Unit: `RevenueCatEntitlementResolverTest` (status/plan/trial/grace-period/will-renew derivation), `BillingAccessServiceImplTest` (all four enforcement paths, including lazy expiry via `effectiveStatus`).
- Integration: existing flow tests across `stock`, `insight`, `ocr`, `imports`, `automation`, `goal` were updated to seed a Plus entitlement via a new `IntegrationTestBase.grantPlusEntitlement(token)` helper, since those tests exercise now-gated endpoints for reasons unrelated to billing itself.
- Migration: verified against a real Postgres 15 container (Flyway `V22` applies cleanly on top of `V1`-`V20`; Hibernate schema validation passes) and against a real local dev database that already had `integration/firebase-messaging`'s `V21` applied.

## Related Documents

- [Environment Variables](../env-reference.md) — `REVENUECAT_*` configuration
- [Error Codes](../error-codes.md) — Complete error catalogue
- [API Reference](../api-reference.md) — Endpoint list and conventions
- [Architecture](../architecture.md) — Module structure
- Cross-repo plan: `saveapenny-app/docs/MONETIZATION_IMPLEMENTATION_PLAN.md`
