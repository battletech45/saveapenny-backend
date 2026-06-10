package com.saveapenny.config.security;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private RateLimitProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RateLimitingFilter filter;

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
    void skipsRateLimiting_whenNotPostMethod() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/accounts");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void skipsRateLimiting_whenNotApiPath() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void appliesLoginRateLimit_whenLoginPath() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        var loginLimit = new RateLimitProperties.Login(5);
        when(properties.login()).thenReturn(loginLimit);
        when(rateLimiter.tryConsume("login:192.168.1.1", 5)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void returns429_whenRateLimited() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/transactions");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        var apiLimit = new RateLimitProperties.Api(60);
        when(properties.api()).thenReturn(apiLimit);
        when(rateLimiter.tryConsume("api:10.0.0.1", 60)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void usesXForwardedFor_whenPresent() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/transactions");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");

        var apiLimit = new RateLimitProperties.Api(60);
        when(properties.api()).thenReturn(apiLimit);
        when(rateLimiter.tryConsume("api:203.0.113.5", 60)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usesUserId_whenAuthenticated() throws Exception {
        var userId = java.util.UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new CurrentUserPrincipal(userId), null, java.util.Collections.emptyList()));

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/budgets");

        var apiLimit = new RateLimitProperties.Api(60);
        when(properties.api()).thenReturn(apiLimit);
        when(rateLimiter.tryConsume("api:" + userId, 60)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
