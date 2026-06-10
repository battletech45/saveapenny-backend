package com.saveapenny.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import com.saveapenny.user.dto.ChangePasswordRequest;
import com.saveapenny.user.dto.UpdateUserProfileRequest;
import com.saveapenny.user.dto.UserProfileResponse;
import com.saveapenny.user.exception.InvalidPasswordException;
import com.saveapenny.user.service.UserService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUpRateLimitingFilter() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void getCurrentUser_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("john@example.com")
                .fullName("John Doe")
                .active(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        when(userService.getCurrentUser(userId)).thenReturn(response);
        when(jwtService.isAccessTokenValid("token-1")).thenReturn(true);
        when(jwtService.extractUserId("token-1")).thenReturn(userId);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void updateCurrentUserProfile_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder().fullName("Jane Doe").build();
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("john@example.com")
                .fullName("Jane Doe")
                .active(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        when(userService.updateCurrentUserProfile(eq(userId), any(UpdateUserProfileRequest.class))).thenReturn(response);
        when(jwtService.isAccessTokenValid("token-2")).thenReturn(true);
        when(jwtService.extractUserId("token-2")).thenReturn(userId);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer token-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Jane Doe"));
    }

    @Test
    void changeCurrentUserPassword_returnsOkEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("current-credential")
                .newPassword("Next@123")
                .build();

        doNothing().when(userService).changeCurrentUserPassword(eq(userId), any(ChangePasswordRequest.class));
        when(jwtService.isAccessTokenValid("token-3")).thenReturn(true);
        when(jwtService.extractUserId("token-3")).thenReturn(userId);

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer token-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void changeCurrentUserPassword_returnsBadRequest_whenPasswordInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("correct-password")
                .newPassword("Str0ng!NewPass")
                .build();

        doThrow(new InvalidPasswordException())
                .when(userService).changeCurrentUserPassword(eq(userId), any(ChangePasswordRequest.class));
        when(jwtService.isAccessTokenValid("token-err-1")).thenReturn(true);
        when(jwtService.extractUserId("token-err-1")).thenReturn(userId);

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer token-err-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"));
    }

    @Test
    void getCurrentUser_returnsUnauthorized_whenAuthContextMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }
}
