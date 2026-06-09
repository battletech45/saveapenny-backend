package com.saveapenny.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.dto.UpdateNotificationRequest;
import com.saveapenny.notification.dto.UnreadNotificationCountResponse;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.exception.NotificationNotFoundException;
import com.saveapenny.notification.service.NotificationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

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
    void create_returnsCreatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n1")).thenReturn(true);
        when(jwtService.extractUserId("token-n1")).thenReturn(userId);
        when(notificationService.create(eq(userId), any(CreateNotificationRequest.class))).thenReturn(sampleResponse());

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .type(NotificationType.SYSTEM)
                .title("System")
                .message("Hello")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer token-n1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("SYSTEM"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n2")).thenReturn(true);
        when(jwtService.extractUserId("token-n2")).thenReturn(userId);
        when(notificationService.getAll(eq(userId), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("read", "false")
                        .header("Authorization", "Bearer token-n2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("System"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n3")).thenReturn(true);
        when(jwtService.extractUserId("token-n3")).thenReturn(userId);
        when(notificationService.getById(userId, notificationId)).thenThrow(new NotificationNotFoundException(notificationId));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notificationId)
                        .header("Authorization", "Bearer token-n3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void getUnreadCount_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n4")).thenReturn(true);
        when(jwtService.extractUserId("token-n4")).thenReturn(userId);
        when(notificationService.getUnreadCount(userId)).thenReturn(UnreadNotificationCountResponse.builder().unreadCount(3).build());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer token-n4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void markAllAsRead_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n5")).thenReturn(true);
        when(jwtService.extractUserId("token-n5")).thenReturn(userId);
        doNothing().when(notificationService).markAllAsRead(userId);

        mockMvc.perform(patch("/api/v1/notifications/mark-all-read")
                        .header("Authorization", "Bearer token-n5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n6")).thenReturn(true);
        when(jwtService.extractUserId("token-n6")).thenReturn(userId);
        when(notificationService.update(eq(userId), eq(notificationId), any(UpdateNotificationRequest.class)))
                .thenReturn(sampleResponse());

        UpdateNotificationRequest request = UpdateNotificationRequest.builder().read(true).build();

        mockMvc.perform(put("/api/v1/notifications/{notificationId}", notificationId)
                        .header("Authorization", "Bearer token-n6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-n7")).thenReturn(true);
        when(jwtService.extractUserId("token-n7")).thenReturn(userId);
        doNothing().when(notificationService).delete(userId, notificationId);

        mockMvc.perform(delete("/api/v1/notifications/{notificationId}", notificationId)
                        .header("Authorization", "Bearer token-n7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private NotificationResponse sampleResponse() {
        return NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.SYSTEM)
                .title("System")
                .message("Hello")
                .read(false)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
