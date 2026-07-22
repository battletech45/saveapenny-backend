package com.saveapenny.billing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.auth.service.JwtService;
import com.saveapenny.billing.dto.EntitlementLimitsResponse;
import com.saveapenny.billing.dto.EntitlementResponse;
import com.saveapenny.billing.dto.FeatureAccessResponse;
import com.saveapenny.billing.exception.RevenueCatAuthenticationException;
import com.saveapenny.billing.exception.RevenueCatClientException;
import com.saveapenny.billing.exception.RevenueCatDisabledException;
import com.saveapenny.billing.service.BillingEntitlementService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingEntitlementService billingEntitlementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUpRateLimitingFilter() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void getEntitlement_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-1")).thenReturn(true);
        when(jwtService.extractUserId("token-1")).thenReturn(userId);
        when(billingEntitlementService.getEntitlement(userId)).thenReturn(sampleResponse("free"));

        mockMvc.perform(get("/api/v1/billing/entitlement")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan").value("free"));
    }

    @Test
    void sync_returnsUpdatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-2")).thenReturn(true);
        when(jwtService.extractUserId("token-2")).thenReturn(userId);
        when(billingEntitlementService.sync(userId)).thenReturn(sampleResponse("plus"));

        mockMvc.perform(post("/api/v1/billing/sync")
                        .header("Authorization", "Bearer token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan").value("plus"));
    }

    @Test
    void getEntitlement_returnsUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/billing/entitlement"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void sync_returnsServiceUnavailable_whenRevenueCatDisabled() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-3")).thenReturn(true);
        when(jwtService.extractUserId("token-3")).thenReturn(userId);
        when(billingEntitlementService.sync(userId))
                .thenThrow(new RevenueCatDisabledException("RevenueCat integration is disabled."));

        mockMvc.perform(post("/api/v1/billing/sync")
                        .header("Authorization", "Bearer token-3"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REVENUECAT_DISABLED"));
    }

    @Test
    void sync_returnsBadGateway_whenRevenueCatClientFails() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-4")).thenReturn(true);
        when(jwtService.extractUserId("token-4")).thenReturn(userId);
        when(billingEntitlementService.sync(userId))
                .thenThrow(new RevenueCatClientException("Failed to fetch subscriber from RevenueCat: timeout", new RuntimeException()));

        mockMvc.perform(post("/api/v1/billing/sync")
                        .header("Authorization", "Bearer token-4"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REVENUECAT_PROVIDER_ERROR"));
    }

    @Test
    void sync_returnsServiceUnavailable_whenRevenueCatAuthenticationFails() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-5")).thenReturn(true);
        when(jwtService.extractUserId("token-5")).thenReturn(userId);
        when(billingEntitlementService.sync(userId))
                .thenThrow(new RevenueCatAuthenticationException("RevenueCat rejected the configured API key (HTTP 401)", new RuntimeException()));

        mockMvc.perform(post("/api/v1/billing/sync")
                        .header("Authorization", "Bearer token-5"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REVENUECAT_AUTH_ERROR"));
    }

    private EntitlementResponse sampleResponse(String plan) {
        return EntitlementResponse.builder()
                .plan(plan)
                .status("active")
                .active(true)
                .willRenew(true)
                .expiresAt(null)
                .trialEndsAt(null)
                .features(FeatureAccessResponse.builder()
                        .assistant(true)
                        .insights(true)
                        .stocks(true)
                        .ocr(true)
                        .csvImport(true)
                        .reportExport(true)
                        .advancedRecurring(true)
                        .goalWhatIf(true)
                        .build())
                .limits(EntitlementLimitsResponse.builder()
                        .activeBudgets(1)
                        .maxActiveBudgets(null)
                        .activeGoals(1)
                        .maxActiveGoals(null)
                        .reportHistoryMonths(24)
                        .build())
                .build();
    }
}
