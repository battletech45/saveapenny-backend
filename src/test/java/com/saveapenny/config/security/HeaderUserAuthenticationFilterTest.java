package com.saveapenny.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saveapenny.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class HeaderUserAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private HeaderUserAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthentication_whenTokenValid() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(userId, ((CurrentUserPrincipal) auth.getPrincipal()).userId());
    }

    @Test
    void returns401_whenTokenInvalid() throws Exception {
        String token = "invalid-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isAccessTokenValid(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verifyNoInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void continuesChain_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void continuesChain_whenNonBearerHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic some-credentials");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void returns401_whenJwtServiceThrowsException() throws Exception {
        String token = "malformed-jwt";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isAccessTokenValid(token)).thenThrow(new RuntimeException("JWT parse error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void continuesChain_whenAlreadyAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new CurrentUserPrincipal(UUID.randomUUID()), null, java.util.Collections.emptyList()));

        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void returns401_whenEmptyBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(jwtService.isAccessTokenValid("")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verifyNoInteractions(filterChain);
    }
}
