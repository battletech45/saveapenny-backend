package com.saveapenny.ocr.application.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import com.saveapenny.ocr.support.OcrTestFixtureFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrAnalysisServiceTest {

    private final OcrAnalysisService service = OcrTestFixtureFactory.createAnalysisService();

    @Test
    void parseTransactionCandidates_returnsDomainCandidates() {
        String rawText = "2026-05-20 market 15.50\nnoise\n21/05/2026 uber 42,10";

        List<OcrTransactionCandidate> candidates = service.parseTransactionCandidates(rawText);

        assertEquals(2, candidates.size());

        OcrTransactionCandidate first = candidates.get(0);
        assertEquals("2026-05-20", first.date().toString());
        assertEquals("15.50", first.amount().toString());
        assertEquals("FOOD", first.categoryHint());

        OcrTransactionCandidate second = candidates.get(1);
        assertEquals("2026-05-21", second.date().toString());
        assertEquals("42.10", second.amount().toString());
        assertEquals("TRANSPORT", second.categoryHint());
    }

    @Test
    void parseTransactionCandidates_pairsAdjacentDateAndAmountLines() {
        String rawText = "Odeme Tarihi 17/05/2026\n9014 YURT DISI CIKIS HARCI 2026 1 1.250,00-";

        List<OcrTransactionCandidate> candidates = service.parseTransactionCandidates(rawText);

        assertEquals(1, candidates.size());
        OcrTransactionCandidate first = candidates.getFirst();
        assertEquals("2026-05-17", first.date().toString());
        assertEquals("-1250.00", first.amount().toString());
        assertEquals("9014 YURT DISI CIKIS HARCI 2026 1 1.250,00-", first.description());
    }

    @Test
    void analyze_extractsBlocksAndIndependentFields() {
        String rawText = """
                YAPI VE KREDI BANKASI A.S.
                Ref No 253385491674

                Odeme Tarihi 17/05/2026
                9014 YURT DISI CIKIS HARCI

                TOPLAM 1.250,00-
                #TL#
                """;

        OcrDocumentAnalysis analysis = service.analyze(rawText);

        assertEquals(3, analysis.blocks().size());
        assertEquals(OcrDocumentBlockType.HEADER, analysis.blocks().get(0).type());
        assertEquals(OcrDocumentBlockType.DETAIL, analysis.blocks().get(1).type());
        assertEquals(OcrDocumentBlockType.TOTALS, analysis.blocks().get(2).type());
        assertEquals("BANK_RECEIPT", analysis.documentType());
        assertEquals("TRY", analysis.currency());
        assertEquals("YAPI VE KREDI BANKASI A.S.", analysis.merchantName());
        assertEquals("2026-05-17", analysis.paymentDate().toString());
        assertNull(analysis.issueDate());
        assertTrue(analysis.parseConfidence() >= 0.90);
        assertEquals(List.of("253385491674"), analysis.referenceNumbers());
        assertEquals(List.of("2026-05-17"), analysis.dates().stream().map(Object::toString).toList());
        assertEquals(List.of("-1250.00"), analysis.amounts().stream().map(Object::toString).toList());
        assertTrue(analysis.labels().contains("toplam"));
        assertNotNull(analysis.fields());
        assertEquals(1, analysis.transactionCandidates().size());
        assertEquals("9014 YURT DISI CIKIS HARCI", analysis.transactionCandidates().getFirst().description());
    }

    @Test
    void parseTransactionCandidates_ignoresTextWithoutBothAmountAndDate() {
        String rawText = "market only\nreferans no 12345\namount 12.00 no date";

        List<OcrTransactionCandidate> candidates = service.parseTransactionCandidates(rawText);

        assertTrue(candidates.isEmpty());
    }

    @Test
    void analyze_assignsLowerConfidenceToGenericDocumentFallback() {
        String rawText = "2026-05-20 market 15.50\nnoise\n21/05/2026 uber 42,10";

        OcrDocumentAnalysis analysis = service.analyze(rawText);

        assertEquals("GENERIC_DOCUMENT", analysis.documentType());
        assertEquals(2, analysis.transactionCandidates().size());
        assertTrue(analysis.parseConfidence() < 0.90);
    }
}
