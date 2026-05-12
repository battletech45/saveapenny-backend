package com.saveapenny.transaction.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.CreateTransferRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getAll(
                getCurrentUserId(principal),
                from,
                to,
                type,
                accountId,
                categoryId,
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
