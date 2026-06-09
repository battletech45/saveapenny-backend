package com.saveapenny.automation.controller;

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
import com.saveapenny.automation.dto.CreateRecurringTransactionRequest;
import com.saveapenny.automation.dto.RecurringTransactionResponse;
import com.saveapenny.automation.dto.UpdateRecurringTransactionRequest;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.exception.RecurringTransactionNotFoundException;
import com.saveapenny.automation.service.RecurringTransactionService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import com.saveapenny.transaction.entity.TransactionType;
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

@WebMvcTest(RecurringTransactionController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class RecurringTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecurringTransactionService recurringTransactionService;

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
        when(jwtService.isAccessTokenValid("token-a1")).thenReturn(true);
        when(jwtService.extractUserId("token-a1")).thenReturn(userId);
        when(recurringTransactionService.create(eq(userId), any(CreateRecurringTransactionRequest.class))).thenReturn(sampleResponse());

        CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/automations/recurring-transactions")
                        .header("Authorization", "Bearer token-a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.frequency").value("MONTHLY"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a2")).thenReturn(true);
        when(jwtService.extractUserId("token-a2")).thenReturn(userId);
        when(recurringTransactionService.getAll(eq(userId), any())).thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/automations/recurring-transactions")
                        .header("Authorization", "Bearer token-a2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].amount").value(100.0));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a3")).thenReturn(true);
        when(jwtService.extractUserId("token-a3")).thenReturn(userId);
        when(recurringTransactionService.getById(userId, recurringId)).thenThrow(new RecurringTransactionNotFoundException(recurringId));

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer token-a3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RECURRING_TRANSACTION_NOT_FOUND"));
    }

    @Test
    void update_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a4")).thenReturn(true);
        when(jwtService.extractUserId("token-a4")).thenReturn(userId);
        when(recurringTransactionService.update(eq(userId), eq(recurringId), any(UpdateRecurringTransactionRequest.class)))
                .thenReturn(sampleResponse());

        UpdateRecurringTransactionRequest request = UpdateRecurringTransactionRequest.builder()
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("50.0000"))
                .frequency(RecurringFrequency.WEEKLY)
                .nextRunDate(LocalDate.now().plusDays(2))
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer token-a4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a5")).thenReturn(true);
        when(jwtService.extractUserId("token-a5")).thenReturn(userId);
        doNothing().when(recurringTransactionService).delete(userId, recurringId);

        mockMvc.perform(delete("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer token-a5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private RecurringTransactionResponse sampleResponse() {
        return RecurringTransactionResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.now().plusDays(1))
                .active(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
