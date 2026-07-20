package com.saveapenny.insight.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.analytics.AnomalyDetector;
import com.saveapenny.insight.analytics.CategoryInsightAnalyzer;
import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.analytics.SpendingPatternAnalyzer;
import com.saveapenny.insight.analytics.TrendAnalyzer;
import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.insight.repository.InsightRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightGenerationPipelineTest {

    @Mock
    private SpendingPatternAnalyzer spendingPatternAnalyzer;

    @Mock
    private AnomalyDetector anomalyDetector;

    @Mock
    private TrendAnalyzer trendAnalyzer;

    @Mock
    private CategoryInsightAnalyzer categoryInsightAnalyzer;

    @Mock
    private AiEnhancementService aiEnhancementService;

    @Mock
    private InsightRepository insightRepository;

    @Mock
    private InsightNotificationService insightNotificationService;

    @Mock
    private InsightProperties insightProperties;

    @Captor
    private ArgumentCaptor<InsightEntity> entityCaptor;

    private InsightGenerationPipeline pipeline;

    private UUID userId;

    @BeforeEach
    void setUp() {
        pipeline = new InsightGenerationPipeline(
                spendingPatternAnalyzer, anomalyDetector, trendAnalyzer,
                categoryInsightAnalyzer, aiEnhancementService,
                insightRepository, insightNotificationService, insightProperties);
        userId = UUID.randomUUID();
    }

    @Test
    void execute_savesCandidatesAndCreatesNotifications() {
        InsightCandidate warning = new InsightCandidate(InsightType.SPENDING_PATTERN, "Spike", "Summary", "Detail",
                null, "WARNING", null);

        when(spendingPatternAnalyzer.analyze(userId)).thenReturn(List.of(warning));
        when(anomalyDetector.analyze(userId)).thenReturn(List.of());
        when(trendAnalyzer.analyze(userId)).thenReturn(List.of());
        when(categoryInsightAnalyzer.analyze(userId)).thenReturn(List.of());
        when(aiEnhancementService.enhance(any())).thenAnswer(i -> i.getArgument(0));
        when(insightProperties.maxInsightsPerGeneration()).thenReturn(10);
        when(insightProperties.deduplicationWindowDays()).thenReturn(7);
        when(insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                any(), any(), any(), any())).thenReturn(false);

        int count = pipeline.execute(userId);

        assertEquals(1, count);
        verify(insightRepository).save(any(InsightEntity.class));
        verify(insightNotificationService).createInsightGeneratedNotification(eq(userId), any(UUID.class), eq(warning));
    }

    @Test
    void execute_skipsNotificationForInfoSeverity() {
        InsightCandidate info = new InsightCandidate(InsightType.TREND, "Trend", "Summary", "Detail",
                null, "INFO", null);

        when(spendingPatternAnalyzer.analyze(userId)).thenReturn(List.of());
        when(anomalyDetector.analyze(userId)).thenReturn(List.of());
        when(trendAnalyzer.analyze(userId)).thenReturn(List.of(info));
        when(categoryInsightAnalyzer.analyze(userId)).thenReturn(List.of());
        when(aiEnhancementService.enhance(any())).thenAnswer(i -> i.getArgument(0));
        when(insightProperties.maxInsightsPerGeneration()).thenReturn(10);
        when(insightProperties.deduplicationWindowDays()).thenReturn(7);
        when(insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                any(), any(), any(), any())).thenReturn(false);

        pipeline.execute(userId);

        verify(insightNotificationService, never()).createInsightGeneratedNotification(any(), any(), any());
    }

    @Test
    void execute_deduplicatesExistingInsights() {
        InsightCandidate candidate = new InsightCandidate(InsightType.ANOMALY, "Duplicate", "Summary", "Detail",
                null, "WARNING", null);

        when(spendingPatternAnalyzer.analyze(userId)).thenReturn(List.of(candidate));
        when(anomalyDetector.analyze(userId)).thenReturn(List.of());
        when(trendAnalyzer.analyze(userId)).thenReturn(List.of());
        when(categoryInsightAnalyzer.analyze(userId)).thenReturn(List.of());
        when(aiEnhancementService.enhance(any())).thenAnswer(i -> i.getArgument(0));
        when(insightProperties.deduplicationWindowDays()).thenReturn(7);
        when(insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                any(), any(), any(), any()))
                .thenReturn(true);

        int count = pipeline.execute(userId);

        assertEquals(0, count);
        verify(insightRepository, never()).save(any());
        verify(insightNotificationService, never()).createInsightGeneratedNotification(any(), any(), any());
    }

    @Test
    void execute_capsAtMaxInsightsPerGeneration() {
        List<InsightCandidate> candidates = List.of(
                new InsightCandidate(InsightType.SPENDING_PATTERN, "A", "S", null, null, "INFO", null),
                new InsightCandidate(InsightType.TREND, "B", "S", null, null, "INFO", null),
                new InsightCandidate(InsightType.ANOMALY, "C", "S", null, null, "INFO", null));

        when(spendingPatternAnalyzer.analyze(userId)).thenReturn(candidates);
        when(anomalyDetector.analyze(userId)).thenReturn(List.of());
        when(trendAnalyzer.analyze(userId)).thenReturn(List.of());
        when(categoryInsightAnalyzer.analyze(userId)).thenReturn(List.of());
        when(aiEnhancementService.enhance(any())).thenAnswer(i -> i.getArgument(0));
        when(insightProperties.maxInsightsPerGeneration()).thenReturn(2);
        when(insightProperties.deduplicationWindowDays()).thenReturn(7);
        when(insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                any(), any(), any(), any())).thenReturn(false);

        int count = pipeline.execute(userId);

        assertEquals(2, count);
        verify(insightRepository, times(2)).save(entityCaptor.capture());
        assertEquals("A", entityCaptor.getAllValues().get(0).getTitle());
        assertEquals("B", entityCaptor.getAllValues().get(1).getTitle());
    }

    @Test
    void execute_handlesNotificationFailureGracefully() {
        InsightCandidate warning = new InsightCandidate(InsightType.SPENDING_PATTERN, "Spike", "Summary", "Detail",
                null, "WARNING", null);

        when(spendingPatternAnalyzer.analyze(userId)).thenReturn(List.of(warning));
        when(anomalyDetector.analyze(userId)).thenReturn(List.of());
        when(trendAnalyzer.analyze(userId)).thenReturn(List.of());
        when(categoryInsightAnalyzer.analyze(userId)).thenReturn(List.of());
        when(aiEnhancementService.enhance(any())).thenAnswer(i -> i.getArgument(0));
        when(insightProperties.maxInsightsPerGeneration()).thenReturn(10);
        when(insightProperties.deduplicationWindowDays()).thenReturn(7);
        when(insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                any(), any(), any(), any())).thenReturn(false);
        doThrow(new RuntimeException("DB down"))
                .when(insightNotificationService)
                .createInsightGeneratedNotification(any(), any(), any());

        int count = pipeline.execute(userId);

        assertEquals(1, count);
        verify(insightRepository).save(any(InsightEntity.class));
    }

    @Test
    void execute_returnsZeroWhenNoAnalyzersProduceCandidates() {
        when(spendingPatternAnalyzer.analyze(userId)).thenReturn(List.of());
        when(anomalyDetector.analyze(userId)).thenReturn(List.of());
        when(trendAnalyzer.analyze(userId)).thenReturn(List.of());
        when(categoryInsightAnalyzer.analyze(userId)).thenReturn(List.of());
        when(aiEnhancementService.enhance(any())).thenAnswer(i -> i.getArgument(0));

        int count = pipeline.execute(userId);

        assertEquals(0, count);
        verify(insightRepository, never()).save(any());
    }
}
