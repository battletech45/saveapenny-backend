package com.saveapenny.ocr.support.monitoring;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class OcrMetrics {

    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();

    public void markSuccess() {
        successCount.incrementAndGet();
    }

    public void markFailure() {
        failureCount.incrementAndGet();
    }

    public long successCount() {
        return successCount.get();
    }

    public long failureCount() {
        return failureCount.get();
    }
}
