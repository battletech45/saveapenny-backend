package com.saveapenny.push.dto;

import com.saveapenny.push.entity.DevicePlatform;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenResponse {

    private UUID id;
    private String token;
    private DevicePlatform platform;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastSeenAt;
}
