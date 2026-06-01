package com.saveapenny.ocr.interfaces.http.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrTransactionCandidateResponse {

    private LocalDate date;
    private BigDecimal amount;
    private String description;
    private String categoryHint;
}
