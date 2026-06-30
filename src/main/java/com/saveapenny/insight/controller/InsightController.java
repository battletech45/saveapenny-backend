package com.saveapenny.insight.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.insight.dto.GenerateInsightsRequest;
import com.saveapenny.insight.dto.InsightResponse;
import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.insight.service.InsightService;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Insights", description = "AI-generated financial insights and analysis.")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InsightResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) InsightType type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "generatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<InsightResponse> response = insightService.getAll(getUserId(principal), type, severity, isRead, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InsightResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID id) {
        InsightResponse response = insightService.getById(getUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<InsightResponse>> markAsRead(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID id) {
        InsightResponse response = insightService.markAsRead(getUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<InsightResponse>> dismiss(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID id) {
        InsightResponse response = insightService.dismiss(getUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generate(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody(required = false) GenerateInsightsRequest request) {
        int count = insightService.generate(getUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("generatedCount", count)));
    }

    private UUID getUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
