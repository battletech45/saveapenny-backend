package com.saveapenny.account.controller;

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
import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import com.saveapenny.account.entity.AccountType;
import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.service.AccountService;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
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

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

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
        AccountResponse response = sampleResponse();
        when(jwtService.isAccessTokenValid("token-1")).thenReturn(true);
        when(jwtService.extractUserId("token-1")).thenReturn(userId);
        when(accountService.create(eq(userId), any(CreateAccountRequest.class))).thenReturn(response);

        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .initialBalance(new BigDecimal("100.0000"))
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Wallet"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-2")).thenReturn(true);
        when(jwtService.extractUserId("token-2")).thenReturn(userId);
        when(accountService.getAll(eq(userId), any())).thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Wallet"));
    }

    @Test
    void update_returnsUpdatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountResponse response = sampleResponse();
        response.setName("Main Wallet");

        when(jwtService.isAccessTokenValid("token-3")).thenReturn(true);
        when(jwtService.extractUserId("token-3")).thenReturn(userId);
        when(accountService.update(eq(userId), eq(accountId), any(UpdateAccountRequest.class))).thenReturn(response);

        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .name("Main Wallet")
                .type(AccountType.BANK)
                .currency("EUR")
                .build();

        mockMvc.perform(put("/api/v1/accounts/{id}", accountId)
                        .header("Authorization", "Bearer token-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Main Wallet"));
    }

    @Test
    void delete_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-4")).thenReturn(true);
        when(jwtService.extractUserId("token-4")).thenReturn(userId);
        doNothing().when(accountService).delete(userId, accountId);

        mockMvc.perform(delete("/api/v1/accounts/{id}", accountId)
                        .header("Authorization", "Bearer token-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-err-1")).thenReturn(true);
        when(jwtService.extractUserId("token-err-1")).thenReturn(userId);
        when(accountService.getById(userId, accountId)).thenThrow(new AccountNotFoundException(accountId));

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .header("Authorization", "Bearer token-err-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void create_returnsConflict_whenNameAlreadyExists() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-err-2")).thenReturn(true);
        when(jwtService.extractUserId("token-err-2")).thenReturn(userId);
        when(accountService.create(eq(userId), any(CreateAccountRequest.class)))
                .thenThrow(new AccountNameAlreadyExistsException("Wallet"));

        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .initialBalance(new BigDecimal("0"))
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer token-err-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_ALREADY_EXISTS"));
    }

    private AccountResponse sampleResponse() {
        return AccountResponse.builder()
                .id(UUID.randomUUID())
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .balance(new BigDecimal("100.0000"))
                .initialBalance(new BigDecimal("100.0000"))
                .active(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
