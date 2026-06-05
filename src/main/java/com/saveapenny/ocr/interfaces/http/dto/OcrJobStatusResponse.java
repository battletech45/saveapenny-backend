package com.saveapenny.ocr.interfaces.http.dto;

import com.saveapenny.ocr.domain.model.OcrJobStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
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
public class OcrJobStatusResponse {

    private UUID jobId;
    private OcrJobStatus status;
    private String originalFileName;
    private String errorMessage;
    private String resultSnippet;
    private String rawText;
    private String documentType;
    private String currency;
    private String merchantName;
    private LocalDate paymentDate;
    private LocalDate issueDate;
    private List<LocalDate> extractedDates;
    private List<BigDecimal> extractedAmounts;
    private List<String> referenceNumbers;
    private List<String> labels;
    private Double parseConfidence;
    private String parseWarning;
    private OcrParseDiagnosticsResponse parseDiagnostics;
    private List<OcrTransactionCandidateResponse> transactionCandidates;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
