# Firebase Analytics (Server-Side Events)

> Status: implemented (Phases 2, 3, and 5 of the rollout plan, including per-platform credentials — see below). `FIREBASE_ANALYTICS_ENABLED` defaults to `false`; turning it on in any environment requires four real values — `FIREBASE_ANDROID_APP_ID`/`FIREBASE_ANDROID_API_SECRET` and `FIREBASE_IOS_APP_ID`/`FIREBASE_IOS_API_SECRET` — none of which have been supplied/configured yet (a `@PostConstruct` check fails startup with a clear error if any is missing while enabled). Validation against GA4 DebugView (Phase 4) is still outstanding, blocked on those credentials.
>
> **Mobile side (client-id correlation) — status as of this check:** the mobile team initially reported "setup is okay," but inspecting `saveapenny-app` directly showed neither the `X-Analytics-Client-Id` header nor any `appInstanceId` retrieval exist in the codebase yet. Separately, correlating Firebase's device/installation-level analytics identity with an authenticated user identity on every request is a privacy/consent-posture decision, not just an interceptor — that sign-off has now been confirmed as covered by the existing privacy policy/consent flow, so mobile is clear to implement the interceptor (which still also needs to send the new `X-Client-Platform` header below). Do not treat a verbal "it's fine" from any side as sufficient going forward for this feature — verify against the actual repo, and confirm consent sign-off explicitly, before flipping `FIREBASE_ANALYTICS_ENABLED=true`.
>
> **Backend-side gap found during this check — now fixed:** mobile has two separate Firebase App IDs (Android `1:236642936869:android:...`, iOS `1:236642936869:ios:...`, same project). A single App ID/secret pair would have misattributed or failed Measurement Protocol validation for whichever platform didn't match. `AnalyticsProperties` now holds separate Android/iOS credential pairs, and `AnalyticsClientIdFilter` captures a new `X-Client-Platform: android|ios` header to select the right pair per request. Events with no platform header (the three batch/async-triggered ones — see the Integration Points table) are dropped with an `analytics.events.failed{reason=unknown_platform}` counter rather than sent under a guessed platform's credentials.

## Overview

