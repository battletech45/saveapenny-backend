package com.saveapenny.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void reusesSafeIncomingRequestId() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123_ABC.def");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Request-Id", "req-123_ABC.def");
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get("requestId"));
    }

    @Test
    void exposesRequestIdInMdcDuringFilterChain() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123_ABC.def");

        FilterChain assertingChain = (req, res) -> assertEquals("req-123_ABC.def", MDC.get("requestId"));

        filter.doFilterInternal(request, response, assertingChain);

        verify(response).setHeader("X-Request-Id", "req-123_ABC.def");
        assertNull(MDC.get("requestId"));
    }

    @Test
    void replacesUnsafeIncomingRequestId() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("bad\nvalue");

        filter.doFilterInternal(request, response, filterChain);

        org.mockito.ArgumentCaptor<String> requestIdCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(org.mockito.Mockito.eq("X-Request-Id"), requestIdCaptor.capture());
        verify(filterChain).doFilter(request, response);
        assertNotNull(requestIdCaptor.getValue());
        assertEquals(36, requestIdCaptor.getValue().length());
        assertTrue(requestIdCaptor.getValue().matches("^[A-Za-z0-9-]{36}$"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void replacesTooLongIncomingRequestId() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("a".repeat(65));

        filter.doFilterInternal(request, response, filterChain);

        org.mockito.ArgumentCaptor<String> requestIdCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(org.mockito.Mockito.eq("X-Request-Id"), requestIdCaptor.capture());
        verify(filterChain).doFilter(request, response);
        assertNotNull(requestIdCaptor.getValue());
        assertEquals(36, requestIdCaptor.getValue().length());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void generatesRequestIdWhenMissing() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        org.mockito.ArgumentCaptor<String> requestIdCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(org.mockito.Mockito.eq("X-Request-Id"), requestIdCaptor.capture());
        verify(filterChain).doFilter(request, response);
        assertNotNull(requestIdCaptor.getValue());
        assertEquals(36, requestIdCaptor.getValue().length());
        assertNull(MDC.get("requestId"));
    }
}
