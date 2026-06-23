package com.saveapenny.stock.service;

import com.saveapenny.stock.dto.StockBalanceSheetResponse;
import com.saveapenny.stock.dto.StockCashFlowResponse;
import com.saveapenny.stock.dto.StockDailySeriesResponse;
import com.saveapenny.stock.dto.StockIncomeStatementResponse;
import com.saveapenny.stock.dto.StockNewsResponse;
import com.saveapenny.stock.dto.StockOverviewResponse;
import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.dto.StockTechnicalIndicatorResponse;

public interface StockService {

    StockQuoteResponse getQuote(String symbol);

    StockDailySeriesResponse getDailySeries(String symbol);

    StockDailySeriesResponse getDailySeries(String symbol, String outputSize);

    StockNewsResponse getNewsSentiment(String symbol);

    StockOverviewResponse getCompanyOverview(String symbol);

    StockIncomeStatementResponse getIncomeStatement(String symbol);

    StockBalanceSheetResponse getBalanceSheet(String symbol);

    StockCashFlowResponse getCashFlow(String symbol);

    StockTechnicalIndicatorResponse getSma(String symbol, String interval, String timePeriod, String seriesType);

    StockTechnicalIndicatorResponse getEma(String symbol, String interval, String timePeriod, String seriesType);

    StockTechnicalIndicatorResponse getRsi(String symbol, String interval, String timePeriod, String seriesType);
}
