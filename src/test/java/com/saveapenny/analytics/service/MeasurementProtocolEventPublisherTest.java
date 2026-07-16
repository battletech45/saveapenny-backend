package com.saveapenny.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.saveapenny.analytics.AnalyticsClientIdFilter;
import com.saveapenny.analytics.config.AnalyticsProperties;
import com.saveapenny.analytics.dto.AnalyticsEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MeasurementProtocolEventPublisherTest {

    private final AnalyticsClientIdFilter clientIdFilter = new AnalyticsClientIdFilter();

    private static AnalyticsProperties properties(boolean validateOnly) {
        return properties(
                validateOnly,
                "1:1234567890:android:abcdef", "android-secret",
                "1:1234567890:ios:abcdef", "ios-secret");
    }

    private static AnalyticsProperties properties(
            boolean validateOnly, String androidAppId, String androidApiSecret, String iosAppId, String iosApiSecret) {
        return new AnalyticsProperties(
                true,
                androidAppId,
                androidApiSecret,
                iosAppId,
                iosApiSecret,
                "https://www.google-analytics.com/mp/collect",
                "https://www.google-analytics.com/debug/mp/collect",
                validateOnly,
                2000);
    }

    private static MeasurementProtocolEventPublisher publisher(RestClient restClient, AnalyticsProperties properties, MeterRegistry meterRegistry) {
        MeasurementProtocolEventPublisher publisher = new MeasurementProtocolEventPublisher(restClient, properties, meterRegistry);
        publisher.validateCredentials();
        return publisher;
    }

    /** Runs {@code action} with the given platform header populated in MDC, exactly as a real request would via {@link AnalyticsClientIdFilter}. */
    private void withPlatform(String platform, Runnable action) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (platform != null) {
            request.addHeader(AnalyticsClientIdFilter.PLATFORM_HEADER, platform);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> action.run();
        clientIdFilter.doFilter(request, response, chain);
    }

    @Test
    void sendsAndroidEventWithAndroidCredentials() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AnalyticsProperties properties = properties(false);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        server.expect(requestTo("https://www.google-analytics.com/mp/collect?firebase_app_id=1:1234567890:android:abcdef&api_secret=android-secret"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        withPlatform("android", () ->
                publisher(restClient, properties, meterRegistry).publish(new AnalyticsEvent("goal_achieved", Map.of("goal_id", "g-1"))));

        server.verify();
        assertEquals(1.0, meterRegistry.counter("analytics.events.published", "event_name", "goal_achieved").count());
    }

    @Test
    void sendsIosEventWithIosCredentials() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AnalyticsProperties properties = properties(false);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        server.expect(requestTo("https://www.google-analytics.com/mp/collect?firebase_app_id=1:1234567890:ios:abcdef&api_secret=ios-secret"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        withPlatform("ios", () -> publisher(restClient, properties, meterRegistry).publish(new AnalyticsEvent("goal_achieved")));

        server.verify();
    }

    @Test
    void dropsEventWhenPlatformUnknownWithoutCallingRestClient() throws Exception {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestClient restClient = RestClient.builder().build();
        AnalyticsProperties properties = properties(false);

        withPlatform(null, () -> publisher(restClient, properties, meterRegistry).publish(new AnalyticsEvent("goal_off_track")));

        assertEquals(1.0, meterRegistry.counter("analytics.events.failed", "event_name", "goal_off_track", "reason", "unknown_platform").count());
    }

    @Test
    void routesToDebugEndpointWhenValidateOnly() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AnalyticsProperties properties = properties(true);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        server.expect(requestTo("https://www.google-analytics.com/debug/mp/collect?firebase_app_id=1:1234567890:android:abcdef&api_secret=android-secret"))
                .andRespond(withStatus(HttpStatus.OK));

        withPlatform("android", () -> publisher(restClient, properties, meterRegistry).publish(new AnalyticsEvent("goal_achieved")));

        server.verify();
    }

    @Test
    void swallowsFailuresWithoutThrowingAndCountsFailure() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AnalyticsProperties properties = properties(false);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        server.expect(requestTo("https://www.google-analytics.com/mp/collect?firebase_app_id=1:1234567890:android:abcdef&api_secret=android-secret"))
                .andRespond(withServerError());

        withPlatform("android", () -> publisher(restClient, properties, meterRegistry).publish(new AnalyticsEvent("goal_achieved")));

        server.verify();
        assertEquals(1.0, meterRegistry.counter("analytics.events.failed", "event_name", "goal_achieved", "reason", "http_error").count());
    }

    @Test
    void validateCredentialsRejectsBlankAndroidAppId() {
        MeasurementProtocolEventPublisher publisher = new MeasurementProtocolEventPublisher(
                RestClient.builder().build(),
                properties(false, "", "android-secret", "1:1234567890:ios:abcdef", "ios-secret"),
                new SimpleMeterRegistry());

        assertThrows(IllegalStateException.class, publisher::validateCredentials);
    }

    @Test
    void validateCredentialsRejectsBlankIosApiSecret() {
        MeasurementProtocolEventPublisher publisher = new MeasurementProtocolEventPublisher(
                RestClient.builder().build(),
                properties(false, "1:1234567890:android:abcdef", "android-secret", "1:1234567890:ios:abcdef", ""),
                new SimpleMeterRegistry());

        assertThrows(IllegalStateException.class, publisher::validateCredentials);
    }
}
