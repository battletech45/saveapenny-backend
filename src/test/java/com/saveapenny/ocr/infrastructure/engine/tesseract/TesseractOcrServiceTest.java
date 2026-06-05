package com.saveapenny.ocr.infrastructure.engine.tesseract;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.application.port.in.OcrUploadPayload;
import com.saveapenny.ocr.domain.exception.OcrProcessingException;
import com.saveapenny.ocr.infrastructure.engine.tesseract.TesseractOcrService;
import com.saveapenny.ocr.infrastructure.preprocessing.ImagePreprocessingService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

class TesseractOcrServiceTest {

    @Test
    void extractText_throwsWhenOcrDisabled() {
        TesseractOcrService service = new TesseractOcrService(
                new OcrProperties(false, "/tmp", "eng", 3, 1024, 1000, 1, false),
                new ImagePreprocessingService(),
                new SyncTaskExecutor());

        OcrUploadPayload file = new OcrUploadPayload("r.png", "image/png", "a".getBytes());
        OcrProcessingException ex = assertThrows(OcrProcessingException.class, () -> service.extractText(file));
        assertTrue(ex.getMessage().contains("disabled"));
    }

    @Test
    void extractText_throwsWhenFileEmpty() {
        TesseractOcrService service = new TesseractOcrService(
                new OcrProperties(true, "/tmp", "eng", 3, 1024, 1000, 1, false),
                new ImagePreprocessingService(),
                new SyncTaskExecutor());

        OcrUploadPayload file = new OcrUploadPayload("r.png", "image/png", new byte[0]);
        OcrProcessingException ex = assertThrows(OcrProcessingException.class, () -> service.extractText(file));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void extractText_mapsInvalidPdfBytesToOcrProcessingException() {
        TesseractOcrService service = new TesseractOcrService(
                new OcrProperties(true, "/tmp", "eng", 3, 1024, 1000, 1, false),
                new ImagePreprocessingService(),
                new SyncTaskExecutor());

        OcrUploadPayload file = new OcrUploadPayload("r.pdf", "application/pdf", "not-a-pdf".getBytes());

        OcrProcessingException ex = assertThrows(OcrProcessingException.class, () -> service.extractText(file));
        assertTrue(ex.getMessage().contains("Unable to read"));
    }

    @Test
    void extractText_throwsForUnsupportedBinaryContent() {
        TesseractOcrService service = new TesseractOcrService(
                new OcrProperties(true, "/tmp", "eng", 3, 1024, 1000, 1, false),
                new ImagePreprocessingService(),
                new SyncTaskExecutor());

        OcrUploadPayload file = new OcrUploadPayload("r.png", "image/png", "not-image".getBytes());
        OcrProcessingException ex = assertThrows(OcrProcessingException.class, () -> service.extractText(file));
        assertTrue(ex.getMessage().contains("Unsupported file content"));
    }

    @Test
    void extractTextAsync_runsForSmallFileImmediately() {
        TesseractOcrService service = new TesseractOcrService(
                new OcrProperties(true, "/tmp", "eng", 3, 1024, 1000, 1, false),
                new ImagePreprocessingService(),
                new SyncTaskExecutor());

        OcrUploadPayload file = new OcrUploadPayload("r.png", "image/png", "x".getBytes());
        assertThrows(OcrProcessingException.class, () -> service.extractTextAsync(file).join());
    }
}
