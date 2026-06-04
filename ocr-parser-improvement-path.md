## OCR Parser Improvement Path

### Goal

Make the OCR feature more general so it can parse not only simple retail receipts, but also bank receipts, tax receipts, invoices, and other structured documents.

### Current Limitation

The current parser is too narrow:

- it requires a `date` and an `amount` on the same line
- it only supports simple amount formats such as `15.50` or `42,10`
- it does not understand tables, labels, totals, or multi-line receipts
- it does not distinguish final total from unrelated numeric values

Current implementation:

- `src/main/java/com/saveapenny/ocr/application/analysis/OcrAnalysisService.java`

### Recommended Direction

Replace the current line-based parser with a small parsing pipeline:

1. OCR text extraction
2. Text normalization
3. Document analysis
4. Field extraction
5. Candidate scoring and selection

Instead of:

- OCR text -> regex per line -> candidate

Use:

- OCR text -> normalized lines/blocks -> detect document structure -> extract fields -> build scored candidates

## Phase 1: Improve the Generic Parser

Status: completed

### 1. Add a normalization layer

Create a service that prepares OCR text before parsing.

Suggested class:

- `OcrTextNormalizationService`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/extractor/OcrTextNormalizationService.java`

Responsibilities:

- normalize whitespace
- normalize common OCR mistakes in numeric contexts
- normalize amount formats
- normalize date formats for matching
- preserve original text for audit/debug

Examples:

- `1.250,00-` -> `-1250.00`
- `1 250,00` -> `1250.00`
- `1,250.00` -> `1250.00`

Common OCR corrections in numeric contexts:

- `O` -> `0`
- `I` -> `1`
- `S` -> `5`

### 2. Replace regex-only amount parsing

Create a dedicated money parser.

Suggested class:

- `MoneyParser`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/extractor/MoneyParser.java`

Supported formats should include:

- `15.50`
- `42,10`
- `1.250,00`
- `1.250,00-`
- `-1.250,00`
- `1,250.00`
- `1 250,00`

This is the biggest immediate improvement for general OCR parsing.

### 3. Replace simple date matching with a reusable parser

Suggested class:

- `DateParser`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/extractor/DateParser.java`

Support:

- `YYYY-MM-DD`
- `DD/MM/YYYY`
- `DD-MM-YYYY`

Also distinguish between:

- transaction date
- issue date
- period date

Example:

- `01/2026-01/2026` should be treated as a period, not a transaction date.

### 4. Stop requiring date and amount on the same line

The parser should be able to combine nearby lines.

Example:

- line 1 contains payment date
- line 2 contains description and amount

This is necessary for bank receipts, tax receipts, and table-like PDFs.

Implemented in:

- `src/main/java/com/saveapenny/ocr/application/analysis/OcrAnalysisService.java`

### 5. Improve failure messaging

If OCR text exists but no transaction is extracted, do not report the document as empty.

Use clearer messages such as:

- `OCR text extracted, but no transaction could be confidently parsed`
- `Amount candidates found, but no transaction date matched`
- `Detected structured receipt, but parser confidence was too low`

Note:

- Parser behavior was expanded in Phase 1, but user-facing error messaging is only partially addressed. Full diagnostics are still part of later phases.

## Phase 2: Parse Document Structure

Status: completed

### 6. Parse blocks instead of isolated lines

Group OCR text into logical sections.

Suggested models:

- `OcrDocumentBlock`
- `OcrParsedField`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/analysis/OcrDocumentBlock.java`
- `src/main/java/com/saveapenny/ocr/application/analysis/OcrDocumentBlockType.java`
- `src/main/java/com/saveapenny/ocr/application/analysis/OcrParsedField.java`
- `src/main/java/com/saveapenny/ocr/application/analysis/OcrParsedFieldType.java`

Typical blocks:

- header block
- merchant or institution block
- detail block
- totals block
- footer block

Useful heuristics:

- neighboring non-empty lines usually belong together
- labels such as `TOPLAM`, `TOTAL`, `GENEL TOPLAM`, `Tarih`, `Odeme Tarihi` indicate structure
- repeated numeric columns often indicate a table

