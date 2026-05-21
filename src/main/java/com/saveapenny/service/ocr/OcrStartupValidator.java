package com.saveapenny.service.ocr;

import com.saveapenny.config.OcrProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OcrStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OcrStartupValidator.class);

    private final OcrProperties ocrProperties;

    public OcrStartupValidator(OcrProperties ocrProperties) {
        this.ocrProperties = ocrProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ocrProperties.enabled()) {
            return;
        }

        Path tessdataPath = Path.of(ocrProperties.tessdataPath());
        if (!Files.isDirectory(tessdataPath)) {
            log.warn("OCR is enabled but tessdata path is missing: {}", tessdataPath);
        } else {
            log.info("OCR startup validation passed. tessdataPath={}", tessdataPath);
        }
    }
}
