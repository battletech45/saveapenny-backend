package com.saveapenny.ocr.application.port.in;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    String extractText(MultipartFile file);

    CompletableFuture<String> extractTextAsync(MultipartFile file);
}
