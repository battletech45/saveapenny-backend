package com.saveapenny.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = AsyncConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:async-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class AsyncConfigTest {

    @Test
    void contextLoadsWithAsyncConfig() {
    }

    @Test
    void importTaskExecutor_isConfigured(@Qualifier("importTaskExecutor") TaskExecutor executor) {
        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor tp = (ThreadPoolTaskExecutor) executor;
        assertTrue(tp.getThreadNamePrefix().startsWith("import-worker-"));
    }

    @Test
    void ocrTaskExecutor_isConfigured(@Qualifier("ocrTaskExecutor") TaskExecutor executor) {
        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor tp = (ThreadPoolTaskExecutor) executor;
        assertTrue(tp.getThreadNamePrefix().startsWith("ocr-worker-"));
    }
}
