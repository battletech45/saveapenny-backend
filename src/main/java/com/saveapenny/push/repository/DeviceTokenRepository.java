package com.saveapenny.push.repository;

import com.saveapenny.push.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserId(UUID userId);

    void deleteByUserIdAndToken(UUID userId, String token);
}
