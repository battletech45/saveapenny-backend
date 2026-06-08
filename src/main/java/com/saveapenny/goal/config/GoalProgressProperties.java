package com.saveapenny.goal.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "goal.progress")
public record GoalProgressProperties(
        BigDecimal offTrackRatio,
        BigDecimal atRiskRatio,
        int offTrackPersistenceMonths,
        String cron) {}
