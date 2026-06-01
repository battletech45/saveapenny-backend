package com.saveapenny.ocr.interfaces.http.dto;

import com.saveapenny.ocr.domain.model.OcrJobStatus;
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
public class OcrSubmitResponse {

    private UUID jobId;
    private OcrJobStatus status;
}
