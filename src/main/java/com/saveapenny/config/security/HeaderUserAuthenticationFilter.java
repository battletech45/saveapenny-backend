package com.saveapenny.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.shared.api.ApiError;
import com.saveapenny.shared.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HeaderUserAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public HeaderUserAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null
                && authHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                UUID userId = jwtService.extractUserId(token);
                if (userId == null) {
                    writeUnauthorizedResponse(response);
                    return;
                }

                CurrentUserPrincipal principal = new CurrentUserPrincipal(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ex) {
                writeUnauthorizedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        ApiError error = ApiError.builder()
                .code("ACCESS_DENIED")
                .message("Invalid or expired access token.")
                .details(List.of())
                .build();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(error));
    }
}
