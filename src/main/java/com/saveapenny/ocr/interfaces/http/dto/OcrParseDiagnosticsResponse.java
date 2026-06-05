package com.saveapenny.ocr.interfaces.http.dto;

import java.util.List;
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
public class OcrParseDiagnosticsResponse {

    private String detectedDocumentType;
    private Double confidenceScore;
    private List<String> warnings;
    private List<String> notes;
    private String selectedCandidateReason;
    private String noCandidateReason;
}
