package com.saveapenny.feedback.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.feedback.dto.CreateFeedbackRequest;
import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.entity.FeedbackType;
import com.saveapenny.feedback.service.FeedbackService;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Feedback", description = "Feedback submission and user history endpoints.")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @Operation(
            summary = "Submit feedback",
            description = "Creates a new feedback entry for the current user. Status starts as OPEN and is updated by an admin.")
    public ResponseEntity<ApiResponse<FeedbackResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateFeedbackRequest request) {
        FeedbackResponse response = feedbackService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(
            summary = "List feedback",
            description = "Returns paginated feedback submitted by the current user, including its current status (OPEN, IN_REVIEW, RESOLVED, REJECTED). Optionally filter by type. Pagination query params: page, size, sort.")
    public ResponseEntity<ApiResponse<PagedResponse<FeedbackResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Parameter(description = "Optional type filter.", example = "FEATURE_REQUEST")
            @RequestParam(required = false) FeedbackType type,
            @ParameterObject Pageable pageable) {
        Page<FeedbackResponse> response = feedbackService.getAll(getCurrentUserId(principal), type, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
    }

    @GetMapping("/{feedbackId}")
    @Operation(
            summary = "Get feedback by id",
            description = "Returns a single feedback item owned by the current user, including its current status.")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID feedbackId) {
        FeedbackResponse response = feedbackService.getById(getCurrentUserId(principal), feedbackId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{feedbackId}")
    @Operation(
            summary = "Delete feedback",
            description = "Deletes a feedback item owned by the current user.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID feedbackId) {
        feedbackService.delete(getCurrentUserId(principal), feedbackId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
