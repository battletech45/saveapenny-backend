package com.saveapenny.stock.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.saveapenny.stock.config.StockProperties;
import com.saveapenny.stock.domain.BalanceSheetResponse;
import com.saveapenny.stock.domain.BalanceSheetResponse.BalanceSheetItem;
import com.saveapenny.stock.domain.CashFlowResponse;
import com.saveapenny.stock.domain.CashFlowResponse.CashFlowItem;
import com.saveapenny.stock.domain.CompanyOverview;
import com.saveapenny.stock.domain.DailyTimeSeriesResponse;
import com.saveapenny.stock.domain.DailyTimeSeriesResponse.MetaData;
import com.saveapenny.stock.domain.EmaResponse;
import com.saveapenny.stock.domain.GlobalQuote;
import com.saveapenny.stock.domain.GlobalQuoteResponse;
import com.saveapenny.stock.domain.IncomeStatementResponse;
import com.saveapenny.stock.domain.IncomeStatementResponse.IncomeReportItem;
import com.saveapenny.stock.domain.NewsArticle;
import com.saveapenny.stock.domain.NewsSentimentResponse;
import com.saveapenny.stock.domain.RsiResponse;
import com.saveapenny.stock.domain.SmaResponse;
import com.saveapenny.stock.domain.SmaResponse.SmaPoint;
import com.saveapenny.stock.domain.TechnicalIndicatorMetaData;
import com.saveapenny.stock.domain.TickerSentiment;
import com.saveapenny.stock.domain.TimeSeriesPoint;
import com.saveapenny.stock.dto.StockBalanceSheetResponse;
import com.saveapenny.stock.dto.StockCashFlowResponse;
import com.saveapenny.stock.dto.StockDailySeriesResponse;
import com.saveapenny.stock.dto.StockIncomeStatementResponse;
import com.saveapenny.stock.dto.StockNewsResponse;
import com.saveapenny.stock.dto.StockOverviewResponse;
import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.dto.StockTechnicalIndicatorResponse;
import com.saveapenny.stock.exception.InvalidStockSymbolException;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import com.saveapenny.stock.exception.StockQuoteNotAvailableException;
import com.saveapenny.stock.infrastructure.AlphaVantageClient;
import com.saveapenny.stock.service.impl.StockServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private AlphaVantageClient alphaVantageClient;

    private StockProperties enabledProperties;
    private StockProperties disabledProperties;
    private StockServiceImpl enabledService;
    private StockServiceImpl disabledService;

    @BeforeEach
    void setUp() {
        enabledProperties = new StockProperties(true, "demo-key", "https://www.alphavantage.co", 5, 25);
        disabledProperties = new StockProperties(false, "", "https://www.alphavantage.co", 5, 25);
        enabledService = new StockServiceImpl(alphaVantageClient, enabledProperties);
        disabledService = new StockServiceImpl(alphaVantageClient, disabledProperties);
    }

    @Test
    void throwsStockDisabledWhenFeatureNotEnabled() {
        assertThrows(StockDisabledException.class, () -> disabledService.getQuote("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getDailySeries("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getNewsSentiment("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getCompanyOverview("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getIncomeStatement("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getBalanceSheet("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getCashFlow("IBM"));
        assertThrows(StockDisabledException.class, () -> disabledService.getSma("IBM", "daily", "20", "close"));
        assertThrows(StockDisabledException.class, () -> disabledService.getEma("IBM", "daily", "20", "close"));
        assertThrows(StockDisabledException.class, () -> disabledService.getRsi("IBM", "daily", "14", "close"));
    }

    @Test
    void normalizesAndValidatesSymbol() {
        when(alphaVantageClient.fetchQuote("IBM")).thenReturn(
                new GlobalQuoteResponse(new GlobalQuote("IBM", null, null, null, null, null, null, null, null, null)));

        enabledService.getQuote("  ibm  ");
    }

    @Test
    void throwsOnBlankSymbol() {
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getQuote(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getQuote("   "));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getQuote(null));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getDailySeries(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getDailySeries((String) null));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getNewsSentiment(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getCompanyOverview(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getIncomeStatement(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getBalanceSheet(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getCashFlow(""));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getSma("", "daily", "20", "close"));
    }

    @Test
    void throwsOnSymbolTooLong() {
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getQuote("ABCDEFGHIJK"));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getDailySeries("ABCDEFGHIJK"));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getNewsSentiment("ABCDEFGHIJK"));
    }

    @Test
    void throwsOnSymbolWithInvalidCharacters() {
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getQuote("IBM@"));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getDailySeries("GOOG/L"));
        assertThrows(InvalidStockSymbolException.class, () -> enabledService.getNewsSentiment("IBM@"));
    }

    @Test
    void mapsProviderQuoteToApiResponse() {
        var providerQuote = new GlobalQuote("IBM", "175.0000", "177.0000", "174.5000", "176.3000",
                "5000000", "2025-06-20", "174.9000", "1.4000", "0.8008%");

        when(alphaVantageClient.fetchQuote("IBM")).thenReturn(new GlobalQuoteResponse(providerQuote));

        StockQuoteResponse result = enabledService.getQuote("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertEquals(new BigDecimal("175.0000"), result.getOpen());
        assertEquals(new BigDecimal("177.0000"), result.getHigh());
        assertEquals(new BigDecimal("174.5000"), result.getLow());
        assertEquals(new BigDecimal("176.3000"), result.getPrice());
        assertEquals(5000000L, result.getVolume());
        assertEquals(LocalDate.of(2025, 6, 20), result.getLatestTradingDay());
        assertEquals(new BigDecimal("174.9000"), result.getPreviousClose());
        assertEquals(new BigDecimal("1.4000"), result.getChange());
        assertEquals(new BigDecimal("0.8008"), result.getChangePercent());
    }

    @Test
    void mapsNullFieldsToNull() {
        var providerQuote = new GlobalQuote("IBM", null, null, null, null, null, null, null, null, null);

        when(alphaVantageClient.fetchQuote("IBM")).thenReturn(new GlobalQuoteResponse(providerQuote));

        StockQuoteResponse result = enabledService.getQuote("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNull(result.getOpen());
        assertNull(result.getPrice());
        assertNull(result.getVolume());
        assertNull(result.getLatestTradingDay());
    }

    @Test
    void throwsStockClientException_whenQuoteHasProviderError() {
        when(alphaVantageClient.fetchQuote("IBM"))
                .thenReturn(new GlobalQuoteResponse(null, "Invalid API call.", null));

        StockClientException exception = assertThrows(StockClientException.class, () -> enabledService.getQuote("IBM"));

        assertTrue(exception.getMessage().contains("quote"));
        assertTrue(exception.getMessage().contains("IBM"));
        assertTrue(exception.getMessage().contains("Invalid API call."));
    }

    @Test
    void throwsStockClientException_whenQuoteHasProviderNote() {
        when(alphaVantageClient.fetchQuote("IBM"))
                .thenReturn(new GlobalQuoteResponse(null, null,
                        "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 500 calls per day."));

        StockClientException exception = assertThrows(StockClientException.class, () -> enabledService.getQuote("IBM"));

        assertTrue(exception.getMessage().contains("note"));
        assertTrue(exception.getMessage().contains("IBM"));
    }

    @Test
    void throwsStockQuoteNotAvailableException_whenQuoteHasNoData() {
        when(alphaVantageClient.fetchQuote("IBM"))
                .thenReturn(new GlobalQuoteResponse(null, null, null));

        StockQuoteNotAvailableException exception = assertThrows(
                StockQuoteNotAvailableException.class,
                () -> enabledService.getQuote("IBM"));

        assertTrue(exception.getMessage().contains("quote data"));
        assertTrue(exception.getMessage().contains("IBM"));
    }

    @Test
    void mapsQuoteWithMalformedNumericsToNull() {
        var providerQuote = new GlobalQuote("IBM", "N/A", "N/A", "N/A", "N/A",
                "N/A", "N/A", "N/A", "N/A", "N/A");

        when(alphaVantageClient.fetchQuote("IBM")).thenReturn(new GlobalQuoteResponse(providerQuote));

        StockQuoteResponse result = enabledService.getQuote("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNull(result.getOpen());
        assertNull(result.getPrice());
        assertNull(result.getVolume());
        assertNull(result.getLatestTradingDay());
    }

    @Test
    void mapsDailySeriesToApiResponse() {
        Map<String, TimeSeriesPoint> timeSeries = new LinkedHashMap<>();
        timeSeries.put("2025-06-20", new TimeSeriesPoint("175.0000", "177.0000", "174.5000", "176.3000", "5000000"));
        timeSeries.put("2025-06-19", new TimeSeriesPoint("174.5000", "176.0000", "173.8000", "175.2000", "4500000"));

        var providerResponse = new DailyTimeSeriesResponse(
                new MetaData("Daily Prices", "IBM", "2025-06-20", "Compact", "US/Eastern"),
                timeSeries);

        when(alphaVantageClient.fetchDailySeries("IBM", "compact")).thenReturn(providerResponse);

        StockDailySeriesResponse result = enabledService.getDailySeries("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNotNull(result.getDataPoints());
        assertEquals(2, result.getDataPoints().size());

        var first = result.getDataPoints().get(0);
        assertEquals(LocalDate.of(2025, 6, 20), first.getDate());
        assertEquals(new BigDecimal("175.0000"), first.getOpen());
    }

    @Test
    void mapsDailySeries_withEmptyTimeSeries() {
        var providerResponse = new DailyTimeSeriesResponse(
                new MetaData("Daily Prices", "IBM", "2025-06-20", "Compact", "US/Eastern"),
                Map.of());

        when(alphaVantageClient.fetchDailySeries("IBM", "compact")).thenReturn(providerResponse);

        StockDailySeriesResponse result = enabledService.getDailySeries("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNotNull(result.getDataPoints());
        assertTrue(result.getDataPoints().isEmpty());
    }

    @Test
    void throwsOnDailySeriesWithNullMetaData() {
        var providerResponse = new DailyTimeSeriesResponse(null, Map.of());

        when(alphaVantageClient.fetchDailySeries("IBM", "compact")).thenReturn(providerResponse);

        assertThrows(StockQuoteNotAvailableException.class, () -> enabledService.getDailySeries("IBM"));
    }

    @Test
    void throwsOnInvalidOutputSize() {
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getDailySeries("IBM", "invalid"));
    }

    @Test
    void defaultsToCompactWhenOutputSizeIsNull() {
        Map<String, TimeSeriesPoint> timeSeries = new LinkedHashMap<>();
        timeSeries.put("2025-06-20", new TimeSeriesPoint("175.0000", "177.0000", "174.5000", "176.3000", "5000000"));

        var providerResponse = new DailyTimeSeriesResponse(
                new MetaData("Daily Prices", "IBM", "2025-06-20", "Compact", "US/Eastern"),
                timeSeries);

        when(alphaVantageClient.fetchDailySeries("IBM", "compact")).thenReturn(providerResponse);

        StockDailySeriesResponse result = enabledService.getDailySeries("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertEquals(1, result.getDataPoints().size());
    }

    @Test
    void throwsStockClientException_whenDailySeriesHasProviderError() {
        when(alphaVantageClient.fetchDailySeries("IBM", "compact"))
                .thenReturn(new DailyTimeSeriesResponse(null, null, "Invalid API call.", null));

        assertThrows(StockClientException.class, () -> enabledService.getDailySeries("IBM"));
    }

    @Test
    void throwsStockClientException_whenDailySeriesHasProviderNote() {
        when(alphaVantageClient.fetchDailySeries("IBM", "compact"))
                .thenReturn(new DailyTimeSeriesResponse(null, null, null,
                        "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 500 calls per day."));

        assertThrows(StockClientException.class, () -> enabledService.getDailySeries("IBM"));
    }

    @Test
    void mapsDailySeriesWithMalformedNumericsToNull() {
        Map<String, TimeSeriesPoint> timeSeries = new LinkedHashMap<>();
        timeSeries.put("2025-06-20", new TimeSeriesPoint("N/A", null, null, "N/A", "not-a-number"));

        var providerResponse = new DailyTimeSeriesResponse(
                new MetaData("Daily Prices", "IBM", "2025-06-20", "Compact", "US/Eastern"),
                timeSeries);

        when(alphaVantageClient.fetchDailySeries("IBM", "compact")).thenReturn(providerResponse);

        StockDailySeriesResponse result = enabledService.getDailySeries("IBM");

        assertNotNull(result);
        assertEquals(1, result.getDataPoints().size());
        var point = result.getDataPoints().get(0);
        assertNull(point.getOpen());
        assertNull(point.getClose());
        assertNull(point.getVolume());
    }

    @Test
    void mapsNewsSentimentToApiResponse() {
        var article = new NewsArticle("Title", "https://url", "20250120T100000", "Summary",
                "Source", new BigDecimal("0.5"), "Bullish",
                List.of(new TickerSentiment("IBM", "0.9", new BigDecimal("0.5"), "Bullish")));
        var response = new NewsSentimentResponse("1", List.of(article));

        when(alphaVantageClient.fetchNewsSentiment("IBM")).thenReturn(response);

        StockNewsResponse result = enabledService.getNewsSentiment("IBM");

        assertNotNull(result);
        assertEquals(1, result.getItems());
        assertNotNull(result.getArticles());
        assertEquals(1, result.getArticles().size());

        var dto = result.getArticles().get(0);
        assertEquals("Title", dto.getTitle());
        assertEquals("Source", dto.getSource());
        assertEquals(new BigDecimal("0.5"), dto.getOverallSentimentScore());
        assertEquals("Bullish", dto.getOverallSentimentLabel());
        assertNotNull(dto.getTickerSentiment());
        assertEquals(1, dto.getTickerSentiment().size());
        assertEquals("IBM", dto.getTickerSentiment().get(0).getTicker());
    }

    @Test
    void mapsNewsSentiment_withEmptyFeed() {
        when(alphaVantageClient.fetchNewsSentiment("IBM")).thenReturn(new NewsSentimentResponse("0", List.of()));

        StockNewsResponse result = enabledService.getNewsSentiment("IBM");

        assertNotNull(result);
        assertEquals(0, result.getItems());
        assertNotNull(result.getArticles());
        assertTrue(result.getArticles().isEmpty());
    }

    @Test
    void throwsStockClientException_whenNewsHasProviderError() {
        when(alphaVantageClient.fetchNewsSentiment("IBM"))
                .thenReturn(new NewsSentimentResponse(null, null, "Invalid API call.", null));

        StockClientException exception = assertThrows(
                StockClientException.class,
                () -> enabledService.getNewsSentiment("IBM"));

        assertTrue(exception.getMessage().contains("news sentiment"));
        assertTrue(exception.getMessage().contains("IBM"));
    }

    @Test
    void throwsStockClientException_whenNewsHasProviderNote() {
        when(alphaVantageClient.fetchNewsSentiment("IBM"))
                .thenReturn(new NewsSentimentResponse(null, null, null,
                        "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 500 calls per day."));

        assertThrows(StockClientException.class, () -> enabledService.getNewsSentiment("IBM"));
    }

    @Test
    void throwsStockQuoteNotAvailableException_whenNewsHasNullFeed() {
        when(alphaVantageClient.fetchNewsSentiment("IBM"))
                .thenReturn(new NewsSentimentResponse("0", null, null, null));

        StockQuoteNotAvailableException exception = assertThrows(
                StockQuoteNotAvailableException.class,
                () -> enabledService.getNewsSentiment("IBM"));

        assertTrue(exception.getMessage().contains("news sentiment data"));
        assertTrue(exception.getMessage().contains("IBM"));
    }

    @Test
    void mapsNewsSentimentWithEmptyTickerSentiment() {
        var article = new NewsArticle("Title", "https://url", "20250120T100000", "Summary",
                "Source", new BigDecimal("0.5"), "Bullish", null);
        var response = new NewsSentimentResponse("1", List.of(article));

        when(alphaVantageClient.fetchNewsSentiment("IBM")).thenReturn(response);

        StockNewsResponse result = enabledService.getNewsSentiment("IBM");

        assertNotNull(result);
        assertEquals(1, result.getItems());
        assertNotNull(result.getArticles());
        assertEquals(1, result.getArticles().size());
        assertNotNull(result.getArticles().get(0).getTickerSentiment());
        assertTrue(result.getArticles().get(0).getTickerSentiment().isEmpty());
    }

    @Test
    void mapsCompanyOverviewToApiResponse() {
        var overview = new CompanyOverview("IBM", "Common Stock", "IBM Corp", "Description",
                "123", "NYSE", "USD", "USA", "Technology", "Services", "Address",
                "Dec", "2025-03-31", "200000000000", "18000000000", "25.5", "2.0",
                "10.00", "1.50", "0.035", "8.50", "100.00", "0.12", "0.15",
                "0.08", "0.15", "60000000000", "35000000000", "8.50", "0.05",
                "0.03", "200.00", "5", "10", "15", "2", "1",
                "22.0", "20.0", "3.0", "4.0", "2.5", "12.0",
                "0.85", "200.00", "150.00", "180.00", "175.00",
                "900000000", "2025-06-01", "2025-05-15");

        when(alphaVantageClient.fetchCompanyOverview("IBM")).thenReturn(overview);

        StockOverviewResponse result = enabledService.getCompanyOverview("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertEquals("IBM Corp", result.getName());
        assertEquals("NYSE", result.getExchange());
        assertEquals(new BigDecimal("200000000000"), result.getMarketCapitalization());
        assertEquals(new BigDecimal("25.5"), result.getPeRatio());
        assertEquals(new BigDecimal("0.035"), result.getDividendYield());
        assertEquals(new BigDecimal("8.50"), result.getEps());
        assertEquals(new BigDecimal("0.85"), result.getBeta());
        assertEquals(new BigDecimal("200.00"), result.getWeekHigh52());
        assertEquals(new BigDecimal("150.00"), result.getWeekLow52());
        assertEquals(900000000L, result.getSharesOutstanding());
    }

    @Test
    void throwsOnCompanyOverviewWithNullSymbol() {
        when(alphaVantageClient.fetchCompanyOverview("IBM"))
                .thenReturn(new CompanyOverview(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThrows(StockQuoteNotAvailableException.class, () -> enabledService.getCompanyOverview("IBM"));
    }

    @Test
    void throwsStockClientException_whenOverviewHasProviderError() {
        when(alphaVantageClient.fetchCompanyOverview("IBM"))
                .thenReturn(new CompanyOverview(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        "Invalid API call.", null));

        StockClientException exception = assertThrows(
                StockClientException.class,
                () -> enabledService.getCompanyOverview("IBM"));

        assertTrue(exception.getMessage().contains("company overview"));
        assertTrue(exception.getMessage().contains("IBM"));
    }

    @Test
    void mapsIncomeStatementToApiResponse() {
        var annual = new IncomeReportItem("2024-12-31", "USD", "35000000000", "60000000000",
                "25000000000", "20000000000", "15000000000", "5000000000",
                "10000000000", "20000000000", "1000000000", "500000000",
                "200000000", "300000000", "100000000", "500000000",
                "2000000000", "3000000000", "12000000000", "3000000000",
                "200000000", "8000000000", "8000000000", "12000000000", "18000000000", "8000000000");
        var response = new IncomeStatementResponse("IBM", List.of(annual), List.of());

        when(alphaVantageClient.fetchIncomeStatement("IBM")).thenReturn(response);

        StockIncomeStatementResponse result = enabledService.getIncomeStatement("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNotNull(result.getAnnualReports());
        assertEquals(1, result.getAnnualReports().size());

        var item = result.getAnnualReports().get(0);
        assertEquals("2024-12-31", item.getFiscalDateEnding());
        assertEquals("USD", item.getReportedCurrency());
        assertTrue(item.getFields().containsKey("totalRevenue"));
        assertEquals("60000000000", item.getFields().get("totalRevenue"));
        assertTrue(item.getFields().containsKey("netIncome"));
        assertEquals("8000000000", item.getFields().get("netIncome"));
    }

    @Test
    void mapsIncomeStatement_withEmptyReports() {
        when(alphaVantageClient.fetchIncomeStatement("IBM"))
                .thenReturn(new IncomeStatementResponse("IBM", List.of(), List.of()));

        StockIncomeStatementResponse result = enabledService.getIncomeStatement("IBM");

        assertNotNull(result);
        assertNotNull(result.getAnnualReports());
        assertTrue(result.getAnnualReports().isEmpty());
    }

    @Test
    void throwsOnIncomeStatementWithNullSymbol() {
        when(alphaVantageClient.fetchIncomeStatement("IBM"))
                .thenReturn(new IncomeStatementResponse(null, null, null));

        assertThrows(StockQuoteNotAvailableException.class, () -> enabledService.getIncomeStatement("IBM"));
    }

    @Test
    void throwsStockClientException_whenIncomeStatementHasProviderError() {
        when(alphaVantageClient.fetchIncomeStatement("IBM"))
                .thenReturn(new IncomeStatementResponse(null, null, null, "Invalid API call.", null));

        assertThrows(StockClientException.class, () -> enabledService.getIncomeStatement("IBM"));
    }

    @Test
    void extractsFinancialReportItemWithSparseFields() {
        var annual = new IncomeReportItem("2024-12-31", "USD", null, "60000000000",
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null);
        var response = new IncomeStatementResponse("IBM", List.of(annual), List.of());

        when(alphaVantageClient.fetchIncomeStatement("IBM")).thenReturn(response);

        StockIncomeStatementResponse result = enabledService.getIncomeStatement("IBM");

        assertNotNull(result);
        assertEquals(1, result.getAnnualReports().size());
        var item = result.getAnnualReports().get(0);
        assertEquals("2024-12-31", item.getFiscalDateEnding());
        assertEquals("USD", item.getReportedCurrency());
        assertTrue(item.getFields().containsKey("totalRevenue"));
        assertEquals("60000000000", item.getFields().get("totalRevenue"));
        assertFalse(item.getFields().containsKey("grossProfit"));
        assertFalse(item.getFields().containsKey("netIncome"));
    }

    @Test
    void mapsBalanceSheetToApiResponse() {
        var annual = new BalanceSheetItem("2024-12-31", "USD", "150000000000", "80000000000",
                "20000000000", "30000000000", "10000000000", "15000000000",
                "70000000000", "30000000000", "10000000000", "20000000000",
                "10000000000", "5000000000", "10000000000", "5000000000",
                "3000000000", "5000000000", "20000000000", "100000000000",
                "40000000000", "15000000000", "5000000000", "10000000000",
                "5000000000", "60000000000", "1000000000", "30000000000",
                "5000000000", "25000000000", "15000000000", "5000000000",
                "10000000000", "50000000000", "1000000000", "30000000000",
                "5000000000", "1000000000");

        when(alphaVantageClient.fetchBalanceSheet("IBM"))
                .thenReturn(new BalanceSheetResponse("IBM", List.of(annual), List.of()));

        StockBalanceSheetResponse result = enabledService.getBalanceSheet("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNotNull(result.getAnnualReports());
        assertEquals(1, result.getAnnualReports().size());

        var item = result.getAnnualReports().get(0);
        assertEquals("2024-12-31", item.getFiscalDateEnding());
        assertTrue(item.getFields().containsKey("totalAssets"));
        assertEquals("150000000000", item.getFields().get("totalAssets"));
    }

    @Test
    void throwsStockClientException_whenBalanceSheetHasProviderError() {
        when(alphaVantageClient.fetchBalanceSheet("IBM"))
                .thenReturn(new BalanceSheetResponse(null, null, null, "Invalid API call.", null));

        assertThrows(StockClientException.class, () -> enabledService.getBalanceSheet("IBM"));
    }

    @Test
    void mapsCashFlowToApiResponse() {
        var annual = new CashFlowItem("2024-12-31", "USD", "15000000000", "2000000000",
                "1000000000", "500000000", "300000000", "3000000000",
                "5000000000", "2000000000", "1000000000", "8000000000",
                "4000000000", "6000000000", "3000000000",
                "2000000000", "1000000000", "500000000",
                "2000000000", "1000000000", "500000000",
                "1000000000", "500000000",
                "1000000000", "500000000",
                "500000000", "100000000", "50000000", "8000000000");

        when(alphaVantageClient.fetchCashFlow("IBM"))
                .thenReturn(new CashFlowResponse("IBM", List.of(annual), List.of()));

        StockCashFlowResponse result = enabledService.getCashFlow("IBM");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertNotNull(result.getAnnualReports());
        assertEquals(1, result.getAnnualReports().size());

        var item = result.getAnnualReports().get(0);
        assertEquals("2024-12-31", item.getFiscalDateEnding());
        assertTrue(item.getFields().containsKey("operatingCashflow"));
        assertEquals("15000000000", item.getFields().get("operatingCashflow"));
    }

    @Test
    void throwsStockClientException_whenCashFlowHasProviderError() {
        when(alphaVantageClient.fetchCashFlow("IBM"))
                .thenReturn(new CashFlowResponse(null, null, null, "Invalid API call.", null));

        assertThrows(StockClientException.class, () -> enabledService.getCashFlow("IBM"));
    }

    @Test
    void mapsSmaToApiResponse() {
        Map<String, SmaPoint> dataPoints = new LinkedHashMap<>();
        dataPoints.put("2025-06-20", new SmaPoint("178.4567"));
        dataPoints.put("2025-06-19", new SmaPoint("177.8901"));

        var metaData = new TechnicalIndicatorMetaData("IBM", "SMA", "2025-06-20", "daily", "20", "close", "US/Eastern");
        var response = new SmaResponse(metaData, dataPoints);

        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close")).thenReturn(response);

        StockTechnicalIndicatorResponse result = enabledService.getSma("IBM", "daily", "20", "close");

        assertNotNull(result);
        assertEquals("IBM", result.getSymbol());
        assertEquals("SMA", result.getIndicator());
        assertNotNull(result.getDataPoints());
        assertEquals(2, result.getDataPoints().size());

        var first = result.getDataPoints().get(0);
        assertEquals(LocalDate.of(2025, 6, 20), first.getDate());
        assertEquals(new BigDecimal("178.4567"), first.getValue());

        var second = result.getDataPoints().get(1);
        assertEquals(LocalDate.of(2025, 6, 19), second.getDate());
        assertEquals(new BigDecimal("177.8901"), second.getValue());
    }

    @Test
    void mapsSma_withEmptyDataPoints() {
        var metaData = new TechnicalIndicatorMetaData("IBM", "SMA", "2025-06-20", "daily", "20", "close", "US/Eastern");
        var response = new SmaResponse(metaData, Map.of());

        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close")).thenReturn(response);

        StockTechnicalIndicatorResponse result = enabledService.getSma("IBM", "daily", "20", "close");

        assertNotNull(result);
        assertNotNull(result.getDataPoints());
        assertTrue(result.getDataPoints().isEmpty());
    }

    @Test
    void throwsOnSmaWithNullMetaData() {
        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close"))
                .thenReturn(new SmaResponse(null, Map.of()));

        assertThrows(StockQuoteNotAvailableException.class,
                () -> enabledService.getSma("IBM", "daily", "20", "close"));
    }

    @Test
    void throwsStockClientException_whenSmaHasProviderError() {
        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close"))
                .thenReturn(new SmaResponse(null, null, "Invalid API call.", null));

        StockClientException exception = assertThrows(
                StockClientException.class,
                () -> enabledService.getSma("IBM", "daily", "20", "close"));

        assertTrue(exception.getMessage().contains("SMA"));
        assertTrue(exception.getMessage().contains("IBM"));
    }

    @Test
    void mapsTechnicalIndicatorWithMalformedNumericToNullValue() {
        Map<String, SmaPoint> dataPoints = new LinkedHashMap<>();
        dataPoints.put("2025-06-20", new SmaPoint("N/A"));
        dataPoints.put("2025-06-19", new SmaPoint(null));

        var metaData = new TechnicalIndicatorMetaData("IBM", "SMA", "2025-06-20", "daily", "20", "close", "US/Eastern");
        var response = new SmaResponse(metaData, dataPoints);

        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close")).thenReturn(response);

        StockTechnicalIndicatorResponse result = enabledService.getSma("IBM", "daily", "20", "close");

        assertNotNull(result);
        assertEquals(2, result.getDataPoints().size());
        assertNull(result.getDataPoints().get(0).getValue());
        assertNull(result.getDataPoints().get(1).getValue());
    }

    @Test
    void mapsEmaToApiResponse() {
        Map<String, EmaResponse.EmaPoint> dataPoints = new LinkedHashMap<>();
        dataPoints.put("2025-06-20", new EmaResponse.EmaPoint("179.1234"));

        var metaData = new TechnicalIndicatorMetaData("IBM", "EMA", "2025-06-20", "daily", "20", "close", "US/Eastern");
        var response = new EmaResponse(metaData, dataPoints);

        when(alphaVantageClient.fetchEma("IBM", "daily", "20", "close")).thenReturn(response);

        StockTechnicalIndicatorResponse result = enabledService.getEma("IBM", "daily", "20", "close");

        assertNotNull(result);
        assertEquals("EMA", result.getIndicator());
        assertEquals(1, result.getDataPoints().size());
        assertEquals(new BigDecimal("179.1234"), result.getDataPoints().get(0).getValue());
    }

    @Test
    void throwsStockClientException_whenEmaHasProviderError() {
        when(alphaVantageClient.fetchEma("IBM", "daily", "20", "close"))
                .thenReturn(new EmaResponse(null, null, "Invalid API call.", null));

        assertThrows(StockClientException.class,
                () -> enabledService.getEma("IBM", "daily", "20", "close"));
    }

    @Test
    void mapsRsiToApiResponse() {
        Map<String, RsiResponse.RsiPoint> dataPoints = new LinkedHashMap<>();
        dataPoints.put("2025-06-20", new RsiResponse.RsiPoint("55.4321"));

        var metaData = new TechnicalIndicatorMetaData("IBM", "RSI", "2025-06-20", "daily", "14", "close", "US/Eastern");
        var response = new RsiResponse(metaData, dataPoints);

        when(alphaVantageClient.fetchRsi("IBM", "daily", "14", "close")).thenReturn(response);

        StockTechnicalIndicatorResponse result = enabledService.getRsi("IBM", "daily", "14", "close");

        assertNotNull(result);
        assertEquals("RSI", result.getIndicator());
        assertEquals(1, result.getDataPoints().size());
        assertEquals(new BigDecimal("55.4321"), result.getDataPoints().get(0).getValue());
    }

    @Test
    void throwsStockClientException_whenRsiHasProviderError() {
        when(alphaVantageClient.fetchRsi("IBM", "daily", "14", "close"))
                .thenReturn(new RsiResponse(null, null, "Invalid API call.", null));

        assertThrows(StockClientException.class,
                () -> enabledService.getRsi("IBM", "daily", "14", "close"));
    }

    @Test
    void validatesTechnicalIndicatorParams() {
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "invalid", "20", "close"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "daily", "abc", "close"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "daily", "20", "invalid"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getEma("IBM", "invalid", "20", "close"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getRsi("IBM", "invalid", "14", "close"));
    }

    @Test
    void rejectsTimePeriodZero() {
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "daily", "0", "close"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getEma("IBM", "daily", "00", "close"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getRsi("IBM", "daily", "0", "close"));
    }

    @Test
    void rejectsNegativeTimePeriod() {
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "daily", "-1", "close"));
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "daily", "-5", "close"));
    }

    @Test
    void rejectsWhitespaceOnlyTimePeriod() {
        assertThrows(InvalidStockSymbolException.class,
                () -> enabledService.getSma("IBM", "daily", "  ", "close"));
    }

    @Test
    void acceptsNormalizedIntervalAndSeriesType() {
        var smaMetaData = new TechnicalIndicatorMetaData("IBM", "SMA", "2025-06-20", "daily", "20", "close", "US/Eastern");

        when(alphaVantageClient.fetchSma("IBM", "weekly", "20", "close")).thenReturn(new SmaResponse(smaMetaData, Map.of()));
        when(alphaVantageClient.fetchSma("IBM", "monthly", "20", "open")).thenReturn(new SmaResponse(smaMetaData, Map.of()));
        when(alphaVantageClient.fetchEma("IBM", "daily", "20", "low")).thenReturn(
                new EmaResponse(new TechnicalIndicatorMetaData("IBM", "EMA", "2025-06-20", "daily", "20", "low", "US/Eastern"), Map.of()));
        when(alphaVantageClient.fetchRsi("IBM", "daily", "14", "high")).thenReturn(
                new RsiResponse(new TechnicalIndicatorMetaData("IBM", "RSI", "2025-06-20", "daily", "14", "high", "US/Eastern"), Map.of()));

        assertDoesNotThrow(() -> enabledService.getSma("IBM", "Weekly", "20", "Close"));
        assertDoesNotThrow(() -> enabledService.getSma("IBM", "MONTHLY", "20", "OPEN"));
        assertDoesNotThrow(() -> enabledService.getEma("IBM", "daily", "20", "Low"));
        assertDoesNotThrow(() -> enabledService.getRsi("IBM", "Daily", "14", "HIGH"));
    }

    @Test
    void defaultsIntervalAndSeriesType() {
        var metaData = new TechnicalIndicatorMetaData("IBM", "SMA", "2025-06-20", "daily", "20", "close", "US/Eastern");
        var response = new SmaResponse(metaData, Map.of());

        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close")).thenReturn(response);

        enabledService.getSma("IBM", null, "20", null);
        enabledService.getSma("IBM", "", "20", "");
    }

    @Test
    void normalizesSymbolForAllPhaseThreeEndpoints() {
        when(alphaVantageClient.fetchNewsSentiment("IBM")).thenReturn(new NewsSentimentResponse("0", List.of()));
        when(alphaVantageClient.fetchCompanyOverview("IBM")).thenReturn(
                new CompanyOverview("IBM", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        when(alphaVantageClient.fetchIncomeStatement("IBM")).thenReturn(new IncomeStatementResponse("IBM", List.of(), List.of()));
        when(alphaVantageClient.fetchBalanceSheet("IBM")).thenReturn(new BalanceSheetResponse("IBM", List.of(), List.of()));
        when(alphaVantageClient.fetchCashFlow("IBM")).thenReturn(new CashFlowResponse("IBM", List.of(), List.of()));
        when(alphaVantageClient.fetchSma("IBM", "daily", "20", "close")).thenReturn(
                new SmaResponse(new TechnicalIndicatorMetaData("IBM", "SMA", null, "daily", "20", "close", null), Map.of()));
        when(alphaVantageClient.fetchEma("IBM", "daily", "20", "close")).thenReturn(
                new EmaResponse(new TechnicalIndicatorMetaData("IBM", "EMA", null, "daily", "20", "close", null), Map.of()));
        when(alphaVantageClient.fetchRsi("IBM", "daily", "14", "close")).thenReturn(
                new RsiResponse(new TechnicalIndicatorMetaData("IBM", "RSI", null, "daily", "14", "close", null), Map.of()));

        assertDoesNotThrow(() -> enabledService.getNewsSentiment("  ibm  "));
        assertDoesNotThrow(() -> enabledService.getCompanyOverview("  ibm  "));
        assertDoesNotThrow(() -> enabledService.getIncomeStatement("  ibm  "));
        assertDoesNotThrow(() -> enabledService.getBalanceSheet("  ibm  "));
        assertDoesNotThrow(() -> enabledService.getCashFlow("  ibm  "));
        assertDoesNotThrow(() -> enabledService.getSma("  ibm  ", "daily", "20", "close"));
        assertDoesNotThrow(() -> enabledService.getEma("  ibm  ", "daily", "20", "close"));
        assertDoesNotThrow(() -> enabledService.getRsi("  ibm  ", "daily", "14", "close"));
    }
}
