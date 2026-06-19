# Audit Logs

## Overview

Audit logs track changes to key resources for accountability, debugging, and compliance. Audit entries are primarily created automatically by the backend, but can also be created manually via API. Entries cannot be modified or deleted once created. Audit logs are user-scoped.

## Tracked Resources

| Resource | Events Tracked |
|----------|---------------|
| Transactions | Create, update, delete |
| Accounts | Create, update, delete |
| Categories | Create, update, delete |
| Budgets | Create, update, delete |
| Recurring Transactions | Create, update, delete, pause, resume |
| Users | Profile updates, password changes |

## Audit Entry Contents

| Field | Description |
|-------|-------------|
| `id` | UUID |
| `userId` | The user who performed the action |
| `entityType` | Resource type (e.g., `TRANSACTION`, `ACCOUNT`) |
| `entityId` | UUID of the affected resource |
| `action` | Action performed (e.g., `CREATE`, `UPDATE`, `DELETE`) |
| `changes` | JSON diff of changed fields (for updates) |
| `createdAt` | When the action occurred |

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/audits` | Create an audit entry manually |
| GET | `/api/v1/audits` | List audit logs (paginated, filterable) |
| GET | `/api/v1/audits/{id}` | Get audit entry details |

## Query Filters

`GET /api/v1/audits` supports:

| Parameter | Type | Description |
|-----------|------|-------------|
| `entityType` | String | Filter by resource type |
| `entityId` | UUID | Filter by resource ID |
| `action` | String | Filter by action |
| `from` | Date | Start date (inclusive) |
| `to` | Date | End date (inclusive) |
| `page` | Integer | Page number (0-based) |
| `size` | Integer | Page size |

## Example

```bash
curl -X GET "http://localhost:8080/api/v1/audits?entityType=TRANSACTION&page=0&size=20" \
  -H "Authorization: Bearer <accessToken>"
```

## Rules

- Audit logs are **append-only** — entries can be created, but cannot be modified or deleted via the API
- Diff data is stored as a JSON string for schema flexibility
- Soft-deleted resources retain their full audit history
- Only the owning user can access their audit logs

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `AUDIT_LOG_NOT_FOUND` | 404 | Audit entry not found or not owned by the caller |
| `AUDIT_LOG_ACCESS_DENIED` | 403 | Attempt to access another user's audit log |
| `INVALID_AUDIT_DATE_RANGE` | 400 | Date range parameters are invalid |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Append-only entries | Tamper-evident: once created, audit data cannot be altered |
| JSON diff for changes | Flexible schema; captures any field change without schema changes |
| User-scoped access | Each user sees only their own audit trail |
| No delete endpoint | Prevents audit trail tampering |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/audit/entity/AuditLog.java` | JPA entity |
| `src/main/java/com/saveapenny/audit/controller/AuditLogController.java` | REST endpoints |
| `src/main/java/com/saveapenny/audit/service/impl/AuditServiceImpl.java` | Audit event creation and query logic |
