package com.saveapenny.budget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.exception.BudgetAlreadyExistsException;
import com.saveapenny.budget.exception.BudgetNotFoundException;
import com.saveapenny.budget.service.BudgetService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BudgetController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BudgetService budgetService;

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
    void create_returnsCreatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-1")).thenReturn(true);
        when(jwtService.extractUserId("token-1")).thenReturn(userId);
        when(budgetService.create(eq(userId), any(CreateBudgetRequest.class))).thenReturn(sampleResponse());

        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period").value("MONTHLY"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-2")).thenReturn(true);
        when(jwtService.extractUserId("token-2")).thenReturn(userId);
        when(budgetService.getAll(eq(userId), eq(BudgetPeriod.MONTHLY), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/budgets")
                        .param("period", "MONTHLY")
                        .header("Authorization", "Bearer token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].amount").value(400.0));
    }

    @Test
    void getStatus_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-3")).thenReturn(true);
        when(jwtService.extractUserId("token-3")).thenReturn(userId);
        when(budgetService.getStatus(userId, budgetId)).thenReturn(BudgetStatusResponse.builder()
                .category("Food")
                .budgetAmount(new BigDecimal("400.0000"))
                .spentAmount(new BigDecimal("100.0000"))
                .remainingAmount(new BigDecimal("300.0000"))
                .usagePercentage(new BigDecimal("25.00"))
                .status("ON_TRACK")
                .build());

        mockMvc.perform(get("/api/v1/budgets/{budgetId}/status", budgetId)
                        .header("Authorization", "Bearer token-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ON_TRACK"));
    }

    @Test
    void update_returnsUpdatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        BudgetResponse response = sampleResponse();
        response.setAmount(new BigDecimal("500.0000"));

        when(jwtService.isAccessTokenValid("token-4")).thenReturn(true);
        when(jwtService.extractUserId("token-4")).thenReturn(userId);
        when(budgetService.update(eq(userId), eq(budgetId), any(UpdateBudgetRequest.class))).thenReturn(response);

        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("500.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .build();

        mockMvc.perform(put("/api/v1/budgets/{budgetId}", budgetId)
                        .header("Authorization", "Bearer token-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(500.0));
    }

    @Test
    void delete_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-5")).thenReturn(true);
        when(jwtService.extractUserId("token-5")).thenReturn(userId);
        doNothing().when(budgetService).delete(userId, budgetId);

        mockMvc.perform(delete("/api/v1/budgets/{budgetId}", budgetId)
                        .header("Authorization", "Bearer token-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getById_returnsNotFound_whenBudgetMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-6")).thenReturn(true);
        when(jwtService.extractUserId("token-6")).thenReturn(userId);
        when(budgetService.getById(userId, budgetId)).thenThrow(new BudgetNotFoundException(budgetId));

        mockMvc.perform(get("/api/v1/budgets/{budgetId}", budgetId)
                        .header("Authorization", "Bearer token-6"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUDGET_NOT_FOUND"));
    }

    @Test
    void create_returnsConflict_whenBudgetAlreadyExists() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-7")).thenReturn(true);
        when(jwtService.extractUserId("token-7")).thenReturn(userId);
        when(budgetService.create(eq(userId), any(CreateBudgetRequest.class))).thenThrow(new BudgetAlreadyExistsException());

        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer token-7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUDGET_ALREADY_EXISTS"));
    }

    private BudgetResponse sampleResponse() {
        return BudgetResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
