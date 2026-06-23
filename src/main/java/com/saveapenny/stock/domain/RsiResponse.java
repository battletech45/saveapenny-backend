package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RsiResponse(
        @JsonProperty("Meta Data") TechnicalIndicatorMetaData metaData,
        @JsonProperty("Technical Analysis: RSI") Map<String, RsiPoint> dataPoints,
        @JsonProperty("Error Message") String errorMessage,
        @JsonProperty("Note") String note) {

    public RsiResponse(TechnicalIndicatorMetaData metaData, Map<String, RsiPoint> dataPoints) {
        this(metaData, dataPoints, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RsiPoint(
            @JsonProperty("RSI") String value) {}
}
