package com.saveapenny.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.shared.api.ApiError;
import com.saveapenny.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter,
            HeaderUserAuthenticationFilter headerUserAuthenticationFilter,
            ObjectMapper objectMapper)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeUnauthorizedResponse(response, objectMapper, "Unauthorized."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeUnauthorizedResponse(response, objectMapper, accessDeniedException.getMessage())))
                .addFilterBefore(headerUserAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, HeaderUserAuthenticationFilter.class);

        return http.build();
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, ObjectMapper objectMapper, String message)
            throws IOException {
        ApiError error = ApiError.builder()
                .code("ACCESS_DENIED")
                .message(message)
                .details(List.of())
                .build();
        ApiResponse<Void> apiResponse = ApiResponse.failure(error);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
