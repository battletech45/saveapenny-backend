package com.saveapenny.budget.controller;

import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.service.BudgetService;
import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
@PreAuthorize("isAuthenticated()")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateBudgetRequest request) {
        BudgetResponse response = budgetService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BudgetResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) BudgetPeriod period,
            Pageable pageable) {
        Page<BudgetResponse> response = budgetService.getAll(getCurrentUserId(principal), period, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID budgetId) {
        BudgetResponse response = budgetService.getById(getCurrentUserId(principal), budgetId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{budgetId}/status")
    public ResponseEntity<ApiResponse<BudgetStatusResponse>> getStatus(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID budgetId) {
        BudgetStatusResponse response = budgetService.getStatus(getCurrentUserId(principal), budgetId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<BudgetResponse>> update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) {
        BudgetResponse response = budgetService.update(getCurrentUserId(principal), budgetId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID budgetId) {
        budgetService.delete(getCurrentUserId(principal), budgetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
