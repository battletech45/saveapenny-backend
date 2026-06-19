package com.saveapenny.test;

import org.testcontainers.containers.PostgreSQLContainer;

public class TestPostgresContainer {

    private static PostgreSQLContainer<?> container;

    public static PostgreSQLContainer<?> getInstance() {
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:15-alpine");
            container.start();
        }
        return container;
    }
}
