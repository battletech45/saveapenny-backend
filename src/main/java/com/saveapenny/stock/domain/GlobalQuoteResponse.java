package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GlobalQuoteResponse(
        @JsonProperty("Global Quote") GlobalQuote globalQuote,
        @JsonProperty("Error Message") String errorMessage,
        @JsonProperty("Note") String note) {

    public GlobalQuoteResponse(GlobalQuote globalQuote) {
        this(globalQuote, null, null);
    }
}
