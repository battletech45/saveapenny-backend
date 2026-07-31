# Feedback

## Overview

The feedback module lets authenticated users submit product feedback directly from the app and review their own past submissions later. Feedback is user-scoped and supports three categories: general feedback, feature requests, and bug reports.

## Feedback Types

| Type | Purpose |
|------|---------|
| `GENERAL` | Broad comments, impressions, or suggestions |
| `FEATURE_REQUEST` | Requests for new capabilities or UX improvements |
| `BUG_REPORT` | Reports about broken flows, crashes, or incorrect behavior |

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `type` | Yes | `GENERAL`, `FEATURE_REQUEST`, or `BUG_REPORT` |
| `rating` | No | Integer from `1` to `5` |
| `message` | Yes | Main feedback text, max 5000 chars |
| `metadata` | No | Free-form JSON for client context such as app version, platform, screen, or device |

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/feedback` | Submit feedback |
| GET | `/api/v1/feedback` | List current user's feedback (shared paginated response) |
| GET | `/api/v1/feedback/{id}` | Get one feedback item owned by the current user |
| DELETE | `/api/v1/feedback/{id}` | Delete one feedback item owned by the current user |

## List Response Shape

`GET /api/v1/feedback` returns the shared pagination contract from [API Reference](../api-reference.md). Feedback records are returned in `items`.

## Query Filters

`GET /api/v1/feedback` supports:

| Parameter | Type | Description |
|-----------|------|-------------|
| `type` | String | Optional type filter |
| `page` | Integer | Page number (0-based) |
| `size` | Integer | Page size |
| `sort` | String | Sort field and direction |

## Ownership Rules

- Feedback is always tied to the authenticated user
- Users can list, read, and delete only their own feedback
- Looking up another user's feedback returns `404 FEEDBACK_NOT_FOUND`
- The module does not include an admin review workflow in this version

## Example Request

```json
{
  "type": "FEATURE_REQUEST",
  "rating": 5,
  "message": "Please add home-screen widgets for account balances.",
  "metadata": {
    "platform": "ios",
    "appVersion": "2.3.1",
    "screen": "settings"
  }
}
```

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `FEEDBACK_NOT_FOUND` | 404 | Feedback item does not exist or is not owned by the caller |
| `VALIDATION_FAILED` | 400 | Request body validation fails |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Authenticated-only submissions | Matches the rest of the app's user-scoped model and allows follow-up per user |
| Free-form metadata JSON | Lets the frontend attach app/version/screen context without schema churn |
| Hard delete | Feedback is user-submitted content, not financial ledger data or audit history |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/feedback/entity/Feedback.java` | JPA entity |
| `src/main/java/com/saveapenny/feedback/controller/FeedbackController.java` | REST endpoints |
| `src/main/java/com/saveapenny/feedback/service/impl/FeedbackServiceImpl.java` | Business logic |
| `src/main/resources/db/migration/V25__add_feedback.sql` | Database schema |
