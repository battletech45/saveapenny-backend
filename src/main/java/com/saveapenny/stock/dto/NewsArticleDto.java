package com.saveapenny.stock.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleDto {

    private String title;
    private String url;
    private String timePublished;
    private String summary;
    private String source;
    private BigDecimal overallSentimentScore;
    private String overallSentimentLabel;
    private List<TickerSentimentDto> tickerSentiment;
}
