package com.saveapenny.insight.dto;

import com.saveapenny.insight.entity.InsightType;
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
public class GenerateInsightsRequest {

    private InsightType type;
}
