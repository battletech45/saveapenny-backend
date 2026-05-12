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

## Account Endpoints

All account endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/accounts`

Request:

```json
{
  "name": "Wallet",
  "type": "CASH",
  "currency": "USD",
  "initialBalance": 250.0000
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "id": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
    "name": "Wallet",
    "type": "CASH",
    "currency": "USD",
    "balance": 250.0000,
    "initialBalance": 250.0000,
    "active": true,
    "createdAt": "2026-05-12T13:00:00Z",
    "updatedAt": "2026-05-12T13:00:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T13:00:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `409` `ACCOUNT_NAME_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### GET `/accounts`

Supports `page`, `size`, `sort` query params via Spring `Pageable`.

Response `200`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "1e45d9a5-7a63-4c33-b4b9-6db7e12f45ab",
        "name": "Wallet",
        "type": "CASH",
        "currency": "USD",
        "balance": 250.0000,
        "initialBalance": 250.0000,
        "active": true,
        "createdAt": "2026-05-12T13:00:00Z",
        "updatedAt": "2026-05-12T13:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "error": null,
  "timestamp": "2026-05-12T13:00:00Z"
}
```

### GET `/accounts/{accountId}`

Response `200`: same `AccountResponse` payload as create/update.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `ACCOUNT_NOT_FOUND`

## Category Endpoints

All category endpoints require:

`Authorization: Bearer <accessToken>`

### POST `/categories`

Request:

```json
{
  "name": "Food",
  "type": "EXPENSE",
  "color": "#ff0000",
  "icon": "utensils"
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "id": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
    "userId": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
    "name": "Food",
    "type": "EXPENSE",
    "color": "#ff0000",
    "icon": "utensils",
    "createdAt": "2026-05-12T14:45:00Z",
    "updatedAt": "2026-05-12T14:45:00Z"
  },
  "error": null,
  "timestamp": "2026-05-12T14:45:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `409` `CATEGORY_NAME_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### GET `/categories?type=INCOME|EXPENSE`

Returns system categories (`userId=null`) plus categories owned by the current user.

Response `200`:

```json
{
  "success": true,
  "data": [
    {
      "id": "f3672791-2ca5-4b5f-aaf4-46de79f4b324",
      "userId": null,
      "name": "Salary",
      "type": "INCOME",
      "color": "#00aa00",
      "icon": "briefcase",
      "createdAt": "2026-05-01T10:00:00Z",
      "updatedAt": "2026-05-01T10:00:00Z"
    },
    {
      "id": "2e7f71b7-e5e7-4f11-8db0-0cb17f2dbd7d",
      "userId": "7f7bcf67-27aa-46eb-a5f6-3bc52d278e39",
      "name": "Food",
      "type": "EXPENSE",
      "color": "#ff0000",
      "icon": "utensils",
      "createdAt": "2026-05-12T14:45:00Z",
      "updatedAt": "2026-05-12T14:45:00Z"
    }
  ],
  "error": null,
  "timestamp": "2026-05-12T14:45:00Z"
}
```

### GET `/categories/{categoryId}`

Response `200`: same `CategoryResponse` payload as create.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`

### PUT `/categories/{categoryId}`

Request:

```json
{
  "name": "Groceries",
  "type": "EXPENSE",
  "color": "#00ff00",
  "icon": "basket"
}
```

Response `200`: same `CategoryResponse` payload as create.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`
- `409` `CATEGORY_NAME_ALREADY_EXISTS`
- `400` `SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED`
- `400` `VALIDATION_FAILED`

### DELETE `/categories/{categoryId}`

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T14:45:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `CATEGORY_NOT_FOUND`
- `400` `SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED`

### PUT `/accounts/{accountId}`

Request:

```json
{
  "name": "Main Wallet",
  "type": "BANK",
  "currency": "EUR"
}
```

Response `200`: same `AccountResponse` payload as create.

Common errors:

- `401` `ACCESS_DENIED`
- `404` `ACCOUNT_NOT_FOUND`
- `409` `ACCOUNT_NAME_ALREADY_EXISTS`
- `400` `VALIDATION_FAILED`

### DELETE `/accounts/{accountId}`

Soft deletes account (`active=false`).

Response `200`:

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-05-12T13:00:00Z"
}
```

Common errors:

- `401` `ACCESS_DENIED`
- `404` `ACCOUNT_NOT_FOUND`

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

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Wallet","type":"CASH","currency":"USD","initialBalance":250.0000}'
```

```bash
curl -X GET "http://localhost:8080/api/v1/categories?type=EXPENSE" \
  -H "Authorization: Bearer <access-token>"
```
