# SaveAPenny API Endpoints (v1)

Base URL: `/api/v1`

Protected routes require:

`Authorization: Bearer <accessToken>`

## Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": ["email: must be a well-formed email address"]
  },
  "timestamp": "2026-05-12T10:30:00Z"
}
```

## Auth Endpoints

### POST `/auth/register`

Request:

```json
{
  "email": "john.doe@example.com",
  "password": "strong-pass-123",
  "fullName": "John Doe"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-access-token>",
    "refreshToken": "<refresh-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `409` `EMAIL_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### POST `/auth/login`

Request:

```json
{
  "email": "john.doe@example.com",
  "password": "strong-pass-123"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-access-token>",
    "refreshToken": "<refresh-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `INVALID_CREDENTIALS`
- `400` `VALIDATION_FAILED`

### POST `/auth/refresh`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "<new-jwt-access-token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `INVALID_REFRESH_TOKEN`
- `401` `REFRESH_TOKEN_EXPIRED`
- `400` `VALIDATION_FAILED`

### POST `/auth/logout`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

## User Endpoints

### GET `/users/me`

Headers:

`Authorization: Bearer <accessToken>`

Response `200`:

```json
{
  "success": true,
  "data": {
    "id": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "active": true,
    "createdAt": "2026-05-11T15:13:00Z",
    "updatedAt": "2026-05-12T09:20:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `USER_NOT_FOUND`

### PUT `/users/me`

Headers:

`Authorization: Bearer <accessToken>`

Request:

```json
{
  "fullName": "John A. Doe"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "id": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
    "email": "john.doe@example.com",
    "fullName": "John A. Doe",
    "active": true,
    "createdAt": "2026-05-11T15:13:00Z",
    "updatedAt": "2026-05-12T10:28:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `USER_NOT_FOUND`
- `400` `VALIDATION_FAILED`

### PUT `/users/me/password`

Headers:

`Authorization: Bearer <accessToken>`

Request:

```json
{
  "currentPassword": "strong-pass-123",
  "newPassword": "new-strong-pass-456"
}
```

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T10:30:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `USER_NOT_FOUND`
- `400` `INVALID_PASSWORD`
- `400` `PASSWORD_REUSE_NOT_ALLOWED`
- `400` `VALIDATION_FAILED`

## Quick cURL

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"strong-pass-123"}'
```

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```
