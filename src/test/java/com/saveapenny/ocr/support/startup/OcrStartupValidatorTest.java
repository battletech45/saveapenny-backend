package com.saveapenny.ocr.support.startup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.support.runtime.OcrRuntimeChecker;
import com.saveapenny.ocr.support.runtime.OcrRuntimeStatus;
import org.junit.jupiter.api.Test;

class OcrStartupValidatorTest {

    @Test
    void run_doesNothingWhenOcrDisabled() {
        OcrStartupValidator validator = new OcrStartupValidator(
                new OcrProperties(false, "/tmp", "eng", 3, 1024, 1000, 1, false),
                mock(OcrRuntimeChecker.class));

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void run_failsWhenRuntimeNotReady() {
        OcrRuntimeChecker checker = mock(OcrRuntimeChecker.class);
        when(checker.check()).thenReturn(new OcrRuntimeStatus(true, true, false, "eng", "/tmp", "native load failed"));

        OcrStartupValidator validator = new OcrStartupValidator(
                new OcrProperties(true, "/tmp", "eng", 3, 1024, 1000, 1, false),
                checker);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }
}
