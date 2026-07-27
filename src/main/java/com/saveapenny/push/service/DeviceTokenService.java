package com.saveapenny.push.service;

import com.saveapenny.push.entity.DevicePlatform;
import com.saveapenny.push.entity.DeviceToken;
import java.util.List;
import java.util.UUID;

public interface DeviceTokenService {

    void register(UUID userId, String token, DevicePlatform platform);

    void unregister(UUID userId, String token);

    List<DeviceToken> getAllForUser(UUID userId);
}
