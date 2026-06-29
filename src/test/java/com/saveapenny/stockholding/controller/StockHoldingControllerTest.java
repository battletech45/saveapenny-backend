package com.saveapenny.stockholding.controller;

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
import com.saveapenny.stockholding.dto.CreateHoldingRequest;
import com.saveapenny.stockholding.dto.HoldingResponse;
import com.saveapenny.stockholding.dto.HoldingSummaryResponse;
import com.saveapenny.stockholding.dto.UpdateHoldingRequest;
import com.saveapenny.stockholding.exception.DuplicateHoldingException;
import com.saveapenny.stockholding.exception.HoldingNotFoundException;
import com.saveapenny.stockholding.service.StockHoldingService;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockHoldingController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class StockHoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockHoldingService stockHoldingService;

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

    private void setupAuth(String token, UUID userId) {
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
    }

    private HoldingResponse sampleResponse() {
        return HoldingResponse.builder()
                .id(UUID.randomUUID())
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .notes("First position")
                .investedAmount(new BigDecimal("1400.00"))
                .currentPrice(new BigDecimal("175.42"))
                .currentValue(new BigDecimal("1754.20"))
                .profitLoss(new BigDecimal("354.20"))
                .profitLossPercent(new BigDecimal("25.30"))
                .latestTradingDay(LocalDate.of(2026, 6, 20))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        setupAuth("token-1", userId);
        when(stockHoldingService.create(eq(userId), any(CreateHoldingRequest.class))).thenReturn(sampleResponse());

        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.currentPrice").value(175.42))
                .andExpect(jsonPath("$.data.profitLossPercent").value(25.30));
    }

    @Test
    void create_returns401_withoutToken() throws Exception {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void create_returns409_whenDuplicate() throws Exception {
        UUID userId = UUID.randomUUID();
        setupAuth("token-dup", userId);
        when(stockHoldingService.create(eq(userId), any(CreateHoldingRequest.class)))
                .thenThrow(new DuplicateHoldingException("Already exists"));

        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer token-dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_STOCK_HOLDING"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        setupAuth("token-2", userId);
        when(stockHoldingService.getAll(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].symbol").value("IBM"));
    }

    @Test
    void getAll_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/holdings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getSummary_returnsPortfolioSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        setupAuth("token-3", userId);
        when(stockHoldingService.getSummary(userId)).thenReturn(
                HoldingSummaryResponse.builder()
                        .totalInvested(new BigDecimal("5000.00"))
                        .totalCurrentValue(new BigDecimal("5875.30"))
                        .totalProfitLoss(new BigDecimal("875.30"))
                        .totalProfitLossPercent(new BigDecimal("17.51"))
                        .holdingCount(3)
                        .holdings(List.of())
                        .build());

        mockMvc.perform(get("/api/v1/stocks/holdings/summary")
                        .header("Authorization", "Bearer token-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalInvested").value(5000.00))
                .andExpect(jsonPath("$.data.totalCurrentValue").value(5875.30))
                .andExpect(jsonPath("$.data.totalProfitLoss").value(875.30))
                .andExpect(jsonPath("$.data.totalProfitLossPercent").value(17.51))
                .andExpect(jsonPath("$.data.holdingCount").value(3));
    }

    @Test
    void getSummary_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/holdings/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getById_returnsHolding() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();
        setupAuth("token-4", userId);
        when(stockHoldingService.getById(userId, holdingId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer token-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.currentPrice").value(175.42));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();
        setupAuth("token-err", userId);
        when(stockHoldingService.getById(userId, holdingId))
                .thenThrow(new HoldingNotFoundException("Stock holding not found."));

        mockMvc.perform(get("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer token-err"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_HOLDING_NOT_FOUND"));
    }

    @Test
    void getById_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/holdings/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_returnsSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();
        setupAuth("token-5", userId);
        when(stockHoldingService.update(eq(userId), eq(holdingId), any(UpdateHoldingRequest.class)))
                .thenReturn(sampleResponse());

        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("15.00000000"))
                .notes("Increased position")
                .build();

        mockMvc.perform(put("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer token-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"));
    }

    @Test
    void update_returns404_whenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();
        setupAuth("token-err2", userId);
        when(stockHoldingService.update(eq(userId), eq(holdingId), any(UpdateHoldingRequest.class)))
                .thenThrow(new HoldingNotFoundException("Stock holding not found."));

        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("15.00000000"))
                .build();

        mockMvc.perform(put("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer token-err2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_HOLDING_NOT_FOUND"));
    }

    @Test
    void update_returns401_withoutToken() throws Exception {
        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("15.00000000"))
                .build();

        mockMvc.perform(put("/api/v1/stocks/holdings/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_returnsSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();
        setupAuth("token-6", userId);
        doNothing().when(stockHoldingService).delete(userId, holdingId);

        mockMvc.perform(delete("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer token-6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();
        setupAuth("token-err3", userId);
        org.mockito.Mockito.doThrow(new HoldingNotFoundException("Stock holding not found."))
                .when(stockHoldingService).delete(userId, holdingId);

        mockMvc.perform(delete("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer token-err3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_HOLDING_NOT_FOUND"));
    }

    @Test
    void delete_returns401_withoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/stocks/holdings/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
