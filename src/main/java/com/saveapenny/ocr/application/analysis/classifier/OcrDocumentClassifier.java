package com.saveapenny.ocr.application.analysis.classifier;

import com.saveapenny.ocr.application.extractor.OcrTextNormalizationService;
import org.springframework.stereotype.Service;

@Service
public class OcrDocumentClassifier {

    private final OcrTextNormalizationService normalizationService;

    public OcrDocumentClassifier(OcrTextNormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    public String classify(String rawText) {
        String normalized = normalizationService.normalizeForMatching(rawText);
        if (normalized.contains("vergi") || normalized.contains("mukellef") || normalized.contains("tahsil")) {
            return "TAX_RECEIPT";
        }
        if (normalized.contains("banka") || normalized.contains("sube") || normalized.contains("kredi kart") || normalized.contains("dekont") || normalized.contains("ref no")) {
            return "BANK_RECEIPT";
        }
        if (normalized.contains("invoice") || normalized.contains("fatura")) {
            return "INVOICE";
        }
        boolean retailMerchant = normalized.contains("market") || normalized.contains("restaurant") || normalized.contains("grocery");
        boolean retailStructure = normalized.contains("fis")
                || normalized.contains("toplam")
                || normalized.contains("kdv")
                || normalized.contains("kasiyer");
        if (retailMerchant && retailStructure) {
            return "RETAIL_RECEIPT";
        }
        return "GENERIC_DOCUMENT";
    }
}
