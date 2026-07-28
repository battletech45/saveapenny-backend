package com.saveapenny.push.service.impl;

import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.push.config.FirebaseServiceAccount;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FcmPushNotificationSender implements PushNotificationSender {

    private final RestClient pushRestClient;
    private final GoogleServiceAccountTokenProvider tokenProvider;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushProperties properties;
    private final FirebaseServiceAccount serviceAccount;
    private final MeterRegistry meterRegistry;

    public FcmPushNotificationSender(
            RestClient pushRestClient,
            GoogleServiceAccountTokenProvider tokenProvider,
            DeviceTokenRepository deviceTokenRepository,
            PushProperties properties,
            FirebaseServiceAccount serviceAccount,
            MeterRegistry meterRegistry) {
        this.pushRestClient = pushRestClient;
        this.tokenProvider = tokenProvider;
        this.deviceTokenRepository = deviceTokenRepository;
        this.properties = properties;
        this.serviceAccount = serviceAccount;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void validateCredentials() {
        boolean configured = StringUtils.hasText(serviceAccount.projectId())
                && StringUtils.hasText(serviceAccount.clientEmail())
                && StringUtils.hasText(serviceAccount.privateKey());
        if (!configured) {
            throw new IllegalStateException(
                    "push.fcm.enabled=true requires a service account JSON at push.fcm.credentials-path "
                            + "with project_id, client_email and private_key set");
        }
    }

    @Override
    @Async("pushTaskExecutor")
    public void send(UUID userId, NotificationType type, String title, String message, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            return;
        }

        String endpoint = properties.fcmEndpointTemplate().formatted(serviceAccount.projectId());
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
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                // FCM returns 404 UNREGISTERED for tokens that are no longer valid (uninstall,
                // token rotation the app never re-registered) — nothing will ever succeed for
                // this token again, so drop it rather than retry it on every future notification.
                deviceTokenRepository.delete(deviceToken);
                meterRegistry.counter("push.notifications.failed", "type", type.name(), "reason", "unregistered").increment();
                log.info("push_token_removed userId={} reason=unregistered", deviceToken.getUserId());
            } else {
                // 400 INVALID_ARGUMENT covers malformed requests (bad token format, bad payload
                // shape) as well as genuinely invalid tokens — unlike 404 it isn't a reliable
                // signal that the token is permanently dead, so keep it and surface the FCM
                // error body for diagnosis instead of silently deleting a possibly-valid token.
                meterRegistry.counter("push.notifications.failed", "type", type.name(), "reason", "http_error").increment();
                log.warn(
                        "push_notification_send_failed userId={} status={} body={}",
                        deviceToken.getUserId(),
                        ex.getStatusCode(),
                        ex.getResponseBodyAsString());
            }
        } catch (Exception ex) {
            meterRegistry.counter("push.notifications.failed", "type", type.name(), "reason", "error").increment();
            log.warn("push_notification_send_failed userId={} error={}", deviceToken.getUserId(), ex.getMessage());
        }
    }
}
