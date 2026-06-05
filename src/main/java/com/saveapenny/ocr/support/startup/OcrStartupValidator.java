package com.saveapenny.ocr.support.startup;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.support.runtime.OcrRuntimeChecker;
import com.saveapenny.ocr.support.runtime.OcrRuntimeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OcrStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OcrStartupValidator.class);

    private final OcrProperties ocrProperties;
    private final OcrRuntimeChecker ocrRuntimeChecker;

    public OcrStartupValidator(OcrProperties ocrProperties, OcrRuntimeChecker ocrRuntimeChecker) {
        this.ocrProperties = ocrProperties;
        this.ocrRuntimeChecker = ocrRuntimeChecker;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ocrProperties.enabled()) {
            return;
        }

        OcrRuntimeStatus status = ocrRuntimeChecker.check();
        if (!status.ready()) {
            log.error(
                    "OCR startup validation failed. tessdataPath={}, language={}, tessdataPathValid={}, nativeLibraryLoaded={}, message={}",
                    status.tessdataPath(),
                    status.language(),
                    status.tessdataPathValid(),
                    status.nativeLibraryLoaded(),
                    status.message());
            throw new IllegalStateException("OCR runtime validation failed: " + status.message());
        }
        log.info(
                "OCR startup validation passed. tessdataPath={}, language={}, nativeLibraryLoaded={}",
                status.tessdataPath(),
                status.language(),
                status.nativeLibraryLoaded());
    }
}
