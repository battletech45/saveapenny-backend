package com.saveapenny.ocr.support.runtime;

import com.saveapenny.config.OcrProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.tess4j.TessAPI;
import org.springframework.stereotype.Component;

@Component
public class OcrRuntimeChecker {

    private final OcrProperties ocrProperties;

    public OcrRuntimeChecker(OcrProperties ocrProperties) {
        this.ocrProperties = ocrProperties;
    }

    public OcrRuntimeStatus check() {
        if (!ocrProperties.enabled()) {
            return new OcrRuntimeStatus(false, false, false, ocrProperties.language(), ocrProperties.tessdataPath(), "OCR is disabled");
        }

        String tessdataPath = ocrProperties.tessdataPath();
        boolean tessdataPathValid = tessdataPath != null && Files.isDirectory(Path.of(tessdataPath));
        boolean nativeLibraryLoaded = canLoadNativeTesseract();

        String message = null;
        if (!tessdataPathValid) {
            message = "tessdata path not found: " + tessdataPath;
        } else if (!nativeLibraryLoaded) {
            message = "native tesseract library could not be loaded; set -Djna.library.path and enable native access";
        }

        return new OcrRuntimeStatus(
                true,
                tessdataPathValid,
                nativeLibraryLoaded,
                ocrProperties.language(),
                tessdataPath,
                message);
    }

    private boolean canLoadNativeTesseract() {
        try {
            String version = TessAPI.INSTANCE.TessVersion();
            return version != null && !version.isBlank();
        } catch (Throwable ex) {
            return false;
        }
    }
}
