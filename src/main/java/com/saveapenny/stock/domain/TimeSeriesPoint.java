package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TimeSeriesPoint(
        @JsonAlias("1. open") String open,
        @JsonAlias("2. high") String high,
        @JsonAlias("3. low") String low,
        @JsonAlias("4. close") String close,
        @JsonAlias("5. volume") String volume) {
}
