package com.saveapenny.ocr.application.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.ocr.application.analysis.OcrAnalysisService;
import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysis;
import com.saveapenny.ocr.support.OcrTestFixtureFactory;
import org.junit.jupiter.api.Test;

class OcrParserStrategyTest {

    private final OcrAnalysisService service = OcrTestFixtureFactory.createAnalysisService();

    @Test
    void analyze_usesTaxReceiptParserWhenClassifierMatches() {
        String rawText = """
                VERGI TAHSIL ALINDISI
                MUKELLEFIN

                Odeme Tarihi 17/05/2026
                9014 YURT DISI CIKIS HARCI

                TOPLAM 1.250,00-
                #TL#
                """;

        OcrDocumentAnalysis analysis = service.analyze(rawText);

        assertEquals("TAX_RECEIPT", analysis.documentType());
        assertEquals(1, analysis.transactionCandidates().size());
        assertTrue(analysis.parseConfidence() >= 0.90);
        assertEquals("9014 YURT DISI CIKIS HARCI", analysis.transactionCandidates().getFirst().description());
        assertEquals("-1250.00", analysis.transactionCandidates().getFirst().amount().toString());
    }

    @Test
    void analyze_usesRetailReceiptParserWhenClassifierMatches() {
        String rawText = """
                HAPPY MARKET
                Tarih 17/05/2026
                Kasiyer 12
                TOPLAM 245,90
                """;

        OcrDocumentAnalysis analysis = service.analyze(rawText);

        assertEquals("RETAIL_RECEIPT", analysis.documentType());
        assertEquals(1, analysis.transactionCandidates().size());
        assertTrue(analysis.parseConfidence() >= 0.90);
        assertEquals("HAPPY MARKET", analysis.transactionCandidates().getFirst().description());
        assertEquals("245.90", analysis.transactionCandidates().getFirst().amount().toString());
        assertEquals("FOOD", analysis.transactionCandidates().getFirst().categoryHint());
    }

    @Test
    void analyze_usesInvoiceParserWhenClassifierMatches() {
        String rawText = """
                ACME LTD
                FATURA
                Duzenlenme Tarihi 17/05/2026
                Genel Toplam 3.450,00
                """;

        OcrDocumentAnalysis analysis = service.analyze(rawText);

        assertEquals("INVOICE", analysis.documentType());
        assertEquals(1, analysis.transactionCandidates().size());
        assertTrue(analysis.parseConfidence() >= 0.90);
        assertEquals("ACME LTD", analysis.transactionCandidates().getFirst().description());
        assertEquals("3450.00", analysis.transactionCandidates().getFirst().amount().toString());
        assertEquals("2026-05-17", analysis.transactionCandidates().getFirst().date().toString());
    }
}
