package com.saveapenny.push.service.impl;

import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.push.config.PushProperties;
import com.saveapenny.push.entity.DeviceToken;
import com.saveapenny.push.repository.DeviceTokenRepository;
import com.saveapenny.push.service.GoogleServiceAccountTokenProvider;
import com.saveapenny.push.service.PushNotificationSender;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Sends pushes via the FCM HTTP v1 API, authenticating with the OAuth2 access token
 * from {@link GoogleServiceAccountTokenProvider}. One request per registered device
 * token so a single dead token never blocks delivery to a user's other devices.
 */
@Service
@ConditionalOnProperty(prefix = "push.fcm", name = "enabled", havingValue = "true")
public class FcmPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationSender.class);

    private final RestClient pushRestClient;
    private final GoogleServiceAccountTokenProvider tokenProvider;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushProperties properties;
    private final MeterRegistry meterRegistry;

    public FcmPushNotificationSender(
            RestClient pushRestClient,
            GoogleServiceAccountTokenProvider tokenProvider,
            DeviceTokenRepository deviceTokenRepository,
            PushProperties properties,
            MeterRegistry meterRegistry) {
        this.pushRestClient = pushRestClient;
        this.tokenProvider = tokenProvider;
        this.deviceTokenRepository = deviceTokenRepository;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void validateCredentials() {
        boolean configured = StringUtils.hasText(properties.projectId())
                && StringUtils.hasText(properties.clientEmail())
                && StringUtils.hasText(properties.privateKey());
        if (!configured) {
            throw new IllegalStateException(
                    "push.fcm.enabled=true requires project-id, client-email and private-key "
                            + "(PUSH_FCM_PROJECT_ID / PUSH_FCM_CLIENT_EMAIL / PUSH_FCM_PRIVATE_KEY) to be set");
        }
    }

    @Override
    @Async("pushTaskExecutor")
    public void send(UUID userId, NotificationType type, String title, String message, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            return;
        }

        String endpoint = properties.fcmEndpointTemplate().formatted(properties.projectId());
        for (DeviceToken deviceToken : tokens) {
            sendToToken(endpoint, deviceToken, type, title, message, data);
        }
    }

    private void sendToToken(
            String endpoint, DeviceToken deviceToken, NotificationType type, String title, String message, Map<String, String> data) {
        Map<String, Object> payload = Map.of(
                "message", Map.of(
                        "token", deviceToken.getToken(),
                        "notification", Map.of("title", title, "body", message),
                        "data", data,
                        "android", Map.of("priority", "HIGH"),
                        "apns", Map.of(
                                "headers", Map.of("apns-priority", "10"),
                                "payload", Map.of("aps", Map.of("sound", "default")))));

        try {
            pushRestClient.post()
                    .uri(endpoint)
                    .headers(headers -> {
                        headers.setBearerAuth(tokenProvider.getAccessToken());
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            meterRegistry.counter("push.notifications.sent", "type", type.name()).increment();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                // FCM returns 404 UNREGISTERED for tokens that are no longer valid (uninstall,
                // token rotation the app never re-registered) — nothing will ever succeed for
                // this token again, so drop it rather than retry it on every future notification.
                deviceTokenRepository.delete(deviceToken);
                meterRegistry.counter("push.notifications.failed", "type", type.name(), "reason", "unregistered").increment();
                log.info("push_token_removed userId={} reason=unregistered", deviceToken.getUserId());
            } else {
                meterRegistry.counter("push.notifications.failed", "type", type.name(), "reason", "http_error").increment();
                log.warn("push_notification_send_failed userId={} status={}", deviceToken.getUserId(), ex.getStatusCode());
            }
        } catch (Exception ex) {
            meterRegistry.counter("push.notifications.failed", "type", type.name(), "reason", "error").increment();
            log.warn("push_notification_send_failed userId={} error={}", deviceToken.getUserId(), ex.getMessage());
        }
    }
}
