package com.saveapenny.analytics.service;

import com.saveapenny.analytics.AnalyticsClientIdFilter;
import com.saveapenny.analytics.config.AnalyticsProperties;
import com.saveapenny.analytics.dto.AnalyticsEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(prefix = "firebase.analytics", name = "enabled", havingValue = "true")
public class MeasurementProtocolEventPublisher implements AnalyticsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MeasurementProtocolEventPublisher.class);

    private final RestClient analyticsRestClient;
    private final AnalyticsProperties properties;
    private final MeterRegistry meterRegistry;

    public MeasurementProtocolEventPublisher(RestClient analyticsRestClient, AnalyticsProperties properties, MeterRegistry meterRegistry) {
        this.analyticsRestClient = analyticsRestClient;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void validateCredentials() {
        boolean androidConfigured = StringUtils.hasText(properties.androidAppId()) && StringUtils.hasText(properties.androidApiSecret());
        boolean iosConfigured = StringUtils.hasText(properties.iosAppId()) && StringUtils.hasText(properties.iosApiSecret());
        if (!androidConfigured || !iosConfigured) {
            throw new IllegalStateException(
                    "firebase.analytics.enabled=true requires both Android (FIREBASE_ANDROID_APP_ID / FIREBASE_ANDROID_API_SECRET) "
                            + "and iOS (FIREBASE_IOS_APP_ID / FIREBASE_IOS_API_SECRET) credentials to be set");
        }
    }

    @Override
    @Async("analyticsTaskExecutor")
    public void publish(AnalyticsEvent event) {
        String platform = AnalyticsClientIdFilter.currentPlatform();
        Optional<PlatformCredentials> credentials = resolveCredentials(platform);
        if (credentials.isEmpty()) {
            // No X-Client-Platform on this request (e.g. a batch/scheduled job with no HTTP
            // context) — there is no safe app_id/api_secret pair to attribute the event to,
            // and sending it under the wrong platform's credentials would just be silently
            // rejected by Google anyway. Drop rather than guess.
            meterRegistry.counter("analytics.events.failed", "event_name", event.name(), "reason", "unknown_platform").increment();
            log.warn("analytics_event_dropped name={} reason=unknown_platform", event.name());
            return;
        }

        String clientId = AnalyticsClientIdFilter.currentClientId();
        String resolvedClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        String endpoint = properties.validateOnly() ? properties.debugEndpoint() : properties.endpoint();

        Map<String, Object> body = Map.of(
                "app_instance_id", resolvedClientId,
                "events", List.of(Map.of(
                        "name", event.name(),
                        "params", event.params())));

        java.net.URI uri = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("firebase_app_id", credentials.get().appId())
                .queryParam("api_secret", credentials.get().apiSecret())
                .build()
                .toUri();

        try {
            analyticsRestClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            meterRegistry.counter("analytics.events.published", "event_name", event.name()).increment();
        } catch (Exception ex) {
            meterRegistry.counter("analytics.events.failed", "event_name", event.name(), "reason", "http_error").increment();
            log.warn("analytics_event_publish_failed name={} reason={}", event.name(), ex.getMessage());
        }
    }

    private Optional<PlatformCredentials> resolveCredentials(String platform) {
        if ("android".equals(platform)) {
            return Optional.of(new PlatformCredentials(properties.androidAppId(), properties.androidApiSecret()));
        }
        if ("ios".equals(platform)) {
            return Optional.of(new PlatformCredentials(properties.iosAppId(), properties.iosApiSecret()));
        }
        return Optional.empty();
    }

    private record PlatformCredentials(String appId, String apiSecret) {
    }
}
