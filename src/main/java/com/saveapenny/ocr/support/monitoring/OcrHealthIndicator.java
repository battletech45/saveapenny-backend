package com.saveapenny.ocr.support.monitoring;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.support.runtime.OcrRuntimeChecker;
import com.saveapenny.ocr.support.runtime.OcrRuntimeStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OcrHealthIndicator implements HealthIndicator {

    private final OcrProperties ocrProperties;
    private final OcrMetrics ocrMetrics;
    private final OcrRuntimeChecker ocrRuntimeChecker;

    public OcrHealthIndicator(OcrProperties ocrProperties, OcrMetrics ocrMetrics, OcrRuntimeChecker ocrRuntimeChecker) {
        this.ocrProperties = ocrProperties;
        this.ocrMetrics = ocrMetrics;
        this.ocrRuntimeChecker = ocrRuntimeChecker;
    }

    @Override
    public Health health() {
        if (!ocrProperties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("language", ocrProperties.language())
                    .build();
        }

        OcrRuntimeStatus status = ocrRuntimeChecker.check();
        Health.Builder builder = status.ready() ? Health.up() : Health.down();
        builder.withDetail("enabled", status.enabled())
                .withDetail("tessdataPathValid", status.tessdataPathValid())
                .withDetail("nativeLibraryLoaded", status.nativeLibraryLoaded())
                .withDetail("language", status.language())
                .withDetail("tessdataPath", status.tessdataPath())
                .withDetail("successCount", ocrMetrics.successCount())
                .withDetail("failureCount", ocrMetrics.failureCount());
        if (status.message() != null && !status.message().isBlank()) {
            builder.withDetail("message", status.message());
        }
        return builder.build();
    }
}
