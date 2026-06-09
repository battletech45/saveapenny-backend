package com.saveapenny.insight.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightListResponse {

    private List<InsightResponse> insights;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
