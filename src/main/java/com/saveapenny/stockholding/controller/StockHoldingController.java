package com.saveapenny.stockholding.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import com.saveapenny.stockholding.dto.CreateHoldingRequest;
import com.saveapenny.stockholding.dto.HoldingResponse;
import com.saveapenny.stockholding.dto.HoldingSummaryResponse;
import com.saveapenny.stockholding.dto.UpdateHoldingRequest;
import com.saveapenny.stockholding.service.StockHoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/stocks/holdings")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Stock Holdings", description = "User stock holding management and profit/loss tracking.")
public class StockHoldingController {

    private final StockHoldingService stockHoldingService;

    public StockHoldingController(StockHoldingService stockHoldingService) {
        this.stockHoldingService = stockHoldingService;
    }

    @PostMapping
    @Operation(summary = "Create a stock holding", description = "Records a stock purchase with symbol, quantity, purchase price, and date.")
    public ResponseEntity<ApiResponse<HoldingResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateHoldingRequest request) {
        HoldingResponse response = stockHoldingService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List stock holdings", description = "Returns paginated holdings with live profit/loss data.")
    public ResponseEntity<ApiResponse<PagedResponse<HoldingResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @ParameterObject Pageable pageable) {
        Page<HoldingResponse> response = stockHoldingService.getAll(principal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Portfolio summary", description = "Returns aggregate profit/loss across all holdings.")
    public ResponseEntity<ApiResponse<HoldingSummaryResponse>> getSummary(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        HoldingSummaryResponse response = stockHoldingService.getSummary(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{holdingId}")
    @Operation(summary = "Get holding details", description = "Returns a single holding with live profit/loss data.")
    public ResponseEntity<ApiResponse<HoldingResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID holdingId) {
        HoldingResponse response = stockHoldingService.getById(principal.userId(), holdingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{holdingId}")
    @Operation(summary = "Update a holding", description = "Partial update of holding fields. Only provided fields are changed.")
    public ResponseEntity<ApiResponse<HoldingResponse>> update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID holdingId,
            @Valid @RequestBody UpdateHoldingRequest request) {
        HoldingResponse response = stockHoldingService.update(principal.userId(), holdingId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{holdingId}")
    @Operation(summary = "Delete a holding", description = "Removes a stock holding. Hard delete — use when selling.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID holdingId) {
        stockHoldingService.delete(principal.userId(), holdingId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