The mobile client already sends events directly to Firebase Analytics (GA4 for Firebase) using its own SDK — that path needs no backend involvement. This feature adds a **second** path: backend-triggered events (goal reached, budget exceeded, scheduled report generated, OCR import completed, stock alerts, automation runs) sent from this service into the *same* Firebase Analytics property via the [Measurement Protocol](https://developers.google.com/analytics/devguides/collection/protocol/ga4), correlated to the same user using the mobile app's Firebase Installations ID (`app_instance_id`). This gives a unified funnel across client and server actions without adding the Firebase Admin SDK or any new database as a hard dependency.

## Goals

- Emit backend-originated business events into the mobile app's existing GA4/Firebase property.
- Correlate backend events to the originating mobile session via `app_instance_id`.
- Never let analytics dispatch add latency or failure modes to request-handling threads.
- Ship incrementally: a handful of events first, expand once validated in GA4 DebugView.

## Non-goals

- Firebase Cloud Messaging / push notifications (separate feature, would use the Firebase Admin SDK).
- Reading Firebase Analytics data back out (BigQuery export querying) — out of scope for this doc.
- Replacing or duplicating any event the mobile client already sends client-side.

## Architecture

```
Mobile app                          Backend                              Firebase / GA4
-----------                          -------                              --------------
Firebase SDK ──(app_instance_id)──▶  X-Analytics-Client-Id header
                                      │
                                      ▼
                              AnalyticsClientIdFilter (MDC)
                                      │
                       GoalService / BudgetService / ReportService / ...
                                      │  publish(AnalyticsEvent)
                                      ▼
                            AnalyticsEventPublisher (async)
                                      │
                                      ▼
                          MeasurementProtocolClient (RestClient)
                                      │  POST /mp/collect
                                      ▼
                                                                    Google Analytics
                                                                    (same property as
                                                                     the mobile app)
```

Key property: the mobile client must forward its Firebase `app_instance_id` and platform (`android`/`ios`) on API calls (as request headers) so backend-originated events attach to the same user/device timeline instead of creating an orphan stream, and so the backend can pick the right Firebase App ID/API secret pair. If the platform header is absent, the event is dropped rather than sent under a guessed platform's credentials — see [Client ID Correlation](#client-id-correlation).

## Module Layout

New package: `com.saveapenny.analytics`, following the existing per-feature module convention (`config`, `service`, `dto` subpackages — no controller/entity/repository needed since this feature has no persistence or HTTP surface of its own).

```
src/main/java/com/saveapenny/analytics/
├── config/
│   └── AnalyticsProperties.java
├── dto/
│   └── AnalyticsEvent.java
├── service/
│   ├── AnalyticsEventPublisher.java          (interface)
│   ├── MeasurementProtocolEventPublisher.java (implementation, @ConditionalOnProperty)
│   └── NoOpAnalyticsEventPublisher.java       (implementation when disabled)
└── AnalyticsClientIdFilter.java               (captures X-Analytics-Client-Id into MDC)
```

## Configuration

`application.yml` — new top-level `firebase.analytics` block, following the same shape as `ocr.*`:

```yaml
firebase:
  analytics:
    enabled: ${FIREBASE_ANALYTICS_ENABLED:false}
    android-app-id: ${FIREBASE_ANDROID_APP_ID:}
    android-api-secret: ${FIREBASE_ANDROID_API_SECRET:}
    ios-app-id: ${FIREBASE_IOS_APP_ID:}
    ios-api-secret: ${FIREBASE_IOS_API_SECRET:}
    endpoint: ${FIREBASE_ANALYTICS_ENDPOINT:https://www.google-analytics.com/mp/collect}
    debug-endpoint: ${FIREBASE_ANALYTICS_DEBUG_ENDPOINT:https://www.google-analytics.com/debug/mp/collect}
    validate-only: ${FIREBASE_ANALYTICS_VALIDATE_ONLY:false}
    timeout-millis: ${FIREBASE_ANALYTICS_TIMEOUT_MS:2000}
```

`AnalyticsProperties.java`:

```java
package com.saveapenny.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase.analytics")
public record AnalyticsProperties(
        boolean enabled,
        String androidAppId,
        String androidApiSecret,
        String iosAppId,
        String iosApiSecret,
        String endpoint,
        String debugEndpoint,
        boolean validateOnly,
        long timeoutMillis) {
}
```

Two separate credential pairs are required because mobile has two separate Firebase Apps (Android and iOS) under the same Firebase project — a single App ID/API secret can't validly represent both. See [Client ID Correlation](#client-id-correlation) for how the right pair gets selected per request.

### New environment variables

| Variable | Default | Description |
|----------|---------|--------------|
| `FIREBASE_ANALYTICS_ENABLED` | `false` | Master switch; disabled leaves the no-op publisher wired in |
| `FIREBASE_ANDROID_APP_ID` | — | Android Firebase App ID (`1:XXXXXXXX:android:...`), from Firebase console → Project Settings |
| `FIREBASE_ANDROID_API_SECRET` | — | Measurement Protocol API secret for the Android data stream |
| `FIREBASE_IOS_APP_ID` | — | iOS Firebase App ID (`1:XXXXXXXX:ios:...`), from Firebase console → Project Settings |
| `FIREBASE_IOS_API_SECRET` | — | Measurement Protocol API secret for the iOS data stream |
| `FIREBASE_ANALYTICS_ENDPOINT` | `https://www.google-analytics.com/mp/collect` | Production collection endpoint |
| `FIREBASE_ANALYTICS_DEBUG_ENDPOINT` | `https://www.google-analytics.com/debug/mp/collect` | DebugView validation endpoint (returns validation messages instead of collecting) |
| `FIREBASE_ANALYTICS_VALIDATE_ONLY` | `false` | When `true`, routes to the debug endpoint and never records real events — use in staging during rollout |
| `FIREBASE_ANALYTICS_TIMEOUT_MS` | `2000` | HTTP call timeout; failures are logged and swallowed, never propagated to the caller |

These are documented in `docs/env-reference.md` under `## Firebase Analytics` (see [env-reference.md](../env-reference.md)).

## Event Model

`AnalyticsEvent.java`:

```java
package com.saveapenny.analytics.dto;

import java.util.Map;
import java.util.regex.Pattern;

public record AnalyticsEvent(String name, Map<String, Object> params) {

    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_PARAM_COUNT = 25;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public AnalyticsEvent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Analytics event name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Analytics event name is not a valid GA4 event name: " + name);
        }
        params = params == null ? Map.of() : Map.copyOf(params);
        if (params.size() > MAX_PARAM_COUNT) {
            throw new IllegalArgumentException("Analytics event '" + name + "' exceeds the maximum of " + MAX_PARAM_COUNT + " params");
        }
    }

    public AnalyticsEvent(String name) {
        this(name, Map.of());
    }
}
```

Enforced at construction (GA4 limits): event name ≤ 40 chars, `[A-Za-z_][A-Za-z0-9_]*`; ≤ 25 params per event; param key ≤ 40 chars; param value ≤ 100 chars (checked via `String.valueOf(value)`, so numeric/boolean params are covered too). `userId` was deliberately left out of the event shape for v1 — see [Open Questions](#open-questions) on whether GA4's `user_id` field should be populated later; call sites should not assume it exists yet.

**No PII in params.** Use internal IDs (`goal_id`, `category_id`) and enums/amounts, never emails, names, or free-text descriptions.

## Client ID Correlation

`AnalyticsClientIdFilter.java` (modeled on the existing `RequestCorrelationFilter`):

```java
package com.saveapenny.analytics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class AnalyticsClientIdFilter extends OncePerRequestFilter {

    public static final String CLIENT_ID_HEADER = "X-Analytics-Client-Id";
    public static final String PLATFORM_HEADER = "X-Client-Platform";
    private static final String CLIENT_ID_MDC_KEY = "analyticsClientId";
    private static final String PLATFORM_MDC_KEY = "analyticsClientPlatform";
    private static final Set<String> KNOWN_PLATFORMS = Set.of("android", "ios");
    // name/pattern validation for the client id omitted here for brevity — see the real source

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientId = sanitizeClientId(request.getHeader(CLIENT_ID_HEADER));
        if (clientId != null) {
            MDC.put(CLIENT_ID_MDC_KEY, clientId);
        }
        String platform = sanitizePlatform(request.getHeader(PLATFORM_HEADER));
        if (platform != null) {
            MDC.put(PLATFORM_MDC_KEY, platform);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CLIENT_ID_MDC_KEY);
            MDC.remove(PLATFORM_MDC_KEY);
        }
    }

    public static String currentClientId() {
        return MDC.get(CLIENT_ID_MDC_KEY);
    }

    public static String currentPlatform() {
        return MDC.get(PLATFORM_MDC_KEY);
    }

    // sanitizeClientId(...) and sanitizePlatform(...): platform is lowercased and
    // rejected unless it's exactly "android" or "ios" (from KNOWN_PLATFORMS).
}
```

The mobile client must send **both** `X-Analytics-Client-Id` (the Firebase `app_instance_id`) and `X-Client-Platform: android` or `X-Client-Platform: ios` — the publisher uses the platform to pick the matching Android/iOS credential pair (see [Publisher & Client](#publisher--client)). Requests without a recognized platform value are treated the same as a missing one.

Registered as a `@Bean` directly in `SecurityConfig` alongside `RequestCorrelationFilter`, following the same pattern:

```java
.addFilterBefore(requestCorrelationFilter, AnonymousAuthenticationFilter.class)
.addFilterBefore(analyticsClientIdFilter, RequestCorrelationFilter.class)
.addFilterAfter(headerUserAuthenticationFilter, RequestCorrelationFilter.class)
.addFilterAfter(rateLimitingFilter, HeaderUserAuthenticationFilter.class);
```

**Ordering pitfall (hit and fixed during implementation):** inserting the new filter *between* `RequestCorrelationFilter` and `HeaderUserAuthenticationFilter` (i.e. `addFilterAfter(analyticsClientIdFilter, RequestCorrelationFilter.class)` followed by `addFilterAfter(headerUserAuthenticationFilter, AnalyticsClientIdFilter.class)`) shifts `HeaderUserAuthenticationFilter`'s resolved order past `AnonymousAuthenticationFilter`. Spring Security's `AnonymousAuthenticationFilter` then populates an anonymous `Authentication` before the JWT filter runs; `HeaderUserAuthenticationFilter`'s `SecurityContextHolder.getContext().getAuthentication() == null` guard is then always false, so it never applies the real token, and every authenticated endpoint returns 401. Placing the new filter *before* `RequestCorrelationFilter` instead avoids disturbing the existing `RequestCorrelationFilter` → `HeaderUserAuthenticationFilter` → `AnonymousAuthenticationFilter` ordering that the auth flow depends on. Any future filter added here should be verified against the full test suite, not just its own unit test — this class of bug only shows up as unrelated controller tests failing with 401.

The mobile client must add both `X-Analytics-Client-Id: <Firebase app_instance_id>` and `X-Client-Platform: android|ios` to authenticated API requests — the two client-side changes required for correlation and correct credential selection (see [Rollout Phases, Phase 1](#rollout-phases) below).

## Publisher & Client

```java
package com.saveapenny.analytics.service;

import com.saveapenny.analytics.dto.AnalyticsEvent;

public interface AnalyticsEventPublisher {
    void publish(AnalyticsEvent event);
}
```

```java
package com.saveapenny.analytics.service;

import com.saveapenny.analytics.AnalyticsClientIdFilter;
import com.saveapenny.analytics.config.AnalyticsProperties;
import com.saveapenny.analytics.dto.AnalyticsEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(prefix = "firebase.analytics", name = "enabled", havingValue = "true")
public class MeasurementProtocolEventPublisher implements AnalyticsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MeasurementProtocolEventPublisher.class);

    private final RestClient analyticsRestClient;
    private final AnalyticsProperties properties;
    private final MeterRegistry meterRegistry;

    public MeasurementProtocolEventPublisher(RestClient analyticsRestClient, AnalyticsProperties properties, MeterRegistry meterRegistry) {
        this.analyticsRestClient = analyticsRestClient;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void validateCredentials() {
        boolean androidConfigured = StringUtils.hasText(properties.androidAppId()) && StringUtils.hasText(properties.androidApiSecret());
        boolean iosConfigured = StringUtils.hasText(properties.iosAppId()) && StringUtils.hasText(properties.iosApiSecret());
        if (!androidConfigured || !iosConfigured) {
            throw new IllegalStateException(
                    "firebase.analytics.enabled=true requires both Android and iOS credentials to be set");
        }
    }

    @Override
    @Async("analyticsTaskExecutor")
    public void publish(AnalyticsEvent event) {
        String platform = AnalyticsClientIdFilter.currentPlatform();
        Optional<PlatformCredentials> credentials = resolveCredentials(platform);
        if (credentials.isEmpty()) {
            // No X-Client-Platform on this request (e.g. a batch/scheduled job with no HTTP
            // context) — there is no safe app_id/api_secret pair to attribute the event to,
            // and sending it under the wrong platform's credentials would just be silently
            // rejected by Google anyway. Drop rather than guess.
            meterRegistry.counter("analytics.events.failed", "event_name", event.name(), "reason", "unknown_platform").increment();
            log.warn("analytics_event_dropped name={} reason=unknown_platform", event.name());
            return;
        }

        String clientId = AnalyticsClientIdFilter.currentClientId();
        String resolvedClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        String endpoint = properties.validateOnly() ? properties.debugEndpoint() : properties.endpoint();

        Map<String, Object> body = Map.of(
                "app_instance_id", resolvedClientId,
                "events", List.of(Map.of(
                        "name", event.name(),
                        "params", event.params())));

        java.net.URI uri = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("firebase_app_id", credentials.get().appId())
                .queryParam("api_secret", credentials.get().apiSecret())
                .build()
                .toUri();

        try {
            analyticsRestClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            meterRegistry.counter("analytics.events.published", "event_name", event.name()).increment();
        } catch (Exception ex) {
            meterRegistry.counter("analytics.events.failed", "event_name", event.name(), "reason", "http_error").increment();
            log.warn("analytics_event_publish_failed name={} reason={}", event.name(), ex.getMessage());
        }
    }

    private Optional<PlatformCredentials> resolveCredentials(String platform) {
        if ("android".equals(platform)) {
            return Optional.of(new PlatformCredentials(properties.androidAppId(), properties.androidApiSecret()));
        }
        if ("ios".equals(platform)) {
            return Optional.of(new PlatformCredentials(properties.iosAppId(), properties.iosApiSecret()));
        }
        return Optional.empty();
    }

    private record PlatformCredentials(String appId, String apiSecret) {
    }
}
```

Batch/async-triggered events (`goal_off_track`, `receipt_import_completed`, `automation_rule_triggered`) never have a platform header available and are therefore always dropped under this design — see the note in [Integration Points](#integration-points) and the corresponding [Open Question](#open-questions).

```java
package com.saveapenny.analytics.service;

import com.saveapenny.analytics.dto.AnalyticsEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "firebase.analytics", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAnalyticsEventPublisher implements AnalyticsEventPublisher {

    @Override
    public void publish(AnalyticsEvent event) {
    }
}
```

The HTTP client and the executor both live in a module-owned `AnalyticsConfig` (not the shared `AsyncConfig`/`StockConfig`) — consistent with how `goal/config/GoalConfig` and `stock/config/StockConfig` keep their own beans local rather than centralizing them:

```java
package com.saveapenny.analytics.config;

@Configuration
public class AnalyticsConfig {

    @Bean
    public RestClient analyticsRestClient(RestClient.Builder builder, AnalyticsProperties properties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.timeoutMillis()))
                .withReadTimeout(Duration.ofMillis(properties.timeoutMillis()));
        return builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings)).build();
    }

    @Bean(name = "analyticsTaskExecutor")
    public TaskExecutor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("analytics-worker-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.initialize();
        return executor;
    }
}
```

Note: on Spring Boot 4.1 the timeout/factory API is `org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder` + `HttpClientSettings` (the older `ClientHttpRequestFactorySettings` name from Boot 3.x was renamed).

## Integration Points

Inject `AnalyticsEventPublisher` into existing services and call it at the point the business event is already known to have occurred — do not add new listeners/observers for this first pass, a direct call keeps it traceable.

"HTTP context? = Yes" means the trigger point runs inside a request, so it *can* carry `X-Analytics-Client-Id`/`X-Client-Platform` once the mobile client actually sends them — it does not mean they're sent today.

| Module | Trigger point | Event name | Params | HTTP context? | Status |
|--------|---------------|------------|--------|----------------|--------|
| `goal` | `GoalServiceImpl.updateStatus`, on transition to `ACHIEVED` | `goal_achieved` | `goal_id`, `goal_type` | Yes | Implemented |
| `goal` | `GoalOffTrackNotifier.notifyIfTransitionedToOffTrack`, when a new notification is actually created | `goal_off_track` | `goal_id`, `off_track_months` | No (nightly job) | Implemented |
| `budget` | `BudgetServiceImpl.toStatusResponse`, when resolved status is `EXCEEDED` | `budget_exceeded` | `budget_id`, `category_id`, `usage_percentage` | Yes | Implemented |
| `report` | `ReportServiceImpl.exportMonthlySummaryCsv`, after the CSV body is built | `report_generated` | `report_type` | Yes | Implemented |
| `ocr` | `OcrJobAsyncProcessor.process`, at the `OcrJobStatus.COMPLETED` transition | `receipt_import_completed` | `job_id` | No (`@Async` worker thread) | Implemented |
| `automation` | `RecurringTransactionExecutionServiceImpl.executeRun`, after `RecurringExecutionStatus.SUCCESS` is recorded | `automation_rule_triggered` | `recurring_transaction_id`, `type` | No (cron job) | Implemented |
| `stock` / `stockholding` | price alert evaluation | `stock_price_alert` | `symbol`, `direction` | — | **Not built — no price-alert feature exists in this codebase yet.** Wiring this in would mean building a new feature, not adding analytics; deliberately dropped from this pass, see [Open Questions](#open-questions). |

Notes on the batch-triggered events (`goal_off_track`, `receipt_import_completed`, `automation_rule_triggered`): they run outside any HTTP request (a nightly cron, an `@Async` worker thread, and a `@Scheduled` job respectively), so neither `AnalyticsClientIdFilter.currentClientId()` nor `.currentPlatform()` are ever set when they fire. Since the per-platform credential change, **these three events are now always dropped** (`analytics.events.failed{reason=unknown_platform}`) rather than sent with a guessed platform — sending under the wrong platform's credentials would just fail Google-side validation anyway, so this is strictly better than the old behavior of sending real HTTP requests that were quietly wrong. Fixing it for real would require passing the client id *and* platform through from whenever the underlying job/entity was originally created via an HTTP request, which none of these three currently do — see the corresponding [Open Question](#open-questions).

Other things worth calling out:

- `goal_achieved` is **not** fired from the nightly `GoalProgressJob` — that job only computes a projected `ACHIEVED` status, it doesn't persist it, and it has no HTTP request in scope. The actual persisted transition happens in `GoalServiceImpl.updateStatus`, called from an authenticated endpoint, so correlation with the mobile `app_instance_id` actually works there.
- `budget_exceeded` fires on every `getStatus`/`getStatuses` call while a budget is over 100%, not only on the first crossing — there's no persisted "already notified" state for budgets the way `GoalOffTrackNotifier` has for goals via existing unread notifications. Acceptable for a first pass since GA4 aggregates by day; revisit if event volume becomes noisy.
- `automation_rule_triggered` is really "recurring transaction executed" — there's no generic "automation rule" concept in this codebase, `automation` is currently just the recurring-transactions feature.

Example call site (`BudgetServiceImpl.toStatusResponse`):

```java
if ("EXCEEDED".equals(status)) {
    analyticsEventPublisher.publish(new AnalyticsEvent(
            "budget_exceeded",
            Map.of(
                    "budget_id", budget.getId().toString(),
                    "category_id", budget.getCategoryId().toString(),
                    "usage_percentage", usagePercentage.doubleValue())));
}
```

## Testing Plan

Implemented:

- `AnalyticsEventTest` — validation (blank/invalid/too-long name, too many params, defensive param copy).
- `AnalyticsClientIdFilterTest` — header capture, sanitization of unsafe values, MDC cleared after the request.
- `MeasurementProtocolEventPublisherTest` — asserts against `MockRestServiceServer`: correct endpoint + query params, debug-endpoint routing when `validateOnly`, and that a 500 response is swallowed rather than thrown.
- Existing `GoalServiceImplTest`, `GoalOffTrackNotifierTest`, `BudgetServiceImplTest`, `ReportServiceImplTest`, `OcrJobAsyncProcessorTest`, `RecurringTransactionExecutionServiceImplTest` updated with an `AnalyticsEventPublisher` mock so the new dependency doesn't NPE or change existing assertions.

Not yet done:

- Manual validation: set `FIREBASE_ANALYTICS_VALIDATE_ONLY=true` in staging, trigger each event, confirm `validationMessages: []` in the debug response, then check GA4 DebugView for the event appearing under the test device's `app_instance_id`. Blocked on Phase 1 (mobile team supplying `FIREBASE_APP_ID` / API secret and adding the request header).

## Observability

Implemented: `MeasurementProtocolEventPublisher` increments `analytics.events.published` on success and `analytics.events.failed` on any exception/non-2xx response, both tagged with `event_name`, via the existing Micrometer `MeterRegistry` — visible on the existing Prometheus dashboard (`management.endpoints.web.exposure.include: health,metrics,prometheus`) once the app is running with `firebase.analytics.enabled=true`. A warning log line (`analytics_event_publish_failed name=... reason=...`) still accompanies every failure for ad hoc debugging.

Also implemented: a `@PostConstruct` check on `MeasurementProtocolEventPublisher` that throws `IllegalStateException` at startup if `firebase.analytics.enabled=true` but `FIREBASE_APP_ID` or `FIREBASE_ANALYTICS_API_SECRET` is blank — fails fast instead of silently sending broken requests with empty credentials.

## Rollout Phases

1. **Discovery & contract** — mixed. Consent/privacy sign-off for the client-id linkage is confirmed. Actual mobile implementation (header + `appInstanceId` retrieval + the new `X-Client-Platform` header) was verified **not** to exist in `saveapenny-app` yet despite an earlier verbal "it's ready" — mobile still needs to build it. On the backend side, the real `FIREBASE_ANDROID_APP_ID`/`FIREBASE_ANDROID_API_SECRET`/`FIREBASE_IOS_APP_ID`/`FIREBASE_IOS_API_SECRET` values have not been supplied/configured in any environment yet. **Nothing in Phase 4 can run until both are done.**
2. **Core plumbing** — done: `analytics` module, config, filter, executor (this section).
3. **Instrument events** — done: `goal_achieved`, `goal_off_track`, `budget_exceeded`, `report_generated`, `receipt_import_completed`, `automation_rule_triggered`. `stock_price_alert` deliberately dropped — no underlying feature exists to hook into (see Open Questions).
4. **Validation** — blocked on Phase 1 (mobile's interceptor + the four real credential values). Once both are in place: set `FIREBASE_ANALYTICS_VALIDATE_ONLY=true` in staging, trigger each event from both an Android and an iOS test device, confirm in GA4 DebugView that each lands under the correct app.
5. **Reliability & expansion** — done: Micrometer counters, startup credential validation, GA4 param-value-length validation, per-platform credential selection. Still open: an outbox table + scheduler if fire-and-forget drop rate proves unacceptable in practice (not built until there's evidence it's needed); deciding whether to plumb client id + platform through the three batch-triggered events, which are currently always dropped.
6. **Docs** — done: this document and the `## Firebase Analytics` section in `env-reference.md`. Still needed: share the event catalog with the mobile/analytics stakeholders.

## Open Questions

- ~~Retention/consent: does sending backend events for a user require anything beyond what the mobile app's existing consent flow already covers?~~ **Resolved.** Linking Firebase's device/installation-level analytics identity (`X-Analytics-Client-Id`) to the authenticated user on every request is a real privacy-posture decision. Confirmed covered by the existing privacy policy/consent flow — mobile is clear to build the interceptor.
- ~~Two Firebase App IDs exist (Android/iOS) but the backend only supported one credential pair.~~ **Resolved.** `AnalyticsProperties` now holds separate Android/iOS pairs, selected via the new `X-Client-Platform` header — see [Configuration](#configuration) and [Publisher & Client](#publisher--client).
- Does the mobile team need backend confirmation of successful correlation before this ships, or is best-effort (missing header/platform drops the event) acceptable for v1?
- Should `userId` also be sent as a GA4 user property (`user_id` field in the Measurement Protocol payload) for cross-device stitching? Requires confirming this is enabled on the GA4 property and is consistent with the mobile app's existing `user_id` usage, if any.
- Is a stock price-alert feature actually wanted? If so it needs to be scoped and built as its own feature (threshold config, a scheduled check against holdings, a notification path) before `stock_price_alert` can be instrumented — this is new product surface, not analytics work.
- The three batch/async-triggered events (`goal_off_track`, `receipt_import_completed`, `automation_rule_triggered`) are now unconditionally dropped since they never carry a platform header. Is that acceptable, or does someone need this data enough to justify plumbing the client id *and* platform through from wherever the underlying job/entity was originally created via an HTTP request?

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Measurement Protocol over Firebase Admin SDK | Analytics has no server SDK concept; MP is Google's documented server-to-GA4 path and needs only an HTTP client already present in the project (`spring-boot-starter-restclient`) |
| Fire-and-forget `@Async` dispatch, no outbox in v1 | Analytics data loss is tolerable; blocking or complicating request-handling code for it is not. Upgrade to an outbox only if measured drop rate matters |
| No-op publisher by default (`enabled: false`) | Keeps local dev/tests free of external calls and secrets, consistent with `ocr.enabled` and the `openai.api-key` pattern already in `application.yml` |
| Correlation via header, not a new identity system | The mobile app already owns a Firebase Installations ID; reusing it avoids inventing a parallel identity scheme on the backend |
| No PII in event params | Matches GA4 terms of service and avoids duplicating data already covered by the app's privacy policy for client-side events |

## Referenced Files

New:

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/analytics/config/AnalyticsProperties.java` | Typed config binding for `firebase.analytics.*`, separate Android/iOS credential pairs |
| `src/main/java/com/saveapenny/analytics/config/AnalyticsConfig.java` | `RestClient` bean with configured timeout + `analyticsTaskExecutor` bean |
| `src/main/java/com/saveapenny/analytics/dto/AnalyticsEvent.java` | Event value object with GA4 validation (name, param count, param key/value length) |
| `src/main/java/com/saveapenny/analytics/service/AnalyticsEventPublisher.java` | Publisher interface used by feature modules |
| `src/main/java/com/saveapenny/analytics/service/MeasurementProtocolEventPublisher.java` | GA4 Measurement Protocol HTTP implementation; per-platform credential selection; startup credential validation; Micrometer counters |
| `src/main/java/com/saveapenny/analytics/service/NoOpAnalyticsEventPublisher.java` | Disabled-mode implementation |
| `src/main/java/com/saveapenny/analytics/AnalyticsClientIdFilter.java` | Captures `X-Analytics-Client-Id` and `X-Client-Platform` per request |
| `src/test/java/com/saveapenny/analytics/**` | Unit tests for the above |

Edited:

| File | Change |
|------|--------|
| `src/main/resources/application.yml` | Added `firebase.analytics.*` block |
| `src/main/java/com/saveapenny/config/security/SecurityConfig.java` | Registered `AnalyticsClientIdFilter` bean and filter-chain position (see the ordering pitfall note above) |
| `src/main/java/com/saveapenny/goal/service/impl/GoalServiceImpl.java` | Publishes `goal_achieved` in `updateStatus` |
| `src/main/java/com/saveapenny/goal/notification/GoalOffTrackNotifier.java` | Publishes `goal_off_track` when a new notification is created |
| `src/main/java/com/saveapenny/budget/service/impl/BudgetServiceImpl.java` | Publishes `budget_exceeded` in `toStatusResponse` |
| `src/main/java/com/saveapenny/report/service/impl/ReportServiceImpl.java` | Publishes `report_generated` in `exportMonthlySummaryCsv` |
| `src/main/java/com/saveapenny/ocr/application/job/OcrJobAsyncProcessor.java` | Publishes `receipt_import_completed` at the `COMPLETED` status transition |
| `src/main/java/com/saveapenny/automation/service/impl/RecurringTransactionExecutionServiceImpl.java` | Publishes `automation_rule_triggered` after a successful recurring-transaction run |
| `src/test/java/com/saveapenny/goal/service/impl/GoalServiceImplTest.java`, `GoalOffTrackNotifierTest.java`, `src/test/java/com/saveapenny/budget/service/impl/BudgetServiceImplTest.java`, `src/test/java/com/saveapenny/report/service/impl/ReportServiceImplTest.java`, `src/test/java/com/saveapenny/ocr/application/job/OcrJobAsyncProcessorTest.java`, `src/test/java/com/saveapenny/automation/service/impl/RecurringTransactionExecutionServiceImplTest.java` | Added `AnalyticsEventPublisher` mocks for the new constructor dependency |
| `docs/env-reference.md` | Added `## Firebase Analytics` section |
