package com.saveapenny.push.service.impl;

import com.saveapenny.push.entity.DevicePlatform;
import com.saveapenny.push.entity.DeviceToken;
import com.saveapenny.push.repository.DeviceTokenRepository;
import com.saveapenny.push.service.DeviceTokenService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenServiceImpl(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public void register(UUID userId, String token, DevicePlatform platform) {
        DeviceToken deviceToken = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        deviceToken.setUserId(userId);
        deviceToken.setPlatform(platform);
        deviceToken.setLastSeenAt(OffsetDateTime.now());
        deviceTokenRepository.save(deviceToken);
    }

    @Override
    public void unregister(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
