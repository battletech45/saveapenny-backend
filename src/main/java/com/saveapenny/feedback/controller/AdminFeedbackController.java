package com.saveapenny.feedback.controller;

import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.dto.UpdateFeedbackStatusRequest;
import com.saveapenny.feedback.service.FeedbackService;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/feedback")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative observability and metrics endpoints.")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PatchMapping("/{feedbackId}/status")
    @Operation(summary = "Update feedback status", description = "Admin-only. Updates the status of a feedback entry and notifies its owner.")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateStatus(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {
        FeedbackResponse response = feedbackService.updateStatus(feedbackId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
