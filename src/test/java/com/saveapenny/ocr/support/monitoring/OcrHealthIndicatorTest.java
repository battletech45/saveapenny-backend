package com.saveapenny.ocr.support.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.support.runtime.OcrRuntimeChecker;
import com.saveapenny.ocr.support.runtime.OcrRuntimeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class OcrHealthIndicatorTest {

    @Test
    void health_reportsDisabledState() {
        OcrHealthIndicator indicator = new OcrHealthIndicator(
                new OcrProperties(false, "/tmp", "eng", 3, 1024, 1000, 1, false),
                new OcrMetrics(),
                mock(OcrRuntimeChecker.class));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(false, health.getDetails().get("enabled"));
        assertEquals("eng", health.getDetails().get("language"));
    }

    @Test
    void health_reportsReadinessDetailsWhenEnabled() {
        OcrRuntimeChecker checker = mock(OcrRuntimeChecker.class);
        when(checker.check()).thenReturn(new OcrRuntimeStatus(true, true, false, "tur", "/opt/homebrew/share/tessdata", "native load failed"));

        OcrMetrics metrics = new OcrMetrics();
        metrics.markSuccess();
        metrics.markFailure();

        OcrHealthIndicator indicator = new OcrHealthIndicator(
                new OcrProperties(true, "/opt/homebrew/share/tessdata", "tur", 3, 1024, 1000, 1, false),
                metrics,
                checker);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(true, health.getDetails().get("enabled"));
        assertEquals(true, health.getDetails().get("tessdataPathValid"));
        assertEquals(false, health.getDetails().get("nativeLibraryLoaded"));
        assertEquals("tur", health.getDetails().get("language"));
        assertEquals("native load failed", health.getDetails().get("message"));
        assertTrue(((Number) health.getDetails().get("successCount")).longValue() >= 1L);
        assertTrue(((Number) health.getDetails().get("failureCount")).longValue() >= 1L);
    }
}
