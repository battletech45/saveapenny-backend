package com.saveapenny.ocr.application.analysis.classifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.ocr.application.extractor.OcrTextNormalizationService;
import org.junit.jupiter.api.Test;

class OcrDocumentClassifierTest {

    private final OcrDocumentClassifier classifier = new OcrDocumentClassifier(new OcrTextNormalizationService());

    @Test
    void classify_returnsBankReceipt() {
        assertEquals("BANK_RECEIPT", classifier.classify("Banka dekontu\nRef No 123\nSube"));
    }

    @Test
    void classify_returnsTaxReceipt() {
        assertEquals("TAX_RECEIPT", classifier.classify("Vergi Tahsil Alindisi\nMukellef"));
    }

    @Test
    void classify_returnsRetailReceiptOnlyWhenStructureExists() {
        assertEquals("RETAIL_RECEIPT", classifier.classify("Happy Market\nKasiyer 12\nToplam 245,90"));
        assertEquals("GENERIC_DOCUMENT", classifier.classify("2026-05-20 market 15.50"));
    }

    @Test
    void classify_returnsInvoice() {
        assertEquals("INVOICE", classifier.classify("FATURA\nGenel Toplam 3450,00"));
    }
}
