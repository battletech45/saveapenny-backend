package com.saveapenny.push.service;

import com.saveapenny.notification.entity.NotificationType;
import java.util.Map;
import java.util.UUID;

public interface PushNotificationSender {

    void send(UUID userId, NotificationType type, String title, String message, Map<String, String> data);
}
