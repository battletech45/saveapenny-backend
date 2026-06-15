package com.saveapenny.ocr.support.runtime;

import com.saveapenny.config.OcrProperties;
import org.springframework.stereotype.Component;

@Component
public class OcrRuntimeChecker {

    private final OcrProperties ocrProperties;

    public OcrRuntimeChecker(OcrProperties ocrProperties) {
        this.ocrProperties = ocrProperties;
    }

    public OcrRuntimeStatus check() {
        if (!ocrProperties.enabled()) {
            return new OcrRuntimeStatus(false, false, false, ocrProperties.language(), resolveTessdataPath(), "OCR is disabled");
        }

        String tessdataPath = resolveTessdataPath();
        boolean tessdataPathValid = OcrRuntimeEnvironment.isTessdataAvailable(tessdataPath, ocrProperties.language());
        boolean nativeLibraryLoaded = OcrRuntimeEnvironment.canLoadNativeTesseract();

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

    private String resolveTessdataPath() {
        return OcrRuntimeEnvironment.resolveTessdataPath(ocrProperties.tessdataPath());
    }
}
