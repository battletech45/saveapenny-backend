package com.saveapenny.shared.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(hidden = true)
    @JsonProperty("insights")
    public List<T> insights() {
        return items;
    }

    @Schema(hidden = true)
    @JsonProperty("totalElements")
    public long totalElements() {
        return totalItems;
    }
}
