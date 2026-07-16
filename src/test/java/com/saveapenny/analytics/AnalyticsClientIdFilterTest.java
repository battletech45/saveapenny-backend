package com.saveapenny.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AnalyticsClientIdFilterTest {

    private final AnalyticsClientIdFilter filter = new AnalyticsClientIdFilter();

    @Test
    void capturesClientIdForDurationOfRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AnalyticsClientIdFilter.CLIENT_ID_HEADER, "app-instance-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertEquals("app-instance-123", AnalyticsClientIdFilter.currentClientId());

        filter.doFilter(request, response, chain);

        assertNull(AnalyticsClientIdFilter.currentClientId());
    }

    @Test
    void ignoresUnsafeClientId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AnalyticsClientIdFilter.CLIENT_ID_HEADER, "not safe; drop table");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertNull(AnalyticsClientIdFilter.currentClientId());

        filter.doFilter(request, response, chain);
    }

    @Test
    void ignoresMissingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertNull(AnalyticsClientIdFilter.currentClientId());

        filter.doFilter(request, response, chain);
    }

    @Test
    void capturesPlatformForDurationOfRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AnalyticsClientIdFilter.PLATFORM_HEADER, "Android");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertEquals("android", AnalyticsClientIdFilter.currentPlatform());

        filter.doFilter(request, response, chain);

        assertNull(AnalyticsClientIdFilter.currentPlatform());
    }

    @Test
    void ignoresUnknownPlatform() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AnalyticsClientIdFilter.PLATFORM_HEADER, "windows");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertNull(AnalyticsClientIdFilter.currentPlatform());

        filter.doFilter(request, response, chain);
    }

    @Test
    void ignoresMissingPlatformHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertNull(AnalyticsClientIdFilter.currentPlatform());

        filter.doFilter(request, response, chain);
    }
}
