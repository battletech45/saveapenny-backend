package com.saveapenny.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.saveapenny.auth.mapper.AuthMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.auth.service.RefreshTokenService;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.entity.UserRole;
import com.saveapenny.user.entity.UserRoleId;
import com.saveapenny.user.repository.RoleRepository;
import com.saveapenny.user.repository.UserRepository;
import com.saveapenny.user.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("john@example.com")
                .passwordHash("hashed-password")
                .fullName("John Doe")
                .active(true)
                .userRoles(Set.of())
                .build();
    }

    @Test
    void register_returnsAuthTokenResponse_whenSuccessful() {
        RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .password("strong-pass")
                .fullName("John Doe")
                .build();
        Role role = Role.builder().id(UUID.randomUUID()).name("ROLE_USER").build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token("refresh-token")
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        AuthTokenResponse mapped = AuthTokenResponse.builder().accessToken("access-token").build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-pass")).thenReturn("hashed-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User value = invocation.getArgument(0);
            value.setId(userId);
            return value;
        });
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.create(any(User.class))).thenReturn(refreshToken);
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(authMapper.toAuthTokenResponse("access-token", "refresh-token", 900L)).thenReturn(mapped);

        AuthTokenResponse result = authService.register(request);

        assertEquals("access-token", result.getAccessToken());
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void register_throws_whenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .password("strong-pass")
                .fullName("John Doe")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_returnsAuthTokenResponse_whenCredentialsValid() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("plain-password")
                .build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token("refresh-token")
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        AuthTokenResponse mapped = AuthTokenResponse.builder().accessToken("access-token").build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.create(user)).thenReturn(refreshToken);
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(authMapper.toAuthTokenResponse("access-token", "refresh-token", 900L)).thenReturn(mapped);

        AuthTokenResponse result = authService.login(request);

        assertEquals("access-token", result.getAccessToken());
    }

    @Test
    void login_throws_whenPasswordInvalid() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("wrong-password")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refresh_returnsAccessTokenResponse_whenRefreshTokenValid() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("valid-refresh").build();
        RefreshToken rotatedToken = RefreshToken.builder().userId(userId).token("rotated").build();
        RefreshTokenResponse mapped = RefreshTokenResponse.builder().accessToken("new-access").build();
        User userForRefresh = User.builder().id(userId).email("john@example.com").userRoles(Set.of()).build();

        when(refreshTokenService.rotate("valid-refresh")).thenReturn(rotatedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userForRefresh));
        when(jwtService.generateAccessToken(userForRefresh)).thenReturn("new-access");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(authMapper.toRefreshTokenResponse("new-access", 900L)).thenReturn(mapped);

        RefreshTokenResponse result = authService.refresh(request);

        assertEquals("new-access", result.getAccessToken());
    }

    @Test
    void refresh_throws_whenUserNotFoundForRotatedToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("valid-refresh").build();
        RefreshToken rotatedToken = RefreshToken.builder().userId(userId).token("rotated").build();

        when(refreshTokenService.rotate("valid-refresh")).thenReturn(rotatedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh(request));
    }

    @Test
    void logout_revokesRefreshToken() {
        LogoutRequest request = LogoutRequest.builder().refreshToken("refresh-token").build();

        authService.logout(request);

        verify(refreshTokenService).revoke("refresh-token");
    }
}