### 7. Extract fields independently

Do not build a transaction directly from a single line. First extract fields independently.

Suggested fields:

- merchant or institution name
- transaction date
- issue date
- amount
- currency
- description
- reference number
- document type

Suggested output model:

- `OcrDocumentAnalysis`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/analysis/OcrDocumentAnalysis.java`
- `src/main/java/com/saveapenny/ocr/application/analysis/OcrAnalysisService.java#analyze`

Possible contents:

- `documentType`
- `merchantName`
- `dates`
- `amounts`
- `references`
- `labels`
- `transactionCandidates`

Current Phase 2 result:

- OCR text is grouped into normalized blocks
- blocks are classified as `HEADER`, `DETAIL`, `TOTALS`, `FOOTER`, or `OTHER`
- dates, amounts, references, labels, and merchant name are extracted independently
- OCR status responses now expose `documentType`, `currency`, `merchantName`, `paymentDate`, `issueDate`, `extractedDates`, `extractedAmounts`, `referenceNumbers`, and `labels`
- candidate building uses block analysis instead of only isolated line parsing

## Phase 3: Add Document-Type Strategies

Status: completed for initial strategy rollout

### 8. Introduce parser strategies by document family

Suggested interface:

- `OcrDocumentParser`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/parser/OcrDocumentParser.java`

Suggested implementations:

- `GenericDocumentParser`
- `RetailReceiptParser`
- `BankReceiptParser`
- `TaxReceiptParser`
- `InvoiceParser`

Implemented now:

- `src/main/java/com/saveapenny/ocr/application/parser/GenericDocumentParser.java`
- `src/main/java/com/saveapenny/ocr/application/parser/strategy/BankReceiptParser.java`
- `src/main/java/com/saveapenny/ocr/application/parser/strategy/TaxReceiptParser.java`
- `src/main/java/com/saveapenny/ocr/application/parser/strategy/RetailReceiptParser.java`
- `src/main/java/com/saveapenny/ocr/application/parser/strategy/InvoiceParser.java`

Flow:

1. detect likely document type
2. run the matching parser strategy
3. fall back to `GenericDocumentParser` if confidence is low

### 9. Add a document classifier

Suggested class:

- `OcrDocumentClassifier`

Implemented:

- `src/main/java/com/saveapenny/ocr/application/analysis/classifier/OcrDocumentClassifier.java`

Example keyword signals:

- bank: `banka`, `subesi`, `kredi kart`, `iban`, `ref no`
- tax: `vergi`, `tahsil`, `mukellef`, `toplam`
- retail: `market`, `fis`, `kdv`, `kasiyer`, `toplam`

The classifier does not need to be perfect. It only needs to route obvious cases and fall back safely.

Current Phase 3 result:

- OCR analysis now classifies documents before final candidate selection
- specialized parser strategies run for `BANK_RECEIPT` and `TAX_RECEIPT`
- specialized parser strategies also run for `RETAIL_RECEIPT` and `INVOICE`
- `GENERIC_DOCUMENT` fallback remains in place through `GenericDocumentParser`
- strategy selection happens inside `OcrAnalysisService`

## Phase 4: Candidate Scoring

Status: completed

### 10. Score candidates instead of hard-accepting or rejecting

Each candidate should receive a confidence score.

Useful scoring signals:

- amount near `TOPLAM` or `TOTAL` -> high confidence
- date near `Tarih` or `Odeme Tarihi` -> high confidence
- merchant name detected in header -> high confidence
- amount inside card mask or reference line -> low confidence
- parsed total in totals block -> high confidence

This prevents valid OCR documents from being treated as empty just because one heuristic failed.

Implemented:

- `src/main/java/com/saveapenny/ocr/application/analysis/OcrScoredCandidate.java`
- strategy-specific scoring in:
  - `src/main/java/com/saveapenny/ocr/application/parser/strategy/BankReceiptParser.java`
  - `src/main/java/com/saveapenny/ocr/application/parser/strategy/TaxReceiptParser.java`
  - `src/main/java/com/saveapenny/ocr/application/parser/strategy/RetailReceiptParser.java`
  - `src/main/java/com/saveapenny/ocr/application/parser/strategy/InvoiceParser.java`
- generic fallback scoring in:
  - `src/main/java/com/saveapenny/ocr/application/analysis/OcrAnalysisService.java`

Current Phase 4 result:

- parser strategies now return scored candidates internally
- OCR status responses expose overall `parseConfidence`
- specialized document parsers return higher confidence when they match strong signals
- generic fallback candidates receive lower confidence than specialized parses

### 11. Choose the best amount, not the first amount

Structured documents often include many numeric values:

- subtotal
- tax
- fee
- installment
- total

Selection rules should prefer:

- values labeled `TOPLAM`, `TOTAL`, `GENEL TOPLAM`
- values found in totals block
- final bottom-most total when structure is table-like

Implemented result:

- bank, tax, retail, and invoice strategies prefer totals lines before fallback amounts
- generic parsing still falls back safely when no specialized rule applies

## API Improvement

### 12. Return diagnostics together with candidates

The OCR status response should eventually include parse diagnostics.

Status: completed

Recommended additions:

- detected document type
- confidence score
- parse warnings
- extracted key fields
- reason no final candidate was selected

Examples:

- `Found OCR text but no valid transaction date`
- `Detected bank receipt format`
- `Multiple amount candidates found; selected amount near TOPLAM`

Implemented:

- `src/main/java/com/saveapenny/ocr/interfaces/http/dto/OcrParseDiagnosticsResponse.java`
- `src/main/java/com/saveapenny/ocr/application/service/OcrJobServiceImpl.java`
- `src/main/java/com/saveapenny/ocr/interfaces/http/dto/OcrJobStatusResponse.java`

Current API improvement result:

- OCR status responses now include nested `parseDiagnostics`
- diagnostics expose:
  - detected document type
  - confidence score
  - warnings
  - notes
  - selected candidate reason
  - no-candidate reason
- top-level response fields still expose extracted key fields such as dates, amounts, references, merchant name, currency, and parse confidence

## Suggested Class Layout

- `OcrTextNormalizationService`
- `MoneyParser`
- `DateParser`
- `OcrDocumentClassifier`
- `OcrDocumentParser`
- `GenericDocumentParser`
- `RetailReceiptParser`
- `BankReceiptParser`
- `TaxReceiptParser`
- `InvoiceParser`

## Recommended Implementation Order

### Step 1

Keep `TesseractOcrService` as-is for now.

### Step 2

Refactor `OcrAnalysisService` into a pipeline-oriented service.

### Step 3

Implement first:

- robust amount parsing
- robust date parsing
- multi-line pairing of nearby fields
- better error and warning messages

Status:

- robust amount parsing: completed
- robust date parsing: completed
- multi-line pairing of nearby fields: completed
- better error and warning messages: partially completed

### Step 4

Add `BankReceiptParser` and `TaxReceiptParser` because these are more structured than simple store receipts and expose the current parser weaknesses clearly.

### Step 5

Add confidence scoring and diagnostics to avoid misleading `document is empty` style errors.

## Best Immediate Win

For currently failing structured receipts, the highest-value short-term improvement is:

1. parse amounts like `1.250,00-`
2. detect labels such as `TOPLAM`
3. pair the selected amount with the nearest valid payment date
4. produce at least one candidate with a confidence score and description

Phase 1 result:

- the parser now supports structured adjacent-line extraction and amount formats such as `1.250,00-`
- targeted OCR parser and import flow tests pass for this behavior

## Summary

The OCR engine is already extracting text. The main improvement needed is not better OCR itself, but a more general parser architecture that:

- normalizes OCR output
- understands structured documents
- extracts fields independently
- scores candidates instead of rejecting aggressively
- supports multiple document types with fallback behavior

This path will make the OCR feature much more reliable for real-world receipts and financial documents.
