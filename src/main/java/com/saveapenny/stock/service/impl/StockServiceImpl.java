package com.saveapenny.stock.service.impl;

import com.saveapenny.stock.config.StockProperties;
import com.saveapenny.stock.domain.BalanceSheetResponse;
import com.saveapenny.stock.domain.BalanceSheetResponse.BalanceSheetItem;
import com.saveapenny.stock.domain.CashFlowResponse;
import com.saveapenny.stock.domain.CashFlowResponse.CashFlowItem;
import com.saveapenny.stock.domain.CompanyOverview;
import com.saveapenny.stock.domain.DailyTimeSeriesResponse;
import com.saveapenny.stock.domain.EmaResponse;
import com.saveapenny.stock.domain.GlobalQuote;
import com.saveapenny.stock.domain.IncomeStatementResponse;
import com.saveapenny.stock.domain.IncomeStatementResponse.IncomeReportItem;
import com.saveapenny.stock.domain.NewsArticle;
import com.saveapenny.stock.domain.NewsSentimentResponse;
import com.saveapenny.stock.domain.RsiResponse;
import com.saveapenny.stock.domain.SmaResponse;
import com.saveapenny.stock.domain.TechnicalIndicatorMetaData;
import com.saveapenny.stock.domain.TickerSentiment;
import com.saveapenny.stock.domain.TimeSeriesPoint;
import com.saveapenny.stock.dto.DailyPoint;
import com.saveapenny.stock.dto.FinancialReportItem;
import com.saveapenny.stock.dto.NewsArticleDto;
import com.saveapenny.stock.dto.StockBalanceSheetResponse;
import com.saveapenny.stock.dto.StockCashFlowResponse;
import com.saveapenny.stock.dto.StockDailySeriesResponse;
import com.saveapenny.stock.dto.StockIncomeStatementResponse;
import com.saveapenny.stock.dto.StockNewsResponse;
import com.saveapenny.stock.dto.StockOverviewResponse;
import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.dto.StockTechnicalIndicatorResponse;
import com.saveapenny.stock.dto.TechnicalIndicatorDataPoint;
import com.saveapenny.stock.dto.TickerSentimentDto;
import com.saveapenny.stock.exception.InvalidStockSymbolException;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import com.saveapenny.stock.exception.StockQuoteNotAvailableException;
import com.saveapenny.stock.infrastructure.AlphaVantageClient;
import com.saveapenny.stock.service.StockService;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public class StockServiceImpl implements StockService {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9.\\-]+$");
    private static final int MAX_SYMBOL_LENGTH = 10;
    private static final Pattern OUTPUT_SIZE_PATTERN = Pattern.compile("^(compact|full)$");
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("^(daily|weekly|monthly)$");
    private static final Pattern TIME_PERIOD_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern SERIES_TYPE_PATTERN = Pattern.compile("^(close|open|high|low)$");

    private final AlphaVantageClient alphaVantageClient;
    private final StockProperties properties;

    public StockServiceImpl(AlphaVantageClient alphaVantageClient, StockProperties properties) {
        this.alphaVantageClient = alphaVantageClient;
        this.properties = properties;
    }

    @Override
    public StockQuoteResponse getQuote(String symbol) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);

        var response = alphaVantageClient.fetchQuote(normalized);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "quote");
        var quote = response.globalQuote();

        if (quote == null) {
            throw new StockQuoteNotAvailableException(
                    "No quote data returned for symbol: " + normalized);
        }

        return mapToQuoteResponse(quote);
    }

    @Override
    public StockDailySeriesResponse getDailySeries(String symbol) {
        return getDailySeries(symbol, "compact");
    }

    @Override
    public StockDailySeriesResponse getDailySeries(String symbol, String outputSize) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);
        String normalizedOutputSize = normalizeOutputSize(outputSize);

        var response = alphaVantageClient.fetchDailySeries(normalized, normalizedOutputSize);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "daily series");

        if (response.metaData() == null || !StringUtils.hasText(response.metaData().symbol())) {
            throw new StockQuoteNotAvailableException(
                    "No daily series data returned for symbol: " + normalized);
        }

        if (response.timeSeries() == null || response.timeSeries().isEmpty()) {
            return StockDailySeriesResponse.builder()
                    .symbol(normalized)
                    .dataPoints(List.of())
                    .build();
        }

        List<DailyPoint> points = response.timeSeries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(entry -> mapToDailyPoint(entry.getKey(), entry.getValue()))
                .toList();

        return StockDailySeriesResponse.builder()
                .symbol(normalized)
                .dataPoints(points)
                .build();
    }

    @Override
    public StockNewsResponse getNewsSentiment(String symbol) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);

        NewsSentimentResponse response = alphaVantageClient.fetchNewsSentiment(normalized);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "news sentiment");

        if (response.feed() == null) {
            throw new StockQuoteNotAvailableException(
                    "No news sentiment data returned for symbol: " + normalized);
        }

        if (response.feed().isEmpty()) {
            return StockNewsResponse.builder()
                    .items(0)
                    .articles(List.of())
                    .build();
        }

        int items = parseInt(response.items(), 0);

        List<NewsArticleDto> articles = response.feed().stream()
                .map(this::mapToArticleDto)
                .toList();

        return StockNewsResponse.builder()
                .items(items)
                .articles(articles)
                .build();
    }

    @Override
    public StockOverviewResponse getCompanyOverview(String symbol) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);

        CompanyOverview overview = alphaVantageClient.fetchCompanyOverview(normalized);
        throwIfProviderError(overview.errorMessage(), overview.note(), normalized, "company overview");

        if (!StringUtils.hasText(overview.symbol())) {
            throw new StockQuoteNotAvailableException(
                    "No company overview data returned for symbol: " + normalized);
        }

        return mapToOverviewResponse(overview);
    }

    @Override
    public StockIncomeStatementResponse getIncomeStatement(String symbol) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);

        IncomeStatementResponse response = alphaVantageClient.fetchIncomeStatement(normalized);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "income statement");

        if (!StringUtils.hasText(response.symbol())) {
            throw new StockQuoteNotAvailableException(
                    "No income statement data returned for symbol: " + normalized);
        }

        return StockIncomeStatementResponse.builder()
                .symbol(normalized)
                .annualReports(toFinancialReportItems(response.annualReports()))
                .quarterlyReports(toFinancialReportItems(response.quarterlyReports()))
                .build();
    }

    @Override
    public StockBalanceSheetResponse getBalanceSheet(String symbol) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);

        BalanceSheetResponse response = alphaVantageClient.fetchBalanceSheet(normalized);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "balance sheet");

        if (!StringUtils.hasText(response.symbol())) {
            throw new StockQuoteNotAvailableException(
                    "No balance sheet data returned for symbol: " + normalized);
        }

        return StockBalanceSheetResponse.builder()
                .symbol(normalized)
                .annualReports(toFinancialReportItems(response.annualReports()))
                .quarterlyReports(toFinancialReportItems(response.quarterlyReports()))
                .build();
    }

    @Override
    public StockCashFlowResponse getCashFlow(String symbol) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);

        CashFlowResponse response = alphaVantageClient.fetchCashFlow(normalized);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "cash flow");

        if (!StringUtils.hasText(response.symbol())) {
            throw new StockQuoteNotAvailableException(
                    "No cash flow data returned for symbol: " + normalized);
        }

        return StockCashFlowResponse.builder()
                .symbol(normalized)
                .annualReports(toFinancialReportItems(response.annualReports()))
                .quarterlyReports(toFinancialReportItems(response.quarterlyReports()))
                .build();
    }

    @Override
    public StockTechnicalIndicatorResponse getSma(String symbol, String interval, String timePeriod, String seriesType) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);
        String normalizedInterval = normalizeInterval(interval);
        String normalizedTimePeriod = normalizeTimePeriod(timePeriod);
        String normalizedSeriesType = normalizeSeriesType(seriesType);

        SmaResponse response = alphaVantageClient.fetchSma(normalized, normalizedInterval, normalizedTimePeriod, normalizedSeriesType);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "SMA");

        return mapToTechnicalIndicatorResponse(normalized, "SMA", response.metaData(), response.dataPoints());
    }

    @Override
    public StockTechnicalIndicatorResponse getEma(String symbol, String interval, String timePeriod, String seriesType) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);
        String normalizedInterval = normalizeInterval(interval);
        String normalizedTimePeriod = normalizeTimePeriod(timePeriod);
        String normalizedSeriesType = normalizeSeriesType(seriesType);

        EmaResponse response = alphaVantageClient.fetchEma(normalized, normalizedInterval, normalizedTimePeriod, normalizedSeriesType);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "EMA");

        return mapToTechnicalIndicatorResponse(normalized, "EMA", response.metaData(), response.dataPoints());
    }

    @Override
    public StockTechnicalIndicatorResponse getRsi(String symbol, String interval, String timePeriod, String seriesType) {
        if (!properties.enabled()) {
            throw new StockDisabledException("Stock market feature is not enabled.");
        }

        String normalized = normalizeSymbol(symbol);
        String normalizedInterval = normalizeInterval(interval);
        String normalizedTimePeriod = normalizeTimePeriod(timePeriod);
        String normalizedSeriesType = normalizeSeriesType(seriesType);

        RsiResponse response = alphaVantageClient.fetchRsi(normalized, normalizedInterval, normalizedTimePeriod, normalizedSeriesType);
        throwIfProviderError(response.errorMessage(), response.note(), normalized, "RSI");

        return mapToTechnicalIndicatorResponse(normalized, "RSI", response.metaData(), response.dataPoints());
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new InvalidStockSymbolException("Symbol must not be blank");
        }

        String trimmed = symbol.trim().toUpperCase();

        if (trimmed.length() > MAX_SYMBOL_LENGTH) {
            throw new InvalidStockSymbolException(
                    "Symbol must not exceed " + MAX_SYMBOL_LENGTH + " characters");
        }

        if (!SYMBOL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidStockSymbolException(
                    "Symbol contains invalid characters. Allowed: A-Z, 0-9, dot, hyphen");
        }

        return trimmed;
    }

    private String normalizeOutputSize(String outputSize) {
        if (!StringUtils.hasText(outputSize)) {
            return "compact";
        }

        String trimmed = outputSize.trim().toLowerCase();

        if (!OUTPUT_SIZE_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidStockSymbolException(
                    "Output size must be 'compact' or 'full', got: " + outputSize);
        }

        return trimmed;
    }

    private String normalizeInterval(String interval) {
        if (!StringUtils.hasText(interval)) {
            return "daily";
        }

        String trimmed = interval.trim().toLowerCase();

        if (!INTERVAL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidStockSymbolException(
                    "Interval must be 'daily', 'weekly', or 'monthly', got: " + interval);
        }

        return trimmed;
    }

    private String normalizeTimePeriod(String timePeriod) {
        if (!StringUtils.hasText(timePeriod)) {
            throw new InvalidStockSymbolException("Time period must not be blank");
        }

        String trimmed = timePeriod.trim();

        if (!TIME_PERIOD_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidStockSymbolException(
                    "Time period must be a positive integer, got: " + timePeriod);
        }

        return trimmed;
    }

    private String normalizeSeriesType(String seriesType) {
        if (!StringUtils.hasText(seriesType)) {
            return "close";
        }

        String trimmed = seriesType.trim().toLowerCase();

        if (!SERIES_TYPE_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidStockSymbolException(
                    "Series type must be 'close', 'open', 'high', or 'low', got: " + seriesType);
        }

        return trimmed;
    }

    private void throwIfProviderError(String errorMessage, String note, String symbol, String operation) {
        if (StringUtils.hasText(errorMessage)) {
            throw new StockClientException(
                    "Alpha Vantage returned an error for " + operation + " and symbol " + symbol + ": " + errorMessage);
        }

        if (StringUtils.hasText(note)) {
            throw new StockClientException(
                    "Alpha Vantage returned a note for " + operation + " and symbol " + symbol + ": " + note);
        }
    }

    private StockQuoteResponse mapToQuoteResponse(GlobalQuote quote) {
        return StockQuoteResponse.builder()
                .symbol(quote.symbol())
                .open(toBigDecimal(quote.open()))
                .high(toBigDecimal(quote.high()))
                .low(toBigDecimal(quote.low()))
                .price(toBigDecimal(quote.price()))
                .volume(toLong(quote.volume()))
                .latestTradingDay(toLocalDate(quote.latestTradingDay()))
                .previousClose(toBigDecimal(quote.previousClose()))
                .change(toBigDecimal(quote.change()))
                .changePercent(parsePercent(quote.changePercent()))
                .build();
    }

    private DailyPoint mapToDailyPoint(String dateStr, TimeSeriesPoint point) {
        return DailyPoint.builder()
                .date(toLocalDate(dateStr))
                .open(toBigDecimal(point.open()))
                .high(toBigDecimal(point.high()))
                .low(toBigDecimal(point.low()))
                .close(toBigDecimal(point.close()))
                .volume(toLong(point.volume()))
                .build();
    }

    private NewsArticleDto mapToArticleDto(NewsArticle article) {
        List<TickerSentimentDto> sentimentDto = Optional.ofNullable(article.tickerSentiment())
                .map(list -> list.stream()
                        .map(this::mapToTickerSentimentDto)
                        .toList())
                .orElse(List.of());

        return NewsArticleDto.builder()
                .title(article.title())
                .url(article.url())
                .timePublished(article.timePublished())
                .summary(article.summary())
                .source(article.source())
                .overallSentimentScore(article.overallSentimentScore())
                .overallSentimentLabel(article.overallSentimentLabel())
                .tickerSentiment(sentimentDto)
                .build();
    }

    private TickerSentimentDto mapToTickerSentimentDto(TickerSentiment ts) {
        return TickerSentimentDto.builder()
                .ticker(ts.ticker())
                .relevanceScore(ts.relevanceScore())
                .tickerSentimentScore(ts.tickerSentimentScore())
                .tickerSentimentLabel(ts.tickerSentimentLabel())
                .build();
    }

    private StockOverviewResponse mapToOverviewResponse(CompanyOverview overview) {
        return StockOverviewResponse.builder()
                .symbol(overview.symbol())
                .name(overview.name())
                .description(overview.description())
                .exchange(overview.exchange())
                .currency(overview.currency())
                .country(overview.country())
                .sector(overview.sector())
                .industry(overview.industry())
                .marketCapitalization(toBigDecimal(overview.marketCapitalization()))
                .ebitda(toBigDecimal(overview.ebitda()))
                .peRatio(toBigDecimal(overview.peRatio()))
                .pegRatio(toBigDecimal(overview.pegRatio()))
                .bookValue(toBigDecimal(overview.bookValue()))
                .dividendPerShare(toBigDecimal(overview.dividendPerShare()))
                .dividendYield(toBigDecimal(overview.dividendYield()))
                .eps(toBigDecimal(overview.eps()))
                .revenuePerShareTTM(toBigDecimal(overview.revenuePerShareTTM()))
                .profitMargin(toBigDecimal(overview.profitMargin()))
                .operatingMarginTTM(toBigDecimal(overview.operatingMarginTTM()))
                .returnOnAssetsTTM(toBigDecimal(overview.returnOnAssetsTTM()))
                .returnOnEquityTTM(toBigDecimal(overview.returnOnEquityTTM()))
                .revenueTTM(toBigDecimal(overview.revenueTTM()))
                .grossProfitTTM(toBigDecimal(overview.grossProfitTTM()))
                .dilutedEpsTTM(toBigDecimal(overview.dilutedEpsTTM()))
                .quarterlyEarningsGrowthYOY(toBigDecimal(overview.quarterlyEarningsGrowthYOY()))
                .quarterlyRevenueGrowthYOY(toBigDecimal(overview.quarterlyRevenueGrowthYOY()))
                .analystTargetPrice(toBigDecimal(overview.analystTargetPrice()))
                .trailingPE(toBigDecimal(overview.trailingPE()))
                .forwardPE(toBigDecimal(overview.forwardPE()))
                .priceToSalesRatioTTM(toBigDecimal(overview.priceToSalesRatioTTM()))
                .priceToBookRatio(toBigDecimal(overview.priceToBookRatio()))
                .evToRevenue(toBigDecimal(overview.evToRevenue()))
                .evToEBITDA(toBigDecimal(overview.evToEBITDA()))
                .beta(toBigDecimal(overview.beta()))
                .weekHigh52(toBigDecimal(overview.weekHigh52()))
                .weekLow52(toBigDecimal(overview.weekLow52()))
                .movingAverage50Day(toBigDecimal(overview.movingAverage50Day()))
                .movingAverage200Day(toBigDecimal(overview.movingAverage200Day()))
                .sharesOutstanding(toLong(overview.sharesOutstanding()))
                .dividendDate(overview.dividendDate())
                .exDividendDate(overview.exDividendDate())
                .build();
    }

    private <T extends Record> List<FinancialReportItem> toFinancialReportItems(List<T> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(this::extractFinancialReportItem)
                .toList();
    }

    private <T extends Record> FinancialReportItem extractFinancialReportItem(T item) {
        if (item == null) {
            return FinancialReportItem.builder()
                    .fiscalDateEnding(null)
                    .reportedCurrency(null)
                    .fields(Map.of())
                    .build();
        }

        RecordComponent[] components = item.getClass().getRecordComponents();
        Map<String, String> fields = new HashMap<>();

        String fiscalDateEnding = null;
        String reportedCurrency = null;

        for (RecordComponent component : components) {
            String name = component.getName();
            try {
                Method accessor = component.getAccessor();
                Object value = accessor.invoke(item);
                String strValue = value != null ? value.toString() : null;

                if ("fiscalDateEnding".equals(name)) {
                    fiscalDateEnding = strValue;
                } else if ("reportedCurrency".equals(name)) {
                    reportedCurrency = strValue;
                } else if (strValue != null) {
                    fields.put(name, strValue);
                }
            } catch (Exception e) {
                // skip field on error
            }
        }

        return FinancialReportItem.builder()
                .fiscalDateEnding(fiscalDateEnding)
                .reportedCurrency(reportedCurrency)
                .fields(fields)
                .build();
    }

    private StockTechnicalIndicatorResponse mapToTechnicalIndicatorResponse(
            String symbol, String indicator, TechnicalIndicatorMetaData metaData,
            Map<String, ? extends Record> dataPoints) {

        if (metaData == null || !StringUtils.hasText(metaData.symbol())) {
            throw new StockQuoteNotAvailableException(
                    "No " + indicator + " data returned for symbol: " + symbol);
        }

        if (dataPoints == null || dataPoints.isEmpty()) {
            return StockTechnicalIndicatorResponse.builder()
                    .symbol(symbol)
                    .indicator(indicator)
                    .dataPoints(List.of())
                    .build();
        }

        List<TechnicalIndicatorDataPoint> points = dataPoints.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(entry -> {
                    String value = extractValueField(entry.getValue());
                    return TechnicalIndicatorDataPoint.builder()
                            .date(toLocalDate(entry.getKey()))
                            .value(toBigDecimal(value))
                            .build();
                })
                .toList();

        return StockTechnicalIndicatorResponse.builder()
                .symbol(symbol)
                .indicator(indicator)
                .dataPoints(points)
                .build();
    }

    private String extractValueField(Record record) {
        if (record == null) return null;
        try {
            RecordComponent[] components = record.getClass().getRecordComponents();
            if (components.length > 0) {
                Method accessor = components[0].getAccessor();
                Object value = accessor.invoke(record);
                return value != null ? value.toString() : null;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private BigDecimal toBigDecimal(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long toLong(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate toLocalDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parsePercent(String value) {
        if (!StringUtils.hasText(value)) return null;
        String cleaned = value.trim().replace("%", "").replace(",", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
