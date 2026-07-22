package com.saveapenny.stock.infrastructure;

import com.saveapenny.stock.config.StockProperties;
import com.saveapenny.stock.domain.BalanceSheetResponse;
import com.saveapenny.stock.domain.CashFlowResponse;
import com.saveapenny.stock.domain.CompanyOverview;
import com.saveapenny.stock.domain.DailyTimeSeriesResponse;
import com.saveapenny.stock.domain.EmaResponse;
import com.saveapenny.stock.domain.GlobalQuoteResponse;
import com.saveapenny.stock.domain.IncomeStatementResponse;
import com.saveapenny.stock.domain.NewsSentimentResponse;
import com.saveapenny.stock.domain.RsiResponse;
import com.saveapenny.stock.domain.SmaResponse;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

public class AlphaVantageClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);

    private final RestClient restClient;
    private final StockProperties properties;
    private final RateLimitTracker rateLimitTracker;

    public AlphaVantageClient(RestClient restClient, StockProperties properties, RateLimitTracker rateLimitTracker) {
        this.restClient = restClient;
        this.properties = properties;
        this.rateLimitTracker = rateLimitTracker;
    }

    public GlobalQuoteResponse fetchQuote(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(GlobalQuoteResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch quote from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch quote from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public DailyTimeSeriesResponse fetchDailySeries(String symbol, String outputSize) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "TIME_SERIES_DAILY")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .queryParam("outputsize", outputSize)
                            .build())
                    .retrieve()
                    .body(DailyTimeSeriesResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch daily series from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch daily series from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public NewsSentimentResponse fetchNewsSentiment(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "NEWS_SENTIMENT")
                            .queryParam("tickers", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(NewsSentimentResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch news sentiment from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch news sentiment from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public CompanyOverview fetchCompanyOverview(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "OVERVIEW")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(CompanyOverview.class);
        } catch (Exception e) {
            log.warn("Failed to fetch company overview from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch company overview from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public IncomeStatementResponse fetchIncomeStatement(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "INCOME_STATEMENT")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(IncomeStatementResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch income statement from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch income statement from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public BalanceSheetResponse fetchBalanceSheet(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "BALANCE_SHEET")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(BalanceSheetResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch balance sheet from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch balance sheet from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public CashFlowResponse fetchCashFlow(String symbol) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "CASH_FLOW")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(CashFlowResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch cash flow from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch cash flow from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public SmaResponse fetchSma(String symbol, String interval, String timePeriod, String seriesType) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "SMA")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("time_period", timePeriod)
                            .queryParam("series_type", seriesType)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(SmaResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch SMA from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch SMA from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public EmaResponse fetchEma(String symbol, String interval, String timePeriod, String seriesType) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "EMA")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("time_period", timePeriod)
                            .queryParam("series_type", seriesType)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(EmaResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch EMA from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch EMA from Alpha Vantage: " + e.getMessage(), e);
        }
    }

    public RsiResponse fetchRsi(String symbol, String interval, String timePeriod, String seriesType) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new StockDisabledException("Alpha Vantage API key is not configured. Set ALPHA_VANTAGE_API_KEY.");
        }

        rateLimitTracker.checkQuota();

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "RSI")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("time_period", timePeriod)
                            .queryParam("series_type", seriesType)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(RsiResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch RSI from Alpha Vantage for symbol {}", symbol, e);
            throw new StockClientException("Failed to fetch RSI from Alpha Vantage: " + e.getMessage(), e);
        }
    }
}
