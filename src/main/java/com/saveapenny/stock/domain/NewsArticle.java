package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsArticle(
        @JsonAlias("title") String title,
        @JsonAlias("url") String url,
        @JsonAlias("time_published") String timePublished,
        @JsonAlias("summary") String summary,
        @JsonAlias("source") String source,
        @JsonAlias("overall_sentiment_score") BigDecimal overallSentimentScore,
        @JsonAlias("overall_sentiment_label") String overallSentimentLabel,
        @JsonAlias("ticker_sentiment") List<TickerSentiment> tickerSentiment) {
}
