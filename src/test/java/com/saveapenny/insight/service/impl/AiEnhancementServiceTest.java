package com.saveapenny.insight.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.entity.InsightType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AiEnhancementServiceTest {

    @Mock
    private InsightProperties insightProperties;

    @Mock
    private ObjectProvider<ChatClient> chatClientProvider;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private AiEnhancementService aiEnhancementService;

    @BeforeEach
    void setUp() {
        aiEnhancementService = new AiEnhancementService(
                insightProperties,
                chatClientProvider,
                new ObjectMapper());
    }

    @Test
    void enhance_returnsCandidatesUnchanged_whenAiNotEnabled() {
        when(insightProperties.aiEnhanced()).thenReturn(false);

        List<InsightCandidate> candidates = List.of(
                new InsightCandidate(InsightType.TREND, "High spending", "Detail", null, null, "high", null));

        List<InsightCandidate> result = aiEnhancementService.enhance(candidates);

        assertEquals(candidates, result);
        verifyNoInteractions(chatClientProvider);
    }

    @Test
    void enhance_returnsCandidatesUnchanged_whenAiEnabledButClientMissing() {
        when(insightProperties.aiEnhanced()).thenReturn(true);
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

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
        verifyNoInteractions(chatClientProvider);
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

    @Test
    void enhance_rewritesTitleSummaryAndDetail_whenAiReturnsValidJson() {
        when(insightProperties.aiEnhanced()).thenReturn(true);
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(any(String.class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(any(String.class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("""
                        [
                          {
                            "title": "Dining spending is up",
                            "summary": "You spent 20% more than usual.",
                            "detail": "This increase was concentrated in dining purchases."
                          }
                        ]
                        """);

        InsightCandidate candidate = new InsightCandidate(
                InsightType.SPENDING_PATTERN,
                "Spending pattern detected",
                "Spending increased by 20%",
                "Current period: $120.00, Previous period: $100.00",
                UUID.randomUUID(),
                "WARNING",
                "raw-metadata");

        List<InsightCandidate> result = aiEnhancementService.enhance(List.of(candidate));

        assertEquals(1, result.size());
        assertEquals("Dining spending is up", result.getFirst().title());
        assertEquals("You spent 20% more than usual.", result.getFirst().summary());
        assertEquals("This increase was concentrated in dining purchases.", result.getFirst().detail());
        assertEquals(candidate.type(), result.getFirst().type());
        assertEquals(candidate.categoryId(), result.getFirst().categoryId());
        assertEquals(candidate.severity(), result.getFirst().severity());
        assertEquals(candidate.metadata(), result.getFirst().metadata());
    }

    @Test
    void enhance_returnsCandidatesUnchanged_whenAiReturnsInvalidJson() {
        when(insightProperties.aiEnhanced()).thenReturn(true);
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(any(String.class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(any(String.class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("not-json");

        List<InsightCandidate> candidates = List.of(
                new InsightCandidate(InsightType.ANOMALY, "Over budget", "Exceeded by 20%", null, null, "warning", null));

        List<InsightCandidate> result = aiEnhancementService.enhance(candidates);

        assertEquals(candidates, result);
    }
}
