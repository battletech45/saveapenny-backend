# Rate Limiting

## Overview

Rate limiting protects the API from abuse and ensures fair resource usage. The implementation uses a token bucket algorithm per client.

## Scope

| Method | Rate Limited | Notes |
|--------|-------------|-------|
| POST | Yes | Login and API endpoints |
| GET | No | Currently unlimited |
| PUT | No | Currently unlimited |
| PATCH | No | Currently unlimited |
| DELETE | No | Currently unlimited |

## Limits

| Bucket | Limit | Scope | Key |
|--------|-------|-------|-----|
| Login | 5 requests per minute | Per IP address | Client IP |
| General API | 60 requests per minute | Per authenticated user | User ID (or IP for anonymous) |

## Response When Rate Limited

HTTP `429 Too Many Requests` with a consistent error body:

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

The response includes the `Retry-After` header:

```text
Retry-After: 60
```

Mobile clients must respect this header and wait the specified number of seconds before retrying.

## Headers

Future responses may include the following headers for client-side visibility (not yet implemented):

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Maximum requests per window |
| `X-RateLimit-Remaining` | Requests left in current window |
| `X-RateLimit-Reset` | Seconds until the bucket refills |

## Mobile Client Best Practices

1. **Respect Retry-After** — never retry before the specified delay
2. **Exponential backoff** — if retrying after a rate-limited response, increase wait time with each attempt
3. **Avoid aggressive polling** — cache data locally and poll at reasonable intervals
4. **Queue non-critical writes** — defer non-urgent POST requests if the user is approaching the limit

## Implementation Details

- The rate limiter uses an in-memory token bucket (`ConcurrentHashMap`)
- State resets on server restart (no persistence)
- Single-node only — no distributed rate limiting
- For multi-node deployments, a Redis-backed implementation would be needed
