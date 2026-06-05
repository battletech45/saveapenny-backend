package com.saveapenny.ocr.application.analysis;

import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OcrDocumentAnalysisSeed(
        String rawText,
        String documentType,
        String currency,
        String merchantName,
        LocalDate paymentDate,
        LocalDate issueDate,
        List<OcrDocumentBlock> blocks,
        List<OcrParsedField> fields,
        List<LocalDate> dates,
        List<BigDecimal> amounts,
        List<String> referenceNumbers,
        List<String> labels,
        List<OcrScoredCandidate> genericCandidates) {
}
