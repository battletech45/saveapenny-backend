package com.saveapenny.ocr.interfaces.http.dto;

import com.saveapenny.ocr.domain.model.OcrJobStatus;
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
    private List<OcrTransactionCandidateResponse> transactionCandidates;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
