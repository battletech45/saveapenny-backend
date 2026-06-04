package com.saveapenny.ocr.application.port.in;

import java.util.concurrent.CompletableFuture;

public interface OcrService {

    String extractText(OcrUploadPayload file);

    CompletableFuture<String> extractTextAsync(OcrUploadPayload file);
}
