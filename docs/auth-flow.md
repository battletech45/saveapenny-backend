# Authentication Flow

## Overview

SaveAPenny uses a dual-token auth system:

- **Access token** — short-lived JWT (15 min), carried in `Authorization: Bearer` header
- **Refresh token** — long-lived opaque token (7 days), used only to obtain new access tokens

Both tokens are issued on login and registration. Refresh tokens support rotation — each refresh invalidates the previous token and issues a new pair.

## Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/auth/register` | POST | None | Create account |
| `/api/v1/auth/login` | POST | None | Authenticate |
| `/api/v1/auth/refresh` | POST | None | Rotate tokens |
| `/api/v1/auth/logout` | POST | None | Revoke refresh token |

## Token Lifecycle

```
Register / Login
    │
    ├── accessToken (JWT, 15 min)
    └── refreshToken (opaque, 7 days)
              │
              ▼
        Refresh (rotate)
              │
              ├── new accessToken (15 min)
              └── new refreshToken (7 days)
                    │
                    ├── Refresh → rotate again
                    └── Logout → revoked
```

## Mobile Client Implementation

### Token Storage

Store both tokens in secure platform storage:

- **iOS**: `kSecClassGenericPassword` (Keychain)
- **Android**: `EncryptedSharedPreferences`

Never store tokens in plaintext preferences, `UserDefaults`, or `SharedPreferences`.

### Auto-Refresh Strategy

Before each API call, check if the access token is expired or expires within 60 seconds. If so, refresh proactively:

```
1. Check access token expiry (decode JWT `exp` claim)
2. If expired or expires in < 60s:
   a. Call POST /api/v1/auth/refresh with current refreshToken
   b. Store new accessToken and refreshToken
   c. Retry original request with new accessToken
3. If refresh fails (401):
   a. Clear stored tokens
   b. Redirect to login
```

### Why Proactive Refresh?

Proactive refresh avoids race conditions where multiple in-flight requests all attempt to refresh simultaneously. For a simpler approach, intercept 401 responses and attempt a single refresh before retrying.

### Password Change Behavior

Changing passwords via `PUT /api/v1/users/me/password` revokes **all** active refresh tokens for that user. After a password change:

- The current access token remains valid until it expires (15 min max)
- The refresh token is immediately revoked
- The user must log in again to obtain new tokens

### Token Reuse Detection

If a stolen refresh token is used after the legitimate token has already been rotated, the legitimate user's next refresh will fail with `401 INVALID_REFRESH_TOKEN`. Mobile clients should handle this by clearing tokens and redirecting to login.

## Response Shapes

### Login / Register

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<opaque>",
    "expiresIn": 900,
    "tokenType": "Bearer"
  }
}
```

### Refresh

```json
{
  "success": true,
  "data": {
    "accessToken": "<new-jwt>",
    "refreshToken": "<new-opaque>",
    "expiresIn": 900,
    "tokenType": "Bearer"
  }
}
```

### Error

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REFRESH_TOKEN",
    "message": "Invalid or expired refresh token"
  }
}
```

## Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| `INVALID_CREDENTIALS` | 401 | Email or password is wrong |
| `INVALID_REFRESH_TOKEN` | 401 | Token is invalid, expired, or revoked |
| `EMAIL_ALREADY_EXISTS` | 409 | Registration email is taken |
| `RATE_LIMITED` | 429 | Too many login attempts |

## Public Endpoints

These do not require authentication:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /actuator/health`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`
