package com.saveapenny.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.shared.api.ApiError;
import com.saveapenny.shared.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equalsIgnoreCase(method) || !path.startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        int limit = resolveLimit(path);

        if (!rateLimiter.tryConsume(key, limit)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiError error = ApiError.builder()
                    .code("RATE_LIMITED")
                    .message("Too many requests. Try again later.")
                    .details(List.of())
                    .build();
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        if (LOGIN_PATH.equals(request.getRequestURI())) {
            return "login:" + getClientIp(request);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUserPrincipal principal) {
            return "api:" + principal.userId();
        }
        return "api:" + getClientIp(request);
    }

    private int resolveLimit(String path) {
        if (LOGIN_PATH.equals(path)) {
            return properties.login().maxPerMinute();
        }
        return properties.api().maxPerMinute();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfwh = request.getHeader("X-Forwarded-For");
        if (xfwh != null && !xfwh.isBlank()) {
            return xfwh.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
