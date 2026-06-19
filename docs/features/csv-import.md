# CSV Import

## Overview

The CSV import workflow lets users bulk-import transactions from bank or accounting CSV files. The flow uses a preview-confirm-status pattern that gives users control before committing data to the ledger.

## Workflow

```
Upload CSV ──▶ Preview ──▶ Confirm ──▶ Poll Status
                   │                        │
                   └── validation errors     └── PENDING / PROCESSING / COMPLETED / FAILED
```

### Step 1: Preview

`POST /api/v1/imports/transactions/preview`

Upload the CSV file. The endpoint parses the file, returns parsed rows with validation feedback, and returns an `importId`.

```bash
curl -X POST "http://localhost:8080/api/v1/imports/transactions/preview" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@transactions.csv"
```

Response includes:

| Field | Description |
|-------|-------------|
| `importId` | UUID used in subsequent confirm/poll steps |
| `rows` | Array of parsed transactions with per-row status |
| `totalCount` | Total rows parsed |
| `validCount` | Rows ready for import |

Per-row status values:

| Status | Meaning |
|--------|---------|
| `VALID` | Row parsed successfully and ready to import |
| `WARNING` | Row parsed but has non-blocking issues |
| `ERROR` | Row cannot be imported (invalid data) |

### Step 2: Confirm

`POST /api/v1/imports/transactions/confirm`

```json
{
  "importId": "<importId>"
}
```

Starts the async import process. Returns immediately — the import runs asynchronously in the background.

### Step 3: Poll Status

`GET /api/v1/imports/transactions/{importId}/status`

Poll until status reaches a terminal state:

| Status | Meaning |
|--------|---------|
| `PENDING` | Waiting to be processed |
| `PROCESSING` | Import is actively running |
| `COMPLETED` | All valid rows imported |
| `FAILED` | Import encountered an unrecoverable error |
| `PARTIALLY_COMPLETED` | Some rows imported, some failed |

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/imports/transactions/preview` | Upload and preview CSV |
| POST | `/api/v1/imports/transactions/confirm` | Confirm and start import |
| GET | `/api/v1/imports/transactions/{importId}/status` | Poll import status |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `INVALID_IMPORT_FILE` | 400 | CSV file cannot be parsed or has invalid format |
| `IMPORT_NOT_FOUND` | 404 | Import ID does not exist or is not owned by the caller |
| `IMPORT_ALREADY_RUNNING` | 409 | An import is already in progress |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Preview before confirm | Users verify parsed data before committing; prevents mass incorrect imports |
| Async import processing | Large CSV files do not block the request thread |
| Per-row validation status | Granular feedback — users see exactly which rows will or will not be imported |
| Idempotent confirm | Confirming the same importId twice does not create duplicate transactions |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/imports/controller/ImportController.java` | REST endpoints |
| `src/main/java/com/saveapenny/imports/service/impl/CsvImportServiceImpl.java` | CSV parsing and import orchestration |
