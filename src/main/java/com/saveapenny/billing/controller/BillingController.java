package com.saveapenny.billing.controller;

import com.saveapenny.billing.dto.EntitlementResponse;
import com.saveapenny.billing.service.BillingEntitlementService;
import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing", description = "Subscription entitlement and RevenueCat sync.")
public class BillingController {

    private final BillingEntitlementService billingEntitlementService;

    public BillingController(BillingEntitlementService billingEntitlementService) {
        this.billingEntitlementService = billingEntitlementService;
    }

    @GetMapping("/entitlement")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get entitlement", description = "Returns the canonical entitlement snapshot for the current user.")
    public ResponseEntity<ApiResponse<EntitlementResponse>> getEntitlement(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        EntitlementResponse response = billingEntitlementService.getEntitlement(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sync entitlement", description = "Fetches the latest subscriber state from RevenueCat and persists it.")
    public ResponseEntity<ApiResponse<EntitlementResponse>> sync(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        EntitlementResponse response = billingEntitlementService.sync(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
