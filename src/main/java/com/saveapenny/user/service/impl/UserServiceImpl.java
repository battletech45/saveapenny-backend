package com.saveapenny.user.service.impl;

import com.saveapenny.auth.service.RefreshTokenService;
import com.saveapenny.user.dto.ChangePasswordRequest;
import com.saveapenny.user.dto.UpdateUserProfileRequest;
import com.saveapenny.user.dto.UserProfileResponse;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.exception.InvalidPasswordException;
import com.saveapenny.user.exception.PasswordReuseNotAllowedException;
import com.saveapenny.user.exception.UserNotFoundException;
import com.saveapenny.user.mapper.UserMapper;
import com.saveapenny.user.repository.UserRepository;
import com.saveapenny.user.service.UserService;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        return userMapper.toUserProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UUID currentUserId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        user.setFullName(request.getFullName());
        User savedUser = userRepository.save(user);
        return userMapper.toUserProfileResponse(savedUser);
    }

    @Override
    public void changeCurrentUserPassword(UUID currentUserId, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new PasswordReuseNotAllowedException();
        }

        refreshTokenService.revokeAllByUser(user);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
