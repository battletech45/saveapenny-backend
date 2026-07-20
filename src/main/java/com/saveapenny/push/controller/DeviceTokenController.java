package com.saveapenny.push.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.push.dto.RegisterDeviceTokenRequest;
import com.saveapenny.push.service.DeviceTokenService;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/device-tokens")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Device Tokens", description = "Registers and unregisters FCM device tokens for push notification delivery.")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenService.register(getCurrentUserId(principal), request.getToken(), request.getPlatform());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unregister(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        deviceTokenService.unregister(getCurrentUserId(principal), token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
