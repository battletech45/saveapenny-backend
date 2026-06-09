package com.saveapenny.insight.analytics;

import com.saveapenny.insight.entity.InsightType;
import java.util.UUID;

public record InsightCandidate(
        InsightType type,
        String title,
        String summary,
        String detail,
        UUID categoryId,
        String severity,
        String metadata) {
}
