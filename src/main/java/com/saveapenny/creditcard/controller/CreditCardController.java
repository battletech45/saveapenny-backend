package com.saveapenny.creditcard.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.creditcard.dto.CreditCardDetailsRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentResponse;
import com.saveapenny.creditcard.dto.CreditCardStatementResponse;
import com.saveapenny.creditcard.dto.CreditCardSummaryResponse;
import com.saveapenny.creditcard.service.CreditCardService;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/credit")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Credit Cards", description = "Credit card limit, statement, and payment endpoints.")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    @PatchMapping
    @Operation(
            summary = "Update credit card details",
            description = "Updates the credit limit, APR, and/or statement day for a CREDIT account.")
    public ResponseEntity<ApiResponse<CreditCardSummaryResponse>> updateDetails(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditCardDetailsRequest request) {
        CreditCardSummaryResponse response =
                creditCardService.updateDetails(getCurrentUserId(principal), accountId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/statements")
    @Operation(
            summary = "List credit card statements",
            description = "Returns paginated statement history, most recent first. Pagination query params: page, size, sort.")
    public ResponseEntity<ApiResponse<PagedResponse<CreditCardStatementResponse>>> listStatements(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID accountId,
            @ParameterObject Pageable pageable) {
        Page<CreditCardStatementResponse> response =
                creditCardService.listStatements(getCurrentUserId(principal), accountId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
    }

    @PostMapping("/payments")
    @Operation(
            summary = "Make a credit card payment",
            description = "Pays down the credit card balance from another account. paymentType is MINIMUM_DUE, "
                    + "FULL_BALANCE, or CUSTOM (amount required for CUSTOM).")
    public ResponseEntity<ApiResponse<CreditCardPaymentResponse>> makePayment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditCardPaymentRequest request) {
        CreditCardPaymentResponse response =
                creditCardService.makePayment(getCurrentUserId(principal), accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
