package com.saveapenny.ocr.application.analysis;

public record OcrParsedField(
        OcrParsedFieldType type,
        String value,
        String sourceLine,
        int blockIndex,
        int lineIndex) {
}
