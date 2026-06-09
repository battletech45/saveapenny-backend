package com.saveapenny.transaction.controller;

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
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.CreateTransferRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
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

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

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
    void create_returnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-1")).thenReturn(true);
        when(jwtService.extractUserId("token-1")).thenReturn(userId);
        when(transactionService.create(eq(userId), any(CreateTransactionRequest.class))).thenReturn(sampleResponse());

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("12.5000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createTransfer_returnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-2")).thenReturn(true);
        when(jwtService.extractUserId("token-2")).thenReturn(userId);
        when(transactionService.createTransfer(eq(userId), any(CreateTransferRequest.class))).thenReturn(
                TransferResponse.builder().transactionId(UUID.randomUUID()).build());

        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer token-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAll_returnsPageEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-3")).thenReturn(true);
        when(jwtService.extractUserId("token-3")).thenReturn(userId);
        when(transactionService.getAll(eq(userId), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer token-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("EXPENSE"));
    }

    @Test
    void getAll_withAdvancedFilters_returnsPageEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-3b")).thenReturn(true);
        when(jwtService.extractUserId("token-3b")).thenReturn(userId);
        when(transactionService.getAll(
                        eq(userId),
                        eq(LocalDate.of(2026, 5, 1)),
                        eq(LocalDate.of(2026, 5, 31)),
                        eq(TransactionType.EXPENSE),
                        any(),
                        any(),
                        eq(new BigDecimal("10.00")),
                        eq(new BigDecimal("50.00")),
                        eq("groceries"),
                        any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer token-3b")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .param("type", "EXPENSE")
                        .param("minAmount", "10.00")
                        .param("maxAmount", "50.00")
                        .param("keyword", "groceries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_returnsSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-4")).thenReturn(true);
        when(jwtService.extractUserId("token-4")).thenReturn(userId);
        doNothing().when(transactionService).delete(userId, id);

        mockMvc.perform(delete("/api/v1/transactions/{id}", id)
                        .header("Authorization", "Bearer token-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_returnsSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-5")).thenReturn(true);
        when(jwtService.extractUserId("token-5")).thenReturn(userId);
        when(transactionService.update(eq(userId), eq(id), any(UpdateTransactionRequest.class))).thenReturn(sampleResponse());

        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("25.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/v1/transactions/{id}", id)
                        .header("Authorization", "Bearer token-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private TransactionResponse sampleResponse() {
        return TransactionResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("12.5000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
