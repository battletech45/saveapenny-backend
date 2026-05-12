package com.saveapenny.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.user.entity.Role;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.entity.UserRole;
import com.saveapenny.user.entity.UserRoleId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl("0123456789012345678901234567890123456789012345678901234567890123");

        UUID userId = UUID.randomUUID();
        Role role = Role.builder().id(UUID.randomUUID()).name("ROLE_USER").build();
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(userId, role.getId()))
                .role(role)
                .build();

        user = User.builder()
                .id(userId)
                .email("john@example.com")
                .userRoles(Set.of(userRole))
                .build();
    }

    @Test
    void generateAndValidateToken_returnsUsableToken() {
        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isAccessTokenValid(token));
        assertEquals(user.getId(), jwtService.extractUserId(token));
        assertEquals("john@example.com", jwtService.extractEmail(token));
        assertEquals(900L, jwtService.getAccessTokenExpirySeconds());
    }

    @Test
    void isAccessTokenValid_returnsFalseForInvalidToken() {
        assertFalse(jwtService.isAccessTokenValid("not-a-jwt-token"));
    }

    @Test
    void isAccessTokenValid_returnsFalseForTokenSignedWithDifferentKey() {
        JwtServiceImpl otherJwtService =
                new JwtServiceImpl("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        String token = otherJwtService.generateAccessToken(user);

        assertFalse(jwtService.isAccessTokenValid(token));
    }
}
