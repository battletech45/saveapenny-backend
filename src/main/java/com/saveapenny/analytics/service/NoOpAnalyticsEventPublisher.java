package com.saveapenny.analytics.service;

import com.saveapenny.analytics.dto.AnalyticsEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "firebase.analytics", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAnalyticsEventPublisher implements AnalyticsEventPublisher {

    @Override
    public void publish(AnalyticsEvent event) {
    }
}
