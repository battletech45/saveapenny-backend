package com.saveapenny.ocr.application.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrTextParserServiceTest {

    private final OcrTextParserService parser = new OcrTextParserService();

    @Test
    void parseTransactionCandidates_returnsDomainCandidates() {
        String rawText = "2026-05-20 market 15.50\nnoise\n21/05/2026 uber 42,10";

        List<OcrTransactionCandidate> candidates = parser.parseTransactionCandidates(rawText);

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
    void parseTransactionCandidates_ignoresLinesWithoutBothDateAndAmount() {
        String rawText = "market only\n2026-05-20 no amount\namount 12.00 no date";

        List<OcrTransactionCandidate> candidates = parser.parseTransactionCandidates(rawText);

        assertTrue(candidates.isEmpty());
    }
}
