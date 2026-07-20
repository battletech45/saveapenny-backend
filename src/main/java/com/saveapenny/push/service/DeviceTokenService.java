package com.saveapenny.push.service;

import com.saveapenny.push.entity.DevicePlatform;
import java.util.UUID;

public interface DeviceTokenService {

    void register(UUID userId, String token, DevicePlatform platform);

    void unregister(UUID userId, String token);
}
