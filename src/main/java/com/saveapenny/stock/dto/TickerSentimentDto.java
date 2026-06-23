package com.saveapenny.stock.dto;

import java.math.BigDecimal;
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
public class TickerSentimentDto {

    private String ticker;
    private String relevanceScore;
    private BigDecimal tickerSentimentScore;
    private String tickerSentimentLabel;
}
