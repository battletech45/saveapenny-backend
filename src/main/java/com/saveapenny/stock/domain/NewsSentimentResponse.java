package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsSentimentResponse(
        @JsonProperty("items") String items,
        @JsonProperty("feed") List<NewsArticle> feed,
        @JsonProperty("Error Message") String errorMessage,
        @JsonProperty("Note") String note) {

    public NewsSentimentResponse(String items, List<NewsArticle> feed) {
        this(items, feed, null, null);
    }
}
