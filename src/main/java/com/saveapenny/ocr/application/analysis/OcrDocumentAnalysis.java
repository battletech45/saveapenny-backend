package com.saveapenny.ocr.application.analysis;

import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OcrDocumentAnalysis(
        List<OcrDocumentBlock> blocks,
        List<OcrParsedField> fields,
        String documentType,
        String currency,
        String merchantName,
        LocalDate paymentDate,
        LocalDate issueDate,
        List<LocalDate> dates,
        List<BigDecimal> amounts,
        List<String> referenceNumbers,
        List<String> labels,
        double parseConfidence,
        List<OcrTransactionCandidate> transactionCandidates) {
}
