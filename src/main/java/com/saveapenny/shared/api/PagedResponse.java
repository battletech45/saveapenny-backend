package com.saveapenny.shared.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    @JsonProperty("insights")
    public List<T> insights() {
        return items;
    }

    @JsonProperty("totalElements")
    public long totalElements() {
        return totalItems;
    }
}
