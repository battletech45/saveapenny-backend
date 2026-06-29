package com.saveapenny.config.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void protectedEndpoint_returns401_withCorrectEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/protected/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.message").value("Unauthorized."));
    }

    @Test
    void protectedEndpoint_reusesSafeRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/v1/protected/test").header("X-Request-Id", "req-123_ABC.def"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "req-123_ABC.def"));
    }

    @Test
    void protectedEndpoint_replacesUnsafeRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/v1/protected/test").header("X-Request-Id", "a".repeat(65)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(result -> {
                    String requestId = result.getResponse().getHeader("X-Request-Id");
                    org.junit.jupiter.api.Assertions.assertNotNull(requestId);
                    org.junit.jupiter.api.Assertions.assertEquals(36, requestId.length());
                });
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/protected/test")
        Map<String, String> test() {
            return Map.of("status", "ok");
        }
    }
}
