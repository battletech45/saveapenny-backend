package com.saveapenny.service.ocr;

import com.saveapenny.config.OcrProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OcrHealthIndicator implements HealthIndicator {

    private final OcrProperties ocrProperties;
    private final OcrMetrics ocrMetrics;

    public OcrHealthIndicator(OcrProperties ocrProperties, OcrMetrics ocrMetrics) {
        this.ocrProperties = ocrProperties;
        this.ocrMetrics = ocrMetrics;
    }

    @Override
    public Health health() {
        if (!ocrProperties.enabled()) {
            return Health.up().withDetail("ocr", "disabled").build();
        }

        boolean tessdataExists = Files.isDirectory(Path.of(ocrProperties.tessdataPath()));
        if (!tessdataExists) {
            return Health.down()
                    .withDetail("ocr", "enabled")
                    .withDetail("reason", "tessdata path not found")
                    .build();
        }

        return Health.up()
                .withDetail("ocr", "enabled")
                .withDetail("successCount", ocrMetrics.successCount())
                .withDetail("failureCount", ocrMetrics.failureCount())
                .build();
    }
}
