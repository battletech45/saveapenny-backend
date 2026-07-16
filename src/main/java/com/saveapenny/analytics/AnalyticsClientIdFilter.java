package com.saveapenny.analytics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class AnalyticsClientIdFilter extends OncePerRequestFilter {

    public static final String CLIENT_ID_HEADER = "X-Analytics-Client-Id";
    public static final String PLATFORM_HEADER = "X-Client-Platform";
    private static final String CLIENT_ID_MDC_KEY = "analyticsClientId";
    private static final String PLATFORM_MDC_KEY = "analyticsClientPlatform";
    private static final int MAX_CLIENT_ID_LENGTH = 64;
    private static final Pattern SAFE_CLIENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final Set<String> KNOWN_PLATFORMS = Set.of("android", "ios");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientId = sanitizeClientId(request.getHeader(CLIENT_ID_HEADER));
        if (clientId != null) {
            MDC.put(CLIENT_ID_MDC_KEY, clientId);
        }
        String platform = sanitizePlatform(request.getHeader(PLATFORM_HEADER));
        if (platform != null) {
            MDC.put(PLATFORM_MDC_KEY, platform);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CLIENT_ID_MDC_KEY);
            MDC.remove(PLATFORM_MDC_KEY);
        }
    }

    public static String currentClientId() {
        return MDC.get(CLIENT_ID_MDC_KEY);
    }

    public static String currentPlatform() {
        return MDC.get(PLATFORM_MDC_KEY);
    }

    private String sanitizeClientId(String clientId) {
        if (clientId == null) {
            return null;
        }
        String normalized = clientId.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_CLIENT_ID_LENGTH) {
            return null;
        }
        if (!SAFE_CLIENT_ID_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private String sanitizePlatform(String platform) {
        if (platform == null) {
            return null;
        }
        String normalized = platform.trim().toLowerCase(java.util.Locale.ROOT);
        return KNOWN_PLATFORMS.contains(normalized) ? normalized : null;
    }
}
