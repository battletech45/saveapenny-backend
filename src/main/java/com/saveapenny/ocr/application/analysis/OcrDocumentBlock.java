package com.saveapenny.ocr.application.analysis;

import java.util.List;

public record OcrDocumentBlock(
        int index,
        OcrDocumentBlockType type,
        List<String> lines) {
}
