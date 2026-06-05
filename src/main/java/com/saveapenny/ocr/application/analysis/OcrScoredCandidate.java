package com.saveapenny.ocr.application.analysis;

import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;

public record OcrScoredCandidate(
        OcrTransactionCandidate candidate,
        double confidence) {
}
