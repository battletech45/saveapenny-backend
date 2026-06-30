package com.saveapenny.notification.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.dto.UnreadNotificationCountResponse;
import com.saveapenny.notification.dto.UpdateNotificationRequest;
import com.saveapenny.notification.service.NotificationService;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "Notification read/write and unread count endpoints.")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse response = notificationService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(
            summary = "List notifications",
            description = "Returns paginated notifications. Optionally filter by read/unread state. Pagination query params: page, size, sort.")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Parameter(description = "Optional read-state filter.", example = "false")
            @RequestParam(required = false) Boolean read,
            @ParameterObject
            Pageable pageable) {
        Page<NotificationResponse> response = notificationService.getAll(getCurrentUserId(principal), read, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID notificationId) {
        NotificationResponse response = notificationService.getById(getCurrentUserId(principal), notificationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID notificationId,
            @Valid @RequestBody UpdateNotificationRequest request) {
        NotificationResponse response = notificationService.update(getCurrentUserId(principal), notificationId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID notificationId) {
        notificationService.delete(getCurrentUserId(principal), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        UnreadNotificationCountResponse response = notificationService.getUnreadCount(getCurrentUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        notificationService.markAllAsRead(getCurrentUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
