# OCR Receipt Processing

## Overview

OCR (Optical Character Recognition) extracts text from receipt and document images using Tesseract. It is an optional feature enabled by default (requires Tesseract to be installed on the server). Users upload images (PNG, JPEG, PDF) and receive extracted text and parsed candidates (amounts, dates, merchants).

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| Tesseract OCR engine | Must be installed on the server |
| Tessdata language files | Available at configured path or auto-detected platform path |
| Supported formats | PNG, JPEG, PDF |
| Maximum file size | 10 MB |

Common auto-detected tessdata paths:

| Platform | Path |
|----------|------|
| macOS Homebrew | `/opt/homebrew/share/tessdata` |
| Linux (apt) | `/usr/share/tesseract-ocr/5/tessdata` or `/usr/share/tessdata` |
| Windows | `C:\Program Files\Tesseract-OCR\tessdata` |

## Enabling OCR

```yaml
ocr:
  enabled: true
  tessdata-path: /opt/homebrew/share/tessdata  # optional; auto-detected if omitted
```

Or via environment variable:

```bash
OCR_TESSDATA_PATH=/opt/homebrew/share/tessdata
```

## Workflow

```
Upload image ──▶ Receive jobId ──▶ Poll job status ──▶ Review results
```

### Step 1: Upload

`POST /api/imports/ocr`

```bash
curl -X POST "http://localhost:8080/api/imports/ocr" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@receipt.jpg"
```

Returns HTTP `202 Accepted` with a `jobId`.

### Step 2: Poll Status

`GET /api/imports/ocr/{jobId}`

| Status | Meaning |
|--------|---------|
| `PENDING` | Job queued, not yet processed |
| `PROCESSING` | OCR is running |
| `COMPLETED` | Text extracted, results available |
| `FAILED` | Processing error |

### Step 3: Review Results

When the job completes, the response includes:

- **Extracted text** — Raw OCR output
- **Parsed candidates** — Structured fields (amounts, dates, merchants) parsed from the text

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/imports/ocr` | Upload a document for OCR processing |
| GET | `/api/imports/ocr/{jobId}` | Get job status and results |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `ocr.enabled` | `true` | Enable OCR processing |
| `ocr.tessdata-path` | auto-detected | Path to Tesseract language data |
| `ocr.language` | `eng` | Tesseract language pack |
| `ocr.max-file-size-bytes` | `10485760` | Max upload size (10 MB) |
| `ocr.job-timeout-millis` | `30000` | Per-job timeout (30 seconds) |
| `ocr.max-retries` | `2` | Retry count on processing failure |

## Performance Characteristics

- OCR is CPU-intensive; processing runs asynchronously with configurable timeout
- Default timeout: 30 seconds per job
- Maximum retries: 2
- Concurrent processing is limited by available CPU resources

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `OCR_JOB_NOT_FOUND` | 404 | Job ID not found or not owned by the caller |
| `INVALID_OCR_FILE` | 400 | File exceeds size limit or has unsupported format |
| `OCR_PROCESSING_FAILED` | 400 | Tesseract encountered an unrecoverable error |
| `INVALID_OCR_FILE` | 400 | OCR is disabled, the file exceeds size limits, or the file format is unsupported |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Async processing with polling | OCR is CPU-intensive; synchronous processing would block request threads |
| Tesseract over cloud OCR API | No external API calls; works offline; no per-page costs |
| Job timeout with retries | Prevents runaway OCR jobs; transient failures are automatically retried |
| Enabled by default | OCR requires native dependencies but Tesseract is included in the Docker image |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/ocr/domain/model/OcrJob.java` | OCR job entity |
| `src/main/java/com/saveapenny/ocr/interfaces/http/OcrImportController.java` | REST endpoints |
| `src/main/java/com/saveapenny/ocr/infrastructure/engine/tesseract/TesseractOcrService.java` | Tesseract integration |
| `src/main/resources/application.yml` | OCR configuration |
