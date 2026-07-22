package com.saveapenny.push.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.push.config.PushProperties;
import com.saveapenny.push.entity.DevicePlatform;
import com.saveapenny.push.entity.DeviceToken;
import com.saveapenny.push.repository.DeviceTokenRepository;
import com.saveapenny.push.service.GoogleServiceAccountTokenProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationSenderTest {

    private static final String ENDPOINT = "https://fcm.googleapis.com/v1/projects/proj-1/messages:send";

    @Mock
    private GoogleServiceAccountTokenProvider tokenProvider;
    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private static PushProperties properties() {
        return new PushProperties(
                true,
                "proj-1",
                "svc@proj-1.iam.gserviceaccount.com",
                "dummy-key",
                "https://oauth2.googleapis.com/token",
                "https://fcm.googleapis.com/v1/projects/%s/messages:send",
                5000);
    }

    private FcmPushNotificationSender sender(RestClient restClient, MeterRegistry meterRegistry) {
        FcmPushNotificationSender sender = new FcmPushNotificationSender(
                restClient, tokenProvider, deviceTokenRepository, properties(), meterRegistry);
        sender.validateCredentials();
        return sender;
    }

    private DeviceToken deviceToken(UUID userId, String token) {
        return DeviceToken.builder().id(UUID.randomUUID()).userId(userId).token(token).platform(DevicePlatform.ANDROID).build();
    }

    @Test
    void sendsOneRequestPerRegisteredDeviceToken() {
        UUID userId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        when(tokenProvider.getAccessToken()).thenReturn("access-token");
        when(deviceTokenRepository.findAllByUserId(userId))
                .thenReturn(List.of(deviceToken(userId, "token-a"), deviceToken(userId, "token-b")));

        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

        sender(restClient, meterRegistry)
                .send(userId, NotificationType.GOAL_OFF_TRACK, "Title", "Body", Map.of("goalId", "g-1"));

        server.verify();
        assertEquals(2.0, meterRegistry.counter("push.notifications.sent", "type", "GOAL_OFF_TRACK").count());
    }

    @Test
    void removesTokenWhenFcmReportsUnregistered() {
        UUID userId = UUID.randomUUID();
        DeviceToken staleToken = deviceToken(userId, "stale-token");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        when(tokenProvider.getAccessToken()).thenReturn("access-token");
        when(deviceTokenRepository.findAllByUserId(userId)).thenReturn(List.of(staleToken));

        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        sender(restClient, meterRegistry).send(userId, NotificationType.SYSTEM, "Title", "Body", Map.of());

        server.verify();
        verify(deviceTokenRepository).delete(staleToken);
        assertEquals(1.0, meterRegistry.counter("push.notifications.failed", "type", "SYSTEM", "reason", "unregistered").count());
    }

    @Test
    void doesNothingWhenUserHasNoRegisteredDevices() {
        UUID userId = UUID.randomUUID();
        RestClient restClient = RestClient.builder().build();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        when(deviceTokenRepository.findAllByUserId(userId)).thenReturn(List.of());

        sender(restClient, meterRegistry).send(userId, NotificationType.SYSTEM, "Title", "Body", Map.of());

        verifyNoInteractions(tokenProvider);
    }

    @Test
    void validateCredentialsRejectsMissingProjectId() {
        FcmPushNotificationSender sender = new FcmPushNotificationSender(
                RestClient.builder().build(),
                tokenProvider,
                deviceTokenRepository,
                new PushProperties(true, "", "svc@proj-1.iam.gserviceaccount.com", "dummy-key",
                        "https://oauth2.googleapis.com/token", "https://fcm.googleapis.com/v1/projects/%s/messages:send", 5000),
                new SimpleMeterRegistry());

        assertThrows(IllegalStateException.class, sender::validateCredentials);
    }
}
