package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmaResponse(
        @JsonProperty("Meta Data") TechnicalIndicatorMetaData metaData,
        @JsonProperty("Technical Analysis: EMA") Map<String, EmaPoint> dataPoints,
        @JsonProperty("Error Message") String errorMessage,
        @JsonProperty("Note") String note) {

    public EmaResponse(TechnicalIndicatorMetaData metaData, Map<String, EmaPoint> dataPoints) {
        this(metaData, dataPoints, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmaPoint(
            @JsonProperty("EMA") String value) {}
}
