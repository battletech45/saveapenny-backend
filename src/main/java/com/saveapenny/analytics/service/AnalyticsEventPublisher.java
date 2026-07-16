package com.saveapenny.analytics.service;

import com.saveapenny.analytics.dto.AnalyticsEvent;

public interface AnalyticsEventPublisher {
    void publish(AnalyticsEvent event);
}
