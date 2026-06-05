package com.saveapenny.ocr.application.service;

import com.saveapenny.ocr.application.job.OcrJobAsyncProcessor;
import com.saveapenny.ocr.application.analysis.OcrAnalysisService;
import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysis;
import com.saveapenny.ocr.application.port.in.OcrJobService;
import com.saveapenny.ocr.application.port.in.OcrUploadPayload;
import com.saveapenny.ocr.domain.exception.InvalidOcrFileException;
import com.saveapenny.ocr.domain.exception.OcrJobNotFoundException;
import com.saveapenny.ocr.domain.model.OcrJob;
import com.saveapenny.ocr.domain.model.OcrJobStatus;
import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import com.saveapenny.ocr.infrastructure.persistence.mapper.OcrJobMapper;
import com.saveapenny.ocr.infrastructure.persistence.repository.OcrJobRepository;
import com.saveapenny.ocr.interfaces.http.dto.OcrJobStatusResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrParseDiagnosticsResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrSubmitResponse;
import com.saveapenny.config.OcrProperties;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class OcrJobServiceImpl implements OcrJobService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "application/pdf");

    private final OcrProperties ocrProperties;
    private final OcrJobRepository ocrJobRepository;
    private final OcrJobMapper ocrJobMapper;
    private final OcrAnalysisService ocrAnalysisService;
    private final OcrJobAsyncProcessor ocrJobAsyncProcessor;

    public OcrJobServiceImpl(
            OcrProperties ocrProperties,
            OcrJobRepository ocrJobRepository,
            OcrJobMapper ocrJobMapper,
            OcrAnalysisService ocrAnalysisService,
            OcrJobAsyncProcessor ocrJobAsyncProcessor) {
        this.ocrProperties = ocrProperties;
        this.ocrJobRepository = ocrJobRepository;
        this.ocrJobMapper = ocrJobMapper;
        this.ocrAnalysisService = ocrAnalysisService;
        this.ocrJobAsyncProcessor = ocrJobAsyncProcessor;
    }

    @Override
    public OcrSubmitResponse createJob(UUID currentUserId, MultipartFile file) {
        validateUpload(file);

        OcrJob job = OcrJob.builder()
                .userId(currentUserId)
                .originalFileName(normalizeFileName(file.getOriginalFilename()))
                .contentType(file.getContentType().toLowerCase(Locale.ROOT))
                .status(OcrJobStatus.PENDING)
                .build();
        OcrJob saved = ocrJobRepository.saveAndFlush(job);
        ocrJobAsyncProcessor.process(saved.getId(), snapshotUpload(file));

        return ocrJobMapper.toSubmitResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OcrJobStatusResponse getJobStatus(UUID currentUserId, UUID jobId) {
        OcrJob job = ocrJobRepository.findByIdAndUserId(jobId, currentUserId)
                .orElseThrow(() -> new OcrJobNotFoundException(jobId));

        OcrDocumentAnalysis analysis = ocrAnalysisService.analyze(job.getRawText());
        List<OcrTransactionCandidate> candidates = analysis.transactionCandidates();

        OcrJobStatusResponse response = ocrJobMapper.toStatusResponse(job);
        response.setTransactionCandidates(ocrJobMapper.toTransactionCandidateResponses(candidates));
        response.setDocumentType(analysis.documentType());
        response.setCurrency(analysis.currency());
        response.setMerchantName(analysis.merchantName());
        response.setPaymentDate(analysis.paymentDate());
        response.setIssueDate(analysis.issueDate());
        response.setExtractedDates(analysis.dates());
        response.setExtractedAmounts(analysis.amounts());
        response.setReferenceNumbers(analysis.referenceNumbers());
        response.setLabels(analysis.labels());
        response.setParseConfidence(analysis.parseConfidence());
        String parseWarning = buildParseWarning(job, candidates);
        response.setParseWarning(parseWarning);
        response.setParseDiagnostics(buildParseDiagnostics(job, analysis, candidates, parseWarning));
        return response;
    }

    private OcrParseDiagnosticsResponse buildParseDiagnostics(
            OcrJob job,
            OcrDocumentAnalysis analysis,
            List<OcrTransactionCandidate> candidates,
            String parseWarning) {
        List<String> warnings = new ArrayList<>();
        if (parseWarning != null && !parseWarning.isBlank()) {
            warnings.add(parseWarning);
        }

        List<String> notes = new ArrayList<>();
        if (analysis.documentType() != null) {
            notes.add("Detected document type: " + analysis.documentType());
        }
        if (!analysis.referenceNumbers().isEmpty()) {
            notes.add("Reference numbers extracted: " + analysis.referenceNumbers().size());
        }
        if (!analysis.labels().isEmpty()) {
            notes.add("Detected labels: " + String.join(", ", analysis.labels()));
        }

        return OcrParseDiagnosticsResponse.builder()
                .detectedDocumentType(analysis.documentType())
                .confidenceScore(analysis.parseConfidence())
                .warnings(warnings.isEmpty() ? List.of() : List.copyOf(warnings))
                .notes(notes.isEmpty() ? List.of() : List.copyOf(notes))
                .selectedCandidateReason(buildSelectedCandidateReason(job, analysis, candidates))
                .noCandidateReason(buildNoCandidateReason(job, analysis, candidates, parseWarning))
                .build();
    }

    private String buildSelectedCandidateReason(
            OcrJob job,
            OcrDocumentAnalysis analysis,
            List<OcrTransactionCandidate> candidates) {
        if (job.getStatus() != OcrJobStatus.COMPLETED || candidates.isEmpty()) {
            return null;
        }
        if (containsTotalLabel(analysis.labels())) {
            return "Selected candidate amount near TOPLAM/TOTAL label";
        }
        if (analysis.paymentDate() != null) {
            return "Selected candidate matched extracted payment date";
        }
        return "Selected highest-confidence parsed candidate";
    }

    private String buildNoCandidateReason(
            OcrJob job,
            OcrDocumentAnalysis analysis,
            List<OcrTransactionCandidate> candidates,
            String parseWarning) {
        if (job.getStatus() != OcrJobStatus.COMPLETED || !candidates.isEmpty()) {
            return null;
        }
        if (parseWarning != null && parseWarning.contains("no text could be extracted")) {
            return "No OCR text was extracted from the uploaded document";
        }
        if (!analysis.amounts().isEmpty() && analysis.dates().isEmpty()) {
            return "Found amount candidates but no valid transaction date";
        }
        if (!analysis.dates().isEmpty() && analysis.amounts().isEmpty()) {
            return "Found date candidates but no valid transaction amount";
        }
        return "No candidate satisfied the parsing heuristics with sufficient confidence";
    }

    private boolean containsTotalLabel(List<String> labels) {
        return labels.stream().anyMatch(label -> "toplam".equals(label) || "total".equals(label) || "genel toplam".equals(label));
    }

    private String buildParseWarning(OcrJob job, List<OcrTransactionCandidate> candidates) {
        if (job.getStatus() != OcrJobStatus.COMPLETED) {
            return null;
        }
        if (job.getRawText() == null || job.getRawText().isBlank()) {
            return "OCR completed, but no text could be extracted from the document";
        }
        if (!candidates.isEmpty()) {
            return null;
        }
        return "OCR text extracted, but no transaction could be confidently parsed";
    }

    private void validateUpload(MultipartFile file) {
        if (!ocrProperties.enabled()) {
            throw new InvalidOcrFileException("OCR feature is disabled by configuration");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidOcrFileException("file is empty");
        }
        if (file.getSize() > ocrProperties.maxFileSizeBytes()) {
            throw new InvalidOcrFileException("file exceeds max size of " + ocrProperties.maxFileSizeBytes() + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidOcrFileException("supported types are image/png, image/jpeg, application/pdf");
        }
    }

    private OcrUploadPayload snapshotUpload(MultipartFile file) {
        try {
            return new OcrUploadPayload(
                    normalizeFileName(file.getOriginalFilename()),
                    file.getContentType(),
                    file.getBytes());
        } catch (IOException ex) {
            throw new InvalidOcrFileException("unable to read uploaded file");
        }
    }

    private String normalizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "ocr-upload";
        }
        return originalFileName.trim();
    }
}
