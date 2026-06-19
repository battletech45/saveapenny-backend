# Rate Limiting

## Overview

Rate limiting protects the API from abuse and ensures fair resource usage. The implementation uses a per-client token bucket algorithm backed by a `ConcurrentHashMap`. Rate limiting is applied only on POST requests to `/api/v1/*` paths.

## Scope

| Method | Rate Limited | Notes |
|--------|-------------|-------|
| POST | Yes | Login and business API endpoints |
| GET | No | Currently unlimited |
| PUT | No | Currently unlimited |
| PATCH | No | Currently unlimited |
| DELETE | No | Currently unlimited |

## Limits

| Bucket | Limit | Scope | Key Derivation |
|--------|-------|-------|----------------|
| Login | 5 requests/minute | Per IP address | Client IP (respects `X-Forwarded-For`) |
| General API | 60 requests/minute | Per authenticated user | User ID (falls back to IP for anonymous requests) |

## Response When Rate Limited

When the limit is exceeded, the API returns HTTP `429 Too Many Requests`:

```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMITED",
    "message": "Too many requests. Please wait before retrying."
  },
  "timestamp": "2026-06-10T12:00:00Z"
}
```

The response includes a `Retry-After` header:

```text
Retry-After: 60
```

Mobile clients must respect this header and wait the specified number of seconds before retrying.

## Response Headers

Rate-limited responses include a `Retry-After` header indicating seconds to wait before retrying.

## Configuration

```yaml
rate-limit:
  login:
    max-per-minute: 5
  api:
    max-per-minute: 60
```

These values can be overridden via environment variables (Spring property binding):

| Variable | Default | Description |
|----------|---------|-------------|
| `RATE_LIMIT_LOGIN_MAX_PER_MINUTE` | `5` | Login POST requests per minute per IP |
| `RATE_LIMIT_API_MAX_PER_MINUTE` | `60` | API POST requests per minute per user |

## Client Best Practices

1. **Respect Retry-After** — never retry before the specified delay
2. **Exponential backoff** — increase wait time with each consecutive rate-limited response
3. **Avoid aggressive polling** — cache data locally and poll at reasonable intervals
4. **Queue non-critical writes** — defer non-urgent POST requests if the user is approaching the limit
5. **Monitor rate limit status** — track `Retry-After` and implement backoff strategies

## Implementation Details

| Aspect | Detail |
|--------|--------|
| Algorithm | Token bucket (tokens refill every 60 seconds to max capacity) |
| Storage | In-memory `ConcurrentHashMap` (no persistence) |
| Thread safety | `AtomicInteger` for tokens, double-checked locking for refill |
| Key resolution | Login: client IP; API: user ID (falls back to IP) |
| Proxy support | `X-Forwarded-For` header respected for IP extraction |

### Limitations

- **Single-node only** — state resets on server restart; not distributed
- **Memory-bound** — keys accumulate until server restart (no eviction of idle keys)
- **POST-only** — read operations are not rate-limited

For multi-node deployments, a Redis-backed implementation would be needed.

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Token bucket over sliding window | Simple, correct, well-understood; `AtomicInteger` makes it lock-free for common path |
| In-memory over Redis | Zero infrastructure dependency; appropriate for single-node MVP |
| POST-only rate limiting | Writes are mutation-heavy and more expensive; reads are intentionally unbounded |
| Per-user over per-IP for API | Prevents one user from exhausting a shared IP pool (NAT, mobile carrier) |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/config/security/RateLimiter.java` | Token bucket implementation |
| `src/main/java/com/saveapenny/config/security/RateLimitingFilter.java` | HTTP filter that applies rate limits |
| `src/main/java/com/saveapenny/config/security/RateLimitProperties.java` | Configuration properties record |
| `src/main/resources/application.yml` | Rate limit configuration defaults |
| `src/test/java/com/saveapenny/config/security/RateLimiterTest.java` | Token bucket unit tests |
| `src/test/java/com/saveapenny/config/security/RateLimitingFilterTest.java` | Filter behaviour tests |
