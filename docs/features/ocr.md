# OCR Receipt Processing

## Overview

OCR (Optical Character Recognition) extracts text from receipt and document images. It is an optional feature disabled by default.

## Enabling OCR

Set the following in `application.yml`:

```yaml
ocr:
  enabled: true
  tessdata-path: /path/to/tessdata
```

`ocr.tessdata-path` is optional. If it is left blank, the application will try to auto-detect common Tesseract locations on macOS, Linux, and Windows.

You can also set the environment variable equivalent:

```bash
OCR_TESSDATA_PATH=/path/to/tessdata
```

## Prerequisites

- Tesseract OCR engine installed on the server
- Tessdata language files available at the configured path or a standard platform path
- Supported file formats: PNG, JPEG, PDF
- Maximum file size: 10 MB

Common detected paths include:

- macOS Homebrew: `/opt/homebrew/share/tessdata`
- Linux packages: `/usr/share/tesseract-ocr/5/tessdata`, `/usr/share/tessdata`
- Windows installer: `C:\Program Files\Tesseract-OCR\tessdata`

## Workflow

```
1. Upload image ──▶ 2. Receive jobId ──▶ 3. Poll job status ──▶ 4. Review results
```

### Step 1: Upload

`POST /api/v1/imports/ocr`

```bash
curl -X POST "http://localhost:8080/api/v1/imports/ocr" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@receipt.jpg"
```

Returns HTTP `202 Accepted` with a `jobId`.

### Step 2: Poll Status

`GET /api/v1/imports/ocr/{jobId}`

Returns the job status and extracted data when complete:

| Status | Meaning |
|--------|---------|
| `PENDING` | Job queued, not yet processed |
| `PROCESSING` | OCR is running |
| `COMPLETED` | Text extracted, results available |
| `FAILED` | Processing error |

### Step 3: Review Results

When the job completes, the response includes extracted text and parsed candidates (amounts, dates, merchants).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/imports/ocr` | Upload a document for OCR |
| GET | `/api/v1/imports/ocr/{jobId}` | Get job status and results |

## Notes

- OCR is CPU-intensive; processing is async with retries
- Default timeout: 30 seconds per job
- Maximum retries: 2
- When disabled, endpoints return `503 OCR_DISABLED`
