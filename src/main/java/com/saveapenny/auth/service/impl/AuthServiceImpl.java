package com.saveapenny.auth.service.impl;

import com.saveapenny.auth.dto.AuthTokenResponse;
import com.saveapenny.auth.dto.LoginRequest;
import com.saveapenny.auth.dto.LogoutRequest;
import com.saveapenny.auth.dto.RefreshTokenRequest;
import com.saveapenny.auth.dto.RefreshTokenResponse;
import com.saveapenny.auth.dto.RegisterRequest;
import com.saveapenny.auth.entity.RefreshToken;
import com.saveapenny.auth.exception.EmailAlreadyExistsException;
import com.saveapenny.auth.exception.InvalidCredentialsException;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RoleNotFoundException;
import com.saveapenny.auth.mapper.AuthMapper;
import com.saveapenny.auth.service.AuthService;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.auth.service.RefreshTokenService;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.entity.UserRole;
import com.saveapenny.user.entity.UserRoleId;
import com.saveapenny.user.repository.RoleRepository;
import com.saveapenny.user.repository.UserRepository;
import com.saveapenny.user.repository.UserRoleRepository;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authMapper = authMapper;
    }

    @Override
    public AuthTokenResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .active(true)
                .build();
        User savedUser = userRepository.save(user);

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new RoleNotFoundException(DEFAULT_ROLE));

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();
        userRoleRepository.save(userRole);
        savedUser.getUserRoles().add(userRole);

        String accessToken = jwtService.generateAccessToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.create(savedUser);

        return authMapper.toAuthTokenResponse(accessToken, refreshToken.getToken(), jwtService.getAccessTokenExpirySeconds());
    }

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return authMapper.toAuthTokenResponse(accessToken, refreshToken.getToken(), jwtService.getAccessTokenExpirySeconds());
    }

    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken rotatedToken = refreshTokenService.rotate(request.getRefreshToken());
        User user = userRepository.findById(rotatedToken.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        String accessToken = jwtService.generateAccessToken(user);
        return authMapper.toRefreshTokenResponse(accessToken, jwtService.getAccessTokenExpirySeconds());
    }

    @Override
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
