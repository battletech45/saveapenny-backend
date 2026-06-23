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
public class StockOverviewResponse {

    private String symbol;
    private String name;
    private String description;
    private String exchange;
    private String currency;
    private String country;
    private String sector;
    private String industry;
    private BigDecimal marketCapitalization;
    private BigDecimal ebitda;
    private BigDecimal peRatio;
    private BigDecimal pegRatio;
    private BigDecimal bookValue;
    private BigDecimal dividendPerShare;
    private BigDecimal dividendYield;
    private BigDecimal eps;
    private BigDecimal revenuePerShareTTM;
    private BigDecimal profitMargin;
    private BigDecimal operatingMarginTTM;
    private BigDecimal returnOnAssetsTTM;
    private BigDecimal returnOnEquityTTM;
    private BigDecimal revenueTTM;
    private BigDecimal grossProfitTTM;
    private BigDecimal dilutedEpsTTM;
    private BigDecimal quarterlyEarningsGrowthYOY;
    private BigDecimal quarterlyRevenueGrowthYOY;
    private BigDecimal analystTargetPrice;
    private BigDecimal trailingPE;
    private BigDecimal forwardPE;
    private BigDecimal priceToSalesRatioTTM;
    private BigDecimal priceToBookRatio;
    private BigDecimal evToRevenue;
    private BigDecimal evToEBITDA;
    private BigDecimal beta;
    private BigDecimal weekHigh52;
    private BigDecimal weekLow52;
    private BigDecimal movingAverage50Day;
    private BigDecimal movingAverage200Day;
    private Long sharesOutstanding;
    private String dividendDate;
    private String exDividendDate;
}
