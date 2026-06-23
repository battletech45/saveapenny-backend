package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GlobalQuote(
        @JsonAlias("01. symbol") String symbol,
        @JsonAlias("02. open") String open,
        @JsonAlias("03. high") String high,
        @JsonAlias("04. low") String low,
        @JsonAlias("05. price") String price,
        @JsonAlias("06. volume") String volume,
        @JsonAlias("07. latest trading day") String latestTradingDay,
        @JsonAlias("08. previous close") String previousClose,
        @JsonAlias("09. change") String change,
        @JsonAlias("10. change percent") String changePercent) {
}
