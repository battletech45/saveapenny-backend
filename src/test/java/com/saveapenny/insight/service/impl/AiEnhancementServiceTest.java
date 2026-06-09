package com.saveapenny.insight.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.entity.InsightType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiEnhancementServiceTest {

    @Mock
    private InsightProperties insightProperties;

    @InjectMocks
    private AiEnhancementService aiEnhancementService;

    @Test
    void enhance_returnsCandidatesUnchanged_whenAiNotEnabled() {
        when(insightProperties.aiEnhanced()).thenReturn(false);

        List<InsightCandidate> candidates = List.of(
                new InsightCandidate(InsightType.TREND, "High spending", "Detail", null, null, "high", null));

        List<InsightCandidate> result = aiEnhancementService.enhance(candidates);

        assertEquals(candidates, result);
    }

    @Test
    void enhance_returnsCandidatesUnchanged_whenAiEnabled() {
        when(insightProperties.aiEnhanced()).thenReturn(true);

        List<InsightCandidate> candidates = List.of(
                new InsightCandidate(InsightType.ANOMALY, "Over budget", "Exceeded by 20%", null, null, "warning", null));

        List<InsightCandidate> result = aiEnhancementService.enhance(candidates);

        assertEquals(candidates, result);
    }

    @Test
    void enhance_returnsEmptyList_whenNoCandidates() {
        when(insightProperties.aiEnhanced()).thenReturn(true);

        List<InsightCandidate> result = aiEnhancementService.enhance(List.of());

        assertEquals(0, result.size());
    }

    @Test
    void enhance_withMultipleCandidates_returnsAllUnchanged() {
        when(insightProperties.aiEnhanced()).thenReturn(false);

        List<InsightCandidate> candidates = List.of(
                new InsightCandidate(InsightType.SPENDING_PATTERN, "Pattern 1", "Detail 1", null, null, "low", null),
                new InsightCandidate(InsightType.RECOMMENDATION, "Recommendation 1", "Detail 2", null, null, "medium", null),
                new InsightCandidate(InsightType.TREND, "Trend 1", "Detail 3", null, UUID.randomUUID(), "high", null));

        List<InsightCandidate> result = aiEnhancementService.enhance(candidates);

        assertEquals(3, result.size());
        assertEquals(candidates, result);
    }
}
