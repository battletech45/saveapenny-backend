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
    @Mapping(target = "transactionCandidates", ignore = true)
    OcrJobStatusResponse toStatusResponse(OcrJob job);

    OcrTransactionCandidateResponse toTransactionCandidateResponse(OcrTransactionCandidate candidate);

    List<OcrTransactionCandidateResponse> toTransactionCandidateResponses(List<OcrTransactionCandidate> candidates);
}
