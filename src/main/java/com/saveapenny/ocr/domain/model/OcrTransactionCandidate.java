package com.saveapenny.ocr.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OcrTransactionCandidate(
        LocalDate date,
        BigDecimal amount,
        String description,
        String categoryHint) {
}
