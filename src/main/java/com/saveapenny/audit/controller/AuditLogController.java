package com.saveapenny.audit.controller;

import com.saveapenny.audit.dto.AuditLogFilterRequest;
import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.service.AuditLogService;
import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audits")
@PreAuthorize("isAuthenticated()")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AuditLogResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateAuditLogRequest request) {
        AuditLogResponse response = auditLogService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {
        AuditLogFilterRequest filter = AuditLogFilterRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .from(from)
                .to(to)
                .build();

        Page<AuditLogResponse> response = auditLogService.getAll(getCurrentUserId(principal), filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{auditLogId}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID auditLogId) {
        AuditLogResponse response = auditLogService.getById(getCurrentUserId(principal), auditLogId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
