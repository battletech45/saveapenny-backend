package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DailyTimeSeriesResponse(
        @JsonProperty("Meta Data") MetaData metaData,
        @JsonProperty("Time Series (Daily)") Map<String, TimeSeriesPoint> timeSeries,
        @JsonProperty("Error Message") String errorMessage,
        @JsonProperty("Note") String note) {

    public DailyTimeSeriesResponse(MetaData metaData, Map<String, TimeSeriesPoint> timeSeries) {
        this(metaData, timeSeries, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetaData(
            @JsonProperty("1. Information") String information,
            @JsonProperty("2. Symbol") String symbol,
            @JsonProperty("3. Last Refreshed") String lastRefreshed,
            @JsonProperty("4. Output Size") String outputSize,
            @JsonProperty("5. Time Zone") String timeZone) {}
}
