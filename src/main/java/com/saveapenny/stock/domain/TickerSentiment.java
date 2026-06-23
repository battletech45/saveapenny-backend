package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TickerSentiment(
        @JsonAlias("ticker") String ticker,
        @JsonAlias("relevance_score") String relevanceScore,
        @JsonAlias("ticker_sentiment_score") BigDecimal tickerSentimentScore,
        @JsonAlias("ticker_sentiment_label") String tickerSentimentLabel) {
}
