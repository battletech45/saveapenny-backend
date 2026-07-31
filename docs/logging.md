# Logging

## Current State

The application uses SLF4J (via `spring-boot-starter-parent`) backed by Logback's default implementation. There is no dedicated `logback-spring.xml` — configuration lives entirely in `application.yml`.

| Aspect | Current Behavior |
|--------|-------------------|
| Facade | SLF4J (`org.slf4j.Logger` / `LoggerFactory`) |
| Backend | Logback (Spring Boot default), no custom config file |
| Declaration style | Manual `private static final Logger log = LoggerFactory.getLogger(X.class)` in each class |
| Output | Console only, single pattern for all environments |
| Correlation | `RequestCorrelationFilter` puts a `requestId` into MDC per request |
| Client context | `AnalyticsClientIdFilter` puts `analyticsClientId` / `analyticsClientPlatform` into MDC |
| Pattern | `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %X{requestId:-} %-5level %logger{36} - %msg%n` (`application.yml`) |
| Levels | Only `org.springframework` and `org.hibernate.SQL` set to `info`; no app package tuning |
| Log calls | ~14 classes across push, insight, OCR, billing, stock, goal, analytics modules call `log.info/warn/error` directly with key=value-style messages (e.g. `push_token_removed userId={} reason=unregistered`) |

This is a reasonable starting point — SLF4J is the right facade, MDC-based correlation already exists, and log messages already lean toward a semi-structured `event_name key=value` convention. The gaps are consistency, per-environment output, and use of MDC context that's captured but never rendered.

## Gaps

1. **No `logback-spring.xml`.** Everything is forced through one console pattern, in one format, from `application.yml`. No file appender, no rotation, no JSON output for shipping to a log aggregator (ELK, CloudWatch, Datadog, etc.).
2. **Boilerplate logger declarations.** Every class hand-writes `LoggerFactory.getLogger(...)`, even though Lombok (already a dependency) provides `@Slf4j` to generate the same field.
3. **Dropped MDC context.** `AnalyticsClientIdFilter` populates `analyticsClientId` and `analyticsClientPlatform` in MDC, but the console pattern only renders `requestId` — that data is computed and then silently discarded.
4. **No log level strategy per package.** Only Spring/Hibernate noise is tuned down; there's no `com.saveapenny: info` (or per-module) baseline, so a `debug` flip would flood output with framework logs at the same volume as application logs.
5. **No structured/JSON encoder.** Since this is a fintech app likely to ship logs to an aggregator eventually, unstructured console text makes searching/alerting (e.g. "all `push_notification_send_failed` for user X") harder than it needs to be.
6. **No documented sensitive-data policy.** Nothing prevents a future log line from including a JWT, refresh token, password, or full account/transaction payload. Given this handles financial data, that's worth being explicit about before it becomes a habit.

## Proposed Design

### 1. `logback-spring.xml` with explicit appenders

Add `src/main/resources/logback-spring.xml`, replacing the pattern currently embedded in `application.yml`:

- One human-readable console appender can remain the default for local development.
- One JSON console appender (e.g. `logstash-logback-encoder`) can be enabled explicitly when logs need to feed an aggregator.
- Both output modes should render the full MDC map (not just `requestId`), so `analyticsClientId`, `analyticsClientPlatform`, and any future MDC keys show up automatically without pattern edits.

### 2. Standardize on Lombok `@Slf4j`

Since Lombok is already a dependency, replace the 14 manual `LoggerFactory.getLogger(X.class)` declarations with `@Slf4j` at the class level. Purely mechanical, removes repeated boilerplate, and is the idiomatic Spring+Lombok pattern. New classes should use `@Slf4j` from the start — worth a one-line note in the contribution guide or a Checkstyle/ArchUnit rule if the codebase already enforces conventions that way.

### 3. Package-level log level baseline

In `application.yml`, add an explicit application baseline alongside the existing framework tuning:

```yaml
logging:
  level:
    org.springframework: info
    org.hibernate.SQL: info
    com.saveapenny: info
```

This lets a developer bump `com.saveapenny: debug` locally (or per-module, e.g. `com.saveapenny.push: debug`) without also enabling Spring/Hibernate debug noise.

### 4. Keep (and document) the existing semi-structured message convention

Several classes already log in a `event_name key=value key=value` style (e.g. `push_token_removed userId={} reason=unregistered` in `FcmPushNotificationSender`). This is a good convention — it's grep-able and becomes trivially structured once JSON encoding is in place. Worth writing down as the house style so it's applied consistently rather than emerging ad hoc:

- First token: a stable, snake_case event name (`push_notification_send_failed`, not a free-form sentence).
- Followed by `key=value` pairs for anything you'd want to filter or alert on.
- Reserve `error`/`warn` for actionable failures; `info` for lifecycle events; `debug` for diagnostic detail not needed in production by default.

### 5. Sensitive-data rule

Document (README or `docs/security.md`) that log statements must never include: JWTs/refresh tokens, passwords, full request/response bodies containing PII, or raw financial account numbers. Where an identifier is useful for correlation, log the entity ID (`userId`, `accountId`), never the payload. This costs nothing to state now and prevents an accidental leak later — the codebase already scopes data by `user_id`, so `userId=...` in logs is consistent with that model and low-risk.

## Suggested Rollout Order

1. `logback-spring.xml` + explicit output-mode split (highest value, zero behavior change to existing call sites).
2. Add `com.saveapenny` level baseline to `application.yml`.
3. Sweep the 14 existing files to `@Slf4j` (mechanical, low-risk, one PR).
4. Add the sensitive-data note to `docs/security.md`.

Each step is independently shippable and none require touching business logic.
