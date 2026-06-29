package com.saveapenny.ocr.support.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class OcrMetrics {

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer processingTimer;

    public OcrMetrics(MeterRegistry meterRegistry) {
        this.successCounter = meterRegistry.counter("ocr.job.results", "result", "success");
        this.failureCounter = meterRegistry.counter("ocr.job.results", "result", "failure");
        this.processingTimer = meterRegistry.timer("ocr.job.duration");
    }

    public void markSuccess() {
        successCounter.increment();
    }

    public void markFailure() {
        failureCounter.increment();
    }

    public void recordDuration(long nanos) {
        processingTimer.record(nanos, TimeUnit.NANOSECONDS);
    }

    public long successCount() {
        return (long) successCounter.count();
    }

    public long failureCount() {
        return (long) failureCounter.count();
    }

    public long completedCount() {
        return successCount() + failureCount();
    }
}
