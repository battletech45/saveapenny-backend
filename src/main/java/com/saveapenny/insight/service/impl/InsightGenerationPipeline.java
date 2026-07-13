package com.saveapenny.insight.service.impl;

import com.saveapenny.insight.analytics.AnomalyDetector;
import com.saveapenny.insight.analytics.CategoryInsightAnalyzer;
import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.analytics.SpendingPatternAnalyzer;
import com.saveapenny.insight.analytics.TrendAnalyzer;
import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.repository.InsightRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InsightGenerationPipeline {

    private static final Logger log = LoggerFactory.getLogger(InsightGenerationPipeline.class);

    private final SpendingPatternAnalyzer spendingPatternAnalyzer;
    private final AnomalyDetector anomalyDetector;
    private final TrendAnalyzer trendAnalyzer;
    private final CategoryInsightAnalyzer categoryInsightAnalyzer;
    private final AiEnhancementService aiEnhancementService;
    private final InsightRepository insightRepository;
    private final InsightNotificationService insightNotificationService;
    private final InsightProperties insightProperties;

    public InsightGenerationPipeline(
            SpendingPatternAnalyzer spendingPatternAnalyzer,
            AnomalyDetector anomalyDetector,
            TrendAnalyzer trendAnalyzer,
            CategoryInsightAnalyzer categoryInsightAnalyzer,
            AiEnhancementService aiEnhancementService,
            InsightRepository insightRepository,
            InsightNotificationService insightNotificationService,
            InsightProperties insightProperties) {
        this.spendingPatternAnalyzer = spendingPatternAnalyzer;
        this.anomalyDetector = anomalyDetector;
        this.trendAnalyzer = trendAnalyzer;
        this.categoryInsightAnalyzer = categoryInsightAnalyzer;
        this.aiEnhancementService = aiEnhancementService;
        this.insightRepository = insightRepository;
        this.insightNotificationService = insightNotificationService;
        this.insightProperties = insightProperties;
    }

    public int execute(UUID userId) {
        List<InsightCandidate> candidates = new ArrayList<>();

        candidates.addAll(spendingPatternAnalyzer.analyze(userId));
        candidates.addAll(anomalyDetector.analyze(userId));
        candidates.addAll(trendAnalyzer.analyze(userId));
        candidates.addAll(categoryInsightAnalyzer.analyze(userId));

        candidates = aiEnhancementService.enhance(candidates);

        List<InsightCandidate> deduplicated = deduplicate(userId, candidates);

        int maxInsights = insightProperties.maxInsightsPerGeneration();
        if (deduplicated.size() > maxInsights) {
            deduplicated = deduplicated.subList(0, maxInsights);
        }

        int savedCount = 0;
        for (InsightCandidate candidate : deduplicated) {
            InsightEntity entity = toEntity(userId, candidate);
            insightRepository.save(entity);
            savedCount++;

            if ("WARNING".equals(candidate.severity()) || "CRITICAL".equals(candidate.severity())) {
                createNotification(userId, candidate);
            }
        }

        log.info("Generated {} insights for user {}", savedCount, userId);
        return savedCount;
    }

    private List<InsightCandidate> deduplicate(UUID userId, List<InsightCandidate> candidates) {
        int windowDays = insightProperties.deduplicationWindowDays();
        OffsetDateTime since = OffsetDateTime.now().minusDays(windowDays);
        List<InsightCandidate> result = new ArrayList<>();

        for (InsightCandidate candidate : candidates) {
            boolean exists = insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                    userId, candidate.type(), candidate.title(), since);
            if (!exists) {
                result.add(candidate);
            }
        }

        return result;
    }

    private InsightEntity toEntity(UUID userId, InsightCandidate candidate) {
        return InsightEntity.builder()
                .userId(userId)
                .type(candidate.type())
                .title(candidate.title())
                .summary(candidate.summary())
                .detail(candidate.detail())
                .categoryId(candidate.categoryId())
                .severity(candidate.severity())
                .metadata(candidate.metadata())
                .read(false)
                .dismissed(false)
                .build();
    }

    private void createNotification(UUID userId, InsightCandidate candidate) {
        try {
            insightNotificationService.createInsightGeneratedNotification(userId, candidate);
        } catch (RuntimeException ex) {
            log.warn("Failed to create notification for insight of user {}", userId, ex);
        }
    }
}
