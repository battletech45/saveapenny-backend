package com.saveapenny.ocr.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import com.saveapenny.ocr.interfaces.http.dto.OcrTransactionCandidateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OcrJobMapperTest {

    private final OcrJobMapper mapper = Mappers.getMapper(OcrJobMapper.class);

    @Test
    void toTransactionCandidateResponses_mapsDomainCandidatesToHttpDto() {
        List<OcrTransactionCandidate> input = List.of(
                new OcrTransactionCandidate(LocalDate.of(2026, 5, 20), new BigDecimal("15.50"), "market 15.50", "FOOD"),
                new OcrTransactionCandidate(LocalDate.of(2026, 5, 21), new BigDecimal("42.10"), "uber 42.10", "TRANSPORT"));

        List<OcrTransactionCandidateResponse> output = mapper.toTransactionCandidateResponses(input);

        assertEquals(2, output.size());
        assertEquals(LocalDate.of(2026, 5, 20), output.get(0).getDate());
        assertEquals(new BigDecimal("15.50"), output.get(0).getAmount());
        assertEquals("FOOD", output.get(0).getCategoryHint());

        assertEquals(LocalDate.of(2026, 5, 21), output.get(1).getDate());
        assertEquals(new BigDecimal("42.10"), output.get(1).getAmount());
        assertEquals("TRANSPORT", output.get(1).getCategoryHint());
    }
}
