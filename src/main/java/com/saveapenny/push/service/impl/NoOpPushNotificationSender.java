package com.saveapenny.push.service.impl;

import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.push.service.PushNotificationSender;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "push.fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushNotificationSender implements PushNotificationSender {

    @Override
    public void send(UUID userId, NotificationType type, String title, String message, Map<String, String> data) {
    }
}
