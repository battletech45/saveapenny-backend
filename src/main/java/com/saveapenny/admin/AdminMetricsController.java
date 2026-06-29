package com.saveapenny.admin;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/metrics")
@Tag(name = "Admin", description = "Administrative observability and metrics endpoints.")
public class AdminMetricsController {

    private final MeterRegistry meterRegistry;

    public AdminMetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping
    @Operation(summary = "Get aggregated application metrics")
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> result = new HashMap<>();

        result.put("stock", stockMetrics());
        result.put("ocr", ocrMetrics());
        result.put("goalProgress", goalProgressMetrics());
        return result;
    }

    private Map<String, Object> stockMetrics() {
        Map<String, Object> stock = new HashMap<>();
        stock.put("requests", sumMetric("stock.provider.requests"));
        stock.put("failures", sumMetric("stock.provider.failures"));
        stock.put("rate_limit_rejections", sumMetric("stock.rate_limit.rejections"));
        stock.put("average_duration_ms", averageDurationMillis("stock.provider.duration"));
        return stock;
    }

    private Map<String, Object> ocrMetrics() {
        Map<String, Object> ocr = new HashMap<>();
        double successes = sumMetric("ocr.job.results", "result", "success");
        double failures = sumMetric("ocr.job.results", "result", "failure");
        ocr.put("jobs_completed", successes + failures);
        ocr.put("successes", successes);
        ocr.put("failures", failures);
        ocr.put("average_duration_ms", averageDurationMillis("ocr.job.duration"));
        return ocr;
    }

    private Map<String, Object> goalProgressMetrics() {
        Map<String, Object> goalProgress = new HashMap<>();
        goalProgress.put("runs", sumMetric("goal.progress.job.runs"));
        goalProgress.put("failures", sumMetric("goal.progress.job.failures"));
        goalProgress.put("on_track", sumMetric("goal.progress.job.runs", "outcome", "on_track"));
        goalProgress.put("at_risk", sumMetric("goal.progress.job.runs", "outcome", "at_risk"));
        goalProgress.put("off_track", sumMetric("goal.progress.job.runs", "outcome", "off_track"));
        goalProgress.put("achieved", sumMetric("goal.progress.job.runs", "outcome", "achieved"));
        goalProgress.put("no_projection", sumMetric("goal.progress.job.runs", "outcome", "no_projection"));
        goalProgress.put("errors", sumMetric("goal.progress.job.runs", "outcome", "error"));
        goalProgress.put("average_duration_ms", averageDurationMillis("goal.progress.job.duration"));
        return goalProgress;
    }

    private double sumMetric(String metricName, String... tags) {
        return meterRegistry.find(metricName).tags(tags).meters().stream()
                .flatMap(meter -> StreamSupport.stream(meter.measure().spliterator(), false))
                .filter(measurement -> measurement.getStatistic() == Statistic.COUNT)
                .mapToDouble(Measurement::getValue)
                .sum();
    }

    private double averageDurationMillis(String metricName) {
        double count = 0;
        double totalTimeSeconds = 0;

        for (var meter : meterRegistry.find(metricName).meters()) {
            for (Measurement measurement : meter.measure()) {
                if (measurement.getStatistic() == Statistic.COUNT) {
                    count += measurement.getValue();
                }
                if (measurement.getStatistic() == Statistic.TOTAL_TIME) {
                    totalTimeSeconds += measurement.getValue();
                }
            }
        }

        if (count == 0) {
            return 0;
        }

        return (totalTimeSeconds * 1000) / count;
    }
}
