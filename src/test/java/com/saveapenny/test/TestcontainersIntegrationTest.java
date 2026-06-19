package com.saveapenny.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class TestcontainersIntegrationTest extends IntegrationTestBase {

    private static final String PG_PROFILE_PROP = "pg-integration";

    @DynamicPropertySource
    static void overrideWithPostgresIfActive(DynamicPropertyRegistry registry) {
        if (isPgIntegrationActive()) {
            var pg = TestPostgresContainer.getInstance();
            registry.add("spring.datasource.url", pg::getJdbcUrl);
            registry.add("spring.datasource.username", pg::getUsername);
            registry.add("spring.datasource.password", pg::getPassword);
        }
    }

    static boolean isPgIntegrationActive() {
        return "true".equals(System.getProperty(PG_PROFILE_PROP));
    }
}
