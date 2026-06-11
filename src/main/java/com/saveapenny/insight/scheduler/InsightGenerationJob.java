package com.saveapenny.insight.scheduler;

import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.service.impl.InsightGenerationPipeline;
import com.saveapenny.user.repository.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InsightGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(InsightGenerationJob.class);

    private final InsightProperties insightProperties;
    private final InsightGenerationPipeline insightGenerationPipeline;
    private final UserRepository userRepository;

    public InsightGenerationJob(
            InsightProperties insightProperties,
            InsightGenerationPipeline insightGenerationPipeline,
            UserRepository userRepository) {
        this.insightProperties = insightProperties;
        this.insightGenerationPipeline = insightGenerationPipeline;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${insight.cron:0 30 6 * * *}")
    public void generateInsightsForAllUsers() {
        if (!insightProperties.enabled()) {
            log.info("Insight generation is disabled");
            return;
        }

        int pageNumber = 0;
        Page<UUID> page;
        do {
            page = userRepository.findAllUserIds(PageRequest.of(pageNumber, 100));
            if (pageNumber == 0) {
                log.info("Starting insight generation for {} users", page.getTotalElements());
            }
            for (UUID userId : page.getContent()) {
                try {
                    int count = insightGenerationPipeline.execute(userId);
                    log.debug("Generated {} insights for user {}", count, userId);
                } catch (RuntimeException ex) {
                    log.warn("Failed to generate insights for user {}", userId, ex);
                }
            }
            pageNumber++;
        } while (page.hasNext());
    }
}
