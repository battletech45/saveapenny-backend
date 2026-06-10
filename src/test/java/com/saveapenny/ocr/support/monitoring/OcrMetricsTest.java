package com.saveapenny.ocr.support.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcrMetricsTest {

    private OcrMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new OcrMetrics();
    }

    @Test
    void initialCountsAreZero() {
        assertEquals(0, metrics.successCount());
        assertEquals(0, metrics.failureCount());
    }

    @Test
    void markSuccess_incrementsSuccessCount() {
        metrics.markSuccess();
        assertEquals(1, metrics.successCount());
        assertEquals(0, metrics.failureCount());
    }

    @Test
    void markFailure_incrementsFailureCount() {
        metrics.markFailure();
        assertEquals(0, metrics.successCount());
        assertEquals(1, metrics.failureCount());
    }

    @Test
    void multipleMarks_accumulate() {
        metrics.markSuccess();
        metrics.markSuccess();
        metrics.markSuccess();
        metrics.markFailure();

        assertEquals(3, metrics.successCount());
        assertEquals(1, metrics.failureCount());
    }
}
