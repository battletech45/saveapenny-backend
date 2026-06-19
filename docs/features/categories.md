# Categories

## Overview

Categories organize income and expense transactions. The system provides default categories seeded on application startup, and users can create their own. Categories are user-scoped for custom categories; system categories have no `userId` and are shared across all users.

## Category Types

| Type | Description |
|------|-------------|
| `INCOME` | Income categories (e.g., Salary, Freelance) |
| `EXPENSE` | Expense categories (e.g., Groceries, Rent) |

## System vs User Categories

| Attribute | System Categories | User Categories |
|-----------|------------------|-----------------|
| Created by | Backend on startup | User via API |
| `userId` | `null` | Set to the creating user |
| Editable | No | Yes |
| Deletable | No | Yes |
| Listed | Always included | Included for the owning user only |

## Fields

| Field | Required | Notes |
|-------|----------|-------|
| `name` | Yes | Must be unique per user (including deleted categories) |
| `type` | Yes | `INCOME` or `EXPENSE` |
| `color` | No | Hex color code for UI display (e.g., `#FF5733`) |
| `icon` | No | Icon identifier for UI display |

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/categories` | Create a user category |
| GET | `/api/v1/categories` | List categories (system + user) |
| GET | `/api/v1/categories/{id}` | Get category details |
| PUT | `/api/v1/categories/{id}` | Update category name, color, or icon |
| DELETE | `/api/v1/categories/{id}` | Delete a user category |

## Rules

- Category names must be unique per user (no duplicate names)
- System categories cannot be modified or deleted
- Deleting a category does **not** delete transactions using it
- Filter by type: `GET /api/v1/categories?type=EXPENSE`

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `CATEGORY_NOT_FOUND` | 404 | Category not found or not owned by the caller |
| `CATEGORY_NAME_ALREADY_EXISTS` | 409 | Name conflicts with another category |
| `SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED` | 400 | Attempt to modify or delete a system category |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| System categories seeded on startup | Provides immediate usability; consistent base categories across all users |
| Immutable system categories | Prevents accidental modification of base categories; users extend with their own |
| Category deletion does not cascade to transactions | Preserves historical financial data integrity |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/category/entity/Category.java` | JPA entity |
| `src/main/java/com/saveapenny/category/controller/CategoryController.java` | REST endpoints |
| `src/main/java/com/saveapenny/category/service/impl/CategoryServiceImpl.java` | Business logic |
