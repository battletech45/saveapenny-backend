package com.saveapenny.insight.dto;

import com.saveapenny.insight.entity.InsightType;
import java.time.OffsetDateTime;
import java.util.UUID;
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
public class InsightResponse {

    private UUID id;
    private InsightType type;
    private String title;
    private String summary;
    private String detail;
    private UUID categoryId;
    private String severity;
    private String metadata;
    private Boolean read;
    private Boolean dismissed;
    private OffsetDateTime generatedAt;
    private OffsetDateTime createdAt;
}
