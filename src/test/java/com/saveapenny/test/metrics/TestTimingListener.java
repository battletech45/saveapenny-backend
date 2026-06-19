package com.saveapenny.test.metrics;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestTimingListener implements TestExecutionListener {

    private static final Path METRICS_DIR = Paths.get("target/test-metrics");
    private static final Path TIMING_FILE = METRICS_DIR.resolve("class-timing.csv");

    private final Map<String, Instant> classStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Duration> classDurations = new ConcurrentHashMap<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isContainer() && testIdentifier.getParentId().isPresent()) {
            classStartTimes.put(testIdentifier.getUniqueId(), Instant.now());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isContainer() && testIdentifier.getParentId().isPresent()) {
            Instant start = classStartTimes.remove(testIdentifier.getUniqueId());
            if (start != null) {
                classDurations.put(testIdentifier.getDisplayName(), Duration.between(start, Instant.now()));
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            Files.createDirectories(METRICS_DIR);
            writeTimingCsv();
        } catch (IOException e) {
            System.err.println("[TestTimingListener] Failed to write metrics: " + e.getMessage());
        }
    }

    private void writeTimingCsv() throws IOException {
        Files.writeString(TIMING_FILE, "class,duration_ms\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        for (var entry : classDurations.entrySet()) {
            String line = entry.getKey() + "," + entry.getValue().toMillis() + "\n";
            Files.writeString(TIMING_FILE, line, StandardOpenOption.APPEND);
        }
        System.out.println("[TestTimingListener] Wrote timing metrics for " + classDurations.size()
                + " classes to " + TIMING_FILE);
    }
}
