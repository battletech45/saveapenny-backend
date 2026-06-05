package com.saveapenny.ocr.infrastructure.persistence.mapper;

import com.saveapenny.ocr.domain.model.OcrJob;
import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import com.saveapenny.ocr.interfaces.http.dto.OcrJobStatusResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrSubmitResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrTransactionCandidateResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OcrJobMapper {

    @Mapping(target = "jobId", source = "id")
    OcrSubmitResponse toSubmitResponse(OcrJob job);

    @Mapping(target = "jobId", source = "id")
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "merchantName", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "extractedDates", ignore = true)
    @Mapping(target = "extractedAmounts", ignore = true)
    @Mapping(target = "referenceNumbers", ignore = true)
    @Mapping(target = "labels", ignore = true)
    @Mapping(target = "parseConfidence", ignore = true)
    @Mapping(target = "parseWarning", ignore = true)
    @Mapping(target = "parseDiagnostics", ignore = true)
    @Mapping(target = "transactionCandidates", ignore = true)
    OcrJobStatusResponse toStatusResponse(OcrJob job);

    OcrTransactionCandidateResponse toTransactionCandidateResponse(OcrTransactionCandidate candidate);

    List<OcrTransactionCandidateResponse> toTransactionCandidateResponses(List<OcrTransactionCandidate> candidates);
}
