package com.saveapenny.transaction.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.CreateTransferRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/api/v1/transactions")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Transactions", description = "Transaction CRUD, filtering, and transfer endpoints.")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateTransferRequest request) {
        TransferResponse response = transactionService.createTransfer(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(
            summary = "Search transactions",
            description = "Returns paginated transactions with optional filters by date range, type, account, category, amount range, and keyword. Pagination query params: page, size, sort.")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Parameter(description = "Optional start date (ISO-8601).", example = "2026-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Optional end date (ISO-8601).", example = "2026-06-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Optional transaction type.", example = "EXPENSE")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Optional account UUID.", example = "7fc9eaf7-2f95-4fd7-b632-56f0ce7a2c9f")
            @RequestParam(required = false) UUID accountId,
            @Parameter(description = "Optional category UUID.", example = "68f89bdf-c23f-4124-a332-967ffd344a06")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Optional minimum amount.", example = "10.00")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Optional maximum amount.", example = "500.00")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Optional free-text keyword.", example = "market")
            @RequestParam(required = false) String keyword,
            @ParameterObject
            Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getAll(
                getCurrentUserId(principal),
                from,
                to,
                type,
                accountId,
                categoryId,
                minAmount,
                maxAmount,
                keyword,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID transactionId) {
        TransactionResponse response = transactionService.getById(getCurrentUserId(principal), transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionRequest request) {
        TransactionResponse response = transactionService.update(getCurrentUserId(principal), transactionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID transactionId) {
        transactionService.delete(getCurrentUserId(principal), transactionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
