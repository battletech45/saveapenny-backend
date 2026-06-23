package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SmaResponse(
        @JsonProperty("Meta Data") TechnicalIndicatorMetaData metaData,
        @JsonProperty("Technical Analysis: SMA") Map<String, SmaPoint> dataPoints,
        @JsonProperty("Error Message") String errorMessage,
        @JsonProperty("Note") String note) {

    public SmaResponse(TechnicalIndicatorMetaData metaData, Map<String, SmaPoint> dataPoints) {
        this(metaData, dataPoints, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SmaPoint(
            @JsonProperty("SMA") String value) {}
}
