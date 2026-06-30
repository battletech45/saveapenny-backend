package com.saveapenny.audit.controller;

import com.saveapenny.audit.dto.AuditLogFilterRequest;
import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.service.AuditLogService;
import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Audit", description = "Audit log creation and search endpoints.")
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
    @Operation(
            summary = "Search audit logs",
            description = """
                    Returns audit entries for the current user.

                    Parameter guide:
                    - entityType: optional enum filter (for example TRANSACTION, BUDGET, ACCOUNT).
                    - entityId: optional UUID of a specific entity.
                    - from / to: optional ISO-8601 datetime range (inclusive).
                    - page / size / sort: pagination controls.

                    Example:
                    /api/v1/audits?entityType=TRANSACTION&from=2026-06-01T00:00:00Z&to=2026-06-01T23:59:59Z&page=0&size=20&sort=createdAt,desc
                    """)
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Parameter(description = "Optional entity type filter.", example = "TRANSACTION")
            @RequestParam(required = false) AuditEntityType entityType,
            @Parameter(description = "Optional entity UUID filter.", example = "2c2d67ca-bb31-4eef-a1dd-98459631f4af")
            @RequestParam(required = false) UUID entityId,
            @Parameter(description = "Optional start datetime in ISO-8601.", example = "2026-06-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @Parameter(description = "Optional end datetime in ISO-8601.", example = "2026-06-01T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @ParameterObject
            Pageable pageable) {
        AuditLogFilterRequest filter = AuditLogFilterRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .from(from)
                .to(to)
                .build();

        Page<AuditLogResponse> response = auditLogService.getAll(getCurrentUserId(principal), filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
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
