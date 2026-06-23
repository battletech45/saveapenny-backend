package com.saveapenny.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class StockQuoteResponse {

    private String symbol;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal price;
    private Long volume;
    private LocalDate latestTradingDay;
    private BigDecimal previousClose;
    private BigDecimal change;
    private BigDecimal changePercent;
}
