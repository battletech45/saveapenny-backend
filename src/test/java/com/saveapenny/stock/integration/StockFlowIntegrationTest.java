package com.saveapenny.stock.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.stock.dto.DailyPoint;
import com.saveapenny.stock.dto.StockDailySeriesResponse;
import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.exception.InvalidStockSymbolException;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import com.saveapenny.stock.exception.StockQuoteNotAvailableException;
import com.saveapenny.stock.exception.StockRateLimitExceededException;
import com.saveapenny.stock.service.StockService;
import com.saveapenny.test.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "stock.enabled=true"
})
class StockFlowIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private StockService stockService;

    private String authToken;

    @BeforeEach
    void ensureAuthToken() throws Exception {
        if (authToken == null) {
            authToken = register("stock.flow." + UUID.randomUUID() + "@example.com", "Stock Flow");
            grantPlusEntitlement(authToken);
        }
    }

    @Test
    void quote_returns200_forAuthenticatedUser() throws Exception {
        var response = StockQuoteResponse.builder()
                .symbol("IBM")
                .open(new BigDecimal("175.00"))
                .high(new BigDecimal("177.00"))
                .low(new BigDecimal("174.50"))
                .price(new BigDecimal("176.30"))
                .volume(5000000L)
                .latestTradingDay(LocalDate.of(2025, 6, 20))
                .previousClose(new BigDecimal("174.90"))
                .change(new BigDecimal("1.40"))
                .changePercent(new BigDecimal("0.8008"))
                .build();

        org.mockito.Mockito.when(stockService.getQuote("IBM")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.price").value(176.30))
                .andExpect(jsonPath("$.data.changePercent").value(0.8008))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void quote_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quote_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote(anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void quote_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote(anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void quote_returns429_whenRateLimited() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote(anyString()))
                .thenThrow(new StockRateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("STOCK_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void quote_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote(anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void daily_returns200_forAuthenticatedUser() throws Exception {
        var point = DailyPoint.builder()
                .date(LocalDate.of(2025, 6, 20))
                .open(new BigDecimal("175.00"))
                .high(new BigDecimal("177.00"))
                .low(new BigDecimal("174.50"))
                .close(new BigDecimal("176.30"))
                .volume(5000000L)
                .build();

        var response = StockDailySeriesResponse.builder()
                .symbol("IBM")
                .dataPoints(List.of(point))
                .build();

        org.mockito.Mockito.when(stockService.getDailySeries("IBM", "compact")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.dataPoints[0].date").value("2025-06-20"))
                .andExpect(jsonPath("$.data.dataPoints[0].close").value(176.30))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void daily_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void daily_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getDailySeries(anyString(), anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void daily_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getDailySeries(anyString(), anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void daily_returns429_whenRateLimited() throws Exception {
        org.mockito.Mockito.when(stockService.getDailySeries(anyString(), anyString()))
                .thenThrow(new StockRateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("STOCK_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void daily_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getDailySeries(anyString(), anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void news_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockNewsResponse.builder()
                .items(1)
                .articles(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getNewsSentiment("IBM")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").value(1))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void news_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void news_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getNewsSentiment(anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void news_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getNewsSentiment(anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void news_returns429_whenRateLimited() throws Exception {
        org.mockito.Mockito.when(stockService.getNewsSentiment(anyString()))
                .thenThrow(new StockRateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("STOCK_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void news_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getNewsSentiment(anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void news_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getNewsSentiment(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No news for symbol"));

        mockMvc.perform(get("/api/v1/stocks/news")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void overview_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockOverviewResponse.builder()
                .symbol("IBM")
                .name("IBM Corp")
                .build();

        org.mockito.Mockito.when(stockService.getCompanyOverview("IBM")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.name").value("IBM Corp"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void overview_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void overview_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getCompanyOverview(anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void overview_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getCompanyOverview(anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void overview_returns429_whenRateLimited() throws Exception {
        org.mockito.Mockito.when(stockService.getCompanyOverview(anyString()))
                .thenThrow(new StockRateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("STOCK_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void overview_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getCompanyOverview(anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void incomeStatement_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockIncomeStatementResponse.builder()
                .symbol("IBM")
                .annualReports(List.of())
                .quarterlyReports(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getIncomeStatement("IBM")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/income-statement")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void incomeStatement_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/income-statement")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incomeStatement_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getIncomeStatement(anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/income-statement")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void incomeStatement_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getIncomeStatement(anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/income-statement")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void incomeStatement_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getIncomeStatement(anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/income-statement")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void incomeStatement_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getIncomeStatement(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No income statement for symbol"));

        mockMvc.perform(get("/api/v1/stocks/income-statement")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void balanceSheet_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockBalanceSheetResponse.builder()
                .symbol("IBM")
                .annualReports(List.of())
                .quarterlyReports(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getBalanceSheet("IBM")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/balance-sheet")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void balanceSheet_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/balance-sheet")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void balanceSheet_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getBalanceSheet(anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/balance-sheet")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void balanceSheet_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getBalanceSheet(anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/balance-sheet")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void balanceSheet_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getBalanceSheet(anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/balance-sheet")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void balanceSheet_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getBalanceSheet(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No balance sheet for symbol"));

        mockMvc.perform(get("/api/v1/stocks/balance-sheet")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void cashFlow_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockCashFlowResponse.builder()
                .symbol("IBM")
                .annualReports(List.of())
                .quarterlyReports(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getCashFlow("IBM")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/cash-flow")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void cashFlow_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/cash-flow")
                        .param("symbol", "IBM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cashFlow_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getCashFlow(anyString()))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/cash-flow")
                        .param("symbol", "INVALID@")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void cashFlow_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getCashFlow(anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/cash-flow")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void cashFlow_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getCashFlow(anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/cash-flow")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void cashFlow_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getCashFlow(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No cash flow for symbol"));

        mockMvc.perform(get("/api/v1/stocks/cash-flow")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void sma_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockTechnicalIndicatorResponse.builder()
                .symbol("IBM")
                .indicator("SMA")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getSma("IBM", "daily", "20", "close")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.indicator").value("SMA"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void sma_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sma_returns400_forInvalidSymbol() throws Exception {
        org.mockito.Mockito.when(stockService.getSma("INVALID@", "daily", "20", "close"))
                .thenThrow(new InvalidStockSymbolException("Symbol contains invalid characters"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "INVALID@")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"));
    }

    @Test
    void sma_returns400_forInvalidInterval() throws Exception {
        org.mockito.Mockito.when(stockService.getSma("IBM", "invalid", "20", "close"))
                .thenThrow(new InvalidStockSymbolException("Interval must be 'daily', 'weekly', or 'monthly'"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("interval", "invalid")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void sma_returns400_forInvalidTimePeriod() throws Exception {
        org.mockito.Mockito.when(stockService.getSma("IBM", "daily", "abc", "close"))
                .thenThrow(new InvalidStockSymbolException("Time period must be a positive integer"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "abc")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void sma_returns400_forInvalidSeriesType() throws Exception {
        org.mockito.Mockito.when(stockService.getSma("IBM", "daily", "20", "invalid"))
                .thenThrow(new InvalidStockSymbolException("Series type must be 'close', 'open', 'high', or 'low'"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .param("seriesType", "invalid")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void sma_returns503_whenFeatureDisabled() throws Exception {
        org.mockito.Mockito.when(stockService.getSma(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("STOCK_DISABLED"));
    }

    @Test
    void sma_returns429_whenRateLimited() throws Exception {
        org.mockito.Mockito.when(stockService.getSma(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockRateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("STOCK_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void sma_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getSma(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void ema_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockTechnicalIndicatorResponse.builder()
                .symbol("IBM")
                .indicator("EMA")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getEma("IBM", "daily", "20", "close")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.indicator").value("EMA"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void ema_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ema_returns400_forInvalidInterval() throws Exception {
        org.mockito.Mockito.when(stockService.getEma("IBM", "invalid", "20", "close"))
                .thenThrow(new InvalidStockSymbolException("Interval must be 'daily', 'weekly', or 'monthly'"));

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("interval", "invalid")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void ema_returns400_forInvalidTimePeriod() throws Exception {
        org.mockito.Mockito.when(stockService.getEma("IBM", "daily", "0", "close"))
                .thenThrow(new InvalidStockSymbolException("Time period must be a positive integer"));

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("timePeriod", "0")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void ema_returns400_forInvalidSeriesType() throws Exception {
        org.mockito.Mockito.when(stockService.getEma("IBM", "daily", "20", "bad"))
                .thenThrow(new InvalidStockSymbolException("Series type must be 'close', 'open', 'high', or 'low'"));

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .param("seriesType", "bad")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void ema_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getEma(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void ema_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getEma(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No EMA for symbol"));

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "UNKNOWN")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void rsi_returns200_forAuthenticatedUser() throws Exception {
        var response = com.saveapenny.stock.dto.StockTechnicalIndicatorResponse.builder()
                .symbol("IBM")
                .indicator("RSI")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getRsi("IBM", "daily", "14", "close")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("timePeriod", "14")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"))
                .andExpect(jsonPath("$.data.indicator").value("RSI"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void rsi_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("timePeriod", "14"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rsi_returns400_forInvalidInterval() throws Exception {
        org.mockito.Mockito.when(stockService.getRsi("IBM", "invalid", "14", "close"))
                .thenThrow(new InvalidStockSymbolException("Interval must be 'daily', 'weekly', or 'monthly'"));

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("interval", "invalid")
                        .param("timePeriod", "14")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void rsi_returns400_forInvalidTimePeriod() throws Exception {
        org.mockito.Mockito.when(stockService.getRsi("IBM", "daily", "-1", "close"))
                .thenThrow(new InvalidStockSymbolException("Time period must be a positive integer"));

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("timePeriod", "-1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void rsi_returns400_forInvalidSeriesType() throws Exception {
        org.mockito.Mockito.when(stockService.getRsi("IBM", "daily", "14", "invalid"))
                .thenThrow(new InvalidStockSymbolException("Series type must be 'close', 'open', 'high', or 'low'"));

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("timePeriod", "14")
                        .param("seriesType", "invalid")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STOCK_SYMBOL"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void rsi_returns502_whenProviderFails() throws Exception {
        org.mockito.Mockito.when(stockService.getRsi(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockClientException("Provider error"));

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("timePeriod", "14")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("STOCK_PROVIDER_ERROR"));
    }

    @Test
    void rsi_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getRsi(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No RSI for symbol"));

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "UNKNOWN")
                        .param("timePeriod", "14")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void quote_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No quote data for symbol"));

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"));
    }

    @Test
    void daily_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getDailySeries(anyString(), anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No daily series for symbol"));

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"));
    }

    @Test
    void overview_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getCompanyOverview(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No overview for symbol"));

        mockMvc.perform(get("/api/v1/stocks/overview")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"));
    }

    @Test
    void sma_returns404_whenDataNotAvailable() throws Exception {
        org.mockito.Mockito.when(stockService.getSma(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No SMA for symbol"));

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "UNKNOWN")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"));
    }

    @Test
    void daily_defaultsOutputSizeToCompact() throws Exception {
        var response = StockDailySeriesResponse.builder()
                .symbol("IBM")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getDailySeries("IBM", "compact")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/daily")
                        .param("symbol", "IBM")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("IBM"));

        verify(stockService).getDailySeries("IBM", "compact");
    }

    @Test
    void sma_defaultsIntervalAndSeriesType() throws Exception {
        var response = com.saveapenny.stock.dto.StockTechnicalIndicatorResponse.builder()
                .symbol("IBM")
                .indicator("SMA")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getSma("IBM", "daily", "20", "close")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/sma")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.indicator").value("SMA"));

        verify(stockService).getSma("IBM", "daily", "20", "close");
    }

    @Test
    void ema_defaultsIntervalAndSeriesType() throws Exception {
        var response = com.saveapenny.stock.dto.StockTechnicalIndicatorResponse.builder()
                .symbol("IBM")
                .indicator("EMA")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getEma("IBM", "daily", "20", "close")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/ema")
                        .param("symbol", "IBM")
                        .param("timePeriod", "20")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.indicator").value("EMA"));

        verify(stockService).getEma("IBM", "daily", "20", "close");
    }

    @Test
    void rsi_defaultsIntervalAndSeriesType() throws Exception {
        var response = com.saveapenny.stock.dto.StockTechnicalIndicatorResponse.builder()
                .symbol("IBM")
                .indicator("RSI")
                .dataPoints(List.of())
                .build();

        org.mockito.Mockito.when(stockService.getRsi("IBM", "daily", "14", "close")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stocks/rsi")
                        .param("symbol", "IBM")
                        .param("timePeriod", "14")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.indicator").value("RSI"));

        verify(stockService).getRsi("IBM", "daily", "14", "close");
    }

    @Test
    void errorEnvelope_includesSuccessFalseAndErrorMessage() throws Exception {
        org.mockito.Mockito.when(stockService.getQuote(anyString()))
                .thenThrow(new StockQuoteNotAvailableException("No data"));

        mockMvc.perform(get("/api/v1/stocks/quote")
                        .param("symbol", "UNKNOWN")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STOCK_QUOTE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
