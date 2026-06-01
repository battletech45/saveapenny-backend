package com.saveapenny.ocr.interfaces.http;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.ocr.interfaces.http.dto.OcrJobStatusResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrSubmitResponse;
import com.saveapenny.ocr.application.port.in.OcrJobService;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports/ocr")
@PreAuthorize("isAuthenticated()")
@Tag(name = "OCR Imports", description = "Receipt OCR upload and async job status endpoints.")
public class OcrImportController {

    private final OcrJobService ocrJobService;

    public OcrImportController(OcrJobService ocrJobService) {
        this.ocrJobService = ocrJobService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Submit OCR job",
            description = "Uploads a PNG/JPEG/PDF receipt and starts asynchronous OCR processing. Use returned jobId to poll status.")
    public ResponseEntity<ApiResponse<OcrSubmitResponse>> upload(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestPart("file") MultipartFile file) {
        OcrSubmitResponse response = ocrJobService.createJob(getCurrentUserId(principal), file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    @GetMapping("/{jobId}")
    @Operation(
            summary = "Get OCR job status",
            description = "Returns OCR lifecycle status, raw text, snippet, and parsed transaction candidates when available.")
    public ResponseEntity<ApiResponse<OcrJobStatusResponse>> getStatus(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID jobId) {
        OcrJobStatusResponse response = ocrJobService.getJobStatus(getCurrentUserId(principal), jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
