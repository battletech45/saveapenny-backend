package com.saveapenny.insight.service.impl;

import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.config.InsightProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiEnhancementService {

    private static final Logger log = LoggerFactory.getLogger(AiEnhancementService.class);

    private final InsightProperties insightProperties;

    public AiEnhancementService(InsightProperties insightProperties) {
        this.insightProperties = insightProperties;
    }

    public List<InsightCandidate> enhance(List<InsightCandidate> candidates) {
        if (!insightProperties.aiEnhanced()) {
            return candidates;
        }

        log.info("AI enhancement not yet implemented; returning raw candidates");

        return candidates;
    }
}
