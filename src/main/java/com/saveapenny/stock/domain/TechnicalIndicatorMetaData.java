package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TechnicalIndicatorMetaData(
        @JsonAlias("1: Symbol") String symbol,
        @JsonAlias("2: Indicator") String indicator,
        @JsonAlias("3: Last Refreshed") String lastRefreshed,
        @JsonAlias("4: Interval") String interval,
        @JsonAlias("5: Time Period") String timePeriod,
        @JsonAlias("6: Series Type") String seriesType,
        @JsonAlias("7: Time Zone") String timeZone) {}
