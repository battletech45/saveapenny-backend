package com.saveapenny.user.controller;

import com.saveapenny.user.dto.ChangePasswordRequest;
import com.saveapenny.user.dto.UpdateUserProfileRequest;
import com.saveapenny.user.dto.UserProfileResponse;
import com.saveapenny.user.service.UserService;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.config.security.CurrentUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current user profile and password management endpoints.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(getCurrentUserId(principal))));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateCurrentUserProfile(getCurrentUserId(principal), request)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changeCurrentUserPassword(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeCurrentUserPassword(getCurrentUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private java.util.UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
