package com.saveapenny.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminMetricsControllerTest {

    @Test
    void returnsAggregatedMetricsSummary() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.counter("stock.provider.requests", "provider", "alphavantage", "operation", "quote").increment(2);
        meterRegistry.counter("stock.provider.failures", "provider", "alphavantage", "operation", "quote",
                "result", "failure", "error_type", "provider_error").increment();
        meterRegistry.counter("stock.rate_limit.rejections", "provider", "alphavantage", "operation", "quote",
                "result", "rate_limited").increment();
        meterRegistry.timer("stock.provider.duration", "provider", "alphavantage", "operation", "quote")
                .record(Duration.ofMillis(250));
        meterRegistry.counter("ocr.job.results", "result", "success").increment(3);
        meterRegistry.counter("ocr.job.results", "result", "failure").increment();
        meterRegistry.timer("ocr.job.duration").record(Duration.ofSeconds(8));
        meterRegistry.counter("goal.progress.job.runs", "outcome", "on_track").increment(2);
        meterRegistry.counter("goal.progress.job.runs", "outcome", "error").increment();
        meterRegistry.counter("goal.progress.job.failures").increment();
        meterRegistry.timer("goal.progress.job.duration").record(Duration.ofMillis(900));

        AdminMetricsController controller = new AdminMetricsController(meterRegistry);

        Map<String, Object> result = controller.getAllMetrics();
        Map<String, Object> stock = cast(result.get("stock"));
        Map<String, Object> ocr = cast(result.get("ocr"));
        Map<String, Object> goalProgress = cast(result.get("goalProgress"));

        assertEquals(2.0, stock.get("requests"));
        assertEquals(1.0, stock.get("failures"));
        assertEquals(1.0, stock.get("rate_limit_rejections"));
        assertEquals(250.0, stock.get("average_duration_ms"));

        assertEquals(4.0, ocr.get("jobs_completed"));
        assertEquals(3.0, ocr.get("successes"));
        assertEquals(1.0, ocr.get("failures"));
        assertEquals(8000.0, ocr.get("average_duration_ms"));

        assertEquals(3.0, goalProgress.get("runs"));
        assertEquals(1.0, goalProgress.get("failures"));
        assertEquals(2.0, goalProgress.get("on_track"));
        assertEquals(1.0, goalProgress.get("errors"));
        assertEquals(900.0, goalProgress.get("average_duration_ms"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }
}
