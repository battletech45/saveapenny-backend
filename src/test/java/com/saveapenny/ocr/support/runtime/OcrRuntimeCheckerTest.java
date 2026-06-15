package com.saveapenny.ocr.support.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.saveapenny.config.OcrProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OcrRuntimeCheckerTest {

    @TempDir
    Path tempDir;

    @Mock
    private OcrProperties ocrProperties;

    private OcrRuntimeChecker checker;

    @BeforeEach
    void setUp() {
        checker = new OcrRuntimeChecker(ocrProperties);
    }

    @Test
    void check_whenDisabled_returnsReadyWithMessage() {
        when(ocrProperties.enabled()).thenReturn(false);
        when(ocrProperties.language()).thenReturn("eng");
        when(ocrProperties.tessdataPath()).thenReturn("/usr/share/tessdata");

        OcrRuntimeStatus status = checker.check();

        assertFalse(status.enabled());
        assertTrue(status.ready());
        assertEquals("OCR is disabled", status.message());
    }

    @Test
    void check_whenEnabledAndTessdataInvalid_reportsPathNotFound() {
        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.tessdataPath()).thenReturn("/nonexistent/path");
        when(ocrProperties.language()).thenReturn("eng");

        OcrRuntimeStatus status = checker.check();

        assertTrue(status.enabled());
        assertFalse(status.tessdataPathValid());
        assertNotNull(status.message());
        assertTrue(status.message().contains("tessdata path not found"));
        assertFalse(status.ready());
    }

    @Test
    void check_whenEnabledAndTessdataValid_reportsStatusFields() throws IOException {
        Path tessdata = tempDir.resolve("tessdata");
        Files.createDirectories(tessdata);
        Files.writeString(tessdata.resolve("eng.traineddata"), "fixture");

        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.tessdataPath()).thenReturn(tessdata.toString());
        when(ocrProperties.language()).thenReturn("eng");

        OcrRuntimeStatus status = checker.check();

        assertTrue(status.enabled());
        assertTrue(status.tessdataPathValid());
        assertEquals(tessdata.toString(), status.tessdataPath());
        assertEquals("eng", status.language());
    }

    @Test
    void check_whenConfiguredPathPointsToParentDirectory_resolvesTessdataChild() throws IOException {
        Path installDir = tempDir.resolve("tesseract-install");
        Path tessdata = installDir.resolve("tessdata");
        Files.createDirectories(tessdata);
        Files.writeString(tessdata.resolve("eng.traineddata"), "fixture");

        when(ocrProperties.enabled()).thenReturn(true);
        when(ocrProperties.tessdataPath()).thenReturn(installDir.toString());
        when(ocrProperties.language()).thenReturn("eng");

        OcrRuntimeStatus status = checker.check();

        assertTrue(status.tessdataPathValid());
        assertEquals(tessdata.toString(), status.tessdataPath());
    }
}
