package com.saveapenny.automation.controller;

import com.saveapenny.automation.dto.CreateRecurringTransactionRequest;
import com.saveapenny.automation.dto.RecurringTransactionResponse;
import com.saveapenny.automation.dto.UpdateRecurringTransactionRequest;
import com.saveapenny.automation.service.RecurringTransactionService;
import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/automations/recurring-transactions")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Automation", description = "Recurring transaction setup and lifecycle endpoints.")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateRecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(
            summary = "List recurring transactions",
            description = "Returns paginated recurring transaction schedules. Pagination query params: page, size, sort.")
    public ResponseEntity<ApiResponse<Page<RecurringTransactionResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @ParameterObject
            Pageable pageable) {
        Page<RecurringTransactionResponse> response = recurringTransactionService.getAll(getCurrentUserId(principal), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{recurringTransactionId}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID recurringTransactionId) {
        RecurringTransactionResponse response = recurringTransactionService.getById(
                getCurrentUserId(principal), recurringTransactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{recurringTransactionId}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID recurringTransactionId,
            @Valid @RequestBody UpdateRecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.update(
                getCurrentUserId(principal), recurringTransactionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{recurringTransactionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID recurringTransactionId) {
        recurringTransactionService.delete(getCurrentUserId(principal), recurringTransactionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
