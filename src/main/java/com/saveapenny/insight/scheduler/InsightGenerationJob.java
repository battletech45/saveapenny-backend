package com.saveapenny.insight.scheduler;

import com.saveapenny.insight.config.InsightProperties;
import com.saveapenny.insight.service.impl.InsightGenerationPipeline;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        List<User> allUsers = userRepository.findAll();
        log.info("Starting insight generation for {} users", allUsers.size());

        for (User user : allUsers) {
            try {
                UUID userId = user.getId();
                int count = insightGenerationPipeline.execute(userId);
                log.debug("Generated {} insights for user {}", count, userId);
            } catch (RuntimeException ex) {
                log.warn("Failed to generate insights for user {}", user.getId(), ex);
            }
        }
    }
}
