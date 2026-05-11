package com.saveapenny.user.service;

import com.saveapenny.user.dto.ChangePasswordRequest;
import com.saveapenny.user.dto.UpdateUserProfileRequest;
import com.saveapenny.user.dto.UserProfileResponse;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getCurrentUser(UUID currentUserId);

    UserProfileResponse updateCurrentUserProfile(UUID currentUserId, UpdateUserProfileRequest request);

    void changeCurrentUserPassword(UUID currentUserId, ChangePasswordRequest request);
}
