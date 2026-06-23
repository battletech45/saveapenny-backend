package com.saveapenny.stock.controller;

import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.stock.dto.StockBalanceSheetResponse;
import com.saveapenny.stock.dto.StockCashFlowResponse;
import com.saveapenny.stock.dto.StockDailySeriesResponse;
import com.saveapenny.stock.dto.StockIncomeStatementResponse;
import com.saveapenny.stock.dto.StockNewsResponse;
import com.saveapenny.stock.dto.StockOverviewResponse;
import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.dto.StockTechnicalIndicatorResponse;
import com.saveapenny.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Stocks", description = "Stock market data endpoints powered by Alpha Vantage.")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/quote")
    @Operation(summary = "Get stock quote", description = "Returns a real-time price snapshot for the given symbol.")
    public ResponseEntity<ApiResponse<StockQuoteResponse>> getQuote(@RequestParam String symbol) {
        StockQuoteResponse response = stockService.getQuote(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/daily")
    @Operation(summary = "Get daily time series",
            description = "Returns daily open/high/low/close/volume data points for the given symbol.")
    public ResponseEntity<ApiResponse<StockDailySeriesResponse>> getDailySeries(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "compact") String outputSize) {
        StockDailySeriesResponse response = stockService.getDailySeries(symbol, outputSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/news")
    @Operation(summary = "Get news and sentiment",
            description = "Returns news articles and sentiment data for the given symbol.")
    public ResponseEntity<ApiResponse<StockNewsResponse>> getNewsSentiment(@RequestParam String symbol) {
        StockNewsResponse response = stockService.getNewsSentiment(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get company overview",
            description = "Returns company financial overview including market cap, PE ratio, etc.")
    public ResponseEntity<ApiResponse<StockOverviewResponse>> getCompanyOverview(@RequestParam String symbol) {
        StockOverviewResponse response = stockService.getCompanyOverview(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/income-statement")
    @Operation(summary = "Get income statement",
            description = "Returns annual and quarterly income statements for the given symbol.")
    public ResponseEntity<ApiResponse<StockIncomeStatementResponse>> getIncomeStatement(@RequestParam String symbol) {
        StockIncomeStatementResponse response = stockService.getIncomeStatement(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/balance-sheet")
    @Operation(summary = "Get balance sheet",
            description = "Returns annual and quarterly balance sheets for the given symbol.")
    public ResponseEntity<ApiResponse<StockBalanceSheetResponse>> getBalanceSheet(@RequestParam String symbol) {
        StockBalanceSheetResponse response = stockService.getBalanceSheet(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Get cash flow statement",
            description = "Returns annual and quarterly cash flow statements for the given symbol.")
    public ResponseEntity<ApiResponse<StockCashFlowResponse>> getCashFlow(@RequestParam String symbol) {
        StockCashFlowResponse response = stockService.getCashFlow(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sma")
    @Operation(summary = "Get Simple Moving Average (SMA)",
            description = "Returns SMA values for the given symbol, interval, time period, and series type.")
    public ResponseEntity<ApiResponse<StockTechnicalIndicatorResponse>> getSma(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "daily") String interval,
            @RequestParam String timePeriod,
            @RequestParam(defaultValue = "close") String seriesType) {
        StockTechnicalIndicatorResponse response = stockService.getSma(symbol, interval, timePeriod, seriesType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/ema")
    @Operation(summary = "Get Exponential Moving Average (EMA)",
            description = "Returns EMA values for the given symbol, interval, time period, and series type.")
    public ResponseEntity<ApiResponse<StockTechnicalIndicatorResponse>> getEma(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "daily") String interval,
            @RequestParam String timePeriod,
            @RequestParam(defaultValue = "close") String seriesType) {
        StockTechnicalIndicatorResponse response = stockService.getEma(symbol, interval, timePeriod, seriesType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rsi")
    @Operation(summary = "Get Relative Strength Index (RSI)",
            description = "Returns RSI values for the given symbol, interval, time period, and series type.")
    public ResponseEntity<ApiResponse<StockTechnicalIndicatorResponse>> getRsi(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "daily") String interval,
            @RequestParam String timePeriod,
            @RequestParam(defaultValue = "close") String seriesType) {
        StockTechnicalIndicatorResponse response = stockService.getRsi(symbol, interval, timePeriod, seriesType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
