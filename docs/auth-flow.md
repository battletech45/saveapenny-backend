# Authentication Flow

## Overview

SaveAPenny uses a dual-token authentication system: short-lived JWTs for stateless API access and opaque refresh tokens for secure credential rotation. This design avoids server-side session storage while enabling token revocation.

| Token | Format | Expiry | Revocable | Stored Server-Side |
|-------|--------|--------|-----------|-------------------|
| **Access token** | JWT (HS512-signed) | 15 minutes | No (stateless) | No |
| **Refresh token** | Opaque Base64URL string | 7 days | Yes | Stored in database |

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | None | Create account, return token pair |
| POST | `/api/v1/auth/login` | None | Authenticate, return token pair |
| POST | `/api/v1/auth/refresh` | None | Rotate refresh token, return new pair |
| POST | `/api/v1/auth/logout` | None | Revoke refresh token |

`POST /api/v1/auth/login` is rate-limited at 5 requests/min per IP. Other `POST /api/v1/*` endpoints use the general API bucket. See [Rate Limiting](rate-limiting.md).

## Token Lifecycle

```
Register / Login
    │
    ├── accessToken (JWT, 15 min, stateless)
    └── refreshToken (opaque Base64URL string, 7 days, stored in DB)
              │
              ▼
          Refresh (rotate)
              │
              ├── new accessToken (15 min)
              └── new refreshToken (7 days, old one revoked)
                    │
                    ├── Refresh → rotate again
                    └── Logout → revoked

Password change
    │
    └── All refresh tokens for user revoked
        (existing access tokens remain valid until expiry)
```

## Response Shapes

### Login / Register

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<opaque-token>",
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
    "refreshToken": "<new-opaque-token>",
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

## Mobile Client Implementation

### Token Storage

| Platform | Storage Mechanism |
|----------|------------------|
| iOS | `kSecClassGenericPassword` (Keychain) |
| Android | `EncryptedSharedPreferences` |

Never store tokens in `UserDefaults`, `SharedPreferences`, or plaintext files.

### Auto-Refresh Strategy

Proactive refresh avoids race conditions where multiple in-flight requests all attempt to refresh simultaneously:

```
Before each API call:
1. Decode JWT `exp` claim
2. If expired or expires in < 60 seconds:
   a. POST /api/v1/auth/refresh with current refreshToken
   b. Store new accessToken and refreshToken
   c. Retry original request with new accessToken
3. If refresh fails (401):
   a. Clear stored tokens
   b. Redirect to login screen
```

### Reactive Fallback

For simpler clients, intercept 401 responses and attempt a single refresh before retrying the original request. If the refresh also returns 401, clear tokens and redirect to login.

### Token Reuse Detection

Refresh tokens are rotated on every use. If a stolen token is used after the legitimate token has already been rotated, the next legitimate refresh attempt returns `401 INVALID_REFRESH_TOKEN`. Clients should handle this by clearing tokens and redirecting to login.

## Password Change Behavior

`PUT /api/v1/users/me/password` revokes **all** active refresh tokens for the user:

- The current access token remains valid until it expires (max 15 minutes)
- The refresh token is immediately revoked
- The user must log in again to obtain new tokens

## Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| `INVALID_CREDENTIALS` | 401 | Email or password is incorrect |
| `INVALID_REFRESH_TOKEN` | 401 | Token is invalid, expired, or has been rotated |
| `REFRESH_TOKEN_EXPIRED` | 401 | Token is past its 7-day expiry |
| `EMAIL_ALREADY_EXISTS` | 409 | Registration email is already in use |
| `RATE_LIMITED` | 429 | Too many login attempts (5/min per IP) |

## Public Endpoints

These endpoints do not require authentication:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /actuator/health`
- `GET /v3/api-docs/**`
- `GET /swagger-ui/**`

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Access tokens are not revocable | Stateless verification avoids DB lookup on every request; short expiry limits exposure |
| Refresh tokens are opaque random strings | Enables lookup and rotation without exposing JWT claims in the token itself |
| Rotation on every refresh | Limits the window for token theft; stolen tokens are invalidated after first legitimate use |
| Proactive refresh before expiry | Avoids race conditions from concurrent 401 handling |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/auth/service/JwtService.java` | JWT creation, parsing, validation |
| `src/main/java/com/saveapenny/auth/service/RefreshTokenService.java` | Refresh token generation, validation, rotation |
| `src/main/java/com/saveapenny/auth/controller/AuthController.java` | REST endpoints for register, login, refresh, logout |
| `src/main/java/com/saveapenny/config/security/HeaderUserAuthenticationFilter.java` | Extracts JWT from `Authorization` header, sets security context |
| `src/main/resources/application.yml` | `security.jwt.secret` and `security.jwt.refresh-token-expiry-days` |
