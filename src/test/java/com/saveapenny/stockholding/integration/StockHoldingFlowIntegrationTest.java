package com.saveapenny.stockholding.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.service.StockService;
import com.saveapenny.test.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:holding-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "stock.enabled=true"
})
class StockHoldingFlowIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private StockService stockService;

    private String authToken;

    private StockQuoteResponse ibmQuote() {
        return StockQuoteResponse.builder()
                .symbol("IBM")
                .price(new BigDecimal("175.42"))
                .latestTradingDay(LocalDate.of(2026, 6, 20))
                .build();
    }

    private StockQuoteResponse aaplQuote() {
        return StockQuoteResponse.builder()
                .symbol("AAPL")
                .price(new BigDecimal("200.00"))
                .latestTradingDay(LocalDate.of(2026, 6, 20))
                .build();
    }

    private String holdingJson(String symbol, String qty, String price, String currency, String date) {
        return """
                {
                  "symbol": "%s",
                  "quantity": "%s",
                  "purchasePrice": "%s",
                  "currency": "%s",
                  "purchaseDate": "%s"
                }
                """.formatted(symbol, qty, price, currency, date);
    }

    @BeforeEach
    void ensureAuthToken() throws Exception {
        authToken = register("holding." + UUID.randomUUID() + "@example.com", "Holding Tester");
    }

    @AfterEach
    void tearDown() {
        org.mockito.Mockito.reset(stockService);
    }

    @Test
    void fullLifecycle_createGetUpdateDelete() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote("IBM")).thenReturn(ibmQuote());

        // Create
        var createResult = mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdingJson("IBM", "10.00000000", "140.0000", "USD", "2025-04-25")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.currentPrice").value(175.42))
                .andExpect(jsonPath("$.data.currentValue").value(1754.20))
                .andExpect(jsonPath("$.data.profitLoss").value(354.20))
                .andExpect(jsonPath("$.data.profitLossPercent").value(25.30))
                .andExpect(jsonPath("$.data.investedAmount").value(1400.00))
                .andReturn();

        String holdingId = extractField(createResult, "id");

        // Get by ID
        mockMvc.perform(get("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.symbol").value("IBM"));

        // Update quantity
        String updateBody = """
                {
                  "quantity": "15.00000000",
                  "notes": "Increased"
                }
                """;

        mockMvc.perform(put("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(15.00000000))
                .andExpect(jsonPath("$.data.notes").value("Increased"))
                .andExpect(jsonPath("$.data.currentValue").value(2631.30));

        // Delete
        mockMvc.perform(delete("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify deleted
        mockMvc.perform(get("/api/v1/stocks/holdings/{id}", holdingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_preventsDuplicate() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote("IBM")).thenReturn(ibmQuote());

        String body = holdingJson("IBM", "10.00000000", "140.0000", "USD", "2025-04-25");

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_STOCK_HOLDING"));
    }

    @Test
    void summary_aggregatesMultipleHoldings() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote("IBM")).thenReturn(ibmQuote());
        org.mockito.Mockito.when(stockService.getQuote("AAPL")).thenReturn(aaplQuote());

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdingJson("IBM", "10.00000000", "140.0000", "USD", "2025-04-25")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdingJson("AAPL", "5.00000000", "180.0000", "USD", "2025-05-10")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/stocks/holdings/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.holdingCount").value(2))
                .andExpect(jsonPath("$.data.totalInvested").value(2300.00))
                .andExpect(jsonPath("$.data.totalCurrentValue").value(2754.20))
                .andExpect(jsonPath("$.data.totalProfitLoss").value(454.20))
                .andExpect(jsonPath("$.data.totalProfitLossPercent").value(19.75));
    }

    @Test
    void getAll_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/holdings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void create_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdingJson("IBM", "10.00000000", "140.0000", "USD", "2025-04-25")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summary_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/holdings/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_allowsSameSymbolDifferentDate() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote("IBM")).thenReturn(ibmQuote());

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdingJson("IBM", "10.00000000", "140.0000", "USD", "2025-04-25")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/stocks/holdings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdingJson("IBM", "5.00000000", "150.0000", "USD", "2025-06-01")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/stocks/holdings/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holdingCount").value(2));
    }
}
