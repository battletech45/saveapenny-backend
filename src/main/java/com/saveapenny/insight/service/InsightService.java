package com.saveapenny.insight.service;

import com.saveapenny.insight.dto.GenerateInsightsRequest;
import com.saveapenny.insight.dto.InsightListResponse;
import com.saveapenny.insight.dto.InsightResponse;
import com.saveapenny.insight.entity.InsightType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface InsightService {

    InsightListResponse getAll(UUID currentUserId, InsightType type, String severity, Boolean isRead, Pageable pageable);

    InsightResponse getById(UUID currentUserId, UUID insightId);

    InsightResponse markAsRead(UUID currentUserId, UUID insightId);

    InsightResponse dismiss(UUID currentUserId, UUID insightId);

    int generate(UUID currentUserId, GenerateInsightsRequest request);
}
